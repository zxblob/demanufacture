---
status: draft
updated: 2026-10-10
---
# IANA potato cannons: animation-ready models

Source models pulled from the pack jars and rebuilt as Blockbench projects (generic model format) with IANA's bone layout. Open the `.bbmodel` files in Blockbench.

| file | from | parts | notes |
| --- | --- | --- | --- |
| `create_potato_cannon.bbmodel` | Create 6.0.10 (`create:item/potato_cannon/item` + `cog`) | 11 cubes | the `cog` group is Create's spinning accelerator; pivot at its centre |
| `tfmg_advanced_potato_cannon.bbmodel` | TFMG 1.3.2a (also in 1.2.2) | 8 cubes | fires Napalm Potatoes, air-powered |
| `tfmg_quad_potato_cannon.bbmodel` | TFMG | 16 cubes | original part names kept (Body, Back, Pipe, Tank, Handle, Grip) |

## Bone layout
`root` > `left_arm`, `right_arm` (empty, place them yourself) and `gun` > one group per cube. IANA's code looks for these names (`GunBones`: `root`, `gun`, `hammer`, `magazine`, `ramrod`, `left_arm`, `right_arm`, `attachment_muzzle`, `attachment_optic`, `attachment_bayonet`). Animations IANA plays: `idle`, `fire`, `reload`, `equip`. Rename or regroup cubes into the bones you want to animate.

## What I changed from the originals
- Shifted X and Z by -8 so the model is centred on the origin (Java item models sit in a 0..16 block). Muzzle points to -Z, which matches IANA's revolver.
- 45 faces used UV rotation, which Bedrock/GeckoLib cannot store. I baked them: the rotated pixels were copied into extra rows at the bottom of each texture (Create 64x64 → 64x96, Advanced 64x64 → 64x93). **Use the `.png` next to each model, not the original.**
- Part names (muzzle_ring, barrel, chamber, grip ...) are my guesses from the shapes. Rename freely.

## Not done yet
- Export to `.geo.json`: in Blockbench use File > Convert Project to a Bedrock / GeckoLib model format, then Export.
- Design decisions (2026-10-10): each IANA cannon is crafted from its original cannon, and fires food like the original (no new bullet types). See To Do, section 0.
- Railways only mixes into the Create cannon (no model of its own); the Create: Things and Misc "Spout Gun" is not a potato cannon, so it is not included.

## 2026-10-10 update
Animation-ready Bedrock geo, starter animations and textures now live in the addon project: `~/Projects/Demanufacture/ArtificeCannons/src/main/resources/assets/artifice_cannons/` (see that project's README). These `.bbmodel` files remain the editable "free" format sources.
