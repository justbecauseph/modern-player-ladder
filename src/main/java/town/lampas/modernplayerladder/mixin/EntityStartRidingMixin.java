package town.lampas.modernplayerladder.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import town.lampas.modernplayerladder.ladder.PlayerLadderHandler;

/**
 * Allows authorized mounts to use players as vehicles despite the player entity type being non-serializable.
 */
@Mixin(Entity.class)
public abstract class EntityStartRidingMixin {

    @Redirect(
        method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"
        )
    )
    private boolean modernPlayerLadder$allowPlayerVehicle(
        EntityType<?> type,
        Entity vehicle,
        boolean force,
        boolean emitGameEvent
    ) {
        Entity passenger = (Entity) (Object) this;
        return type.canSerialize()
            || (type == EntityTypes.PLAYER
                && PlayerLadderHandler.canMountNonSerializablePlayerVehicle(passenger, vehicle, force));
    }
}
