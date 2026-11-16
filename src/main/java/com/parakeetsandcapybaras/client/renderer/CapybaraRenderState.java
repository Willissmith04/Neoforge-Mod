package com.parakeetsandcapybaras.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Everything the capybara model needs to know to draw one frame. */
public class CapybaraRenderState extends LivingEntityRenderState {
    public boolean isSitting;

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sniffAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
}
