package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.client.model.ModModelLayers;
import com.parakeetsandcapybaras.client.model.ParakeetModel;
import com.parakeetsandcapybaras.entity.Parakeet;
import com.parakeetsandcapybaras.registry.ModEntities;
import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/**
 * Draws parakeets sitting on a player's shoulders. Based on Minecraft's ParrotOnShoulderLayer,
 * which only draws parrots. A bird on a shoulder isn't a real entity any more: it's saved as data
 * on the player, so we read its colour from that data.
 */
public class ParakeetOnShoulderLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
    private final ParakeetModel model;

    public ParakeetOnShoulderLayer(RenderLayerParent<T, PlayerModel<T>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new ParakeetModel(modelSet.bakeLayer(ModModelLayers.PARAKEET));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.renderOnShoulder(poseStack, buffer, packedLight, player, player.getShoulderEntityLeft(), true, ageInTicks, netHeadYaw, headPitch);
        this.renderOnShoulder(poseStack, buffer, packedLight, player, player.getShoulderEntityRight(), false, ageInTicks, netHeadYaw, headPitch);
    }

    @Nullable
    private static Parakeet.Variant getParakeetVariant(CompoundTag shoulderEntity) {
        boolean isParakeet = EntityType.byString(shoulderEntity.getString("id"))
                .filter(type -> type == ModEntities.PARAKEET.get())
                .isPresent();
        return isParakeet ? Parakeet.Variant.byId(shoulderEntity.getInt("Variant")) : null;
    }

    private void renderOnShoulder(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player, CompoundTag shoulderEntity,
                                  boolean leftShoulder, float ageInTicks, float netHeadYaw, float headPitch) {
        Parakeet.Variant variant = getParakeetVariant(shoulderEntity);
        if (variant == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(leftShoulder ? 0.4F : -0.4F, player.isCrouching() ? -1.3F : -1.5F, 0.0F);
        // Resize around the bird's feet (1.5 blocks below the model's origin) so it stays on the shoulder.
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(ParakeetRenderer.SIZE, ParakeetRenderer.SIZE, ParakeetRenderer.SIZE);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        this.model.setupOnShoulder(netHeadYaw, headPitch, ageInTicks);
        this.model.renderToBuffer(poseStack, buffer.getBuffer(this.model.renderType(ParakeetRenderer.getTexture(variant))),
                packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
