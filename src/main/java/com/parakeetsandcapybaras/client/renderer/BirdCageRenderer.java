package com.parakeetsandcapybaras.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.parakeetsandcapybaras.block.BirdCageBlock;
import com.parakeetsandcapybaras.block.BirdCageBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

/**
 * Draws the bird inside a placed bird cage. The cage itself is a normal block model
 * (assets/parakeetsandcapybaras/models/block/*_bird_cage.json); this only adds the bird.
 */
public class BirdCageRenderer implements BlockEntityRenderer<BirdCageBlockEntity> {
    /** Height of the top of the perch inside the cage, in blocks. */
    private static final float PERCH_HEIGHT = 4.5F / 16.0F;
    /** Birds are drawn a bit smaller so they fit nicely inside the cage. */
    private static final float BIRD_SCALE = 0.7F;

    private final EntityRenderDispatcher entityRenderer;

    public BirdCageRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(BirdCageBlockEntity cage, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Entity bird = cage.getOrCreateBirdForRendering();
        if (bird == null) {
            return;
        }

        // Make the bird face the same way as the cage and sit on the perch.
        float yaw = cage.getBlockState().getValue(BirdCageBlock.FACING).toYRot();
        bird.setYRot(yaw);
        bird.yRotO = yaw;
        bird.setXRot(0.0F);
        bird.xRotO = 0.0F;
        bird.setOnGround(true);
        if (bird instanceof LivingEntity living) {
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
        }
        if (bird instanceof TamableAnimal pet) {
            pet.setInSittingPose(true);
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, PERCH_HEIGHT, 0.5F);
        poseStack.scale(BIRD_SCALE, BIRD_SCALE, BIRD_SCALE);
        this.entityRenderer.setRenderShadow(false);
        this.entityRenderer.render(bird, 0.0, 0.0, 0.0, partialTick, poseStack, buffer, packedLight);
        this.entityRenderer.setRenderShadow(true);
        poseStack.popPose();
    }
}
