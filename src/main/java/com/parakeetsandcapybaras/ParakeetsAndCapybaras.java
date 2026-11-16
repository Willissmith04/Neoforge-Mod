package com.parakeetsandcapybaras;

import com.parakeetsandcapybaras.registry.ModBlockEntities;
import com.parakeetsandcapybaras.registry.ModBlocks;
import com.parakeetsandcapybaras.registry.ModDataComponents;
import com.parakeetsandcapybaras.registry.ModEntities;
import com.parakeetsandcapybaras.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * The main class of the mod. NeoForge creates this class once when the game starts.
 *
 * <p>All it does is hand every "Deferred Register" to the mod event bus. A deferred register
 * is a list of things (entities, items, blocks...) that we want to add to the game. NeoForge
 * adds them at the right moment during loading.</p>
 *
 * <p>Event listeners (spawning rules, attributes, rendering...) are in the {@code event} and
 * {@code client} packages. They use the {@code @EventBusSubscriber} annotation, so NeoForge finds
 * and registers them automatically.</p>
 */
@Mod(ParakeetsAndCapybaras.MOD_ID)
public class ParakeetsAndCapybaras {
    /** The mod id. It must match "mod_id" in gradle.properties. */
    public static final String MOD_ID = "parakeetsandcapybaras";

    public ParakeetsAndCapybaras(IEventBus modEventBus) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
    }

    /** Small helper: turns "parakeet" into "parakeetsandcapybaras:parakeet". */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
