package town.lampas.modernplayerladder.mixin;

import net.minecraft.server.commands.RideCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

/**
 * RideCommand mixin to allow players to be vehicles in the /ride command when enabled.
 * Fabric API does not provide a command modifier hook for vanilla /ride validation.
 */
@Mixin(RideCommand.class)
public abstract class RideCommandMixin {

    @Redirect(
        method = "mount(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;is(Ljava/lang/Object;)Z"
        )
    )
    private static boolean modernPlayerLadder$allowPlayerVehicle(Entity vehicle, Object type) {
        boolean vanillaResult = vehicle.is((EntityType<?>) type);
        return vanillaResult && !PlayerLadderConfig.get().rideCommandExtension();
    }
}
