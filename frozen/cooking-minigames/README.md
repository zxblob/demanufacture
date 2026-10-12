# Frozen: cooking minigames

Frozen on 2026-10-12 (user decision). This is the whole cooking module exactly as it stood, moved out of the build so
Demanufacture compiles without it. Nothing here is compiled.

## What is in it
Mirrors `src/main/` of the Demanufacture project:
- `java/com/zxblob/demanufacture/cooking`: cook mode, basket scan, stirring server logic, dish naming, `CookStand`, `KcQuality`.
- `java/.../network`: the payloads.
- `java/.../client`: the in-world stirring scene (`scene/CookScene`, `SpoonModels`, `SpoonProp`), the Player Animator
  glue (`anim/`), and the screens (stirring screen is now an overlay; scrambling is still the old GUI).
- `java/.../Demanufacture.java`: the registry class of the old mod (cookbook, dish, attachments, creative tab).
- `resources/`: lang, cookbook models and recipe, `player_animations/` and `cook_props/` exported from your Blockbench
  animations, the spoon model, and `META-INF/accesstransformer.cfg` (opens `Camera.setPosition` for the cook camera).

## How to unfreeze
1. Copy `src/main/java/com/zxblob/demanufacture/{cooking,network,client}` and `Demanufacture.java` back over the
   skeleton in the project's `src/main/java/...`, and `resources/*` into `src/main/resources/`.
2. Restore the dependencies in the project `build.gradle` (compileOnly + localRuntime, jars in `libs/`):
   `FarmersDelight-1.21.1-1.3.2.jar`, `kaleidoscopecookery-1.6.0-neoforge+mc1.21.1.jar`,
   `player-animation-lib-forge-2.0.4+1.21.1.jar`.
3. Restore the dependency blocks in `src/main/templates/META-INF/neoforge.mods.toml`: `farmersdelight` and
   `kaleidoscope_cookery` (required, AFTER) and `playeranimator` (optional, client).
4. `./gradlew compileJava`.

## State when frozen
- Stirring (FD Cooking Pot, KC Stockpot) plays in the world: camera above the player's head, player locked 14 px from
  the pot, arm follows the mouse circle by phase, own spoon (FD's baked spoon is hidden), steam / smoke feedback,
  right-click serves, H shows debug meters. **Spoon spin direction and the old spoon's removal were still being tuned**
  (`PHASE_DIRECTION` in the screen, `SPOON_DIRECTION` in `CookScene`).
- Skillet scrambling still uses the old GUI; the flip rework was not started.
- Not done: Wok, boards, Stockpot rig, dish memory, persistence tested in game, multiplayer.
- Tools that feed it: `tools/cooking_rigs.py` (rigs) and `tools/export_player_animations.py` (animations, writes to
  this folder's `resources/`).
- Rigs and notes: `planning/art/Cooking Rigs/`, `planning/KC and FD Compat./minigames/`.
