---
status: planned
blocks: KC Wok (internally "Pot")
---
# Wok

## Your design
1. **Pour oil first** - inside the minigame screen, as its first stage, using the [[Liquid Flask]] panel.
2. Then shake the pan: move the mouse back and forth for normal shaking.
3. **Left-click at the right moment to flip** the food.

## Facts found in KC 1.6.0
- The block you called Wok is `kaleidoscope_cookery:pot` (class `PotBlock`, lang name "Wok"). Registry field `ModBlocks.POT`.
- It already has blockstate flags `HAS_OIL`, `SHOW_OIL`, `HAS_BASE`; KC's own tooltip: "Place on heat source, add oil, add ingredients, stir-fry, serve."
- KC's "Oil Can" is a different block (`OilPotBlock`, an oil counter). Its item `kaleidoscope_cookery:oil` comes from killing pigs/piglins/hoglins with a kitchen knife.
- Block opens its menu in `useItemOn`, so it is interceptable the same way as the others ([[Cook Mode]]).

## Open
- How does the pre-minigame oil step interact with KC's own `HAS_OIL` state? Mirror it or ignore it?
- Flip timing window and what a missed flip does (burn? quality drop?). See [[Open Questions]].
- Needs a new payload + screen like [[Scrambling]] did.

Likely **next minigame** along with the boards.
