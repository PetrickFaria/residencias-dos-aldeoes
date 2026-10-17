package br.com.residenciasaldeoes.test;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.VillagerResidenceEvents;
import br.com.residenciasaldeoes.block.*;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.data.ResidentSpawnRules;
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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.lang.reflect.Method;
import java.util.List;
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
        // Weather is shared by the whole GameTest server world, not by each
        // template. Isolate rain fixtures so later bed tests run under clear skies.
        level.setWeatherParameters(0,0,false,false);
        level.setRainLevel(0.0F);
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
    @BeforeBatch(batch="rain-sleep") public static void before_rain_sleep(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="rain-night-door") public static void before_rain_night_door(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
    @BeforeBatch(batch="negative-y-sleep") public static void before_negative_y_sleep(net.minecraft.server.level.ServerLevel level){clearFixtureWorld(level);}
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
        check(data.residentOpacity(translucent.getUUID())==0,"opacity should clamp to zero");
        data.unlink(level,translucent.getUUID());data.unlink(level,opaque.getUUID());
        translucent.discard();opaque.discard();
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void wardrobe_package_accepts_1024_skin_atlas(GameTestHelper h){
        try{
            var image=new java.awt.image.BufferedImage(1024,1024,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream png=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",png);
            ByteArrayOutputStream archive=new ByteArrayOutputStream();
            try(ZipOutputStream zip=new ZipOutputStream(archive)){
                zip.putNextEntry(new ZipEntry("manifest.json"));zip.write("{\"schema\":1,\"base_texture\":\"textures/base.png\"}".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
                zip.putNextEntry(new ZipEntry("textures/base.png"));zip.write(png.toByteArray());zip.closeEntry();
            }
            check(WardrobePackage.parse(archive.toByteArray()).baseTexture().equals("textures/base.png"),"1024x1024 skin atlas should pass wardrobe validation");
        }catch(Exception invalid){throw new net.minecraft.gametest.framework.GameTestAssertException("1024x1024 skin atlas was rejected: "+invalid.getMessage());}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ten_output_slots_and_legacy_nbt(GameTestHelper h){
        BlockPos pos=new BlockPos(2,2,2);
        h.setBlock(pos,ResidenciasDosAldeoes.ANCHOR.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(pos);
        check(anchor.getContainerSize()==10,"Anchor must expose ten slots");
        var legacy=net.minecraft.core.NonNullList.withSize(5,ItemStack.EMPTY);
        for(int i=0;i<5;i++)legacy.set(i,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),i+10));
        net.minecraft.nbt.CompoundTag old=new net.minecraft.nbt.CompoundTag();
        net.minecraft.world.ContainerHelper.saveAllItems(old,legacy);
        anchor.setOutputItem(9,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),33));
        anchor.load(old);
        for(int i=0;i<5;i++)check(anchor.getItem(i).getCount()==i+10,"Legacy slot moved/lost/duplicated: "+i);
        for(int i=5;i<10;i++)check(anchor.getItem(i).isEmpty(),"New legacy slot not empty: "+i);
        check(!anchor.setOutputItem(9,new ItemStack(Items.DIRT,5)),"Output accepted invalid item");
        anchor.clearContent();
        anchor.setOutputItem(9,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),60));
        long event=ResidenceData.get(h.getLevel()).nextProductionDawnEventId();
        anchor.recordDailyProduction(event,10);
        check(anchor.getItem(9).getCount()==64&&anchor.getItem(0).getCount()==6,"Existing pile not filled before empty slot");
        anchor.clearContent();
        anchor.recordDailyProduction(ResidenceData.get(h.getLevel()).nextProductionDawnEventId(),700);
        for(int i=0;i<10;i++)check(anchor.getItem(i).getCount()==64,"Capacity did not include slot "+i);
        check(anchor.getPendingFragments()==60,"Overflow lost");
        check(anchor.getUpdateTag().getList("Items",10).size()==10,"Client update omitted output items");
        var saved=anchor.saveWithFullMetadata();
        ResidentialAnchorBlockEntity reloaded=new ResidentialAnchorBlockEntity(h.absolutePos(pos),anchor.getBlockState());
        reloaded.load(saved);
        h.getLevel().removeBlockEntity(h.absolutePos(pos));h.getLevel().setBlockEntity(reloaded);
        for(int i=0;i<10;i++)check(reloaded.getItem(i).getCount()==64,"Reload lost slot "+i);
        check(reloaded.getPendingFragments()==60,"Reload lost overflow");
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.getInventory().clearContent();
        var menu=new br.com.residenciasaldeoes.menu.AnchorMenu(1,player.getInventory(),h.absolutePos(pos));
        check(menu.slots.size()==10,"Menu does not expose ten slots");
        for(int i=0;i<10;i++){
            var slot=menu.slots.get(i);
            check(!slot.mayPlace(new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())),"Manual fragment insertion allowed");
            check(!slot.mayPlace(new ItemStack(Items.DIRT)),"Manual dirt insertion allowed");
            check(slot.y==210&&slot.x==32+i*21,"Slots not centered in a single bottom row");
        }
        check(menu.quickMoveStack(player,9).getCount()==64,"Cannot withdraw tenth slot with shift-click");
        check(player.getInventory().countItem(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())==64,"Withdrawal lost items");
        check(reloaded.getItem(9).getCount()==60&&reloaded.getPendingFragments()==0,"Overflow not replenished");
        menu.clicked(9,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        check(menu.getCarried().getCount()==60&&reloaded.getItem(9).isEmpty(),"Normal withdrawal failed");
        menu.clicked(9,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        check(menu.getCarried().getCount()==60&&reloaded.getItem(9).isEmpty(),"Manual insertion consumed items");
        int cargo=0;
        while(!reloaded.isEmpty()){
            var trip=reloaded.takeAvailableFragments(15);check(!trip.isEmpty(),"Transport cannot read remaining slots");
            for(ItemStack stack:trip)cargo+=stack.getCount();
        }
        check(cargo+64+menu.getCarried().getCount()==700,"Collection lost or duplicated fragments");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chest_filter_and_capacity(GameTestHelper h){
        h.setBlock(new BlockPos(2,2,2),ResidenciasDosAldeoes.EMERALD_CHEST.get());
        EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(new BlockPos(2,2,2));
        IItemHandler handler=chest.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new AssertionError("Missing item handler"));
        ItemStack dirt=new ItemStack(Items.DIRT,17);
        check(handler.insertItem(0,dirt,false).getCount()==17,"Invalid items consumed");
        check(dirt.getCount()==17&&chest.isEmpty(),"Invalid insertion mutated storage/source");
        for(int i=0;i<27;i++)chest.setItem(i,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),64));
        ItemStack remainder=new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),12);
        check(chest.insertFragments(remainder)==0&&remainder.getCount()==12,"Full chest lost fragments");
        chest.removeItem(0,64); // An empty destination exposes invalid-item deletion regressions.
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.getInventory().setItem(9,new ItemStack(Items.DIRT,17));
        var menu=new br.com.residenciasaldeoes.menu.EmeraldChestMenu(1,player.getInventory(),chest);
        check(menu.quickMoveStack(player,27).isEmpty(),"Invalid shift-click accepted");
        check(player.getInventory().getItem(9).getCount()==17,"Invalid shift-click deleted player's items");
        check(chest.getItem(0).isEmpty(),"Invalid item entered an empty chest slot");
        chest.setItem(0,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),64));
        net.minecraft.nbt.CompoundTag saved=chest.saveWithFullMetadata();
        chest.load(saved);check(chest.getItem(26).getCount()==64,"NBT persistence failed");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void door_states(GameTestHelper h){
        floor(h);
        int x=2;
        for(Direction facing:Direction.Plane.HORIZONTAL){
            BlockPos p=new BlockPos(x++,2,2);
            for(DoorHingeSide hinge:DoorHingeSide.values())for(boolean open:new boolean[]{false,true}){
                var state=ResidenciasDosAldeoes.REINFORCED_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING,facing).setValue(DoorBlock.HINGE,hinge).setValue(DoorBlock.OPEN,open);
                h.setBlock(p,state);h.setBlock(p.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
                var reference=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,facing).setValue(DoorBlock.HINGE,hinge).setValue(DoorBlock.OPEN,open);
                check(state.getCollisionShape(h.getLevel(),h.absolutePos(p)).bounds().equals(reference.getCollisionShape(h.getLevel(),h.absolutePos(p)).bounds()),"Door collision changed");
                BlockPos absolute=h.absolutePos(p);
                var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
                state.getBlock().use(state,h.getLevel(),absolute,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),Direction.UP,absolute,false));
                check(h.getLevel().getBlockState(absolute).getValue(DoorBlock.OPEN)==!open,"Manual door toggle failed");
                check(h.getLevel().getBlockState(absolute.above()).getValue(DoorBlock.OPEN)==!open,"Upper half did not toggle");
            }
        }
        h.succeed();
    }
    private static void placeOpenDoor(GameTestHelper h,BlockPos pos,net.minecraft.world.level.block.Block block){
        placeDoor(h,pos,block,true);
    }
    private static void placeDoor(GameTestHelper h,BlockPos pos,net.minecraft.world.level.block.Block block,boolean open){
        var lower=block.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN,open);
        var upper=block.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER).setValue(DoorBlock.OPEN,open);
        h.setBlock(pos,lower);h.setBlock(pos.above(),upper);
    }
    @GameTest(template="empty",timeoutTicks=1000,batch="courier-door")
    public static void courier_opens_only_a_residential_door_on_its_active_path(GameTestHelper h){
        floor(h);var level=h.getLevel();level.setDayTime(13_001L);level.setWeatherParameters(0,12_000,true,false);level.setRainLevel(1.0F);
        BlockPos homeLocal=new BlockPos(6,2,8);h.setBlock(homeLocal,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(homeLocal)).setSize(5),"Could not expand courier-door residence");
        Villager courier=h.spawn(EntityType.VILLAGER,new BlockPos(6,2,6));
        ResidenceData data=ResidenceData.get(level);BlockPos home=h.absolutePos(homeLocal);
        check("ok".equals(data.link(level,home,courier)),"Could not link courier-door resident");
        BlockPos doorLocal=new BlockPos(7,2,6),unrelatedLocal=new BlockPos(7,2,9);
        var courierLower=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST)
                .setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN,false);
        var courierUpper=courierLower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER);
        h.setBlock(doorLocal,courierLower);h.setBlock(doorLocal.above(),courierUpper);
        placeDoor(h,unrelatedLocal,Blocks.SPRUCE_DOOR,false);
        for(int x=6;x<=8;x++)for(int z:new int[]{5,7})for(int y=2;y<=3;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
        BlockPos door=h.absolutePos(doorLocal),destination=h.absolutePos(new BlockPos(9,2,6));
        BlockPos chest=h.absolutePos(new BlockPos(10,2,6));h.setBlock(new BlockPos(10,2,6),ResidenciasDosAldeoes.EMERALD_CHEST.get());
        var job=new net.minecraft.nbt.CompoundTag();job.putLong("anchor",home.asLong());job.putLong("chest",chest.asLong());
        job.putInt("stage",1);job.putLong("chest_approach",destination.asLong());job.putLong("home_approach",h.absolutePos(new BlockPos(6,2,6)).asLong());job.putLong("started",level.getGameTime());
        courier.getPersistentData().put(JOB,job);
        check(VillagerResidenceEvents.shouldBlockResidentDoorOpening(level,courier,door),"Night-rain closure protection was not active before the scoped courier exception");
        // Start the real bounded transfer tick. A pathfinder cannot return a complete
        // path through a closed door, so this test exercises the courier's nearby
        // residential-door exception and then lets vanilla navigation replan.
        invoke("tickEmeraldTransfer",new Class[]{net.minecraft.server.level.ServerLevel.class,Villager.class},level,courier);
        check(level.getBlockState(door).getValue(DoorBlock.OPEN),"Courier's active route could not open the protected residential door");
        check(!level.getBlockState(h.absolutePos(unrelatedLocal)).getValue(DoorBlock.OPEN),"Courier exception opened a door outside the active route");
        check((Boolean)invoke("transferJobListsDoor",new Class[]{net.minecraft.nbt.CompoundTag.class,BlockPos.class},
                courier.getPersistentData().getCompound(JOB),door),"Opened residential door was not tracked for post-passage closure");
        check(VillagerResidenceEvents.shouldBlockResidentDoorOpening(level,courier,door),"Scoped passage exception leaked beyond the setOpen call");
        h.succeedWhen(()->{
            check(courier.blockPosition().getX()>=door.getX()+1,"Courier did not physically cross the residential doorway");
            check(!courier.getPersistentData().contains(JOB),"Courier did not finish the bounded test route");
            check(!level.getBlockState(door).getValue(DoorBlock.OPEN),"Residential door did not close after the courier passed through");
            check(!level.getBlockState(h.absolutePos(unrelatedLocal)).getValue(DoorBlock.OPEN),"Door outside the route changed during passage");
        });
    }
    @GameTest(template="empty",timeoutTicks=900,batch="physical")
    public static void first_resident_physically_closes_loaded_wooden_doors_in_bounded_area(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,9);
        h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(homePos)).setSize(5),"Could not expand residence for two residents");
        Villager first=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,7));first.setAge(0);
        Villager second=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,10));second.setAge(0);second.setNoAi(true);
        ResidenceData data=ResidenceData.get(level);BlockPos absoluteHome=h.absolutePos(homePos);
        check("ok".equals(data.link(level,absoluteHome,first)),"First resident registration failed");
        check("ok".equals(data.link(level,absoluteHome,second)),"Second resident registration failed");

        BlockPos inside=new BlockPos(7,2,7),oneBeyond=new BlockPos(9,2,8),twoBeyond=new BlockPos(10,2,7),iron=new BlockPos(6,2,8);
        placeOpenDoor(h,inside,Blocks.OAK_DOOR);placeOpenDoor(h,oneBeyond,Blocks.SPRUCE_DOOR);
        placeOpenDoor(h,twoBeyond,Blocks.OAK_DOOR);placeOpenDoor(h,iron,Blocks.IRON_DOOR);
        level.setDayTime(13_001L);
        long uniqueNight=100_000L+level.getGameTime();
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,uniqueNight);
        check(first.getPersistentData().contains("residenciasaldeoes_night_door_task"),"The first resident did not receive the task");
        check(!second.getPersistentData().contains("residenciasaldeoes_night_door_task"),"A later resident received the task");
        BlockPos doorApproach=(BlockPos)invoke("findApproach",new Class[]{net.minecraft.server.level.ServerLevel.class,BlockPos.class,Villager.class},level,h.absolutePos(inside),first);
        net.minecraft.world.level.pathfinder.Path doorPath=doorApproach==null?null:first.getNavigation().createPath(doorApproach,0);
        System.out.println("NIGHTDOOR-DIAG initial path="+(doorPath==null?"null":"nodes="+doorPath.getNodeCount()+" reach="+doorPath.canReach()+" target="+doorPath.getTarget())+" floor="+level.getBlockState(h.absolutePos(new BlockPos(4,1,6)))+" villager="+first.position());
        h.runAtTickTime(100,()->System.out.println("NIGHTDOOR-DIAG tick100 alive="+first.isAlive()+" sleep="+first.isSleeping()+" owner="+(data.owner(first.getUUID())!=null)+" pos="+first.position()+" below="+level.getBlockState(first.blockPosition().below())+" task="+first.getPersistentData().getCompound("residenciasaldeoes_night_door_task")+" navDone="+first.getNavigation().isDone()));
        h.runAtTickTime(500,()->{
            check(!level.getBlockState(h.absolutePos(inside)).getValue(DoorBlock.OPEN),"In-bounds wooden door stayed open; villager="+first.position()+" navDone="+first.getNavigation().isDone()+" task="+first.getPersistentData().getCompound("residenciasaldeoes_night_door_task"));
            check(!level.getBlockState(h.absolutePos(oneBeyond)).getValue(DoorBlock.OPEN),"One-block-out wooden door stayed open");
            check(level.getBlockState(h.absolutePos(twoBeyond)).getValue(DoorBlock.OPEN),"Door two blocks outside the boundary was changed");
            check(level.getBlockState(h.absolutePos(iron)).getValue(DoorBlock.OPEN),"Iron door was manipulated");
            check(!first.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Completed door task remained stuck");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100,batch="doors")
    public static void open_door_task_uses_awake_resident_before_waking_sleeping_leader(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(homePos)).setSize(5),"Could not configure home");
        Villager leader=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));Villager awake=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));
        Villager nearestDoor=h.spawn(EntityType.VILLAGER,new BlockPos(8,2,6));
        ResidenceData data=ResidenceData.get(level);BlockPos absoluteHome=h.absolutePos(homePos);
        check(data.link(level,absoluteHome,leader).equals("ok")&&data.link(level,absoluteHome,awake).equals("ok")
                &&data.link(level,absoluteHome,nearestDoor).equals("ok"),"Could not link residents");
        BlockPos bed=h.absolutePos(new BlockPos(4,2,8));h.setBlock(bed,Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        leader.startSleeping(bed);check(leader.isSleeping(),"Leader did not enter the fixture sleeping state");
        placeOpenDoor(h,new BlockPos(7,2,6),Blocks.OAK_DOOR);level.setDayTime(13_001L);
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,200_000L+level.getGameTime());
        check(leader.isSleeping(),"Sleeping registration leader was woken despite another awake resident");
        check(!leader.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Sleeping leader received the door route");
        check(awake.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Awake resident closest to the first resident did not receive the door route");
        check(!nearestDoor.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Door proximity incorrectly overrode the required resident priority");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="doors")
    public static void unlink_sleeping_resident_restores_standing_pose_and_cancels_route(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        Villager villager=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));ResidenceData data=ResidenceData.get(level);BlockPos absoluteHome=h.absolutePos(homePos);
        check(data.link(level,absoluteHome,villager).equals("ok"),"Could not link fixture resident");
        BlockPos bed=h.absolutePos(new BlockPos(5,2,7));h.setBlock(bed,Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        check(data.reserveBed(villager.getUUID(),level.dimension(),absoluteHome,bed),"Could not reserve fixture bed");
        villager.startSleeping(bed);check(villager.isSleeping(),"Resident did not enter the fixture sleeping state");
        villager.getPersistentData().putBoolean("residenciasaldeoes_home_return_active",true);
        villager.getPersistentData().put("residenciasaldeoes_night_door_task",new net.minecraft.nbt.CompoundTag());
        data.unlink(level,villager.getUUID());
        check(!villager.isSleeping(),"Unlinked resident remained in the sleeping pose");
        check(!villager.getPersistentData().contains("residenciasaldeoes_home_return_active")
                &&!villager.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Residential navigation state survived unlink");
        check(data.owner(villager.getUUID())==null&&data.bedClaim(villager.getUUID())==null,"Unlink retained residential registration or bed claim");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=1000,batch="doors")
    public static void night_door_task_wakes_leader_then_returns_to_same_bed(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(homePos)).setSize(5),"Could not expand sleeping residence");
        for(int x=4;x<=8;x++)for(int z=4;z<=8;z++)if(x!=6||z!=6)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        Villager leader=h.spawn(EntityType.VILLAGER,new BlockPos(5,3,7));ResidenceData data=ResidenceData.get(level);BlockPos absoluteHome=h.absolutePos(homePos);
        check(data.link(level,absoluteHome,leader).equals("ok"),"Could not link sleeping leader");
        BlockPos bedLocal=new BlockPos(5,3,5),bed=h.absolutePos(bedLocal);var bedHead=Blocks.WHITE_BED.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                .setValue(net.minecraft.world.level.block.BedBlock.FACING,Direction.NORTH);
        h.setBlock(bedLocal.relative(Direction.SOUTH),bedHead.setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT));h.setBlock(bedLocal,bedHead);
        ResidenceData.Home residentHome=data.owner(leader.getUUID());BlockPos bedFoot=bed.relative(Direction.SOUTH);
        boolean bedIsInside=(Boolean)invoke("bedInsideHome",new Class[]{net.minecraft.server.level.ServerLevel.class,BlockPos.class,ResidenceData.Home.class},level,bed,residentHome);
        check(bedIsInside,"Fixture bed is outside the registered residential volume; home="+residentHome.pos+" size="+residentHome.size+" head="+bed+" headState="+level.getBlockState(bed)+" foot="+bedFoot+" footState="+level.getBlockState(bedFoot)+" chunk="+level.hasChunkAt(bed));
        check(data.reserveBed(leader.getUUID(),level.dimension(),absoluteHome,bed),"Could not reserve leader's current bed");
        leader.startSleeping(bed);placeOpenDoor(h,new BlockPos(7,3,6),Blocks.OAK_DOOR);level.setDayTime(13_001L);
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,300_000L+level.getGameTime());
        check(!leader.isSleeping(),"Sleeping leader was not woken when no other resident was available");
        check(leader.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Woken leader did not receive the bounded door task");
        check(data.bedClaim(leader.getUUID())!=null&&data.bedClaim(leader.getUUID()).bed().equals(bed),"The original bed claim was not preserved while waking the leader; sleeping="+leader.isSleeping()+" position="+leader.getSleepingPos()+" owner="+data.owner(leader.getUUID())+" state="+level.getBlockState(bed));
        h.succeedWhen(()->{
            check(!level.getBlockState(h.absolutePos(new BlockPos(7,3,6))).getValue(DoorBlock.OPEN),"Resident did not physically close the nighttime door");
            check(leader.isSleeping()&&leader.getSleepingPos().orElse(null)!=null&&leader.getSleepingPos().orElseThrow().equals(bed),"Resident did not return to its original bed after closing the door");
        });
    }
    @GameTest(template="empty",timeoutTicks=900,batch="doors")
    public static void night_door_task_resumes_inside_wandering_without_bed(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos home=new BlockPos(6,2,6);h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(home)).setSize(5),"Could not configure bedless residence");
        // The home bounds start one block above the anchor. Give the resident a real,
        // walkable interior floor at that height instead of testing from below it.
        for(int x=4;x<=8;x++)for(int z=4;z<=8;z++)if(x!=6||z!=6)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,3,6));var data=ResidenceData.get(level);
        check(data.link(level,h.absolutePos(home),resident).equals("ok"),"Could not link bedless door resident");
        placeOpenDoor(h,new BlockPos(7,3,6),Blocks.OAK_DOOR);level.setDayTime(13_001L);
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,350_000L+level.getGameTime());
        h.succeedWhen(()->{
            BlockPos door=h.absolutePos(new BlockPos(7,3,6));
            check(!level.getBlockState(door).getValue(DoorBlock.OPEN),"Resident did not close the door");
            check(!resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Door route remained active after closing");
            var target=resident.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET).orElse(null);
            check(target!=null,"Bedless resident did not receive an interior stroll after the route ended");
            check((Boolean)invoke("insideResidence",new Class[]{BlockPos.class,ResidenceData.Home.class},target.getTarget().currentBlockPosition(),data.owner(resident.getUUID())),
                    "Post-door stroll target escaped the residential bounds");
        });
    }
    @GameTest(template="empty",timeoutTicks=950,batch="doors")
    public static void bedless_night_stroll_does_not_reopen_completed_door(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos home=new BlockPos(6,2,6);h.setBlock(home,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(home)).setSize(5),"Could not configure bedless residence");
        for(int x=4;x<=8;x++)for(int z=4;z<=8;z++)if(x!=6||z!=6)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,3,6));ResidenceData data=ResidenceData.get(level);
        check(data.link(level,h.absolutePos(home),resident).equals("ok"),"Could not link bedless resident");
        BlockPos doorLocal=new BlockPos(7,3,6);placeOpenDoor(h,doorLocal,Blocks.OAK_DOOR);level.setDayTime(13_001L);
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,450_000L+level.getGameTime());
        h.runAtTickTime(850,()->{
            check(!level.getBlockState(h.absolutePos(doorLocal)).getValue(DoorBlock.OPEN),
                    "Bedless nighttime strolling reopened the door after the close task completed");
            check(!resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),
                    "Completed door task was started again without a new open-door event");
            check(resident.getPersistentData().contains("residenciasaldeoes_night_wander"),
                    "Bedless resident did not enter the bounded night-wander state");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100,batch="rain-night-door")
    public static void night_door_closure_runs_during_storms(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));ResidenceData data=ResidenceData.get(level);
        check(data.link(level,h.absolutePos(homePos),resident).equals("ok"),"Could not link rain resident");
        placeOpenDoor(h,new BlockPos(7,2,6),Blocks.OAK_DOOR);level.setDayTime(13_001L);level.setWeatherParameters(0,12_000, true, true);
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,400_000L+level.getGameTime());
        check(resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Storm-time nighttime door was not assigned to an awake resident");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=150,batch="rain-sleep")
    public static void night_rain_without_open_doors_keeps_bed_residents_asleep(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(homePos)).setSize(5),"Could not expand rainy sleep residence");
        for(int x=4;x<=8;x++)for(int z=4;z<=8;z++)if(x!=6||z!=6)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,3,7));ResidenceData data=ResidenceData.get(level);
        BlockPos home=h.absolutePos(homePos),bedLocal=new BlockPos(5,3,5),bed=h.absolutePos(bedLocal);
        check(data.link(level,home,resident).equals("ok"),"Could not link resident with bed");
        var bedHead=Blocks.WHITE_BED.defaultBlockState()
                .setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                .setValue(BedBlock.FACING,Direction.NORTH);
        h.setBlock(bedLocal.relative(Direction.SOUTH),bedHead.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        h.setBlock(bedLocal,bedHead);
        check(data.reserveBed(resident.getUUID(),level.dimension(),home,bed),"Could not reserve the resident's bed");
        level.setDayTime(13_001L);resident.startSleeping(bed);
        check(resident.isSleeping(),"Resident did not enter sleep before the rain transition");
        level.setWeatherParameters(0,12_000,true,false);level.setRainLevel(1.0F);
        check(level.isRaining(),"Rain fixture did not activate");
        h.runAtTickTime(100,()->{
            check(resident.isSleeping(),"Rain alone woke a resident who had a bed and no door task");
            check(data.ownsBed(level.dimension(),bed,resident.getUUID()),"Night rain discarded the bed reservation");
            check(!resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Resident received a door task although no door was open");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=1000,batch="rain-sleep")
    public static void night_rain_wakes_only_one_resident_to_close_a_door_and_return_to_bed(GameTestHelper h){
        floor(h);var level=h.getLevel();BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(homePos)).setSize(5),"Could not expand rainy sleep residence");
        for(int x=4;x<=8;x++)for(int z=4;z<=8;z++)if(x!=6||z!=6)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        Villager leader=h.spawn(EntityType.VILLAGER,new BlockPos(5,3,7));Villager second=h.spawn(EntityType.VILLAGER,new BlockPos(8,3,7));
        ResidenceData data=ResidenceData.get(level);BlockPos home=h.absolutePos(homePos);
        check(data.link(level,home,leader).equals("ok")&&data.link(level,home,second).equals("ok"),"Could not link sleeping residents");
        // Keep the second bed's foot clear of the doorway; the previous fixture
        // placed the open door on that half and made its sleeping state ambiguous.
        BlockPos firstBedLocal=new BlockPos(5,3,5),secondBedLocal=new BlockPos(7,3,7);
        for(BlockPos bedLocal:List.of(firstBedLocal,secondBedLocal)){
            var head=Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD).setValue(BedBlock.FACING,Direction.NORTH);
            h.setBlock(bedLocal.relative(Direction.SOUTH),head.setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT));h.setBlock(bedLocal,head);
        }
        BlockPos firstBed=h.absolutePos(firstBedLocal),secondBed=h.absolutePos(secondBedLocal);
        check(data.reserveBed(leader.getUUID(),level.dimension(),home,firstBed)&&data.reserveBed(second.getUUID(),level.dimension(),home,secondBed),"Could not reserve both beds");
        leader.startSleeping(firstBed);second.startSleeping(secondBed);check(leader.isSleeping()&&second.isSleeping(),"Residents did not begin asleep");
        placeOpenDoor(h,new BlockPos(7,3,6),Blocks.OAK_DOOR);level.setDayTime(13_001L);level.setWeatherParameters(0,12_000,true,false);level.setRainLevel(1.0F);
        invoke("beginNightDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},level,500_000L+level.getGameTime());
        check(leader.getPersistentData().contains("residenciasaldeoes_night_door_task"),"A resident was not assigned to close the open door");
        check(!leader.isSleeping(),"The selected resident did not wake to perform the door task");
        check(second.isSleeping(),"A second resident was woken unnecessarily");
        check(data.ownsBed(level.dimension(),firstBed,leader.getUUID())&&data.ownsBed(level.dimension(),secondBed,second.getUUID()),"Waking discarded a bed reservation");
        h.succeedWhen(()->{
            check(!level.getBlockState(h.absolutePos(new BlockPos(7,3,6))).getValue(DoorBlock.OPEN),"Resident did not physically close the rainy-night door");
            ResidenceData.BedClaim leaderClaim=data.bedClaim(leader.getUUID()),secondClaim=data.bedClaim(second.getUUID());
            check(leader.isSleeping()&&leaderClaim!=null&&leaderClaim.bed().equals(firstBed)
                    &&level.getBlockState(firstBed).getValue(BedBlock.OCCUPIED),"Selected resident did not return to its original reserved bed");
            check(second.isSleeping()&&secondClaim!=null&&secondClaim.bed().equals(secondBed)
                    &&level.getBlockState(secondBed).getValue(BedBlock.OCCUPIED),"Other resident stopped sleeping during the task");
        });
    }
    @GameTest(template="empty",timeoutTicks=700,batch="rain-doors")
    public static void daytime_rain_physically_closes_only_exposed_residential_doors(GameTestHelper h){
        floor(h);var level=h.getLevel();level.setDayTime(1_000L);level.setWeatherParameters(0,12_000,true,true);level.setRainLevel(1.0F);
        BlockPos wetHome=new BlockPos(4,2,6),shelteredHome=new BlockPos(12,2,6);
        h.setBlock(wetHome,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(shelteredHome,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(wetHome)).setSize(5),"Could not expand exposed rain residence");
        // A roof over the second anchor means dimension-wide rain is not local there.
        for(int x=10;x<=14;x++)for(int z=4;z<=8;z++)h.setBlock(new BlockPos(x,5,z),Blocks.STONE);
        Villager wetResident=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,6));
        Villager secondWetResident=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,7));
        Villager shelteredResident=h.spawn(EntityType.VILLAGER,new BlockPos(11,2,6));
        var data=ResidenceData.get(level);
        check("ok".equals(data.link(level,h.absolutePos(wetHome),wetResident)),"Could not link exposed rain resident");
        check("ok".equals(data.link(level,h.absolutePos(wetHome),secondWetResident)),"Could not link second exposed rain resident");
        check("ok".equals(data.link(level,h.absolutePos(shelteredHome),shelteredResident)),"Could not link sheltered resident");
        // Keep vanilla villager navigation from independently crossing and closing this door;
        // this assertion isolates whether the residence weather route touched the sheltered home.
        shelteredResident.setNoAi(true);
        BlockPos wetDoor=new BlockPos(5,2,6),shelteredDoor=new BlockPos(13,2,6);
        net.minecraft.world.phys.Vec3 initialPosition=wetResident.position();
        placeOpenDoor(h,wetDoor,Blocks.OAK_DOOR);placeOpenDoor(h,shelteredDoor,Blocks.OAK_DOOR);
        BlockPos absoluteWetHome=h.absolutePos(wetHome),absoluteShelteredHome=h.absolutePos(shelteredHome);
        check(level.isRainingAt(absoluteWetHome.above()),"Rain fixture is not wet at the exposed residence");
        check(!level.isRainingAt(absoluteShelteredHome.above()),"Roof did not shelter the second residence from local rain");
        invoke("beginWeatherDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                level,data.home(level,absoluteWetHome));
        check(level.getBlockState(h.absolutePos(wetDoor)).getValue(DoorBlock.OPEN),
                "The controller closed the door remotely instead of assigning a physical route");
        check(wetResident.getPersistentData().contains("residenciasaldeoes_night_door_task"),
                "Daytime rain did not assign a resident to walk to the open door");
        check(!secondWetResident.getPersistentData().contains("residenciasaldeoes_night_door_task"),
                "Two residents were assigned the same residential door");
        check(wetResident.position().equals(initialPosition),"The resident was moved instantly when the door task was assigned");
        check(!shelteredResident.getPersistentData().contains("residenciasaldeoes_night_door_task"),
                "Dimension-wide rain incorrectly assigned a route to the sheltered residence");
        h.succeedWhen(()->{
            check(!level.getBlockState(h.absolutePos(wetDoor)).getValue(DoorBlock.OPEN),
                    "Resident failed to physically close its door during daytime rain");
            check(!wetResident.position().equals(initialPosition),"Resident closed the door without walking to it");
            check(wetResident.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(wetDoor)))<=2.25D,
                    "Door was closed before the assigned resident reached it");
            check(level.getBlockState(h.absolutePos(shelteredDoor)).getValue(DoorBlock.OPEN),
                    "Sheltered residence door was modified");
        });
    }
    @GameTest(template="empty",timeoutTicks=100,batch="rain-transition-a")
    public static void daytime_door_task_stops_after_local_rain_ends(GameTestHelper h){
        floor(h);var level=h.getLevel();level.setDayTime(1_000L);level.setWeatherParameters(0,12_000,true,true);level.setRainLevel(1.0F);
        BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));
        var data=ResidenceData.get(level);BlockPos home=h.absolutePos(homePos);
        check("ok".equals(data.link(level,home,resident)),"Could not link rain-transition resident");
        placeOpenDoor(h,new BlockPos(7,2,6),Blocks.OAK_DOOR);
        invoke("beginWeatherDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                level,data.home(level,home));
        BlockPos door=h.absolutePos(new BlockPos(7,2,6));
        check(level.getBlockState(door).getValue(DoorBlock.OPEN),"Rain closed the door remotely before the resident arrived");
        check(resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),"Rain did not assign a physical close task");
        h.runAtTickTime(1,()->{
            level.setWeatherParameters(0,0,false,false);level.setRainLevel(0.0F);
            invoke("beginDaytimeDoorOpening",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                    level,data.owner(resident.getUUID()));
        });
        h.succeedWhen(()->{
            check(!resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),
                    "A physical close task continued after daytime rain ended");
            check(level.getBlockState(door).getValue(DoorBlock.OPEN),"The door was closed after the rain task was cancelled");
        });
    }
    @GameTest(template="empty",timeoutTicks=100,batch="rain-doors")
    public static void residential_rain_is_detected_at_exposed_perimeter_under_roofed_anchor(GameTestHelper h){
        floor(h);var level=h.getLevel();level.setDayTime(1_000L);level.setWeatherParameters(0,12_000,true,true);level.setRainLevel(1.0F);
        BlockPos homeLocal=new BlockPos(6,2,6);h.setBlock(homeLocal,ResidenciasDosAldeoes.ANCHOR.get());
        h.setBlock(homeLocal.above(),Blocks.STONE);
        BlockPos home=h.absolutePos(homeLocal);BlockPos exposedPerimeter=home.east(2).above();
        check(!level.isRainingAt(home.above()),"Roof did not shield the anchor sample");
        check(level.isRainingAt(exposedPerimeter),"Rain fixture is not effective at the exposed residential perimeter");
        check((Boolean)invoke("isRainingAtResidentialArea",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                level,ResidenceData.get(level).home(level,home)),"Residential weather detection ignored rain around a roofed anchor");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=900,batch="rain-doors")
    public static void residential_door_reopened_during_rain_is_closed_again(GameTestHelper h){
        floor(h);var level=h.getLevel();level.setDayTime(1_000L);level.setWeatherParameters(0,12_000,true,true);level.setRainLevel(1.0F);
        BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));var data=ResidenceData.get(level);
        check("ok".equals(data.link(level,h.absolutePos(homePos),resident)),"Could not link rain-door resident");
        BlockPos localDoor=new BlockPos(7,2,6);placeOpenDoor(h,localDoor,Blocks.OAK_DOOR);
        invoke("beginWeatherDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},level,data.owner(resident.getUUID()));
        h.runAtTickTime(250,()->{
            BlockPos door=h.absolutePos(localDoor);var state=level.getBlockState(door);
            check(!state.getValue(DoorBlock.OPEN),"Initial rain route did not physically close the test door before reopening it");
            // Exercise the real transition edge: a resident can already be walking
            // home after the first route, but that must not exclude it from closing
            // a door that reopens while local rain still requires closure.
            resident.getPersistentData().putBoolean("residenciasaldeoes_home_return_active",true);
            DoorBlock doorBlock=(DoorBlock)state.getBlock();
            for(int attempt=0;attempt<4;attempt++)doorBlock.setOpen(resident,level,state,door,true);
            check(!level.getBlockState(door).getValue(DoorBlock.OPEN),
                    "Resident AI reopened a controlled residential door during rain");
            Villager ordinary=h.spawn(EntityType.VILLAGER,new BlockPos(10,2,6));
            ordinary.setNoAi(true);
            doorBlock.setOpen(ordinary,level,level.getBlockState(door),door,true);
            check(level.getBlockState(door).getValue(DoorBlock.OPEN),
                    "Rain protection incorrectly blocked an unregistered villager");
        });
        h.runAtTickTime(800,()->{
            check(!level.getBlockState(h.absolutePos(localDoor)).getValue(DoorBlock.OPEN),
                    "An open door found during rain was not physically closed by a resident");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=700,batch="rain-transition-b")
    public static void daytime_residential_doors_reopen_after_rain_ends(GameTestHelper h){
        floor(h);var level=h.getLevel();level.setDayTime(1_000L);level.setWeatherParameters(0,12_000,true,true);level.setRainLevel(1.0F);
        BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));var data=ResidenceData.get(level);
        check("ok".equals(data.link(level,h.absolutePos(homePos),resident)),"Could not link dry-transition resident");
        BlockPos localDoor=new BlockPos(7,2,6);placeOpenDoor(h,localDoor,Blocks.OAK_DOOR);
        ResidenceData.Home home=data.owner(resident.getUUID());
        check((Boolean)invoke("isRainingAtResidentialArea",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                level,home),"Rain did not reach the residence before the closure transition");
        invoke("beginWeatherDoorClosure",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},level,home);
        check(level.getBlockState(h.absolutePos(localDoor)).getValue(DoorBlock.OPEN),
                "Rain closed the door remotely instead of assigning a resident route");
        check(resident.getPersistentData().contains("residenciasaldeoes_night_door_task"),
                "Resident was not assigned the rain door route");
        h.runAtTickTime(20,()->{
            check(!level.getBlockState(h.absolutePos(localDoor)).getValue(DoorBlock.OPEN),
                    "Resident did not physically close the door before the dry transition");
            level.setWeatherParameters(0,0,false,false);level.setRainLevel(0.0F);
            check(!level.isRaining(),"Weather fixture still reports global rain after clearing the storm");
            check(!(Boolean)invoke("isRainingAtResidentialArea",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                    level,home),"Residence still reports effective rain after the storm was cleared");
            check(!(Boolean)invoke("shouldCloseResidentialDoors",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},
                    level,home),"Dry daytime was still considered a closed-door period");
            invoke("beginDaytimeDoorOpening",new Class[]{net.minecraft.server.level.ServerLevel.class,ResidenceData.Home.class},level,home);
            check(level.getBlockState(h.absolutePos(localDoor)).getValue(DoorBlock.OPEN),
                    "Dry-day opening transition did not update the door immediately");
        });
        h.succeedWhen(()->check(level.getBlockState(h.absolutePos(localDoor)).getValue(DoorBlock.OPEN),
                "A door controlled during daytime rain did not reopen in dry daytime"));
    }
    @GameTest(template="empty",timeoutTicks=100,batch="spawns")
    public static void linked_residents_do_not_count_for_natural_golem_or_cat_population(GameTestHelper h){
        floor(h);BlockPos homePos=new BlockPos(6,2,6);h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));
        Villager vanilla=h.spawn(EntityType.VILLAGER,new BlockPos(7,2,6));
        var level=h.getLevel();var data=ResidenceData.get(level);
        check("ok".equals(data.link(level,h.absolutePos(homePos),resident)),"Could not link spawn-exclusion resident");
        var eligible=ResidentSpawnRules.withoutResidents(level,Villager.class,java.util.List.of(resident,vanilla));
        check(eligible.size()==1&&eligible.get(0)==vanilla,"Resident remained in the vanilla golem population");
        check(!ResidentSpawnRules.hasEnoughNonResidentHomesForVillageCats(4),"Four common occupied beds passed the vanilla cat threshold");
        check(!ResidentSpawnRules.hasEnoughNonResidentHomesForVillageCats(0),"Resident-only occupied beds passed the vanilla cat threshold");
        check(ResidentSpawnRules.hasEnoughNonResidentHomesForVillageCats(5),"Five common occupied beds no longer meet the vanilla cat threshold");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=2600,batch="physical")
    public static void physical_transport_after_payment(GameTestHelper h){
        floor(h);
        BlockPos homePos=new BlockPos(6,2,6),chestPos=new BlockPos(10,2,6);
        h.setBlock(homePos,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(chestPos,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(homePos);anchor.setSize(11);
        EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(chestPos);
        Villager first=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,6));first.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());
        check(data.link(h.getLevel(),h.absolutePos(homePos),first).equals("ok"),"Link failed");
        Villager second=h.spawn(EntityType.VILLAGER,new BlockPos(2,2,6));second.setAge(0);second.setNoAi(true);
        check(data.link(h.getLevel(),h.absolutePos(homePos),second).equals("ok"),"Second link failed");
        var home=data.home(h.getLevel(),h.absolutePos(homePos));
        long id=data.nextProductionDawnEventId();
        // Use the real production processor and its existing amount calculation/scheduling.
        data.queueProductionDawn(home,h.getLevel().getServer().overworld().getGameTime(),id);
        invoke("processLoadedProduction",new Class[]{net.minecraft.server.level.ServerLevel.class,long.class},h.getLevel(),h.getLevel().getServer().overworld().getGameTime());
        int producedPayment=anchor.getItem(0).getCount();check(producedPayment>0,"No payment");
        anchor.setOutputItem(9,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),17));
        int produced=producedPayment+17;
        double startX=first.getX();
        double originalSpeed=first.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
        boolean[] walked={false,false};int[] lastStage={-1},trips={0};
        h.runAtTickTime(290,()->check(chest.isEmpty()&&!first.getPersistentData().contains(JOB),"Collection started before 300 ticks"));
        h.succeedWhen(()->{
            if(first.getPersistentData().contains(JOB)&&first.getNavigation().getPath()!=null){
                int stage=first.getPersistentData().getCompound(JOB).getInt("stage");
                if(stage==0||stage==1){check(Math.abs(navigationSpeed(first)-.5D)<.0001D,"Courier is running");walked[stage]=true;}
                if(stage==1&&lastStage[0]!=1){trips[0]++;int carried=0;var cargo=first.getPersistentData().getCompound(JOB).getList("cargo",10);for(int i=0;i<cargo.size();i++)carried+=ItemStack.of(cargo.getCompound(i)).getCount();check(carried>0&&carried<=15,"Courier exceeded trip capacity");}
                lastStage[0]=stage;
            }
            check(!chest.isEmpty(),"Waiting for physical deposit; resident="+first.blockPosition()+" job="+first.getPersistentData().getCompound(JOB));
            check(chest.getItem(0).getCount()==produced,"Lost or duplicated fragments: stored="+chest.getItem(0).getCount()+" expected="+produced+" anchor="+anchor.getItem(0).getCount()+" pending="+anchor.getPendingFragments()+" resident="+first.blockPosition()+" job="+first.getPersistentData().getCompound(JOB));
            check(Math.abs(first.getX()-startX)>2,"Villager did not walk");
            check(!second.getPersistentData().contains(JOB),"Second resident was assigned conflicting transport");
            check(anchor.isEmpty(),"Anchor not collected");
            check(walked[0]&&walked[1]&&trips[0]>=2,"Walking speed or multiple capped trips were not observed");
            check(first.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue()==originalSpeed,"Normal movement attribute changed");
        });
    }
    @GameTest(template="empty",timeoutTicks=300,batch="full")
    public static void full_chest_multiple_trips_and_recovery(GameTestHelper h){
        BlockPos hp=new BlockPos(6,2,6),cp=new BlockPos(10,2,6);
        h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(cp,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);
        EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(cp);
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));resident.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());check(data.link(h.getLevel(),h.absolutePos(hp),resident).equals("ok"),"Link failed");
        long period=data.nextProductionDawnEventId();anchor.recordDailyProduction(period,50);
        var cargo=anchor.takeAvailableFragments(15);check(cargo.size()==1&&cargo.get(0).getCount()==15,"First trip was not capped at 15");
        net.minecraft.nbt.ListTag carried=new net.minecraft.nbt.ListTag();for(ItemStack stack:cargo)carried.add(stack.save(new net.minecraft.nbt.CompoundTag()));
        net.minecraft.nbt.CompoundTag job=new net.minecraft.nbt.CompoundTag();job.putLong("anchor",h.absolutePos(hp).asLong());job.putLong("chest",h.absolutePos(cp).asLong());job.put("cargo",carried);job.putInt("stage",1);job.putLong("started",h.getLevel().getGameTime());resident.getPersistentData().put(JOB,job);
        resident.getPersistentData().putBoolean("residenciasaldeoes_fragment_drop_processed",true);
        for(int slot=0;slot<27;slot++)chest.setItem(slot,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),64));
        ItemStack refused=new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),15);check(chest.insertFragments(refused)==0&&refused.getCount()==15,"Full chest consumed in-transit cargo");
        long total=anchor.getPendingFragments();for(int slot=0;slot<anchor.getContainerSize();slot++)total+=anchor.getItem(slot).getCount();total+=refused.getCount();
        check(total==50,"Full chest lost or duplicated fragments");
        resident.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,1000F);
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
    @GameTest(template="empty",timeoutTicks=800,batch="negative-y-sleep")
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
    @GameTest(template="empty",timeoutTicks=40,batch="dawn")
    public static void baby_production_is_one_and_adult_production_is_capped_at_five(GameTestHelper h){
        var baby=java.util.UUID.randomUUID();var adult=java.util.UUID.randomUUID();
        java.util.function.Predicate<java.util.UUID> onlyBaby=java.util.Set.of(baby)::contains;
        java.util.List<java.util.UUID> oneBaby=java.util.List.of(baby);
        check(VillagerResidenceEvents.calculateDailyFragments(oneBaby,new int[]{0,0,0,0},onlyBaby)==1,
                "Baby resident base production must be exactly one fragment");
        check(VillagerResidenceEvents.calculateDailyFragments(oneBaby,new int[]{1,0,0,1},onlyBaby)==1,
                "Beds and food must not increase baby production");
        check(VillagerResidenceEvents.calculateDailyFragments(oneBaby,new int[]{1,4,4,4},onlyBaby)==1,
                "Baby maximum must be exactly one fragment");
        check(VillagerResidenceEvents.calculateDailyFragments(java.util.List.of(adult),new int[]{0,0,0,0},onlyBaby)==1,
                "Adult base production must be one fragment");
        check(VillagerResidenceEvents.calculateDailyFragments(java.util.List.of(adult),new int[]{1,1,1,1},onlyBaby)==5,
                "Adult bed, workstation, chest, food benefits must cap at five");
        check(VillagerResidenceEvents.calculateDailyFragments(java.util.List.of(baby,adult),new int[]{2,1,1,1},onlyBaby)==6,
                "Baby-only restrictions consumed benefits reserved for an adult");
        var allocations=VillagerResidenceEvents.allocateDailyProduction(java.util.List.of(baby,adult),new int[]{2,1,1,1},onlyBaby);
        check(!allocations.get(0).workstation()&&!allocations.get(0).chest(),
                "Baby resident UI allocation must not include workstation or chest benefits");
        check(allocations.get(1).workstation()&&allocations.get(1).chest(),
                "Baby residents consumed benefits reserved for an adult in roster order");
        check(VillagerResidenceEvents.calculateDailyFragments(java.util.List.of(baby,baby),new int[]{2,0,0,1},onlyBaby)==2,
                "Each baby must receive exactly one fragment regardless of resources");
        check(VillagerResidenceEvents.calculateDailyFragments(java.util.List.of(adult,adult),new int[]{2,2,2,2},onlyBaby)==10,
                "Adult resource benefits exceeded the five-fragment daily maximum");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40,batch="dawn")
    public static void resident_fragment_balances_cap_at_twenty_and_persist(GameTestHelper h){
        BlockPos localHome=new BlockPos(5,2,5);h.setBlock(localHome,ResidenciasDosAldeoes.ANCHOR.get());
        check(((ResidentialAnchorBlockEntity)h.getBlockEntity(localHome)).setSize(5),"Could not expand storage test residence for two residents");
        Villager adult=h.spawn(EntityType.VILLAGER,new BlockPos(6,2,6));adult.setAge(0);
        Villager baby=h.spawn(EntityType.VILLAGER,new BlockPos(7,2,6));baby.setAge(-24000);
        ResidenceData data=ResidenceData.get(h.getLevel());BlockPos home=h.absolutePos(localHome);
        check("ok".equals(data.link(h.getLevel(),home,adult))&&"ok".equals(data.link(h.getLevel(),home,baby)),"Could not link storage test residents");
        check(data.creditResidentFragments(adult.getUUID(),18)==18,"Adult initial balance was not credited");
        check(data.previewResidentFragmentCredit(adult.getUUID(),5)==2&&data.creditResidentFragments(adult.getUUID(),5)==2,"Adult daily credit did not stop at twenty");
        check(data.creditResidentFragments(baby.getUUID(),25)==20,"Baby balance exceeded or failed to reach twenty");
        var sources=data.reserveResidentFragments(h.getLevel().dimension(),home,8);
        check(sources.stream().mapToInt(ResidenceData.ResidentFragmentSource::amount).sum()==8,"Courier could not reserve eight stored fragments");
        check(data.residentFragmentBalance(adult.getUUID())==20&&data.residentFragmentsInTransit(adult.getUUID())==8,"In-transit fragments were freed from the twenty-item cap");
        var restored=ResidenceData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        check(restored.residentFragmentBalance(adult.getUUID())==20&&restored.residentFragmentsInTransit(adult.getUUID())==8,"Resident balance or courier reservation did not persist");
        check(restored.residentFragmentBalance(baby.getUUID())==20,"Baby balance did not persist");
        var remainder=restored.deliverResidentFragments(sources,5);
        check(restored.residentFragmentBalance(adult.getUUID())==15&&restored.residentFragmentsInTransit(adult.getUUID())==3,"Delivered stock was not removed from the resident balance");
        restored.releaseResidentFragments(remainder);
        check(restored.residentFragmentsInTransit(adult.getUUID())==0,"Undelivered cargo reservation was not released");
        check(restored.consumeAvailableResidentFragments(h.getLevel().dimension(),home,7)==7,"Withdrawals did not free resident capacity");
        check(restored.previewResidentFragmentCredit(adult.getUUID(),20)==12,"Freed storage capacity was unavailable to later production");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40,batch="dawn")
    public static void resident_baby_status_persists_and_switches_to_adult_rules_on_growth(GameTestHelper h){
        var level=h.getLevel();BlockPos localHome=new BlockPos(5,2,5);BlockPos home=h.absolutePos(localHome);
        h.setBlock(localHome,ResidenciasDosAldeoes.ANCHOR.get());
        Villager resident=h.spawn(EntityType.VILLAGER,new BlockPos(7,2,7));resident.setAge(-1);
        var data=ResidenceData.get(level);
        check("ok".equals(data.link(level,home,resident)),"Could not register baby resident");
        check(data.residentIsBaby(resident.getUUID()),"Baby age category was not recorded at registration");
        var saved=data.save(new net.minecraft.nbt.CompoundTag());
        var reloaded=ResidenceData.load(saved);
        check(reloaded.residentIsBaby(resident.getUUID()),"Baby age category was not persisted in residence data");
        resident.setAge(0);data.updateResidentAge(resident);
        check(!data.residentIsBaby(resident.getUUID()),"Resident did not switch to adult classification after growing");
        check(VillagerResidenceEvents.calculateDailyFragments(java.util.List.of(resident.getUUID()),new int[]{0,0,0,0},data::residentIsBaby)==1,
                "Grown resident did not return to the existing adult production rule");
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
