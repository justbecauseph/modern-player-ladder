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
| **Phase 11** | Unit & Integration Tests | Completed | 14/14 unit tests passing, dedicated server (`runServer`) startup verified |
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
- [x] Implemented `RideCommandMixin` with HEAD injection and shadow exception handling.
- [x] Bypasses only player vehicle rejection when `rideCommandExtension` is enabled.
- [x] All other `/ride` validations (already riding, mounting loop, dimension, startRiding) remain intact.
- [x] Mixin registered in `modern-player-ladder.mixins.json`.

---

## Phase 10: Passenger Sync Audit
- [x] Audited Minecraft 26.2 `ServerEntity#sendChanges` and passenger tracking pipeline.
- [x] Verified full entity position resynchronization and passenger delta broadcasting are native in 26.2.
- [x] Conclusion: `EntityMixin` manual packet workaround from legacy versions is unnecessary and omitted.

---

## Phase 11: Unit & Integration Tests
- [x] **Unit Testing (14/14 tests passing)**:
  - `PlayerLadderConfigTest`: Default config, valid JSON parsing, malformed JSON fallback, non-object JSON fallback, invalid bounds clamping, config file persistence, entity type/tag exclusion matching.
  - `PlayerLadderHandlerTest`: Null safety, 3-level recursive passenger hierarchy detection (`A -> B -> C`), stack traversal and cycle/limit handling in `getHighestOrSelf`, hit-predicate filter branches (`allowInteractions` on/off, vehicle/non-vehicle), grounded carrier crouch dismount, game-mode change passenger dismount, player logout from player vehicle vs non-player vehicle.
- [x] **Static & Codebase Audits**: Zero legacy NeoForge references across entire source tree.
- [x] **Dedicated Server Integration (`runServer`)**:
  - Successfully booted dedicated server on 26.2 (`*:25565`).
  - Verified mixin transformations (`ServerPlayerMixin`, `ServerPlayerGameModeMixin`, `ProjectileUtilMixin`, `RideCommandMixin`).
  - Verified world creation, spawn preparation, and clean shutdown.
- [x] `./gradlew clean build` completes successfully.
