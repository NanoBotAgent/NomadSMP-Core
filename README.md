# NomadSMP-Core

A PaperMC plugin for curated SMP servers (<=20 players). Keeps the server alive, social, fair, and fun.

## Modules

1. **Nomad System** - Weekly house migration (FAWE-powered)
2. **Daily Buffs** - 50 unique daily effects with weekday/weekend scheduling
3. **Progression Lock** - End lockdown, Netherite ban, command blocking
4. **Anti-Cheat** - Seed protection, ore obfuscation, structure seed scrambling
5. **Social** - World border, player head drops, teleport disable, OP stripping

## Build

```bash
./gradlew shadowJar
```

Output: `build/libs/NomadSMP-Core-1.0.0.jar`

## Commands

| Command | Description |
|---------|-------------|
| `/nomad reload` | Reload config |
| `/nomad status` | View active buffs, border, migration schedule |
| `/nomad migratenow` | Force-trigger weekly migration |
| `/nomad setbuff <id>` | Override today's buff |
| `/nomad sethome` | Set your nomad home location |

## Requirements

- PaperMC 26.1+ (Java 25)
- FastAsyncWorldEdit (FAWE)

## License

MIT
