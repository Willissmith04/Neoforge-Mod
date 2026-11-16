package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.item.BirdCageItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers our items: two bird cages and two spawn eggs. */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ParakeetsAndCapybaras.MOD_ID);

    // The cage items place the cage blocks. Empty cages stack up to 16.
    public static final DeferredItem<BirdCageItem> GOLD_BIRD_CAGE = ITEMS.registerItem(
            "gold_bird_cage",
            props -> new BirdCageItem(ModBlocks.GOLD_BIRD_CAGE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().stacksTo(16));

    public static final DeferredItem<BirdCageItem> IRON_BIRD_CAGE = ITEMS.registerItem(
            "iron_bird_cage",
            props -> new BirdCageItem(ModBlocks.IRON_BIRD_CAGE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().stacksTo(16));

    // Spawn eggs. Their colours are set in assets/parakeetsandcapybaras/items/*_spawn_egg.json
    public static final DeferredItem<SpawnEggItem> PARAKEET_SPAWN_EGG = ITEMS.registerItem(
            "parakeet_spawn_egg", props -> new SpawnEggItem(ModEntities.PARAKEET.get(), props));

    public static final DeferredItem<SpawnEggItem> CAPYBARA_SPAWN_EGG = ITEMS.registerItem(
            "capybara_spawn_egg", props -> new SpawnEggItem(ModEntities.CAPYBARA.get(), props));
}
