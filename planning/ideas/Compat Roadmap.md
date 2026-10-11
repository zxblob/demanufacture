---
status: idea
---
# Compat Roadmap

**Demanufacture** (named after the album) is meant to be a general compat mod: your observation is that the mods in the pack each have their own ecosystem and don't harmonise. Cooking (Farmer's Delight + Kaleidoscope Cookery) is the first thing, not the only thing.

## Principles we've followed
- Reuse what other mods already provide instead of reinventing it: FD's Basket for storage, vanilla smelting recipes for raw -> cooked, KC's Oil item.
- Detect through standard hooks (events, capabilities, interfaces) before reaching for mixins.
- Keep the interception generic: "supported block -> which minigame" is just a set per minigame ([[Cook Mode]]).

## Other mods already in the pack that touch cooking
`kaleidoscopecookery_automation`, `kaleidoscope_contraption`, `kaleidoscope_ImmersiveEating`, `kaleidoscopetavern`, `kaleidoscope_world_liquor`, `farmerspizzeria`. A disabled `kaleidoscope_compat` jar also exists - possibly prior art for something.

## Pack scan (2026-10-07)
Full list of candidate features found by reading the pack's jars: [[Pack Scan - Compat Features]]. Key findings: Create already automates FD/KC cooking, and KC has its own quality + recipe-memory systems we should plug into.

## Electricity (first pack-wide target)
Audit done, findings and options in [[Electricity Audit]].

## Beyond cooking
Not decided. Add your ideas here or as new cards on the canvas.
See also [[Other Cook Blocks]].
