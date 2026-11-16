package com.parakeetsandcapybaras.block;

import com.parakeetsandcapybaras.registry.ModBlockEntities;
import com.parakeetsandcapybaras.registry.ModDataComponents;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the bird that lives inside a placed bird cage.
 *
 * <p>The bird is saved as NBT (exactly like when Minecraft saves a mob to disk), so everything
 * about it is kept: its type, colour, name, owner, health...</p>
 *
 * <p>When the cage is placed, the bird is copied from the item (see
 * {@link #applyImplicitComponents}). When the cage is broken, the bird is copied back onto the
 * dropped item (see {@link #collectImplicitComponents} and the block's loot table).</p>
 */
public class BirdCageBlockEntity extends BlockEntity {
    private CompoundTag birdData = new CompoundTag();

    /** Client only: a copy of the bird that the renderer draws inside the cage. Never added to the world. */
    @Nullable
    private Entity cachedBird;

    public BirdCageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIRD_CAGE.get(), pos, state);
    }

    public boolean hasBird() {
        return !this.birdData.isEmpty();
    }

    public CompoundTag getBirdData() {
        return this.birdData;
    }

    public void setBirdData(CompoundTag data) {
        this.birdData = data.copy();
        this.cachedBird = null;
        this.setChanged();
        if (this.level != null) {
            // Tell nearby players that the cage changed so they see the bird appear/disappear.
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Used by the renderer: creates (once) a copy of the bird to draw. */
    @Nullable
    public Entity getOrCreateBirdForRendering() {
        if (this.cachedBird == null && this.hasBird() && this.level != null) {
            Optional<Entity> bird = EntityType.create(this.birdData, this.level, EntitySpawnReason.LOAD);
            this.cachedBird = bird.orElse(null);
        }
        return this.cachedBird;
    }

    // ---------- saving and loading ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.hasBird()) {
            tag.put("Bird", this.birdData);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.birdData = tag.getCompound("Bird");
        this.cachedBird = null;
    }

    // ---------- sending the bird to players (so they can see it) ----------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Always send the "Bird" entry, even when it is empty. (An update with no data at all is
        // ignored by the game, so players would keep seeing a bird that was already let out.)
        CompoundTag tag = new CompoundTag();
        tag.put("Bird", this.birdData.copy());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---------- moving the bird between the item and the block ----------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.birdData = input.getOrDefault(ModDataComponents.CAGED_BIRD.get(), CustomData.EMPTY).copyTag();
        this.cachedBird = null;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.hasBird()) {
            components.set(ModDataComponents.CAGED_BIRD.get(), CustomData.of(this.birdData));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Bird");
    }
}
