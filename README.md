# Airship

A Fabric mod for player-built airships in Minecraft 26.3.

## How it works

1. **Build** a ship from any blocks. You need one **Airship Core**, at least one **Airship Seat**,
   **Airship Balloons** (1 per 5 blocks of the ship) and optionally **Airship Engines**.
   Use **Airship Build Blocks** as a separator so the ship does not touch the terrain:
   the structure scan never crosses a build block.
2. **Sit down:** right-click a seat (or the Core). Every block connected to it (except air and build
   blocks) lifts off as an airship. Everyone standing on the ship is seated automatically,
   so you need one seat per player.
3. **Fly.** The first passenger is the pilot. The ship turns to face the way you look.
   - `W/A/S/D` move relative to where you look
   - `Space` ascend, `Ctrl` descend
4. **Land:** press `Shift` to leave the seat. As soon as the last player leaves, the ship turns back
   into solid blocks right where it is (snapped to the block grid and to the nearest 90 degrees),
   and you are standing on the seat. Rebuild whatever you like and sit down again.

## Engines

Right-click an Airship Engine with coal or charcoal to fuel it (before takeoff). Every fuelled engine
burns fuel while the pilot is thrusting and makes the ship faster (with diminishing returns).
Without fuel the ship still flies at its base speed.

## Notes

- While flying, the ship's blocks are not world blocks: sit in your seat. Landing needs free space
  around the ship (build blocks count as obstacles when landing).
- Blocks with special rendering (chests, signs, ...) are shown with their plain block model while flying.
