package town.lampas.modernplayerladder.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import town.lampas.modernplayerladder.ladder.PlayerLadderHandler;

/**
 * ServerPlayer mixin for per-player carrier tick logic (grounded crouch dismount).
 * Fabric API does not provide a per-ServerPlayer post-tick event.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void modernPlayerLadder$handleCarrierTick(CallbackInfo ci) {
        PlayerLadderHandler.handleCarrierTick((ServerPlayer) (Object) this);
    }
}
