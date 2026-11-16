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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
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
 * flies, wanders between trees, imitates nearby monsters, dances to music and can ride on your
 * shoulder. It is a separate mob, so normal parrots are not changed.
 *
 * <p>What is different from a parrot:</p>
 * <ul>
 *   <li>It comes in 6 real-life budgie colours (see {@link Variant}).</li>
 *   <li>It can be bred with seeds and has babies.</li>
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

    /** Health, speed etc. Same numbers as the parrot. Registered in CommonModEvents. */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.4F)
                .add(Attributes.MOVEMENT_SPEED, 0.2F)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
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
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
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
                return Parakeet.this.isAllowedToRoam() && super.canUse();
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

        // Real budgies are great mimics too! Re-use the parrot's imitation code.
        if (this.level().random.nextInt(400) == 0) {
            Parrot.imitateNearbyMobs(this.level(), this);
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
        // Like a parrot: getting hurt makes it stand up.
        if (this.mode == Mode.SIT) {
            this.setMode(Mode.FOLLOW);
        }
        return super.hurtServer(level, source, amount);
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
    // Sounds (parrot sounds, slightly higher pitched because parakeets are smaller)
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return Parrot.getAmbient(this.level(), this.level().random);
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
        this.playSound(SoundEvents.PARROT_STEP, 0.15F, 1.0F);
    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.playSound(SoundEvents.PARROT_FLY, 0.15F, 1.0F);
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.3F;
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
     * The 6 parakeet colours. These are real budgerigar colours: green is the wild colour found
     * in Australia, the others are common pet colours.
     */
    public enum Variant implements StringRepresentable {
        GREEN(0, "green"),       // wild-type light green with a yellow face
        SKY_BLUE(1, "sky_blue"), // blue body with a white face
        YELLOW(2, "yellow"),     // "lutino": all yellow with red eyes
        WHITE(3, "white"),       // "albino": all white with red eyes
        VIOLET(4, "violet"),     // violet body with a white face
        GREY(5, "grey");         // grey body with a white face

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
