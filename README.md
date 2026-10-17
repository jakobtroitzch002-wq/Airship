# Airship

A Fabric mod for player-built airships in Minecraft 26.3.

## How it works

1. **Build** a ship from any blocks. You need one **Airship Core**, exactly **one Airship Seat** (the
   control block), **Airship Balloons** (1 per 5 blocks of the ship) and optionally **Airship Engines**
   and **cushions** for passengers. Use **Airship Build Blocks** as a separator so the ship does not touch
   the terrain: the structure scan never crosses a build block.
2. **Check and register it:** right-click the **Core**. This also *registers* the ship: the Core saves exactly which
   blocks belong to it. Blocks you place later are **not** part of the ship until you check the Core again.
   The Core screen shows a checklist (Airship Seat, balloons, size,
   cushions, engines with fuel) and tells you what is missing. "Neu prüfen" checks again, "Gelände vergessen"
   clears the terrain the Core remembers from the last landing (you normally never need it). Fuel is shown on the
   engine's own screen. If the ship cannot start (no seat, not enough balloons, too big ...), the Core screen opens
   by itself and shows the problem. Nothing is written to the chat. It does not start the ship.
3. **Fly:** right-click the **Airship Seat** to sit down. The ship registered by the Core lifts off (a ship that
   was never checked shows "Nicht registriert" on the Core screen) and you are the
   pilot. Only the Airship Seat steers. The seat has a direction (like a chair, it faces the way you looked when
   you placed it): that is the **front of the ship**. The ship turns so its front points where you look.
   - `W/A/S/D` move relative to where you look
   - `Space` ascend, `C` descend (rebindable under Controls, "Airship: descend")
4. **Passengers:** sit on a cushion before takeoff and you stay on it while the ship flies. Passengers cannot steer.
   Everybody who is not the pilot needs a cushion.
5. **Get off:** press `Shift` to leave the Airship Seat. In the air the ship **stays exactly where it is**, hovering,
   and you stand on it (passengers can stay seated). Right-click its Airship Seat to steer it again.
   While a ship flies or hovers you can also right-click its **engines** (add fuel) and its **Core**. The Core
   screen of a flying ship has the button **Snap to grid**: the ship turns into blocks right where it is, so you
   can rebuild it in the air; check the Core and sit down again to fly on.
6. **Land:** fly down until the ship rests on the ground, then get off. Only then does it turn back into solid blocks
   (snapped to the block grid and to the nearest 90 degrees). Passengers on cushions are put down on their cushion.
   Rebuild whatever you like and sit down again. A hovering ship without enough lift (balloons shot down) slowly
   sinks and lands by itself.

### Landing next to the ground or a wall

When a ship lands, the Core remembers which neighbouring blocks were terrain. The next time you sit down,
the scan skips them, so the ground is not pulled into the ship. For the very first takeoff there is nothing
remembered yet: if the ship touches the ground, separate it with **Airship Build Blocks**.

## Engines

Right-click an Airship Engine to open its screen. Fuel is added only there, by putting it into the fuel slot
(shift-click works too). The big timer shows how long the engine still runs; it only counts down while the
pilot is thrusting, so it stays put while the ship hovers or is landed. The normal tank holds up to 1 hour (60:00), the turbo tank up to 10 minutes of turbo (10:00).

| Fuel | Time per item |
| --- | --- |
| Coal, charcoal | 1:20 |
| Lava bucket | 20:00 (you get the empty bucket back) |
| Coal block | 13:20 |
| Blaze rod | 2:00 |
| Planks, logs | 0:15 |
| Stick | 0:05 |
| Bamboo | 0:02 |

### Turbo

The engine screen has a second fuel field, the **turbo tank**. Fuel goes in the same way and gives the same time
per item, but the turbo tank burns **5 times as fast** (a coal lasts 0:16 instead of 1:20). While the pilot holds
the boost key (`B`, rebindable, "Airship: turbo (hold)") and thrusts, every engine that has turbo fuel burns it
instead of its normal fuel and the ship gets much faster: top speed 15 blocks per second and 3x the acceleration (scaled by how many
engines have turbo fuel). Engines without turbo fuel keep using their normal tank.

### Flight display

While you pilot a ship, a small compact display right above your hearts shows your speed (blocks per second), how
many engines are pushing, the fuel left in the fullest engine (time and bar), and, if there is any, the turbo fuel
left (as turbo time, marked with T).

### Speeds

| State | Top speed |
| --- | --- |
| No fuel | 2.5 blocks per second |
| With fuel | 5 blocks per second |
| With turbo | 15 blocks per second |

With several engines the speed is blended by the share of engines that have fuel (or turbo fuel). Items that do not
fit into the tank anymore stay in the slot and go back to you when you close the screen.

## Look

Blocks and screens use the "Messing & Dampf" design: walnut and brass compass (Core), canvas with rope net
(Balloon), copper boiler (Engine). The block textures are in `assets/airship/textures/block/`
(`airship_<block>_top.png` and `airship_<block>_side.png`, 16x16). The Airship Seat (walnut, tufted leather,
brass studs) and the Airship Build Block (brass and walnut frame, riveted copper panels, brass compass plates)
use the same style.

## Mobs and items on the ship

The ship's blocks are not world blocks while it flies, so nothing could stand on them. **Mobs, boats and minecarts standing
on the ship come along automatically**: at takeoff they get on board and ride along where they stand. When the ship lands, they
are put down where they were carried. The Core screen shows how many are on board ("Tiere/Boote").

Things that cannot come along stop the takeoff: **dropped items, item frames and paintings** on the ship (the Core
screen shows "Items/Rahmen an Bord: N"). Players and cushions are fine.

Leads do not work in flight. A lead tied to a fence of the ship is released at takeoff (the fence is gone) and the
lead goes back to your inventory.

## Cushions (Sitzkissen)

Cushions (new in 26.3) are entities that players can sit on. On a ship they are passenger seats:
- Players sitting on a cushion stay on that cushion while the ship flies, but they cannot steer.
- When the ship takes off, cushions are removed and drawn as a wool slab of the same colour; when it lands
  they are placed again at the same spot (rotated with the ship).

## Tiers: cheaper parts for small ships

| Part | Difference | Recipe |
| --- | --- | --- |
| Wooden Airship Core | holds ships of up to **25 blocks** | 4 logs (corners) + 4 planks around 1 iron ingot |
| Iron Airship Core | holds ships of up to **100 blocks** | 8 iron ingots around a Wooden Airship Core |
| Airship Core | holds ships of up to **500 blocks** | see Survival |
| Simple Airship Balloon | carries only **3 ship blocks** (a Balloon carries 5), pops from **any** projectile | wool over a stick |
| Reinforced Airship Balloon (makes 4) | like a Balloon, but only pops from **tridents** and **crossbow arrows with Piercing III** | 4 Balloons (corners) + 4 leather + 1 iron ingot (middle) |
| Simple Airship Engine | gives **60 %** of the fuel speed boost, has **no turbo** and burns **twice the fuel** | iron ingot, cobblestone, furnace, 3 planks |
| Large Airship Balloon | carries **8 ship blocks**, pops like a Balloon | 2 leather (top, bottom) + 2 iron ingots (sides) + 1 Balloon (middle) |
| Sky Balloon | carries **12 ship blocks**, only pops from tridents and crossbow arrows with Piercing III | 2 phantom membranes (top, bottom) + 2 wool (sides) + 1 Large Balloon (middle) |
| Advanced Airship Engine | gives **250 %** of the fuel speed boost, has turbo | 4 diamonds (corners), redstone block (top), 2 blaze rods (sides), Airship Engine (middle), blast furnace (bottom) |

All core limits are capped by `maxBlocks` from the config. The Core screen shows how many ship blocks the balloons
carry and how many are needed. A Simple Engine has no turbo tank, so its
screen only has the fuel slot. Mixed ships work: speed is blended by how much of the engine power is fuelled.

## Walking on a flying ship

Players who are not seated can stand and walk on a flying ship: it moves and turns them along while they stand
on it, and they can still walk around and jump. A player who gets off a seat while the ship flies is put on top of
the seat and carried along too. After a jump players keep moving along with the ship as long as they are above it;
only who jumps or falls off the side is left behind. The blocks keep their exact shapes (slabs, stairs, fences ...) at every angle.

## Shooting down balloons

Projectiles pop balloons, placed ones and those of a flying ship (no drop):

| Balloon | Pops from |
| --- | --- |
| Simple Airship Balloon | every projectile: snowballs, eggs, arrows ... |
| Airship Balloon | arrows (bow, crossbow, dispenser), tridents, fireballs, wither skulls, fireworks |
| Reinforced Airship Balloon | tridents, and arrows from a crossbow with Piercing III |

Weaker projectiles bounce off. On a flying ship the hull shields the balloons behind it, and a Piercing arrow
can pop one more balloon per level. If the ship no longer has enough lift, it cannot climb and slowly sinks.
The projectile lists are the entity type tags `airship:pops_balloon` and `airship:pops_reinforced_balloon`, so a
data pack can change them.

## Survival

### Crafting

All recipes are in the recipe book from the start, for every player (also in existing worlds).

| Block | Recipe |
| --- | --- |
| Airship Core | Iron Airship Core in the middle, 4 compasses in the corners, 4 gold ingots on the sides |
| Airship Balloon (makes 2) | wool, wool, string (in a column) |
| Airship Seat | cushion, 3 planks in a row, leather (in a column) |
| Airship Build Block (makes 4) | 4 iron nuggets around a glass pane (plus shape) |
| Airship Engine | 5 iron ingots + furnace + piston |

There are also four small advancements in the Adventure tab (one per craftable ship part; no chat messages).

### Breaking and tools

| Block | Hardness | Tool | Sound |
| --- | --- | --- | --- |
| Core | 3.0 (blast resistance 6) | pickaxe (drops only with one) | metal |
| Engine | 3.5 (blast resistance 6) | pickaxe (drops only with one) | metal |
| Balloon | 0.8 | any (like wool), flammable | wool |
| Seat | 1.5 (blast resistance 3) | axe, flammable | wood |
| Build Block | 0.5 | hoe | grass |

### Keeping the ship safe

- The ship cannot fly below the bottom of the world or above the build height (important in a void world): both
  are solid walls for it.
- If the ship entity is ever removed other than by landing (for example with `/kill`), it tries to land first instead
  of destroying everything on board.
- While a ship flies, its blocks are not world blocks: furnaces, hoppers, redstone and crops on board do not work until
  it lands again.

### Settings

`config/airship.json` is created on the first start. It holds: `maxBlocks` (largest ship), `blocksPerBalloon`,
`speedNoFuel`, `speedFuel`, `speedTurbo` (blocks per second), `tankMinutes`, `turboTankMinutes` and `turboBurnRate`.
Values are checked for sensible limits. On a server, client and server should use the same file.

## Notes

- While flying, the ship's blocks are not world blocks: sit in your seat. Landing needs free space
  around the ship (build blocks count as obstacles when landing).
- Blocks with special rendering (chests, signs, ...) are shown with their plain block model while flying.
- See-through blocks of the ship (glass, glass panes, ice, slime, honey) are drawn in a phase after the terrain, so
  they cannot cut away the water behind them (Minecraft draws entities before water, and glass drawn with an
  entity also writes depth). Key `F8` (rebindable, "Airship: toggle glass mode") switches back to drawing them with
  the rest of the ship, to compare or if the glass does not show up.
