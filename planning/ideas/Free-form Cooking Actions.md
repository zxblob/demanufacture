---
status: idea
---
# Free-form Cooking Actions

The core architecture idea from the start, and the answer to "what if I cut this and then steam it?".

## The idea
- Stop thinking in fixed recipes. Every food carries a **FoodState**: rawness, cut profile, moisture, seasoning, char, ... and the list of actions applied to it.
- Each minigame is an **action** (cut, boil, fry, stir, steam, marinate...) that reads the current state and writes a new one based on how well you did.
- Any sequence of actions is allowed. Doing nonsense just gives a worse dish - "bad technique" lowers quality, **wrong order only limits what's possible**. (Your rule: "you can do anything, but if you do bullshit you don't get a gourmet rating".)
- Naming, backstory and quality are derived from the final state and the action history, not from a recipe match.

## Where we are
The current build is a much simpler stand-in: a list of ingredient stacks + one liquid + conversion via vanilla smelting recipes + a pace snapshot at serve time ([[Dish Output]]). It works, but dishes don't remember how they were made.

## Needed before this is real
- At least one more action that changes food (cutting - [[Chopping Board]], [[Cutting Board]]) so there is something to chain.
- A data component on the food/dish carrying the state + history.
- Rework [[Dish Output]] naming to read it.
