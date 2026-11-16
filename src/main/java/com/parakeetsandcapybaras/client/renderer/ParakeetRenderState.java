package com.parakeetsandcapybaras.client.renderer;

import com.parakeetsandcapybaras.entity.Parakeet;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * Everything the parakeet model needs to draw one frame. It extends the parrot's render state
 * (pose, wing flapping...) because the parakeet uses the same poses, and adds the colour and the
 * animations that are playing.
 */
public class ParakeetRenderState extends ParrotRenderState {
    public Parakeet.Variant parakeetVariant = Parakeet.Variant.GREEN;

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState biteAnimationState = new AnimationState();
    public final AnimationState danceAnimationState = new AnimationState();
    public final AnimationState preenAnimationState = new AnimationState();
    public final AnimationState hurtAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    /** Which of the 3 hurt animations to play (0, 1 or 2). */
    public int hurtVariant;
}
