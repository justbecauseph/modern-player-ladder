# Modern Player Ladder

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-brightgreen.svg)](https://minecraft.net/)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-0.158.0%2B26.2-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MPL--2.0-blue.svg)](LICENSE)

Ever wanted to stack players on top of each other? **Modern Player Ladder** lets you do exactly that. Everyone opts in first, then you right-click another player with an empty hand. That's it.

Running a dedicated server? You only need the mod on the server. Players can join without installing it themselves.

---

## 🌟 Key Features

- **Opt-In by Default**: Nobody gets picked up unless both players have enabled the feature.
- **Stack Up**: Keep adding players to build a whole tower.
- **Ride or Pick Up**: Choose whether you climb onto the player you click or put them on top of you.
- **Your Choice Sticks**: Your toggle is saved across reconnects and death.
- **Easy Dismounting**: Sneaking, toggling off, changing game mode, logging out, and dying all clean up rides normally.
- **Keep Playing Normally**: Aim, attack, and fire projectiles through the players riding on you.
- **Optional Mob Support**: Let other entities join the fun, with a configurable exclusion list.
- **Vanilla `/ride` Support**: Server admins can use players as vehicles with the normal `/ride` command.
- **Server-Side Friendly**: Players joining a dedicated server do not need to install the mod.

---

## 🪜 Consent & Usage

Player Ladder starts disabled for everyone. To use it:

1. Each participating player runs either toggle command:

```mcfunction
/ladder toggle
/playerladder toggle
```

2. Make sure your main hand is empty.
3. Right-click another opted-in player.
4. Keep adding friends until you hit the configured stack limit.

Run the command again whenever you want to opt out. Toggling off immediately dismounts you and anyone riding directly on you.

---

## 🔄 Interaction Modes

The server-wide `rightClickMode` decides who ends up on top.

| Mode | Behavior |
| :--- | :--- |
| **`RIDE`** | You climb onto the player you click—or onto the top of their existing stack. This is the default. |
| **`PICK_UP`** | The player you click gets placed on top of you or your existing stack. |
| **`DO_NOTHING`** | Player Ladder ignores right-clicks and leaves them to vanilla. |

---

## 🧍 Dismounting & Cleanup

- Riders can use the normal vanilla dismount control.
- A carrier standing on the ground can crouch to drop the first player riding on them.
- Toggling Player Ladder off dismounts you and your direct riders.
- Changing game mode drops your first direct rider.
- Logging out or dying clears the ride cleanly for everyone.

---

## ⚙️ Configuration

Configuration is created on first startup at:

```text
config/modern-player-ladder.json
```

Restart the server after editing the file. Invalid values fall back to their corresponding defaults.

| Option | Default | Description |
| :--- | :---: | :--- |
| `rightClickMode` | `"RIDE"` | Chooses who ends up on top: `RIDE`, `PICK_UP`, or `DO_NOTHING`. |
| `pickUpLimit` | `16` | How far `PICK_UP` can search through an existing stack. Must be at least `1`. |
| `stepUpLimit` | `16` | How far `RIDE` can search through an existing stack. Must be at least `1`. |
| `allowLivingEntities` | `false` | Lets non-player entities participate too. |
| `allowPlayers` | `true` | Master switch for player stacking. Players still need to opt in individually. |
| `excludedLivingEntities` | See below | Entity IDs or `#tag` IDs that cannot participate when entity support is enabled. |
| `rideCommandExtension` | `true` | Lets vanilla `/ride` commands use players as vehicles. Turn it off for vanilla behavior. |
| `allowInteractions` | `true` | Lets carriers aim, interact, and fire projectiles through the players riding on them. |

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
  - Turns Player Ladder on or off for you.
- `/playerladder toggle`
  - Does the same thing; this name is kept for compatibility.

Both commands are player-only. If `rideCommandExtension` is enabled, admins can also use the normal vanilla `/ride` command with a player as the vehicle. Normal `/ride` permissions still apply.

---

## 📦 Installation

For a dedicated server, install:

- Fabric Loader `0.19.3` or newer compatible version
- Fabric API for Minecraft 26.2
- Modern Player Ladder

Connecting players do not need Modern Player Ladder installed. Just add it to the dedicated server alongside Fabric API.

For singleplayer, install the mod and Fabric API in the client instance because the client hosts the integrated server.

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
