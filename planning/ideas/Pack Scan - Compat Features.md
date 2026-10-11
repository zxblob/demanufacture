---
status: idea
updated: 2026-10-07
---
# Pack scan: compat features we could build

Result of reading all 228 jars in the instance's `mods/` folder (descriptions, lang files, tags, class lists of the food/economy/oil mods). Nothing here is decided. Part of [[Compat Roadmap]].

**Evidence tags:** `[V]` = I saw it in a jar (lang string, class, tag). `[I]` = my inference or a pitch, not checked. "Effort": S = a day or less, M = a few sessions, L = a big system.

## 0. Findings that change the cooking plan

These matter before anything else, because they affect whether the cooking overhaul should exist in its current shape.

1. **Create already automates FD + KC cooking.** `[V]` Create: Central Kitchen has ponders for the cooking pot, skillet, stove, cutting board (deployer + knife, saw), servings and kegs; `kaleidoscopecookery_automation` lets a Mechanical Arm run a whole Wok / Stockpot / Steamer / Shawarma Spit recipe (set the recipe, the arm adds oil, ingredients, stirs, scoops). So the pack already has "cook without playing a minigame". Our minigames are not a replacement; they can only be the **hand-made alternative**. That is also the best pitch: machines make bulk food, a player at the pot makes the *good* food.
2. **KC already has a dish quality system.** `[V]` `item/quality/Quality` = `SUPERB / EXCELLENT / STANDARD / POOR`, stored as a data component (`ModDataComponents.QUALITY`), set with `QualityUtils.setQuality(stack, q)`, and `QualityUtils.modifyFoodProperties / modifyEffects` scale hunger and effects by it. `QualityEvaluator.evaluate(...)` rates a dish from how close the ingredient mix is to a hidden per-recipe "ideal vector" seeded by the recipe id. Our own `Undercooked ... Masterful` tiers are a parallel system. Better: our minigame **writes into KC's QUALITY component** so a hand-cooked dish is a real KC dish with real stat changes.
3. **KC already has a recipe-memory item.** `[V]` `RecipeItem` / `RECIPE_RECORD` ("Right-click a wok or a stockpot to record ingredients for one-click cooking later"; lang: "Recipe: %s, made with %s"). It overlaps with our idea that dishes/cookbook remember how you made them. We should build on it instead of beside it.
4. **Blaze Burners already count as cooking heat.** `[V]` Central Kitchen ponder "Heat sources for Cooking": boiler heaters are valid heat sources and speed up some utensils by heat level; FD ships `create:passive_boiler_heaters` and `farmersdelight:heat_sources` tags. Answers our heat question for free.
5. **Tavern barrels have a 6-step quality scale too.** `[V]` Undrinkable / Inferior / Common / Fine / Crafted / Vintage, and the cocktail shaker refuses brew levels below Fine. Three quality vocabularies exist (KC 4-step, Tavern 6-step, ours 5-step).

## 1. Cooking-adjacent

| # | Idea | Hooks | Effort |
| --- | --- | --- | --- |
| 1.1 | Minigame result writes KC `QUALITY` on the dish (replace or map our tiers) | `[V]` `QualityUtils.setQuality`, `Quality` enum | S |
| 1.2 | Hand-cooked vs machine-cooked: arm/automation output stays Standard, only minigame can reach Superb (needs a check what automation outputs today) | `[V]` automation mods exist; output quality unverified | M |
| 1.3 | Cookbook ties into KC `RecipeItem` records: "cook this again" one-click replay of your best attempt | `[V]` `RECIPE_RECORD` | M |
| 1.4 | Heat level from a Blaze Burner (kindled vs superheated) changes doneness speed in the minigame | `[V]` heat tags, ponder | S |
| 1.5 | **Plant Oil** (Create Diesel Generators fluid) as a liquid in the oil panel, so the pack's oil industry feeds the kitchen, not only pigs; used fryer oil could go back toward biodiesel | `[V]` `plant_oil` fluid + compacting recipe; `[I]` the rest | M |
| 1.6 | Sandwich station minigame (assemble layers, timing for toasting) | `[V]` Some Assembly Required items/station | M |
| 1.7 | Serve a finished dish into Display Delight blocks (plates, bowls, 400+ food blocks) with its name and quality in the tooltip | `[V]` Display Delight blocks | M |
| 1.8 | Cocktail shaker minigame (shake rhythm) gated by Tavern's "Fine+" requirement | `[V]` shaker + brew levels | M |
| 1.9 | Pressing Tub stomp minigame, Keg pouring minigame (B&C), Teapot, Steamer, Millstone, Shawarma (see [[Other Cook Blocks]]) | `[V]` blocks exist | M each |
| 1.10 | Unify quality: one shared scale across KC, Tavern, B&C, ours, with conversion | `[V]` three scales | M |
| 1.11 | Pizza (Farmer's Pizzeria) stretch/toss minigame | `[V]` mod present | M |
| 1.12 | Chopping Board and Cutting Board minigames (already planned) | `[V]` | M |

## 2. Economy and logistics (the pack has three money systems)

| # | Idea | Hooks | Effort |
| --- | --- | --- | --- |
| 2.1 | Dish sale value scales with quality via Numismatics Vendors | `[V]` Vendor exists; note "Cannot accept special filters in Buy mode" so quality pricing needs our own vendor or a workaround | M |
| 2.2 | "Crew Food Crate" contracts: deliver cooked meals to an airship through Delivery Required's Contractor, reward by quality | `[V]` the string `contract.food_crate_delivery` exists; mechanics unread | M |
| 2.3 | Villager Chef profession (Trading Floor) that buys named dishes and sells rare ingredients | `[V]` Trading Floor installed; `[I]` the rest | M |
| 2.4 | Order system: Factory Gauge / Package request for a dish, delivered by Mobile Packages | `[V]` all installed; `[I]` integration | L |
| 2.5 | Menu board: Create Display Link shows today's special / best dish | `[V]` Display Link + Improved Display Link Experience; `[I]` | S |
| 2.6 | One shared price table for Numismatics, Delivery Required (`Price worldshaper`), and villagers | `[V]` Delivery Required has its own price editor | L |

## 3. Aeronautics / Sable (the "Nautics" in the pack name)

| # | Idea | Hooks | Effort |
| --- | --- | --- | --- |
| 3.1 | **Galley that works on a moving ship**: minigames, basket scan and cook mode on Sable sub-levels | `[V]` Sable installed, plus Jade/Sable compat shows the pack already patches things for it; `[I]` unknown how block positions behave | L, risky |
| 3.2 | Turbulence shakes the minigame (pace jitter while the ship banks, steady in calm air) | `[I]` | S once 3.1 works |
| 3.3 | Dishes give flight-relevant buffs (steady hands, no sickness, weight resistance) | `[I]` | S |
| 3.4 | Seat at KC tables and chairs on ships with Immersive Eating animation | `[V]` `createfurnitureseats` already bridges Create/Sable and furniture mods | S |
| 3.5 | Ration packs: pack dishes into Toolgun/vehicle containers with quality kept | `[V]` containers exist | M |
| 3.6 | Steam-powered stove from Create boiler vents | `[V]` `aeroconnectedfluidvessel` (steam vents -> vessel boilers); `[I]` | L |

## 4. Create tie-ins

| # | Idea | Hooks | Effort |
| --- | --- | --- | --- |
| 4.1 | Mechanical Whisk / rotational power drives a stockpot paddle; machine pace decides quality (the machine counterpart to the minigame) | `[I]` | L |
| 4.2 | Slice & Dice's Slicer as the machine for chopping; deployer + KC knife chopping | `[V]` both exist | S |
| 4.3 | Create stress used in steaming/fermenting (Keg + pressure) | `[I]` | M |

## 5. Player-facing information

| # | Idea | Hooks | Effort |
| --- | --- | --- | --- |
| 5.1 | Jade tooltip: pot contents, doneness, who is cooking, dish quality | `[V]` Jade + Sable Jade compat | S |
| 5.2 | AppleSkin shows real hunger/saturation of a quality-adjusted dish | `[V]` AppleSkin; `[I]` whether it reads KC's modified food properties | S |
| 5.3 | JEI recipe category showing the minigame steps for a dish | `[V]` JEI | M |
| 5.4 | Ponder scenes teaching each minigame (PonderJS is installed, Central Kitchen already ships ponders) | `[V]` | M |
| 5.5 | Animated in-world cooking with GeckoLib; hand poses via NotEnoughAnimations / Immersive Eating hooks | `[V]` GeckoLib 4, NotEnoughAnimations, ImmersiveEating, Coasters Immersion (camera physicality as a reference) | L |

## 6. Pack tooling

| # | Idea | Hooks | Effort |
| --- | --- | --- | --- |
| 6.1 | KubeJS bindings so the pack author can define dishes, minigame variants and naming from scripts | `[V]` KubeJS, KubeJSDelight, LootJS | M |
| 6.2 | Data-driven minigame definitions (datapack JSON: block, actions, thresholds) | `[I]` | M |
| 6.3 | CC: Tweaked peripheral: a computer reads dishes in a chest, sets a menu | `[V]` CC:Tweaked + bridges | S |

## 7. Beyond cooking (the original thesis: ecosystems that don't talk)

Seen in the mod list. I did **not** check how well they interoperate today; check before committing.

| # | Idea | What I saw | Effort |
| --- | --- | --- | --- |
| 7.1 | Electricity bridge | Create Crafts & Additions (FE), Create: Power Grid, Create Electro Energetics, Create Nuclear, TFMG electricity all installed. **Partly bridged already** `[V]`: Power Grid ships `FEBridgeEnergyStorage`, Electro Energetics a converter block. TFMG uses its own pressure capability system. Unchecked: whether TFMG electricity and Nuclear talk to the others | M (audit first) |
| 7.2 | Fuel / oil unification | **Fluid identity is mostly solved** `[V]`: shared `c:` tags exist for `diesel` (CDG + Petrochem + TFMG), `gasoline` (same three), `kerosene`, `lpg`, `biodiesel`, `ethanol`, `plantoil` (CDG + Create Additions seed oil + Electro Energetics). Gaps `[V]` in the tag files I read: `c:crude_oil` lists CDG + Petrochem but I saw **no TFMG crude** in it. What is NOT unified is the machines: engine fuel is a per-mod recipe type (`petrochem:diesel_engine_fuel`, `gasoline_engine_fuel`, `turbine_fuel`, `createaddition:liquid_burning`, TFMG's own fuel tags) and fuel *efficiency* values differ per mod | M |
| 7.7 | **Radiation x food** (Create Nuclear has a `RadiationCapability`): contaminated crops/dishes, Geiger readout, lead-lined lunchbox | `[V]` capability exists; `[I]` everything else | M |
| 7.3 | Dye unification | Dye Depot (16 extra dyes), Dye The World, Dye Depot Aeroworks and a CEE bridge, Create Framed, Copycats+, Coasters balloons | M |
| 7.4 | Backpacks as ingredient source or lunch box (Backpacked, Backpack Pixel, Wet Backpacks, Curios backtank) | all installed | M |
| 7.5 | One furniture seating layer (Handcrafted, Let's Do, Create Interiors, KC chairs, Yuushya) | `createfurnitureseats` covers some; coverage unchecked | M |
| 7.6 | Map unification | Xaero + Create Track Map + Xaero Train Map + Sable Xaero Bridge already exist | probably done |

## Honest summary of my take
- The pack **already has** a deep, automated FD/KC cooking chain. A pure minigame overhaul risks being the third cooking system nobody needs.
- The **strongest cooking ideas** are the ones that plug *into* existing systems: 1.1 (KC quality), 1.2 (hand vs machine), 1.4 (Blaze Burner heat), 1.5 (Plant Oil), 2.2 (airship food contracts), 3.1 (galley on a ship).
- The **strongest non-cooking ideas** are 7.1 (electricity) and 7.2 (fuels): that is where the pack has the most mods overlapping with no shared standard.
- Biggest unknown: **3.1**, because it needs a test of whether our block-position code behaves on Sable sub-levels.

## 8. Machine / recipe-type adapters ("make items more useful for the whole pack")

> **Counts below were taken before the 2026-10-09 removals** of Create: Metalwork (it shipped most melting / casting recipes) and Chemica (66 of the vat recipes). Clusters 8.4 and 8.1 are smaller now; re-run the census before building.

Added after you asked for compat like "TFMG Chemical Vat should run ordinary mixing recipes". Source: a census of every recipe type in the 228 jars (147 types). The pattern: **several mods each have their own machine and their own recipe type for the same job, and the recipes are not shared.** An adapter makes machine A also run recipe type B (usually by reading the other mod's recipes at runtime, no per-recipe datapack work). Counts are recipes shipped inside the jars `[V]`.

| # | Cluster | Recipe types (count) | Adapter idea | Effort |
| --- | --- | --- | --- | --- |
| 8.1 | **Mixing** | `create:mixing` (1956), `tfmg:vat_machine_recipe` (223), `createdieselgenerators:basin_fermenting` / `bulk_fermenting` (6+6) | Chemical Vat also runs `create:mixing` (and the reverse: Basin runs vat recipes that need no pressure/electrode). Needs a rule for heat level and fluid ratios | M |
| 8.2 | **Grinding** | `create:milling` (305, Create Millstone), `kaleidoscope_cookery:millstone` (85, KC Millstone), `create:crushing` (361) | KC Millstone and Create Millstone/Crushing Wheels run each other's recipes. Directly cooking-relevant: grain to flour either way | S |
| 8.3 | **Cutting** | `farmersdelight:cutting` (145), `create:cutting` (87), `kaleidoscope_cookery:chopping_board` (16), SAR slicing | Central Kitchen already bridges FD cutting to the Create saw `[V]`; KC chopping board recipes are the gap (only 8 `create:cutting` recipes exist from KC Automation) | S |
| 8.4 | **Melting / casting metal** | `createbigcannons:melting` (252), `createmetallurgy:melting` (164), `createmetallurgy:bulk_melting` (41), `tfmg:industrial_blasting` (100), `tfmg:casting` (68), `createmetallurgy:casting_in_basin/table` (41 each), `createdieselgenerators:casting` (2) | Three melting systems, three casting systems. Create: Metalwork already ships recipes for several of them, which is the "pack-bundled" way. A runtime adapter would stop recipe duplication | L |
| 8.5 | **Fermenting / brewing** | `brewinandchewin:fermenting` (25, Keg), `kaleidoscope_tavern:barrel` (42, brew levels), `kaleidoscope_world_liquor:bamboo_ferment` (1), CDG fermenting (12) | Keg and Tavern Barrel run each other's drink recipes with a shared quality scale (also see 1.10) | M |
| 8.6 | **Freezing** | `kaleidoscope_world_liquor:freezer` (5), `create_dragons_plus:freezing` (4), B&C freeze sources/Ice Crate | One "freezing" interface so any cold source freezes any freezable | S |
| 8.7 | **Distillation / refining** | `tfmg:distillation` (19), `petrochem:distilling` (3), `createdieselgenerators:distillation` (2), `gearbox:distilling` (1) | One crude oil in, same fractions out whichever tower you build. Includes the missing `c:crude_oil` tag for TFMG | M |
| 8.8 | **Centrifuge / electrolysis / pumpjack** | `petrochem:centrifuging`, `gearbox:centrifuging`, `createestrogen:centrifuging`; `petrochem:electrolyzing`, `gearbox:electrolyzing`; `petrochem:pumpjack`, `gearbox:pumpjack`, CDG pumpjack, TFMG pumpjack, plus Petro Deposits | Four pumpjack systems with their own deposit maps. Unify "oil chunk" data so one scanner works with every pumpjack | M |
| 8.9 | **Engine fuel values** | `petrochem:diesel_engine_fuel` (3), `gasoline_engine_fuel` (2), `turbine_fuel` (2), `createaddition:liquid_burning` (14), `createkineticlift:turbofan_fuels` tag (already lists TFMG kerosene `[V]`), TFMG fuel tags, `createliquidfuel` | One fuel-value table all engines read, so a TFMG diesel engine can burn Petrochem diesel and Plant Oil can power things | M |
| 8.10 | **Charging / electric** | `createaddition:charging` (53), `createaddition:rolling` (16), `tfmg:winding` (6), `tfmg:polarizing` (2), `powergrid:magnetization` (2) | Rolling/charging recipes usable by TFMG machines | M |
| 8.11 | **Pressing / compressing** | `create:pressing` (88), `createdieselgenerators:hammering` (13) + `compression_molding` (7), `gearbox:compressing` (3) | Mechanical Press runs CDG hammering, and vice versa | S |
| 8.12 | **Cooking** | `kaleidoscope_cookery:pot` (198), `stockpot` (77), `flex_pot`/`flex_stockpot` (55), `farmersdelight:cooking` (37), steamer, teapot, rice_bowl, `brewinandchewin:keg_pouring`, `farmerspizzeria:pizza_compiling` (24) | Central Kitchen + KC Automation already bridge much of this `[V]`. Remaining gap: FD pot recipes in KC stockpot and the reverse | S |
| 8.13 | **Fan processing** | `create:haunting` (162, mostly Brick and Mortar), `create:splashing` (134), `create:smoking`, blasting | Already a shared Create standard other mods plug into; low value | - |
| 8.14 | Dead recipes | `immersiveengineering:*` (FD ships 8 crusher, 4 cloche, 2 squeezer...) and `mekanism:*` (Create Additions) recipes exist but those mods are not installed | Free content: convert unused IE/Mekanism recipe types to Create equivalents (crusher -> crushing, squeezer -> pressing/compacting). Pure datapack, no code | S |

### What this changes
- The **adapter approach is the real "harmoniser"**: one generic mechanism ("machine X also accepts recipe type Y, with a converter") reused 8+ times. That is a better foundation than one-off hacks.
- **Most of 8.x is data/recipe-manager work, not UI**, which fits "Claude codes, you direct art" well, but it is far less visible in-game than a minigame.
- Because the recipes live in jars we don't control, adapters should read recipes at runtime and be **datapack-configurable** (a JSON saying "machine A accepts types B and C") rather than hardcoded.

## 9. New since the first scan (2026-10-09)

New or updated jars found in `mods/` `[V]`:
- **Iron's Arms 'n Artifice** (+ **Iron's Lib**, **MT: Artifice Hand Fix**): historical-fantasy firearms with modular parts and modifiers. The hand fix only keeps first-person hands in front of the gun when Entity Model Features is installed.
- **Cogs For Arms**: an existing Create integration for Iron's Arms 'n Artifice (parts via sequenced assembly, deploying, mixing, mechanical crafting). This means "Create guns" is partly done already.
- **TACZ** (Timeless & Classics Guns: Zero) + **GunsmithLib** + **TaCZ JS**: a second, data-driven gun system with its own Gun Smith Table recipe type.
- **Sable SA Compat**: makes Create: Stuff & Additions work on Sable ships.
- Updated: Dusty Decorations (2.2.0, now uses GeckoLib), Gadgets & Gizmos 1.2.7, Entity Model Features 3.3.11, Entity Texture Features 7.2.5.

Findings for compat:
- **Pixel Backpacks are not in the inventory.** `[V]` Backpack Pixel stores worn packs in player data (`backpackup`, `backpackdown`, `backpackex`), not as Curios or armour. YAGM only collects inventory and Curios / Accessories, so these packs are missed. YAGM has an API (`IAccessoryCollector`, `IAccessoryRestorer`, `AccessoryLoader`) to add them.
- Two gun systems now overlap (TACZ and Iron's Artifice): ammo and recipes could be unified.

See [[To Do - Compat Features]].

## 10. Removed and added since the scan (2026-10-09, second check)

A diff of the `mods/` folder against the first scan (the first scan's list was typed back from my notes, so I only trust the clear differences):
- **Removed:** Backpacked, Wet Backpacks, Chemica, Create: Metalwork, Create Nuclear, Create Radiologistics, Create: Improved Display Link Experience, Naturalist Delight.
- **Added:** Create Immersive TaCZ (Create recipes for TACZ), TaCZ Pack Upgrader, Create Stuff 'N Additions (`create_sa`: jetpacks, propeller boots, tools).
- **Correction:** Create Stuff 'N Additions is a different mod from "Create: things and misc" (`create_things_and_misc`, turrets, cannon balls, sails). I briefly mislabelled one as the other.

What this removes from the plan: everything involving Create Nuclear (radiation x food, the FE tap, radiation alarm), Chemica and Metalwork (shared molten-metal recipes, most of the melting / casting cluster), Backpacked (the YAGM check). What it adds: TACZ is already bridged to Create, so "craft TACZ guns with Create" is not needed. See [[To Do - Compat Features]].
