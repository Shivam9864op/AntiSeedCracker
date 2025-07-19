# AntiSeedCracker

A Spigot plugin making an effort to work against seed cracking mods.

## Requirements
- ProtocolLib 5.3+
- Spigot (and forks) 1.20.4 - 1.21 - Make sure to update to the latest commit/version

*The plugin will - to a degree - check for old versions of ProtocolLib and warn you once on the first player join.*

## Features
- Randomization of hashed seed on login/respawn/world change
- **Biome data obfuscation to counteract seed cracking tools**
- Modification of end spikes
- Modification of end cities

### Biome Data Obfuscation

The plugin can intercept and obfuscate biome data sent to clients to prevent automated seed cracking tools like SeedcrackerX from extracting accurate biome information. This feature:

- Detects suspicious biome probing patterns (rapid requests, login/respawn timing)
- Randomizes biome data for suspicious requests while preserving vanilla gameplay
- Is configurable and can be toggled on/off
- Includes protection during login and respawn sequences when seed crackers are most active

To enable biome obfuscation, set `biome_obfuscation.enabled: true` in your config.yml.

#### Configuration Options

```yaml
biome_obfuscation:
  # Enable/disable biome data obfuscation
  enabled: false
  
  # Time (ms) after login to obfuscate biome data
  login_protection_duration: 10000
  
  # Time (ms) after respawn to obfuscate biome data  
  respawn_protection_duration: 5000
  
  # Log when chunks are obfuscated (debug mode)
  log_obfuscated_chunks: false
```

The feature automatically detects suspicious biome probing patterns and applies obfuscation when needed, without affecting normal gameplay.

## Help wanted/Features planned

- [~~Biome name randomization~~](https://wiki.vg/Registry_Data#Biome) ✅ **Implemented as biome data obfuscation**
- Modification of more structures, there is a brand-new an API we can make use of (Chunk#getStructures).

## Build

```gradle
gradle build
```
