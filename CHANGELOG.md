# Changelog — Apocalypse Immersion

All notable updates, new features, and fixes for **Apocalypse Immersion (1.20.1)** are documented here.

## - 2026-11-19
### Added
- **Custom Synthesized `.ogg` Tactical Acoustics:** Replaced placeholder explosion sounds with real synthesized `indoor_gun_tail.ogg` (concrete room slapback), `distant_gunshot_crack.ogg` (supersonic snap + valley roll), and `explosive_water_splash.ogg` (hydro-acoustic water eruption).
- **3D Towering Explosive Water Plumes:** Added multi-plane volumetric water spray sheets (`water_plume.png`) and dense splash rings when TNT, grenades, RPGs, or explosive rounds detonate in water.
- **Stained-Glass Colored Spotlights:** Placing any colored stained glass or pane underneath any light source projects a matching colored volumetric spotlight beam and floor/wall glow pool.
- **Sharpshooter (SSR5) & Flashier Flashlights Bridge:** Added shader-safe two-plane 3D laser beams, glowing impact dots, and volumetric weapon flashlight cones compatible with TaCZ, Sharpshooter (`tp_shooting`), and Flashier Flashlights.
- **Line-of-Sight Light & Tactical Hearing AI:** Enemies investigate torches, lanterns, and campfires only with unobstructed line-of-sight, react to flashlights only when the beam hits them directly, and hear close-range reloads, jump-landing stomps, and footsteps.
- **Real-NPC-Gunfire Warzone Ambience:** Plays directional distant gunfight bursts when real NPCs or enemies fire weapons at range without overlapping close-range combat.

### Fixed
- **Zero-Delay Reloads & Gun Swaps:** Bypassed reload, magazine, bolt, and weapon-swap sounds so they play instantly with zero latency and zero accidental explosion triggers.
- **Floor-Grounded Dynamic Shadows:** Fixed elevated wall shadow artifacts so block and entity shadows stretch cleanly across the ground plane.
- **TaCZ Null-Sound Crash Fix (v1.7.14):** Safely guarded `AbstractSoundInstance.getVolume()` in `GunfireDynamicLightBridge` so custom TaCZ draw/weapon sounds with uninitialized inner `Sound` objects never trigger a `NullPointerException`.
### Update (2026-11-20 - `659ba39`)
- Add custom .ogg acoustics, 3D water plumes, stained-glass spotlights, SSR5 laser bridge & auto-changelog

### Update (2026-11-20 - `0430778`)
- Merge branch 'main' of https://github.com/hannedinger77-droid/apocalypse-immersion-mod

### Update (2026-11-20 - `a1f8cd2`)
- Add MWLoadout-compatible Talking Chibi Wolf Gun Buddy Charm with glowing neon-red & purple box, dangling star physics, and voice triggers
