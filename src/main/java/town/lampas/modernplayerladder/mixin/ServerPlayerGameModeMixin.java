package town.lampas.modernplayerladder.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import town.lampas.modernplayerladder.ladder.PlayerLadderHandler;

/**
 * ServerPlayerGameMode mixin to dismount carrier passengers on actual game mode change.
 * Fabric API does not provide a player game mode change lifecycle event.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "changeGameModeForPlayer", at = @At("RETURN"))
    private void modernPlayerLadder$onChangeGameMode(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            PlayerLadderHandler.handleGameModeChange(this.player);
        }
    }
}
