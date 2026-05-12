# 🏕️ NomadSMP-Core

**A modular PaperMC plugin for curated SMP servers (≤20 players).** 
Keeps the server alive, social, fair, and fun — with zero admin babysitting.

Built for **Paper 26.1+** (Java 25). Designed for small whitelisted communities that want structured progression, daily variety, and anti-cheat out of the box.

---

## ✨ Features at a Glance

| Module | What It Does |
|---|---|
| 🏠 **Nomad System** | Weekly house migration via FAWE — no one stays in one base forever |
| 🧪 **Daily Buffs** | 51 unique effects on a per-day schedule (fixed, random, or off) |
| 🔒 **Progression Lock** | End lockdown, Netherite ban, command blocking — timed unlocks |
| 🛡️ **Anti-Cheat** | Seed protection, ore obfuscation, structure seed scrambling |
| 🤝 **Social** | World border, head drops, teleport disable, OP stripping |

Every module can be toggled independently in `config.yml`. All gameplay values are configurable — nothing is hardcoded.

---

## 📦 Requirements

- **PaperMC** 26.1+ (Java 25)
- **FastAsyncWorldEdit (FAWE)** 2.15.1+ or **WorldEdit** 7.4.3 (required for Nomad house migration)

> **Downloads:**
> - [FAWE Paper 2.15.1-SNAPSHOT for MC 26.1](https://github.com/roggy666/FastAsyncWorldEdit-26.1/releases/download/26.1/FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT.jar) — unofficial 26.1 build by [roggy666](https://github.com/roggy666/FastAsyncWorldEdit-26.1) (recommended for async performance)
> - [WorldEdit 7.4.3 (Bukkit/Paper)](https://cdn.modrinth.com/data/1u6JkXh5/versions/yDUBafTJ/worldedit-bukkit-7.4.3.jar) — upstream stable release

---

## 🔨 Build

```bash
./gradlew shadowJar
```

Output: `build/libs/NomadSMP-Core-1.0.2.jar`

Drop the JAR into your server's `plugins/` folder. Default config is generated on first run.

---

## ⌨️ Commands

All commands are under `/nomad`:

| Command | Permission | Description |
|---|---|---|
| `/nomad reload` | `nomad.admin` | Hot-reload config without restart |
| `/nomad status` | `nomad.admin` | Show all modules, active buffs, border, schedule |
| `/nomad stats` | `nomad.admin` | Server uptime, buff activations, migration count, timber/vein uses |
| `/nomad buffs` | everyone | Open buff GUI menu (players) or list in chat (console) |
| `/nomad setbuff <id...>` | `nomad.admin` | Override today's buff (broadcasts to all players) |
| `/nomad setbuffpool <day> <mode> <ids>` | `nomad.admin` | Set per-day buff schedule (`fixed`/`random`/`off`) |
| `/nomad setborder <size> [cx] [cz]` | `nomad.admin` | Resize world border live |
| `/nomad migratenow` | `nomad.admin` | Force-trigger a house migration |
| `/nomad sethome` | everyone | Set your nomad home location |

Tab completion is available for all subcommands, days, and modes.

---

## 🧪 Daily Buffs — Full Reference

51 unique buffs, each with its own config section. Every buff can be individually **enabled/disabled** and has **tunable parameters** — amplifier levels, multipliers, ranges, chances, etc.

### How It Works

- **Per-day schedule**: Each day of the week can be `fixed`, `random`, or `off`
  - `fixed` → always applies specific buff IDs (e.g. Monday = Titanium)
  - `random` → picks N buffs from a configurable pool (e.g. Saturday = random from all 51)
  - `off` → no buff that day
- **Rollover**: Automatic midnight check swaps buffs at 00:00
- **Duration**: Set `duration-hours` to limit how long buffs last (0 = all day)
- **Stacking**: Configure how same-type effects combine (`highest`, `additive`, `replace`)
- **Persistence**: Buff state survives server restarts
- **Broadcast**: All online players are notified on buff changes and when joining

### All 51 Buffs

| # | Name | Effect | Key Config |
|---|---|---|---|
| 1 | **Titanium** | No tool durability loss | — |
| 2 | **Power Miner** | Haste effect | `haste-amplifier` (0=I, 1=II...) |
| 3 | **Roadrunner** | Speed boost (no FOV change) | `speed-modifier` |
| 4 | **Featherweight** | Slow Falling | `amplifier` |
| 5 | **Iron Lung** | Water Breathing | `amplifier` |
| 6 | **Pyro** | Fire Resistance | `amplifier` |
| 7 | **Night Owl** | Night Vision | `amplifier` |
| 8 | **Looter** | Double mob drops | `drop-multiplier` (2=2×, 3=3×) |
| 9 | **Bountiful Harvest** | Double crop growth | `growth-boost` (auto bone-meal) |
| 10 | **Lucky Fisher** | Instant fishing | — |
| 11 | **Magnet** | Auto-pickup items | `range`, `pull-strength` |
| 12 | **Chef** | Cancel hunger drain | `cancel-hunger` |
| 13 | **Blacksmith** | Auto-smelt raw ores | `auto-smelt` |
| 14 | **Timber** | Tree feller (chop whole tree) | `max-blocks` |
| 15 | **Vein Miner** | Mine entire ore vein | `max-blocks` |
| 16 | **Trophy Hunter** | Mob head drops | `head-drop-chance` (0.0–1.0) |
| 17 | **Dolphin** | Swim speed + Dolphin's Grace | `speed-modifier` |
| 18 | **Gravity** | Jump Boost | `jump-amplifier` |
| 19 | **Vampire** | Heal on kill | `heal-per-kill` |
| 20 | **XP Junkie** | Double XP | `xp-multiplier` |
| 21 | **Merchant** | Villager trade discounts | `discount` (0.5 = 50% off) |
| 22 | **Tank** | Resistance | `resistance-amplifier` |
| 23 | **Glass Cannon** | 2× damage dealt & taken | `damage-dealt-multiplier`, `damage-taken-multiplier` |
| 24 | **Archer** | Free arrows | `free-arrows` |
| 25 | **Librarian** | 1-level enchanting | `enchant-cost` |
| 26 | **Ninja** | Invisibility + silent | `amplifier` |
| 27 | **Spider** | Wall climb when sneaking | `climb-velocity` |
| 28 | **Ender** | No ender pearl damage | `pearl-fall-window-ms` |
| 29 | **Healthy** | +5 extra hearts | `extra-hearts` (2.0 = 1 heart) |
| 30 | **Medic** | Regeneration | `regen-amplifier` |
| 31 | **Sonic** | Speed III (no FOV change) | `speed-modifier` |
| 32 | **Rich** | Gold nugget drops | `nugget-chance` (0.0–1.0) |
| 33 | **Scavenger** | Bonus loot from grass | `drop-chance`, `loot` list |
| 34 | **Glowstick** | Glowing effect | `amplifier` |
| 35 | **Gardener** | 3×3 bone meal | `bone-meal-radius` (1=3×3, 2=5×5) |
| 36 | **Snowman** | Snow trail + fire immune | `snow-trail`, `fire-immune` |
| 37 | **Friendly** | Peaceful mob attraction | `attraction-range` |
| 38 | **Slimy** | Bouncy fall + launch | `bounce-multiplier` |
| 39 | **Thor** | Lightning on hit | `lightning-chance` (0.0–1.0) |
| 40 | **Teleporter** | Sneak-teleport forward | `distance`, `cooldown-ms` |
| 41 | **Parachute** | Slow Falling | `amplifier` |
| 42 | **Unstoppable** | Immune to Wither/Slow/Blind | `immune-causes` list |
| 43 | **Warrior** | Strength | `strength-amplifier` |
| 44 | **Inertia** | Knockback immune above threshold | `velocity-threshold` |
| 45 | **Gravity Well** | Pull mobs toward you | `range`, `pull-strength` |
| 46 | **Alchemist** | 3× potion duration | `duration-multiplier` |
| 47 | **Builder** | 20% block refund chance | `refund-chance` (0.0–1.0) |
| 48 | **Double Jump** | Double jump in survival | `velocity` |
| 49 | **Whale** | Infinite oxygen underwater | `infinite-oxygen` |
| 50 | **Pacifist** | Swords deal 0 damage + Regen IV | `regen-amplifier` |
| 51 | **Leaf Cut** | Break all connected leaves at once | `max-leaves` |

> **Potion amplifiers are 0-indexed!** `0` = Level I, `1` = Level II, etc. 
> Speed buffs use `walkSpeed` attribute — **no FOV zoom**.

---

## 🏠 Nomad System

Forces weekly house migration to keep the map fresh and prevent mega-base stagnation.

- **Migration day**: Configurable (default: Monday at 00:00)
- **House radius**: Detects player structures within N blocks of their home
- **Safe land check**: Ensures new location has minimum Y and solid ground
- **Warning**: Players are warned N minutes before migration
- FAWE/WorldEdit pastes the house at the new random location

Config:
```yaml
nomad-system:
  enabled: true
  migrate-day: MONDAY
  migrate-hour: 0
  house-radius: 16
  safe-land-check: true
  min-safe-y: 60
  migration-warning-minutes: 5
```

---

## 🔒 Progression Lock

Prevents speedrunning and keeps late-game content locked until the server has aged.

| Feature | Description | Config |
|---|---|---|
| **End Lock** | End portal is disabled for N days | `unlock-days`, `server-start-date` |
| **Netherite Crafting Ban** | Blocks Netherite ingot crafting | — |
| **Netherite Equip Punish** | Slowness V if Netherite armor is worn | `duration-ticks`, `amplifier` |
| **Block Gamemode** | Prevents `/gamemode` command | — |
| **Block Give** | Prevents `/give` command | — |

---

## 🛡️ Anti-Cheat

Built-in protection without needing separate anti-X-ray plugins.

| Feature | Description |
|---|---|
| **Block Seed Command** | Prevents `/seed` from leaking world seed |
| **Scramble Structure Seeds** | Randomizes structure generation seeds |
| **Ore Obfuscation** | Hides real ore positions from X-ray clients |

---

## 🤝 Social

Community-shaping features for small servers.

| Feature | Description | Config |
|---|---|---|
| **World Border** | Configurable size, center, damage, warning distance | `size`, `center-x`, `center-z`, `damage-amount`, `warning-distance` |
| **Disable TPA** | Blocks teleport requests | — |
| **Disable Warp** | Blocks warp commands | — |
| **Disable Home** | Blocks `/home` teleport (use `/nomad sethome` instead) | — |
| **Player Head Drop** | Players drop their head on death | — |
| **No Admin OP** | Strips OP status from all players on join | — |

---

## ⚙️ Configuration

All settings live in `plugins/NomadSMP-Core/config.yml`. Every module, sub-feature, and gameplay value is configurable — no hardcoded numbers.

Key config sections:
- `nomad-system` — migration schedule, house radius, safety checks
- `daily-buffs` — schedule, duration, stacking, per-buff tuning (51 entries)
- `progression-lock` — end unlock timer, Netherite bans, command blocking
- `anti-cheat` — seed protection, ore obfuscation, structure scrambling
- `social` — world border, teleport rules, head drops, OP stripping

Config is **auto-migrated** on updates — new keys from the default config are merged into your existing config without overwriting custom values. A backup of the previous config is saved as `config.yml.bak`.

Use `/nomad reload` to apply changes without restarting the server.

---

## 🏗️ Project Structure

```
com.nomadsmp.core/
├── NomadCore.java            # Main plugin class
├── commands/
│   └── NomadCommand.java     # All /nomad subcommands + tab completion
├── config/
│   ├── ConfigManager.java    # Config parsing & defaults
│   └── ConfigMigrator.java   # Auto-migration of config on version changes
├── listeners/
│   ├── BuffListeners.java    # Buff effect listeners
│   ├── ProgressionListeners.java
│   ├── AntiCheatListeners.java
│   └── SocialListeners.java
├── modules/
│   ├── DailyBuffModule.java  # Buff scheduling & rollover
│   ├── BuffApplier.java      # Buff effect application/removal
│   ├── NomadModule.java      # House migration logic
│   ├── ProgressionLockModule.java
│   ├── AntiCheatModule.java
│   └── SocialModule.java
└── utils/
    ├── HomeStorage.java      # Per-player home persistence
    ├── StatsManager.java     # Server statistics tracking
    └── BuffStateStorage.java # Buff state persistence across restarts
```

---

## 📄 License

MIT
