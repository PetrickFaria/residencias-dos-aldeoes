package br.com.residenciasaldeoes.block;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.ResidenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

public class ResidentialAnchorBlockEntity extends BlockEntity {
    private int size = 3;
    public ResidentialAnchorBlockEntity(BlockPos pos, BlockState state) { super(ResidenciasDosAldeoes.ANCHOR_BE.get(), pos, state); }
    public int getSize(){return size;}
    public int getCapacity(){for(int i=0;i<ResidenceData.DIMENSIONS.length;i++)if(ResidenceData.DIMENSIONS[i]==size)return ResidenceData.CAPACITIES[i];return 1;}
    public int getResidentCount(){return level instanceof ServerLevel sl?ResidenceData.get(sl).count(sl.dimension(),worldPosition):getPersistentData().getInt("resident_count");}
    public boolean setSize(int value){if(!(level instanceof ServerLevel sl))return false;ResidenceData data=ResidenceData.get(sl);ResidenceData.Home home=data.home(sl,worldPosition);if(!data.setSize(home,value))return false;size=value;setChanged();sl.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);return true;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("size",size);}
    @Override public void load(CompoundTag tag){super.load(tag);size=tag.contains("size")?tag.getInt("size"):3;}
    @Override public CompoundTag getUpdateTag(){CompoundTag t=super.getUpdateTag();t.putInt("size",size);return t;}
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
