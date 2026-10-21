package dev.justromik.tfchorsechests;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class CargoScreen extends AbstractContainerScreen<CargoMenu> {
    public CargoScreen(CargoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 174;
        inventoryLabelY = 78;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, 0xFFFFFFFF);
        graphics.fill(leftPos, topPos, leftPos + 2, topPos + imageHeight, 0xFFFFFFFF);
        graphics.fill(leftPos + imageWidth - 2, topPos + 2, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        graphics.fill(leftPos + 2, topPos + imageHeight - 2, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
            graphics.fill(x - 1, y - 1, x + 16, y + 16, 0xFF373737);
            graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
