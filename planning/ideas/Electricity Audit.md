---
status: partial
updated: 2026-10-07
---
# Electricity audit

First pack-wide compat target ([[Pack Scan - Compat Features]] 7.1). This is a **read-only audit** of the jars and config files; nothing here was tested in game. Tags: `[V]` = seen in a jar/config, `[I]` = my inference, `[?]` = unknown, needs an in-game test.

## 1. The systems in the pack

| System                                                     | Model                                         | Speaks FE?                                                                                                                                                           | Notes                                                                                                                                 |
| ---------------------------------------------------------- | --------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **Create Crafts & Additions** (CA)                         | Kinetic <-> FE                                | Yes, native `[V]` 26 classes use `IEnergyStorage`                                                                                                                    | Alternator `generator_efficiency 0.75`, `fe_at_max_rpm 480`, small connector 1000 in/out, large 5000, accumulator 2,000,000 FE        |
| **Create Propulsion**                                      | Own FE cable network                          | Yes `[V]` `FeCableBlock`, `CableNetworkManager`                                                                                                                      | Cable "Energy transfer" 1000. A **second cable network** parallel to CA wires                                                         |
| **Create: Power Grid** (PG)                                | Simulated physical electricity (watts, volts) | Bridge `[V]` `FEBridgeEnergyStorage`, config `forgeEnergyPerWatt = 10.0`, `feInverterBufferSize 20000`                                                               | The storage class I read is **extract-only** (`canReceive=false`) `[V]`; I did not check the reverse direction (the "inverter") `[?]` |
| **Create Electro Energetics** (EE)                         | Simulated physical electricity                | Converter block `[V]`, both source and sink modes (`isSource`), config `wattFeTConversionRate = 34` ("this many watts is one FE/tick"), `converterMaxPower = 100 kW` | Has its own plant oil fluid                                                                                                           |
| **TFMG**                                                   | Own voltage/resistance network (`IElectric`)  | Partial `[V]` `TFMGForgeEnergyStorage`, `AccumulatorBlockEntity` stores "ForgeEnergy" (2,500,000 cap constant)                                                       | Generators use stress values; no FE *cable* network of its own that I found                                                           |
| ~~Create Nuclear~~ | (removed from the pack 2026-10-09) | n/a | Was rotation-only with a radiation capability. No longer relevant. |
| **Petrochem**                                              | Consumes/produces FE                          | Yes `[V]` electrolyzer consumes, turbine produces                                                                                                                    | Uses CA/FE side                                                                                                                       |
| **Coasters: Engineered**, **Numismatics**, **CC: Tweaked** | Use FE for gadgets / read it                  | Yes `[V]`                                                                                                                                                            | Leaf consumers, not networks                                                                                                          |
| **VS-Sable-HoseConnectors**                                | Wire connector bridging ship/world            | FE `[V]` `ElectricWireConnectorBlockEntity`                                                                                                                          | Sublevel (Sable) wiring, for CA wires                                                                                                 |
| `sable_createaddition_compat`                              | Makes CA wires work across Sable sub-levels   | n/a                                                                                                                                                                  | Evidence that cross-ship wiring is already a known problem                                                                            |

## 2. What is already harmonised
- **FE is the common currency.** CA, Propulsion, PG, EE, TFMG (accumulator), Petrochem all expose it.
- **Physical-simulation mods have FE converters** (PG bridge, EE converter), so you *can* connect them to CA machines.
- **Create kinetic power is the other currency**: TFMG generators feed stress; CA and PG turn that into electricity. (Create Nuclear used to as well; it was removed 2026-10-09.)

## 3. Gaps and risks found
1. **Conversion rates disagree and are not obviously normalised `[V][?]`.** PG: `forgeEnergyPerWatt = 10`. EE: `34 W = 1 FE/tick`. CA: `480 FE at max RPM`, alternator `0.75` efficiency. The time base of PG's number is not stated, so I cannot tell if a PG -> FE -> EE loop gains or loses energy. **Needs an in-game loop test**, because an unbalanced loop is either a free-energy exploit or a dead end.
2. **Two or three separate FE cable networks** `[V]`: CA wires/connectors (16/32 block lengths), Propulsion cables, plus PG/EE/TFMG cables that are their own physical networks. Same FE inside, different items and rules outside.
3. **One-way bridge risk `[V]`**: PG's bridge class does not accept FE input. If there is no inverter in the other direction in practice, PG can power FE machines but FE cannot power PG machines. `[?]` verify the inverter block.
4. ~~Nuclear is isolated~~ **No longer applies:** Create Nuclear was removed from the pack on 2026-10-09.
5. **Duplicate jars `[V]`**: `tfmg-1.2.2.jar` and `tfmg-1.21.1-1.3.2a-community.jar` are both in `mods/` (same mod id `tfmg`), and likewise `cc-tweaked ... 1.113.1` and `1.120.2`. NeoForge normally refuses duplicate ids or picks one. Check the log to see which actually loads and remove the other.
6. **Sable (ship) sub-levels** need wiring compat per mod; only CA has one visible (`sable_createaddition_compat`) `[V]`. PG/EE/Propulsion cables across ships: `[?]`.

## 4. What Demanufacture could do (not decided)
| # | Idea | Why | Effort |
| --- | --- | --- | --- |
| E1 | **Normalise units**: one config table "1 FE = X W = Y SU" and set each mod's rates from it | removes loop exploits and the guesswork | M |
| E2 | **Universal cable/connector**: any wire item connects networks of different mods | the player-facing "it all just works" | L |
| ~~E3~~ | ~~Nuclear -> FE tap~~ | **n/a, Create Nuclear removed** | - |
| E4 | **Ship-safe wiring** for PG/EE/Propulsion cables across Sable sub-levels | the pack is about ships | L, risky |
| E5 | **Shared meter**: one goggles/Jade readout showing W, FE/t and SU for any block | all mods show different units | S |
| E6 | Do nothing in code, ship a **balance sheet** note (this page) and tune configs | cheapest; configs already expose most rates | S |

## 5. Next steps if we continue
1. Playtest the PG -> FE -> EE loop and CA alternator -> PG loop to see which direction leaks or starves (needs the user in game; Claude cannot drive the game window).
2. Read the PG inverter block to confirm the FE input direction.
3. Check `logs/` for which of the duplicate jars loads.
4. Pick E1/E5/E6 for a first, low-risk step.

Related: [[Compat Roadmap]], [[Pack Scan - Compat Features]].

> **Update 2026-10-09:** Create Nuclear (and Chemica, Create: Metalwork, Create Radiologistics) were removed from the pack. Rows and options that mention them are struck through. Everything else here is unchanged and still unverified in game.
