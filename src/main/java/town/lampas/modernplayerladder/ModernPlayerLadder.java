package town.lampas.modernplayerladder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import town.lampas.modernplayerladder.command.PlayerLadderCommands;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;
import town.lampas.modernplayerladder.ladder.PlayerLadderState;

public class ModernPlayerLadder implements ModInitializer {
    public static final String MOD_ID = "modern_player_ladder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Modern Player Ladder initializing...");
        PlayerLadderState.register();
        PlayerLadderConfig.load();
        PlayerLadderConfig.rebuildEntityExclusions();
        PlayerLadderCommands.register();
    }
}
