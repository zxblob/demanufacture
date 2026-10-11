---
status: planned
blocks: KC Chopping Board
---
# Chopping Board (KC)

## Your design
Precision cutting: a slider moves back and forth, click at the right moment to cut at the right place. Used for making slices / parting food into pieces.

## Notes
- KC block `ChoppingBoardBlock`, block entity `ChoppingBoardBlockEntity`, API interface `IChoppingBoard`, recipe type `ChoppingBoardRecipe`.
- A cut is an *action* that changes what the food is - this is where [[Free-form Cooking Actions]] gets real ("cut it, then steam it").
- Needs a decision: what does a cut produce - KC's own recipe output, or our own sliced state?

Sibling with a different feel: [[Cutting Board]].
