package town.lampas.modernplayerladder.ladder;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerLadderHandlerTest {

    @BeforeAll
    static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testNullEntityHandling() {
        assertFalse(PlayerLadderHandler.canPickUpOrRide(null));
        assertNull(PlayerLadderHandler.getHighestOrSelf(null, null, 16));
        assertEquals(InteractionResult.PASS, PlayerLadderHandler.rideEntity(null, null, null, null));
        assertEquals(InteractionResult.PASS, PlayerLadderHandler.pickUpEntity(null, null, null, null));
        assertDoesNotThrow(() -> PlayerLadderHandler.handleCarrierTick(null));
        assertDoesNotThrow(() -> PlayerLadderHandler.handleGameModeChange(null));
    }
}
