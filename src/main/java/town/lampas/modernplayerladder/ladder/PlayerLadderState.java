package town.lampas.modernplayerladder.ladder;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PlayerLadderState {
    public static final Identifier ENABLED_ID = Identifier.fromNamespaceAndPath("modern_player_ladder", "enabled");

    public static final AttachmentType<Boolean> ENABLED_ATTACHMENT = AttachmentRegistry.create(
        ENABLED_ID,
        builder -> builder
            .initializer(() -> false)
            .persistent(Codec.BOOL)
            .copyOnDeath()
    );

    private PlayerLadderState() {}

    public static void register() {
        // Triggers static initialization to register the attachment type with Fabric
    }

    public static boolean isEnabled(Player player) {
        if (player == null) return false;
        return player.getAttachedOrElse(ENABLED_ATTACHMENT, false);
    }

    public static boolean isRidingDisabledByPlayer(Player player) {
        return !isEnabled(player);
    }

    public static void setEnabled(Player player, boolean enabled) {
        if (player != null) {
            player.setAttached(ENABLED_ATTACHMENT, enabled);
        }
    }

    public static boolean toggle(Player player) {
        boolean newState = !isEnabled(player);
        setEnabled(player, newState);
        return newState;
    }

    public static boolean toggle(ServerPlayer player) {
        return toggle((Player) player);
    }
}
