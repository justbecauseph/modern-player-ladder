package town.lampas.modernplayerladder.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.RideCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

/**
 * RideCommand mixin to allow players to be vehicles in the /ride command when enabled.
 * Fabric API does not provide a command modifier hook for vanilla /ride validation.
 */
@Mixin(RideCommand.class)
public abstract class RideCommandMixin {

    @Shadow @Final private static Dynamic2CommandExceptionType ERROR_ALREADY_RIDING;
    @Shadow @Final private static Dynamic2CommandExceptionType ERROR_MOUNT_FAILED;
    @Shadow @Final private static SimpleCommandExceptionType ERROR_MOUNTING_LOOP;
    @Shadow @Final private static SimpleCommandExceptionType ERROR_WRONG_DIMENSION;

    @Inject(
        method = "mount(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)I",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void modernPlayerLadder$handlePlayerVehicleMount(
        CommandSourceStack source,
        Entity target,
        Entity vehicle,
        CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        if (PlayerLadderConfig.get().rideCommandExtension() && vehicle.getType() == EntityTypes.PLAYER) {
            Entity currentVehicle = target.getVehicle();
            if (currentVehicle != null) {
                throw ERROR_ALREADY_RIDING.create(target.getDisplayName(), currentVehicle.getDisplayName());
            } else if (target.getSelfAndPassengers().anyMatch(p -> p == vehicle)) {
                throw ERROR_MOUNTING_LOOP.create();
            } else if (target.level() != vehicle.level()) {
                throw ERROR_WRONG_DIMENSION.create();
            } else if (!target.startRiding(vehicle, true, true)) {
                throw ERROR_MOUNT_FAILED.create(target.getDisplayName(), vehicle.getDisplayName());
            } else {
                source.sendSuccess(() -> Component.translatable("commands.ride.mount.success", target.getDisplayName(), vehicle.getDisplayName()), true);
                cir.setReturnValue(1);
            }
        }
    }
}
