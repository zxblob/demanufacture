---
status: idea
---
# Open Questions

Write your answer directly under a question (start with `A:`). Claude reads this file at the start of a session.

1. **Next minigame:** [[Wok]] first, or the boards ([[Chopping Board]] / [[Cutting Board]])?
A: Your Call

2. **Dedicated liquid item?** Right now we detect water bucket / bottle / KC Oil. Do you still want one flask item that holds any liquid (needs art, needs a fill mechanic)? See [[Liquid Flask]].
A: tell me what you recommend

3. **Wine and other drinks:** OK to add `kaleidoscope_world_liquor` as a real dependency of the mod?
A: Yes i have some plans with that mod

4. **Drag and drop:** is click-to-select good enough in the basket, or do you want true dragging? See [[Basket Integration]].
A: Select but have a cool animation where it drops into the pot in an arch

5. **Held cookbook:** should it also open the book menu instead of instantly toggling cook mode? See [[Cookbook]].
A: only toggle as item 

6. **Should liquid change the dish?** (quality, flavour, which liquid a recipe needs.) You said water/oil should be necessary later - how strict?
A: oil for meats should be almost required but other than that only for flavor

7. **Should dishes remember how they were made?** That means building [[Free-form Cooking Actions]] properly, which gets much bigger once cutting exists.
A: yes we need players to make their favorite dishes and remember how to

8. **Quality tiers and tone:** Questionable / Decent / Tasty / Masterful and the Mad-Libs jokes in [[Dish Output]] - keep, or rewrite?
A: keep until further notice

9. **Skillet feel:** [[Scrambling]] is untested by you. Does it feel different enough from stirring?
A: feels similar so add the flip mechanic i mentioned where you have to click aswell as shake (but make the shakeing less unforgiving)

10. **Heat:** should minigames require the stove under the pot to actually be lit? ([[Stove]])
A: no only to activate  the minigame (like block placement like cookpot)

11. **Cookbook look:** when can you make the 192x192 book background? ([[Art Requests]])
A: as soon as i can 

## Pack harmoniser questions (2026-10-07)

12. **Adapter balance:** when machine A also runs machine B's recipes (e.g. TFMG Chemical Vat running Create mixing), is that free convenience, gated by the source mod's requirements, or a per-adapter config? See [[Pack Scan - Compat Features]] section 8.
A: (you said "we will have to think of something")

13. **Duplicate jars:** `tfmg-1.2.2` + `tfmg-1.21.1-1.3.2a-community`, and two `cc-tweaked` versions are both in the mods folder. Which should stay? See [[Electricity Audit]].
A: stay tfmg-ce is a fork and two CCs just work so im not topuching it 

14. **Electricity playtest:** can you run a PG -> FE -> EE energy loop in the pack and tell me whether energy grows, shrinks or stays flat? See [[Electricity Audit]] section 5.
A: thats the paart we are trying to implemetn , the cables cant connect to eachother 

## New mods / new requests (2026-10-09)

15. **"Create guns" for Iron's Arms 'n Artifice:** Cogs For Arms is already installed and covers Create-built parts (cogs, stocks, barrels, cylinders, magazines, triggers, ammo boxes). (For TACZ, Create Immersive TaCZ already adds Create-made barrels, triggers, casings and primers.) What do you want on top for Iron's Arms 'n Artifice? For example: automating bullets and casings, Create fluids as modifiers, or something else? See [[To Do - Compat Features]] section 0.
A: just new guns for now 

16. **Pixel Backpack grave behaviour:** when you die, should the worn Pixel Backpacks (all three slots) go into the grave with their contents, and come back onto you when you open it? Or only drop as items? And should they also stay on you after respawn (which would duplicate them)? See [[To Do - Compat Features]] section 0.
A: just put them in the grave , when i died they fropped on the ground which we dont want since it would despawn and anger the player
