# Modern Player Ladder — Complete Agent Implementation Plan

## 1. Project objective

Create a new standalone Fabric mod named **Modern Player Ladder** for **Minecraft 26.2**.

The mod is a modern port of the Player Ladder functionality currently implemented inside `justbecauseph/lampas-overrides`. The goal is **behavioral parity first**, followed by cleanup of loader-specific hacks only where Minecraft 26.2/Fabric makes them unnecessary.

The existing implementation already establishes the important gameplay contract: Player Ladder is opt-in by default, supports `/ladder toggle` and `/playerladder toggle`, player stacking, optional entity pickup, stack limits, crouch dismounting, `/ride` extension, passenger synchronization workarounds, and interaction passthrough through carried passengers. 

Do **not** implement this inside `lampas2-overrides`. It is a new independent gameplay mod.

---

# 2. Project identity

Use:

```text
Display name: Modern Player Ladder
Repository: modern-player-ladder
Mod ID: modern_player_ladder
Maven group: town.lampas
Base package: town.lampas.modernplayerladder
```

Recommended artifact:

```text
modern-player-ladder-<version>+26.2.jar
```

Config:

```text
config/modern-player-ladder.json
```

Mixin configuration:

```text
modern-player-ladder.mixins.json
```

Primary command:

```mcfunction
/ladder toggle
```

Compatibility alias:

```mcfunction
/playerladder toggle
```

Keep the old alias so existing players do not have to relearn the command.

---

# 3. Target platform

Use the existing Lampas 26.2 stack as the initial baseline:

```properties
minecraft_version=26.2
loader_version=0.19.3
fabric_api_version=0.158.0+26.2
java=25
```

Fabric API `0.158.0+26.2` is a current Minecraft 26.2 release. 

The existing Lampas 26.2 project already successfully targets Minecraft 26.2 with Java 25 and this Fabric API generation, so it is a useful build reference. 

Use Fabric Loom appropriate for the current 26.2 toolchain. If bootstrapping from the existing `lampas2-overrides` build, `1.17-SNAPSHOT` is currently in use there. Do not arbitrarily downgrade mappings/tooling.

---

# 4. Core design principle

## Server authoritative

**Modern Player Ladder v1 must be server-authoritative.**

Do not introduce:

- custom C2S packets
- custom S2C packets
- client-side consent caches
- keybinds
- screens
- client-specific gameplay logic

unless a Minecraft 26.2 behavior proves they are genuinely required.

The player's Player Ladder preference belongs to the server.

This gives us several benefits:

- simpler implementation
- no desync between client/server consent
- safer multiplayer behavior
- potential server-only compatibility
- no protocol to maintain
- vanilla passenger packets remain authoritative

The mod may be installed on both client and server, but gameplay should not require custom client networking.

---

# 5. Source-of-truth behavior

Before writing code, the agent must inspect these files from `lampas-overrides`:

```text
src/main/java/town/lampas/overrides/PlayerLadderHandler.java
src/main/java/town/lampas/overrides/ModConfig.java
src/main/java/town/lampas/overrides/mixin/EntityMixin.java
src/main/java/town/lampas/overrides/mixin/ProjectileUtilMixin.java
src/main/java/town/lampas/overrides/mixin/RideCommandMixin.java
src/main/java/town/lampas/overrides/LampasOverridesMod.java
```

Do not reconstruct the old behavior from memory.

In particular, `PlayerLadderHandler` is the canonical source for interaction rules and stack traversal. 

---

# 6. Behavior contract

The initial release should preserve the current behavior unless this plan explicitly says otherwise.

## Default player state

Player Ladder is:

```text
DISABLED by default
```

The old mod deliberately changed to opt-in in v1.0.49. 

A new player therefore cannot accidentally mount another player.

---

# 7. Consent rules

This is a core safety/gameplay property and must not change.

## Player → player

Both players must have Player Ladder enabled.

Given:

```text
Alice wants to ride Bob
```

require:

```text
Alice enabled == true
Bob enabled == true
```

If either is disabled:

```text
PASS
```

and vanilla interaction behavior continues.

The existing handler explicitly checks the target player's opt-in state as part of target eligibility. 

### Examples

```text
Alice OFF + Bob OFF
→ no stacking

Alice ON + Bob OFF
→ no stacking

Alice OFF + Bob ON
→ no stacking

Alice ON + Bob ON
→ stacking permitted
```

---

# 8. Command behavior

Implement:

```mcfunction
/ladder toggle
/playerladder toggle
```

using Fabric's server command API.

Minecraft 26.2 Fabric exposes `CommandRegistrationCallback` with the Brigadier `CommandDispatcher<CommandSourceStack>`. 

No operator permission should be required.

Commands must require a player source.

## Enabling

When currently disabled:

```text
enabled = true
```

Message:

```text
Player Ladder interactions enabled for you.
```

Green formatting.

## Disabling

When currently enabled:

```text
enabled = false
```

Message:

```text
Player Ladder interactions disabled for you.
```

Red formatting.

Immediately dismount:

1. every direct passenger currently riding the player
2. the player themselves if they are currently a passenger

The existing implementation performs both operations. 

Do not wait until the next tick.

---

# 9. Persistent player state

## Use Fabric Data Attachments

Replace NeoForge:

```java
player.getPersistentData()
```

with Fabric's Data Attachment API.

Minecraft 26.2 Fabric API includes `fabric-data-attachment-api-v1`. 

Create:

```text
modern_player_ladder:enabled
```

as an:

```java
AttachmentType<Boolean>
```

Use:

```java
.initializer(() -> false)
.persistent(Codec.BOOL)
.copyOnDeath()
```

Fabric's attachment builder supports both persistence and copying across entity death/conversion. 

### Do not synchronize it to clients in v1

The server is authoritative.

There is currently no client UI that needs the value.

This also avoids unnecessary tracking packets.

Provide a small wrapper class so attachment operations are not scattered throughout the mod:

```java
PlayerLadderState.isEnabled(Player)
PlayerLadderState.setEnabled(ServerPlayer, boolean)
PlayerLadderState.toggle(ServerPlayer)
```

---

# 10. Project structure

Recommended:

```text
modern-player-ladder/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── LICENSE
├── README.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── town/lampas/modernplayerladder/
    │   │       ├── ModernPlayerLadder.java
    │   │       │
    │   │       ├── command/
    │   │       │   └── PlayerLadderCommands.java
    │   │       │
    │   │       ├── config/
    │   │       │   ├── PlayerLadderConfig.java
    │   │       │   └── ClickMode.java
    │   │       │
    │   │       ├── ladder/
    │   │       │   ├── PlayerLadderHandler.java
    │   │       │   ├── PlayerLadderState.java
    │   │       │   └── PlayerLadderEvents.java
    │   │       │
    │   │       └── mixin/
    │   │           ├── ProjectileUtilMixin.java
    │   │           ├── RideCommandMixin.java
    │   │           ├── ServerPlayerMixin.java
    │   │           └── EntityMixin.java
    │   │
    │   └── resources/
    │       ├── fabric.mod.json
    │       ├── modern-player-ladder.mixins.json
    │       └── assets/
    │           └── modern_player_ladder/
    │               └── lang/
    │                   └── en_us.json
    │
    └── test/
        └── java/
            └── town/lampas/modernplayerladder/
```

Important:

**`EntityMixin` may ultimately not exist.**

Only add it if multiplayer testing proves Minecraft 26.2 still needs the passenger synchronization workaround.

---

# 11. Main initializer

Create:

```java
town.lampas.modernplayerladder.ModernPlayerLadder
```

implementing:

```java
ModInitializer
```

Initialization order:

```text
1. register attachment types
2. load configuration
3. resolve configured entity exclusions
4. register commands
5. register Fabric interaction/lifecycle events
```

Example conceptual structure:

```java
@Override
public void onInitialize() {
    PlayerLadderState.register();
    PlayerLadderConfig.load();
    PlayerLadderHandler.rebuildEntityExclusions();

    PlayerLadderCommands.register();
    PlayerLadderEvents.register();
}
```

Do not put gameplay implementation into the initializer.

---

# 12. Configuration

Create:

```text
config/modern-player-ladder.json
```

Default:

```json
{
  "rightClickMode": "RIDE",
  "pickUpLimit": 16,
  "stepUpLimit": 16,
  "allowLivingEntities": false,
  "allowPlayers": true,
  "excludedLivingEntities": [
    "minecraft:wither",
    "minecraft:ender_dragon",
    "minecraft:minecart",
    "#minecraft:boat",
    "#minecraft:dismounts_underwater"
  ],
  "rideCommandExtension": true,
  "allowInteractions": true
}
```

These values reproduce the current NeoForge defaults. 

Use plain Gson.

Do **not** add:

- Cloth Config
- YACL
- Mod Menu
- NightConfig

for v1.

They aren't needed for a small server configuration.

---

# 13. Config model

Use an immutable or simple record/class:

```java
public record PlayerLadderConfig(
    ClickMode rightClickMode,
    int pickUpLimit,
    int stepUpLimit,
    boolean allowLivingEntities,
    boolean allowPlayers,
    List<String> excludedLivingEntities,
    boolean rideCommandExtension,
    boolean allowInteractions
) {}
```

Enum:

```java
RIDE
PICK_UP
DO_NOTHING
```

---

# 14. Config validation

Validate configuration on load.

Rules:

```text
pickUpLimit >= 1
stepUpLimit >= 1
rightClickMode != null
excludedLivingEntities != null
```

For malformed values:

- log a clear warning
- use the corresponding default
- do not crash the server unnecessarily

For malformed entity identifiers:

- log the invalid entry
- ignore only that entry
- continue loading everything else

Do not repeatedly parse entity identifiers during player interaction.

---

# 15. Entity exclusion cache

Like the old mod, maintain resolved caches:

```java
Set<EntityType<?>> excludedEntityTypes;
Set<TagKey<EntityType<?>>> excludedEntityTags;
```

At config load:

```text
minecraft:wither
→ EntityType

#minecraft:boat
→ TagKey<EntityType<?>>
```

Then interaction checks become cheap:

```java
excludedEntityTypes.contains(entity.getType())
```

and:

```java
excludedEntityTags.stream()
    .anyMatch(entity.getType()::is)
```

Do not resolve registry IDs every time someone right-clicks an entity.

---

# 16. Right-click interaction

The Fabric interaction module exists for Minecraft 26.2. 

Investigate the current 26.2 `UseEntityCallback` semantics before implementing.

If it cleanly intercepts the server's entity interaction path, use it.

Otherwise use a narrow server-side vanilla mixin at the correct interaction handling point.

**Do not force Fabric events merely for ideological purity if the event does not provide the required cancellation semantics.**

The old NeoForge event allowed the handler to cancel the original entity interaction when Player Ladder consumed it. 

The Fabric implementation must preserve that property.

---

# 17. Interaction eligibility

When an entity is right-clicked:

```text
if spectator:
    PASS

if Player Ladder disabled for initiator:
    PASS

if hand != MAIN_HAND:
    PASS

if main hand is not empty:
    PASS

switch rightClickMode:
    RIDE       -> rideEntity(...)
    PICK_UP    -> pickUpEntity(...)
    DO_NOTHING -> PASS
```

Only consume/cancel vanilla interaction when Player Ladder successfully handles or definitively rejects a ladder operation in the same cases as the old implementation.

Do not turn every entity right-click into `FAIL`.

---

# 18. RIDE mode

This is the default.

Given:

```text
Alice right-clicks Bob
```

Alice should climb onto the **highest currently available entity in Bob's passenger chain**.

Example:

```text
Before:

Bob
↑ Charlie
↑ Dave
```

Alice right-clicks Bob:

```text
Bob
↑ Charlie
↑ Dave
↑ Alice
```

Use the same traversal semantics as the old:

```java
getHighestOrSelf(...)
```

implementation. 

---

# 19. PICK_UP mode

Inverse behavior.

Given:

```text
Alice
↑ Charlie
↑ Dave
```

Alice right-clicks Bob:

```text
Alice
↑ Charlie
↑ Dave
↑ Bob
```

The selected target is mounted onto the top of Alice's current passenger chain.

---

# 20. Stack-depth limits

Maintain both separate limits:

```text
stepUpLimit
pickUpLimit
```

Defaults:

```text
16
16
```

Do not merge them.

## RIDE

Use:

```text
stepUpLimit
```

## PICK_UP

Use:

```text
pickUpLimit
```

If traversal exceeds the configured limit:

```text
return failure
```

without changing the existing passenger graph.

---

# 21. Cycle prevention

Never allow:

```text
A rides B
B eventually rides A
```

or any deeper cycle.

While walking the passenger chain, if the candidate entity already appears in the proposed relationship:

```text
abort
```

before calling `startRiding`.

Preserve the current algorithm initially.

Do not attempt a large rewrite of vanilla passenger graph logic during the port.

---

# 22. Player eligibility

For target players:

```text
target spectator
→ not eligible

allowPlayers == false
→ not eligible

target Player Ladder disabled
→ not eligible
```

Both participant consent and server configuration must therefore agree.

---

# 23. Non-player entities

Current default:

```text
allowLivingEntities = false
```

so only players participate.

When enabled:

- honor configured entity ID exclusions
- honor configured entity-type tags
- preserve source behavior

### Important source audit

The old method is named:

```java
canPickUpOrRideLiving(...)
```

but the current implementation does not appear to enforce an explicit `instanceof LivingEntity` before applying the configuration check. 

The agent must inspect the source behavior carefully.

**Do not silently "fix" this during the port.**

If the original semantics permit non-living entities when `allowLivingEntities=true`, preserve that in the initial parity release and document it as a possible follow-up cleanup.

---

# 24. Crouch-to-dismount

Preserve:

```text
player is server-side
AND player is on ground
AND player is crouching
AND player currently has a passenger
```

then:

```text
first passenger stops riding
```

The old implementation does this every post-player-tick. 

## Implementation

Prefer a narrow:

```java
@Mixin(ServerPlayer.class)
```

injecting at:

```text
tick TAIL
```

rather than scanning every player globally.

Call a method such as:

```java
PlayerLadderHandler.handleCarrierTick(self);
```

Keep gameplay logic out of the mixin.

---

# 25. Logout cleanup

When a player leaves:

```text
if player is a passenger
AND vehicle instanceof Player
→ player.stopRiding()
```

Use the appropriate Fabric `ServerPlayerEvents.LEAVE` hook.

Fabric 26.2 exposes player lifecycle callbacks including join, leave, copy, and respawn events. 

This prevents stale player-to-player vehicle relationships during disconnect.

---

# 26. Death / respawn behavior

Because the consent attachment uses:

```java
.copyOnDeath()
```

the opt-in setting must survive death.

Test:

```text
/ladder toggle
die
respawn
/ladder status-equivalent verification
```

even if there is no public status command.

Passenger relationships must **not** survive death.

Vanilla should normally clean these up, but explicitly test:

```text
carrier dies
passenger dies
middle member of 3-player stack dies
```

Do not manually preserve the vehicle graph across respawn.

---

# 27. Game mode changes

The original NeoForge handler dismounts the carrier's first passenger when the player's game mode changes. 

Port this behavior.

First investigate whether Fabric 26.2 exposes a suitable player/game-mode callback.

If not:

use a narrow `ServerPlayer` or appropriate game-mode mixin at the exact 26.2 state transition method.

Behavior:

```text
player has passenger
AND game mode actually changes
→ first passenger stopRiding()
```

Do not poll game mode every tick.

---

# 28. Passenger synchronization audit

This part must be **investigated before porting**.

The old `EntityMixin` hooks:

```text
Entity#addPassenger
Entity#removePassenger
```

and manually sends a passenger packet when the vehicle is a `ServerPlayer`. 

Do not assume Minecraft 26.2 still needs this.

### Phase A

Implement stacking **without** this mixin.

### Phase B

Test:

```text
Alice rides Bob
Bob sees Alice correctly
Alice sees Bob correctly
Charlie observes both correctly
Alice dismounts
Bob dismounts Alice
three-player stack
middle passenger leaves
top passenger leaves
carrier reconnects
passenger reconnects
```

### If everything synchronizes correctly

Delete/omit `EntityMixin`.

### If the vehicle player's own client becomes stale

Inspect Minecraft 26.2:

```text
Entity
ServerEntity
ServerPlayer
ServerGamePacketListenerImpl
passenger tracking
ClientboundSetPassengersPacket equivalent
```

Then reproduce the minimum necessary workaround.

Do not blindly copy the old packet code.

---

# 29. `/ride` command extension

The old mod modifies vanilla `/ride` so players may be vehicles. 

Default config:

```json
"rideCommandExtension": true
```

## Investigation required

Inspect Minecraft 26.2:

```text
net.minecraft.server.commands.RideCommand
```

Find the exact logic that prevents mounting onto players.

Do not assume:

```java
Entity#getType()
```

is still the right injection point.

The old implementation redirects `getType()` and returns `null` to defeat the player check. That is a compatibility hack, not an architectural requirement. 

### Preferred 26.2 implementation

If the source shape permits it:

```java
@ModifyExpressionValue
```

or another MixinExtras expression hook around the actual boolean test.

Conceptually:

```java
originalPlayerVehicleRejected
```

becomes:

```java
originalPlayerVehicleRejected && !config.rideCommandExtension()
```

Do not alter unrelated `/ride` validation.

---

# 30. Interaction-through-passengers

This feature **must not be forgotten**.

Suppose:

```text
Alice
↑ Bob
```

Without special handling, Bob's hitbox can become the entity selected by Alice's own:

- attacks
- projectile targeting
- reach checks

The old `ProjectileUtilMixin` modifies the entity-hit predicate to ignore entities that are recursive passengers of the shooter. 

Port that behavior.

---

# 31. ProjectileUtil investigation

Inspect Minecraft 26.2's actual:

```text
ProjectileUtil
```

and locate the current equivalent of:

```java
getEntityHitResult(
    Entity shooter,
    Vec3 start,
    Vec3 end,
    AABB box,
    Predicate<Entity> predicate,
    double maxDistance
)
```

Do not copy the old descriptor blindly.

If the shape remains suitable, wrap the predicate.

Conceptual behavior:

```java
if (!config.allowInteractions()) {
    return originalPredicate;
}

if (shooter == null || !shooter.isVehicle()) {
    return originalPredicate;
}

return candidate ->
    originalPredicate.test(candidate)
    && !isRecursivePassenger(shooter, candidate);
```

---

# 32. Recursive passenger test

Implement allocation-free traversal:

```java
private static boolean isRecursivePassenger(Entity root, Entity candidate) {
    Entity vehicle = candidate.getVehicle();

    while (vehicle != null) {
        if (vehicle == root) {
            return true;
        }

        vehicle = vehicle.getVehicle();
    }

    return false;
}
```

Do not construct:

- temporary lists
- streams
- sets
- recursive lambdas

inside the hit-test hot path.

The current implementation explicitly avoids creating the wrapping predicate unless the shooter actually has passengers. 

Preserve that optimization.

---

# 33. Meaning of `allowInteractions`

Maintain:

```json
"allowInteractions": true
```

as:

> The carrier's own recursive passengers are ignored for relevant entity hit testing, allowing the carrier to interact normally while carrying someone.

When false:

do not alter vanilla targeting.

---

# 34. Mixins

Create:

```text
modern-player-ladder.mixins.json
```

Core mixins likely:

```text
ServerPlayerMixin
ProjectileUtilMixin
RideCommandMixin
```

Conditional:

```text
EntityMixin
```

Only if passenger sync testing proves necessary.

Use:

```json
"injectors": {
  "defaultRequire": 1
}
```

Core gameplay mixins should fail loudly when Minecraft changes.

Do not use:

```text
require = 0
```

for a core gameplay hook merely to make crashes disappear.

---

# 35. Mixin rules

Every mixin must:

1. target the smallest possible vanilla region
2. call into normal Java handler code where practical
3. avoid duplicating full vanilla methods
4. contain a comment explaining **why Fabric events cannot perform the job**
5. use descriptive namespaced handler names:

```java
modernPlayerLadder$...
```

6. avoid access wideners unless clearly better than a mixin accessor
7. avoid `@Overwrite`

No `@Overwrite` should be required.

---

# 36. No formal public API in v1

Do not create a published/stable:

```text
modern-player-ladder-api
```

module yet.

Internally, maintain clean service methods:

```java
PlayerLadderState.isEnabled(...)
PlayerLadderHandler.canRide(...)
PlayerLadderHandler.rideEntity(...)
PlayerLadderHandler.pickUpEntity(...)
```

but do not promise API compatibility until another mod actually needs it.

This keeps v1 small.

---

# 37. Translation keys

Do not hard-code player-visible command text directly into handler code.

Add:

```json
{
  "modern_player_ladder.message.enabled": "Player Ladder interactions enabled for you.",
  "modern_player_ladder.message.disabled": "Player Ladder interactions disabled for you."
}
```

Use translatable components.

This preserves the old English text while making the standalone mod localizable.

---

# 38. `fabric.mod.json`

Target:

```json
{
  "schemaVersion": 1,
  "id": "modern_player_ladder",
  "version": "${version}",
  "name": "Modern Player Ladder",
  "description": "Opt-in player stacking and riding for modern Minecraft.",
  "environment": "*",
  "entrypoints": {
    "main": [
      "town.lampas.modernplayerladder.ModernPlayerLadder"
    ]
  },
  "mixins": [
    "modern-player-ladder.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.19.3",
    "fabric-api": "*",
    "minecraft": "~26.2",
    "java": ">=25"
  }
}
```

Do not declare client entrypoints for v1.

---

# 39. Server-only compatibility target

Attempt to make the mod function when installed:

```text
server: Modern Player Ladder installed
client: Modern Player Ladder absent
```

because all functionality can potentially be implemented using vanilla interaction/passenger packets.

This is a **target**, not a claim until tested.

If it works:

document:

```text
Server-side installation supported.
Clients do not need the mod.
```

If 26.2 requires a client mixin for correct targeting/interaction behavior, then declare the client requirement instead.

Do not claim server-only compatibility without a real test.

---

# 40. Unit-testable code

Extract anything that does not need a live Minecraft world.

At minimum test:

### Config

```text
default config
malformed JSON
missing fields
invalid enum
negative limits
invalid entity identifier handling
```

### Utility logic where feasible

```text
mode parsing
configuration clamping
```

Avoid elaborate mocking of `Entity` just to achieve artificial unit-test coverage.

Entity graph behavior is better tested in GameTest/live Minecraft.

---

# 41. GameTests

If Fabric GameTest works cleanly on the 26.2 project, add tests for as much of this as practical:

```text
player attachment defaults false
attachment persists/copies
basic entity mount
stack traversal limit
```

Do not block the entire port on automating multiplayer-only behavior that cannot realistically be represented by GameTest.

Live multiplayer testing is mandatory anyway.

---

# 42. Build verification

At every major phase run:

```bash
./gradlew clean build
```

Final build must succeed with no:

```text
mixin target warnings
invalid descriptors
unresolved injection points
deprecated NeoForge references
```

Search the resulting source tree for:

```text
net.neoforged
NeoForge
ModConfigSpec
SubscribeEvent
PlayerInteractEvent
PlayerTickEvent
```

There should be none.

---

# 43. Development verification

Run:

```bash
./gradlew runServer
```

Verify:

```text
Modern Player Ladder initializes
config file created
no mixin errors
attachment registered
commands registered
dedicated server reaches Done
```

Also run a dev client/integrated server if supported:

```bash
./gradlew runClient
```

---

# 44. Multiplayer test matrix

Use at least two real clients.

Three is preferable because observer synchronization matters.

## Consent

```text
[ ] New player defaults OFF
[ ] A OFF / B OFF cannot stack
[ ] A ON / B OFF cannot stack
[ ] A OFF / B ON cannot stack
[ ] A ON / B ON can stack
```

## Commands

```text
[ ] /ladder toggle works
[ ] /playerladder toggle works
[ ] enable message is green
[ ] disable message is red
[ ] console cannot execute player toggle accidentally
```

## Persistence

```text
[ ] toggle ON
[ ] disconnect
[ ] reconnect
[ ] remains ON

[ ] die
[ ] respawn
[ ] remains ON

[ ] restart dedicated server
[ ] remains ON
```

---

# 45. Interaction tests

```text
[ ] main-hand empty → ladder interaction
[ ] item in main hand → no ladder interaction
[ ] off-hand does not trigger ladder
[ ] spectator initiator cannot stack
[ ] spectator target cannot stack
```

Also test:

```text
villager
boat
minecart
armor stand
hostile mob
passive mob
modded entity
```

when entity stacking is enabled.

Confirm vanilla interactions are not inadvertently swallowed when Player Ladder returns `PASS`.

---

# 46. Stack tests

Test:

```text
A
```

then:

```text
A
↑ B
```

then:

```text
A
↑ B
↑ C
```

then at least:

```text
5-player stack
```

if enough test accounts/entities are available.

Verify:

```text
movement
rotation
teleportation
dimension transitions if permitted by vanilla
dismount
disconnect
death
```

---

# 47. Limit tests

Set:

```json
"stepUpLimit": 2
```

verify exactly where RIDE mode stops.

Set:

```json
"pickUpLimit": 2
```

verify exactly where PICK_UP stops.

Do not just test default 16.

Boundary cases:

```text
limit - 1
limit
limit + 1
```

---

# 48. Crouch dismount tests

Verify:

```text
carrier in air + crouch
→ no forced passenger dismount

carrier grounded + not crouching
→ no dismount

carrier grounded + crouching
→ first passenger dismounts
```

Test nested stacks.

Ensure only the intended first passenger is removed according to source parity.

---

# 49. Toggle-off tests

Given:

```text
Alice
↑ Bob
↑ Charlie
```

toggle Alice off.

Verify the source semantics carefully.

The original toggle explicitly dismounts all **direct** passengers from the toggling player and also dismounts the toggling player if they are themselves a passenger. 

Ensure no ghost passenger relationships remain.

---

# 50. Logout tests

Test each role leaving:

```text
bottom carrier leaves
middle player leaves
top player leaves
observer leaves
```

No remaining client should see a ghost player.

No server entity should retain an invalid vehicle UUID/reference.

---

# 51. Passenger synchronization tests

With observer C:

```text
A rides B
```

C must immediately see it.

Then:

```text
A dismounts
```

C must immediately see it.

Also test the **vehicle player's own client**, because that is what the old manual passenger packet workaround appears particularly concerned with.

Only after these tests should the agent decide whether `EntityMixin` is needed.

---

# 52. Interaction-through-passenger tests

Given:

```text
Alice
↑ Bob
```

and:

```json
"allowInteractions": true
```

Alice must be able to:

```text
attack a mob in front of her
shoot a bow without targeting Bob
perform relevant reach/entity interactions
```

Bob must not be selected merely because he is Alice's passenger.

Test recursive case:

```text
Alice
↑ Bob
↑ Charlie
```

Both Bob and Charlie should be excluded from Alice's relevant hit test.

---

# 53. `/ride` tests

With:

```json
"rideCommandExtension": true
```

verify relevant vanilla commands permit player vehicles.

With:

```json
"rideCommandExtension": false
```

vanilla behavior must be restored exactly.

Do not weaken any unrelated `/ride` errors.

---

# 54. Configuration tests

Test:

```text
allowPlayers=false
allowLivingEntities=true
rightClickMode=RIDE
rightClickMode=PICK_UP
rightClickMode=DO_NOTHING
allowInteractions=false
rideCommandExtension=false
```

Test entity exclusion:

```json
"excludedLivingEntities": [
  "minecraft:pig"
]
```

Pig must be rejected.

Test tag exclusion:

```text
#minecraft:...
```

using a real 26.2 entity-type tag.

---

# 55. Performance rules

Player Ladder should have essentially zero idle cost.

### No:

```text
global every-player scans
global every-entity scans
per-tick registry lookups
per-tick JSON reads
per-hit-test collection allocation
```

### Allowed hot path

`ServerPlayer#tick`:

```text
a handful of booleans
```

before immediately returning.

### Projectile hot path

Return the original predicate immediately unless:

```text
allowInteractions == true
AND shooter has passengers
```

This preserves the optimization already present in the old mod. 

---

# 56. Do not blindly port old hacks

These three areas **must be audited against actual Minecraft 26.2 source**:

### 1. Passenger packet workaround

```text
EntityMixin
```

May no longer be required.

### 2. `/ride` hack

```text
RideCommandMixin
```

The exact check may have changed.

### 3. Projectile hit-result descriptor

```text
ProjectileUtilMixin
```

The method descriptor may have changed.

The gameplay requirement is authoritative.

The old injection point is not.

---

# 57. Minecraft source inspection checklist

Before adding each mixin, inspect the 26.2 source for:

```text
Entity
ServerPlayer
RideCommand
ProjectileUtil
ServerGamePacketListenerImpl
ServerEntity
ClientboundSetPassengersPacket or current equivalent
```

Record in comments/commit notes:

```text
target method
target descriptor
why injection is needed
what vanilla behavior is being changed
```

Do not guess descriptors from an older Minecraft version.

---

# 58. Implementation phases

## Phase 1 — Bootstrap

Create the Fabric 26.2 project.

Deliver:

```text
build.gradle
gradle.properties
settings.gradle
fabric.mod.json
ModernPlayerLadder.java
```

Success criterion:

```bash
./gradlew build
```

passes.

---

## Phase 2 — Configuration

Implement:

```text
ClickMode
PlayerLadderConfig
JSON load/create
validation
entity exclusion cache
```

Success:

server starts and creates valid default config.

---

## Phase 3 — Persistent consent

Implement:

```text
AttachmentType<Boolean>
PlayerLadderState
```

Success:

```text
new player OFF
toggle state persists restart
state survives respawn
```

---

## Phase 4 — Commands

Implement:

```mcfunction
/ladder toggle
/playerladder toggle
```

including:

- messages
- immediate dismount on disable

---

## Phase 5 — Core stacking logic

Port:

```text
rideEntity
pickUpEntity
target eligibility
stack traversal
cycle detection
limits
```

Keep these in `PlayerLadderHandler`.

No mixin logic beyond glue.

---

## Phase 6 — Right-click hook

Hook entity use with the correct Fabric event or narrow server mixin.

Verify:

```text
empty-hand behavior
vanilla interaction cancellation
mutual consent
```

---

## Phase 7 — Lifecycle behavior

Implement:

```text
crouch dismount
logout cleanup
game-mode cleanup
```

---

## Phase 8 — Projectile/interaction passthrough

Audit 26.2 `ProjectileUtil`.

Implement recursive passenger exclusion.

---

## Phase 9 — `/ride` extension

Audit 26.2 `RideCommand`.

Implement minimal conditional bypass.

---

## Phase 10 — Passenger sync investigation

Test without workaround.

Only add `EntityMixin` if objectively required.

---

## Phase 11 — Tests

Implement:

```text
unit tests
GameTests where worthwhile
dedicated server test
multiplayer live test
```

---

## Phase 12 — Documentation

README should explain:

### What it does

```text
Right-click consenting players with an empty hand to form player stacks.
```

### Consent

```text
Both players must opt in.
```

### Commands

```mcfunction
/ladder toggle
/playerladder toggle
```

### Dismounting

Explain:

```text
grounded crouch
toggle off
```

### Config

Document every JSON setting.

### Installation

State accurately whether:

```text
server-only
```

or:

```text
client + server
```

is required, based only on testing.

---

# 59. Version 1 non-goals

Do **not** add during the initial port:

- config screen
- Mod Menu support
- keybind toggle
- permission plugin integration
- command suggestions beyond normal Brigadier
- GUI indicators
- HUD icon
- custom networking
- public API module
- cross-loader support
- NeoForge version
- Forge version
- automatic update checker
- analytics
- arbitrary stack positioning
- custom rider animations

Those can come later.

---

# 60. Optional post-parity improvements

Only after the port is fully verified:

### `/ladder status`

```mcfunction
/ladder status
```

### Explicit commands

```mcfunction
/ladder on
/ladder off
```

### Admin inspection

```mcfunction
/ladder status <player>
```

### Config reload

```mcfunction
/modernplayerladder reload
```

with permission level 2.

### Config UI

YACL/Mod Menu if actually useful.

### Public API

If another mod wants integration.

None belong in the first parity implementation.

---

# 61. Acceptance criteria

The implementation is complete only when all of these are true:

```text
[ ] Standalone Modern Player Ladder repo builds on Minecraft 26.2 / Java 25.

[ ] No NeoForge dependency or source remains.

[ ] New players are opted OUT by default.

[ ] /ladder toggle enables and disables Player Ladder.

[ ] /playerladder toggle remains available.

[ ] Consent survives logout.

[ ] Consent survives server restart.

[ ] Consent survives death/respawn.

[ ] Both players must opt in before player/player stacking works.

[ ] Empty main hand is required.

[ ] Spectators cannot participate.

[ ] RIDE mode works.

[ ] PICK_UP mode works.

[ ] DO_NOTHING works.

[ ] stepUpLimit is enforced.

[ ] pickUpLimit is enforced.

[ ] Cycles cannot be created.

[ ] Grounded crouch dismounts the carrier's first passenger.

[ ] Disabling Player Ladder immediately breaks relevant mount relationships.

[ ] Logging out does not leave stale passenger relationships.

[ ] Game-mode changes preserve old cleanup behavior.

[ ] Entity exclusions by ID work.

[ ] Entity exclusions by tag work.

[ ] allowPlayers works.

[ ] allowLivingEntities works according to documented source-parity semantics.

[ ] allowInteractions correctly ignores recursive passengers.

[ ] /ride extension can be independently enabled/disabled.

[ ] Two clients agree on every passenger relationship.

[ ] Third-party observer sees stacks correctly.

[ ] No ghost riders remain after dismount/logout/death.

[ ] No manual passenger-packet mixin exists unless testing proved it necessary.

[ ] Dedicated server starts without mixin warnings/errors.

[ ] ./gradlew clean build succeeds.

[ ] README accurately describes installation, commands and configuration.
```

---

# 62. Agent hard rules

Give the coding agent these rules verbatim:

> **1. Behavior parity is more important than mechanically preserving old code.**
>
> **2. Inspect the original `lampas-overrides` Player Ladder source before implementing each corresponding subsystem.**
>
> **3. Inspect Minecraft 26.2 source before writing any vanilla mixin. Do not guess method descriptors or assume old injection points still exist.**
>
> **4. Prefer Fabric APIs/events when they provide the exact semantics required. Use mixins where Fabric events cannot reproduce the behavior correctly.**
>
> **5. Keep the implementation server-authoritative. Do not add custom networking unless a demonstrated Minecraft 26.2 limitation makes it necessary.**
>
> **6. Do not add the old `Entity#addPassenger/removePassenger` packet workaround unless multiplayer testing proves Minecraft 26.2 still has the synchronization issue.**
>
> **7. Do not blindly port the old `/ride` null-`EntityType` hack. Patch the actual 26.2 player-vehicle restriction as narrowly as possible.**
>
> **8. Do not silently change the semantics of `allowLivingEntities`; first establish exactly what the existing implementation permits.**
>
> **9. Core mixins must fail loudly if their target changes. Do not hide broken gameplay behind `require = 0`.**
>
> **10. Keep hot paths allocation-light. Do not scan all players/entities each tick.**
>
> **11. Do not add unrelated features while porting. Finish and verify parity first.**
>
> **12. A successful Gradle build is not sufficient verification. Perform real multiplayer tests with at least two clients before declaring the port complete.**

## Final intended architecture

The finished v1 should therefore be surprisingly small:

```text
Fabric APIs
├── commands
├── persistent attachment
├── player lifecycle
└── entity interaction if semantics are sufficient

Modern Player Ladder logic
├── consent
├── config
├── stack traversal
├── ride
└── pickup

Vanilla mixins
├── ServerPlayer tick       ← crouch dismount
├── ProjectileUtil          ← ignore own passengers
├── RideCommand             ← player vehicle support
└── Entity passenger sync   ← ONLY if proven necessary
```

That gives **Modern Player Ladder** a much cleaner foundation than copying the NeoForge implementation wholesale: the actual gameplay remains the same, while loader-specific workarounds are individually revalidated against Minecraft 26.2 instead of being carried forward forever.