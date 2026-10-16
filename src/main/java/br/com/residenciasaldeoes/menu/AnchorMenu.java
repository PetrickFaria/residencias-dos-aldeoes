package br.com.residenciasaldeoes.menu;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.List;

public class AnchorMenu extends AbstractContainerMenu {
 private final BlockPos pos;private final Player ownerPlayer;private int syncedResidents,syncedSize=3;private List<ModNetwork.ResidentEntry> lastResidents=List.of();private long lastResidentSync=Long.MIN_VALUE;
 public AnchorMenu(int id,Inventory i,FriendlyByteBuf b){this(id,i,b.readBlockPos());}
 public AnchorMenu(int id,Inventory i,BlockPos p){super(ResidenciasDosAldeoes.ANCHOR_MENU.get(),id);pos=p;ownerPlayer=i.player;
  BlockEntity anchor=i.player.level.getBlockEntity(p);
  Container output=anchor instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity be?be:new SimpleContainer(ResidentialAnchorBlockEntity.OUTPUT_SLOTS);
  int outputRowLeft=(272-(ResidentialAnchorBlockEntity.OUTPUT_SLOTS*18+(ResidentialAnchorBlockEntity.OUTPUT_SLOTS-1)*3))/2;
  for(int slot=0;slot<ResidentialAnchorBlockEntity.OUTPUT_SLOTS;slot++){final int index=slot;addSlot(new Slot(output,index,outputRowLeft+slot*21,210){@Override public boolean mayPlace(ItemStack stack){return false;}});}
 
  addDataSlot(new DataSlot(){public int get(){BlockEntity b=i.player.level.getBlockEntity(p);return b instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity a?a.getResidentCount():0;}public void set(int v){syncedResidents=v;}});
  addDataSlot(new DataSlot(){public int get(){BlockEntity b=i.player.level.getBlockEntity(p);return b instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity a?a.getSize():3;}public void set(int v){syncedSize=v;}});
 }
 @Override public ItemStack quickMoveStack(Player player,int index){
  if(index<0||index>=ResidentialAnchorBlockEntity.OUTPUT_SLOTS)return ItemStack.EMPTY;
  Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
  ItemStack before=slot.getItem().copy();ItemStack offered=before.copy();int count=offered.getCount();
  player.getInventory().add(offered);int moved=count-offered.getCount();if(moved<=0)return ItemStack.EMPTY;
  ItemStack taken=slot.remove(moved);slot.onTake(player,taken);return taken;
 }
 public BlockPos getPos(){return pos;}public int getSize(Player p){return syncedSize;}public int getCount(Player p){return syncedResidents;}
 @Override public void broadcastChanges(){super.broadcastChanges();if(!(ownerPlayer instanceof ServerPlayer p)||!(p.level instanceof ServerLevel l))return;long now=l.getGameTime();boolean first=lastResidentSync==Long.MIN_VALUE;if(!first&&now-lastResidentSync<10)return;lastResidentSync=now;sendResidentSnapshot(p,l,now,first);}
 /** Sends the persistent resident records and their current resource indicators to this menu's player. */
 public void sendResidentSnapshot(ServerPlayer p){if(!(p.level instanceof ServerLevel l)||p.containerMenu!=this)return;lastResidentSync=l.getGameTime();sendResidentSnapshot(p,l,lastResidentSync,true);}
 private void sendResidentSnapshot(ServerPlayer p,ServerLevel l,long now,boolean force){ResidenceData d=ResidenceData.get(l);ResidenceData.Home h=d.home(l,pos);int configured=l.getBlockEntity(pos) instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity be?be.getSize():h.size;int[] resources=countResources(l,pos,configured);List<java.util.UUID> ids=new java.util.ArrayList<>(d.residents(l.dimension(),pos));java.util.ArrayList<ModNetwork.ResidentEntry> entries=new java.util.ArrayList<>(ids.size());for(int idx=0;idx<ids.size();idx++){var id=ids.get(idx);ResidenceData.ResidentSkin skin=d.residentSkin(id);entries.add(new ModNetwork.ResidentEntry(id,d.residentName(id),idx<resources[0],idx<resources[1],idx<resources[2],idx<resources[3]*2,skin==null?"":skin.key(),skin==null?br.com.residenciasaldeoes.data.SkinApplicationMode.COMPLETE:skin.mode(),d.residentOpacity(id),d.residentModel(id)));}List<ModNetwork.ResidentEntry> es=List.copyOf(entries);if(force||!es.equals(lastResidents)||now%100==0){lastResidents=es;ModNetwork.sendResidents(p,pos,lastResidents);}}
 public static int[] countResources(ServerLevel l,BlockPos a,int size){int r=size/2,beds=0,work=0,chests=0,cakes=0;for(int y=a.getY()+1;y<a.getY()+size+1;y++)for(int x=a.getX()-r;x<=a.getX()+r;x++)for(int z=a.getZ()-r;z<=a.getZ()+r;z++){BlockPos p=new BlockPos(x,y,z);if(!l.hasChunkAt(p))continue;var s=l.getBlockState(p);var b=s.getBlock();if(b==Blocks.CAKE)cakes++;if(b instanceof BedBlock&&s.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD){BlockPos foot=p.relative(s.getValue(BedBlock.FACING).getOpposite());if(inside(a,size,foot)&&l.hasChunkAt(foot)&&l.getBlockState(foot).getBlock() instanceof BedBlock)beds++;}if(b instanceof ChestBlock&&s.getValue(ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.RIGHT)chests++;if(b==Blocks.COMPOSTER||b==Blocks.BARREL||b==Blocks.SMOKER||b==Blocks.BLAST_FURNACE||b==Blocks.FLETCHING_TABLE||b==Blocks.CARTOGRAPHY_TABLE||b==Blocks.BREWING_STAND||b==Blocks.CAULDRON||b==Blocks.LECTERN||b==Blocks.STONECUTTER||b==Blocks.LOOM||b==Blocks.GRINDSTONE||b==Blocks.SMITHING_TABLE||b==Blocks.CRAFTING_TABLE)work++;}return new int[]{beds,work,chests,cakes};}
 private static boolean inside(BlockPos a,int size,BlockPos p){int r=size/2;return p.getX()>=a.getX()-r&&p.getX()<=a.getX()+r&&p.getZ()>=a.getZ()-r&&p.getZ()<=a.getZ()+r&&p.getY()>=a.getY()+1&&p.getY()<a.getY()+size+1;}
 @Override public boolean stillValid(Player p){return p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<64&&p.level.getBlockEntity(pos) instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;}
}
