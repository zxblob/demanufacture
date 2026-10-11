---
status: built
---
# Cook Mode

The switch that turns right-clicking a cook block into a minigame instead of that block's normal menu.

## How it works
- Per-player flag, attachment `demanufacture:cook_mode` (synced, **not** saved - resets on login on purpose).
- **Held [[Cookbook]]:** plain right-click toggles it (looking at air or at a block). Shift+right-click on a block places the cookbook instead.
- **Placed cookbook:** right-click opens the book menu, which has a Cook Mode button (see [[Cookbook]]).
- **Interception:** `PlayerInteractEvent.RightClickBlock` is cancelled on both sides when cook mode is on and the block is supported. No mixins needed. The server then does the [[Basket Integration]] scan and tells the client which screen to open.

## Supported blocks (in `CookBlockInteractionHandler`)
- Stirring: FD Cooking Pot, KC Stockpot -> [[Stirring]]
- Scrambling: FD Skillet -> [[Scrambling]]
- Not yet: [[Wok]], [[Chopping Board]], [[Cutting Board]], [[Stove]]

Adding a block = add it to one of the sets (or a new set + payload for a new minigame).
