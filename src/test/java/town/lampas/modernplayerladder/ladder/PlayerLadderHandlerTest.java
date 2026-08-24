package town.lampas.modernplayerladder.ladder;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import town.lampas.modernplayerladder.config.ClickMode;
import town.lampas.modernplayerladder.config.PlayerLadderConfig;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void testRecursivePassengerDetection() {
        Entity a = mock(Entity.class);
        Entity b = mock(Entity.class);
        Entity c = mock(Entity.class);
        Entity unrelated = mock(Entity.class);

        // A is carrier, B rides A, C rides B
        when(b.getVehicle()).thenReturn(a);
        when(c.getVehicle()).thenReturn(b);
        when(a.getVehicle()).thenReturn(null);
        when(unrelated.getVehicle()).thenReturn(null);

        // A is ancestor of B and C
        assertTrue(PlayerLadderHandler.isRecursivePassenger(a, b));
        assertTrue(PlayerLadderHandler.isRecursivePassenger(a, c));

        // B is ancestor of C
        assertTrue(PlayerLadderHandler.isRecursivePassenger(b, c));

        // C is not ancestor of A or B
        assertFalse(PlayerLadderHandler.isRecursivePassenger(c, a));
        assertFalse(PlayerLadderHandler.isRecursivePassenger(c, b));

        // Unrelated entity is not passenger of A
        assertFalse(PlayerLadderHandler.isRecursivePassenger(a, unrelated));
        assertFalse(PlayerLadderHandler.isRecursivePassenger(unrelated, a));
    }

    @Test
    void testGetHighestOrSelf() {
        Entity a = mock(Entity.class);
        Entity b = mock(Entity.class);
        Entity c = mock(Entity.class);
        Entity newPassenger = mock(Entity.class);

        // Single entity (no passengers) returns self
        when(a.isVehicle()).thenReturn(false);
        assertEquals(a, PlayerLadderHandler.getHighestOrSelf(a, newPassenger, 16));

        // Stack: A -> B -> C
        when(a.isVehicle()).thenReturn(true);
        when(a.getFirstPassenger()).thenReturn(b);
        when(b.isVehicle()).thenReturn(true);
        when(b.getFirstPassenger()).thenReturn(c);
        when(c.isVehicle()).thenReturn(false);

        assertEquals(c, PlayerLadderHandler.getHighestOrSelf(a, newPassenger, 16));

        // Cycle detection: if newPassenger is already in the stack, returns null
        assertNull(PlayerLadderHandler.getHighestOrSelf(a, b, 16));
        assertNull(PlayerLadderHandler.getHighestOrSelf(a, c, 16));

        // Limit enforcement: limit = 1 on a 3-stack (A + 2 passengers) exceeds limit
        assertNull(PlayerLadderHandler.getHighestOrSelf(a, newPassenger, 1));
    }

    @Test
    void testFilterHitResultPredicateBranches() {
        Entity shooter = mock(Entity.class);
        Entity passenger = mock(Entity.class);
        Entity unrelated = mock(Entity.class);

        when(shooter.isVehicle()).thenReturn(true);
        when(passenger.getVehicle()).thenReturn(shooter);
        when(unrelated.getVehicle()).thenReturn(null);

        Predicate<Entity> originalPredicate = e -> true;

        // 1. Shooter is null -> returns original predicate
        assertSame(originalPredicate, PlayerLadderHandler.filterHitResultPredicate(originalPredicate, null));

        // 2. Shooter is not a vehicle -> returns original predicate
        Entity nonVehicleShooter = mock(Entity.class);
        when(nonVehicleShooter.isVehicle()).thenReturn(false);
        assertSame(originalPredicate, PlayerLadderHandler.filterHitResultPredicate(originalPredicate, nonVehicleShooter));

        // 3. Shooter is vehicle but allowInteractions is false -> returns original predicate
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
        assertSame(originalPredicate, PlayerLadderHandler.filterHitResultPredicate(originalPredicate, shooter));

        // 4. Shooter is vehicle AND allowInteractions is true -> wraps predicate and filters passengers
        PlayerLadderConfig enabledConfig = new PlayerLadderConfig(
            ClickMode.RIDE,
            16,
            16,
            true,
            true,
            List.of(),
            true,
            true // allowInteractions = true
        );
        PlayerLadderConfig.setInstance(enabledConfig);
        Predicate<Entity> wrappedPredicate = PlayerLadderHandler.filterHitResultPredicate(originalPredicate, shooter);
        assertNotSame(originalPredicate, wrappedPredicate);

        // Passengers of shooter are rejected by the predicate
        assertFalse(wrappedPredicate.test(passenger));

        // Unrelated entities pass the predicate
        assertTrue(wrappedPredicate.test(unrelated));

        // Reset to default
        PlayerLadderConfig.setInstance(PlayerLadderConfig.createDefault());
    }

    @Test
    void testHandleCarrierTickCrouchDismount() {
        ServerPlayer carrier = mock(ServerPlayer.class);
        Entity passenger = mock(Entity.class);

        when(carrier.onGround()).thenReturn(true);
        when(carrier.isVehicle()).thenReturn(true);
        when(carrier.isCrouching()).thenReturn(true);
        when(carrier.getFirstPassenger()).thenReturn(passenger);

        // Grounded + vehicle + crouching -> dismounts passenger
        PlayerLadderHandler.handleCarrierTick(carrier);
        verify(passenger, times(1)).stopRiding();

        // Not grounded -> does not dismount
        reset(passenger);
        when(carrier.onGround()).thenReturn(false);
        PlayerLadderHandler.handleCarrierTick(carrier);
        verify(passenger, never()).stopRiding();

        // Grounded but not crouching -> does not dismount
        when(carrier.onGround()).thenReturn(true);
        when(carrier.isCrouching()).thenReturn(false);
        PlayerLadderHandler.handleCarrierTick(carrier);
        verify(passenger, never()).stopRiding();
    }

    @Test
    void testHandleGameModeChange() {
        ServerPlayer player = mock(ServerPlayer.class);
        Entity passenger = mock(Entity.class);

        when(player.isVehicle()).thenReturn(true);
        when(player.getFirstPassenger()).thenReturn(passenger);

        PlayerLadderHandler.handleGameModeChange(player);
        verify(passenger, times(1)).stopRiding();

        // When player is not a vehicle -> does nothing
        reset(passenger);
        when(player.isVehicle()).thenReturn(false);
        PlayerLadderHandler.handleGameModeChange(player);
        verify(passenger, never()).stopRiding();
    }

    @Test
    void testOnPlayerLeave() {
        ServerPlayer departingPlayer = mock(ServerPlayer.class);
        Player vehiclePlayer = mock(Player.class);
        Entity nonPlayerVehicle = mock(Entity.class);

        // Departing player is riding another player -> stops riding
        when(departingPlayer.isPassenger()).thenReturn(true);
        when(departingPlayer.getVehicle()).thenReturn(vehiclePlayer);
        PlayerLadderEvents.onPlayerLeave(departingPlayer);
        verify(departingPlayer, times(1)).stopRiding();

        // Departing player is riding a non-player vehicle -> does not stop riding
        reset(departingPlayer);
        when(departingPlayer.isPassenger()).thenReturn(true);
        when(departingPlayer.getVehicle()).thenReturn(nonPlayerVehicle);
        PlayerLadderEvents.onPlayerLeave(departingPlayer);
        verify(departingPlayer, never()).stopRiding();

        // Departing player is not riding -> does not stop riding
        reset(departingPlayer);
        when(departingPlayer.isPassenger()).thenReturn(false);
        PlayerLadderEvents.onPlayerLeave(departingPlayer);
        verify(departingPlayer, never()).stopRiding();
    }
}
