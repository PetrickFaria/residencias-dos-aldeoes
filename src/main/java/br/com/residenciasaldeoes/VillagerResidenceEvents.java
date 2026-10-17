Warning: truncated output (original token count: 23912)
Total output lines: 1356

package br.com.residenciasaldeoes;

import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.event.level.BlockEvent;
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
    private static final String EMERALD_TRANSFER="residenciasaldeoes_emerald_transfer";
    private static final String NIGHT_DOOR_TASK="residenciasaldeoes_night_door_task";
    private static final String NIGHT_DOOR_RESUME="residenciasaldeoes_night_door_resume";
    private static final String NIGHT_WANDER="residenciasaldeoes_night_wander";
    private static final String BED_RETRY_AFTER="residenciasaldeoes_bed_retry_after";
    private static final Map<ServerLevel,Map<String,Long>> NIGHT_DOOR_NIGHTS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<Long,Long>> RESIDENTIAL_DOOR_COOLDOWNS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<Long,Long>> PENDING_OPEN_DOORS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<String,Long>> LAST_PERIODIC_DOOR_SCAN=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<String,Boolean>> RESIDENTIAL_RAINING_STATE=new WeakHashMap<>();
    private static final long NIGHT_START=13_000L, NIGHT_END=23_000L;
    private static final double NIGHT_DOOR_WALK_SPEED=.48D;
    private static final double NIGHT_WANDER_SPEED=.32D;
    private static final int BED_ANCHOR_INTERVAL=100;
    private static final long DOOR_REOPEN_COOLDOWN=200L,DOOR_OPEN_STABILITY_TICKS=40L,DOOR_SCAN_INTERVAL=300L;
    private static final String RESIDENT_DEATH_NOTICE="residenciasaldeoes_death_notice_sent";
    private static final String RESIDENT_WITCH_NOTICE="residenciasaldeoes_witch_notice_sent";
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
        boolean setIntoNight=action.equals("set")&&oldTime<NIGHT_START&&newTime>=NIGHT_START&&newTime<NIGHT_END;
        boolean addIntoNight=action.equals("add")&&oldTime<NIGHT_START&&newTime>=NIGHT_START&&newTime<NIGHT_END;
        PRODUCTION_CLOCKS.put(level,new DawnSample(level.getGameTime(),target));
        if(setIntoNight||addIntoNight)beginNightDoorClosure(level,Math.floorDiv(target,DAY_LENGTH));
        if(setIntoDawn||addIntoDawn)
            processProductionDawn(level,level.getServer().overworld().getGameTime());
    }
    @SubscribeEvent public void onProductionLevelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;
        long productionGameTime=level.getServer().overworld().getGameTime();
        long levelGameTime=level.getGameTime(),dayTime=level.getDayTime();
        DawnSample previous=PRODUCTION_CLOCKS.put(level,new DawnSample(levelGameTime,dayTime));
        if(previous!=null&&crossedNaturalDawn(previous,new DawnSample(levelGameTime,dayTime)))
            processProductionDawn(level,productionGameTime);
        if(previous!=null&&crossedNightfall(previous,new DawnSample(levelGameTime,dayTime)))
            beginNightDoorClosure(level,Math.floorDiv(dayTime,DAY_LENGTH));
        processResidentialWeatherTransitions(level);
        scanLoadedResidentialDoors(level);
        processPendingDoorChanges(level);
        processLoadedProduction(level,productionGameTime);
        beginScheduledCollections(level,productionGameTime);
    }

    /** A real door state change schedules only the affected home's bounded task. */
    @SubscribeEvent public void onResidentialDoorChanged(BlockEvent.NeighborNotifyEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        BlockPos pos=event.getPos();BlockState state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)
                ||!state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)
                ||state.getValue(net.minecraft.world.level.block.DoorBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                ||!state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN))return;
        ResidenceData data=ResidenceData.get(level);
        if(!isDoorClosureRequiredAt(level,data,pos))return;
        PENDING_OPEN_DOORS.computeIfAbsent(level,ignored->new HashMap<>()).put(pos.asLong(),level.getGameTime());
    }

    /** Wait until an open door is no longer being used as a passage before assigning a closer. */
    private void processPendingDoorChanges(ServerLevel level){
        Map<Long,Long> pending=PENDING_OPEN_DOORS.get(level);if(pending==null||pending.isEmpty())return;
        long now=level.getGameTime();if(now%10L!=0L)return;ResidenceData data=ResidenceData.get(level);
        for(var entry:new ArrayList<>(pending.entrySet())){
            BlockPos pos=BlockPos.of(entry.getKey());
            if(!level.hasChunkAt(pos)){pending.remove(entry.getKey());continue;}
            BlockState state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)
                    ||!state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)
                    ||state.getValue(net.minecraft.world.level.block.DoorBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                    ||!state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)){
                pending.remove(entry.getKey());continue;
            }
            if(!isDoorClosureRequiredAt(level,data,pos)){pending.remove(entry.getKey());continue;}
            if(residentUsingDoorway(level,data,pos)){pending.put(entry.getKey(),now);continue;}
            if(RESIDENTIAL_DOOR_COOLDOWNS.getOrDefault(level,Map.of()).getOrDefault(pos.asLong(),Long.MIN_VALUE/2)>now)continue;
            if(now-entry.getValue()<DOOR_OPEN_STABILITY_TICKS)continue;
            pending.remove(entry.getKey());
            for(ResidenceData.Home home:new ArrayList<>(data.homes())){
                if(!home.dimension.equals(level.dimension())||!level.hasChunkAt(home.pos)||!withinDoorBounds(pos,home)
                        ||!shouldCloseResidentialDoors(level,home))continue;
                queueResidentialDoorTask(level,home,List.of(pos.immutable()),Math.floorDiv(level.getDayTime(),DAY_LENGTH));
            }
        }
        if(pending.isEmpty())PENDING_OPEN_DOORS.remove(level);
    }

    private static boolean residentUsingDoorway(ServerLevel level,ResidenceData data,BlockPos door){
        net.minecraft.world.phys.Vec3 center=net.minecraft.world.phys.Vec3.atCenterOf(door);
        for(ResidenceData.Home home:data.homes()){
            if(!home.dimension.equals(level.dimension()))continue;
            for(UUID id:home.residents)if(level.getEntity(id) instanceof Villager resident&&resident.isAlive()
                    &&resident.distanceToSqr(center)<=9.0D)return true;
        }
        return false;
    }

    /** A fixed 300-tick fallback catches door changes that do not emit neighbor notifications. */
    private static void scanLoadedResidentialDoors(ServerLevel level){
        long now=level.getGameTime();
        if(now%DOOR_SCAN_INTERVAL!=0L)return;
        ResidenceData data=ResidenceData.get(level);
        Map<String,Long> scans=LAST_PERIODIC_DOOR_SCAN.computeIfAbsent(level,ignored->new HashMap<>());
        for(ResidenceData.Home home:new ArrayList<>(data.homes())){
            if(!home.dimension.equals(level.dimension())||home.residents.isEmpty()||!level.hasChunkAt(home.pos)
                    ||!shouldCloseResidentialDoors(level,home)
                    ||!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity))continue;
            String key=home.dimension.location()+"|"+home.pos.asLong();
            if(now-scans.getOrDefault(key,Long.MIN_VALUE/2)<DOOR_SCAN_INTERVAL)continue;
            scans.put(key,now);
            for(BlockPos door:findOpenResidentialDoors(level,home))
                PENDING_OPEN_DOORS.computeIfAbsent(level,ignored->new HashMap<>()).putIfAbsent(door.asLong(),now);
        }
    }

    private static boolean withinDoorBounds(BlockPos pos,ResidenceData.Home home){
        int r=home.size/2;
        return pos.getX()>=home.pos.getX()-r-1&&pos.getX()<=home.pos.getX()+r+1
                &&pos.getZ()>=home.pos.getZ()-r-1&&pos.getZ()<=home.pos.getZ()+r+1
                &&pos.getY()>=home.pos.getY()&&pos.getY()<=home.pos.getY()+home.size+1;
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
        return adjustedClock&&oldTime>DAWN_COMMAND_WINDOW_TICKS&&newTime<=DAWN_COMMAND_WINDOW_TICKS;
    }
    private static boolean crossedNightfall(DawnSample before,DawnSample after){
        long oldTime=Math.floorMod(before.dayTime(),DAY_LENGTH);
        long newTime=Math.floorMod(after.dayTime(),DAY_LENGTH);
        if(oldTime>=NIGHT_START||newTime<NIGHT_START||newTime>=NIGHT_END)return false;
        boolean naturalTick=after.gameTime()-before.gameTime()==1L
                &&after.dayTime()-before.dayTime()==1L;
        boolean adjustedClock=after.gameTime()>=before.gameTime()
                &&after.gameTime()-before.gameTime()<=1L
                &&after.dayTime()-before.dayTime()!=1L;
        return naturalTick||adjustedClock;
    }
    private static boolean shouldCloseResidentialDoors(ServerLevel level,ResidenceData.Home home){
        long phase=Math.floorMod(level.getDayTime(),DAY_LENGTH);
        boolean night=phase>=NIGHT_START&&phase<NIGHT_END;
        // Test the airspace immediately above the residence anchor so sheltered homes
        // do not react to rain falling elsewhere in the dimension.
        return night||level.isRainingAt(home.pos.above());
    }
    private static boolean isDoorClosureRequiredAt(ServerLevel level,ResidenceData data,BlockPos pos){
        for(ResidenceData.Home home:data.homes())
            if(home.dimension.equals(level.dimension())&&level.hasChunkAt(home.pos)
                    &&withinDoorBounds(pos,home)&&shouldCloseResidentialDoors(level,home))return true;
        return false;
    }
    /** Detect rain transitions only for loaded residences, then reuse the bounded physical door worker. */
    private void processResidentialWeatherTransitions(ServerLevel level){
        ResidenceData data=ResidenceData.get(level);
        Map<String,Boolean> previous=RESIDENTIAL_RAINING_STATE.computeIfAbsent(level,ignored->new HashMap<>());
        for(ResidenceData.Home home:new ArrayList<>(data.homes())){
            if(!home.dimension.equals(level.dimension())||home.residents.isEmpty()||!level.hasChunkAt(home.pos)
                    ||!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity))continue;
            String key=home.dimension.location()+"|"+home.pos.asLong();
            boolean raining=level.isRainingAt(home.pos.above());
            Boolean wasRaining=previous.put(key,raining);
            if(raining&&!Boolean.TRUE.equals(wasRaining))beginWeatherDoorClosure(level,home);
        }
    }
    /** A rain-triggered task is scoped to one loaded home and only runs while that home is wet. */
    private void beginWeatherDoorClosure(ServerLevel level,ResidenceData.Home home){
        if(!home.dimension.equals(level.dimension())||home.residents.isEmpty()||!level.hasChunkAt(home.pos)
                ||!shouldCloseResidentialDoors(level,home)
                ||!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity))return;
        queueResidentialDoorTask(level,home,findOpenResidentialDoors(level,home),Math.floorDiv(level.getDayTime(),DAY_LENGTH));
    }
    /** One bounded scan per loaded residence at the start of each night. */
    private void beginNightDoorClosure(ServerLevel level,long nightPeriod){
        ResidenceData data=ResidenceData.get(level);
        Map<String,Long> processed=NIGHT_DOOR_NIGHTS.computeIfAbsent(level,ignored->new HashMap<>());
        for(ResidenceData.Home home:new ArrayList<>(data.homes())){
            if(!home.dimension.equals(level.dimension())||home.residents.isEmpty()||!level.hasChunkAt(home.pos))continue;
            String homeKey=home.dimension.location()+"|"+home.pos.asLong();
            if(processed.getOrDefault(homeKey,Long.MIN_VALUE)==nightPeriod)continue;
            processed.put(homeKey,nightPeriod);
            if(!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity))continue;

            List<BlockPos> openDoors=findOpenResidentialDoors(level,home);
            queueResidentialDoorTask(level,home,openDoors,nightPeriod);
            LAST_PERIODIC_DOOR_SCAN.computeIfAbsent(level,ignored->new HashMap<>()).put(homeKey,level.getGameTime());
        }
    }

    private void queueResidentialDoorTask(ServerLevel level,ResidenceData.Home home,List<BlockPos> candidates,long period){
        if(candidates.isEmpty()||home.residents.isEmpty()||!level.hasChunkAt(home.pos)
                ||!(level.getBlockEntity(home.pos) instanceof ResidentialAnchorBlockEntity))return;
        long now=level.getGameTime();ResidenceData data=ResidenceData.get(level);
        Map<Long,Long> cooldowns=RESIDENTIAL_DOOR_COOLDOWNS.computeIfAbsent(level,ignored->new HashMap<>());
        cooldowns.entrySet().removeIf(entry->entry.getValue()<=now);
        candidates=candidates.stream().filter(p->withinDoorBounds(p,home)&&level.hasChunkAt(p)
                &&level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                &&level.getBlockState(p).getValue(net.minecraft.world.level.block.DoorBlock.OPEN)
                &&cooldowns.getOrDefault(p.asLong(),Long.MIN_VALUE/2)<=now).distinct().toList();
        if(candidates.isEmpty())return;

        // At most one resident owns the home's navigation task. New door events join that task.
        for(UUID id:home.residents)if(level.getEntity(id) instanceof Villager active&&active.getPersistentData().contains(NIGHT_DOOR_TASK)){
            CompoundTag task=active.getPersistentData().getCompound(NIGHT_DOOR_TASK);ListTag doors=task.getList("doors",Tag.TAG_LONG);
            Set<Long> already=new HashSet<>();for(int i=0;i<doors.size();i++)already.add(((net.minecraft.nbt.LongTag)doors.get(i)).getAsLong());
            for(BlockPos pos:candidates)if(already.add(pos.asLong()))doors.add(net.minecraft.nbt.LongTag.valueOf(pos.asLong()));
            return;
        }

        // Overlapping residential bounds still share a single physical door. Do not
        // let another home's active route start competing for an already assigned one.
        Set<Long> assignedElsewhere=new HashSet<>();
        for(ResidenceData.Home other:data.homes()){
            if(other==home||!other.dimension.equals(level.dimension()))continue;
            for(UUID id:other.residents)if(level.getEntity(id) instanceof Villager active&&active.getPersistentData().contains(NIGHT_DOOR_TASK)){
                CompoundTag task=active.getPersistentData().getCompound(NIGHT_DOOR_TASK);
                if(task.getLong("home")!=other.pos.asLong())continue;
                ListTag doors=task.getList("doors",Tag.TAG_LONG);
                for(int i=0;i<doors.size();i++)assignedElsewhere.add(((net.minecraft.nbt.LongTag)doors.get(i)).getAsLong());
            }
        }
        candidates=new ArrayList<>(candidates.stream().filter(pos->!assignedElsewhere.contains(pos.asLong())).toList());
        if(candidates.isEmpty())return;

        List<Villager> active=new ArrayList<>();Villager leader=null;
        for(UUID id:home.residents){
            if(!(level.getEntity(id) instanceof Villager villager)||!villager.isAlive()||villager.isBaby()||villager.isNoAi()
                    ||villager.isTrading()||villager.getPersistentData().contains(EMERALD_TRANSFER)
                    ||villager.getPersistentData().contains(RETURN_ACTIVE)||villager.getPersistentData().contains(NIGHT_DOOR_TASK))continue;
            if(leader==null)leader=villager;
            active.add(villager);
        }
        if(active.isEmpty())return;
        // At a nighttime door assignment, reconcile sleeping positions and give each
        // awake resident a distinct reachable bed before deciding who is bedless.
        // This is an event-triggered pass, never part of the per-tick path.
        for(Villager resident:active){
            if(resident.isSleeping()){
                BlockPos sleepingBed=resident.getSleepingPos().orElse(null);
                if(sleepingBed!=null&&bedInsideHome(level,sleepingBed,home)
                        &&(data.bedOwner(level.dimension(),sleepingBed)==null||data.ownsBed(level.dimension(),sleepingBed,resident.getUUID())))
                    data.reserveBed(resident.getUUID(),level.dimension(),home.pos,sleepingBed);
            }else if(!hasValidResidentialBed(level,data,resident,home)){
                BlockPos available=findAvailableBed(level,data,resident,home);
                if(available!=null)data.reserveBed(resident.getUUID(),level.dimension(),home.pos,available);
            }
        }
        List<Villager> awakeWithoutBed=new ArrayList<>();
        for(Villager resident:active)
            if(!resident.isSleeping()&&!hasValidResidentialBed(level,data,resident,home))awakeWithoutBed.add(resident);

        Villager chosen=null;
        if(!awakeWithoutBed.isEmpty()){
            double nearest=Double.MAX_VALUE;
            for(Villager resident:awakeWithoutBed){
                double distance=leader==null?0.0D:resident.distanceToSqr(leader);
                if(chosen==null||distance<nearest){chosen=resident;nearest=distance;}
            }
        }else if(leader!=null&&!leader.isSleeping()){
            chosen=leader;
        }else if(active.stream().allMatch(Villager::isSleeping)){
            // Only wake the first resident when every active resident is asleep.
            chosen=leader;
        }else{
            // If the leader sleeps while another bed-owning resident is already awake,
            // use the first awake resident and leave the leader's bed undisturbed.
            for(Villager resident:active)if(!resident.isSleeping()){chosen=resident;break;}
        }
        if(chosen!=null&&chosen.isSleeping()){
            BlockPos returnBed=chosen.getSleepingPos().orElse(null);
            ResidenceData.BedClaim claim=data.bedClaim(chosen.getUUID());
            if(returnBed==null&&claim!=null&&claim.dimension().equals(level.dimension())&&claim.home().equals(home.pos))returnBed=claim.bed();
            if(returnBed!=null&&bedInsideHome(level,returnBed,home)
                    &&(data.bedOwner(level.dimension(),returnBed)==null||data.own…11912 tokens truncated…"look_yaw");
        float delta=net.minecraft.util.Mth.wrapDegrees(target-current);
        villager.setYHeadRot(current+net.minecraft.util.Mth.clamp(delta,-2.0F,2.0F));

        if(state.getInt("phase")==0){
            BlockPos targetPos=BlockPos.of(state.getLong("target"));
            var walk=villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
            if(walk!=null&&!walk.getTarget().currentBlockPosition().equals(targetPos)){
                // A different vanilla goal took navigation; do not fight it.
                clearNightWander(villager,false);return;
            }
            if(now>=state.getLong("until")
                    ||(now-state.getLong("started")>=10L&&villager.getNavigation().isDone())){
                villager.getNavigation().stop();
                villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                villager.getBrain().eraseMemory(MemoryModuleType.PATH);
                state.putInt("phase",1);state.putLong("until",now+randomWanderDuration(villager));
                persistent.put(NIGHT_WANDER,state);
            }
            return;
        }

        if(villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isPresent()){
            // Preserve higher-priority vanilla behavior if it starts while resting.
            clearNightWander(villager,false);return;
        }
        if(!villager.getNavigation().isDone())villager.getNavigation().stop();
        if(now<state.getLong("until"))return;

        BlockPos destination=findRandomReachableInteriorSpot(level,villager,home,true);
        if(destination==null){state.putLong("until",now+20L);persistent.put(NIGHT_WANDER,state);return;}
        Path path=villager.getNavigation().createPath(destination,0);
        if(path==null||!path.canReach()||!pathStaysInside(path,home)||!pathAvoidsWoodenDoors(level,path)){
            state.putLong("until",now+20L);persistent.put(NIGHT_WANDER,state);return;
        }
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(destination,(float)NIGHT_WANDER_SPEED,1));
        villager.getNavigation().moveTo(path,NIGHT_WANDER_SPEED);
        state.putInt("phase",0);state.putLong("target",destination.asLong());
        state.putLong("started",now);state.putLong("until",now+randomWanderDuration(villager));persistent.put(NIGHT_WANDER,state);
    }

    private static long randomWanderDuration(Villager villager){return 60L+villager.getRandom().nextInt(81);}
    private static long randomLookInterval(Villager villager){return 40L+villager.getRandom().nextInt(61);}
    private static void clearNightWander(Villager villager,boolean stopNavigation){
        if(!villager.getPersistentData().contains(NIGHT_WANDER))return;
        villager.getPersistentData().remove(NIGHT_WANDER);
        if(stopNavigation){
            villager.getNavigation().stop();
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getBrain().eraseMemory(MemoryModuleType.PATH);
        }
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
        return findRandomReachableInteriorSpot(level,villager,home,false);
    }
    private BlockPos findRandomReachableInteriorSpot(ServerLevel level,Villager villager,ResidenceData.Home home,boolean avoidDoors){
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
            if(path==null||!path.canReach()||!pathStaysInside(path,home)||(avoidDoors&&!pathAvoidsWoodenDoors(level,path)))continue;
            return candidate;
        }
        return null;
    }
    private static boolean pathAvoidsWoodenDoors(ServerLevel level,Path path){
        for(int i=path.getNextNodeIndex();i<path.getNodeCount();i++){
            BlockPos node=path.getNode(i).asBlockPos();
            if(!level.hasChunkAt(node))return false;
            BlockState state=level.getBlockState(node);
            if(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                    &&state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS))return false;
        }
        return true;
    }
    private BlockPos findAvailableBed(ServerLevel level,ResidenceData data,Villager villager,ResidenceData.Home home){
        BlockPos anchor=home.pos;int size=home.size,r=size/2;
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
                    BlockState footState=level.getBlockState(foot);
                    if(!(footState.getBlock() instanceof BedBlock)
                            ||footState.getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.FOOT
                            ||footState.getValue(BedBlock.FACING)!=state.getValue(BedBlock.FACING))continue;
                    if(findReachableBedApproach(level,villager,p,home)==null)continue;
                    if(!claimResidentialBedPoi(level,data,villager,home,p))continue;
                    if(data.reserveBed(villager.getUUID(),level.dimension(),home.pos,p))return p;
                    releaseResidentialBedPoi(level,villager,p);
                }
        return null;
    }
    /** Claim the same HOME POI vanilla sleep behaviors use, instead of relying only on the mod's reservation map. */
    private static boolean claimResidentialBedPoi(ServerLevel level,ResidenceData data,Villager villager,ResidenceData.Home home,BlockPos bed){
        var brain=villager.getBrain();
        net.minecraft.core.GlobalPos current=brain.getMemory(MemoryModuleType.HOME).orElse(null);
        boolean sameHome=current!=null&&current.dimension().equals(level.dimension())&&current.pos().equals(bed);
        if(sameHome){
            if(isRegisteredHomePoi(level,bed))return true;
            // A saved HOME memory may outlive the bed/POI. Forget only this stale
            // memory; PoiManager.release throws when the position has no record.
            brain.eraseMemory(MemoryModuleType.HOME);
            current=null;
        }
        ServerLevel oldLevel=current==null?null:(current.dimension().equals(level.dimension())?level:level.getServer().getLevel(current.dimension()));
        boolean oldPoiLoaded=oldLevel!=null&&oldLevel.hasChunkAt(current.pos());
        if(oldPoiLoaded&&!isRegisteredHomePoi(oldLevel,current.pos())){
            brain.eraseMemory(MemoryModuleType.HOME);
            current=null;
            oldLevel=null;
        }

        // Do not call take() based only on the visible block state. During chunk
        // restoration a bed block and its HOME POI can briefly disagree.
        if(!isRegisteredHomePoi(level,bed))return false;
        var acquired=level.getPoiManager().take(type->type.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),
                (type,pos)->pos.equals(bed),bed,1);
        if(acquired.isEmpty())return false;
        // Keep a valid old assignment if the new POI could not be claimed. Once
        // the replacement is secured, release the old ticket only if its POI is
        // still present and its chunk is already loaded.
        if(current!=null&&oldPoiLoaded&&oldLevel!=null&&isRegisteredHomePoi(oldLevel,current.pos()))
            oldLevel.getPoiManager().release(current.pos());
        brain.setMemory(MemoryModuleType.HOME,net.minecraft.core.GlobalPos.of(level.dimension(),bed));
        return true;
    }
    private static void releaseResidentialBedPoi(ServerLevel level,Villager villager,BlockPos bed){
        net.minecraft.core.GlobalPos current=villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
        if(current!=null&&current.dimension().equals(level.dimension())&&current.pos().equals(bed)&&level.hasChunkAt(bed)){
            if(isRegisteredHomePoi(level,bed))level.getPoiManager().release(bed);
            villager.getBrain().eraseMemory(MemoryModuleType.HOME);
        }
    }
    private static boolean isRegisteredHomePoi(ServerLevel level,BlockPos pos){
        return level!=null&&level.hasChunkAt(pos)&&level.getPoiManager().getType(pos)
                .filter(type->type.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME)).isPresent();
    }
    private BlockPos findReachableBedApproach(ServerLevel level,Villager villager,BlockPos bed,ResidenceData.Home home){
        List<BlockPos> approaches=new ArrayList<>();
        for(Direction direction:Direction.Plane.HORIZONTAL){
            BlockPos approach=bed.relative(direction);
            if(!insideResidence(approach,home)||!level.hasChunkAt(approach)||!level.hasChunkAt(approach.below()))continue;
            if(!isSafeDestination(level,villager,approach,home))continue;
            approaches.add(approach);
        }
        approaches.sort(Comparator.comparingDouble(villager.blockPosition()::distSqr));
        for(BlockPos approach:approaches){
            Path path=villager.getNavigation().createPath(approach,0);
            if(path!=null&&path.canReach()&&pathStaysInside(path,home))return approach;
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
    private static void broadcastResidentNotice(ServerLevel level,Villager villager,ResidenceData data,String marker,Component message){
        if(data.owner(villager.getUUID())==null||villager.getPersistentData().getBoolean(marker))return;
        villager.getPersistentData().putBoolean(marker,true);
        for(ServerPlayer player:level.players())player.sendSystemMessage(message);
    }
    private static Component describeResidentDeath(String name,DamageSource source){
        net.minecraft.world.entity.Entity attacker=source.getEntity();
        if(attacker instanceof Zombie)return Component.literal(name+" foi morto por um zumbi.");
        if(attacker instanceof AbstractSkeleton)return Component.literal(name+" foi morto por um esqueleto.");
        if(attacker instanceof net.minecraft.world.entity.monster.Creeper)return Component.literal(name+" foi morto por um creeper.");
        if(attacker!=null)return Component.literal(name+" foi morto por ").append(attacker.getDisplayName()).append(Component.literal("."));
        String suffix=switch(source.getMsgId()){
            case "fall","fallingBlock","flyIntoWall"->" morreu devido a uma queda.";
            case "onFire","inFire","lava","hotFloor"->" morreu queimado.";
            case "drown","dryout"->" morreu afogado.";
            case "lightningBolt"->" morreu atingido por um raio.";
            case "starve"->" morreu de fome.";
            case "freeze"->" morreu congelado.";
            default->" morreu.";
        };
        return Component.literal(name+suffix);
    }
    @SubscribeEvent public void onVillagerConversion(LivingConversionEvent.Post event){
        if(!(event.getEntity() instanceof Villager villager)||!(villager.level instanceof ServerLevel level)
                ||(!(event.getOutcome() instanceof Witch)&&!(event.getOutcome() instanceof net.minecraft.world.entity.monster.ZombieVillager)))return;
        ResidenceData data=ResidenceData.get(level);
        if(villager.getPersistentData().contains(EMERALD_TRANSFER))finishEmeraldTransfer(level,villager,villager.getPersistentData().getCompound(EMERALD_TRANSFER),true);
        if(event.getOutcome() instanceof Witch){
            String name=data.residentName(villager.getUUID());
            broadcastResidentNotice(level,villager,data,RESIDENT_WITCH_NOTICE,Component.literal(name+" foi transformado em bruxa."));
            data.unlinkConverted(level,villager,event.getOutcome());
        }else if(event.getOutcome() instanceof net.minecraft.world.entity.monster.ZombieVillager){
            data.unlinkConverted(level,villager,event.getOutcome());
        }
    }
    @SubscribeEvent public void onVillagerDrops(LivingDropsEvent event){
        if(!(event.getEntity() instanceof Villager villager)||villager.isBaby()||!(villager.level instanceof ServerLevel))return;
        CompoundTag persistent=villager.getPersistentData();
        String marker="residenciasaldeoes_fragment_drop_processed";
        if(persistent.getBoolean(marker))return;
        persistent.putBoolean(marker,true);
        int count=villager.getRandom().nextInt(10)==0?2:1;
        event.getDrops().add(new ItemEntity(villager.level,villager.getX(),villager.getY(),villager.getZ(),new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),count)));
    }
    @SubscribeEvent public void onDeath(LivingDeathEvent event){if(event.getEntity() instanceof Villager v&&v.level instanceof ServerLevel sl){ResidenceData data=ResidenceData.get(sl);if(data.owner(v.getUUID())!=null){String name=data.residentName(v.getUUID());broadcastResidentNotice(sl,v,data,RESIDENT_DEATH_NOTICE,describeResidentDeath(name,event.getSource()));}if(v.getPersistentData().contains(EMERALD_TRANSFER))finishEmeraldTransfer(sl,v,v.getPersistentData().getCompound(EMERALD_TRANSFER),true);data.unlink(sl,v.getUUID());}}
}
