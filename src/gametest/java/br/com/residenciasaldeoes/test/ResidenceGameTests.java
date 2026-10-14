package br.com.residenciasaldeoes.test;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.VillagerResidenceEvents;
import br.com.residenciasaldeoes.block.*;
import br.com.residenciasaldeoes.data.ResidenceData;
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
        h.runAtTickTime(1395,()->check(!v.getPersistentData().contains(JOB)&&chest.isEmpty(),"Transport started before one minute after payment"));
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
        Villager unavailable=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,6));unavailable.setAge(0);unavailable.setNoAi(true);
        Villager courier=h.spawn(EntityType.VILLAGER,new BlockPos(4,2,6));courier.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());check(data.link(h.getLevel(),h.absolutePos(hp),unavailable).equals("ok"),"First link failed");check(data.link(h.getLevel(),h.absolutePos(hp),courier).equals("ok"),"Second link failed");
        var home=data.home(h.getLevel(),h.absolutePos(hp));long id=data.nextProductionDawnEventId();
        anchor.recordDailyProduction(id,23);data.queueCollection(home,id,h.getLevel().getGameTime());
        h.succeedWhen(()->{
            EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(cp);
            check(!unavailable.getPersistentData().contains(JOB),"Unavailable resident assigned");
            check(chest.getItem(0).getCount()==23&&anchor.isEmpty(),"Available second resident did not deliver");
            check(courier.getX()>h.absolutePos(hp).getX()+1,"Second resident did not walk to chest");
        });
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
            var trip=reloaded.takeAvailableFragments();check(!trip.isEmpty(),"Transport cannot read remaining slots");
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
    @GameTest(template="empty",timeoutTicks=2600,batch="physical")
    public static void physical_transport_after_payment(GameTestHelper h){
        floor(h);
        BlockPos homePos=new BlockPos(6,2,6),chestPos=new BlockPos(10,2,6);
        BlockPos deliveryDoor=new BlockPos(8,2,6);
        for(int x=7;x<=10;x++)for(int z:new int[]{5,7})for(int y=2;y<=3;y++)
            h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
        var gate=ResidenciasDosAldeoes.REINFORCED_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST);
        h.setBlock(deliveryDoor,gate);
        h.setBlock(deliveryDoor.above(),gate.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
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
        boolean[] walked={false,false};
        h.runAtTickTime(1190,()->check(chest.isEmpty()&&!first.getPersistentData().contains(JOB),"Collection started before 1200 ticks"));
        h.runAtTickTime(1202,()->ResidenciasDosAldeoes.REINFORCED_DOOR.get().setOpen(null,h.getLevel(),h.getLevel().getBlockState(h.absolutePos(deliveryDoor)),h.absolutePos(deliveryDoor),false));
        h.succeedWhen(()->{
            if(first.getPersistentData().contains(JOB)&&first.getNavigation().getPath()!=null){
                int stage=first.getPersistentData().getCompound(JOB).getInt("stage");
                if(stage==0||stage==1){check(Math.abs(navigationSpeed(first)-.5D)<.0001D,"Courier is running");walked[stage]=true;}
            }
            check(!chest.isEmpty(),"Waiting for physical deposit; resident="+first.blockPosition()+" job="+first.getPersistentData().getCompound(JOB));
            check(chest.getItem(0).getCount()==produced,"Lost or duplicated fragments");
            check(Math.abs(first.getX()-startX)>2,"Villager did not walk");
            check(!second.getPersistentData().contains(JOB),"Second resident was assigned conflicting transport");
            check(h.getLevel().getBlockState(h.absolutePos(deliveryDoor)).getValue(DoorBlock.OPEN),"Courier failed to open the reinforced door");
            check(anchor.isEmpty(),"Anchor not collected");
            check(walked[0]&&walked[1],"Walking speed not observed on both legs");
            check(first.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue()==originalSpeed,"Normal movement attribute changed");
        });
    }
    @GameTest(template="empty",timeoutTicks=1600,batch="full")
    public static void full_chest_multiple_trips_and_recovery(GameTestHelper h){
        floor(h);
        BlockPos hp=new BlockPos(6,2,6),cp=new BlockPos(10,2,6);
        h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(cp,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);anchor.setSize(11);
        EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(cp);
        Villager first=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));first.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());
        check(data.link(h.getLevel(),h.absolutePos(hp),first).equals("ok"),"Link failed");
        Villager replacement=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,6));replacement.setAge(0);
        check(data.link(h.getLevel(),h.absolutePos(hp),replacement).equals("ok"),"Replacement link failed");
        var home=data.home(h.getLevel(),h.absolutePos(hp));
        long id=data.nextProductionDawnEventId();anchor.recordDailyProduction(id,325);
        data.queueCollection(home,id,h.getLevel().getServer().overworld().getGameTime());
        long secondId=data.nextProductionDawnEventId();anchor.recordDailyProduction(secondId,325);
        data.queueCollection(home,secondId,h.getLevel().getServer().overworld().getGameTime());
        boolean[] killed={false};
        h.runAtTickTime(180,()->{
            int total=(int)anchor.getPendingFragments();for(int i=0;i<anchor.getContainerSize();i++)total+=anchor.getItem(i).getCount();
            var cargo=replacement.getPersistentData().getCompound(JOB).getList("cargo",10);
            check(cargo.size()<=1,"Transport has more than one slot");
            for(int i=0;i<cargo.size();i++){int count=ItemStack.of(cargo.getCompound(i)).getCount();check(count<=64,"Slot exceeded 64");total+=count;}
            check(total==650,"Full chest caused loss/duplication");
            check(chest.getItem(26).getCount()==64,"Full chest was overwritten");
            // Existing full contents are extracted normally, leaving room for queued cargo.
            for(int i=0;i<27;i++)chest.removeItem(i,64);
        });
        h.succeedWhen(()->{
            if(!killed[0]){
            var cargo=first.getPersistentData().getCompound(JOB).getList("cargo",10);
            check(cargo.size()==1&&ItemStack.of(cargo.getCompound(0)).getCount()==64,"First resident did not collect one full stack");
            net.minecraft.nbt.CompoundTag save=new net.minecraft.nbt.CompoundTag();first.saveWithoutId(save);first.load(save);
            var restored=first.getPersistentData().getCompound(JOB).getList("cargo",10);
            check(restored.size()==1&&ItemStack.of(restored.getCompound(0)).getCount()==64,"Cargo failed NBT roundtrip");
            for(int i=0;i<27;i++)chest.setItem(i,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),64));
            first.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,1000F);
            int recovered=(int)anchor.getPendingFragments();for(int i=0;i<anchor.getContainerSize();i++)recovered+=anchor.getItem(i).getCount();
            check(recovered==650,"Resident death lost or duplicated in-transit fragments");
                killed[0]=true;
            }
            int total=0;for(int i=0;i<27;i++)total+=chest.getItem(i).getCount();
            check(total==650,"Waiting for multiple safe trips");
            check(anchor.isEmpty()&&anchor.getPendingFragments()==0,"Fragments remain at anchor");
            check(home.pendingCollections.isEmpty(),"Old collection tasks were not drained");
        });
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
