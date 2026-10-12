---
status: built
blocks: FD Cooking Pot, KC Stockpot
---
# Stirring

First minigame, validated and tuned from your playtests.

- Move the mouse in circles around the pot. Ingredients orbit with your motion, each at its own fixed offset so a handful look scattered rather than stacked.
- Judged on angular speed, see [[Doneness and Pace]]. Pace meter instead of a flickering "too fast / too slow" label (that was your feedback).
- Left: [[Basket Integration]]. Right: [[Liquid Flask]]. Bottom: Pace, Doneness, Serve ([[Dish Output]]).
- Fail: pace to 0 destroys the ingredients.
- Original idea still open: sometimes clockwise, sometimes counter-clockwise on demand. Not implemented - only speed matters right now.

Sibling: [[Scrambling]]. Long-term visual target: [[In-World Camera and Animation]].

## 2026-10-12: in-world version
No more flat GUI: opening the minigame now puts the camera behind and above the player's head looking at the pot (`client/scene/CookScene`, tune `CAMERA_BACK` / `CAMERA_UP`), turns the player to face the pot, hides the HUD, and draws the ingredients as 3D items in the pot (click one to take it out, picked via projected screen positions). The screen only handles the mouse stir, the basket / liquid panels, the meters and Serve. The player plays `fd_cooking_pot.work` through Player Animator as a plain loop for now.
Later the same day:
- Camera is right above the player's head (`CAMERA_BACK` 0.25, `CAMERA_UP` 1.15 in `CookScene`).
- **Position lock:** on opening, the server moves the player to 0.875 blocks from the pot centre on the nearest free side, facing it (`CookStand`). The rigs use the same 14 px.
- **Arm follows the mouse:** `PhaseAnimation` plays the `work` loop by phase, one mouse circle = one loop, so the arm stops when you stop. `PHASE_DIRECTION` in the screen flips it.
- **Own spoon:** the FD pot's baked spoon is removed from the pot being cooked at (`SpoonModels`) and a spoon model turns with the stir using the `spoom` bone from `cook_props`. `SPOON_DIRECTION` in `CookScene` flips it.
- No more blur or dimming. Meters are hidden (press H); feedback is steam when on pace, smoke when about to burn, a chime and sparkles when done; right-click serves.
Not done: the Stockpot has no rig of its own (uses the pot animation), the Skillet still uses the old GUI (its flip rework comes first), a new pot spoon for non-FD pots.
