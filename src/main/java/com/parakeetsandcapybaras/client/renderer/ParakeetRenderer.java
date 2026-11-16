package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.client.model.ModModelLayers;
import com.parakeetsandcapybaras.client.model.ParakeetModel;
import com.parakeetsandcapybaras.entity.Parakeet;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Draws parakeets using {@link ParakeetModel} and the texture for each colour. */
public class ParakeetRenderer extends MobRenderer<Parakeet, ParakeetModel> {
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
    public ResourceLocation getTextureLocation(Parakeet parakeet) {
        return getTexture(parakeet.getVariant());
    }

    /** Don't tip over sideways when dying: the budgie has its own death animation. */
    @Override
    protected float getFlipDegrees(Parakeet parakeet) {
        return 0.0F;
    }

    /** Babies (chicks) are drawn at half size. */
    @Override
    protected void scale(Parakeet parakeet, PoseStack poseStack, float partialTick) {
        float size = parakeet.isBaby() ? SIZE * 0.5F : SIZE;
        poseStack.scale(size, size, size);
    }
}
