package br.com.residenciasaldeoes.block;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/** A normal animated 27-slot chest whose inventory only accepts this mod's emerald fragments. */
public final class EmeraldChestBlockEntity extends ChestBlockEntity {
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new InvWrapper(this) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && !stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())) return stack;
            return super.insertItem(slot, stack, simulate);
        }
    });

    public EmeraldChestBlockEntity(BlockPos pos, BlockState state) {
        super(ResidenciasDosAldeoes.EMERALD_CHEST_BE.get(), pos, state);
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < getContainerSize()
                && stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get());
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) return;
        super.setItem(slot, stack);
    }

    @Override protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory) {
        return new br.com.residenciasaldeoes.menu.EmeraldChestMenu(id,inventory,this);
    }

    /** Hoppers may deposit fragments and extract contents, but cannot insert other items. */
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemHandler.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    /** Transfers as much of a fragment stack as possible, leaving the exact remainder in {@code stack}. */
    public int insertFragments(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())) return 0;
        int before = stack.getCount();
        for (int slot = 0; slot < getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack present = getItem(slot);
            if (present.isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack movedStack = stack.copy();
                movedStack.setCount(moved);
                setItem(slot, movedStack);
                stack.shrink(moved);
            } else if (canPlaceItem(slot, stack) && ItemStack.isSameItemSameTags(present, stack)) {
                int moved = Math.min(stack.getCount(), present.getMaxStackSize() - present.getCount());
                if (moved > 0) { present.grow(moved); stack.shrink(moved); setChanged(); }
            }
        }
        return before - stack.getCount();
    }
}
