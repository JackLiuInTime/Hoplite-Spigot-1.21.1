# Death Effect

## Requirement
When a player dies:
1. Summon lightning at the death location.
2. The lightning is visual-only (no direct damage).
3. Play the Wither spawn sound to all online players.

## Implementation
- Listener: com.hoplite.DeathEffect.PlayerDeathEffectListener
- Event: PlayerDeathEvent
- Visual lightning: World#strikeLightningEffect(Location)
- Global sound: iterate all online players and call Player#playSound(..., Sound.ENTITY_WITHER_SPAWN, ...)

## Registration
Registered in plugin bootstrap:
- com.hoplite.HoplitePlugin#onEnable

## Placement Note
This document is categorized under docs/global because the behavior is a global world event effect rather than a specific combat/utility/legendary item skill.
