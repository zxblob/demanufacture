---
status: partial
---
# Dish Output

What you get when you press **Serve** in a minigame.

## How it works (v1)
- Serve consumes everything in the pot (cooked or not) and gives one `demanufacture:cooked_dish` (edible, nutrition 6).
- **Quality** is decided on the server:
  - any ingredient still raw (still has a smelting recipe) -> **Undercooked**, regardless of pace
  - otherwise by pace at the moment of serving: <40 Questionable, <70 Decent, <90 Tasty, else Masterful
- **Name:** `[quality word] [Boiled/Fried/Raw] [up to 2 ingredient names] & More`, e.g. "Masterful Boiled Steak & Potato".
- **Backstory:** a Mad-Libs line ("Legend says a sleep-deprived wizard accidentally discovered this dish under a full moon.") plus a quality remark, stored as the item's Lore so it shows in the normal tooltip.
- **Renaming** is free: put the dish in a vanilla anvil.
- Every served dish is logged to a persistent per-player list that the [[Cookbook]] Recipes page reads.
- Word banks and tone are plain string arrays in `DishNaming.java` - rewrite them freely.

## Known gaps
- The dish item doesn't remember what went into it - the name is the only trace. Real version = [[Free-form Cooking Actions]].
- Recipes page shows only the last dish + a count, not a scrollable history of every named dish.
- Liquid doesn't affect quality ([[Liquid Flask]]).
- Quality only looks at pace at the instant of serving, not the whole cook.
- Dish icon is a placeholder (mushroom stew) - see [[Art Requests]].
- The "instructions with arrows" cookbook pages from the original pitch are not started.
