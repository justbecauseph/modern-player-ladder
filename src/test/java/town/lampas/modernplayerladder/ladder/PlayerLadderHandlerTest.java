package town.lampas.modernplayerladder.ladder;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import town.lampas.modernplayerladder.config.ClickMode;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

import java.util.List;
import java.util.function.Predicate;

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
        assertFalse(PlayerLadderHandler.isRecursivePassenger(null, null));
    }

    @Test
    void testFilterHitResultPredicateWhenNoPassengersOrDisabled() {
        Predicate<net.minecraft.world.entity.Entity> originalPredicate = e -> true;

        // When shooter is null, returns original predicate without wrapping
        assertSame(originalPredicate, PlayerLadderHandler.filterHitResultPredicate(originalPredicate, null));

        // When allowInteractions is false, returns original predicate without wrapping
        PlayerLadderConfig disabledConfig = new PlayerLadderConfig(
            ClickMode.RIDE,
            16,
            16,
            true,
            true,
            List.of(),
            true,
            false // allowInteractions = false
        );
        PlayerLadderConfig.setInstance(disabledConfig);
        assertSame(originalPredicate, PlayerLadderHandler.filterHitResultPredicate(originalPredicate, null));

        // Reset to default
        PlayerLadderConfig.setInstance(PlayerLadderConfig.createDefault());
    }

    @Test
    void testIsRecursivePassengerNullSafety() {
        assertFalse(PlayerLadderHandler.isRecursivePassenger(null, null));
    }
}
