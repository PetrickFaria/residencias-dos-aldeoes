package br.com.residenciasaldeoes.test;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.VillagerResidenceEvents;
import br.com.residenciasaldeoes.block.*;
import br.com.residenciasaldeoes.data.ResidenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
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
    private static void floor(GameTestHelper h){for(int x=0;x<16;x++)for(int z=0;z<16;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);}
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
    @GameTest(template="empty",timeoutTicks=2600)
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
        int produced=anchor.getItem(0).getCount();check(produced>0,"No payment");
        double startX=first.getX();
        h.runAtTickTime(1190,()->check(chest.isEmpty()&&!first.getPersistentData().contains(JOB),"Collection started before 1200 ticks"));
        h.runAtTickTime(1202,()->ResidenciasDosAldeoes.REINFORCED_DOOR.get().setOpen(null,h.getLevel(),h.getLevel().getBlockState(h.absolutePos(deliveryDoor)),h.absolutePos(deliveryDoor),false));
        h.succeedWhen(()->{
            check(!chest.isEmpty(),"Waiting for physical deposit; resident="+first.blockPosition()+" job="+first.getPersistentData().getCompound(JOB));
            check(chest.getItem(0).getCount()==produced,"Lost or duplicated fragments");
            check(Math.abs(first.getX()-startX)>2,"Villager did not walk");
            check(!second.getPersistentData().contains(JOB),"Second resident was assigned conflicting transport");
            check(h.getLevel().getBlockState(h.absolutePos(deliveryDoor)).getValue(DoorBlock.OPEN),"Courier failed to open the reinforced door");
            check(anchor.isEmpty(),"Anchor not collected");
        });
    }
    @GameTest(template="empty",timeoutTicks=1600)
    public static void full_chest_multiple_trips_and_recovery(GameTestHelper h){
        floor(h);
        BlockPos hp=new BlockPos(6,2,6),cp=new BlockPos(10,2,6);
        h.setBlock(hp,ResidenciasDosAldeoes.ANCHOR.get());h.setBlock(cp,ResidenciasDosAldeoes.EMERALD_CHEST.get());
        ResidentialAnchorBlockEntity anchor=(ResidentialAnchorBlockEntity)h.getBlockEntity(hp);anchor.setSize(11);
        EmeraldChestBlockEntity chest=(EmeraldChestBlockEntity)h.getBlockEntity(cp);
        for(int i=0;i<27;i++)chest.setItem(i,new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),64));
        Villager first=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,6));first.setAge(0);
        ResidenceData data=ResidenceData.get(h.getLevel());
        check(data.link(h.getLevel(),h.absolutePos(hp),first).equals("ok"),"Link failed");
        Villager replacement=h.spawn(EntityType.VILLAGER,new BlockPos(3,2,6));replacement.setAge(0);
        check(data.link(h.getLevel(),h.absolutePos(hp),replacement).equals("ok"),"Replacement link failed");
        var home=data.home(h.getLevel(),h.absolutePos(hp));
        long id=data.nextProductionDawnEventId();anchor.recordDailyProduction(id,65);
        data.queueCollection(home,id,h.getLevel().getServer().overworld().getGameTime());
        long secondId=data.nextProductionDawnEventId();anchor.recordDailyProduction(secondId,65);
        data.queueCollection(home,secondId,h.getLevel().getServer().overworld().getGameTime());
        h.runAtTickTime(5,()->{
            var cargo=first.getPersistentData().getCompound(JOB).getList("cargo",10);
            check(cargo.size()==1&&ItemStack.of(cargo.getCompound(0)).getCount()==64,"First resident did not collect one full stack");
            net.minecraft.nbt.CompoundTag save=new net.minecraft.nbt.CompoundTag();first.saveWithoutId(save);first.load(save);
            var restored=first.getPersistentData().getCompound(JOB).getList("cargo",10);
            check(restored.size()==1&&ItemStack.of(restored.getCompound(0)).getCount()==64,"Cargo failed NBT roundtrip");
            first.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC,1000F);
            int recovered=(int)anchor.getPendingFragments();for(int i=0;i<5;i++)recovered+=anchor.getItem(i).getCount();
            check(recovered==130,"Resident death lost or duplicated in-transit fragments");
        });
        h.runAtTickTime(180,()->{
            int total=(int)anchor.getPendingFragments();for(int i=0;i<5;i++)total+=anchor.getItem(i).getCount();
            var cargo=replacement.getPersistentData().getCompound(JOB).getList("cargo",10);
            check(cargo.size()<=1,"Transport has more than one slot");
            for(int i=0;i<cargo.size();i++){int count=ItemStack.of(cargo.getCompound(i)).getCount();check(count<=64,"Slot exceeded 64");total+=count;}
            check(total==130,"Full chest caused loss/duplication");
            check(chest.getItem(26).getCount()==64,"Full chest was overwritten");
            // Existing full contents are extracted normally, leaving room for queued cargo.
            for(int i=0;i<27;i++)chest.removeItem(i,64);
        });
        h.succeedWhen(()->{
            int total=0;for(int i=0;i<27;i++)total+=chest.getItem(i).getCount();
            check(total==130,"Waiting for multiple safe trips");
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
