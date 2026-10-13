package br.com.residenciasaldeoes.block;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.VillagerResidenceEvents;
import br.com.residenciasaldeoes.data.ResidenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

public class ResidentialAnchorBlockEntity extends BlockEntity implements Container {
    public static final int OUTPUT_SLOTS=10;
    private int size = 3;
    private final NonNullList<ItemStack> outputItems=NonNullList.withSize(OUTPUT_SLOTS,ItemStack.EMPTY);
    private long pendingFragments;
    private long lastProductionGameTime=Long.MIN_VALUE;
    private long lastProductionEventId=Long.MIN_VALUE;
    public ResidentialAnchorBlockEntity(BlockPos pos, BlockState state) { super(ResidenciasDosAldeoes.ANCHOR_BE.get(), pos, state); }
    public int getSize(){return size;}
    public int getCapacity(){for(int i=0;i<ResidenceData.DIMENSIONS.length;i++)if(ResidenceData.DIMENSIONS[i]==size)return ResidenceData.CAPACITIES[i];return 1;}
    public int getResidentCount(){return level instanceof ServerLevel sl?ResidenceData.get(sl).count(sl.dimension(),worldPosition):getPersistentData().getInt("resident_count");}
    public boolean setSize(int value){if(!(level instanceof ServerLevel sl))return false;ResidenceData data=ResidenceData.get(sl);ResidenceData.Home home=data.home(sl,worldPosition);int oldSize=home.size;if(!data.setSize(home,value))return false;size=value;setChanged();sl.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);if(oldSize!=value)VillagerResidenceEvents.onResidenceSizeChanged(sl,home,data);return true;}
    /** Server-only insertion point for future output systems; menu interactions cannot place items. */
    public boolean setOutputItem(int slot,ItemStack stack){
        if(slot<0||slot>=OUTPUT_SLOTS||stack==null||(!stack.isEmpty()&&!stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()))||level==null||level.isClientSide)return false;
        ItemStack copy=stack.copy();if(!copy.isEmpty()&&copy.getCount()>copy.getMaxStackSize())copy.setCount(copy.getMaxStackSize());
        outputItems.set(slot,copy);flushPendingFragments();setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);return true;
    }
    /** Records one production period exactly once and retains overflow across saves. */
    public boolean recordDailyProduction(long eventId,int amount){
        if(eventId<=lastProductionEventId)return false;
        lastProductionEventId=eventId;
        lastProductionGameTime=level instanceof ServerLevel sl?sl.getGameTime():lastProductionGameTime;
        if(amount>0)pendingFragments=Math.min(Long.MAX_VALUE-amount,pendingFragments)+amount;
        flushPendingFragments();setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        return true;
    }
    /** Moves pending output into the first available slots, preserving the existing output-only rule. */
    public void flushPendingFragments(){
        if(pendingFragments<=0||level==null||level.isClientSide)return;
        boolean changed=false;
        // Complete existing piles before allocating empty slots, even if an earlier slot is empty.
        for(int slot=0;slot<OUTPUT_SLOTS&&pendingFragments>0;slot++){
            ItemStack current=outputItems.get(slot);
            if(!current.isEmpty()&&current.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())){
                int moved=(int)Math.min((long)Math.max(0,current.getMaxStackSize()-current.getCount()),pendingFragments);
                if(moved>0){current.grow(moved);pendingFragments-=moved;changed=true;}
            }
        }
        for(int slot=0;slot<OUTPUT_SLOTS&&pendingFragments>0;slot++){
            if(outputItems.get(slot).isEmpty()){
                ItemStack produced=new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get());
                int moved=(int)Math.min((long)produced.getMaxStackSize(),pendingFragments);
                produced.setCount(moved);outputItems.set(slot,produced);
                pendingFragments-=moved;changed=true;
            }
        }
        if(changed){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    }
    public java.util.List<ItemStack> takeAvailableFragments(){
        java.util.List<ItemStack> cargo=new java.util.ArrayList<>();
        if(level==null||level.isClientSide)return cargo;
        for(int slot=0;slot<OUTPUT_SLOTS;slot++){
            ItemStack current=outputItems.get(slot);
            if(!current.isEmpty()&&current.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())){
                int moved=Math.min(64,current.getCount());
                ItemStack carried=current.copy();carried.setCount(moved);cargo.add(carried);current.shrink(moved);
                if(current.isEmpty())outputItems.set(slot,ItemStack.EMPTY);
                break;
            }
        }
        if(!cargo.isEmpty()){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
        return cargo;
    }
    public void restoreFragments(java.util.List<ItemStack> cargo){
        if(level==null||level.isClientSide)return;
        for(ItemStack stack:cargo)if(!stack.isEmpty()&&stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()))pendingFragments=Math.min(Long.MAX_VALUE-stack.getCount(),pendingFragments)+stack.getCount();
        flushPendingFragments();setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public long getPendingFragments(){return pendingFragments;}
    @Override public int getContainerSize(){return OUTPUT_SLOTS;}
    @Override public boolean isEmpty(){return outputItems.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<OUTPUT_SLOTS?outputItems.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){if(slot<0||slot>=OUTPUT_SLOTS)return ItemStack.EMPTY;ItemStack result=ContainerHelper.removeItem(outputItems,slot,amount);if(!result.isEmpty()){setChanged();flushPendingFragments();}return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){if(slot<0||slot>=OUTPUT_SLOTS)return ItemStack.EMPTY;ItemStack result=outputItems.get(slot);outputItems.set(slot,ItemStack.EMPTY);if(!result.isEmpty())flushPendingFragments();return result;}
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=OUTPUT_SLOTS)return;
        if(stack==null||stack.isEmpty()){outputItems.set(slot,ItemStack.EMPTY);setChanged();flushPendingFragments();}
        else if(level==null||level.isClientSide)outputItems.set(slot,stack.copy());
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return false;}
    @Override public void clearContent(){for(int slot=0;slot<OUTPUT_SLOTS;slot++)outputItems.set(slot,ItemStack.EMPTY);setChanged();flushPendingFragments();}
    @Override public boolean stillValid(Player player){return !isRemoved()&&player.distanceToSqr(worldPosition.getX()+.5D,worldPosition.getY()+.5D,worldPosition.getZ()+.5D)<64.0D;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("size",size);tag.putLong("pending_fragments",pendingFragments);tag.putLong("last_production_game_time",lastProductionGameTime);tag.putLong("last_production_event_id",lastProductionEventId);ContainerHelper.saveAllItems(tag,outputItems);}
    @Override public void load(CompoundTag tag){super.load(tag);size=tag.contains("size")?tag.getInt("size"):3;pendingFragments=tag.getLong("pending_fragments");lastProductionGameTime=tag.contains("last_production_game_time")?tag.getLong("last_production_game_time"):Long.MIN_VALUE;lastProductionEventId=tag.contains("last_production_event_id")?tag.getLong("last_production_event_id"):Long.MIN_VALUE;outputItems.clear();ContainerHelper.loadAllItems(tag,outputItems);}
    @Override public CompoundTag getUpdateTag(){CompoundTag t=super.getUpdateTag();t.putInt("size",size);return t;}
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
