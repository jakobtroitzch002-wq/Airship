# Airship

A Fabric mod for player-built airships in Minecraft 26.3.

## How it works

1. **Build** a ship from any blocks. You need one **Airship Core**, exactly **one Airship Seat** (the
   control block), **Airship Balloons** (1 per 5 blocks of the ship) and optionally **Airship Engines**
   and **cushions** for passengers. Use **Airship Build Blocks** as a separator so the ship does not touch
   the terrain: the structure scan never crosses a build block.
2. **Check it:** right-click the **Core**. The Core screen shows a checklist (Airship Seat, balloons, size,
   cushions, engines with fuel) and tells you what is missing. "Neu prüfen" checks again, "Gelände vergessen"
   clears the terrain the Core remembers from the last landing (you normally never need it). Fuel is shown on the
   engine's own screen. If the ship cannot start (no seat, not enough balloons, too big ...), the Core screen opens
   by itself and shows the problem. Nothing is written to the chat. It does not start the ship.
3. **Fly:** right-click the **Airship Seat** to sit down. The connected structure lifts off and you are the
   pilot. Only the Airship Seat steers. The seat has a direction (like a chair, it faces the way you looked when
   you placed it): that is the **front of the ship**. The ship turns so its front points where you look.
   - `W/A/S/D` move relative to where you look
   - `Space` ascend, `C` descend (rebindable under Controls, "Airship: descend")
4. **Passengers:** sit on a cushion before takeoff and you stay on it while the ship flies. Passengers cannot steer.
   Everybody who is not the pilot needs a cushion.
5. **Land:** press `Shift` to leave the Airship Seat. The ship turns back into solid blocks right where it is
   (snapped to the block grid and to the nearest 90 degrees). Passengers on cushions are put down on their cushion.
   Rebuild whatever you like and sit down again.

### Landing next to the ground or a wall

When a ship lands, the Core remembers which neighbouring blocks were terrain. The next time you sit down,
the scan skips them, so the ground is not pulled into the ship. For the very first takeoff there is nothing
remembered yet: if the ship touches the ground, separate it with **Airship Build Blocks**.

## Engines

Right-click an Airship Engine to open its screen. Fuel is added only there, by putting it into the fuel slot
(shift-click works too). The big timer shows how long the engine still runs; it only counts down while the
pilot is thrusting, so it stays put while the ship hovers or is landed. The tank holds 20:00 minutes.

| Fuel | Time per item |
| --- | --- |
| Coal, charcoal | 1:20 |
| Lava bucket | 20:00 (needs an empty tank, you get the empty bucket back) |
| Coal block | 13:20 |
| Blaze rod | 2:00 |
| Planks, logs | 0:15 |
| Stick | 0:05 |
| Bamboo | 0:02 |

### Turbo

The engine screen has a second fuel field, the **turbo tank**. Fuel goes in the same way and gives the same time
per item, but the turbo tank burns **5 times as fast** (a coal lasts 0:16 instead of 1:20). While the pilot holds
the boost key (`B`, rebindable, "Airship: turbo (hold)") and thrusts, every engine that has turbo fuel burns it
instead of its normal fuel and the ship gets much faster: top speed x2.5, acceleration x3 (scaled by how many
engines have turbo fuel). Engines without turbo fuel keep using their normal tank.

### Flight display

While you pilot a ship, a small display above the hotbar shows your speed (blocks per second), how many engines are
pushing, the fuel left in the fullest engine, and, if there is any, the turbo fuel left (as turbo time).

Every fuelled engine makes the ship faster (with diminishing returns). Without fuel the ship still flies at its
base speed. Items that do not fit into the tank anymore stay in the slot and go back to you when you close the screen.

## Look

Blocks and screens use the "Messing & Dampf" design: walnut and brass compass (Core), canvas with rope net
(Balloon), copper boiler (Engine). The block textures are in `assets/airship/textures/block/`
(`airship_<block>_top.png` and `airship_<block>_side.png`, 16x16). The Airship Seat (walnut, tufted leather,
brass studs) and the Airship Build Block (brass and walnut frame, riveted copper panels, brass compass plates)
use the same style.

## Cushions (Sitzkissen)

Cushions (new in 26.3) are entities that players can sit on. On a ship they are passenger seats:
- Players sitting on a cushion stay on that cushion while the ship flies, but they cannot steer.
- When the ship takes off, cushions are removed and drawn as a wool slab of the same colour; when it lands
  they are placed again at the same spot (rotated with the ship).

## Notes

- While flying, the ship's blocks are not world blocks: sit in your seat. Landing needs free space
  around the ship (build blocks count as obstacles when landing).
- Blocks with special rendering (chests, signs, ...) are shown with their plain block model while flying.
- See-through blocks of the ship (glass, glass panes, ice, slime, honey) are drawn in a phase after the terrain, so
  they cannot cut away the water behind them (Minecraft draws entities before water, and glass drawn with an
  entity also writes depth). Key `F8` (rebindable, "Airship: toggle glass mode") switches back to drawing them with
  the rest of the ship, to compare or if the glass does not show up.
