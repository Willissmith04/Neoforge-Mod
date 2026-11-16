package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.client.model.CapybaraModel;
import com.parakeetsandcapybaras.client.model.ModModelLayers;
import com.parakeetsandcapybaras.entity.Capybara;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Draws capybaras using {@link CapybaraModel} and the capybara texture. */
public class CapybaraRenderer extends MobRenderer<Capybara, CapybaraModel> {
    private static final ResourceLocation TEXTURE = ParakeetsAndCapybaras.id("textures/entity/capybara/capybara.png");

    public CapybaraRenderer(EntityRendererProvider.Context context) {
        super(context, new CapybaraModel(context.bakeLayer(ModModelLayers.CAPYBARA)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(Capybara capybara) {
        return TEXTURE;
    }

    /** Don't tip over sideways when dying: the capybara has its own death animation. */
    @Override
    protected float getFlipDegrees(Capybara capybara) {
        return 0.0F;
    }

    /** Babies are drawn at half size. */
    @Override
    protected void scale(Capybara capybara, PoseStack poseStack, float partialTick) {
        if (capybara.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
