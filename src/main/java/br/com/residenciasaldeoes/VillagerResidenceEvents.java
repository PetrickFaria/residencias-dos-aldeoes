package br.com.residenciasaldeoes;

import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.data.ResidenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public class VillagerResidenceEvents {
    private static final String UNLINK="residenciasaldeoes_unlink";
    private static final String REG="residenciasaldeoes_register", POS="residenciasaldeoes_anchor", DIM="residenciasaldeoes_dimension";
    @SubscribeEvent public void onVillagerJoin(EntityJoinLevelEvent event){if(event.getEntity() instanceof Villager v&&event.getLevel() instanceof ServerLevel sl){ResidenceData data=ResidenceData.get(sl);data.applyPendingNameRestoration(v);if(data.owner(v.getUUID())!=null)data.applyResidentName(v);}}
    @SubscribeEvent public void onStartTracking(PlayerEvent.StartTracking event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&event.getTarget() instanceof Villager villager&&player.level instanceof ServerLevel level&&level.dimension().equals(villager.level.dimension())){ResidenceData.ResidentSkin skin=ResidenceData.get(level).residentSkin(villager.getUUID());if(skin!=null)br.com.residenciasaldeoes.network.ModNetwork.sendSkin(player,villager.getUUID(),skin);}}
    @SubscribeEvent public void onVillageCatSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.animal.Cat cat) || event.getSpawner()!=null || !(cat.level instanceof ServerLevel level)) return;
        ResidenceData data=ResidenceData.get(level);BlockPos spawn=cat.blockPosition();
        for(ResidenceData.Home h:data.homes()) {
            if(!h.dimension.equals(level.dimension()) || !level.hasChunkAt(h.pos) || h.pos.distSqr(spawn)>48.0D*48.0D) continue;
            int villagers=level.getEntitiesOfClass(Villager.class,new AABB(h.pos).inflate(48.0D),v->true).size();
            if(villagers>=5){event.setCanceled(true);return;}
        }
    }
    @SubscribeEvent public void syncPlayerPreviews(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.player instanceof net.minecraft.server.level.ServerPlayer player) || player.tickCount%20!=0) return;
        syncPreviews(player);
    }
    public static void syncPreviews(net.minecraft.server.level.ServerPlayer player) {
        if(!(player.level instanceof ServerLevel level))return;
        ResidenceData data=ResidenceData.get(level);List<br.com.residenciasaldeoes.network.ModNetwork.PreviewEntry> entries=new ArrayList<>();
        int radius=player.server.getPlayerList().getViewDistance()*16;double radiusSq=(double)radius*radius;
        for(ResidenceData.Home h:data.homes()) {
            if(!h.dimension.equals(level.dimension()) || !level.hasChunkAt(h.pos) || player.distanceToSqr(h.pos.getX()+.5,h.pos.getY()+.5,h.pos.getZ()+.5)>radiusSq) continue;
            boolean permanent=br.com.residenciasaldeoes.network.ModNetwork.previewEnabled(player,h.dimension.location(),h.pos);
            // Send nearby anchor bounds regardless of current location. The client
            // decides automatic in-volume visibility every frame, without a timeout.
            entries.add(new br.com.residenciasaldeoes.network.ModNetwork.PreviewEntry(h.dimension.location(),h.pos,h.size,permanent));
        }
        br.com.residenciasaldeoes.network.ModNetwork.sendPreviews(player,entries);
    }
    @SubscribeEvent public void onVillagerTick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof Villager villager)||!(villager.level instanceof ServerLevel level)||villager.tickCount%10!=0)return;
        ResidenceData data=ResidenceData.get(level);ResidenceData.Home home=data.owner(villager.getUUID());if(home==null)return;
        if(!home.dimension.equals(level.dimension()))return;
        long time=level.getDayTime()%24000L;boolean night=time>=13000L&&time<23000L;boolean weather=level.isRaining();if(!night)data.releaseBed(villager.getUUID());
        if(!weather && time>=12000L && time<13000L){
            if(level.hasChunkAt(home.pos) && !villager.isSleeping() && villager.tickCount%40==0){
                BlockPos approach=findSafeSpot(level,villager,home.pos,home.size);
                if(approach!=null) villager.getNavigation().moveTo(approach.getX()+0.5,approach.getY(),approach.getZ()+0.5,0.85D);
            }
            return;
        }
        if(!night&&!weather)return;
        if(!level.hasChunkAt(home.pos))return;
        if(!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity)){data.unlinkAll(level,home.dimension,home.pos);return;}
        int r=home.size/2;double x=villager.getX(),y=villager.getY(),z=villager.getZ();
        boolean inside=x>=home.pos.getX()-r && x<home.pos.getX()+r+1 && z>=home.pos.getZ()-r && z<home.pos.getZ()+r+1 && y>=home.pos.getY()+1-0.05 && y<home.pos.getY()+home.size+1;
        if(!inside){data.releaseBed(villager.getUUID());BlockPos target=findSafeSpot(level,villager,home.pos,home.size);if(target!=null){villager.stopSleeping();villager.teleportTo(target.getX()+.5,target.getY(),target.getZ()+.5);}}
        else if(!night) { if(villager.isSleeping()) villager.stopSleeping(); }
        else if(villager.isSleeping()){
            BlockPos sleepingAt=villager.getSleepingPos().orElse(null);ResidenceData.BedClaim claim=data.bedClaim(villager.getUUID());
            if(sleepingAt!=null&&level.getBlockState(sleepingAt).getBlock() instanceof BedBlock&&(claim==null||!sleepingAt.equals(claim.bed())))data.reserveBed(villager.getUUID(),level.dimension(),home.pos,sleepingAt);
        }else if(villager.tickCount%40==0){
            ResidenceData.BedClaim claim=data.bedClaim(villager.getUUID());BlockPos bed=claim!=null&&claim.dimension().equals(level.dimension())&&claim.home().equals(home.pos)?claim.bed():null;
            if(bed!=null&&(!level.hasChunkAt(bed)||!(level.getBlockState(bed).getBlock() instanceof BedBlock)||level.getBlockState(bed).getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD||level.getBlockState(bed).getValue(BedBlock.OCCUPIED))){data.releaseBed(villager.getUUID());bed=null;}
            if(bed==null){bed=findAvailableBed(level,data,villager,home.pos,home.size);if(bed!=null&&!data.reserveBed(villager.getUUID(),level.dimension(),home.pos,bed))bed=null;}
            if(bed!=null){if(villager.blockPosition().distSqr(bed)<=4.0D)villager.startSleeping(bed);else villager.getNavigation().moveTo(bed.getX()+.5D,bed.getY(),bed.getZ()+.5D,.85D);}
        }
    }
    private BlockPos findAvailableBed(ServerLevel level,ResidenceData data,Villager villager,BlockPos anchor,int size){
        int r=size/2;
        for(int y=anchor.getY()+1;y<anchor.getY()+size+1;y++)
            for(int x=anchor.getX()-r;x<=anchor.getX()+r;x++)
                for(int z=anchor.getZ()-r;z<=anchor.getZ()+r;z++){
                    BlockPos p=new BlockPos(x,y,z);
                    if(!level.hasChunkAt(p))continue;
                    BlockState state=level.getBlockState(p);
                    if(!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.OCCUPIED) || data.isBedReserved(level.dimension(),p,villager.getUUID()))continue;
                    // Use the head part to avoid selecting the same bed twice.
                    if(state.getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD)continue;
                    BlockPos foot=p.relative(state.getValue(BedBlock.FACING).getOpposite());
                    if(foot.getX()<anchor.getX()-r||foot.getX()>anchor.getX()+r||foot.getZ()<anchor.getZ()-r||foot.getZ()>anchor.getZ()+r||foot.getY()<anchor.getY()+1||foot.getY()>=anchor.getY()+size+1)continue;
                    if(!level.hasChunkAt(foot))continue;
                    return p;
                }
        return null;
    }
    private BlockPos findSafeSpot(ServerLevel level,Villager v,BlockPos anchor,int size){
        int r=size/2;List<BlockPos> bedApproaches=new ArrayList<>(),candidates=new ArrayList<>();
        for(int y=anchor.getY()+1;y<anchor.getY()+size+1;y++)for(int x=anchor.getX()-r;x<=anchor.getX()+r;x++)for(int z=anchor.getZ()-r;z<=anchor.getZ()+r;z++){
            BlockPos floor=new BlockPos(x,y,z);if(!level.hasChunkAt(floor))continue;
            if(level.getBlockState(floor).getBlock() instanceof BedBlock)for(Direction d:Direction.Plane.HORIZONTAL)bedApproaches.add(floor.relative(d));
            candidates.add(floor);
        }
        bedApproaches.sort(Comparator.comparingInt(p->p.distManhattan(v.blockPosition())));
        candidates.sort(Comparator.comparingInt(p->p.distManhattan(v.blockPosition())));
        bedApproaches.addAll(candidates);candidates=bedApproaches;
        Set<BlockPos> seen=new HashSet<>();for(BlockPos feet:candidates){if(!seen.add(feet)||!level.hasChunkAt(feet)||!level.hasChunkAt(feet.above())||!level.hasChunkAt(feet.below()))continue;int fx=feet.getX(),fy=feet.getY(),fz=feet.getZ();
            if(fx<anchor.getX()-r||fx>anchor.getX()+r||fz<anchor.getZ()-r||fz>anchor.getZ()+r||fy<anchor.getY()+1||fy>=anchor.getY()+size+1)continue;
            BlockState floor=level.getBlockState(feet.below());BlockState a=level.getBlockState(feet),b=level.getBlockState(feet.above());
            if(!floor.isFaceSturdy(level,feet.below(),Direction.UP)||!a.getCollisionShape(level,feet).isEmpty()||!b.getCollisionShape(level,feet.above()).isEmpty())continue;
            if(a.getFluidState().isSource()||b.getFluidState().isSource()||a.isBurning(level,feet)||b.isBurning(level,feet.above()))continue;
            if(!level.noCollision(v,new AABB(fx+.15,fy,fz+.15,fx+.85,fy+1.95,fz+.85)))continue;
            return feet;
        }
        return null;
    }
    @SubscribeEvent public void onRegister(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof Villager villager)||event.getLevel().isClientSide)return;
        var tag=event.getEntity().getPersistentData();if(!tag.getBoolean(REG) && !tag.getBoolean(UNLINK))return;
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(!tag.getString(DIM).equals(level.dimension().location().toString())){event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.residenciasaldeoes.wrong_dimension"),true);return;}
        BlockPos pos=BlockPos.of(tag.getLong(POS));if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof ResidentialAnchorBlockEntity)){tag.putBoolean(REG,false);return;}
        if(tag.getBoolean(UNLINK)){
            ResidenceData data=ResidenceData.get(level);ResidenceData.Home home=data.owner(villager.getUUID());
            if(home!=null && home.dimension.equals(level.dimension()) && home.pos.equals(pos)){
                data.unlink(level,villager.getUUID());tag.putBoolean(UNLINK,false);
                event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.residenciasaldeoes.unlinked_one"),true);
                event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }else event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.residenciasaldeoes.not_resident"),true);
            return;
        }
        String result=ResidenceData.get(level).link(level,pos,villager);
        String key=switch(result){case "ok"->"message.residenciasaldeoes.linked";case "full"->"message.residenciasaldeoes.full";default->"message.residenciasaldeoes.already";};
        event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable(key),true);
        if(result.equals("ok")){tag.putBoolean(REG,false);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}
    }
    @SubscribeEvent public void onDeath(LivingDeathEvent event){if(event.getEntity() instanceof Villager v&&v.level instanceof ServerLevel sl)ResidenceData.get(sl).unlink(sl,v.getUUID());}
}
