# Implementation Progress — Modern Player Ladder

## Phase Tracking

| Phase | Description | Status | Commit / Notes |
|---|---|---|---|
| **Phase 1** | Project Bootstrap (Gradle, Loom, Fabric metadata, ModInitializer) | Completed | Commit `2962c0e` |
| **Phase 2** | Configuration (`PlayerLadderConfig`, `ClickMode`, JSON storage, exclusions) | Completed | Commit `51e4080` |
| **Phase 3** | Persistent Consent (Fabric Data Attachments, `PlayerLadderState`) | Completed | Commit `5d291e5` |
| **Phase 4** | Commands (`/ladder toggle`, `/playerladder toggle`, immediate dismount) | Completed | Commit `8c5fd36` |
| **Phase 5** | Core Stacking Logic (`PlayerLadderHandler` port) | Completed | Pure stacking logic: `rideEntity`, `pickUpEntity`, `canPickUpOrRide`, `getHighestOrSelf` |
| **Phase 6** | Right-Click Interaction Hook (`UseEntityCallback` / interaction hook) | Pending | |
| **Phase 7** | Lifecycle Behavior (Crouch dismount, logout cleanup, gamemode cleanup) | Pending | |
| **Phase 8** | Projectile / Interaction Passthrough (`ProjectileUtilMixin`) | Pending | |
| **Phase 9** | `/ride` Command Extension (`RideCommandMixin`) | Pending | |
| **Phase 10** | Passenger Sync Audit | Pending | |
| **Phase 11** | Unit & Integration Tests | Pending | |
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
