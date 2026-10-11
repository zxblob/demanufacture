---
status: partial
---
# Cookbook

## What exists
- **Item** `demanufacture:cookbook` (placeholder icon: vanilla book). Plain right-click toggles [[Cook Mode]]; shift+right-click on a block places it.
- **Block** `demanufacture:cookbook_block` - plain full cube with the bookshelf texture as a stand-in. Right-click opens the book menu (it does **not** toggle directly). Breaking it drops the item.
- **Menu:** two-page book, centred, using vanilla's 192x192 book layout and page-turn arrows (those sprites are built in, no art needed).
  - Page 1 Settings: Cook Mode button at the bottom of the page.
  - Page 2 Recipes: last served dish + "Served: N". See [[Dish Output]].

## Styling goal
Look like the Starcatcher book. Decision: **do not** depend on Starcatcher / Fancy Tab Sections / LibTooltips (inspected the jars - unrelated libraries). Build our own screen and skin it.
- Needs a 192x192 `book.png` from you - see [[Art Requests]].
- Code currently points at vanilla's `textures/gui/book.png`; swapping to ours is changing one constant in `CookbookMenuScreen`.

## Still to do
- Real recipe pages: scrollable history of every served dish, with its name, story, quality.
- Step-by-step instructions with arrows showing which ingredient is next (from your first message).
- Per-recipe rename inside the book (right now: vanilla anvil).
- Held book still toggles instantly - you may want it to open the menu too. See [[Open Questions]].
