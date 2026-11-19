# Apocalypse Immersion (Minecraft Forge 1.20.1)

**Apocalypse Immersion** is a tactical sensory, lighting, and directional audio overhaul mod for **Minecraft 1.20.1 (Forge 47.4+)** designed for hardcore zombie apocalypse and modern tactical modpacks.

## Core Features

### 1. Line-of-Sight Light & Focused Flashlight AI
* **Line-of-Sight Light Attraction:** Hostile mobs and zombies investigate torches, lanterns, campfires, and bright light sources **only when they have direct unobstructed line-of-sight**—even from far down a street, and never through solid walls.
* **Focused Flashlight Beam Detection:** Enemies only react to your flashlight when you shine the beam directly onto them within a narrow cone with clear line-of-sight (no artificial 360-degree aggro).

### 2. Tactical Sound Detection (Stealth & Hearing)
* **Reload & Gear Audio Cues:** Weapon reloads, magazine changes, and bolt racking alert nearby enemies at close range.
* **Jump-Landing Stomps & Footsteps:** Heavy jump landings and sprinting/walking footsteps draw nearby enemies, while crouching keeps you stealthy.

### 3. Dependable Directional Acoustics & Warzone Ambience
* **Directional Gunshots, Footsteps & Voices:** Distance-attenuated sound rolloff and indoor slapback reverb let you reliably judge enemy direction and distance by ear.
* **Dynamic Warzone Ambience:** Plays distant gunfire bursts, sniper cracks, and artillery thuds when armed NPC or enemy mods (such as *Simple Enemies*, *TaCZ*, or *Guard Villagers*) are active.

### 4. Atmospheric & Tactical Visuals
* **Cold Breath Fog & Stamina Breathing:** Visible breath vapor in cold environments and heavy breathing audio/visual cues under exertion.
* **Dynamic Shadows, Leaf Dapple & Water Splashes:** Real-time light/shadow enhancements, bullet water splashes, and integrated suppressor support.

## Companion Standalone Addons
To keep **Apocalypse Immersion** focused and intuitive, optional HUD/paranoia features are split into standalone addons:
* **Acoustic Radar (`acousticradar`):** Optional directional sound visualizer HUD.
* **NPC Voice Echo (`npcvoiceecho`):** Optional indoor NPC voice echo and low-health/darkness auditory cues.
* **Bodycam 360 (`bodycammod`):** Optional Axon-style 360° fisheye bodycam lens, camera shake, and explosive water splashes.

## Building from Source
1. Clone this repository:
   ```bash
   git clone https://github.com
   cd apocalypse-immersion
   ```
2. Build the mod JAR using Gradle (Java 17 required):
   ```bash
   ./gradlew build --no-configuration-cache
   ```
3. Find the compiled mod in `build/libs/`.

## Contributing
Pull requests are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on submitting bug fixes, compatibility bridges, and new tactical features.