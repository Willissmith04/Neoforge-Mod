package com.parakeetsandcapybaras.client.model;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import net.minecraft.client.model.geom.ModelLayerLocation;

/**
 * Names for our 3D model shapes. The parakeet re-uses the vanilla parrot model shape
 * ({@code ModelLayers.PARROT}), so only the capybara needs its own.
 */
public class ModModelLayers {
    public static final ModelLayerLocation CAPYBARA = new ModelLayerLocation(ParakeetsAndCapybaras.id("capybara"), "main");
}
