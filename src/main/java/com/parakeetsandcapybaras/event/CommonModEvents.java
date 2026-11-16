package com.parakeetsandcapybaras.event;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Capybara;
import com.parakeetsandcapybaras.entity.Parakeet;
import com.parakeetsandcapybaras.registry.ModEntities;
import com.parakeetsandcapybaras.registry.ModItems;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Setup events that run while the game is loading (on both the client and the server).
 *
 * <p>Note: <i>which biomes</i> the mobs spawn in, and how often, is not set here. That is in the
 * JSON files in data/parakeetsandcapybaras/neoforge/biome_modifier/.</p>
 */
@EventBusSubscriber(modid = ParakeetsAndCapybaras.MOD_ID)
public class CommonModEvents {

    /** Every living mob needs attributes (health, speed...). */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PARAKEET.get(), Parakeet.createAttributes().build());
        event.put(ModEntities.CAPYBARA.get(), Capybara.createAttributes().build());
    }

    /** Rules for *where exactly* a mob may appear when it spawns naturally. */
    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Parakeets: like parrots, on leaves/grass/logs (so also on top of trees).
        event.register(ModEntities.PARAKEET.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING,
                Parakeet::checkParakeetSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Capybaras: like other farm animals, on grass blocks in daylight.
        event.register(ModEntities.CAPYBARA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Puts our items into the vanilla creative inventory tabs. */
    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.PARAKEET_SPAWN_EGG.get());
            event.accept(ModItems.CAPYBARA_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.GOLD_BIRD_CAGE.get());
            event.accept(ModItems.IRON_BIRD_CAGE.get());
        }
    }
}
