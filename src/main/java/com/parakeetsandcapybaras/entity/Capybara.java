package com.parakeetsandcapybaras.entity;

import com.parakeetsandcapybaras.entity.ai.BabyRideAdultGoal;
import com.parakeetsandcapybaras.registry.ModEntities;
import com.parakeetsandcapybaras.registry.ModTags;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The capybara: a calm, tameable animal.
 *
 * <p>Model, texture and animations: converted from the Nog's Menagerie nm_capybara model. Stats
 * follow its MythicMobs config: 24 health, no attack, silent apart from deep strider hurt/death
 * sounds, and highly flammable (4x fire damage).</p>
 *
 * <ul>
 *   <li>Tame it with melon slices (1 in 3 chance), then feed it melons to breed it.</li>
 *   <li>Right-click with an empty hand and it sniffs you.</li>
 *   <li>Right-click a tamed capybara to cycle Follow -> Sit -> Wander.</li>
 *   <li>While sitting it lies flat on its belly with its legs spread out.</li>
 *   <li>Baby capybaras like to ride on an adult's back, like baby striders do. Right-click the
 *       baby (or the adult carrying it) with an empty hand to take the baby off.</li>
 * </ul>
 */
public class Capybara extends TamableAnimal {
    /** How long (in ticks, 20 ticks = 1 second) a baby waits before climbing back on after being taken off. */
    private static final int RIDE_COOLDOWN_AFTER_REMOVAL = 20 * 60;

    private Mode mode = Mode.FOLLOW;
    private int rideCooldown;

    /** Fire hurts capybaras 4 times as much ("DamageModifiers: FIRE 4" in the config). */
    private static final float FIRE_DAMAGE_MULTIPLIER = 4.0F;
    /** Wait 2 seconds between sniffs (the "cd=2" in the config). */
    private static final int SNIFF_COOLDOWN = 40;
    private static final byte SNIFF_EVENT = 100;
    private long lastSniffTime = -SNIFF_COOLDOWN;

    // Animations (client only). The renderer copies these and the model plays the matching
    // animation from assets/parakeetsandcapybaras/neoforge/animations/entity/.
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sniffAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    public Capybara(EntityType<? extends Capybara> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------
    // Attributes and AI
    // ------------------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this)); // capybaras are great swimmers
        this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false) {
            @Override
            public boolean canUse() {
                return !Capybara.this.isOrderedToSit() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F) {
            @Override
            public boolean canUse() {
                return Capybara.this.mode == Mode.FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Capybara.this.mode == Mode.FOLLOW && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(6, new BabyRideAdultGoal(this));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    /** Only teleport to the owner while in Follow mode (not while wandering). */
    @Override
    public boolean shouldTryTeleportToOwner() {
        return this.mode == Mode.FOLLOW && super.shouldTryTeleportToOwner();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            this.idleAnimationState.startIfStopped(this.tickCount);
            if (this.isDeadOrDying()) {
                this.deathAnimationState.startIfStopped(this.tickCount);
            }
        } else if (!this.isInSittingPose() && this.getNavigation().isDone() && this.random.nextInt(600) == 0) {
            this.trySniff(); // every now and then, sniff around
        }
        if (this.rideCooldown > 0) {
            this.rideCooldown--;
        }
    }

    // ------------------------------------------------------------------
    // Player interaction
    // ------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1) Take a riding baby off: click the baby itself, or the adult carrying it.
        if (stack.isEmpty()) {
            if (this.isBaby() && this.getVehicle() instanceof Capybara) {
                if (!this.level().isClientSide) {
                    this.takeOffAdult();
                }
                return InteractionResult.SUCCESS;
            }
            if (this.getFirstPassenger() instanceof Capybara baby) {
                if (!this.level().isClientSide) {
                    baby.takeOffAdult();
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 2) Empty hand = it sniffs you. (Your own capybara also changes mode, see step 5.)
        if (stack.isEmpty() && !player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                this.trySniff();
            }
            if (!(this.isTame() && this.isOwnedBy(player))) {
                return InteractionResult.SUCCESS;
            }
        }

        // 3) Not tamed + melon = try to tame (1 in 3 chance, like a wolf).
        if (!this.isTame() && this.isFood(stack)) {
            this.usePlayerItem(player, hand, stack);
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
            if (!this.level().isClientSide) {
                if (this.random.nextInt(3) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.setMode(Mode.FOLLOW);
                    this.level().broadcastEntityEvent(this, (byte) 7); // hearts
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6); // smoke
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isTame() && this.isOwnedBy(player)) {
            // 4) Tamed + melon = heal, grow a baby, or make it ready to breed.
            if (this.isFood(stack)) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.usePlayerItem(player, hand, stack);
                    this.heal(4.0F);
                    this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
                return super.mobInteract(player, hand); // the normal Animal breeding code
            }

            // 5) Anything else (that isn't used on mobs, like a lead or name tag) = change mode.
            InteractionResult result = super.mobInteract(player, hand);
            if (result.consumesAction()) {
                return result;
            }
            if (!this.level().isClientSide) {
                this.setMode(this.mode.next());
                player.displayClientMessage(
                        Component.translatable("message.parakeetsandcapybaras.capybara.mode." + this.mode.getSerializedName(), this.getDisplayName()), true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    public Mode getMode() {
        return this.mode;
    }

    public void setMode(Mode newMode) {
        this.mode = newMode;
        boolean sit = newMode == Mode.SIT;
        this.setOrderedToSit(sit);
        this.setInSittingPose(sit);
        this.jumping = false;
        this.navigation.stop();
        this.setTarget(null);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= FIRE_DAMAGE_MULTIPLIER; // includes lava
        }
        if (this.mode == Mode.SIT) {
            this.setMode(Mode.FOLLOW);
        }
        return super.hurtServer(level, source, amount);
    }

    /** Server side: start a sniff (at most once every 2 seconds) and tell all players to show it. */
    private void trySniff() {
        long now = this.level().getGameTime();
        if (now - this.lastSniffTime >= SNIFF_COOLDOWN) {
            this.lastSniffTime = now;
            this.level().broadcastEntityEvent(this, SNIFF_EVENT);
        }
    }

    /** Client side: receives the sniff sent by {@link #trySniff()}. */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == SNIFF_EVENT) {
            this.sniffAnimationState.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------
    // Babies riding adults (like striders)
    // ------------------------------------------------------------------

    /** Used by {@link BabyRideAdultGoal}: may this baby climb onto an adult right now? */
    public boolean canClimbOnAdult() {
        return this.isBaby() && !this.isPassenger() && !this.isOrderedToSit() && this.rideCooldown <= 0;
    }

    /** Get off the adult and don't climb back on for a minute. */
    public void takeOffAdult() {
        this.stopRiding();
        this.rideCooldown = RIDE_COOLDOWN_AFTER_REMOVAL;
    }

    /** Only one baby capybara at a time may ride on an adult. */
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isBaby() && this.getPassengers().isEmpty() && passenger instanceof Capybara capybara && capybara.isBaby();
    }

    /** The baby on our back is just along for the ride: the adult keeps walking on its own. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    /** When the adult lies down, the baby on its back goes down with it. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Vec3 point = super.getPassengerAttachmentPoint(passenger, dimensions, partialTick);
        return this.isInSittingPose() ? point.subtract(0.0, 5.0 / 16.0, 0.0) : point; // the body sinks 5 pixels
    }

    /** When a riding baby grows up it climbs down. */
    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby() && this.getVehicle() instanceof Capybara) {
            this.stopRiding();
        }
    }

    // ------------------------------------------------------------------
    // Breeding
    // ------------------------------------------------------------------

    /** Melons (see the capybara_food item tag). */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.CAPYBARA_FOOD);
    }

    @Override
    public boolean canMate(Animal other) {
        if (other == this || !(other instanceof Capybara partner)) {
            return false;
        }
        return !this.isInSittingPose() && !partner.isInSittingPose() && this.isInLove() && partner.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        Capybara baby = ModEntities.CAPYBARA.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null && this.isTame() && this.getOwnerUUID() != null) {
            baby.setOwnerUUID(this.getOwnerUUID());
            baby.setTame(true, true);
        }
        return baby;
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    /**
     * Like a strider: sometimes an adult spawns with a baby already riding on its back.
     * Groups can also contain babies, which will hop onto adults by themselves.
     */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        if (this.isBaby()) {
            return super.finalizeSpawn(level, difficulty, reason, groupData);
        }

        if (reason != EntitySpawnReason.JOCKEY && level.getRandom().nextInt(5) == 0) {
            Capybara baby = ModEntities.CAPYBARA.get().create(level.getLevel(), EntitySpawnReason.JOCKEY);
            if (baby != null) {
                baby.setAge(-24000);
                baby.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                baby.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
                baby.startRiding(this, true);
            }
        }

        if (groupData == null) {
            groupData = new AgeableMobGroupData(0.2F); // 20% of the others in a group are babies
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // ------------------------------------------------------------------
    // Sounds: capybaras are silent, except for deep strider sounds when hurt or killed
    // ------------------------------------------------------------------

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // silent
    }

    /** Pitch 0.8: a bit deeper than normal. */
    @Override
    public float getVoicePitch() {
        return 0.8F;
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Mode", this.mode.getSerializedName());
        tag.putInt("RideCooldown", this.rideCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.mode = Mode.byName(tag.getString("Mode"));
        if (this.isOrderedToSit()) {
            this.mode = Mode.SIT;
        }
        this.rideCooldown = tag.getInt("RideCooldown");
    }

    /** What a tamed capybara does. Right-click it to switch to the next mode. */
    public enum Mode implements StringRepresentable {
        FOLLOW("follow"), // follows you around
        SIT("sit"),       // lies down and stays put
        WANDER("wander"); // walks around freely, doesn't follow you

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public Mode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (mode.name.equals(name)) {
                    return mode;
                }
            }
            return FOLLOW;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
