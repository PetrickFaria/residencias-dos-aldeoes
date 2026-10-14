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
    private final java.util.Map<java.util.UUID,Long> courierLeases=new java.util.HashMap<>();
    private int playerOpeners,courierOpeners,lastSentCourierOpeners;
    @Override public void onLoad(){super.onLoad();if(level instanceof net.minecraft.server.level.ServerLevel server)br.com.residenciasaldeoes.data.EmeraldChestIndex.register(server,worldPosition);}
    @Override public void setRemoved(){if(level instanceof net.minecraft.server.level.ServerLevel server)br.com.residenciasaldeoes.data.EmeraldChestIndex.unregister(server,worldPosition);super.setRemoved();}
    public boolean hasFragmentSpace(){
        for(int i=0;i<getContainerSize();i++){ItemStack item=getItem(i);if(item.isEmpty()||(item.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())&&item.getCount()<item.getMaxStackSize()))return true;}
        return false;
    }
    public int getCourierOpeners(){return level!=null&&!level.isClientSide?courierLeases.size():courierOpeners;}
    public int getCombinedOpeners(){return (level!=null&&!level.isClientSide?ChestBlockEntity.getOpenCount(level,worldPosition):playerOpeners)+getCourierOpeners();}
    public boolean isCourierDepositing(java.util.UUID id){return courierLeases.containsKey(id);}
    public void beginCourierDeposit(java.util.UUID id){
        if(level==null||level.isClientSide)return;
        boolean wasClosed=getCombinedOpeners()==0;
        courierLeases.put(id,level.getGameTime()+60L);
        if(wasClosed)playCourierSound(net.minecraft.sounds.SoundEvents.CHEST_OPEN);
        syncCouriers();level.scheduleTick(worldPosition,getBlockState().getBlock(),5);
    }
    public void refreshCourierDeposit(java.util.UUID id){if(level!=null&&!level.isClientSide&&courierLeases.containsKey(id))courierLeases.put(id,level.getGameTime()+60L);}
    public void finishCourierDeposit(java.util.UUID id){if(level!=null&&!level.isClientSide&&courierLeases.containsKey(id)){courierLeases.put(id,level.getGameTime()+20L);level.scheduleTick(worldPosition,getBlockState().getBlock(),5);}}
    private void syncCouriers(){
        if(level==null||level.isClientSide)return;
        int count=courierLeases.size();if(count==lastSentCourierOpeners)return;
        lastSentCourierOpeners=count;level.blockEvent(worldPosition,getBlockState().getBlock(),2,count);
    }
    private void playCourierSound(net.minecraft.sounds.SoundEvent sound){level.playSound(null,worldPosition,sound,net.minecraft.sounds.SoundSource.BLOCKS,.5F,.9F+level.random.nextFloat()*.1F);}
    @Override public void recheckOpen(){
        super.recheckOpen();if(level==null||level.isClientSide)return;
        int before=courierLeases.size();courierLeases.entrySet().removeIf(e->e.getValue()<=level.getGameTime());
        if(before>0&&courierLeases.isEmpty()&&ChestBlockEntity.getOpenCount(level,worldPosition)==0)playCourierSound(net.minecraft.sounds.SoundEvents.CHEST_CLOSE);
        syncCouriers();if(!courierLeases.isEmpty())level.scheduleTick(worldPosition,getBlockState().getBlock(),5);
    }
    @Override public boolean triggerEvent(int id,int count){
        if(id==1){playerOpeners=Math.max(0,count);return super.triggerEvent(1,playerOpeners+courierOpeners);}
        if(id==2){courierOpeners=Math.max(0,count);return super.triggerEvent(1,playerOpeners+courierOpeners);}
        return super.triggerEvent(id,count);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(){var tag=super.getUpdateTag();tag.putInt("courier_openers",getCourierOpeners());tag.putInt("player_openers",level==null?0:ChestBlockEntity.getOpenCount(level,worldPosition));return tag;}
    @Override public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag){load(tag);playerOpeners=tag.getInt("player_openers");courierOpeners=tag.getInt("courier_openers");super.triggerEvent(1,playerOpeners+courierOpeners);}

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
