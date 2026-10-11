---
status: planned
updated: 2026-10-10
---
# To do: compat features

One list for everything we've talked about. Tick boxes in Obsidian as things get done.

**Tags:** `P1` do first, `P2` next, `P3` later or optional. `S` = about a day, `M` = a few sessions, `L` = big. `[V]` = checked in the jars, `[I]` = my idea, not checked. **Frozen** = cooking work you paused on 2026-10-08.

Where the ideas came from: [[Pack Scan - Compat Features]], [[Electricity Audit]], [[Compat Roadmap]]. The arrows for these are in `Mind Maps/Mod Map - Connect.canvas` (red) and `Mod Map - Similar.canvas` (green). Open questions that block items are in [[Open Questions]].

---
## 0. New requests (2026-10-09)

- [ ] **P1 · M · Worn Pixel Backpacks go into your YAGM grave.**
  - `[V]` Backpack Pixel does **not** keep worn backpacks in your inventory. It stores them in player data: three `ItemStack` slots called `backpackup`, `backpackdown` and `backpackex`. YAGM only collects the normal inventory plus Curios / Accessories slots, so these packs are not picked up.
  - `[V]` YAGM has a public API for exactly this: `IAccessoryCollector`, `IAccessoryRestorer`, `IAccessoryHandler`, `IAccessorySerializer`, `AccessoryLoader`. The plan is to register a Pixel Backpack handler through it.
  - To check first: does Pixel Backpack keep the worn packs after death (MCreator player data usually copies on respawn)? If yes, the grave must also clear those slots or you get duplicates. And what happens to a pack that is open at the moment you die?
  - Done when: die with all three slots filled, find the packs (with their contents) in the grave, and not on you after respawning.
- [x] **P1 · M · Iron's Arms 'n Artifice ↔ Create ("Create guns").**
  - `[V]` **Cogs For Arms is already installed** and "integrates Iron's Arms and Artifice with Create". It adds cogs, stocks, barrels, cylinders, magazines, triggers and ammo boxes, using sequenced assembly (5), deploying, mixing (4), mechanical crafting (2), cutting, filling and a smithing transform.
  - So step one is an audit, not a build: list what Cogs For Arms already covers and what is still hand-crafted only. Candidates for gaps `[I]`: automating bullets and casings (press / filling / packager), gun oil and other modifiers as Create fluids, Create-style ammo boxes with packagers, and unlocking more modifiers through Create machines.
  - Related: `MT: Artifice Hand Fix` only patches first-person hands when Entity Model Features is installed. It does not add compat.
  - Needs your input: what you want from "Create guns" beyond what Cogs For Arms does. See Open Question 15.
- [x] **P2 · S · Check TACZ + Iron's Artifice work side by side.** `[V]` The pack now has two separate gun systems (TACZ with GunsmithLib and TaCZ JS, plus Iron's Arms 'n Artifice). Check ammo, damage and recipes don't clash, and that Entity Model Features / Texture Features (both updated) don't break first-person hands for either.
- [x] **P2 · M · Unify ammo / gun parts across TACZ and Iron's Artifice.** `[I]` One ammo family, or conversion recipes between them. Green arrow on the map.
- [x] **Craft TACZ guns and ammo with Create machines: already done by Create Immersive TaCZ (installed).** `[V]` It adds Create recipes (mechanical crafting 38, sequenced assembly 6, cutting 6, mixing, compacting, filling) for gun barrels, trigger mechanisms, casings, primers and firing mechanisms. The earlier plan for an adapter is not needed.
- [ ] **P2 · S · Audit Create Immersive TaCZ** the same way as Cogs For Arms: what is covered, what is still hand-crafted only.
- [ ] **P3 · M · Share parts and ammo between Cogs For Arms (Iron's Artifice) and Create Immersive TaCZ (TACZ).** `[I]` Two separate Create bridges for two gun mods. Green arrow on the map.
- [ ] **P1 · L · IANA addon: every Create-addon Potato Cannon becomes an Iron's Arms 'n Artifice gun (2026-10-10 request).**
  - `[V]` Cannons in the pack today: **Create Potato Cannon**, **TFMG Advanced Potato Cannon** (Napalm Potato ammo) and **TFMG Quad Potato Cannon**. No other addon adds one (searched all jars; Railways only mixes into Create's). Spout Gun (Create: Things and Misc) is not a potato cannon.
  - `[V]` IANA guns are Java `GunItem`s built from a `GunProfile` (`gun/Guns`), rendered with GeckoLib geo + animations (`idle`, `fire`, `reload`, `equip`), no data-driven path. So this needs a small NeoForge addon mod (separate from Demanufacture; soft-depends on Create + TFMG). Public hooks: `GunShootEvent`, `AmmoEvent`, `ComposeShotEvent`, `BulletImpactEvent`.
  - **Models: done (draft)**, see `art/IANA Potato Cannons/`. **Project scaffolded 2026-10-10 at `~/Projects/Demanufacture/ArtificeCannons`** (mod id `artifice_cannons`, compiles, three placeholder guns, geo + starter animations + textures ready under `src/main/resources/assets/artifice_cannons/`; see its README). You do the animations in Blockbench. Not yet run in game.
  - `[V]` **All three originals read Create's `PotatoCannonProjectileType` registry** (TFMG's Quad uses Create's `PotatoCannonItem.Ammo`), so food ammo can come from that one registry for every cannon.
  - Next: you animate; I decompile `GunProfile` / `Guns` to learn the fields, scaffold the addon with one placeholder gun, then add the three guns. **Decided 2026-10-10:** (a) each IANA cannon is **crafted from the original cannon** (original stays as an item; recipe takes it as an ingredient, plus IANA/Cogs For Arms parts TBD); (b) they **fire food like the originals do**, not new bullet types.
  - `[I]` Ammo plan: read the same item list the original uses (Create has a data-driven projectile-type registry for its cannon; TFMG's Advanced uses Napalm Potato, Quad has its own `Ammo` enum), so datapack/addon foods keep working. To verify in the jars before building.
  - Still open: air-pressure/backtank use for the TFMG ones, exact recipe parts.
  - **2026-10-11 gun-class survey (IANA has 7 guns):** covered = single-shot pistol (Flintlock), revolver (Blackpowder, Six-Shooter), single-shot rifle (Musket), shotgun (Blunderbuss), slow repeater (Arquebus), full-auto rifle (Clockwork). **Missing:** semi-auto battle rifle / DMR, LMG / heavy auto, explosive or incendiary launcher, SMG (no one-handed auto), burst fire (FireMode only has SEMI and AUTO). **Decided 2026-10-11 (user's call):** Create cannon = heavy slow repeater (arquebus-like, 8 shots), TFMG Advanced = LMG (AUTO, 30 rounds), TFMG Quad = rocket launcher (4 rockets per pull, 8 rounds so 2 pulls, explodes on impact, no block damage yet). Implemented in `ArtificeCannons.java` / `RocketBehaviour.java`, not yet playtested.
  - **Paintjobs (2026-10-11, built, untested):** gun + any dye in the crafting grid (all 32 colours incl. Dye Depot), or Create Spout with paint fluid made in a Basin (1 dye + 250 mB water -> 250 mB paint; a gun takes 50 mB; keeps magazine and modifiers). Whole-model tint for now; a per-gun paint mask (so metal stays unpainted) is the next art step. Applies to all IANA guns, not just ours.
  - **More gun ideas from the pack (2026-10-11):** TFMG Flamethrower (auto, short range, fire), Powergrid Electro-Zapper (energy pistol / arc), TFMG grenades (Copper, Zinc, Thermite) as ammo for a grenade launcher, Create Spout Gun (fluid cannon, damage by fluid). No natural fit for SMG / sniper / burst: those would be invented items.
  - **Flamethrower + Electro-Zapper (2026-10-11, built, untested):** models in `ArtificeCannons/blockbench/` ready to animate. Placeholder ammo (IANA bullets); TODO real fuel (TFMG fluid) and energy (Powergrid Portable Battery) hooks.
  - **Balance pass (2026-10-11, untested):** table of all five guns vs IANA's seven is in `ArtificeCannons/README.md`. Slots: Potato 7, Advanced 6, Quad 4, Flamethrower 5, Zapper 5.

---
## 1. Housekeeping (small, do before building)

- [x] **Duplicate jars: decided, leave them.** `tfmg-1.2.2` + `tfmg-1.21.1-1.3.2a-community` (the community one is a fork) and two `cc-tweaked` versions stay as they are; you said they work. Open Question 13.
- [ ] **P1 · S · Playtest the energy loop** Power Grid → FE → Electro Energetics (and back through the Crafts & Additions alternator): does energy grow, shrink or stay flat? Open Question 14.
- [ ] **P2 · S · Confirm the FE direction on Power Grid's FE inverter.** `[V]` The bridge class I read is output-only; check the inverter block covers FE → Power Grid.
- [ ] **P2 · S · Check which pack changes on 2026-10-09 need follow-up.** Removed: Backpacked, Wet Backpacks, Chemica, Create: Metalwork, Create Nuclear, Create Radiologistics, Create IDLX, Naturalist Delight. Added: Create Immersive TaCZ, TaCZ Pack Upgrader, Create Stuff 'N Additions. Also: `sable_sa_compat` (Create: Stuff & Additions on ships), Dusty Decorations 2.2.0 (now GeckoLib), Gadgets & Gizmos 1.2.7. All dependencies of the new jars are present `[V]`.
- [ ] **P3 · S · Decide the mod vs KubeJS line.** Default: Java only where scripts can't do it. Balance stance for adapters is Open Question 12.

---
## 2. Electricity and energy (first pack-wide target)

- [ ] **P1 · S · E6: write a balance sheet** of every generation / conversion rate (Crafts & Additions, Power Grid, Electro Energetics, TFMG, Propulsion) so tuning is possible even without code. Needs the playtest above.
- [ ] **P1 · M · E1: normalise units.** One config table "1 FE = X W = Y SU", then set each mod's rates from it. `[V]` Rates today: PG `forgeEnergyPerWatt 10`, EE `34 W = 1 FE/tick`, CA alternator `0.75` efficiency, `480 FE at max RPM`.
- [ ] **P2 · S · E5: shared meter** (goggles / Jade readout) showing W, FE/t and SU for any electric block.
- [x] ~~E3: Create Nuclear → FE tap~~ **n/a: Create Nuclear was removed from the pack (2026-10-09).**
- [x] ~~Nuclear radiation alarm~~ **n/a: Create Nuclear removed.**
- [ ] **P3 · L · E2: one universal cable / connector** that joins Crafts & Additions wires, Propulsion FE cables and the other cable networks.
- [ ] **P3 · L · E4: ship-safe wiring** for Power Grid, Electro Energetics and Propulsion cables across Sable sub-levels. `[V]` Only Crafts & Additions has this today (`sable_createaddition_compat`).
- [ ] **P3 · M · Hose Connectors for Crafts & Additions / Power Grid / Electro Energetics** wires between ships and the world. `[V]` `VS-Sable-HoseConnectors` has an electric wire connector for FE only.

---
## 3. Machine and recipe-type adapters (the generic harmoniser)

One mechanism: machine A also runs recipe type B, read at runtime and configurable by datapack. Counts are recipes in the jars.

- [ ] **P1 · M · 8.1 TFMG Chemical Vat runs `create:mixing`**, and Basin runs vat recipes that need no pressure. `[V]` `create:mixing` 1,956 recipes, `tfmg:vat_machine_recipe` 223. First proof of the adapter idea.
- [ ] **P1 · S · 8.2 KC Millstone ↔ Create Millstone / Crushing Wheels.** `[V]` `kaleidoscope_cookery:millstone` 85 recipes, `create:milling` 305.
- [ ] **P2 · S · 8.14 Convert dead recipes** (`immersiveengineering:*`, `mekanism:*` shipped by FD and Crafts & Additions but those mods are not installed) into Create equivalents. Pure datapack.
- [ ] **P2 · M · 8.5 Keg ↔ Tavern Barrel** drink recipes, plus fermenting (CDG basin / bulk fermenting).
- [ ] **P2 · S · 8.6 Freezing:** one interface so any cold source freezes anything freezable (KC World Liquor freezer, Dragons Plus freezing, B&C freeze sources).
- [ ] **P2 · M · 8.7 Distillation / refining:** same crude in, same fractions out whichever tower you build (TFMG, Petrochem, CDG).
- [ ] **P2 · M · 8.9 One engine-fuel table.** `[V]` Per-mod today: `petrochem:*_engine_fuel`, `createaddition:liquid_burning`, TFMG fuel tags, `createkineticlift:turbofan_fuels`.
- [ ] **P2 · S · Add TFMG crude oil to `c:crude_oil`.** `[V]` The tag lists CDG and Petrochem but I found no TFMG crude in it.
- [ ] **P3 · M · 8.8 One oil-chunk map** for every pumpjack (TFMG, CDG, Petrochem, Petro Deposits).
- [ ] **P3 · L · 8.4 Metal melting / casting:** `[V]` Create: Metalwork (which shipped most of the melting and casting recipes) was **removed**; re-check what is left (TFMG casting, CDG casting) before deciding if this is still needed.
- [ ] **P3 · M · 8.3 KC chopping board recipes on the Create saw** (FD cutting is already bridged by Central Kitchen `[V]`).
- [ ] **P3 · S · 8.11 Mechanical Press runs CDG hammering** and the reverse.
- [ ] **P3 · M · 8.10 Rolling / charging recipes usable by TFMG machines.**
- [ ] **P3 · S · 8.12 FD pot recipes usable in the KC stockpot** and the reverse.
- [x] ~~Chemica ↔ TFMG ↔ Metalwork shared molten-metal and vat recipes~~ **n/a: Chemica and Create: Metalwork were removed (2026-10-09).**

---
## 4. Fuel, oil and thrust

- [ ] **P2 · S · Create Liquid Fuel accepts every `c:fuel` fluid** (diesel, kerosene, LPG, biodiesel).
- [ ] **P2 · S · Propulsion thruster fuel from the `c:fuel` tags** and Plant Oil from Diesel Generators.
- [ ] **P3 · S · Kinetic Lift jets burn Petrochem kerosene** (already accepts TFMG kerosene `[V]`).
- [ ] **P3 · M · Pipe liquid fuel into the KC Stove** (Create Liquid Fuel → heat source).

---
## 5. Economy and delivery

- [ ] **P2 · M · One shared price table** for Numismatics, Delivery Required (its "Price worldshaper") and villagers.
- [ ] **P2 · M · Crew Food Crate contracts:** deliver meals to an airship through Delivery Required, reward by quality. `[V]` The contract string exists; mechanics not read.
- [ ] **P3 · M · Sell dishes by quality** in Numismatics Vendors (note: Vendors cannot filter special data in Buy mode `[V]`).
- [ ] **P3 · M · Chef villager** (Trading Floor) that buys named dishes and sells rare ingredients.
- [ ] **P3 · S · Tradeworks shelves as shop displays**, and as holders for plated dishes (Display Delight).
- [ ] **P3 · L · Order a dish as a package** (Mobile Packages / Factory Gauge).
- [ ] **P3 · S · CC: Tweaked peripherals:** bank / vendor (Numismatics), menu / pantry (cooking).

---
## 6. Aeronautics, Sable and ships

- [ ] **P3 · L · Galley on a moving ship:** cook mode and basket scan on Sable sub-levels. Biggest unknown; needs a test of block positions on sub-levels first.
- [ ] **P3 · S · KC chairs and tables as seats on ships** (`createfurnitureseats` already bridges other furniture `[V]`).
- [ ] **P3 · L · Steam-powered stove** from Create boiler vents `[I]`.

---
## 7. Player, backpacks and graves

- [x] ~~Check Backpacked with YAGM~~ **n/a: Backpacked and Wet Backpacks were removed (2026-10-09). Pixel Backpacks are now the only backpack mod, which makes the YAGM item above more useful.**
- [ ] **P3 · M · Backpack as an ingredient source / lunch box** for cooking `[I]`.

---
## 8. Information and tooling

- [ ] **P2 · S · Jade tooltip** for pot contents and doneness.
- [ ] **P2 · S · Check AppleSkin shows the real hunger/saturation** of quality-adjusted dishes (KC modifies food properties `[V]`).
- [ ] **P3 · M · JEI category** showing the minigame steps for a dish.
- [ ] **P3 · M · Ponder scenes** teaching each minigame (PonderJS installed).
- [ ] **P3 · M · KubeJS bindings** so the pack author can define dishes, minigame variants and adapters from scripts.
- [ ] **P3 · M · Data-driven adapter and minigame definitions** (datapack JSON).
- [ ] **P3 · S · Dye unification** across Dye Depot, Dye The World, Dragons Plus and the bridge mods.

---
## 9. Cooking minigames (FROZEN since 2026-10-08)

Nothing here starts until you unfreeze it. Decisions already made: hand-cooking stays optional (no quality gate); dish quality will write into Kaleidoscope Cookery's own quality (Superb / Excellent / Standard / Poor).

- [ ] ⏸ **Write the minigame result into KC's `QUALITY` component** (replace our five tiers). S.
- [ ] ⏸ **Fix persistence:** `pot_ingredients` and `pot_liquid` are lost on chunk unload or restart (known bug from the handoff). S.
- [ ] ⏸ **Cookbook recipe and creative tab** (right now it is `/give` only). S.
- [ ] ⏸ **Skillet rework:** add the click-to-flip mechanic and make the shake band more forgiving (your answer to Open Question 9). M.
- [ ] ⏸ **Arc drop animation** when you add an ingredient to the pot (Open Question 4). M.
- [ ] ⏸ **Dishes remember how they were made** (action history), and build on KC's `RecipeItem` records. M.
- [ ] ⏸ **Oil required for meats**, otherwise liquids only change flavor (Open Question 6). M.
- [ ] ⏸ **Next minigames:** Wok (oil first, shake, flip) and the boards (chopping, cutting). After the third, extract a shared base screen. M each.
- [ ] ⏸ **Blaze Burner heat level** changes cooking speed (heat is only needed to open the minigame). S.
- [ ] ⏸ **Plant Oil as cooking oil** (`c:plantoil` tag: Diesel Generators, Crafts & Additions seed oil, Electro Energetics). S.
- [ ] ⏸ **Shared quality scale** across KC (4 steps), Tavern (6 steps), Brewin' & Chewin' and ours. M.
- [ ] ⏸ **More minigames for other blocks:** Steamer, Teapot, Millstone, Shawarma Spit, cocktail shaker, pressing tub, Keg pouring, sandwich station, pizza. M each.
- [ ] ⏸ **Serve a finished dish into Display Delight blocks** with its name and quality. M.
- [x] ~~Radiation × food~~ **n/a: Create Nuclear removed.**
- [ ] ⏸ **Multiplayer / dedicated server test** and two players at one pot. M.
- [ ] ⏸ **Later:** cookbook scroll history and step pages, `book.png` art, sounds, wine via `kaleidoscope_world_liquor`, in-world camera / GeckoLib animation.

---
## Decided: not doing

- Touching the duplicate TFMG and CC: Tweaked jars (you answered Open Question 13: they work, leave them).
- Making hand-cooking a quality gate over machines (you chose fun and optional).
- Hypertube / Escalators on ships, radio-guided vehicles, random food filter and other low-value arrows from the first mind map. Removed 2026-10-08.
