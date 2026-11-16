package com.parakeetsandcapybaras.client.model;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Capybara;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;

/**
 * The capybara model.
 *
 * <ul>
 *   <li>The shape comes from {@link CapybaraGeometry} (converted from nm_capybara.bbmodel by
 *       tools/convert_capybara_model.py).</li>
 *   <li>The animations (idle, walk, sniff, death) are JSON files in
 *       assets/parakeetsandcapybaras/neoforge/animations/entity/, loaded by NeoForge.</li>
 *   <li>The sitting pose ("sploot": lying flat with the legs stretched out) is done in code
 *       below, because the Blockbench model has no sitting animation.</li>
 * </ul>
 */
public class CapybaraModel extends HierarchicalModel<Capybara> {
    private static final AnimationHolder IDLE = animation("capybara_idle");
    private static final AnimationHolder WALK = animation("capybara_walk");
    private static final AnimationHolder SNIFF = animation("capybara_sniff");
    private static final AnimationHolder DEATH = animation("capybara_death");

    /** How far the body sinks when lying down, in pixels (about the length of the legs). */
    private static final float SIT_DROP = 5.0F;
    private static final float LEG_SPREAD = 0.4F;

    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart head;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public CapybaraModel(ModelPart root) {
        this.root = root;
        this.base = root.getChild("base");
        ModelPart base2 = this.base.getChild("base_2");
        this.head = base2.getChild("torso").getChild("head");
        this.rightFrontLeg = base2.getChild("right_front_leg");
        this.leftFrontLeg = base2.getChild("left_front_leg");
        this.rightHindLeg = base2.getChild("right_hind_leg");
        this.leftHindLeg = base2.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return CapybaraGeometry.createBodyLayer();
    }

    private static AnimationHolder animation(String name) {
        return AnimationLoader.INSTANCE.getAnimationHolder(ParakeetsAndCapybaras.id(name));
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** Called every frame to pose the model. */
    @Override
    public void setupAnim(Capybara capybara, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose); // put every part back in its default position first

        // Turn the head to look where the capybara is looking.
        this.head.xRot += headPitch * Mth.DEG_TO_RAD;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD;

        // Gentle breathing / tail movement all the time.
        this.animate(capybara.idleAnimationState, IDLE, ageInTicks);

        if (capybara.isInSittingPose()) {
            // "Sploot": lie flat on the belly with the front legs stretched forward and the
            // back legs stretched backward, all spread a little outwards.
            this.base.y += SIT_DROP;
            for (ModelPart leg : new ModelPart[]{this.rightFrontLeg, this.leftFrontLeg, this.rightHindLeg, this.leftHindLeg}) {
                leg.y -= 0.75F; // so the flat legs rest on the ground instead of sinking into it
            }
            this.rightFrontLeg.xRot = -Mth.HALF_PI;
            this.leftFrontLeg.xRot = -Mth.HALF_PI;
            this.rightHindLeg.xRot = Mth.HALF_PI;
            this.leftHindLeg.xRot = Mth.HALF_PI;
            this.rightFrontLeg.yRot = LEG_SPREAD;
            this.leftFrontLeg.yRot = -LEG_SPREAD;
            this.rightHindLeg.yRot = -LEG_SPREAD;
            this.leftHindLeg.yRot = LEG_SPREAD;
        } else {
            this.animateWalk(WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
        }

        this.animate(capybara.sniffAnimationState, SNIFF, ageInTicks);
        this.animate(capybara.deathAnimationState, DEATH, ageInTicks);
    }
}
