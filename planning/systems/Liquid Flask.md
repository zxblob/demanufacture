---
status: partial
---
# Liquid Flask

Right-hand panel in every minigame. Pick a liquid with `<` `>`, click the icon to pour it into the pot. One liquid at a time; pouring a new one replaces the old.

## Recognised liquids (`LiquidItems`)
- Vanilla water bucket (you get the empty bucket back)
- Vanilla water bottle
- KC `Oil` item
- Pulled automatically from the merged baskets ([[Basket Integration]]) - no separate filling step.

## What liquid does today
- A pot only starts cooking when it has **both** an ingredient and a liquid ([[Doneness and Pace]]).
- Name prefix in [[Dish Output]]: water = Boiled, oil = Fried, none = Raw.
- Nothing else yet - it does not change quality or flavour.

## Open / planned
- **Water/Oil should become necessary later** (you said): specific dishes need a specific liquid, not just "any liquid".
- Wine and other drinks: needs `kaleidoscope_world_liquor` as a real dependency first. See [[Open Questions]].
- A dedicated flask item (so detection is trivial instead of matching bucket/bottle/oil) was floated but deferred. Would need art - see [[Art Requests]].
- Glass bottle is not returned when a water bottle is used (bucket is).
- Wok needs oil poured **before** its minigame starts ([[Wok]]).
