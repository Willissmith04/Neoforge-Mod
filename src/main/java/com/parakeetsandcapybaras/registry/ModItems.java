package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.item.BirdCageItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers our items: two bird cages and two spawn eggs. */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ParakeetsAndCapybaras.MOD_ID);

    // The cage items place the cage blocks. Empty cages stack up to 16.
    public static final DeferredItem<BirdCageItem> GOLD_BIRD_CAGE = ITEMS.registerItem(
            "gold_bird_cage",
            props -> new BirdCageItem(ModBlocks.GOLD_BIRD_CAGE.get(), props),
            new Item.Properties().stacksTo(16));

    public static final DeferredItem<BirdCageItem> IRON_BIRD_CAGE = ITEMS.registerItem(
            "iron_bird_cage",
            props -> new BirdCageItem(ModBlocks.IRON_BIRD_CAGE.get(), props),
            new Item.Properties().stacksTo(16));

    // Spawn eggs: the two numbers are the egg's base colour and spot colour (as 0xRRGGBB).
    public static final DeferredItem<DeferredSpawnEggItem> PARAKEET_SPAWN_EGG = ITEMS.registerItem(
            "parakeet_spawn_egg", props -> new DeferredSpawnEggItem(ModEntities.PARAKEET, 0x5E7428, 0xE2D85A, props));

    public static final DeferredItem<DeferredSpawnEggItem> CAPYBARA_SPAWN_EGG = ITEMS.registerItem(
            "capybara_spawn_egg", props -> new DeferredSpawnEggItem(ModEntities.CAPYBARA, 0x9C6B3F, 0x4A3020, props));
}
