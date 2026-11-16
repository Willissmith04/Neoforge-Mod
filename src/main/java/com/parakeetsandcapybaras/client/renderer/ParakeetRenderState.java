package com.parakeetsandcapybaras.client.renderer;

import com.parakeetsandcapybaras.entity.Parakeet;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;

/**
 * The parakeet uses the same poses as the parrot, so it extends the parrot's render state
 * (pose, wing flapping...) and adds the parakeet colour.
 */
public class ParakeetRenderState extends ParrotRenderState {
    public Parakeet.Variant parakeetVariant = Parakeet.Variant.GREEN;
}
