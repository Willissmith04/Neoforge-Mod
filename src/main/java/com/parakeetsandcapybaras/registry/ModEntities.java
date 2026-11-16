package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.entity.Capybara;
import com.parakeetsandcapybaras.entity.Parakeet;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
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
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ParakeetsAndCapybaras.MOD_ID);

    // A bit smaller than a parrot (the drawn size is set by ParakeetRenderer.SIZE).
    public static final Supplier<EntityType<Parakeet>> PARAKEET = ENTITY_TYPES.register("parakeet",
            () -> EntityType.Builder.of(Parakeet::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.6F).eyeHeight(0.45F).passengerAttachments(0.55F).clientTrackingRange(8)
                    .build("parakeet"));

    // passengerAttachments = how high (in blocks) a baby capybara sits when riding on an adult's back.
    public static final Supplier<EntityType<Capybara>> CAPYBARA = ENTITY_TYPES.register("capybara",
            () -> EntityType.Builder.of(Capybara::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.1F).eyeHeight(0.9F).passengerAttachments(0.875F).clientTrackingRange(10)
                    .build("capybara"));
}
