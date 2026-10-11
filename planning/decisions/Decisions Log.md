---
status: built
---
# Decisions Log

Each line: what we decided, why. Newest at the bottom.

- **Mod name / id:** Demanufacture / `demanufacture`, package `com.zxblob.demanufacture`. Target Minecraft 1.21.1, NeoForge 21.1.255, ModDevGradle.
- **Roles:** you are creative direction + all art; Claude writes the code and asks for assets by name.
- **Build order:** stirring first, then more minigames. Done: [[Stirring]], [[Scrambling]].
- **Intercept via event, not mixins:** cancel `RightClickBlock` for supported blocks while in [[Cook Mode]].
- **Basket is the ingredient store; remote access within radius 6.** Picked over a carried item because FD's basket is a placed block only. See [[Basket Integration]].
- **Basket scan is server-side** ([[Technical Gotchas]]).
- **Don't depend on Starcatcher or its sibling libs** - inspected, they're unrelated to book rendering. We build our own screen and skin it ([[Cookbook]]).
- **Pace is a meter, not a flickering label** - your feedback after the first test.
- **Doneness requires pace >= 90** ([[Doneness and Pace]]).
- **Pace hitting 0 destroys the ingredients** (your pick over reset-progress or close-screen).
- **Raw -> cooked uses vanilla smelting recipes** instead of our own table.
- **Client is trusted for pace-derived events** (finish / ruin / pace at serve). Acceptable for a friends' SMP; revisit if that ever changes.
- **Click-to-select instead of drag and drop** - cheaper; open to change ([[Open Questions]]).
- **Liquids reuse existing items** (water bucket/bottle, KC Oil) rather than a new flask item - for now ([[Liquid Flask]]).
- **Cookbook controls:** plain right-click = toggle, shift = place, placed block = menu with a button ([[Cookbook]]).
- **Dish rename = vanilla anvil.** No custom rename UI.
- **Skillet screen is a sibling copy, not a subclass of stirring** - extract a base class after the third minigame, when the real shared shape is visible.
- **Placeholders everywhere for art** - ship working, swap art later ([[Art Requests]]).
- **2026-10-07 - Direction change:** Demanufacture is a **pack harmoniser; cooking is one module** (user's choice after the pack scan, see [[Pack Scan - Compat Features]]). Cooking minigames are **frozen as is** (Stirring, Scrambling, cookbook, dish naming stay; no Wok/boards for now).
- **2026-10-07 - Dish quality will write into KC's QUALITY component** (SUPERB/EXCELLENT/STANDARD/POOR) instead of our own parallel scale. Not implemented yet while cooking is frozen.
- **2026-10-07 - Hand-cooking stays a fun optional alternative**, not a quality gate (Create/KC Automation keep working equally well).
- **2026-10-07 - First pack-wide target: electricity audit** ([[Electricity Audit]]); fuel/oil standard next. Mod-vs-KubeJS split left to Claude's judgement (default: Java only where scripts can't). Adapter balance policy (free vs gated) is undecided.
- **2026-10-08 - Mod Map pruned** to only connections we can implement; the two filtered maps show only mods with at least one arrow of that colour ([[README]]).
- **2026-10-09 - Duplicate jars stay** (`tfmg` community fork + original, two `cc-tweaked` versions): they work, user does not want them touched (Open Question 13).
- **2026-10-09 - New mods in the pack:** Iron's Arms 'n Artifice (+ Iron's Lib, MT Artifice Hand Fix), Cogs For Arms (existing Create bridge for it), TACZ (+ GunsmithLib, TaCZ JS), Sable SA Compat; Dusty Decorations, Gadgets & Gizmos, EMF and ETF updated. Added a "Guns & Combat" branch to the Mod Map.
- **2026-10-09 - To-do list created:** [[To Do - Compat Features]] holds every compat idea in one place, with the two new requests (Create guns for Iron's Artifice; worn Pixel Backpacks into YAGM graves) at the top.
- **2026-10-09 - Mods removed / added (second check):** removed Backpacked, Wet Backpacks, Chemica, Create: Metalwork, Create Nuclear, Create Radiologistics, Create IDLX, Naturalist Delight; added Create Immersive TaCZ, TaCZ Pack Upgrader, Create Stuff 'N Additions. All to-do items and map arrows that depended on the removed mods were struck out or removed. TACZ is already bridged to Create.
