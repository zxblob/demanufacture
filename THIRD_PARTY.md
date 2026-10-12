# Third-party material

## Create: Power Grid (Apache-2.0)
The Electro-Zapper (`zapper_tool`) and Factory Light in `ArtificeCannons/src/main/java/com/zxblob/artificecannons/electricity/` are ports of items from
**Create: Power Grid** by patryk3211, Da-Negy, rvndm and casvara (https://github.com/patryk3211/PowerGrid), licensed
under the Apache License 2.0. Copied or adapted from it:
- models, textures and the shoot sound of the Electro-Zapper and Factory Light (`ArtificeCannons/src/main/resources/assets/artifice_cannons/...`);
- the strip-shape rules of the Factory Light block and the logic of the zapper's projectile.

The code was rewritten to run on FE and on Create Electro Energetics instead of Power Grid's own simulation; changes
are noted in the commit history. Licence text: https://www.apache.org/licenses/LICENSE-2.0

## Create Electro Energetics (MIT)
Used as a compile-time dependency only (not redistributed).

## Farmer's Delight (MIT) and Kaleidoscope Cookery (BSD-3-Clause + CC BY-NC-SA 4.0)
Compile-time / runtime dependencies (`libs/` is git-ignored, not redistributed). The Blockbench rigs in
`planning/art/Cooking Rigs/` embed block models and textures converted from these two mods (FD by vectorwing, KC by
ysbbbbbb, tartaric_acid and Azumic) **for posing only**; the planning vault is not part of the shipped mod. Do not
ship those textures in the mod jar: KC's art is non-commercial / share-alike.
