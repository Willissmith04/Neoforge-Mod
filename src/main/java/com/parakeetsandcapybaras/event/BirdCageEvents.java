package com.parakeetsandcapybaras.event;

import com.parakeetsandcapybaras.ParakeetsAndCapybaras;
import com.parakeetsandcapybaras.item.BirdCageItem;
import com.parakeetsandcapybaras.registry.ModDataComponents;
import com.parakeetsandcapybaras.registry.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Catching birds: right-click a parrot or parakeet while holding an empty bird cage.
 *
 * <p>We use an event instead of the item's own code because the event runs <i>before</i> the
 * bird's own right-click code. Otherwise a tamed bird would just sit down instead of going into
 * the cage.</p>
 */
@EventBusSubscriber(modid = ParakeetsAndCapybaras.MOD_ID)
public class BirdCageEvents {

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack cage = event.getItemStack();
        Entity target = event.getTarget();
        if (!(cage.getItem() instanceof BirdCageItem) || !target.getType().is(ModTags.FITS_IN_BIRD_CAGE) || !(target instanceof Mob bird)) {
            return; // not a cage, or not a bird: do nothing special
        }

        // From here on we handle the click ourselves.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        Player player = event.getEntity();
        if (event.getLevel().isClientSide || !bird.isAlive()) {
            return; // the server does the real work
        }

        if (BirdCageItem.hasBird(cage)) {
            player.displayClientMessage(Component.translatable("message.parakeetsandcapybaras.bird_cage.full"), true);
            return;
        }
        if (bird instanceof TamableAnimal pet && pet.isTame() && !pet.isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("message.parakeetsandcapybaras.bird_cage.not_yours"), true);
            return;
        }

        if (bird.isLeashed()) {
            bird.dropLeash(true, true); // drops the lead as an item
        }

        // Save everything about the bird (type, colour, name, owner, health...) like a save file does.
        CompoundTag birdData = new CompoundTag();
        if (!bird.save(birdData)) {
            return;
        }
        birdData.remove("UUID"); // the released bird will get a fresh id

        // Put a cage holding the bird in the player's hand (or inventory if they hold a stack of cages).
        ItemStack filledCage = cage.copyWithCount(1);
        filledCage.set(ModDataComponents.CAGED_BIRD.get(), CustomData.of(birdData));
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(cage, player, filledCage, false));

        bird.discard();
        event.getLevel().playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }
}
