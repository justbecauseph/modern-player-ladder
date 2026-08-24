package town.lampas.modernplayerladder.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import town.lampas.modernplayerladder.ladder.PlayerLadderHandler;

/**
 * Syncs a player vehicle's passenger list to the carrier's own connection.
 * Vanilla passenger tracking sends to observers but excludes the tracked player itself.
 */
@Mixin(Entity.class)
public abstract class EntityPassengerSyncMixin {

    @Inject(
        method = "addPassenger(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void modernPlayerLadder$syncAddedPassenger(Entity passenger, CallbackInfo ci) {
        PlayerLadderHandler.syncPlayerVehiclePassengers((Entity) (Object) this);
    }

    @Inject(
        method = "removePassenger(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void modernPlayerLadder$syncRemovedPassenger(Entity passenger, CallbackInfo ci) {
        PlayerLadderHandler.syncPlayerVehiclePassengers((Entity) (Object) this);
    }
}
