package town.lampas.modernplayerladder.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.commands.RideCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

/**
 * RideCommand mixin to allow players to be vehicles in the /ride command when enabled.
 * Fabric API does not provide a command modifier hook for vanilla /ride validation.
 */
@Mixin(RideCommand.class)
public abstract class RideCommandMixin {

    @ModifyExpressionValue(
        method = "mount",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;is(Lnet/minecraft/world/entity/EntityType;)Z")
    )
    private static boolean modernPlayerLadder$allowPlayerVehicle(boolean original) {
        return original && !PlayerLadderConfig.get().rideCommandExtension();
    }
}
