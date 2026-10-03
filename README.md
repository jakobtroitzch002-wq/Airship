# Airship

A Fabric mod for player-built airships in Minecraft 26.3.

## How it works

1. **Build** a ship from any blocks. Place the **Airship Core**, at least one **Airship Seat**,
   **Airship Balloons** (1 per 5 blocks of the ship) and optionally **Airship Engines**.
   Use **Airship Build Blocks** as a separator so the ship does not touch the terrain:
   the structure scan never crosses a build block.
2. **Right-click the Core.** Every block connected to it (except air and build blocks) becomes an airship.
   Everyone standing on the ship is seated automatically (so you need one seat per player).
3. **Fly.** The first passenger is the pilot:
   - `W/A/S/D` move relative to where you look
   - `Space` ascend, `Ctrl` descend
   - `Shift` leaves the seat
4. **Land.** When nobody sits in the ship it sinks slowly and turns back into blocks once it touches the ground.
   Ships fly through build blocks but need free space to land.

## Engines

Right-click an Airship Engine with coal or charcoal to fuel it (before takeoff). Every fuelled engine
burns fuel while the pilot is thrusting and makes the ship faster (with diminishing returns).
Without fuel the ship still flies at its base speed.

## Current limitations

- The ship translates only, it does not rotate.
- Players cannot walk on the deck while it flies; they ride in seats.
- Blocks with special rendering (chests, signs, ...) are shown with their plain block model while flying.
