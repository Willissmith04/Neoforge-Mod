package com.parakeetsandcapybaras.registry;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Item tags are lists of items written in JSON files. Using tags lets you change which items
 * tame/breed our mobs without touching any Java code. See:
 * <ul>
 *   <li>data/parakeetsandcapybaras/tags/item/parakeet_food.json (seeds)</li>
 *   <li>data/parakeetsandcapybaras/tags/item/capybara_food.json (melons)</li>
 *   <li>data/parakeetsandcapybaras/tags/entity_type/fits_in_bird_cage.json (parrots and parakeets)</li>
 * </ul>
 */
public class ModTags {
    public static final TagKey<Item> PARAKEET_FOOD = TagKey.create(Registries.ITEM, ParakeetsAndCapybaras.id("parakeet_food"));
    public static final TagKey<Item> CAPYBARA_FOOD = TagKey.create(Registries.ITEM, ParakeetsAndCapybaras.id("capybara_food"));

    public static final TagKey<EntityType<?>> FITS_IN_BIRD_CAGE = TagKey.create(Registries.ENTITY_TYPE, ParakeetsAndCapybaras.id("fits_in_bird_cage"));
}
