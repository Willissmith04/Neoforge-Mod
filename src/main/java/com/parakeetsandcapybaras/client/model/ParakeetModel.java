package com.parakeetsandcapybaras.client.model;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.client.renderer.ParakeetRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
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
public class ParakeetModel extends EntityModel<ParakeetRenderState> {
    private static final AnimationHolder IDLE = animation("parakeet_idle");
    private static final AnimationHolder WALK = animation("parakeet_walk");
    private static final AnimationHolder BITE = animation("parakeet_bite");
    private static final AnimationHolder DANCE = animation("parakeet_dance");
    private static final AnimationHolder PREEN = animation("parakeet_preen");
    private static final AnimationHolder DEATH = animation("parakeet_death");
    private static final AnimationHolder[] HURT = {animation("parakeet_hurt_1"), animation("parakeet_hurt_2"), animation("parakeet_hurt_3")};

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ParakeetModel(ModelPart root) {
        // entityCutout (instead of the default "NoCull") shows each side of the flat wing and
        // tail pieces with its own texture, the same way Blockbench shows them.
        super(root, RenderType::entityCutout);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return ParakeetGeometry.createBodyLayer();
    }

    private static AnimationHolder animation(String name) {
        return AnimationLoader.INSTANCE.getAnimationHolder(ParakeetsAndCapybaras.id(name));
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
        this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot += state.yRot * Mth.DEG_TO_RAD;

        // Gentle idle movement all the time.
        this.animate(state.idleAnimationState, IDLE, state.ageInTicks);

        switch (pose) {
            case STANDING -> this.animateWalk(WALK, state.walkAnimationPos, state.walkAnimationSpeed, 2.0F, 2.5F);
            case FLYING -> {
                // Tuck the legs back and flap the wings.
                this.leftLeg.xRot += 0.7F;
                this.rightLeg.xRot += 0.7F;
                this.leftWing.zRot -= state.flapAngle;
                this.rightWing.zRot += state.flapAngle;
            }
            case SITTING -> this.body.y += 2.5F; // sink down onto the perch / ground
            default -> {
            }
        }

        // Tricks (right-click), dancing to a jukebox, getting hurt and dying.
        this.animate(state.biteAnimationState, BITE, state.ageInTicks);
        this.animate(state.danceAnimationState, DANCE, state.ageInTicks);
        this.animate(state.preenAnimationState, PREEN, state.ageInTicks);
        this.animate(state.hurtAnimationState, HURT[state.hurtVariant], state.ageInTicks);
        this.animate(state.deathAnimationState, DEATH, state.ageInTicks);
    }
}
