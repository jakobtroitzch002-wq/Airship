package com.airship;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The engine's screen: one fuel slot plus the player's inventory. Coal and charcoal put into the slot are
 * turned into fuel right away (as long as the engine has room); whatever is left goes back to the player.
 */
public class AirshipEngineMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOTS = 36;

    private final AirshipEngineBlockEntity engine; // null on the client
    private final ContainerData data;
    private final SimpleContainer fuelContainer;

    /** Client side. */
    public AirshipEngineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(1));
    }

    /** Server side. */
    public AirshipEngineMenu(int containerId, Inventory inventory, AirshipEngineBlockEntity engine, ContainerData data) {
        super(ModMenus.ENGINE, containerId);
        this.engine = engine;
        this.data = data;
        this.fuelContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                AirshipEngineMenu.this.slotsChanged(this);
            }
        };

        addSlot(new Slot(fuelContainer, 0, AirshipEngineLayout.FUEL_X + 1, AirshipEngineLayout.FUEL_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + column + row * 9,
                        AirshipEngineLayout.INV_X + 1 + column * 18,
                        AirshipEngineLayout.INV_Y + 1 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                    AirshipEngineLayout.INV_X + 1 + column * 18,
                    AirshipEngineLayout.INV_Y + 1 + 58));
        }
        addDataSlots(data);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    /** Remaining fuel of the engine, in ticks of thrust. */
    public int getFuel() {
        return data.get(0);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == fuelContainer) {
            absorbFuel();
        }
    }

    private void absorbFuel() {
        if (engine == null) {
            return;
        }
        ItemStack stack = fuelContainer.getItem(0);
        while (!stack.isEmpty()
                && engine.getFuel() + AirshipEngineBlock.FUEL_PER_ITEM <= AirshipEngineBlockEntity.MAX_FUEL) {
            engine.addFuel(AirshipEngineBlock.FUEL_PER_ITEM);
            stack.shrink(1);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, 1 + PLAYER_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFuel(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (engine == null) {
            return true;
        }
        return player.distanceToSqr(
                engine.getBlockPos().getX() + 0.5, engine.getBlockPos().getY() + 0.5, engine.getBlockPos().getZ() + 0.5)
                <= 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, fuelContainer);
        }
    }
}
