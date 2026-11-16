package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Parakeet;
import com.parakeetsandcapybaras.registry.ModEntities;
import javax.annotation.Nullable;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;

/**
 * Draws parakeets sitting on a player's shoulders. This is a copy of Minecraft's
 * ParrotOnShoulderLayer, changed to draw parakeets (Minecraft's own layer only draws parrots).
 *
 * <p>Since Minecraft 1.21.2, renderers only get a "render state" instead of the player itself,
 * so we use NeoForge "render data": {@link #storeShoulderParakeets} copies the parakeet colours
 * from the player into the render state every frame, and {@link #render} reads them back.</p>
 */
public class ParakeetOnShoulderLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    public static final ContextKey<Parakeet.Variant> LEFT_SHOULDER = new ContextKey<>(ParakeetsAndCapybaras.id("parakeet_on_left_shoulder"));
    public static final ContextKey<Parakeet.Variant> RIGHT_SHOULDER = new ContextKey<>(ParakeetsAndCapybaras.id("parakeet_on_right_shoulder"));

    private final ParrotModel model;
    private final ParrotRenderState parakeetState = new ParrotRenderState();

    public ParakeetOnShoulderLayer(RenderLayerParent<PlayerRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new ParrotModel(modelSet.bakeLayer(ModelLayers.PARROT));
        this.parakeetState.pose = ParrotModel.Pose.ON_SHOULDER;
    }

    /** Registered in ClientModEvents. Runs every frame for every player that is drawn. */
    public static void storeShoulderParakeets(AbstractClientPlayer player, PlayerRenderState state) {
        state.setRenderData(LEFT_SHOULDER, getParakeetVariant(player.getShoulderEntityLeft()));
        state.setRenderData(RIGHT_SHOULDER, getParakeetVariant(player.getShoulderEntityRight()));
    }

    @Nullable
    private static Parakeet.Variant getParakeetVariant(CompoundTag shoulderEntity) {
        boolean isParakeet = EntityType.byString(shoulderEntity.getString("id"))
                .filter(type -> type == ModEntities.PARAKEET.get())
                .isPresent();
        return isParakeet ? Parakeet.Variant.byId(shoulderEntity.getInt("Variant")) : null;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, PlayerRenderState state, float yRot, float xRot) {
        Parakeet.Variant left = state.getRenderData(LEFT_SHOULDER);
        if (left != null) {
            this.renderOnShoulder(poseStack, buffer, packedLight, state, left, yRot, xRot, true);
        }
        Parakeet.Variant right = state.getRenderData(RIGHT_SHOULDER);
        if (right != null) {
            this.renderOnShoulder(poseStack, buffer, packedLight, state, right, yRot, xRot, false);
        }
    }

    private void renderOnShoulder(PoseStack poseStack, MultiBufferSource buffer, int packedLight, PlayerRenderState state,
                                  Parakeet.Variant variant, float yRot, float xRot, boolean leftShoulder) {
        poseStack.pushPose();
        poseStack.translate(leftShoulder ? 0.4F : -0.4F, state.isCrouching ? -1.3F : -1.5F, 0.0F);
        this.parakeetState.ageInTicks = state.ageInTicks;
        this.parakeetState.walkAnimationPos = state.walkAnimationPos;
        this.parakeetState.walkAnimationSpeed = state.walkAnimationSpeed;
        this.parakeetState.yRot = yRot;
        this.parakeetState.xRot = xRot;
        this.model.setupAnim(this.parakeetState);
        this.model.renderToBuffer(poseStack, buffer.getBuffer(this.model.renderType(ParakeetRenderer.getTexture(variant))),
                packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
