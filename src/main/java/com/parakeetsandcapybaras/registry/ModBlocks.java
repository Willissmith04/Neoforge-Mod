package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.block.BirdCageBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the two bird cage blocks (gold and iron). */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ParakeetsAndCapybaras.MOD_ID);

    public static final DeferredBlock<BirdCageBlock> GOLD_BIRD_CAGE = BLOCKS.registerBlock(
            "gold_bird_cage", BirdCageBlock::new, cageProperties(MapColor.GOLD));

    public static final DeferredBlock<BirdCageBlock> IRON_BIRD_CAGE = BLOCKS.registerBlock(
            "iron_bird_cage", BirdCageBlock::new, cageProperties(MapColor.METAL));

    private static BlockBehaviour.Properties cageProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.0F)                 // a bit easier to break than a lantern
                .sound(SoundType.LANTERN)
                .noOcclusion()                  // the cage is see-through, so don't hide neighbouring faces
                .pushReaction(PushReaction.DESTROY); // pistons break it (and it drops with the bird inside)
    }
}
