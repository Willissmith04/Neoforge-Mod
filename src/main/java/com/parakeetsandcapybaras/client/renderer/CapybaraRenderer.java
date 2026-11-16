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
public class CapybaraRenderer extends MobRenderer<Capybara, CapybaraRenderState, CapybaraModel> {
    private static final ResourceLocation TEXTURE = ParakeetsAndCapybaras.id("textures/entity/capybara/capybara.png");

    public CapybaraRenderer(EntityRendererProvider.Context context) {
        super(context, new CapybaraModel(context.bakeLayer(ModModelLayers.CAPYBARA)), 0.6F);
    }

    @Override
    public CapybaraRenderState createRenderState() {
        return new CapybaraRenderState();
    }

    /** Copies what we need from the entity into the render state. */
    @Override
    public void extractRenderState(Capybara capybara, CapybaraRenderState state, float partialTick) {
        super.extractRenderState(capybara, state, partialTick);
        state.isSitting = capybara.isInSittingPose();
        state.idleAnimationState.copyFrom(capybara.idleAnimationState);
        state.sniffAnimationState.copyFrom(capybara.sniffAnimationState);
        state.deathAnimationState.copyFrom(capybara.deathAnimationState);
    }

    @Override
    public ResourceLocation getTextureLocation(CapybaraRenderState state) {
        return TEXTURE;
    }

    /** Don't tip over sideways when dying: the capybara has its own death animation. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    /** Babies are drawn at half size. */
    @Override
    protected void scale(CapybaraRenderState state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
