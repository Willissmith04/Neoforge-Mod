package com.parakeetsandcapybaras.item;

import com.parakeetsandcapybaras.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * The bird cage item. It places a bird cage block.
 *
 * <p>Catching a bird with it (right-clicking a parrot or parakeet) is handled in
 * {@link com.parakeetsandcapybaras.event.BirdCageEvents}. The caught bird is stored in the
 * {@link ModDataComponents#CAGED_BIRD} component and moved into the block when placed.</p>
 */
public class BirdCageItem extends BlockItem {
    public BirdCageItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean hasBird(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.CAGED_BIRD.get());
        return data != null && !data.isEmpty();
    }

    /** Shows which bird is in the cage, e.g. "Contains: Parakeet" or "Contains: Kiwi (Parakeet)". */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!hasBird(stack)) {
            tooltip.add(Component.translatable("item.parakeetsandcapybaras.bird_cage.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        CompoundTag bird = stack.get(ModDataComponents.CAGED_BIRD.get()).copyTag();
        Component birdType = EntityType.byString(bird.getString("id"))
                .map(EntityType::getDescription)
                .orElse(Component.literal("?"));

        Component description = birdType;
        if (bird.contains("CustomName", 8) && context.registries() != null) {
            Component customName = Component.Serializer.fromJson(bird.getString("CustomName"), context.registries());
            if (customName != null) {
                description = Component.translatable("item.parakeetsandcapybaras.bird_cage.named", customName, birdType);
            }
        }
        tooltip.add(Component.translatable("item.parakeetsandcapybaras.bird_cage.contains", description).withStyle(ChatFormatting.GRAY));
    }
}
