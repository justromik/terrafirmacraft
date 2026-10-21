package dev.justromik.tfchorsechests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/** The entity owns this container. Every edit is immediately written to its persistent NBT. */
public final class HorseCargo extends SimpleContainer {
    private final AbstractHorse horse;
    private boolean loading = true;

    public HorseCargo(AbstractHorse horse) {
        super(15);
        this.horse = horse;
        ContainerHelper.loadAllItems(horse.getPersistentData().getCompound(HorseChests.DATA), getItems(), horse.registryAccess());
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading && !horse.level().isClientSide) {
            CompoundTag data = horse.getPersistentData().getCompound(HorseChests.DATA);
            ContainerHelper.saveAllItems(data, getItems(), horse.registryAccess());
            horse.getPersistentData().put(HorseChests.DATA, data);
        }
    }
}
