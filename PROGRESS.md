# Implementation Progress — Modern Player Ladder

## Phase Tracking

| Phase | Description | Status | Commit / Notes |
|---|---|---|---|
| **Phase 1** | Project Bootstrap (Gradle, Loom, Fabric metadata, ModInitializer) | Completed | Commit `2962c0e` |
| **Phase 2** | Configuration (`PlayerLadderConfig`, `ClickMode`, JSON storage, exclusions) | Completed | Commit `51e4080` |
| **Phase 3** | Persistent Consent (Fabric Data Attachments, `PlayerLadderState`) | Completed | Commit `5d291e5` |
| **Phase 4** | Commands (`/ladder toggle`, `/playerladder toggle`, immediate dismount) | Completed | Commit `8c5fd36` |
| **Phase 5** | Core Stacking Logic (`PlayerLadderHandler` port) | Completed | Commit `f2ef18b` |
| **Phase 6** | Right-Click Interaction Hook (`UseEntityCallback` / interaction hook) | Completed | Commit `516237f` |
| **Phase 7** | Lifecycle Behavior (Crouch dismount, logout cleanup, gamemode cleanup) | Completed | Commit `5725c62` |
| **Phase 8** | Projectile / Interaction Passthrough (`ProjectileUtilMixin`) | Completed | Commit `74586b4` |
| **Phase 9** | `/ride` Command Extension (`RideCommandMixin`) | Completed | Commit `9ca8e5c` |
| **Phase 10** | Passenger Sync Audit | Completed | Commit `cd98cc3` |
| **Phase 11** | Unit & Integration Tests | Completed | Corrective verification: narrow `/ride`, runtime player mounting/sync, and full live multiplayer matrix |
| **Phase 12** | Documentation (README, configuration guide) | Pending | |

---

## Phase 1: Bootstrap
- [x] Standalone build configured for Fabric 26.2 and Java 25.
- [x] Basic Fabric mod metadata and mixin configuration present.
- [x] `./gradlew build` completes successfully.

---

## Phase 2: Configuration
- [x] `ClickMode` enum implemented (`RIDE`, `PICK_UP`, `DO_NOTHING`).
- [x] `PlayerLadderConfig` implemented matching all parity settings and NeoForge defaults.
- [x] JSON config loading and saving via Gson at `config/modern-player-ladder.json`.
- [x] Validation with safe fallback handling for corrupted JSON, malformed IDs/tags, and invalid limit bounds.
- [x] Pre-parsed entity exclusion cache for entity types and tags.
- [x] Comprehensive JUnit 5 unit tests passing in `./gradlew test`.

---

## Phase 3: Persistent Consent
- [x] Registered `modern_player_ladder:enabled` using Fabric Data Attachment API (`fabric-data-attachment-api-v1`).
- [x] Default initializer set to `false` (opt-in by default).
- [x] Codec persistence (`Codec.BOOL`) and `copyOnDeath()` enabled.
- [x] `PlayerLadderState` helper provides `isEnabled`, `setEnabled`, `toggle`, and `isRidingDisabledByPlayer`.

---

## Phase 4: Commands
- [x] Implemented `PlayerLadderCommands` with `/ladder toggle` and compatibility alias `/playerladder toggle`.
- [x] Player source verification via `getPlayerOrException()`.
- [x] Translatable feedback messages in green (`enabled`) and red (`disabled`).
- [x] Immediate dismount of carrier passengers and self vehicle relationship on disable.

---

## Phase 5: Core Stacking Logic
- [x] Implemented `PlayerLadderHandler` with pure callable methods (`rideEntity`, `pickUpEntity`, `canPickUpOrRide`, `getHighestOrSelf`).
- [x] Enforced mutual player consent, spectator rejection, empty main-hand check, and server-side graph mutation.
- [x] Stack traversal depth limits (`stepUpLimit`, `pickUpLimit`) and cycle detection implemented.
- [x] No mixins, callbacks, or event registrations added early.

---

## Phase 6: Right-Click Interaction Hook
- [x] Implemented `PlayerLadderEvents` registering `UseEntityCallback`.
- [x] Mode dispatching wired to `PlayerLadderHandler` for `RIDE`, `PICK_UP`, and `DO_NOTHING`.
- [x] Spectators passed through to vanilla with `InteractionResult.PASS`.
- [x] Server-authoritative design without custom networking.
- [x] `PlayerLadderEvents.register()` called in `ModernPlayerLadder.onInitialize()`.

---

## Phase 7: Lifecycle Behavior
- [x] Crouch-to-dismount implemented via `ServerPlayerMixin` on `tick` TAIL -> `PlayerLadderHandler.handleCarrierTick`.
- [x] Logout cleanup implemented via `ServerPlayerEvents.LEAVE` in `PlayerLadderEvents` dismounting departing player from player vehicles.
- [x] Game-mode change cleanup implemented via `ServerPlayerGameModeMixin` on `changeGameModeForPlayer` RETURN -> `PlayerLadderHandler.handleGameModeChange`.
- [x] Mixins registered in `modern-player-ladder.mixins.json`.

---

## Phase 8: Projectile / Interaction Passthrough
- [x] Implemented `ProjectileUtilMixin` on `getEntityHitResult` with `@ModifyVariable` on `Predicate<Entity>`.
- [x] Implemented allocation-free `PlayerLadderHandler.isRecursivePassenger(root, candidate)`.
- [x] Gated predicate wrapping behind `allowInteractions` and `shooter.isVehicle()` to skip lambda allocations in empty case.
- [x] Mixin registered in `modern-player-ladder.mixins.json`.

---

## Phase 9: `/ride` Command Extension
- [x] Implemented `RideCommandMixin` as a narrow standard `@Redirect` of vanilla's `Entity.is(Object)` player-vehicle check.
- [x] Changes only the player-vehicle rejection result when `rideCommandExtension` is enabled.
- [x] All other `/ride` validations (already riding, mounting loop, dimension, startRiding) remain intact.
- [x] Mixin registered in `modern-player-ladder.mixins.json`.

---

## Phase 10: Passenger Sync Audit
- [x] Audited Minecraft 26.2 `ServerEntity#sendChanges` and passenger tracking pipeline.
- [x] Verified full entity position resynchronization and passenger delta broadcasting are native in 26.2 for ordinary tracking observers.
- [x] Phase 11 live multiplayer testing disproved the original conclusion for player vehicles: `sendToTrackingPlayersFiltered` excludes the tracked player entity's own connection, so the carrier did not receive its passenger-list change.
- [x] Added a focused `EntityPassengerSyncMixin` at `addPassenger` / `removePassenger` that sends the vanilla `ClientboundSetPassengersPacket` only to a `ServerPlayer` vehicle's own connection; observer synchronization remains vanilla.

---

## Phase 11: Unit & Integration Tests

### Review correction scope and acceptance criteria

Scope: replace the runtime-working but overbroad `RideCommandMixin` HEAD implementation with a narrow injection at vanilla's player-vehicle check, then complete the mandatory real multiplayer matrix without adding custom networking or a passenger-sync workaround unless the live evidence requires one.

- [x] `RideCommandMixin` changes only the result of vanilla's player-vehicle rejection and leaves the rest of `RideCommand.mount` untouched.
- [x] `./gradlew clean build` succeeds with all unit tests passing and no mixin target warnings.
- [x] `./gradlew runServer` reaches a fully started 26.2 dedicated server with all mixins applied and shuts down cleanly.
- [x] At least two real clients verify consent, passenger graph agreement, dismount, logout, death, PASS-path interaction, projectile passthrough, and `/ride` player vehicles.
- [x] An observer client verifies the stack where practical.
- [x] A dedicated server with the mod accepts clients without the mod; Phase 12 may document that clients do not require Modern Player Ladder.

### Previously completed coverage

- [x] **Unit Testing (15/15 tests passing)**:
  - `PlayerLadderConfigTest`: Default config, valid JSON parsing, malformed JSON fallback, non-object JSON fallback, invalid bounds clamping, config file persistence, entity type/tag exclusion matching.
  - `PlayerLadderHandlerTest`: Null safety, 3-level recursive passenger hierarchy detection (`A -> B -> C`), stack traversal and cycle/limit handling in `getHighestOrSelf`, hit-predicate filter branches (`allowInteractions` on/off, vehicle/non-vehicle), grounded carrier crouch dismount, game-mode change passenger dismount, player logout from player vehicle vs non-player vehicle.
- [x] **Static & Codebase Audits**: Zero legacy NeoForge references across entire source tree.
- [x] **Dedicated Server Integration (`runServer`)**:
  - Successfully booted dedicated server on 26.2 (`*:25565`).
  - Verified mixin transformations (`EntityPassengerSyncMixin`, `EntityStartRidingMixin`, `ServerPlayerMixin`, `ServerPlayerGameModeMixin`, `ProjectileUtilMixin`, `RideCommandMixin`).
  - Verified world creation, spawn preparation, and clean shutdown.
- [x] `./gradlew clean build` completes successfully.

### Live multiplayer evidence (completed)

- [x] Two real modded 26.2 clients (`LadderA` and `LadderB`) connected to the dedicated server and confirmed that right-click player mounting succeeds when both players opt in.
- [x] The two clients agreed on the player passenger graph after the focused carrier-self passenger packet fix; the demonstrated one-client-only view no longer reproduced.
- [x] A tamed horse fixture was summoned on the live test platform and its tested ride interaction worked.
- [x] A third real client (`ObserverC`) visibly confirmed the full horse -> player -> player passenger stack.
- [x] `ObserverC` visibly confirmed the top rider's dismount propagated immediately with no ghost rider.
- [x] Live carrier logout (`LadderB`) cleared the player-vehicle relationship for the remaining clients with no ghost rider.
- [x] Killing mounted rider `LadderA` from the server console removed the rider immediately for `LadderB` and `ObserverC`, with no ghost rider.
- [x] Changing carrier `LadderB` from Survival to Creative immediately dismounted `LadderA` for all clients.
- [x] The tamed-horse interaction passed through to vanilla while `allowLivingEntities=false`, proving the server-authoritative right-click `PASS` path.
- [x] A bow fired by carrier `LadderB` passed through mounted passenger `LadderA` without damaging or colliding with the passenger, as seen by the live clients.
- [x] Vanilla `/ride LadderA mount LadderB` succeeded against a player vehicle and the resulting stack appeared correctly on all three clients.
- [x] Two replacement clients (`VanillaA` and `VanillaB`) launched with the project classes and resources excluded; both loader logs omitted `modern_player_ladder`.
- [x] Both mod-absent clients connected to the modded dedicated server, opted in through the server command, mounted successfully, agreed on the passenger graph, and dismounted cleanly.
