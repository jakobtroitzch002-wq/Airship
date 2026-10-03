# Airship

A Fabric mod for player-built airships in Minecraft 26.3.

## How it works

1. **Build** a ship from any blocks. Place the **Airship Core**, at least one **Airship Seat**,
   **Airship Balloons** (1 per 5 blocks of the ship) and optionally **Airship Engines**.
   Use **Airship Build Blocks** as a separator so the ship does not touch the terrain:
   the structure scan never crosses a build block.
2. **Right-click the Core.** Every block connected to it (except air and build blocks) becomes an airship.
   Everyone standing on the ship is seated automatically (so you need one seat per player).
3. **Fly.** The first passenger is the pilot. The ship turns to face the way you look.
   - `W/A/S/D` move relative to where you look
   - `Space` ascend, `Ctrl` descend
   - `Shift` leaves the seat
4. **Hover.** When nobody flies it, the ship stays where it is.
5. **Board again:** right-click the ship.
6. **Land / rebuild:** with nobody sitting in it, sneak + right-click the ship. It turns back into blocks
   (snapped to the nearest 90 degrees). Rebuild as you like, then right-click the Core again.

## Engines

Right-click an Airship Engine with coal or charcoal to fuel it (before takeoff). Every fuelled engine
burns fuel while the pilot is thrusting and makes the ship faster (with diminishing returns).
Without fuel the ship still flies at its base speed.

## Current limitations

- Players cannot walk on the deck while it flies; they ride in seats. Leaving the seat mid-air means falling.
- Blocks with special rendering (chests, signs, ...) are shown with their plain block model while flying.
