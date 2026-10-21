package dev.justromik.tfchorsechests.mixin;

import dev.justromik.tfchorsechests.CargoHolder;
import dev.justromik.tfchorsechests.HorseCargo;
import dev.justromik.tfchorsechests.HorseChests;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin implements CargoHolder {
    @Unique private HorseCargo tfchests$cargo;

    @Override
    public HorseCargo tfchests$cargo() {
        if (tfchests$cargo == null) tfchests$cargo = new HorseCargo((AbstractHorse) (Object) this);
        return tfchests$cargo;
    }

    @Inject(method = "openCustomInventoryScreen", at = @At("HEAD"), cancellable = true)
    private void tfchests$openRiddenCargo(Player player, CallbackInfo callback) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        // The usual riding inventory key (E) sends vanilla's OPEN_INVENTORY command here.
        // On foot, holding any item retains access to the normal saddle/armor screen.
        if ((player.getVehicle() == horse || player.getMainHandItem().isEmpty()) && HorseChests.open(horse, player)) {
            callback.cancel();
        }
    }
}
