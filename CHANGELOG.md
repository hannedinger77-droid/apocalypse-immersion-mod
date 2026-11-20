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
- **Performance & FPS Optimization Suite (v1.7.16):** Added tick-cached block geometry for leaf/window dapple projections, auto low-FPS throttling, configurable scan radii, and in-game Settings toggles/sliders for Custom Shadows, Leaf Dapple, Scan Cache Ticks, and Particle Density.
- **Dedicated 7th Charm Slot & 3D Blockbench Wolf Afton Battle Buddy (v1.7.19):** Added a dedicated 7th Charm Slot button inside TaCZ Refit (Z) and MWLoadout screens (plus B quick-toggle) that renders your 3D Blockbench .geo.json keychain charm and triggers all 10 custom high-pitched voice lines without occupying your laser rail.
- **3D Blockbench Wolf Afton Battle Buddy Charm & Custom Voice Pack (v1.7.18):** Built native TaCZ/MWLoadout 3D Bedrock keychain charm (.geo.json + custom chibi wolf UV texture) with top keychain hook and bottom star, wired to all 10 high-pitched custom voice recordings (greeting on equip, creeper warning, out of ammo, creative pig lecture, sheep warning, enemy kill, and idle stories).
- **3D Blockbench Wolf Afton Battle Buddy Charm & Custom Voice Pack (v1.7.17):** Built native TaCZ/MWLoadout 3D Bedrock keychain charm (.geo.json + custom chibi wolf UV texture) with top keychain hook and bottom star, wired to all 10 high-pitched custom voice recordings (greeting on equip, creeper warning, out of ammo, creative pig lecture, sheep warning, enemy kill, and idle stories).
- **TaCZ Null-Sound Crash Fix (v1.7.14):** Safely guarded `AbstractSoundInstance.getVolume()` in `GunfireDynamicLightBridge` so custom TaCZ draw/weapon sounds with uninitialized inner `Sound` objects never trigger a `NullPointerException`.
- **TaCZ Echo Handler Null-Sound Fix (v1.7.15):** Added the same null-sound guard to `DistantGunshotEchoHandler.onSoundPlayed` so TaCZ draw/handling sounds with uninitialized inner `Sound` objects never crash the client.
### Update (2026-11-20 - `659ba39`)
- Add custom .ogg acoustics, 3D water plumes, stained-glass spotlights, SSR5 laser bridge & auto-changelog

### Update (2026-11-20 - `0430778`)
- Merge branch 'main' of https://github.com/hannedinger77-droid/apocalypse-immersion-mod

### Update (2026-11-20 - `a1f8cd2`)
- Add MWLoadout-compatible Talking Chibi Wolf Gun Buddy Charm with glowing neon-red & purple box, dangling star physics, and voice triggers

### Update (2026-11-20 - `2849a59`)
- Fix TaCZ null-sound NullPointerException in GunfireDynamicLightBridge (v1.7.14)

### Update (2026-11-20 - `8851fe2`)
- Fix TaCZ null-sound NullPointerException in DistantGunshotEchoHandler (v1.7.15)

### Update (2026-11-20 - `7170112`)
- Add FPS optimization suite, tick-cached dapple rendering & in-game performance settings (v1.7.16)

### Update (2026-11-20 - `a1a7497`)
- Add 3D Blockbench Wolf Afton Battle Buddy charm & custom voice triggers (v1.7.23)

### Update (2026-11-20 - `83ca07b`)
- Support all firearm mods & anchor 3D Chibi Wolf keychain charm to gun side rail

### Update (2026-11-20 - `778b428`)
- Render 3D Blockbench Wolf Battle Buddy charm in 3D viewmodel overlay so it is always visible

### Update (2026-11-20 - `83dfe59`)
- Add gun-follow physics, receiver collision & BufferBuilder PiP scope guard (v1.7.25)

### Update (2026-11-20 - `b4f8795`)
- Make 3D Battle Buddy charm 100% compatible with Oculus shaders & HUD mods

### Update (2026-11-20 - `fc1c99f`)
- Guard BufferBuilder.building() in DynamicShadowRenderer & world renderers for PiP scopes

### Update (2026-11-20 - `a127d50`)
- Cancel nested RenderLevelStageEvent during PiP scope passes when BufferBuilder is active

### Update (2026-11-20 - `2921c33`)
- Fix Tesselator buffer cleanup in GlobalRenderStageGuardMixin (v1.7.30)

### Update (2026-11-20 - `7acaa6c`)
- Cancel RenderLevelStageEvent at HEAD when Oculus shadow BufferBuilder is active (v1.7.31)
