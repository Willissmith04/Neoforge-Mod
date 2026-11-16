package com.parakeetsandcapybaras.client.model;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Parakeet;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;

/**
 * The budgie model.
 *
 * <ul>
 *   <li>The shape comes from {@link ParakeetGeometry} (converted from the nm_budgie Blockbench
 *       models by tools/convert_budgie_models.py).</li>
 *   <li>The animations (idle, walk, bite, dance, preen, hurt, death) are JSON files in
 *       assets/parakeetsandcapybaras/neoforge/animations/entity/, loaded by NeoForge.</li>
 *   <li>Flying (flapping wings) and sitting are done in code below, because the Blockbench
 *       models don't have animations for those.</li>
 * </ul>
 */
public class ParakeetModel extends HierarchicalModel<Parakeet> {
    private static final AnimationHolder IDLE = animation("parakeet_idle");
    private static final AnimationHolder WALK = animation("parakeet_walk");
    private static final AnimationHolder BITE = animation("parakeet_bite");
    private static final AnimationHolder DANCE = animation("parakeet_dance");
    private static final AnimationHolder PREEN = animation("parakeet_preen");
    private static final AnimationHolder DEATH = animation("parakeet_death");
    private static final AnimationHolder[] HURT = {animation("parakeet_hurt_1"), animation("parakeet_hurt_2"), animation("parakeet_hurt_3")};

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    /** Keeps the idle animation going for parakeets sitting on a player's shoulder. */
    private final AnimationState shoulderIdleAnimationState = new AnimationState();

    public ParakeetModel(ModelPart root) {
        // entityCutout (instead of the default "NoCull") shows each side of the flat wing and
        // tail pieces with its own texture, the same way Blockbench shows them.
        super(RenderType::entityCutout);
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
        this.shoulderIdleAnimationState.start(0);
    }

    public static LayerDefinition createBodyLayer() {
        return ParakeetGeometry.createBodyLayer();
    }

    private static AnimationHolder animation(String name) {
        return AnimationLoader.INSTANCE.getAnimationHolder(ParakeetsAndCapybaras.id(name));
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** Poses the model for one frame. */
    @Override
    public void setupAnim(Parakeet parakeet, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose); // put every part back in its default position first

        boolean sitting = parakeet.isInSittingPose();
        this.leftLeg.visible = !sitting;
        this.rightLeg.visible = !sitting;

        // Look where the bird is looking.
        this.head.xRot += headPitch * Mth.DEG_TO_RAD;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD;

        // Gentle idle movement all the time.
        this.animate(parakeet.idleAnimationState, IDLE, ageInTicks);

        if (sitting) {
            this.body.y += 2.5F; // sink down onto the perch / ground
        } else if (parakeet.isFlying()) {
            // Tuck the legs back and flap the wings (same wing maths as the parrot).
            float partialTick = ageInTicks - parakeet.tickCount;
            float flap = Mth.lerp(partialTick, parakeet.oFlap, parakeet.flap);
            float flapSpeed = Mth.lerp(partialTick, parakeet.oFlapSpeed, parakeet.flapSpeed);
            float flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
            this.leftLeg.xRot += 0.7F;
            this.rightLeg.xRot += 0.7F;
            this.leftWing.zRot -= flapAngle;
            this.rightWing.zRot += flapAngle;
        } else {
            this.animateWalk(WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
        }

        // Tricks (right-click), dancing to a jukebox, getting hurt and dying.
        this.animate(parakeet.biteAnimationState, BITE, ageInTicks);
        this.animate(parakeet.danceAnimationState, DANCE, ageInTicks);
        this.animate(parakeet.preenAnimationState, PREEN, ageInTicks);
        this.animate(parakeet.hurtAnimationState, HURT[parakeet.hurtVariant], ageInTicks);
        this.animate(parakeet.deathAnimationState, DEATH, ageInTicks);
    }

    /** Poses the model for a parakeet riding on a player's shoulder (there is no entity then). */
    public void setupOnShoulder(float netHeadYaw, float headPitch, float ageInTicks) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.leftLeg.visible = true;
        this.rightLeg.visible = true;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        this.animate(this.shoulderIdleAnimationState, IDLE, ageInTicks);
    }
}
