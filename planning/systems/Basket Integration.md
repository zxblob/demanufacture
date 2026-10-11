---
status: built
---
# Basket Integration

The Farmer's Delight Basket is the central storage for ingredients. Every minigame has a basket panel on the left.

## How it works
- On opening a minigame, the **server** scans a cube of radius 6 around the cook block for FD `Basket` block entities and merges all their contents into one list.
- The list is sent to the client in the open-screen payload. 3-wide grid, 5 visible rows, mouse wheel scrolls.
- Click an item -> server removes 1 from that basket slot and adds it to the pot ([[Doneness and Pace]] / `POT_INGREDIENTS`, max 6).
- Click an ingredient already in the pot -> it goes back to your inventory (drops at your feet if full).
- Liquid items are filtered out of this panel and shown in [[Liquid Flask]].

## Known gaps
- Click-to-select, **not** drag and drop (your sketch said drag). Cheaper to build; revisit if the feel matters. See [[Open Questions]].
- The list is a snapshot from when the screen opened - baskets changed while the screen is open don't update until you reopen.
- Only one unit per click.

## Why server-side
See [[Technical Gotchas]] - chest-like containers never sync their contents to clients, so a client-side scan sees stale/empty data.
