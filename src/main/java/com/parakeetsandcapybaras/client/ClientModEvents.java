package com.parakeetsandcapybaras.client;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.client.model.CapybaraModel;
import com.parakeetsandcapybaras.client.model.ModModelLayers;
import com.parakeetsandcapybaras.client.renderer.BirdCageRenderer;
import com.parakeetsandcapybaras.client.renderer.CapybaraRenderer;
import com.parakeetsandcapybaras.client.renderer.ParakeetOnShoulderLayer;
import com.parakeetsandcapybaras.client.renderer.ParakeetRenderer;
import com.parakeetsandcapybaras.registry.ModBlockEntities;
import com.parakeetsandcapybaras.registry.ModEntities;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Client-only setup: tells the game how to draw our mobs and the bird inside cages.
 * {@code value = Dist.CLIENT} makes sure this never runs on a dedicated server.
 */
@EventBusSubscriber(modid = ParakeetsAndCapybaras.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PARAKEET.get(), ParakeetRenderer::new);
        event.registerEntityRenderer(ModEntities.CAPYBARA.get(), CapybaraRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BIRD_CAGE.get(), BirdCageRenderer::new);
    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CAPYBARA, CapybaraModel::createBodyLayer);
    }

    /** Adds the "parakeet on shoulder" layer to both player models (Steve and Alex arms). */
    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new ParakeetOnShoulderLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.<AbstractClientPlayer, PlayerRenderState>registerEntityModifier(PlayerRenderer.class, ParakeetOnShoulderLayer::storeShoulderParakeets);
    }
}
