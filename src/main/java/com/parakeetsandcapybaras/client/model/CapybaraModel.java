package com.parakeetsandcapybaras.client.model;

import com.parakeetsandcapybaras.client.renderer.CapybaraRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The 3D model of the capybara: a big boxy body, a large head with a blunt nose, small ears and
 * four short legs.
 *
 * <p>Model coordinates are in pixels (16 pixels = 1 block). Y goes DOWN and y = 24 is the
 * ground.</p>
 */
public class CapybaraModel extends EntityModel<CapybaraRenderState> {
    private static final float HALF_PI = Mth.HALF_PI;
    private static final float LEG_SPREAD = 0.4F;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public CapybaraModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    /** Defines all the boxes. texOffs = where the box's skin starts on the 64x64 texture. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Body: 10 wide, 9 tall, 16 long.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.5F, -8.0F, 10.0F, 9.0F, 16.0F),
                PartPose.offset(0.0F, 14.5F, 0.0F));

        // Head: 7 wide, 7 tall, 8 long, with a long snout and two little ears attached to it.
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 25).addBox(-3.5F, -4.0F, -8.0F, 7.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 11.0F, -7.0F));
        // Long, blunt snout: 5 wide, 5 tall and 4 long, darkening towards the tip.
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(32, 25).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(52, 25).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-3.0F, -4.0F, -2.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(52, 29).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(3.0F, -4.0F, -2.0F));

        // Four short legs, 5 pixels tall.
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 41).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 19.0F, 5.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(3.0F, 19.0F, 5.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 19.0F, -5.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(3.0F, 19.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Called every frame to pose the model. */
    @Override
    public void setupAnim(CapybaraRenderState state) {
        super.setupAnim(state); // puts every part back in its default position first

        // Turn the head to look where the capybara is looking.
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

        if (state.isSitting) {
            // "Sploot": lie flat on the belly with the front legs stretched forward and the
            // back legs stretched backward, all spread a little outwards.
            this.body.y += 5.0F;   // belly touches the ground
            this.head.y += 5.5F;

            float legY = 22.5F; // legs lie flat on the ground
            this.rightFrontLeg.y = legY;
            this.leftFrontLeg.y = legY;
            this.rightHindLeg.y = legY;
            this.leftHindLeg.y = legY;

            this.rightFrontLeg.xRot = -HALF_PI;
            this.leftFrontLeg.xRot = -HALF_PI;
            this.rightHindLeg.xRot = HALF_PI;
            this.leftHindLeg.xRot = HALF_PI;

            this.rightFrontLeg.yRot = LEG_SPREAD;
            this.leftFrontLeg.yRot = -LEG_SPREAD;
            this.rightHindLeg.yRot = -LEG_SPREAD;
            this.leftHindLeg.yRot = LEG_SPREAD;
        } else {
            // Walking: swing the legs back and forth (same maths as cows and pigs).
            float swing = state.walkAnimationPos * 0.6662F;
            float amount = 1.4F * state.walkAnimationSpeed;
            this.rightHindLeg.xRot = Mth.cos(swing) * amount;
            this.leftHindLeg.xRot = Mth.cos(swing + Mth.PI) * amount;
            this.rightFrontLeg.xRot = Mth.cos(swing + Mth.PI) * amount;
            this.leftFrontLeg.xRot = Mth.cos(swing) * amount;
        }
    }
}
