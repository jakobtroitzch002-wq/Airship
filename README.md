# Airship

A Fabric mod for player-built airships in Minecraft 26.3.

## How it works

1. **Build** a ship from any blocks. You need one **Airship Core**, exactly **one Airship Seat** (the
   control block), **Airship Balloons** (1 per 5 blocks of the ship) and optionally **Airship Engines**
   and **cushions** for passengers. Use **Airship Build Blocks** as a separator so the ship does not touch
   the terrain: the structure scan never crosses a build block.
2. **Check it:** right-click the **Core**. It reports blocks, balloons, cushions and engines and tells you what
   is missing. It does not start the ship.
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

Right-click an Airship Engine with coal or charcoal to fuel it (before takeoff). Every fuelled engine
burns fuel while the pilot is thrusting and makes the ship faster (with diminishing returns).
Without fuel the ship still flies at its base speed.

## Cushions (Sitzkissen)

Cushions (new in 26.3) are entities that players can sit on. On a ship they are passenger seats:
- Players sitting on a cushion stay on that cushion while the ship flies, but they cannot steer.
- When the ship takes off, cushions are removed and drawn as a wool slab of the same colour; when it lands
  they are placed again at the same spot (rotated with the ship).

## Notes

- While flying, the ship's blocks are not world blocks: sit in your seat. Landing needs free space
  around the ship (build blocks count as obstacles when landing).
- Blocks with special rendering (chests, signs, ...) are shown with their plain block model while flying.
