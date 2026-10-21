package dev.justromik.tfchorsechests;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CargoMenu extends AbstractContainerMenu {
    private final AbstractHorse horse;
    private final Container cargo;

    public CargoMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, inventory.player.level().getEntity(buffer.readVarInt()) instanceof AbstractHorse h ? h : null,
            new SimpleContainer(15));
    }

    public CargoMenu(int id, Inventory inventory, AbstractHorse horse, Container cargo) {
        super(HorseChests.CARGO_MENU.get(), id);
        checkContainerSize(cargo, 15);
        this.horse = horse;
        this.cargo = cargo;
        cargo.startOpen(inventory.player);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) addSlot(new Slot(cargo, row * 5 + col, 44 + col * 18, 20 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 90 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 148));
    }

    @Override
    public boolean stillValid(Player player) {
        return horse != null && horse.isAlive() && HorseChests.hasChest(horse) && HorseChests.mayUse(horse, player)
            && player.distanceToSqr(horse) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 15) {
            if (!moveItemStackTo(stack, 15, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 15, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        cargo.stopOpen(player);
        cargo.setChanged();
    }
}
