Warning: truncated output (original token count: 20804)
Total output lines: 907

package br.com.residenciasaldeoes.test;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.VillagerResidenceEvents;
import br.com.residenciasaldeoes.block.*;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.data.ResidentModelType;
import br.com.residenciasaldeoes.data.WardrobePackage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@GameTestHolder("residenciasaldeoes")
@PrefixGameTestTemplate(false)
public final class ResidenceGameTests {
    private static final String JOB="residenciasaldeoes_emerald_transfer";
    private static void check(boolean condition,String message){if(!condition)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static Object invoke(String name,Class<?>[] types,Object... args){
        try{Method m=VillagerResidenceEvents.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(new VillagerResidenceEvents(),args);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static void floor(GameTestHelper h){
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
            // Keep ordinary idle wandering inside the disposable test platform.
            if(x==0||x==15||z==0||z==15)for(int y=2;y<=4;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
        }
    }
    private static void clearFixtureWorld(net.minecraft.server.level.ServerLevel level){
        // The preceding drop test intentionally spawns a skeleton; it must not
        // follow and attack residents in subsequent navigation fixtures.
        java.util.List<net.minecraft.world.entity.Entity> stale=new java.util.ArrayList<>();
        level.getAllEntities().forEach(stale::add);
        for(var entity:stale)
            if(entity instanceof net.minecraft.world.entity.LivingEntity && !(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
        var data=ResidenceData.get(level);
        for(var home:new java.util.ArrayList<>(data.homes())){
            if(!home.dimension.equals(level.dimension()))continue;
            for(var id:new java.util.ArrayList<>(home.residents))if(level.getEntity(id) instanceof Villager resident)resident.discard();
            data.unlinkAll(level,home.dimension,home.pos);
        }
        for(BlockPos pos:br.com.residenciasaldeoes.data.EmeraldChestIndex.registeredPositions(level)){
            if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof EmeraldChestBlockEntity chest){chest.clearContent();level.removeBlock(pos,false);}
            br.com.residenciasaldeoes.data.EmeraldChestIndex.unregister(level,pos);
        }
    }
    @BeforeBatch(batch="physical") public static void before_physical(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="full") public static void before_full(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="dawn") public static void before_dawn(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="floor") public static void before_floor(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="missing") public static void before_missing(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="available") public static void before_available(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="index") public static void before_index(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="lid") public static void before_lid(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="doors") public static void before_doors(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="outputs") public static void before_outputs(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="sleep") public static void before_sleep(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    private static double navigationSpeed(Villager v){
        try{var field=net.minecraft.world.entity.ai.navigation.PathNavigation.class.getDeclaredField("speedModifier");field.setAccessible(true);return field.getDouble(v.getNavigation());}
        catch(Exception e){throw new RuntimeException(e);}
    }
    @GameTest(template="empty",timeoutTicks=100,batch="index")
    public static void indexed_chest_range_and_invalidation(GameTestHelper h){
        var level=h.getLevel();BlockPos center=h.absolutePos(new BlockPos(6,2,6));
        var index=br.com.residenciasaldeoes.data.EmeraldChestIndex.class;
        java.util.List<BlockPos> fixtures=new java.util.ArrayList<>();
        for(int distance:new int[]{5,20,100,500,1000,1001}){
            BlockPos pos=center.offset(distance,0,0);level.getChunkAt(pos); // Fixture setup ONLY: load test chunks explicitly.
            level.setBlock(pos,ResidenciasDosAldeoes.EMERALD_CHEST.get().defaultBlockState(),3);
            br.com.residenciasaldeoes.data.EmeraldChestIndex.register(level,pos);fixtures.add(pos);
        }
        var found=br.com.residenciasaldeoes.data.EmeraldChestIndex.nearby(level,center);
        for(int i=0;i<5;i++)check(found.contains(fixtures.get(i)),"Indexed location missing at "+new int[]{5,20,100,500,1000}[i]+" blocks");
        check(!found.contains(fixtures.get(5)),"Chest beyond 1000 blocks accepted");
        BlockPos unloaded=center.offset(700,0,400);check(!level.hasChunkAt(unloaded),"Unloaded fixture chunk must start unloaded");
        br.com.residenciasaldeoes.data.EmeraldChestIndex.register(level,unloaded);
        check(!br.com.residenciasaldeoes.data.EmeraldChestIndex.nearby(level,center).contains(unloaded),"Unloaded entry accepted");
        check(!level.hasChunkAt(unloaded),"Lookup forced a distant chunk load");
        BlockPos ordinary=center.offset(4,0,0);level.setBlock(ordinary,Blocks.CHEST.defaultBlockState(),3);
        br.com.residenciasaldeoes.data.EmeraldChestIndex.register(level,ordinary);
        check(!br.com.residenciasaldeoes.data.EmeraldChestIndex.nearby(level,center).contains(ordinary),"Ordinary chest accepted");
        level.removeBlock(fixtures.get(0),false);
        check(!br.com.residenciasaldeoes.data.EmeraldChestIndex.nearby(level,center).contains(fixtures.get(0)),"Removed chest retained");
        EmeraldChestBlockEntity full=(EmeraldChestBlockEntity)level.getBlockEntity(fixtures.get(1));
        for(int slot=0;slot<27;slot++)full.setItem(slot,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),64));
        check(!full.hasFragmentSpace(),"Full chest reports available space");full.removeItem(26,1);check(full.hasFragmentSpace(),"Partially free stack ignored");
        System.out.println("CHEST_LOCATION_RANGE_PASSED distances=5,20,100,500,1000; navigation not asserted for distant fixtures");
        for(BlockPos pos:fixtures){if(level.getBlockEntity(pos) instanceof EmeraldChestBlockEntity chest)chest.clearContent();level.removeBlock(pos,false);}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="index")
    public static void captured_resident_identity_rebinds_without_duplicates(GameTestHelper h){
        var level=h.getLevel();var data=ResidenceData.get(level);BlockPos home=h.absolutePos(new BlockPos(6,2,6));check(data.setSize(data.home(level,home),5),"Could not configure capture ordering fixture");
        Villager original=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,4));
        check(data.link(level,home,original).equals("ok"),"Could not register capture fixture");
        Villager following=h.spawn(EntityType.VILLAGER,new BlockPos(9,2,4));check(data.link(level,home,following).equals("ok"),"Could not register resident following capture fixture");
        check(data.reconcileCapturedResident(level,original),"Registered resident identity was not initialized");
        net.minecraft.nbt.CompoundTag captured=original.getPersistentData().copy();
        Villager duplicateWhileOriginalExists=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,4));duplicateWhileOriginalExists.getPersistentData().merge(captured);
        check(!data.reconcileCapturedResident(level,duplicateWhileOriginalExists),"Duplicate capture copy took a live resident registration");
        original.discard();
        Villager released=h.spawn(EntityType.VILLAGER,new BlockPos(7,2,4));released.getPersistentData().merge(captured);
        ResidenceData reloaded=ResidenceData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        check(reloaded.reconcileCapturedResident(level,released),"Release with a new entity UUID did not restore the resident link");
        check(reloaded.owner(released.getUUID())!=null&&reloaded.count(level.dimension(),home)==2,"Resident record was lost or duplicated after release");
        check(new java.util.ArrayList<>(reloaded.residents(level.dimension(),home)).equals(java.util.List.of(released.getUUID(),following.getUUID())),"Captured resident replacement did not preserve its list position");
        check(reloaded.owner(captured.getUUID("residenciasaldeoes_resident_identity"))==null,"Stable identity leaked into the entity UUID owner index");
        Villager ordinary=h.spawn(EntityType.VILLAGER,new BlockPos(8,2,4));
        check(!reloaded.reconcileCapturedResident(level,ordinary),"Ordinary villager was treated as a resident");
        System.out.println("CAPTURE_IDENTITY_REBIND_PASSED persisted_token=true duplicate_live_entity_rejected=true");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="index")
    public static void resident_order_is_per_home_and_persistent(GameTestHelper h){
        var level=h.getLevel();var data=ResidenceData.get(level);
        BlockPos firstHome=h.absolutePos(new BlockPos(4,2,4)),secondHome=h.absolutePos(new BlockPos(10,2,10));
        var firstConfig=data.home(level,firstHome);check(data.setSize(firstConfig,11),"Could not configure first ordering fixture");
        java.util.List<java.util.UUID> original=new java.util.ArrayList<>();
        for(int i=0;i<5;i++){
            Villager villager=h.spawn(EntityType.VILLAGER,new BlockPos(3+i,2,3));villager.setCustomName(net.minecraft.network.chat.Component.literal("Duplicado"));
            check("ok".equals(data.link(level,firstHome,villager)),"Could not link first-home ordering resident "+i);original.add(villager.getUUID());
        }
        var secondConfig=data.home(level,secondHome);check(data.setSize(secondConfig,7),"Could not configure second ordering fixture");
        java.util.List<java.util.UUID> second=new java.util.ArrayList<>();
        for(int i=0;i<3;i++){
            Villager villager=h.spawn(EntityType.VILLAGER,new BlockPos(8+i,2,8));
            check("ok".equals(data.link(level,secondHome,villager)),"Could not link second-home ordering resident "+i);second.add(villager.getUUID());
        }
        check(data.reorderResident(level.dimension(),firstHome,original.get(4),1),"Last resident could not be moved to second position");
        check(data.reorderResident(level.dimension(),firstHome,original.get(0),4),"First resident could not be moved to the end");
        check(data.reorderResident(level.dimension(),firstHome,original.get(3),0),"Resident could not be moved several positions");
        check(data.reorderResident(level.dimension(),secondHome,second.get(0),2),"Second home's independent order could not be changed");
        java.util.List<java.util.UUID> expected=java.util.List.of(original.get(3),original.get(4),original.get(1),original.get(2),original.get(0));
        java.util.List<java.util.UUID> expectedSecond=java.util.List.of(second.get(1),second.get(2),second.get(0));
        check(new java.util.ArrayList<>(data.residents(level.dimension(),firstHome)).equals(expected),"UUID-based reordering produced the wrong first-home sequence");
        check(new java.util.ArrayList<>(data.residents(level.dimension(),secondHome)).equals(expectedSecond),"Reordering one home changed another home's sequence");
        ResidenceData restored=ResidenceData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        check(new java.util.ArrayList<>(restored.residents(level.dimension(),firstHome)).equals(expected),"Resident order was not persisted through SavedData serialization");
        Villager appended=h.spawn(EntityType.VILLAGER,new BlockPos(9,2,3));appended.setCustomName(net.minecraft.network.chat.Component.literal("Duplicado"));
        check("ok".equals(restored.link(level,firstHome,appended)),"New resident could not be appended");
        java.util.List<java.util.UUID> withAppended=new java.util.ArrayList<>(expected);withAppended.add(appended.getUUID());
        check(new java.util.ArrayList<>(restored.residents(level.dimension(),firstHome)).equals(withAppended),"New resident did not appear at the end of the customized order");
        restored.unlink(level,original.get(1));withAppended.remove(original.get(1));
        check(new java.util.ArrayList<>(restored.residents(level.dimension(),firstHome)).equals(withAppended),"Unlink changed the relative order of remaining residents");
        System.out.println("RESIDENT_ORDER_PERSISTENCE_PASSED uuid_based=true independent_homes=true append_and_unlink=true");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="lid")
    public static void courier_lid_leases_and_player_overlap(GameTestHelper h){
        BlockPos pos=new BlockPos(2,2,2);h.setBlock(pos,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        var chest=(EmeraldChestBlockEntity)h.getBlockEntity(pos);var first=java.util.UUID.randomUUID();var second=java.util.UUID.randomUUID();
        chest.beginCourierDeposit(first);chest.beginCourierDeposit(second);
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        chest.startOpen(player);check(chest.getCombinedOpeners()==3,"Player and two couriers did not share opening");
        chest.stopOpen(player);check(chest.getCombinedOpeners()==2,"Player close cancelled courier opening");
        chest.finishCourierDeposit(first);
        h.runAtTickTime(30,()->{check(chest.getCourierOpeners()==1,"Closing one courier cancelled the other");chest.finishCourierDeposit(second);});
        h.runAtTickTime(70,()->{check(chest.getCombinedOpeners()==0,"Courier left lid permanently open");check(chest.getContainerSize()==27&&chest.isEmpty(),"Opening altered inventory");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=2400,batch="floor")
    public static void floor_anchor_real_dawn_and_nearby_chest(GameTestHelper h){
        floor(h);BlockPos hp=new BlockPos(6,1,6),cp=new BlockPos(12,2,6);
        h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(cp,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);
        EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(cp);
        Villager v=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));v.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());check(data.link(h.getLevel(),h.absolutePos(hp),v).equals("ok"),"Floor home link failed");
        check(anchor.getSize()==3,"Use the default small residence");
        check(h.absolutePos(cp).distSqr(h.absolutePos(hp))>1,"Chest must lie outside the old one-block radius");
        check(!h.getLevel().isEmptyBlock(h.absolutePos(hp).relative(Direction.NORTH)),"Reproduce solid neighbors at anchor Y");
        h.runAtTickTime(30,()->check(invoke("findApproach",new Class[]{net.minecraft.server.level.ServerLevel.class,BlockPos.class,Villager.class},h.getLevel(),h.absolutePos(hp),v)!=null,"Floor anchor has no walkable approach"));
        h.getLevel().setDayTime(18000);int[] paid={0};double[] pickup={0};
        h.runAtTickTime(5,()->h.getLevel().setDayTime(0));
        h.runAtTickTime(190,()->check(anchor.isEmpty(),"Payment happened before the original 200 ticks"));
        h.runAtTickTime(220,()->{paid[0]=anchor.getItem(0).getCount();check(paid[0]>0,"Payment absent after 10 seconds");});
        h.runAtTickTime(495,()->check(!v.getPersistentData().contains(JOB)&&chest.isEmpty(),"Transport started before 15 seconds after payment"));
        h.runAtTickTime(2300,()->{
            int anchorCount=0,chestCount=0,cargoCount=0;
            for(int slot=0;slot<anchor.getContainerSize();slot++)anchorCount+=anchor.getItem(slot).getCount();
            for(int slot=0;slot<chest.getContainerSize();slot++)chestCount+=chest.getItem(slot).getCount();
            if(v.getPersistentData().contains(JOB)){var cargo=v.getPersistentData().getCompound(JOB).getList("cargo",10);for(int i=0;i<cargo.size();i++)cargoCount+=ItemStack.of(cargo.getCompound(i)).getCount();}
            System.out.println("PHYSICAL_TRANSPORT_DIAGNOSTIC paid="+paid[0]+" anchor="+anchorCount+" pending="+anchor.getPendingFragments()+" chest="+chestCount+" cargo="+cargoCount+" stage="+(v.getPersistentData().contains(JOB)?v.getPersistentData().getCompound(JOB).getInt("stage"):-1)+" position="+v.blockPosition()+" openers="+chest.getCombinedOpeners());
        });
        h.succeedWhen(()->{
            if(v.getPersistentData().contains(JOB)&&v.getPersistentData().getCompound(JOB).contains("cargo"))pickup[0]=1;
            check(!chest.isEmpty(),"Waiting for floor-anchor physical delivery");
            check(paid[0]>0&&chest.getItem(0).getCount()==paid[0]&&anchor.isEmpty(),"Payment/transport lost or duplicated items");
            check(v.getX()>h.absolutePos(hp).getX()+2,"Courier did not physically approach the chest");
        });
    }
    @GameTest(template="empty",timeoutTicks=900,batch="missing")
    public static void missing_chest_and_temporary_obstruction(GameTestHelper h){
        floor(h);BlockPos hp=new BlockPos(6,1,6),cp=new BlockPos(10,2,6);
        h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);
        Villager v=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));v.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());data.link(h.getLevel(),h.absolutePos(hp),v);
        var home=data.home(h.getLevel(),h.absolutePos(hp));long id=data.nextProductionDawnEventId();
        anchor.recordDailyProduction(id,19);data.queueCollection(home,id,h.getLevel().getGameTime());
        h.runAtTickTime(40,()->{
            check(anchor.getItem(0).getCount()==19&&!v.getPersistentData().contains(JOB),"No chest consumed fragments");
            h.setBlock(cp,ResidenciasDosAldeoes.EMERALD_CHEST.get());
            for(Direction direction:Direction.Plane.HORIZONTAL)for(int y=0;y<3;y++)h.setBlock(cp.relative(direction).above(y),Blocks.STONE);
        });
        h.runAtTickTime(150,()->{
            check(anchor.getItem(0).getCount()==19&&!v.getPersistentData().contains(JOB),"Blocked path consumed fragments or assigned an unreachable courier");
            check(!home.pendingCollections.isEmpty(),"Unavailable route deleted queued work");
            for(Direction direction:Direction.Plane.HORIZONTAL)for(int y=0;y<3;y++)h.setBlock(cp.relative(direction).above(y),Blocks.AIR);
        });
        h.succeedWhen(()->{
            var chest=(EmeraldChestBlockEntity)h.getBlockEntity(cp);
            check(chest!=null&&!chest.isEmpty(),"Waiting for retry after obstruction removed");
            check(chest.getItem(0).getCount()==19&&anchor.isEmpty(),"Retry lost/duplicated fragments");
        });
    }
    @GameTest(template="empty",timeoutTicks=700,batch="available")
    public static void first_available_registered_courier(GameTestHelper h){
        floor(h);BlockPos hp=new BlockPos(6,2,6),cp=new BlockPos(10,2,6);
        h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(cp,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);
        anchor.setSize(11);
        // Keep the unavailable resident away from the courier's one-block path.
        Villager unavailable=h.spawn(EntityType.VILLAGER,new BlockPos(2,2,3));unavailable.setAge(0);unavailable.setNoAi(true);
        Villager courier=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));courier.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());check(data.link(h.getLevel(),h.absolutePos(hp),unavailable).equals("ok"),"First link failed");check(data.link(h.getLevel(),h.absolutePos(hp),courier).equals("ok"),"Second link failed");
        var home=data.home(h.getLevel(),h.absolutePos(hp));long id=data.nextProductionDawnEventId();
        anchor.recordDailyProduction(id,23);data.queueCollection(home,id,h.getLevel().getGameTime());
        h.succeedWhen(()->{
            check(!unavailable.getPersistentData().contains(JOB),"Unavailable resident assigned");
            if(courier.getPersistentData().contains(JOB)){
                check(courier.getPersistentData().getCompound(JOB).getLong("anchor")==h.absolutePos(hp).asLong(),"Second resident assigned to the wrong residence");
                h.succeed();
            }
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void resident_opacity_is_individual_and_persistent(GameTestHelper h){
        var level=h.getLevel();var data=ResidenceData.get(level);
        Villager translucent=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,3));
        Villager opaque=h.spawn(EntityType.VILLAGER,new BlockPos(9,2,9));
        BlockPos firstHome=h.absolutePos(new BlockPos(2,1,2)),secondHome=h.absolutePos(new BlockPos(10,1,10));
        check("ok".equals(data.link(level,firstHome,translucent)),"first resident should link for opacity persistence test");
        check("ok".equals(data.link(level,secondHome,opaque)),"second resident should link for opacity independence test");
        check(data.residentOpacity(translucent.getUUID())==100,"legacy/default opacity should be 100 percent");
        check(data.residentOpacity(opaque.getUUID())==100,"each resident should default independently to 100 percent");
        data.setResidentOpacity(translucent.getUUID(),37);
        data.setResidentOpacity(opaque.getUUID(),0);
        var restored=ResidenceData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        check(restored.residentOpacity(translucent.getUUID())==37,"first resident opacity should survive SavedData serialization");
        check(restored.residentOpacity(opaque.getUUID())==0,"zero opacity should persist independently");
        data.setResidentOpacity(translucent.getUUID(),150);
        check(data.residentOpacity(translucent.getUUID())==100,"opacity should clamp to 100");
        data.setResidentOpacity(translucent.getUUID(),-4);
        check(data.re…8804 tokens truncated…t.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,1000F);
        long recovered=anchor.getPendingFragments();for(int slot=0;slot<anchor.getContainerSize();slot++)recovered+=anchor.getItem(slot).getCount();
        check(recovered==50,"Resident death failed to recover cargo");
        for(int slot=0;slot<27;slot++)chest.removeItem(slot,64);
        int delivered=0,trips=0;
        while(!anchor.isEmpty()){
            var next=anchor.takeAvailableFragments(15);int trip=0;
            for(ItemStack stack:next){check(stack.getCount()<=15,"Trip exceeded 15 fragments");trip+=stack.getCount();ItemStack remaining=stack.copy();chest.insertFragments(remaining);check(remaining.isEmpty(),"Chest rejected fragments after space was freed");}
            check(trip>0&&trip<=15,"Invalid recovery trip");delivered+=trip;trips++;
        }
        int stored=0;for(int slot=0;slot<27;slot++)stored+=chest.getItem(slot).getCount();
        check(delivered==50&&stored==50&&trips==4,"Sequential trips lost or duplicated fragments");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="outputs")
    public static void courier_trip_limit_across_all_output_slots(GameTestHelper h){
        BlockPos pos=new BlockPos(2,2,2);h.setBlock(pos,ResidenciasDosAldeoes.ANCHOR.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(pos);
        for(int total:new int[]{5,15,16,20,30,31,45}){
            anchor.clearContent();int left=total;
            for(int slot=0;slot<anchor.getContainerSize()&&left>0;slot++){int amount=Math.min(5,left);anchor.setOutputItem(slot,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),amount));left-=amount;}
            check(left==0,"Test setup did not span the output inventory");int delivered=0,trips=0;
            while(!anchor.isEmpty()){
                var cargo=anchor.takeAvailableFragments(15);int trip=0;
                for(ItemStack stack:cargo){check(stack.getCount()<=15,"Cargo stack exceeded trip limit");trip+=stack.getCount();}
                check(trip>0&&trip<=15,"Empty or oversized trip");delivered+=trip;trips++;
            }
            check(delivered==total,"Trip limit lost fragments for total "+total);
            check(trips==(total+14)/15,"Wrong number of trips for total "+total);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="sleep")
    public static void resident_sleep_stops_stale_motion(GameTestHelper h){
        Villager v=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,5));v.setAge(0);
        BlockPos home=new BlockPos(6,2,6);h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        check(ResidenceData.get(h.getLevel()).link(h.getLevel(),h.absolutePos(home),v).equals("ok"),"Link failed");
        v.startSleeping(h.absolutePos(new BlockPos(5,2,5)));v.setDeltaMovement(.25D,0.0D,0.0D);
        v.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                new net.minecraft.world.entity.ai.memory.WalkTarget(h.absolutePos(new BlockPos(10,2,5)),.8F,0));
        VillagerResidenceEvents.stabilizeSleepingResident(v);
        check(v.isSleeping(),"Sleep stabilization woke the resident");
        check(v.getNavigation().isDone(),"Sleep stabilization left navigation active");
        check(v.getDeltaMovement().lengthSqr()<1.0E-8D,"Sleep stabilization left residual velocity");
        check(v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET).isEmpty(),"Stale walk target remained during sleep");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="sleep")
    public static void resident_bed_claims_are_exclusive(GameTestHelper h){
        var level=h.getLevel();var data=ResidenceData.get(level);
        BlockPos home=h.absolutePos(new BlockPos(6,2,6));
        BlockPos bed=h.absolutePos(new BlockPos(5,2,5));
        var owner=java.util.UUID.randomUUID();var intruder=java.util.UUID.randomUUID();
        check(data.reserveBed(owner,level.dimension(),home,bed),"First resident could not reserve bed");
        check(!data.reserveBed(intruder,level.dimension(),home,bed),"Second resident replaced an existing bed claim");
        check(owner.equals(data.bedOwner(level.dimension(),bed)),"Bed ownership changed after a rejected claim");
        check(data.bedClaim(intruder)==null,"Rejected resident received a claim");
        check(data.reserveBed(intruder,level.dimension(),home,bed.east()),"Second resident could not reserve another bed");
        check(owner.equals(data.bedOwner(level.dimension(),bed)),"Separate claim disturbed the original bed");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="sleep")
    public static void stale_unregistered_home_poi_is_not_released_or_claimed(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homeLocal=new BlockPos(6,2,6);h.setBlock(homeLocal,ResidenciasDosAldeoes.ANCHOR.get());
        var resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));var data=ResidenceData.get(level);
        BlockPos home=h.absolutePos(homeLocal);check(data.link(level,home,resident).equals("ok"),"Could not link resident with stale HOME memory");
        BlockPos stale=h.absolutePos(new BlockPos(4,3,4)),candidate=h.absolutePos(new BlockPos(8,3,8));
        check(level.getPoiManager().getType(stale).isEmpty(),"Stale fixture unexpectedly has a POI");
        check(level.getPoiManager().getType(candidate).isEmpty(),"Invalid candidate fixture unexpectedly has a POI");
        resident.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME,
                net.minecraft.core.GlobalPos.of(level.dimension(),stale));
        boolean claimed=(Boolean)invoke("claimResidentialBedPoi",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.class,Villager.class,ResidenceData.Home.class,BlockPos.class},
                level,data,resident,data.owner(resident.getUUID()),candidate);
        check(!claimed,"Unregistered POI was treated as a valid residential bed");
        check(resident.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME).isEmpty(),
                "Stale HOME memory was retained after its loaded position proved to have no POI");
        check(data.bedClaim(resident.getUUID())==null,"Resident was assigned a nonexistent bed");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=800,batch="sleep")
    public static void stale_home_poi_during_negative_y_world_load_recovers_to_valid_bed(GameTestHelper h){
        var level=h.getLevel();BlockPos origin=h.absolutePos(BlockPos.ZERO);
        BlockPos home=new BlockPos(origin.getX()+6,-62,origin.getZ()+6);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlock(new BlockPos(home.getX()+x,-61,home.getZ()+z),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(home.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get().defaultBlockState(),3);
        check(((ResidentialAnchorBlockEntity)level.getBlockEntity(home)).setSize(5),"Could not size negative-Y residence");
        BlockPos bed=new BlockPos(home.getX()+1,-60,home.getZ()-1),foot=bed.relative(Direction.SOUTH);
        var bedHead=Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                .setValue(net.minecraft.world.level.block.BedBlock.FACING,Direction.NORTH).setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED,false);
        level.setBlock(foot,bedHead.setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT),3);
        level.setBlock(bed,bedHead,3);
        check(level.getPoiManager().getType(bed).filter(type->type.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME)).isPresent(),
                "Negative-Y test bed did not register its vanilla HOME POI");
        Villager resident=EntityType.VILLAGER.create(level);check(resident!=null,"Could not create negative-Y resident");
        resident.moveTo(home.getX()+2.5,-60,home.getZ()-.5,0F,0F);resident.setAge(0);level.addFreshEntity(resident);
        var data=ResidenceData.get(level);check(data.link(level,home,resident).equals("ok"),"Could not register negative-Y resident");
        BlockPos staleHome=new BlockPos(bed.getX(),-63,bed.getZ());
        check(level.hasChunkAt(staleHome)&&level.getPoiManager().getType(staleHome).isEmpty(),"Negative-Y stale HOME fixture is not a loaded non-POI position");
        resident.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME,
                net.minecraft.core.GlobalPos.of(level.dimension(),staleHome));
        level.setDayTime(13_001L);
        h.succeedWhen(()->{
            check(resident.isAlive(),"Resident died while stale HOME POI was being recovered");
            check(resident.isSleeping()&&resident.getSleepingPos().orElse(null)!=null&&resident.getSleepingPos().orElseThrow().equals(bed),
                    "Resident did not claim/use the valid negative-Y bed after stale HOME cleanup; homeMemory="
                            +resident.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME).orElse(null));
            check(data.bedClaim(resident.getUUID())!=null&&data.bedClaim(resident.getUUID()).bed().equals(bed),
                    "Valid bed reservation was not saved after stale POI recovery");
        });
    }
    @GameTest(template="empty",timeoutTicks=600,batch="sleep")
    public static void three_residents_reach_and_use_three_distinct_residential_beds(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos home=new BlockPos(6,2,6);
        h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(home);
        check(anchor.setSize(5),"Could not expand residence for two sleepers");
        for(int x=4;x<=8;x++)for(int z=4;z<=8;z++)if(x!=6||z!=6)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        var headState=Blocks.RED_BED.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                .setValue(net.minecraft.world.level.block.BedBlock.FACING,Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED,false);
        BlockPos firstBed=h.absolutePos(new BlockPos(4,3,5)),secondBed=h.absolutePos(new BlockPos(5,3,5)),thirdBed=h.absolutePos(new BlockPos(7,3,5));
        for(BlockPos bed:new BlockPos[]{firstBed,secondBed,thirdBed}){
            BlockPos foot=bed.relative(Direction.SOUTH);
            level.setBlock(foot,headState.setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT),2);
            level.setBlock(bed,headState,2);
        }
        Villager first=h.spawn(EntityType.VILLAGER,new BlockPos(5,3,7));first.setAge(0);
        Villager second=h.spawn(EntityType.VILLAGER,new BlockPos(7,3,7));second.setAge(0);
        Villager third=h.spawn(EntityType.VILLAGER,new BlockPos(8,3,7));third.setAge(0);
        var data=ResidenceData.get(level);
        check(data.link(level,h.absolutePos(home),first).equals("ok"),"First resident link failed");
        check(data.link(level,h.absolutePos(home),second).equals("ok"),"Second resident link failed");
        check(data.link(level,h.absolutePos(home),third).equals("ok"),"Third resident link failed");
        var firstHome=data.owner(first.getUUID());
        check(firstHome!=null&&data.owner(second.getUUID())!=null,"Resident registration vanished before sleep checks");
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set night");
        h.succeedWhen(()->{
            check(first.isSleeping()&&second.isSleeping()&&third.isSleeping(),"Three residents did not reach and use residential beds; time="+level.getDayTime()+" first="+first.blockPosition()+" claim="+data.bedClaim(first.getUUID())+" second="+second.blockPosition()+" claim="+data.bedClaim(second.getUUID())+" third="+third.blockPosition()+" claim="+data.bedClaim(third.getUUID()));
            check(data.bedClaim(first.getUUID())!=null&&data.bedClaim(second.getUUID())!=null&&data.bedClaim(third.getUUID())!=null,"Sleeping residents have no persisted bed claims");
            BlockPos firstClaim=data.bedClaim(first.getUUID()).bed(),secondClaim=data.bedClaim(second.getUUID()).bed(),thirdClaim=data.bedClaim(third.getUUID()).bed();
            check(!firstClaim.equals(secondClaim)&&!firstClaim.equals(thirdClaim)&&!secondClaim.equals(thirdClaim),"Residents did not claim three distinct beds");
            check(first.getSleepingPos().orElse(null)!=null&&second.getSleepingPos().orElse(null)!=null&&third.getSleepingPos().orElse(null)!=null,
                    "Resident sleep positions were not recorded");
        });
    }
    @GameTest(template="empty",timeoutTicks=100,batch="sleep")
    public static void steve_model_preference_round_trips_in_residence_saved_data(GameTestHelper h){
        var level=h.getLevel();BlockPos home=new BlockPos(6,2,6);h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));var data=ResidenceData.get(level);
        check(data.link(level,h.absolutePos(home),resident).equals("ok"),"Could not link model persistence resident");
        check(data.residentModel(resident.getUUID())==ResidentModelType.VILLAGER,"Legacy/default model was not traditional");
        data.setResidentModel(resident.getUUID(),ResidentModelType.STEVE);
        ResidenceData restored=ResidenceData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        check(restored.residentModel(resident.getUUID())==ResidentModelType.STEVE,"Steve model selection did not survive SavedData serialization");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="sleep")
    public static void resident_death_notice_uses_alias_and_unregisters_once(GameTestHelper h){
        var level=h.getLevel();BlockPos home=new BlockPos(6,2,6);h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));resident.setAge(0);
        var data=ResidenceData.get(level);check(data.link(level,h.absolutePos(home),resident).equals("ok"),"Resident link failed");
        data.renameResident(resident.getUUID(),"Marta");
        String message=((net.minecraft.network.chat.Component)invoke("describeResidentDeath",
                new Class<?>[]{String.class,net.minecraft.world.damagesource.DamageSource.class},"Marta",net.minecraft.world.damagesource.DamageSource.FALL)).getString();
        check(message.equals("Marta morreu devido a uma queda."),"Unexpected fall notice: "+message);
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);var skeleton=net.minecraft.world.entity.EntityType.SKELETON.create(level);
        check(zombie!=null&&skeleton!=null,"Could not create death cause fixtures");
        String zombieMessage=((net.minecraft.network.chat.Component)invoke("describeResidentDeath",
                new Class<?>[]{String.class,net.minecraft.world.damagesource.DamageSource.class},"Marta",net.minecraft.world.damagesource.DamageSource.mobAttack(zombie))).getString();
        String skeletonMessage=((net.minecraft.network.chat.Component)invoke("describeResidentDeath",
                new Class<?>[]{String.class,net.minecraft.world.damagesource.DamageSource.class},"Marta",net.minecraft.world.damagesource.DamageSource.mobAttack(skeleton))).getString();
        check(zombieMessage.equals("Marta foi morto por um zumbi."),"Unexpected zombie notice: "+zombieMessage);
        check(skeletonMessage.equals("Marta foi morto por um esqueleto."),"Unexpected skeleton notice: "+skeletonMessage);
        String fireMessage=((net.minecraft.network.chat.Component)invoke("describeResidentDeath",
                new Class<?>[]{String.class,net.minecraft.world.damagesource.DamageSource.class},"Marta",net.minecraft.world.damagesource.DamageSource.ON_FIRE)).getString();
        String drownMessage=((net.minecraft.network.chat.Component)invoke("describeResidentDeath",
                new Class<?>[]{String.class,net.minecraft.world.damagesource.DamageSource.class},"Marta",net.minecraft.world.damagesource.DamageSource.DROWN)).getString();
        check(fireMessage.equals("Marta morreu queimado.")&&drownMessage.equals("Marta morreu afogado."),"Fire/drowning cause classification failed");
        VillagerResidenceEvents events=new VillagerResidenceEvents();
        var death=new net.minecraftforge.event.entity.living.LivingDeathEvent(resident,net.minecraft.world.damagesource.DamageSource.FALL);
        events.onDeath(death);events.onDeath(death);
        check(data.owner(resident.getUUID())==null,"Dead resident remained registered");
        check(resident.getPersistentData().getBoolean("residenciasaldeoes_death_notice_sent"),"Death notice was not guarded against duplicates");
        Villager ordinary=h.spawn(EntityType.VILLAGER,new BlockPos(8,2,8));ordinary.setAge(0);
        events.onDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(ordinary,net.minecraft.world.damagesource.DamageSource.GENERIC));
        check(!ordinary.getPersistentData().contains("residenciasaldeoes_death_notice_sent"),"Ordinary villager received resident notification state");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="sleep")
    public static void resident_witch_conversion_notifies_and_unregisters(GameTestHelper h){
        var level=h.getLevel();BlockPos home=new BlockPos(6,2,6);h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));resident.setAge(0);
        var data=ResidenceData.get(level);check(data.link(level,h.absolutePos(home),resident).equals("ok"),"Resident link failed");
        data.renameResident(resident.getUUID(),"Marta");
        var witch=net.minecraft.world.entity.EntityType.WITCH.create(level);check(witch!=null,"Could not create witch conversion outcome");
        new VillagerResidenceEvents().onVillagerConversion(new net.minecraftforge.event.entity.living.LivingConversionEvent.Post(resident,witch));
        check(data.owner(resident.getUUID())==null,"Transformed villager remained registered");
        check(data.bedClaim(resident.getUUID())==null,"Transformed resident retained a bed reservation");
        check(resident.getPersistentData().getBoolean("residenciasaldeoes_witch_notice_sent"),"Witch conversion notice was not emitted");
        check(!resident.getPersistentData().getBoolean("residenciasaldeoes_death_notice_sent"),"Witch conversion was marked as a death");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400,batch="dawn")
    public static void command_dawn_payment(GameTestHelper h){
        floor(h);BlockPos hp=new BlockPos(6,2,6);h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);
        Villager v=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));v.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());data.link(h.getLevel(),h.absolutePos(hp),v);
        h.getLevel().setDayTime(18000);
        h.runAtTickTime(5,()->h.getLevel().setDayTime(0));
        h.succeedWhen(()->check(!anchor.isEmpty(),"Waiting for command dawn payment"));
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void adult_drops_and_baby_exclusion(GameTestHelper h){
        Villager adult=h.spawn(EntityType.VILLAGER,new BlockPos(2,2,2));adult.setAge(0);
        Villager baby=h.spawn(EntityType.VILLAGER,new BlockPos(14,2,14));baby.setAge(-24000);
        VillagerResidenceEvents events=new VillagerResidenceEvents();int twos=0;
        for(int i=0;i<10000;i++){
            adult.getPersistentData().remove("residenciasaldeoes_fragment_drop_processed");
            java.util.Collection<net.minecraft.world.entity.item.ItemEntity> drops=new java.util.ArrayList<>();
            var event=new net.minecraftforge.event.entity.living.LivingDropsEvent(adult,net.minecraft.world.damagesource.DamageSource.GENERIC,drops,5,false);
            events.onVillagerDrops(event);events.onVillagerDrops(event);
            check(drops.size()==1,"Duplicate drop");int count=drops.iterator().next().getItem().getCount();
            check(count==1||count==2,"Invalid count");if(count==2)twos++;
        }
        System.out.println("DROP_SAMPLE size=10000 two_fragments="+twos+" one_fragments="+(10000-twos));
        check(twos>850&&twos<1150,"Distribution outside tolerance: "+twos);
        java.util.Collection<net.minecraft.world.entity.item.ItemEntity> drops=new java.util.ArrayList<>();
        events.onVillagerDrops(new net.minecraftforge.event.entity.living.LivingDropsEvent(baby,net.minecraft.world.damagesource.DamageSource.GENERIC,drops,0,false));
        check(drops.isEmpty(),"Baby received fragments");
        adult.getPersistentData().remove("residenciasaldeoes_fragment_drop_processed");
        adult.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,1000F);
        var actualDrops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(adult.blockPosition()).inflate(2),item->item.getItem().is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()));
        check(actualDrops.size()==1,"Actual environmental death did not produce one exclusive fragment stack");
        check(actualDrops.get(0).getItem().getCount()<=2,"Actual death exceeded two fragments");
        h.setBlock(new BlockPos(12,2,8),ResidenciasDosAldeoes.ANCHOR.get());
        ((ResidentialAnchorBlockEntity)h.getBlockEntity(new BlockPos(12,2,8))).setSize(11);
        var fake=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var creature=h.spawn(EntityType.SKELETON,new BlockPos(13,2,5));
        for(boolean resident:new boolean[]{false,true})for(int cause=0;cause<3;cause++){
            if(!resident&&cause==0)continue; // Common environmental death was checked above.
            BlockPos pos=new BlockPos(resident?8:2,2,2+cause*4);
            Villager target=h.spawn(EntityType.VILLAGER,pos);target.setAge(0);
            if(resident)check(ResidenceData.get(h.getLevel()).link(h.getLevel(),h.absolutePos(new BlockPos(12,2,8)),target).equals("ok"),"Resident death fixture failed to link");
            var source=cause==0?net.minecraft.world.damagesource.DamageSource.GENERIC:
                    cause==1?net.minecraft.world.damagesource.DamageSource.playerAttack(fake):net.minecraft.world.damagesource.DamageSource.mobAttack(creature);
            target.hurt(source,1000F);
            var spawned=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(2),item->item.getItem().is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()));
            check(spawned.size()==1&&spawned.get(0).getItem().getCount()>=1&&spawned.get(0).getItem().getCount()<=2,
                    "Invalid actual death drops: resident="+resident+" cause="+cause);
        }
        baby.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,1000F);
        var babyDrops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(baby.blockPosition()).inflate(2),item->item.getItem().is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()));
        check(babyDrops.isEmpty(),"Actual baby death dropped fragments");
        h.succeed();
    }
}
