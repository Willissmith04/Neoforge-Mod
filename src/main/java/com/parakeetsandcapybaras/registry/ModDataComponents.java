package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components are extra information attached to an item stack.
 *
 * <p>{@link #CAGED_BIRD} holds the saved bird (all of its NBT data: type, colour, owner, name...)
 * inside a bird cage item. It is saved to disk ("persistent") and sent to players
 * ("networkSynchronized") so the tooltip can show which bird is inside.</p>
 */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ParakeetsAndCapybaras.MOD_ID);

    public static final Supplier<DataComponentType<CustomData>> CAGED_BIRD = DATA_COMPONENTS.registerComponentType(
            "caged_bird",
            builder -> builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));
}
