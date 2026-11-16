package com.parakeetsandcapybaras.entity;

import com.parakeetsandcapybaras.registry.ModEntities;
import com.parakeetsandcapybaras.registry.ModTags;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * The parakeet (budgerigar). This class is based on Minecraft's own {@link Parrot} class, so it
 * flies, wanders between trees, dances to music and can ride on your shoulder. It is a separate
 * mob, so normal parrots are not changed.
 *
 * <p>Its stats and wild behaviour follow the "nm_budgie" MythicMobs configs:</p>
 * <ul>
 *   <li>12 health, no attack, takes 40% more damage from fire and lava, can't be leashed.</li>
 *   <li>Wild budgies fly around, look around and fly away from players (unless you hold seeds).</li>
 *   <li>Silent, except for high-pitched parrot hurt/death sounds.</li>
 *   <li>Right-click with an empty hand: it does a random trick (bite, dance or preen).</li>
 *   <li>5 colours: olive, sky blue, white, yellow and blue spangle (see {@link Variant}).</li>
 * </ul>
 *
 * <p>Pet features:</p>
 * <ul>
 *   <li>Tame with seeds and breed with seeds; chicks inherit a parent's colour.</li>
 *   <li>Right-click a tamed parakeet to cycle Follow -> Sit -> Stand.</li>
 *   <li>Shift + right-click (empty hand) to put it on your shoulder. Jump to let it hop off.</li>
 * </ul>
 */
public class Parakeet extends ShoulderRidingEntity implements FlyingAnimal {
    // "Synched" data is automatically sent from the server to every player, so the
    // renderer always knows which colour to draw.
    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Parakeet.class, EntityDataSerializers.INT);

    // Wing flapping animation values (copied from the parrot). Read by the renderer.
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1.0F;
    private float nextFlap = 1.0F;

    // Dancing: Minecraft tells every mob near a jukebox when music starts/stops.
    private boolean partyParakeet;
    @Nullable
    private BlockPos jukebox;

    private Mode mode = Mode.FOLLOW;

    // Tricks (bite / dance / preen) shown when a player right-clicks the bird.
    /** Damage from fire and lava is multiplied by this (from the "DamageModifiers" in the config). */
    private static final float FIRE_DAMAGE_MULTIPLIER = 1.4F;
    /** How long to wait between tricks: 3 seconds (60 ticks), like "cd=3" in the config. */
    private static final int TRICK_COOLDOWN = 60;
    private static final byte FIRST_TRICK_EVENT = 100;
    private long lastTrickTime = -TRICK_COOLDOWN;
    /** Client only: which trick is playing right now, and when it started. Read by the renderer. */
    @Nullable
    public Trick currentTrick;
    public int trickStartTick;

    public Parakeet(EntityType<? extends Parakeet> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    // ------------------------------------------------------------------
    // Attributes, AI and movement
    // ------------------------------------------------------------------

    /** Health 12, speed 0.2 and no attack damage, as in the budgie configs. Registered in CommonModEvents. */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FLYING_SPEED, 0.4F)
                .add(Attributes.MOVEMENT_SPEED, 0.2F)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    /**
     * Goals are the mob's "brain". Lower numbers = higher priority. Some goals are wrapped so
     * that they only run in the right {@link Mode}.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TamableAnimalPanicGoal(1.25));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        // Wild budgies come closer when you hold seeds...
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.0, this::isFood, false) {
            @Override
            public boolean canUse() {
                return !Parakeet.this.isTame() && super.canUse();
            }
        });
        // ...but otherwise they fly away from players ("fleeplayers").
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0, 1.3,
                player -> !this.isTame() && !player.isSpectator() && !(player instanceof Player p && p.isCreative())
                        && !this.isFood(player.getMainHandItem()) && !this.isFood(player.getOffhandItem())));
        // Tamed budgies keep an eye on you; wild ones just look around ("randomlookaround").
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return Parakeet.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F) {
            @Override
            public boolean canUse() {
                return Parakeet.this.mode == Mode.FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Parakeet.this.mode == Mode.FOLLOW && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new FollowParentGoal(this, 1.1) {
            @Override
            public boolean canUse() {
                return Parakeet.this.isAllowedToRoam() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new ParakeetWanderGoal(this, 1.0));
        this.goalSelector.addGoal(3, new FollowMobGoal(this, 1.0, 3.0F, 7.0F) {
            @Override
            public boolean canUse() {
                return Parakeet.this.isTame() && Parakeet.this.isAllowedToRoam() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Parakeet.this.isAllowedToRoam() && super.canContinueToUse();
            }
        });
    }

    /** Wild parakeets, and tamed ones set to Follow, may fly around on their own. */
    boolean isAllowedToRoam() {
        return !this.isTame() || this.mode == Mode.FOLLOW;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    /** Only teleport to the owner while in Follow mode. */
    @Override
    public boolean shouldTryTeleportToOwner() {
        return this.mode == Mode.FOLLOW && super.shouldTryTeleportToOwner();
    }

    @Override
    public void aiStep() {
        // Stop dancing if we flew away from the jukebox or it was removed.
        if (this.jukebox == null
                || !this.jukebox.closerToCenterThan(this.position(), 3.46)
                || !this.level().getBlockState(this.jukebox).is(Blocks.JUKEBOX)) {
            this.partyParakeet = false;
            this.jukebox = null;
        }

        // Forget a trick once it has finished playing.
        if (this.currentTrick != null && this.tickCount - this.trickStartTick > this.currentTrick.durationTicks) {
            this.currentTrick = null;
        }

        super.aiStep();
        this.calculateFlapping();
    }

    /** Called by Minecraft when a nearby jukebox starts or stops playing. */
    @Override
    public void setRecordPlayingNearby(BlockPos pos, boolean isPartying) {
        this.jukebox = pos;
        this.partyParakeet = isPartying;
    }

    public boolean isPartyParakeet() {
        return this.partyParakeet;
    }

    private void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = this.flapSpeed + (float) (!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround() && movement.y < 0.0) {
            // Fall slowly, like a parrot.
            this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    // ------------------------------------------------------------------
    // Player interaction: taming, breeding, modes and shoulder riding
    // ------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 0) Empty hand (not sneaking) = the bird does a random trick: bite, dance or preen.
        boolean emptyHandClick = stack.isEmpty() && !player.isShiftKeyDown();
        if (emptyHandClick && !this.level().isClientSide) {
            this.tryRandomTrick();
        }
        if (emptyHandClick && !(this.isTame() && this.isOwnedBy(player))) {
            return InteractionResult.SUCCESS; // wild bird or someone else's: just the trick
        }

        // 1) Not tamed yet + seeds = try to tame (1 in 10 chance, just like a parrot).
        if (!this.isTame() && this.isFood(stack)) {
            this.usePlayerItem(player, hand, stack);
            if (!this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PARROT_EAT,
                        this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(10) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.setMode(Mode.FOLLOW);
                    this.level().broadcastEntityEvent(this, (byte) 7); // hearts
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6); // smoke
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 2) Cookies are poisonous to parrots, and to parakeets too.
        if (stack.is(ItemTags.PARROT_POISONOUS_FOOD)) {
            this.usePlayerItem(player, hand, stack);
            this.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
            if (player.isCreative() || !this.isInvulnerable()) {
                this.hurt(this.damageSources().playerAttack(player), Float.MAX_VALUE);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isTame() && this.isOwnedBy(player)) {
            // 3) Tamed + seeds = heal it, grow a baby, or make it ready to breed.
            if (this.isFood(stack)) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.usePlayerItem(player, hand, stack);
                    this.heal(2.0F);
                    return InteractionResult.SUCCESS;
                }
                return super.mobInteract(player, hand); // the normal Animal breeding code
            }

            // 4) Shift + right-click with an empty hand = hop onto your shoulder.
            if (player.isShiftKeyDown() && stack.isEmpty()) {
                if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    if (this.isBaby()) {
                        player.displayClientMessage(Component.translatable("message.parakeetsandcapybaras.parakeet.too_young"), true);
                    } else {
                        // Make sure it doesn't stay sitting when it hops off again.
                        this.setMode(Mode.FOLLOW);
                        if (!this.setEntityOnShoulder(serverPlayer)) {
                            player.displayClientMessage(Component.translatable("message.parakeetsandcapybaras.parakeet.no_room"), true);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // 5) Normal right-click = change mode: Follow -> Sit -> Stand -> Follow ...
            if (!this.level().isClientSide) {
                this.setMode(this.mode.next());
                player.displayClientMessage(
                        Component.translatable("message.parakeetsandcapybaras.parakeet.mode." + this.mode.getSerializedName(), this.getDisplayName()), true);
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
        // Fire and lava hurt budgies 40% more.
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= FIRE_DAMAGE_MULTIPLIER;
        }
        // Like a parrot: getting hurt makes it stand up.
        if (this.mode == Mode.SIT) {
            this.setMode(Mode.FOLLOW);
        }
        return super.hurtServer(level, source, amount);
    }

    /** Server side: pick a random trick (at most once every 3 seconds) and tell all players to show it. */
    private void tryRandomTrick() {
        long now = this.level().getGameTime();
        if (now - this.lastTrickTime < TRICK_COOLDOWN) {
            return;
        }
        this.lastTrickTime = now;
        Trick trick = Util.getRandom(Trick.values(), this.random);
        this.level().broadcastEntityEvent(this, (byte) (FIRST_TRICK_EVENT + trick.ordinal()));
    }

    /** Client side: receives the trick sent by {@link #tryRandomTrick()}. */
    @Override
    public void handleEntityEvent(byte id) {
        int index = id - FIRST_TRICK_EVENT;
        if (index >= 0 && index < Trick.values().length) {
            this.currentTrick = Trick.values()[index];
            this.trickStartTick = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Budgies can't be put on a lead ("PreventLeashing"). */
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ------------------------------------------------------------------
    // Breeding
    // ------------------------------------------------------------------

    /** Seeds (see the parakeet_food item tag). */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.PARAKEET_FOOD);
    }

    /** Both parakeets must be tamed, not sitting, and "in love". */
    @Override
    public boolean canMate(Animal other) {
        if (other == this || !this.isTame() || !(other instanceof Parakeet partner)) {
            return false;
        }
        return partner.isTame() && !partner.isInSittingPose() && !this.isInSittingPose() && this.isInLove() && partner.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        Parakeet baby = ModEntities.PARAKEET.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            // The chick gets the colour of one of its parents, with a small chance of a random "mutation".
            if (this.random.nextInt(10) == 0 || !(otherParent instanceof Parakeet otherParakeet)) {
                baby.setVariant(Util.getRandom(Variant.values(), this.random));
            } else {
                baby.setVariant(this.random.nextBoolean() ? this.getVariant() : otherParakeet.getVariant());
            }

            if (this.isTame() && this.getOwnerUUID() != null) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
            }
        }
        return baby;
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        this.setVariant(Util.getRandom(Variant.values(), level.getRandom()));
        if (groupData == null) {
            groupData = new AgeableMobGroupData(false); // wild parakeets spawn as adults
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    /** Same rule as parrots: must spawn on leaves, grass, logs or air with enough light. */
    public static boolean checkParakeetSpawnRules(EntityType<Parakeet> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.PARROTS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    // ------------------------------------------------------------------
    // Sounds: budgies are silent, except for high-pitched parrot hurt and death sounds
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return null; // no chirping or imitating
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // silent
    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        // silent wings, but keep the flap timer going
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    /** Pitch 1.9: a lot higher than a parrot (which uses about 1.0). */
    @Override
    public float getVoicePitch() {
        return 1.9F;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    // ------------------------------------------------------------------
    // Misc parrot behaviour
    // ------------------------------------------------------------------

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Birds don't take fall damage.
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!(entity instanceof Player)) {
            super.doPush(entity);
        }
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    // ------------------------------------------------------------------
    // Colour variant + saving
    // ------------------------------------------------------------------

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.getId());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT_ID, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().getId());
        tag.putString("Mode", this.mode.getSerializedName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(Variant.byId(tag.getInt("Variant")));
        this.mode = Mode.byName(tag.getString("Mode"));
        if (this.isOrderedToSit()) {
            this.mode = Mode.SIT;
        }
    }

    /**
     * The 5 parakeet colours, the same set as the nm_budgie configs. All are real budgerigar
     * colours.
     */
    public enum Variant implements StringRepresentable {
        OLIVE(0, "olive"),               // dark olive-green body with a yellow face
        SKY_BLUE(1, "sky_blue"),         // blue body with a white face
        YELLOW(2, "yellow"),             // "lutino": all yellow with red eyes
        WHITE(3, "white"),               // "albino": all white with red eyes
        BLUE_SPANGLE(4, "blue_spangle"); // blue body, white wings with dark spots

        private static final IntFunction<Variant> BY_ID = ByIdMap.continuous(Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
        private final int id;
        private final String name;

        Variant(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /** Tricks a budgie does when you right-click it (the bite / dance / preen animations). */
    public enum Trick {
        BITE(20),   // two quick pecks
        DANCE(40),  // bobs from side to side
        PREEN(40);  // cleans the feathers on its wing

        /** How long the trick lasts, in ticks (20 ticks = 1 second). */
        public final int durationTicks;

        Trick(int durationTicks) {
            this.durationTicks = durationTicks;
        }
    }

    /** What a tamed parakeet does. Right-click it to switch to the next mode. */
    public enum Mode implements StringRepresentable {
        FOLLOW("follow"), // follows you around (and wanders near you)
        SIT("sit"),       // sits down and stays put
        STAND("stand");   // stands still and stays put

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

    /** Copy of the parrot's wander goal: likes to fly to the tops of trees. */
    static class ParakeetWanderGoal extends WaterAvoidingRandomFlyingGoal {
        private final Parakeet parakeet;

        ParakeetWanderGoal(Parakeet parakeet, double speedModifier) {
            super(parakeet, speedModifier);
            this.parakeet = parakeet;
        }

        @Override
        public boolean canUse() {
            return this.parakeet.isAllowedToRoam() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.parakeet.isAllowedToRoam() && super.canContinueToUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            Vec3 target = null;
            if (this.mob.isInWater()) {
                target = LandRandomPos.getPos(this.mob, 15, 15);
            }

            if (this.mob.getRandom().nextFloat() >= this.probability) {
                target = this.getTreePos();
            }

            return target == null ? super.getPosition() : target;
        }

        @Nullable
        private Vec3 getTreePos() {
            PathfinderMob mob = this.mob;
            BlockPos current = mob.blockPosition();
            BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

            for (BlockPos pos : BlockPos.betweenClosed(
                    Mth.floor(mob.getX() - 3.0), Mth.floor(mob.getY() - 6.0), Mth.floor(mob.getZ() - 3.0),
                    Mth.floor(mob.getX() + 3.0), Mth.floor(mob.getY() + 6.0), Mth.floor(mob.getZ() + 3.0))) {
                if (!current.equals(pos)) {
                    BlockState stateBelow = mob.level().getBlockState(below.setWithOffset(pos, Direction.DOWN));
                    boolean isTree = stateBelow.getBlock() instanceof LeavesBlock || stateBelow.is(BlockTags.LOGS);
                    if (isTree && mob.level().isEmptyBlock(pos) && mob.level().isEmptyBlock(above.setWithOffset(pos, Direction.UP))) {
                        return Vec3.atBottomCenterOf(pos);
                    }
                }
            }
            return null;
        }
    }
}
