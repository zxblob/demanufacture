---
status: planned
---
# Art Requests (what Claude needs from you)

Everything currently uses vanilla placeholders so the mechanics could be tested. Replacing a placeholder never needs a big code change.

| #   | asset                   | spec                                                                                                        | currently                       | notes                                                              |
| --- | ----------------------- | ----------------------------------------------------------------------------------------------------------- | ------------------------------- | ------------------------------------------------------------------ |
| 1   | Cookbook GUI background | `book.png`, **192x192**, two-page open-book look, text starts ~36px from the left edge, text area ~114 wide | vanilla book.png                | Starcatcher-style look. See [[Cookbook]]                           |
| 2   | Cookbook item icon      | 16x16                                                                                                       | vanilla book item texture       |                                                                    |
| 3   | Placed cookbook         | block model + texture (a book resting on a surface)                                                         | full cube, bookshelf texture    | thin/low shape needs a model                                       |
| 4   | Cooked Dish icon        | 16x16, maybe variants per quality tier                                                                      | vanilla mushroom stew           | [[Dish Output]]                                                    |
| 5   | Minigame backdrops      | pot / pan art for the screen centre                                                                         | flat grey squares               |                                                                    |
| 6   | Ingredient-in-pot look  | how items look cooking                                                                                      | the plain item icons            | your note on the screenshot, see [[In-World Camera and Animation]] |
| 7   | Meter / button skin     | pace + doneness bars, Serve button                                                                          | flat coloured rectangles        |                                                                    |
| 8   | Liquid flask item(s)    | 16x16 per liquid                                                                                            | none (we detect existing items) | only if we build it, see [[Liquid Flask]]                          |
| 9   | Sounds                  | too fast/slow, pour, drop-in, done, ruined, served                                                          | none                            | see [[Sound Design]]                                               |
|     |                         |                                                                                                             |                                 |                                                                    |

When you add a file, drop it in the project's `src/main/resources/assets/demanufacture/` (or just tell Claude where it is) and write the filename in the table.
