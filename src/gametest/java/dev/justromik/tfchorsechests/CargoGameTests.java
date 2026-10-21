package dev.justromik.tfchorsechests;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = HorseChests.ID, bus = EventBusSubscriber.Bus.MOD)
public final class CargoGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) { event.register(CargoGameTests.class); }

    private static AbstractHorse horse(GameTestHelper helper) throws Exception {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tfc:horse"));
        AbstractHorse horse = (AbstractHorse) type.create(helper.getLevel());
        if (horse == null) throw new AssertionError("TFC horse missing");
        horse.getClass().getMethod("setFamiliarity", float.class).invoke(horse, 1F);
        horse.getClass().getMethod("setBirthTickToALongTimeAgo").invoke(horse);
        horse.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)).getCenter());
        helper.getLevel().addFreshEntity(horse);
        return horse;
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        return player;
    }

    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void rightClickAndExactly15Slots(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        Player player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST, 2));
        var click = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, horse);
        NeoForge.EVENT_BUS.post(click);
        helper.assertTrue(click.isCanceled(), "Chest interaction was not handled");
        helper.assertTrue(HorseChests.hasChest(horse), "Chest not attached");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Chest not consumed exactly once");
        helper.assertTrue(HorseChests.cargo(horse).getContainerSize() == 15, "Storage must have exactly 15 slots");
        helper.assertTrue(!HorseChests.attach(horse, player, player.getMainHandItem()), "Second chest accepted");
        var menu = new CargoMenu(1, player.getInventory(), horse, HorseChests.cargo(horse));
        helper.assertTrue(menu.slots.size() == 51, "Must have 15 storage + 36 player slots");
        horse.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void saveReloadAndLastSlot(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        Player player = player(helper);
        HorseChests.attach(horse, player, new ItemStack(Items.CHEST));
        HorseChests.cargo(horse).setItem(14, new ItemStack(Items.DIAMOND, 23));
        CompoundTag saved = new CompoundTag();
        horse.saveWithoutId(saved);
        AbstractHorse loaded = (AbstractHorse) horse.getType().create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(HorseChests.hasChest(loaded), "Chest lost on reload");
        helper.assertTrue(HorseChests.cargo(loaded).getItem(14).is(Items.DIAMOND), "Last slot item lost");
        helper.assertTrue(HorseChests.cargo(loaded).getItem(14).getCount() == 23, "Count changed on reload");
        horse.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void rejectsOtherOwnerAndWildHorse(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        Player player = player(helper);
        horse.setOwnerUUID(UUID.randomUUID());
        helper.assertTrue(!HorseChests.attach(horse, player, new ItemStack(Items.CHEST)), "Other owner's horse accepted chest");
        horse.setOwnerUUID(null);
        horse.getClass().getMethod("setFamiliarity", float.class).invoke(horse, 0F);
        helper.assertTrue(!HorseChests.attach(horse, player, new ItemStack(Items.CHEST)), "Wild horse accepted chest");
        horse.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void shiftClickBothDirections(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        Player player = player(helper);
        player.moveTo(horse.position());
        HorseChests.attach(horse, player, new ItemStack(Items.CHEST));
        var cargo = HorseChests.cargo(horse);
        var menu = new CargoMenu(1, player.getInventory(), horse, cargo);
        cargo.setItem(14, new ItemStack(Items.DIAMOND, 32));
        helper.assertTrue(!menu.quickMoveStack(player, 14).isEmpty(), "Cargo shift click failed");
        helper.assertTrue(cargo.isEmpty(), "Cargo retained moved items");
        helper.assertTrue(!menu.quickMoveStack(player, 50).isEmpty(), "Player shift click failed");
        helper.assertTrue(cargo.countItem(Items.DIAMOND) == 32, "Shift click duplicated/lost items");
        horse.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void deathDropsChestAndAllContentsOnce(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        HorseChests.attach(horse, player(helper), new ItemStack(Items.CHEST));
        HorseChests.cargo(horse).setItem(0, new ItemStack(Items.DIAMOND, 12));
        HorseChests.cargo(horse).setItem(14, new ItemStack(Items.GOLD_INGOT, 7));
        var drops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(horse, horse.damageSources().generic(), drops, false));
        helper.assertTrue(drops.size() == 3, "Expected chest and two item stacks");
        helper.assertTrue(drops.stream().mapToInt(x -> x.getItem().is(Items.DIAMOND) ? x.getItem().getCount() : 0).sum() == 12, "Diamond loss/duplication");
        helper.assertTrue(!HorseChests.hasChest(horse), "Death did not clear storage");
        var again = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(horse, horse.damageSources().generic(), again, false));
        helper.assertTrue(again.isEmpty(), "Duplicate death drops");
        horse.discard();
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void riddenInventoryCommandOpensCargo(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "cargo-test-player");
        var server = helper.getLevel().getServer();
        var player = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(), profile,
            net.minecraft.server.level.ClientInformation.createDefault());
        // Exercise real menu/command handling; only the absent client transport is mocked.
        // GameTestHelper's vanilla mock login cannot negotiate TFC's required network payloads.
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                net.minecraft.network.PacketSendListener listener) {}
        };
        player.moveTo(horse.position());
        HorseChests.attach(horse, player, new ItemStack(Items.CHEST));
        helper.assertTrue(player.startRiding(horse, true), "Could not mount horse");
        player.connection.handlePlayerCommand(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
            player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
        helper.assertTrue(player.containerMenu instanceof CargoMenu, "Riding inventory command did not open cargo");
        helper.assertTrue(player.getVehicle() == horse, "Opening cargo dismounted player");
        player.closeContainer();
        player.stopRiding();
        horse.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = HorseChests.ID)
    public static void acceptsTFCChestAndClearsSavedEmptyInventory(GameTestHelper helper) throws Exception {
        AbstractHorse horse = horse(helper);
        Player player = player(helper);
        ItemStack chest = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:wood/chest/oak")));
        helper.assertTrue(!chest.isEmpty(), "TFC oak chest missing");
        helper.assertTrue(HorseChests.attach(horse, player, chest), "TFC chest rejected");
        HorseChests.cargo(horse).setItem(14, new ItemStack(Items.DIAMOND));
        HorseChests.cargo(horse).removeItem(14, 1);
        CompoundTag saved = new CompoundTag();
        horse.saveWithoutId(saved);
        AbstractHorse loaded = (AbstractHorse) horse.getType().create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(HorseChests.cargo(loaded).isEmpty(), "Removed item returned after reload");
        horse.discard();
        helper.succeed();
    }

}
