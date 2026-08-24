# Modern Player Ladder

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-brightgreen.svg)](https://minecraft.net/)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-0.158.0%2B26.2-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MPL--2.0-blue.svg)](LICENSE)

**Modern Player Ladder** is a Fabric gameplay mod for Minecraft 26.2 that lets consenting players form rider stacks. With the default configuration, opt in with `/ladder toggle`, keep your main hand empty, and right-click another opted-in player to climb onto their passenger chain.

The server owns consent and passenger state and uses vanilla riding and passenger packets. Dedicated servers can install Modern Player Ladder without requiring the mod on connecting clients.

---

## 🌟 Key Features

- **Mutual Consent**: Player-to-player stacking works only when both players have opted in.
- **Persistent Preference**: Each player's toggle survives reconnects and is copied across death.
- **Recursive Player Stacks**: New riders are placed at the available end of an existing passenger chain.
- **Two Interaction Directions**: Choose whether the acting player rides the target or picks the target up.
- **Lifecycle Cleanup**: Dismounts are synchronized across crouching, toggling off, game-mode changes, logout, and death.
- **Passenger Passthrough**: Carriers can target and fire projectiles through their own recursive passenger stack when enabled.
- **Vanilla `/ride` Extension**: Player entities can be used as `/ride` vehicles without replacing the rest of vanilla command validation.
- **Optional Entity Handling**: Non-player entities can participate when enabled, with configurable entity and tag exclusions.
- **Server-Only Support**: Live multiplayer testing verified that clients do not need Modern Player Ladder installed.

---

## 🪜 Consent & Usage

Player Ladder is opt-in by default. Each participating player runs either toggle command:

```mcfunction
/ladder toggle
/playerladder toggle
```

Then, with an empty main hand, right-click another opted-in player. Both players must be non-spectators, player interactions must be enabled in the server config, and the configured stack-depth limit must not be exceeded.

Running the toggle command again disables Player Ladder for that player and immediately breaks their active rider and vehicle relationships.

---

## 🔄 Interaction Modes

The server-wide `rightClickMode` controls how an accepted right-click changes the passenger graph.

| Mode | Behavior |
| :--- | :--- |
| **`RIDE`** | The acting player climbs onto the highest available entity in the target's passenger chain. This is the default. |
| **`PICK_UP`** | The selected target mounts the highest available entity in the acting player's passenger chain. |
| **`DO_NOTHING`** | Player Ladder does not handle right-clicks and leaves the interaction to vanilla. |

---

## 🧍 Dismounting & Cleanup

- A passenger can use the normal vanilla dismount control.
- A grounded carrier can crouch to dismount their first direct passenger.
- Toggling Player Ladder off immediately dismounts the player and their direct passengers.
- Changing a carrier's game mode dismounts their first direct passenger.
- Logout and death clean up player-vehicle relationships for all connected clients.

---

## ⚙️ Configuration

Configuration is created on first startup at:

```text
config/modern-player-ladder.json
```

Restart the server after editing the file. Invalid values fall back to their corresponding defaults.

| Option | Default | Description |
| :--- | :---: | :--- |
| `rightClickMode` | `"RIDE"` | Right-click behavior: `RIDE`, `PICK_UP`, or `DO_NOTHING`. |
| `pickUpLimit` | `16` | Maximum passenger-chain traversal depth used by `PICK_UP` mode. Must be at least `1`. |
| `stepUpLimit` | `16` | Maximum passenger-chain traversal depth used by `RIDE` mode. Must be at least `1`. |
| `allowLivingEntities` | `false` | Allows the optional non-player entity interaction path. |
| `allowPlayers` | `true` | Globally allows players to participate; individual mutual consent is still required. |
| `excludedLivingEntities` | See below | Entity IDs or `#tag` IDs rejected by the optional entity interaction path. |
| `rideCommandExtension` | `true` | Allows vanilla `/ride` commands to use players as vehicles. Disabling it restores vanilla's player-vehicle rejection. |
| `allowInteractions` | `true` | Ignores a carrier's recursive passengers during relevant entity hit testing so the carrier can interact and fire projectiles normally. |

The default exclusions are:

```json
[
  "minecraft:wither",
  "minecraft:ender_dragon",
  "minecraft:minecart",
  "#minecraft:boat",
  "#minecraft:dismounts_underwater"
]
```

Entries beginning with `#` are entity-type tags; other entries are entity-type IDs.

---

## 🛠️ Commands

- `/ladder toggle`
  - Enables or disables Player Ladder interactions for the executing player.
- `/playerladder toggle`
  - Compatibility alias for `/ladder toggle`.

The commands are player-only. Vanilla `/ride` permissions and validation remain unchanged except for the configurable player-vehicle rejection.

---

## 📦 Installation

For a dedicated server, install:

- Fabric Loader `0.19.3` or newer compatible version
- Fabric API for Minecraft 26.2
- Modern Player Ladder

Modern Player Ladder is server-side compatible: connecting clients do not need this mod installed. This was verified with two real mod-absent Minecraft 26.2 clients performing opt-in, mounting, passenger synchronization, and dismounting against a modded dedicated server.

For singleplayer, install the mod and Fabric API in the client instance because the client hosts the integrated server.

---

## 🧱 Architecture

Modern Player Ladder keeps gameplay server-authoritative:

```text
vanilla right-click packet
        ↓
server consent + config checks
        ↓
vanilla entity passenger graph
        ↓
vanilla passenger synchronization
```

The mod adds no custom client networking. Focused mixins cover only Minecraft 26.2 gaps for player vehicles, carrier-self passenger synchronization, lifecycle cleanup, projectile hit testing, and the `/ride` player check.

---

## 📦 Building from Source

```bash
git clone https://github.com/justbecauseph/modern-player-ladder.git
cd modern-player-ladder
./gradlew build
```

The compiled mod JAR will be located in:

```text
build/libs/
```

The project targets:

- Minecraft `26.2`
- Fabric Loader `0.19.3`
- Fabric API `0.158.0+26.2`
- Java `25`

---

## 📄 License

Modern Player Ladder is licensed under the [Mozilla Public License 2.0 (MPL-2.0)](LICENSE).
