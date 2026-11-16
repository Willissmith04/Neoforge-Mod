package com.parakeetsandcapybaras.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The bird cage block (used for both the gold and the iron cage).
 *
 * <ul>
 *   <li>Place it on top of any block, or click the bottom of a block to hang it from the ceiling.</li>
 *   <li>Right-click it with an empty hand to let the bird out.</li>
 *   <li>Break it and the bird stays inside the dropped cage item.</li>
 * </ul>
 *
 * <p>The bird itself is stored in {@link BirdCageBlockEntity} and drawn by the BirdCageRenderer.</p>
 */
public class BirdCageBlock extends Block implements EntityBlock {
    public static final MapCodec<BirdCageBlock> CODEC = simpleCodec(BirdCageBlock::new);

    /** Which way the cage (and the bird inside) is facing. */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** true = hanging from the block above, false = standing on the block below. */
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public BirdCageBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HANGING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING);
    }

    // ---------- placing and staying in place ----------

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState facingPlayer = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        // Clicking the underside of a block hangs the cage; anything else stands it up.
        boolean preferHanging = context.getClickedFace() == Direction.DOWN;

        BlockState first = facingPlayer.setValue(HANGING, preferHanging);
        if (first.canSurvive(context.getLevel(), context.getClickedPos())) {
            return first;
        }
        BlockState second = facingPlayer.setValue(HANGING, !preferHanging);
        return second.canSurvive(context.getLevel(), context.getClickedPos()) ? second : null;
    }

    /** The block the cage stands on (or hangs from) can be any block that isn't air, water, grass etc. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction towardsSupport = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        BlockState support = level.getBlockState(pos.relative(towardsSupport));
        return !support.canBeReplaced();
    }

    /** If the supporting block is removed, the cage pops off and drops as an item (with the bird inside). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction towardsSupport = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        if (direction == towardsSupport && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ---------- letting the bird out ----------

    /** Right-click with an empty hand: open the cage and let the bird out. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS; // holding something: let the item do its normal thing
        }
        if (!(level.getBlockEntity(pos) instanceof BirdCageBlockEntity cage) || !cage.hasBird()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            releaseBird(level, pos, state, cage);
        }
        return InteractionResult.SUCCESS;
    }

    /** In creative mode broken blocks don't drop, so free the bird instead of deleting it. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof BirdCageBlockEntity cage && cage.hasBird()) {
            releaseBird(level, pos, state, cage);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public static void releaseBird(Level level, BlockPos pos, BlockState state, BirdCageBlockEntity cage) {
        CompoundTag birdData = cage.getBirdData();
        Direction facing = state.getValue(FACING);

        // Let it out through the front of the cage if there is room, otherwise just above it.
        BlockPos front = pos.relative(facing);
        Vec3 spawnPos = level.getBlockState(front).getCollisionShape(level, front).isEmpty()
                ? Vec3.atBottomCenterOf(front)
                : Vec3.atBottomCenterOf(pos).add(0.0, 1.0, 0.0);

        EntityType.create(birdData, level, EntitySpawnReason.LOAD).ifPresent(bird -> {
            bird.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, facing.toYRot(), 0.0F);
            bird.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(bird);
        });

        cage.setBirdData(new CompoundTag());
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    // ---------- block entity ----------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BirdCageBlockEntity(pos, state);
    }
}
