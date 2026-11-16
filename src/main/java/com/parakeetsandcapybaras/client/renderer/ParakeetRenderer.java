package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Parakeet;
import java.util.EnumMap;
import java.util.Map;
import com.parakeetsandcapybaras.client.model.ModModelLayers;
import com.parakeetsandcapybaras.client.model.ParakeetModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws parakeets using our own chunky {@link ParakeetModel} and the parakeet textures. It uses
 * the same poses as the vanilla parrot ({@link ParrotModel.Pose}: flying, sitting, dancing...).
 */
public class ParakeetRenderer extends MobRenderer<Parakeet, ParakeetRenderState, ParakeetModel> {
    /**
     * How big parakeets are drawn. At 1.0 the budgie model is a little smaller than a vanilla
     * parrot. Change this number to make them bigger or smaller.
     */
    public static final float SIZE = 1.0F;
    private static final Map<Parakeet.Variant, ResourceLocation> TEXTURES = new EnumMap<>(Parakeet.Variant.class);

    static {
        for (Parakeet.Variant variant : Parakeet.Variant.values()) {
            TEXTURES.put(variant, ParakeetsAndCapybaras.id("textures/entity/parakeet/parakeet_" + variant.getSerializedName() + ".png"));
        }
    }

    public ParakeetRenderer(EntityRendererProvider.Context context) {
        super(context, new ParakeetModel(context.bakeLayer(ModModelLayers.PARAKEET)), 0.3F * SIZE);
    }

    public static ResourceLocation getTexture(Parakeet.Variant variant) {
        return TEXTURES.get(variant);
    }

    @Override
    public ParakeetRenderState createRenderState() {
        return new ParakeetRenderState();
    }

    @Override
    public void extractRenderState(Parakeet parakeet, ParakeetRenderState state, float partialTick) {
        super.extractRenderState(parakeet, state, partialTick);
        state.parakeetVariant = parakeet.getVariant();
        state.idleAnimationState.copyFrom(parakeet.idleAnimationState);
        state.biteAnimationState.copyFrom(parakeet.biteAnimationState);
        state.danceAnimationState.copyFrom(parakeet.danceAnimationState);
        state.preenAnimationState.copyFrom(parakeet.preenAnimationState);
        state.hurtAnimationState.copyFrom(parakeet.hurtAnimationState);
        state.deathAnimationState.copyFrom(parakeet.deathAnimationState);
        state.hurtVariant = parakeet.hurtVariant;

        // Wing flapping (same maths as the parrot renderer).
        float flap = Mth.lerp(partialTick, parakeet.oFlap, parakeet.flap);
        float flapSpeed = Mth.lerp(partialTick, parakeet.oFlapSpeed, parakeet.flapSpeed);
        state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;

        // Pick the pose (the same poses the parrot uses).
        if (parakeet.isPartyParakeet()) {
            state.pose = ParrotModel.Pose.PARTY;      // dancing to a jukebox!
        } else if (parakeet.isInSittingPose()) {
            state.pose = ParrotModel.Pose.SITTING;
        } else if (parakeet.isFlying()) {
            state.pose = ParrotModel.Pose.FLYING;
        } else {
            state.pose = ParrotModel.Pose.STANDING;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ParakeetRenderState state) {
        return getTexture(state.parakeetVariant);
    }

    /** Don't tip over sideways when dying: the budgie has its own death animation. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    /** Draw parakeets a bit smaller than parrots, and babies (chicks) at half of that. */
    @Override
    protected void scale(ParakeetRenderState state, PoseStack poseStack) {
        float size = state.isBaby ? SIZE * 0.5F : SIZE;
        poseStack.scale(size, size, size);
    }
}
