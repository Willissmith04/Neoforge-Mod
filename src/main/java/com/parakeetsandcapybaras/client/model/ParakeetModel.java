package com.parakeetsandcapybaras.client.model;

import com.parakeetsandcapybaras.client.renderer.ParakeetRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The chunky 3D model of the parakeet (budgie):
 * <ul>
 *   <li>a tall body that leans forward a little,</li>
 *   <li>a boxy head with a flat "cap" that sticks out over the face,</li>
 *   <li>a blue band above the beak (the cere) and a beak hanging below it,</li>
 *   <li>two big wing panels on the sides that reach down past the body,</li>
 *   <li>a short tail and two little legs.</li>
 * </ul>
 * It uses the same poses as the vanilla parrot (standing, flying, sitting, dancing and riding on
 * a shoulder), so all of the parakeet's behaviour from Parakeet.java still shows up here.
 *
 * <p>Model coordinates are in pixels (16 pixels = 1 block). Y goes DOWN and y = 24 is the
 * ground. The texture is 64x32; see tools/generate_textures.py for where each box is painted.</p>
 */
public class ParakeetModel extends EntityModel<ParakeetRenderState> {
    private static final float BODY_LEAN = 0.3F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ParakeetModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Body: 6 wide, 7 tall, 5 deep. Its pivot is at the hips so it can lean forward.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -7.0F, -2.5F, 6.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 1.0F, BODY_LEAN, 0.0F, 0.0F));

        // Wings: tall checkered panels on each side of the body, reaching down past it.
        CubeListBuilder wing = CubeListBuilder.create().texOffs(22, 0).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 8.0F, 4.0F);
        body.addOrReplaceChild("left_wing", wing, PartPose.offsetAndRotation(3.5F, -6.5F, 0.0F, 0.15F, 0.0F, 0.0F));
        body.addOrReplaceChild("right_wing", wing, PartPose.offsetAndRotation(-3.5F, -6.5F, 0.0F, 0.15F, 0.0F, 0.0F));

        // Tail: a short flat tail pointing backwards and down.
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(24, 12).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 2.5F, -0.6F, 0.0F, 0.0F));

        // Head: a big 5x5x5 box, plus a flat cap on top that sticks out 1 pixel at the front.
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(32, 0).addBox(-2.5F, -5.0F, -3.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 15.5F, -1.5F));
        head.addOrReplaceChild("cap",
                CubeListBuilder.create().texOffs(0, 12).addBox(-3.0F, -6.0F, -4.5F, 6.0F, 1.0F, 6.0F),
                PartPose.ZERO);
        // The blue band above the beak (called the "cere").
        head.addOrReplaceChild("cere",
                CubeListBuilder.create().texOffs(38, 12).addBox(-1.5F, -5.0F, -4.5F, 3.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        // The beak, hanging down below the cere.
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(38, 15).addBox(-1.0F, -4.0F, -4.5F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        // Legs: little grey legs with the toes pointing forwards.
        CubeListBuilder leg = CubeListBuilder.create().texOffs(46, 12).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F);
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.2F, 22.0F, 0.5F));
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.2F, 22.0F, 0.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Poses the model for one frame. */
    @Override
    public void setupAnim(ParakeetRenderState state) {
        super.setupAnim(state); // puts every part back in its default position first

        ParrotModel.Pose pose = state.pose;
        boolean sitting = pose == ParrotModel.Pose.SITTING;
        this.leftLeg.visible = !sitting;
        this.rightLeg.visible = !sitting;

        // Look where the bird is looking.
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

        switch (pose) {
            case SITTING -> {
                // Sink down onto the ground or perch.
                this.body.y += 2.0F;
                this.head.y += 2.0F;
            }
            case PARTY -> {
                // Dancing: bob the whole bird from side to side and tilt the head.
                float sideways = Mth.cos(state.ageInTicks);
                float upDown = Mth.sin(state.ageInTicks);
                this.head.x += sideways;
                this.head.y += upDown;
                this.head.xRot = 0.0F;
                this.head.yRot = 0.0F;
                this.head.zRot = Mth.sin(state.ageInTicks) * 0.4F;
                this.body.x += sideways;
                this.body.y += upDown;
                this.leftLeg.zRot = -Mth.PI / 9.0F;
                this.rightLeg.zRot = Mth.PI / 9.0F;
                this.flapWings(state.flapAngle);
            }
            case FLYING -> {
                // Tuck the legs back and flap.
                this.leftLeg.xRot = 0.7F;
                this.rightLeg.xRot = 0.7F;
                this.bob(state.flapAngle * 0.3F);
                this.flapWings(state.flapAngle);
            }
            case STANDING -> {
                // Walking: swing the legs and wiggle the tail.
                float swing = state.walkAnimationPos * 0.6662F;
                this.leftLeg.xRot = Mth.cos(swing) * 1.4F * state.walkAnimationSpeed;
                this.rightLeg.xRot = Mth.cos(swing + Mth.PI) * 1.4F * state.walkAnimationSpeed;
                this.tail.xRot += Mth.cos(swing) * 0.3F * state.walkAnimationSpeed;
                this.bob(state.flapAngle * 0.3F);
                this.flapWings(state.flapAngle);
            }
            case ON_SHOULDER -> {
                // Riding on a player's shoulder: just sit still and look around.
            }
        }
    }

    /** Moves the whole bird up and down a little while it flaps. */
    private void bob(float amount) {
        this.body.y += amount;
        this.head.y += amount;
        this.leftLeg.y += amount;
        this.rightLeg.y += amount;
    }

    /** Opens the wings outwards. flapAngle is 0 when resting. */
    private void flapWings(float flapAngle) {
        this.leftWing.zRot = -flapAngle;
        this.rightWing.zRot = flapAngle;
    }
}
