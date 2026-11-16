package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Capybara;
import com.parakeetsandcapybaras.entity.Parakeet;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers our two mobs.
 *
 * <p>{@code sized(width, height)} is the hitbox in blocks. Babies automatically get a hitbox
 * half this size.</p>
 */
public class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(ParakeetsAndCapybaras.MOD_ID);

    // A bit smaller than a parrot: 80% of the parrot's size (see ParakeetRenderer.SIZE).
    public static final Supplier<EntityType<Parakeet>> PARAKEET = ENTITY_TYPES.registerEntityType(
            "parakeet",
            Parakeet::new,
            MobCategory.CREATURE,
            builder -> builder.sized(0.4F, 0.72F).eyeHeight(0.43F).passengerAttachments(0.37F).clientTrackingRange(8));

    // passengerAttachments = how high (in blocks) a baby capybara sits when riding on an adult's back.
    public static final Supplier<EntityType<Capybara>> CAPYBARA = ENTITY_TYPES.registerEntityType(
            "capybara",
            Capybara::new,
            MobCategory.CREATURE,
            builder -> builder.sized(0.9F, 1.0F).eyeHeight(0.85F).passengerAttachments(0.875F).clientTrackingRange(10));
}
