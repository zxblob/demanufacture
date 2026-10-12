# Artifice Cannons

NeoForge 1.21.1 addon for Iron's Arms 'n Artifice (IANA): the Create / TFMG potato cannons as IANA guns. Mod id `artifice_cannons`.

## Where to animate
Open **`blockbench/<gun>.bbmodel`** (`potato_cannon`, `advanced_potato_cannon`, `quad_potato_cannon`). These are Bedrock-format Blockbench projects that already contain the model, texture, the four starter animations (`idle`, `fire`, `reload`, `equip`) and a **grey box on each arm bone** (`left_arm_reference`, `right_arm_reference`, 4x12x4, shoulder at the bone pivot) so you can see where the hands are. Those reference boxes are set to *not export*, so they never reach the game.

Workflow: open the `.bbmodel` > Animate tab > edit > **File > Export > Export Bedrock Geometry** over `geo/item/<gun>.geo.json` and **Export Animations** over `animations/item/<gun>.animation.json`. Also File > Save so the `.bbmodel` keeps your work.

Game files (what the mod actually loads), under `src/main/resources/assets/artifice_cannons/`:

| file | what |
| --- | --- |
| `geo/item/<gun>.geo.json` | the model |
| `animations/item/<gun>.animation.json` | `idle`, `fire`, `reload`, `equip` (IANA plays exactly these names) |
| `textures/item/<gun>.png` | the texture (rotated UVs were baked into extra rows) |

`blockbench/free_format_sources/` holds the first, plain conversions of the originals (no bones for arms, no animations); you can ignore them.

## How IANA animates (learned from `example_revolver`)
- Bones IANA looks for by name: `root`, `gun`, `left_arm`, `right_arm`, `hammer`, `magazine`, `ramrod`, `attachment_muzzle`, `attachment_optic`, `attachment_bayonet` (`GunBones`).
- **`left_arm` / `right_arm` are empty bones (in game; the grey boxes are only a Blockbench reference).** IANA renders the player's real arm model at the bone's pivot, using whatever transform the animation gives that bone. So hands are posed entirely by animating those two bones (rotation about -100 on X and a position offset). The left arm only shows while the gun's occupancy is `BOTH`.
- `root` is the bone that moves the whole gun + hands (recoil, equip swing); `gun` moves the gun alone.
- `attachment_muzzle` pivot is where the muzzle flash spawns. Added to all three.
- Animation names: `idle` loops forever, `fire`, `reload` and `equip` are triggered. Length of `reload` should roughly match the `reloadTimeTicks` in the gun profile (30 ticks = 1.5 s for the Create cannon).
- Sound cues are not in the animation file; they are timestamps (seconds) in `ReloadCue`/`FireCycleCue` in the gun profile (Java).
- Create's cannon has a `cog` bone (spins on idle and fire) as a first example of a sub-part.
- Origin = where the hands hold the gun. Cannons were shifted so the origin sits just behind the chamber (Quad: at its handle).

## Status
- Builds (`./gradlew compileJava`); registers three `GunItem`s with placeholder stats. Not yet run in game.
- Not done: recipes (craft from the original cannon), food ammo from Create's projectile registry, sounds, Create/TFMG as optional dependencies.

## Flamethrower and Electro-Zapper (2026-10-11)
Models rebuilt from TFMG's Flamethrower and Powergrid's Electro-Zapper, same layout as the cannons: `blockbench/flamethrower.bbmodel`, `blockbench/electro_zapper.bbmodel` (arm reference boxes, starter `idle` / `fire` / `reload` / `equip`, poses borrowed from IANA's musket). Hand origin: Flamethrower at its handle, Zapper at its grip. The Zapper keeps Create-style `cog` bone (spinning coil). Part names are guesses.
- Flamethrower: AUTO, 60 rounds, fire delay 2, short-lived flames (9 ticks), sets targets on fire.
- Electro-Zapper: one-handed (PISTOL pose), 12 rounds, fast bolt, chain lightning to 3 nearby targets for half damage.
- Both still use IANA bullets as ammo for now (placeholder). Follow-ups: TFMG fuel from a tank / backtank, FE from a Portable Battery.

## Balance (2026-10-11)
IANA reference (damage / shot, magazine, fire delay, reload, slots): Flintlock 20/1/1/40/5, Musket 18/1/1/60/5, Blackpowder Revolver 10/6/10/40/5, Six-Shooter 8/6/3/20/5, Blunderbuss 22 (8 pellets)/2/1/30/5, Arquebus 12/4/20/50/7, Clockwork Rifle 7/10/4/30/6. That is roughly 6 to 27 damage per second including reload.

| gun | class | damage | magazine | fire delay | reload | slots | rough dps (incl. reload) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Potato Cannon | heavy slow repeater | 12 | 8 | 18 | 25 | 7 | ~11 |
| Advanced | burst rifle (3 x 4 ticks + 8 cooldown) | 7 | 24 | 4 | 43 | 6 | ~16 |
| Quad | rocket launcher, 4 rockets / pull, 4 rounds per pull | 8 total + 4 explosions of 1.3 | 8 | 30 | 40 | 4 | ~11 close, splash |
| Flamethrower | short-range auto | 4 (x0.4 at its slow speed) + burn | 50 | 2 | 30 | 5 | ~17 + fire, ~8 blocks |
| Electro-Zapper | energy pistol | 7 + 3 chains at half | 12 | 10 | 38 | 5 | ~9 single, up to ~22 chained |

Notes learned from IANA's code: damage per bullet = clamp(bullet speed per tick, 0..2)/2 x damage / projectile count, so slow projectiles do less damage (flames fly at 0.8 per tick, factor 0.4) and a pellet gun's damage is the total across pellets. Default bullet speed is 12 per tick; gravity 0.05.

## Paintjobs (2026-10-11)
Two layers per gun: **dye** recolours the *accent* colours of the texture (wood stocks, grips, coils, trim), **metal** (ingot, raw ore or ore block tagged `c:ingots|raw_materials|ores/<metal>`) recolours the body. Apply by crafting (gun + dye and/or metal item) or by Create Spout (`paint_<dye>` and `paint_metal_<metal>` fluids, Basin: item + 250 mB water).
No per-gun lists: `client/PaletteRecolor` classifies every *cube* from its own texels. Dark warm browns are **wood** (only a dye changes them; with metal paint alone they stay wood). On guns with no wood, cubes in grip / mag / cog style bones, or clear colour outliers, are **trim** (dye if present, else the metal). Everything else is **metal**. Cube-level roles mean a part is never half recoloured. Each group is repainted from a shadow-base-highlight ramp of the new colour, anchored so the group's median brightness becomes exactly the new colour (darker/lighter original tones stay darker/lighter). Generated textures are cached per gun/dye/metal and cleared on F3+T. Metal colours: `Metals.java`.

Metals: crafting accepts **any** item tagged `c:ingots/<x>`, `c:raw_materials/<x>` or `c:ores/<x>` (tin, tungsten, anything from any mod); the colour is sampled from that item's texture (`client/MetalPalette`), `metal_colors.json` overrides a base colour. Create Spout / Basin plating fluids exist only for the metals listed in `Metals.COLORS` (fluids must be registered at startup).

## Electricity module (merged from Demanufacture, 2026-10-12)
Replaces Create: Power Grid (which left the pack). Registers only when Create (zapper) and Create Electro Energetics (light) are loaded.
- `artifice_cannons:zapper_tool` "Electro-Zapper": 20 000 FE, 500 FE per shot, bolt chains to 2 nearby enemies. The IANA Electro-Zapper gun is crafted from it.
- `artifice_cannons:factory_light` "Factory Light": an Electro Energetics device (1000 ohm per segment) that projects an invisible light beam on the floor below.
- Code: `electricity/`. Ported from Power Grid (Apache-2.0, see `THIRD_PARTY.md`).
