package br.com.residenciasaldeoes;

import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public class VillagerResidenceEvents {
    private record DawnSample(long gameTime,long dayTime){}
    private static final Map<ServerLevel,DawnSample> PRODUCTION_CLOCKS=new WeakHashMap<>();
    private static final long DAY_LENGTH=24_000L;
    private static final long DAWN_COMMAND_WINDOW_TICKS=1_200L;
    private static final String UNLINK="residenciasaldeoes_unlink";
    private static final String REG="residenciasaldeoes_register", POS="residenciasaldeoes_anchor", DIM="residenciasaldeoes_dimension";
    private static final String RETURN_ACTIVE="residenciasaldeoes_home_return_active";
    private static final String RETURN_FORCED="residenciasaldeoes_home_return_forced";
    private static final String RETURN_STARTED="residenciasaldeoes_home_return_started";
    private static final String RETURN_LAST_ATTEMPT="residenciasaldeoes_home_return_last_attempt";
    private static final String RETURN_LAST_SEARCH="residenciasaldeoes_home_return_last_search";
    private static final String RETURN_HOME="residenciasaldeoes_home_return_home";
    private static final String RETURN_TARGET="residenciasaldeoes_home_return_target";
    private static final String RETURN_TARGET_X="residenciasaldeoes_home_return_target_x";
    private static final String RETURN_TARGET_Y="residenciasaldeoes_home_return_target_y";
    private static final String RETURN_TARGET_Z="residenciasaldeoes_home_return_target_z";
    private static final long RETURN_TIMEOUT_TICKS=300L;
    private static final long THREAT_RETURN_TIMEOUT_TICKS=150L;
    private static final long RETURN_RETRY_TICKS=20L;
    private static final long TICKS_AFTER_DAWN_FOR_PAYMENT=200L;
    private static final long TICKS_AFTER_PAYMENT_FOR_COLLECTION=1_200L;
    private static final String EMERALD_TRANSFER="residenciasaldeoes_emerald_transfer";
    /**
     * Captures /time changes before a same-tick set/add can be overwritten by the level sampler.
     * The predicted clock sample also prevents the corresponding END tick from counting twice.
     */
    @SubscribeEvent public void onTimeCommand(CommandEvent event){
        var context=event.getParseResults().getContext();
        ServerLevel level=context.getSource().getLevel();
        String input=event.getParseResults().getReader().getString().trim();
        if(input.startsWith("/"))input=input.substring(1);
        String[] parts=input.split("\\s+");
        if(parts.length<3||!parts[0].equals("time"))return;
        String action=parts[1];
        if(!action.equals("set")&&!action.equals("add"))return;
        long before=level.getDayTime(),target;
        try{
            if(action.equals("set")){
                target=switch(parts[2]){case "day"->1_000L;case "noon"->6_000L;case "night"->13_000L;case "midnight"->18_000L;default->Long.parseLong(parts[2]);};
            }else target=before+Long.parseLong(parts[2]);
        }catch(NumberFormatException ignored){return;}
        long oldTime=Math.floorMod(before,DAY_LENGTH),newTime=Math.floorMod(target,DAY_LENGTH);
        boolean setIntoDawn=action.equals("set")&&newTime<=DAWN_COMMAND_WINDOW_TICKS;
        boolean addIntoDawn=action.equals("add")&&oldTime>DAWN_COMMAND_WINDOW_TICKS&&newTime<=DAWN_COMMAND_WINDOW_TICKS;
        PRODUCTION_CLOCKS.put(level,new DawnSample(level.getGameTime(),target));
        if(setIntoDawn||addIntoDawn)
            processProductionDawn(level,level.getServer().overworld().getGameTime());
    }
    @SubscribeEvent public void onProductionLevelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;
        long productionGameTime=level.getServer().overworld().getGameTime();
        long levelGameTime=level.getGameTime(),dayTime=level.getDayTime();
        DawnSample current=new DawnSample(levelGameTime,dayTime);
        DawnSample previous=PRODUCTION_CLOCKS.put(level,current);
        boolean firstLoadedDawn=previous==null&&Math.floorMod(dayTime,DAY_LENGTH)<=DAWN_COMMAND_WINDOW_TICKS;
        if(firstLoadedDawn||(previous!=null&&crossedNaturalDawn(previous,current)))
            processProductionDawn(level,productionGameTime);
        processLoadedProduction(level,productionGameTime);
        beginScheduledCollections(level,productionGameTime);
    }
    private static boolean crossedNaturalDawn(DawnSample before,DawnSample after){
        long oldTime=Math.floorMod(before.dayTime(),DAY_LENGTH);
        long newTime=Math.floorMod(after.dayTime(),DAY_LENGTH);
        boolean naturalTick=after.gameTime()-before.gameTime()==1L&&after.dayTime()-before.dayTime()==1L
                &&oldTime==DAY_LENGTH-1L&&newTime==0L;
        if(naturalTick)return true;
        // Treat direct command and other valid clock adjustments into dawn as transitions.
        boolean adjustedClock=after.gameTime()>=before.gameTime()
                &&after.gameTime()-before.gameTime()<=1L
                &&after.dayTime()-before.dayTime()!=1L;
        return adjustedClock&&oldTime!=newTime&&newTime<=DAWN_COMMAND_WINDOW_TICKS;
    }
    @SubscribeEvent public void onProductionSleepFinished(SleepFinishedTimeEvent event){
        if(event.getLevel() instanceof ServerLevel level){
            long levelTime=event.getNewTime();
            PRODUCTION_CLOCKS.put(level,new DawnSample(level.getGameTime(),levelTime));
            processProductionDawn(level,level.getServer().overworld().getGameTime());
        }
    }
    private void processProductionDawn(ServerLevel level,long gameTime){
        ResidenceData data=ResidenceData.get(level);
        long eventId=data.nextProductionDawnEventId();
        for(ResidenceData.Home home:new ArrayList<>(data.homes()))
            if(home.dimension.equals(level.dimension())&&!home.residents.isEmpty())
                data.queueProductionDawn(home,gameTime+TICKS_AFTER_DAWN_FOR_PAYMENT,eventId);
    }
    private void processLoadedProduction(ServerLevel level,long currentGameTime){
        ResidenceData data=ResidenceData.get(level);
        for(ResidenceData.Home home:new ArrayList<>(data.homes())){
            if(!home.dimension.equals(level.dimension())||!level.hasChunkAt(home.pos))continue;
            if(!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity anchor))continue;
            anchor.flushPendingFragments();
            for(ResidenceData.PendingProduction pendingTask:new ArrayList<>(home.pendingProduction)){
                if(currentGameTime<pendingTask.dueGameTime())continue;
                ResidenceData.PendingProduction task=pendingTask;
                long eventId=task.period();
                if(eventId==Long.MIN_VALUE){
                    eventId=data.nextProductionDawnEventId();
                    data.adoptLegacyProductionDawn(home,task,eventId);
                    task=new ResidenceData.PendingProduction(task.dueGameTime(),eventId);
                }
                int[] resources=AnchorMenu.countResources(level,home.pos,anchor.getSize());
                List<UUID> residents=new ArrayList<>(home.residents);
                int fragments=calculateDailyFragments(residents,resources);
                boolean recorded=anchor.recordDailyProduction(eventId,fragments);
                if(recorded&&fragments>0){
                    playProductionBell(level,residents);
                    data.queueCollection(home,eventId,currentGameTime+TICKS_AFTER_PAYMENT_FOR_COLLECTION);
                }
                data.completeProductionDawn(home,task);
            }
        }
    }
    private void beginScheduledCollections(ServerLevel level,long currentGameTime){
        ResidenceData data=ResidenceData.get(level);
        for(ResidenceData.Home home:new ArrayList<>(data.homes())){
            if(!home.dimension.equals(level.dimension())||home.residents.isEmpty())continue;
            ResidenceData.PendingCollection pending=data.dueCollection(home,currentGameTime);
            if(pending==null)continue;
            UUID first=home.residents.iterator().next();
            if(!(level.getEntity(first) instanceof Villager villager)||villager.getPersistentData().contains(EMERALD_TRANSFER))continue;
            if(!level.hasChunkAt(home.pos)||!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity anchor)||anchor.isEmpty())continue;
            BlockPos chest=findEmeraldChest(level,home,villager);
            if(chest==null)continue;
            BlockPos homeApproach=findApproach(level,home.pos,villager);
            BlockPos chestApproach=findApproach(level,chest,villager);
            if(homeApproach==null||chestApproach==null)continue;
            CompoundTag job=new CompoundTag();job.putLong("anchor",home.pos.asLong());job.putLong("chest",chest.asLong());job.putLong("period",pending.period());job.putLong("started",currentGameTime);job.put("previous_hand",villager.getItemBySlot(EquipmentSlot.MAINHAND).save(new CompoundTag()));job.putLong("home_approach",homeApproach.asLong());job.putLong("chest_approach",chestApproach.asLong());job.putInt("stage",0);
            villager.getPersistentData().put(EMERALD_TRANSFER,job);
            data.completeCollection(home,pending);
        }
    }
    private static BlockPos findEmeraldChest(ServerLevel level,ResidenceData.Home home,Villager villager){
        int radius=home.size/2;List<BlockPos> candidates=new ArrayList<>();
        for(int y=home.pos.getY()+1;y<=home.pos.getY()+home.size;y++)
            for(int x=home.pos.getX()-radius;x<=home.pos.getX()+radius;x++)
                for(int z=home.pos.getZ()-radius;z<=home.pos.getZ()+radius;z++){
                    BlockPos pos=new BlockPos(x,y,z);
                    if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof br.com.residenciasaldeoes.block.EmeraldChestBlockEntity)candidates.add(pos);
                }
        candidates.sort(Comparator.<BlockPos>comparingDouble(p->p.distSqr(home.pos)).thenComparing(Comparator.naturalOrder()));
        for(BlockPos candidate:candidates)if(findApproach(level,candidate,villager)!=null)return candidate;
        return null;
    }
    private static BlockPos findApproach(ServerLevel level,BlockPos target,Villager villager){
        List<BlockPos> approaches=new ArrayList<>();
        for(Direction direction:Direction.Plane.HORIZONTAL){BlockPos candidate=target.relative(direction);
            if(level.hasChunkAt(candidate)&&level.isEmptyBlock(candidate)&&!level.isEmptyBlock(candidate.below()))approaches.add(candidate);
        }
        approaches.sort(Comparator.<BlockPos>comparingDouble(p->villager.blockPosition().distSqr(p)).thenComparing(Comparator.naturalOrder()));
        for(BlockPos candidate:approaches){net.minecraft.world.level.pathfinder.Path path=villager.getNavigation().createPath(candidate,0);if(path!=null&&path.canReach())return candidate;}
        return null;
    }
    private boolean tickEmeraldTransfer(ServerLevel level,Villager villager){
        if(villager.tickCount%5!=0)return true;
        if(!villager.getPersistentData().contains(EMERALD_TRANSFER))return false;
        CompoundTag job=villager.getPersistentData().getCompound(EMERALD_TRANSFER);
        if(level.getGameTime()-job.getLong("started")>12_000L){finishEmeraldTransfer(level,villager,job,true);return true;}
        ResidenceData.Home owner=ResidenceData.get(level).owner(villager.getUUID());
        if(owner==null||!owner.dimension.equals(level.dimension())||job.getLong("anchor")!=owner.pos.asLong()){
            finishEmeraldTransfer(level,villager,job,true);return true;
        }
        BlockPos anchorPos=BlockPos.of(job.getLong("anchor")),chestPos=BlockPos.of(job.getLong("chest"));
        int stage=job.getInt("stage");
        BlockPos target=BlockPos.of(stage==1?job.getLong("chest_approach"):job.getLong("home_approach"));
        if(!level.hasChunkAt(target)){finishEmeraldTransfer(level,villager,job,true);return true;}
        if(villager.blockPosition().distSqr(target)<2.25D){
            if(job.getInt("stage")==0){
                if(!(level.getBlockEntity(anchorPos) instanceof ResidentialAnchorBlockEntity anchor)){finishEmeraldTransfer(level,villager,job,true);return true;}
                List<ItemStack> cargo=anchor.takeAvailableFragments();
                if(cargo.isEmpty()){finishEmeraldTransfer(level,villager,job,false);return true;}
                ListTag items=new ListTag();for(ItemStack stack:cargo)items.add(stack.save(new CompoundTag()));job.put("cargo",items);job.putInt("stage",1);villager.getPersistentData().put(EMERALD_TRANSFER,job);
                villager.setItemSlot(EquipmentSlot.MAINHAND,cargo.get(0).copy());
            }else if(stage==1){
                if(!(level.getBlockEntity(chestPos) instanceof br.com.residenciasaldeoes.block.EmeraldChestBlockEntity chest)){finishEmeraldTransfer(level,villager,job,true);return true;}
                List<ItemStack> cargo=readTransferCargo(job);List<ItemStack> remainder=new ArrayList<>();
                for(ItemStack stack:cargo){chest.insertFragments(stack);if(!stack.isEmpty())remainder.add(stack);}
                if(remainder.isEmpty())finishEmeraldTransfer(level,villager,job,false);
                else{ListTag items=new ListTag();for(ItemStack stack:remainder)items.add(stack.save(new CompoundTag()));job.put("cargo",items);job.putInt("stage",2);villager.getPersistentData().put(EMERALD_TRANSFER,job);}
            }else{
                List<ItemStack> remainder=readTransferCargo(job);
                if(level.getBlockEntity(anchorPos) instanceof ResidentialAnchorBlockEntity anchor)anchor.restoreFragments(remainder);
                else for(ItemStack stack:remainder)if(!stack.isEmpty())villager.spawnAtLocation(stack);
                finishEmeraldTransfer(level,villager,job,false);
            }
        }else if(villager.getNavigation().isDone()||villager.tickCount%40==0){
            villager.getNavigation().moveTo(target.getX()+.5D,target.getY(),target.getZ()+.5D,.8D);
        }
        return true;
    }
    private static List<ItemStack> readTransferCargo(CompoundTag job){
        List<ItemStack> items=new ArrayList<>();ListTag list=job.getList("cargo",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){ItemStack stack=ItemStack.of(list.getCompound(i));if(!stack.isEmpty())items.add(stack);}
        return items;
    }
    private static void finishEmeraldTransfer(ServerLevel level,Villager villager,CompoundTag job,boolean returnCargo){
        if(returnCargo){List<ItemStack> cargo=readTransferCargo(job);BlockPos anchorPos=BlockPos.of(job.getLong("anchor"));
            if(level.hasChunkAt(anchorPos)&&level.getBlockEntity(anchorPos) instanceof ResidentialAnchorBlockEntity anchor)anchor.restoreFragments(cargo);
            else for(ItemStack stack:cargo)if(!stack.isEmpty())villager.spawnAtLocation(stack);
        }
        if(job.contains("previous_hand",Tag.TAG_COMPOUND))villager.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.of(job.getCompound("previous_hand")));
        villager.getNavigation().stop();villager.getPersistentData().remove(EMERALD_TRANSFER);
    }
    private int calculateDailyFragments(List<UUID> residents,int[] resources){
        int total=0;
        for(int index=0;index<residents.size();index++){
            total+=3;
            if(index<resources[0])total++;
            if(index<resources[1])total++;
            if(index<resources[2])total++;
            if(index<resources[3]*2)total++;
        }
        return total;
    }
    private void playProductionBell(ServerLevel level,List<UUID> residents){
        for(UUID resident:residents)if(level.getEntity(resident) instanceof Villager villager)
            level.playSound(null,villager.blockPosition(),SoundEvents.VILLAGER_NO,SoundSource.NEUTRAL,0.75F,1.0F);
    }
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
        if(!(event.getEntity() instanceof Villager villager)||!(villager.level instanceof ServerLevel level))return;
        if(villager.getPersistentData().contains(EMERALD_TRANSFER)){tickEmeraldTransfer(level,villager);return;}
        boolean weather=level.isRaining();var entityData=villager.getPersistentData();
        boolean forced=entityData.getBoolean(RETURN_FORCED);
        if(villager.tickCount%10!=0&&!weather&&!forced)return;
        ResidenceData data=ResidenceData.get(level);ResidenceData.Home home=data.owner(villager.getUUID());if(home==null)return;
        if(!home.dimension.equals(level.dimension())||!level.hasChunkAt(home.pos))return;
        if(!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity)){data.unlinkAll(level,home.dimension,home.pos);return;}

        long time=level.getDayTime()%24000L;boolean night=time>=13000L&&time<23000L;
        boolean inside=insideResidence(villager.getX(),villager.getY(),villager.getZ(),home);
        BlockPos sleepingAt=villager.getSleepingPos().orElse(null);
        if(villager.isSleeping()&&sleepingAt!=null&&!bedInsideHome(level,sleepingAt,home)){
            // The home bounds may have changed while this villager or its saved bed claim was unloaded.
            villager.stopSleeping();villager.getNavigation().stop();
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);data.releaseBed(villager.getUUID());
            beginHomeReturn(villager,level,home,true);forced=true;inside=insideResidence(villager.getX(),villager.getY(),villager.getZ(),home);
        }

        if(!night&&!weather&&!forced){
            clearHomeReturn(entityData);
            if(villager.tickCount%10==0)data.releaseBed(villager.getUUID());
            return;
        }
        if(!forced&&!weather&&time>=12000L&&time<13000L){
            if(!villager.isSleeping()&&villager.tickCount%40==0){
                BlockPos approach=findSafeSpot(level,villager,home.pos,home.size);
                if(approach!=null)villager.getNavigation().moveTo(approach.getX()+0.5,approach.getY(),approach.getZ()+0.5,0.85D);
            }
            return;
        }

        if(inside){
            clearHomeReturn(entityData);
            if(weather)keepResidentInsideDuringRain(level,data,villager,home,true,!night);
            if(!night){if(villager.isSleeping())villager.stopSleeping();return;}
            if(villager.isSleeping()){
                ResidenceData.BedClaim claim=data.bedClaim(villager.getUUID());
                if(sleepingAt!=null&&level.getBlockState(sleepingAt).getBlock() instanceof BedBlock&&(claim==null||!sleepingAt.equals(claim.bed())))data.reserveBed(villager.getUUID(),level.dimension(),home.pos,sleepingAt);
            }else if(villager.tickCount%40==0){
                ResidenceData.BedClaim claim=data.bedClaim(villager.getUUID());BlockPos bed=claim!=null&&claim.dimension().equals(level.dimension())&&claim.home().equals(home.pos)?claim.bed():null;
                if(bed!=null&&(!level.hasChunkAt(bed)||!(level.getBlockState(bed).getBlock() instanceof BedBlock)||level.getBlockState(bed).getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD||level.getBlockState(bed).getValue(BedBlock.OCCUPIED))){data.releaseBed(villager.getUUID());bed=null;}
                if(bed==null){bed=findAvailableBed(level,data,villager,home.pos,home.size);if(bed!=null&&!data.reserveBed(villager.getUUID(),level.dimension(),home.pos,bed))bed=null;}
                if(bed!=null){if(villager.blockPosition().distSqr(bed)<=4.0D)villager.startSleeping(bed);else villager.getNavigation().moveTo(bed.getX()+.5D,bed.getY(),bed.getZ()+.5D,.85D);}
            }
            return;
        }

        if(night||forced||weather){
            returnResidentToHome(level,data,villager,home);
            return;
        }
    }

    /** Called immediately after the anchor's configured size and persisted bounds change. */
    public static void onResidenceSizeChanged(ServerLevel level,ResidenceData.Home home,ResidenceData data){
        for(UUID id:data.residents(home.dimension,home.pos)){
            if(!(level.getEntity(id) instanceof Villager villager)||!villager.isSleeping())continue;
            BlockPos bed=villager.getSleepingPos().orElse(null);
            if(bed==null||bedInsideHome(level,bed,home))continue;
            villager.stopSleeping();villager.getNavigation().stop();villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            data.releaseBed(id);beginHomeReturn(villager,level,home,true);
        }
    }

    private static void beginHomeReturn(Villager villager,ServerLevel level,ResidenceData.Home home,boolean forced){
        var tag=villager.getPersistentData();String key=home.dimension.location()+"|"+home.pos.asLong();
        if(!tag.getBoolean(RETURN_ACTIVE)||!key.equals(tag.getString(RETURN_HOME))){
            tag.putBoolean(RETURN_ACTIVE,true);tag.putLong(RETURN_STARTED,level.getGameTime());
            tag.putLong(RETURN_LAST_ATTEMPT,level.getGameTime()-RETURN_RETRY_TICKS);
            tag.putLong(RETURN_LAST_SEARCH,level.getGameTime()-RETURN_RETRY_TICKS);tag.putString(RETURN_HOME,key);
            tag.remove(RETURN_TARGET);tag.remove(RETURN_TARGET_X);tag.remove(RETURN_TARGET_Y);tag.remove(RETURN_TARGET_Z);
            tag.remove(RETURN_FORCED);
        }
        if(forced)tag.putBoolean(RETURN_FORCED,true);
    }

    private void returnResidentToHome(ServerLevel level,ResidenceData data,Villager villager,ResidenceData.Home home){
        beginHomeReturn(villager,level,home,false);var tag=villager.getPersistentData();
        if(villager.isSleeping())villager.stopSleeping();data.releaseBed(villager.getUUID());
        long now=level.getGameTime(),elapsed=Math.max(0L,now-tag.getLong(RETURN_STARTED));
        BlockPos target=readReturnTarget(tag);
        if((target==null||!isSafeDestination(level,villager,target,home))&&now-tag.getLong(RETURN_LAST_SEARCH)>=RETURN_RETRY_TICKS){
            tag.putLong(RETURN_LAST_SEARCH,now);
            target=findSafeSpot(level,villager,home.pos,home.size);
            if(target!=null)writeReturnTarget(tag,target);
        }

        long returnTimeout=hasNearbyPerceivedThreat(villager)?THREAT_RETURN_TIMEOUT_TICKS:RETURN_TIMEOUT_TICKS;
        if(elapsed>=returnTimeout&&target!=null&&isSafeDestination(level,villager,target,home)){
            villager.getNavigation().stop();villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.teleportTo(target.getX()+0.5D,target.getY(),target.getZ()+0.5D);
            clearHomeReturn(tag);return;
        }

        long last=tag.getLong(RETURN_LAST_ATTEMPT);Path path=villager.getNavigation().getPath();
        BlockPos navTarget=villager.getNavigation().getTargetPos();
        boolean alreadyHeadingHome=path!=null&&!villager.getNavigation().isDone()&&navTarget!=null&&insideResidence(navTarget,home);
        boolean competingPath=path!=null&&!villager.getNavigation().isDone()&&navTarget!=null&&!insideResidence(navTarget,home);
        boolean competingWalkTarget=villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .filter(walk->!insideResidence(walk.getTarget().currentBlockPosition(),home)).isPresent();
        if(competingPath||competingWalkTarget){
            villager.getNavigation().stop();villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            alreadyHeadingHome=false;
        }
        if(target!=null&&!alreadyHeadingHome&&(competingPath||competingWalkTarget||now-last>=RETURN_RETRY_TICKS)){
            tag.putLong(RETURN_LAST_ATTEMPT,now);
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            Path destinationPath=villager.getNavigation().createPath(target,0);
            if(destinationPath!=null)villager.getNavigation().moveTo(destinationPath,0.85D);
            else villager.getNavigation().moveTo(target.getX()+0.5D,target.getY(),target.getZ()+0.5D,0.85D);
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(target,0.85F,0));
        }
    }

    /** Uses the villager's existing nearest-hostile perception, then filters to close, visible undead threats. */
    private static boolean hasNearbyPerceivedThreat(Villager villager){
        return villager.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE).filter(entity->
                entity.isAlive()&&(entity instanceof Zombie||entity instanceof AbstractSkeleton)
                        &&villager.distanceToSqr(entity)<=64.0D&&villager.hasLineOfSight(entity)).isPresent();
    }

    private static BlockPos readReturnTarget(net.minecraft.nbt.CompoundTag tag){
        return tag.getBoolean(RETURN_TARGET)?new BlockPos(tag.getInt(RETURN_TARGET_X),tag.getInt(RETURN_TARGET_Y),tag.getInt(RETURN_TARGET_Z)):null;
    }
    private static void writeReturnTarget(net.minecraft.nbt.CompoundTag tag,BlockPos target){
        tag.putBoolean(RETURN_TARGET,true);tag.putInt(RETURN_TARGET_X,target.getX());tag.putInt(RETURN_TARGET_Y,target.getY());tag.putInt(RETURN_TARGET_Z,target.getZ());
    }
    private static void clearHomeReturn(net.minecraft.nbt.CompoundTag tag){
        tag.remove(RETURN_ACTIVE);tag.remove(RETURN_FORCED);tag.remove(RETURN_STARTED);tag.remove(RETURN_LAST_ATTEMPT);tag.remove(RETURN_LAST_SEARCH);tag.remove(RETURN_HOME);
        tag.remove(RETURN_TARGET);tag.remove(RETURN_TARGET_X);tag.remove(RETURN_TARGET_Y);tag.remove(RETURN_TARGET_Z);
    }
    private static boolean insideResidence(BlockPos pos,ResidenceData.Home home){
        int r=home.size/2;
        return pos.getX()>=home.pos.getX()-r&&pos.getX()<home.pos.getX()+r+1
                &&pos.getZ()>=home.pos.getZ()-r&&pos.getZ()<home.pos.getZ()+r+1
                &&pos.getY()>=home.pos.getY()+1&&pos.getY()<home.pos.getY()+home.size+1;
    }
    private static boolean bedInsideHome(ServerLevel level,BlockPos head,ResidenceData.Home home){
        if(!insideResidence(head,home)||!level.hasChunkAt(head))return false;
        BlockState state=level.getBlockState(head);
        if(!(state.getBlock() instanceof BedBlock)||state.getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD)return false;
        BlockPos foot=head.relative(state.getValue(BedBlock.FACING).getOpposite());
        return insideResidence(foot,home);
    }
    private static boolean insideResidence(double x,double y,double z,ResidenceData.Home home){
        int r=home.size/2;
        return x>=home.pos.getX()-r&&x<home.pos.getX()+r+1&&z>=home.pos.getZ()-r&&z<home.pos.getZ()+r+1
                &&y>=home.pos.getY()+1-0.05&&y<home.pos.getY()+home.size+1;
    }
    private boolean isSafeDestination(ServerLevel level,Villager villager,BlockPos feet,ResidenceData.Home home){
        if(!insideResidence(feet,home)||!level.hasChunkAt(feet)||!level.hasChunkAt(feet.above())||!level.hasChunkAt(feet.below()))return false;
        BlockState floor=level.getBlockState(feet.below()),at=level.getBlockState(feet),above=level.getBlockState(feet.above());
        return floor.isFaceSturdy(level,feet.below(),Direction.UP)&&at.getCollisionShape(level,feet).isEmpty()
                &&above.getCollisionShape(level,feet.above()).isEmpty()&&at.getFluidState().isEmpty()&&above.getFluidState().isEmpty()
                &&!at.isBurning(level,feet)&&!above.isBurning(level,feet.above())
                &&level.noCollision(villager,new AABB(feet.getX()+.15,feet.getY(),feet.getZ()+.15,feet.getX()+.85,feet.getY()+1.95,feet.getZ()+.85));
    }
    private boolean keepResidentInsideDuringRain(ServerLevel level,ResidenceData data,Villager villager,ResidenceData.Home home,boolean inside,boolean wander){
        if(!inside){
            if(villager.tickCount%10==0)data.releaseBed(villager.getUUID());
            villager.stopSleeping();
            if(hasActiveInternalPath(villager,home))return false;
            if(villager.tickCount%10!=0)return false;
            BlockPos target=findSafeSpot(level,villager,home.pos,home.size);
            if(target!=null){
                villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                Path path=villager.getNavigation().createPath(target,0);
                if(path!=null&&path.canReach())villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(target,.85F,0));
            }
            return false;
        }

        var brain=villager.getBrain();
        var walkTarget=brain.getMemory(MemoryModuleType.WALK_TARGET);
        if(walkTarget.isPresent()&&!insideResidence(walkTarget.get().getTarget().currentBlockPosition(),home)){
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getNavigation().stop();
            walkTarget=Optional.empty();
        }
        Path activePath=villager.getNavigation().getPath();
        if(activePath!=null&&!pathStaysInside(activePath,home)){
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getNavigation().stop();
            walkTarget=Optional.empty();
        }
        if(!wander||walkTarget.isPresent()||!villager.getNavigation().isDone()||villager.tickCount%10!=0)return true;
        if(villager.getRandom().nextInt(5)==0)return true;
        BlockPos destination=findRandomReachableInteriorSpot(level,villager,home);
        if(destination!=null)brain.setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(destination,.48F,1));
        return true;
    }
    private boolean hasActiveInternalPath(Villager villager,ResidenceData.Home home){
        Path path=villager.getNavigation().getPath();
        if(path==null||villager.getNavigation().isDone())return false;
        BlockPos target=villager.getNavigation().getTargetPos();
        return target!=null&&insideResidence(target,home);
    }
    private boolean pathStaysInside(Path path,ResidenceData.Home home){
        for(int i=path.getNextNodeIndex();i<path.getNodeCount();i++)if(!insideResidence(path.getNode(i).asBlockPos(),home))return false;
        return true;
    }
    private BlockPos findRandomReachableInteriorSpot(ServerLevel level,Villager villager,ResidenceData.Home home){
        int r=home.size/2;
        List<BlockPos> candidates=new ArrayList<>();
        for(int y=home.pos.getY()+1;y<home.pos.getY()+home.size+1;y++)
            for(int x=home.pos.getX()-r;x<=home.pos.getX()+r;x++)
                for(int z=home.pos.getZ()-r;z<=home.pos.getZ()+r;z++){
                    BlockPos feet=new BlockPos(x,y,z);
                    if(!level.hasChunkAt(feet)||!level.hasChunkAt(feet.above())||!level.hasChunkAt(feet.below()))continue;
                    BlockState floor=level.getBlockState(feet.below()),at=level.getBlockState(feet),above=level.getBlockState(feet.above());
                    if(!floor.isFaceSturdy(level,feet.below(),Direction.UP)||!at.getCollisionShape(level,feet).isEmpty()||!above.getCollisionShape(level,feet.above()).isEmpty())continue;
                    if(!at.getFluidState().isEmpty()||!above.getFluidState().isEmpty()||at.isBurning(level,feet)||above.isBurning(level,feet.above()))continue;
                    if(!level.noCollision(villager,new AABB(x+.15,y,z+.15,x+.85,y+1.95,z+.85)))continue;
                    candidates.add(feet);
                }
        for(int attempt=0;attempt<24&&!candidates.isEmpty();attempt++){
            int index=villager.getRandom().nextInt(candidates.size());
            BlockPos candidate=candidates.remove(index);
            if(candidate.distSqr(villager.blockPosition())<4.0D)continue;
            Path path=villager.getNavigation().createPath(candidate,0);
            if(path==null||!path.canReach()||!pathStaysInside(path,home))continue;
            return candidate;
        }
        return null;
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
            if(!a.getFluidState().isEmpty()||!b.getFluidState().isEmpty()||a.isBurning(level,feet)||b.isBurning(level,feet.above()))continue;
            if(!level.noCollision(v,new AABB(fx+.15,fy,fz+.15,fx+.85,fy+1.95,fz+.85)))continue;
            return feet;
        }
        return null;
    }
    @SubscribeEvent public void onRegister(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof Villager villager)||event.getLevel().isClientSide)return;
        var tag=event.getEntity().getPersistentData();if(!tag.getBoolean(REG) && !tag.getBoolean(UNLINK))return;
        // A linking/unlinking click is a mod action. Always consume it so a failed
        // link (already linked/full/wrong anchor) cannot continue into vanilla
        // villager breeding or another entity interaction.
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(!tag.getString(DIM).equals(level.dimension().location().toString())){event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.residenciasaldeoes.wrong_dimension"),true);return;}
        BlockPos pos=BlockPos.of(tag.getLong(POS));if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof ResidentialAnchorBlockEntity)){tag.putBoolean(REG,false);return;}
        if(tag.getBoolean(UNLINK)){
            ResidenceData data=ResidenceData.get(level);ResidenceData.Home home=data.owner(villager.getUUID());
            if(home!=null && home.dimension.equals(level.dimension()) && home.pos.equals(pos)){
                data.unlink(level,villager.getUUID());tag.putBoolean(UNLINK,false);
                event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.residenciasaldeoes.unlinked_one"),true);
            }else event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.residenciasaldeoes.not_resident"),true);
            return;
        }
        String result=ResidenceData.get(level).link(level,pos,villager);
        String key=switch(result){case "ok"->"message.residenciasaldeoes.linked";case "full"->"message.residenciasaldeoes.full";default->"message.residenciasaldeoes.already";};
        event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable(key),true);
        if(result.equals("ok"))tag.putBoolean(REG,false);
    }
    @SubscribeEvent public void onVillagerConversion(LivingConversionEvent.Post event){
        if(!(event.getEntity() instanceof Villager villager)
                ||!(event.getOutcome() instanceof net.minecraft.world.entity.monster.ZombieVillager)
                ||!(villager.level instanceof ServerLevel level))return;
        ResidenceData.get(level).unlinkConverted(level,villager,event.getOutcome());
    }
    @SubscribeEvent public void onVillagerDrops(LivingDropsEvent event){
        if(!(event.getEntity() instanceof Villager villager)||villager.isBaby()||!(villager.level instanceof ServerLevel))return;
        CompoundTag persistent=villager.getPersistentData();
        String marker="residenciasaldeoes_fragment_drop_processed";
        if(persistent.getBoolean(marker))return;
        persistent.putBoolean(marker,true);
        int count=villager.getRandom().nextFloat()<0.75F?1:2;
        event.getDrops().add(new ItemEntity(villager.level,villager.getX(),villager.getY(),villager.getZ(),new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),count)));
    }
    @SubscribeEvent public void onDeath(LivingDeathEvent event){if(event.getEntity() instanceof Villager v&&v.level instanceof ServerLevel sl){if(v.getPersistentData().contains(EMERALD_TRANSFER))finishEmeraldTransfer(sl,v,v.getPersistentData().getCompound(EMERALD_TRANSFER),true);ResidenceData.get(sl).unlink(sl,v.getUUID());}}
}
