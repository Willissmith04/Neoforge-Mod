package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.block.BirdCageBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A "block entity" is extra data stored with a block. The bird cage uses one to remember
 * which bird is inside it.
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ParakeetsAndCapybaras.MOD_ID);

    // Both cages (gold and iron) share the same block entity type.
    public static final Supplier<BlockEntityType<BirdCageBlockEntity>> BIRD_CAGE = BLOCK_ENTITY_TYPES.register(
            "bird_cage",
            () -> BlockEntityType.Builder.of(BirdCageBlockEntity::new, ModBlocks.GOLD_BIRD_CAGE.get(), ModBlocks.IRON_BIRD_CAGE.get()).build(null));
}
