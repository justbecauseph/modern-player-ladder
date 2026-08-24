package town.lampas.modernplayerladder.ladder;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

public final class PlayerLadderEvents {

    private PlayerLadderEvents() {}

    public static void register() {
        UseEntityCallback.EVENT.register(PlayerLadderEvents::onUseEntity);
    }

    public static InteractionResult onUseEntity(
        Player player,
        Level level,
        InteractionHand hand,
        Entity entity,
        EntityHitResult hitResult
    ) {
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }

        return switch (PlayerLadderConfig.get().rightClickMode()) {
            case RIDE -> PlayerLadderHandler.rideEntity(player, entity, level, hand);
            case PICK_UP -> PlayerLadderHandler.pickUpEntity(player, entity, level, hand);
            case DO_NOTHING -> InteractionResult.PASS;
        };
    }
}
