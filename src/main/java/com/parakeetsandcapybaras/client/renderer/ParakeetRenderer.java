package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Parakeet;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws parakeets. It re-uses Minecraft's {@link ParrotModel} (the shape and all of its
 * animations: flying, sitting, dancing...) with our own parakeet textures.
 */
public class ParakeetRenderer extends MobRenderer<Parakeet, ParakeetRenderState, ParrotModel> {
    /** Parakeets are drawn at 80% of a parrot's size. Change this number to make them bigger or smaller. */
    public static final float SIZE = 0.8F;
    private static final Map<Parakeet.Variant, ResourceLocation> TEXTURES = new EnumMap<>(Parakeet.Variant.class);

    static {
        for (Parakeet.Variant variant : Parakeet.Variant.values()) {
            TEXTURES.put(variant, ParakeetsAndCapybaras.id("textures/entity/parakeet/parakeet_" + variant.getSerializedName() + ".png"));
        }
    }

    public ParakeetRenderer(EntityRendererProvider.Context context) {
        super(context, new ParrotModel(context.bakeLayer(ModelLayers.PARROT)), 0.3F * SIZE);
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

        // Wing flapping (same maths as the parrot renderer).
        float flap = Mth.lerp(partialTick, parakeet.oFlap, parakeet.flap);
        float flapSpeed = Mth.lerp(partialTick, parakeet.oFlapSpeed, parakeet.flapSpeed);
        state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;

        // Pick the pose for the parrot model.
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

    /** Draw parakeets a bit smaller than parrots, and babies (chicks) at half of that. */
    @Override
    protected void scale(ParakeetRenderState state, PoseStack poseStack) {
        float size = state.isBaby ? SIZE * 0.5F : SIZE;
        poseStack.scale(size, size, size);
    }
}
