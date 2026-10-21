package dev.justromik.tfchorsechests;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HorseChests.ID)
public final class HorseChests {
    public static final String ID = "tfc_horse_chests";
    public static final String DATA = ID + ":cargo";
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredHolder<MenuType<?>, MenuType<CargoMenu>> CARGO_MENU = MENUS.register("cargo",
        () -> IMenuTypeExtension.create((IContainerFactory<CargoMenu>) CargoMenu::new));

    public HorseChests(IEventBus bus) {
        MENUS.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, HorseChests::interact);
        NeoForge.EVENT_BUS.addListener(HorseChests::drops);
    }

    public static boolean isTFCHorse(AbstractHorse horse) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(horse.getType()).toString().equals("tfc:horse");
    }

    public static boolean hasChest(AbstractHorse horse) {
        return horse.getPersistentData().getCompound(DATA).contains("Chest");
    }

    public static boolean mayUse(AbstractHorse horse, Player player) {
        return horse.isTamed() && (horse.getOwnerUUID() == null || horse.getOwnerUUID().equals(player.getUUID()));
    }

    public static HorseCargo cargo(AbstractHorse horse) {
        return ((CargoHolder) horse).tfchests$cargo();
    }

    public static boolean attach(AbstractHorse horse, Player player, ItemStack stack) {
        if (!isTFCHorse(horse) || horse.isBaby() || !mayUse(horse, player) || hasChest(horse)
            || !stack.is(Tags.Items.CHESTS_WOODEN)
            || stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext()) {
            return false;
        }
        if (!horse.level().isClientSide) {
            CompoundTag data = new CompoundTag();
            data.put("Chest", stack.copyWithCount(1).save(horse.registryAccess()));
            horse.getPersistentData().put(DATA, data);
            if (horse.getOwnerUUID() == null) horse.setOwnerUUID(player.getUUID());
            if (!player.getAbilities().instabuild) stack.shrink(1);
            horse.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
        }
        return true;
    }

    private static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractHorse horse) || !isTFCHorse(horse)) return;
        Player player = event.getEntity();
        if (attach(horse, player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(horse.level().isClientSide));
            event.setCanceled(true);
        } else if (!horse.level().isClientSide && hasChest(horse) && player.isSecondaryUseActive()
            && event.getItemStack().isEmpty() && mayUse(horse, player)) {
            open(horse, player);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static boolean open(AbstractHorse horse, Player player) {
        if (!isTFCHorse(horse) || !hasChest(horse)) return false;
        if (player instanceof ServerPlayer serverPlayer && mayUse(horse, player) && horse.isAlive()
            && player.distanceToSqr(horse) <= 64) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new CargoMenu(id, inventory, horse, cargo(horse)),
                Component.translatable("menu.tfc_horse_chests.cargo", horse.getDisplayName())),
                buffer -> buffer.writeVarInt(horse.getId()));
        }
        // Do not fall through to another inventory for an unauthorized player.
        return true;
    }

    private static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !isTFCHorse(horse) || !hasChest(horse)) return;
        HorseCargo cargo = cargo(horse);
        ItemStack chest = ItemStack.parseOptional(horse.registryAccess(), horse.getPersistentData().getCompound(DATA).getCompound("Chest"));
        if (!chest.isEmpty()) event.getDrops().add(new ItemEntity(horse.level(), horse.getX(), horse.getY(), horse.getZ(), chest));
        for (int i = 0; i < 15; i++) {
            ItemStack stack = cargo.removeItemNoUpdate(i);
            if (!stack.isEmpty()) event.getDrops().add(new ItemEntity(horse.level(), horse.getX(), horse.getY(), horse.getZ(), stack));
        }
        horse.getPersistentData().remove(DATA);
    }
}
