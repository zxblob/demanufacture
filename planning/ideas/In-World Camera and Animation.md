---
status: idea
---
# In-World Camera and Animation

The long-term look, from your annotated screenshot of the real pot:

- Replace the flat GUI with an **in-world camera angle** over the actual cook block, with the block underneath visible.
- **The player visibly holds the item and stirs with it** - an arm animation that follows the mouse 1:1.
- **Several small ingredient pieces rotating independently** in the stir direction (currently: icons orbiting at fixed offsets).
- **Look into the basket** itself to see what's in it, instead of only the side panel.
- Ingredient **textures in the pot** - a note to you for art, see [[Art Requests]].

This is the "see yourself doing it, cool camera angles" part of your first message and the biggest, riskiest chunk of the whole project (custom camera + player animation + 3D ingredient rendering). Plan: keep the mechanics solid first, prototype this on [[Stirring]] only.

Research lead: the pack has `kaleidoscope_ImmersiveEating`, which already does first-person animation work and may show how to hook the player model/camera in this environment.
