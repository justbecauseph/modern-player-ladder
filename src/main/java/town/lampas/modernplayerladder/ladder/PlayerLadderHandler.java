package town.lampas.modernplayerladder.ladder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

public final class PlayerLadderHandler {

    private PlayerLadderHandler() {}

    public static InteractionResult rideEntity(Player player, Entity targetVehicle, Level level, InteractionHand hand) {
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (PlayerLadderState.isRidingDisabledByPlayer(player)) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND && canPickUpOrRide(targetVehicle) && player.getItemInHand(hand).isEmpty()) {
            if (!level.isClientSide()) {
                Entity vehicle = getHighestOrSelf(targetVehicle, player, PlayerLadderConfig.get().stepUpLimit());
                if (vehicle == null) {
                    return InteractionResult.FAIL;
                }
                player.startRiding(vehicle);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult pickUpEntity(Player player, Entity targetPassenger, Level level, InteractionHand hand) {
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (PlayerLadderState.isRidingDisabledByPlayer(player)) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND && canPickUpOrRide(targetPassenger) && player.getItemInHand(hand).isEmpty()) {
            if (!level.isClientSide()) {
                Entity vehicle = getHighestOrSelf(player, targetPassenger, PlayerLadderConfig.get().pickUpLimit());
                if (vehicle == null) {
                    return InteractionResult.FAIL;
                }
                targetPassenger.startRiding(vehicle);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static Entity getHighestOrSelf(Entity vehicle, Entity newPassenger, int limit) {
        if (vehicle == null) return null;
        int count = -1;
        while (vehicle.isVehicle()) {
            count++;
            vehicle = vehicle.getFirstPassenger();
            if (vehicle == newPassenger || count >= limit) {
                return null;
            }
        }
        return vehicle;
    }

    public static boolean canPickUpOrRide(Entity entity) {
        if (entity == null) return false;
        PlayerLadderConfig config = PlayerLadderConfig.get();
        if (entity instanceof Player player) {
            if (player.isSpectator()) return false;
            if (PlayerLadderState.isRidingDisabledByPlayer(player)) return false;
            return config.allowPlayers();
        }

        return config.allowLivingEntities() && !PlayerLadderConfig.isEntityExcluded(entity.getType());
    }

    public static void handleCarrierTick(ServerPlayer player) {
        if (player != null && player.onGround() && player.isVehicle() && player.isCrouching()) {
            Entity firstPassenger = player.getFirstPassenger();
            if (firstPassenger != null) {
                firstPassenger.stopRiding();
            }
        }
    }

    public static void handleGameModeChange(ServerPlayer player) {
        if (player != null && player.isVehicle()) {
            Entity firstPassenger = player.getFirstPassenger();
            if (firstPassenger != null) {
                firstPassenger.stopRiding();
            }
        }
    }

    public static java.util.function.Predicate<Entity> filterHitResultPredicate(
        java.util.function.Predicate<Entity> predicate,
        Entity shooter
    ) {
        if (PlayerLadderConfig.get().allowInteractions() && shooter != null && shooter.isVehicle()) {
            return entity -> (predicate == null || predicate.test(entity)) && !isRecursivePassenger(shooter, entity);
        }
        return predicate;
    }

    public static boolean isRecursivePassenger(Entity root, Entity candidate) {
        if (root == null || candidate == null) return false;
        Entity vehicle = candidate.getVehicle();
        while (vehicle != null) {
            if (vehicle == root) {
                return true;
            }
            vehicle = vehicle.getVehicle();
        }
        return false;
    }
}
