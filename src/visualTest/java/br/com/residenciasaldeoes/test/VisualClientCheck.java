package br.com.residenciasaldeoes.test;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.File;

/** Development-only scene; excluded from the installable main JAR. */
@Mod.EventBusSubscriber(modid="residenciasaldeoes",value=Dist.CLIENT)
public final class VisualClientCheck {
    private static final int[] OPACITY_PERCENTAGES={0,10,25,50,75,90,100};
    private static boolean requested,scene,depositCaptured,overlapVerified,closedCaptured,courierStarted,opacityScenePrepared,opacitySceneCaptured,multiOpacityCaptured;
    private static volatile boolean graphicalDeliveryVerified;
    private static int lastCourierIntegrityCheckTick;
    private static volatile int opacityVillagerEntityId=-1;
    private static volatile int[] multiOpacityVillagerIds=new int[0];
    private static final int[] MULTI_OPACITY_PERCENTAGES={0,25,75,100};
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("residencias.visualCheck"))return;
        Minecraft mc=Minecraft.getInstance();
        if(!requested&&mc.screen instanceof TitleScreen){requested=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"visual-check");}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(!scene&&mc.screen!=null)return;
        if(Boolean.getBoolean("residencias.visualRestore")){
            if(!scene){
                scene=true;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
                server.execute(()->{
                    var level=server.overworld();BlockPos pos=new BlockPos(1,-58,8);
                    var blockEntity=level.getBlockEntity(pos);
                    if(!(blockEntity instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity anchor))
                        throw new IllegalStateException("Saved anchor missing after process restart");
                    if(anchor.getContainerSize()!=10)throw new IllegalStateException("Saved inventory size changed");
                    for(int i=0;i<10;i++)if(!anchor.getItem(i).is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())||anchor.getItem(i).getCount()!=i+1)
                        throw new IllegalStateException("Saved output slot lost or duplicated: "+i);
                    var player=server.getPlayerList().getPlayer(uuid);
                    ResidenciasDosAldeoes.ANCHOR.get().use(level.getBlockState(pos),level,pos,player,
                        net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
                    System.out.println("TEN_SLOTS_WORLD_RESTART_PASSED");
                });
            }
            ticks++;
            if(ticks==120){
                if(!(mc.screen instanceof br.com.residenciasaldeoes.client.AnchorScreen))throw new IllegalStateException("Restored anchor GUI did not open");
                Screenshot.grab(new File("../build"),"visual-anchor-restored.png",mc.getMainRenderTarget(),message->{});
            }
            if(ticks==150)mc.stop();
            return;
        }
        if(!scene){
            scene=true;
            mc.options.hideGui=false;
            var server=mc.getSingleplayerServer();
            var uuid=mc.player.getUUID();
            server.execute(()->{
                var level=server.overworld();level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-5,-64,0,24,-44,31)).forEach(net.minecraft.world.entity.Entity::discard);
                for(int x=-5;x<=23;x++)for(int z=0;z<=30;z++){
                    level.setBlock(new BlockPos(x,-59,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
                    for(int y=-58;y<=-45;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                }
                // The copied GameTest world may contain saved residence data at
                // this same coordinate. Remove that fixture entry before creating
                // the visual scene, so previous residents/output cannot affect the
                // courier conservation check.
                br.com.residenciasaldeoes.data.ResidenceData.get(level).unlinkAll(
                        level,level.dimension(),new BlockPos(1,-58,8));
                level.setBlock(new BlockPos(1,-58,8),ResidenciasDosAldeoes.ANCHOR.get().defaultBlockState(),3);
                level.setBlock(new BlockPos(4,-58,8),ResidenciasDosAldeoes.EMERALD_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING,Direction.SOUTH),3);
                BlockPos opened=new BlockPos(7,-58,8);
                level.setBlock(opened,ResidenciasDosAldeoes.EMERALD_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING,Direction.SOUTH),3);
                level.blockEvent(opened,ResidenciasDosAldeoes.EMERALD_CHEST.get(),1,1);
                int x=10;
                for(Direction facing:Direction.Plane.HORIZONTAL){
                    for(boolean open:new boolean[]{false,true}){
                        BlockPos pos=new BlockPos(x,-58,open?12:8);
                        var state=ResidenciasDosAldeoes.REINFORCED_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING,facing).setValue(DoorBlock.OPEN,open);
                        level.setBlock(pos,state,3);level.setBlock(pos.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
                    }
                    x+=2;
                }
                var player=server.getPlayerList().getPlayer(uuid);
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.getAbilities().flying=true;player.onUpdateAbilities();
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,6000,0,false,false));
                player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ResidenciasDosAldeoes.EMERALD_CHEST_ITEM.get()));
                player.inventoryMenu.broadcastChanges();
                player.teleportTo(level,5.5,-56,14.5,180F,24F);
            });
        }
        ticks++;
        if(!courierStarted&&ticks>=60&&mc.screen==null){
            courierStarted=true;var server=mc.getSingleplayerServer();
            server.execute(()->{
                var level=server.overworld();BlockPos home=new BlockPos(1,-58,8);
                var anchor=(br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity)level.getBlockEntity(home);anchor.setSize(11);
                var courier=net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                courier.moveTo(1.5,-58,10.5,0F,0F);courier.setAge(0);level.addFreshEntity(courier);
                var data=br.com.residenciasaldeoes.data.ResidenceData.get(level);data.link(level,home,courier);
                long payment=data.nextProductionDawnEventId();anchor.recordDailyProduction(payment,17);
                data.queueCollection(data.home(level,home),payment,level.getGameTime());
            });
        }
        if(mc.level.getBlockEntity(new BlockPos(4,-58,8)) instanceof br.com.residenciasaldeoes.block.EmeraldChestBlockEntity delivery){
            if(!depositCaptured&&mc.screen==null&&delivery.getCourierOpeners()>0&&delivery.getOpenNess(0)>.8F){
                depositCaptured=true;
                Screenshot.grab(new File("../build"),"visual-courier-deposit.png",mc.getMainRenderTarget(),message->{});
                var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
                server.execute(()->{
                    var level=server.overworld();BlockPos pos=new BlockPos(4,-58,8);var player=server.getPlayerList().getPlayer(uuid);
                    player.teleportTo(level,4.5,-58,11.5,180F,18F);
                    ResidenciasDosAldeoes.EMERALD_CHEST.get().use(level.getBlockState(pos),level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
                });
            }
            // The configured trip limit is 15, so a 17-fragment collection is
            // intentionally delivered in two physical trips. Validate total
            // conservation only after the anchor and courier have both finished.
            if(depositCaptured&&!graphicalDeliveryVerified&&ticks-lastCourierIntegrityCheckTick>=10){
                lastCourierIntegrityCheckTick=ticks;
                var server=mc.getSingleplayerServer();
                server.execute(()->{
                    var level=server.overworld();BlockPos anchorPos=new BlockPos(1,-58,8),chestPos=new BlockPos(4,-58,8);
                    if(!(level.getBlockEntity(anchorPos) instanceof br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity anchor)
                            ||!(level.getBlockEntity(chestPos) instanceof br.com.residenciasaldeoes.block.EmeraldChestBlockEntity chest))return;
                    int deposited=0;for(int i=0;i<27;i++)deposited+=chest.getItem(i).getCount();
                    var home=br.com.residenciasaldeoes.data.ResidenceData.get(level).home(level,anchorPos);
                    boolean courierInFlight=false;
                    for(var id:home.residents)if(level.getEntity(id) instanceof net.minecraft.world.entity.npc.Villager resident
                            &&resident.getPersistentData().contains("residenciasaldeoes_emerald_transfer"))courierInFlight=true;
                    if(!courierInFlight&&anchor.isEmpty()&&anchor.getPendingFragments()==0){
                        long loose=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                                new net.minecraft.world.phys.AABB(anchorPos).inflate(48.0D),item->item.getItem().is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()))
                                .stream().mapToLong(item->item.getItem().getCount()).sum();
                        if(deposited!=17||loose!=0L)throw new IllegalStateException(
                                "Graphical courier delivery lost/duplicated fragments: expected chest=17 loose=0, got chest="+deposited+" loose="+loose);
                        graphicalDeliveryVerified=true;
                        System.out.println("COURIER_GRAPHICAL_DEPOSIT_PASSED fragments="+deposited);
                    }
                });
            }
            if(depositCaptured&&!overlapVerified&&graphicalDeliveryVerified&&delivery.getCourierOpeners()==0&&delivery.getCombinedOpeners()>0&&delivery.getOpenNess(0)>.8F){
                overlapVerified=true;System.out.println("COURIER_PLAYER_OVERLAP_PASSED");
                var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
                server.execute(()->server.getPlayerList().getPlayer(uuid).closeContainer());
            }
            if(overlapVerified&&!closedCaptured&&delivery.getCombinedOpeners()==0&&delivery.getOpenNess(0)<.05F){
                closedCaptured=true;System.out.println("COURIER_CLIENT_OPEN_CLOSE_PASSED");
                Screenshot.grab(new File("../build"),"visual-courier-closed.png",mc.getMainRenderTarget(),message->{});
            }
        }
        if(ticks==250)Screenshot.grab(new File("../build"),"visual-chests.png",mc.getMainRenderTarget(),message->{});
        if(ticks==270){
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->server.getPlayerList().getPlayer(uuid).teleportTo(server.overworld(),14,-56,18.5,180F,22F));
        }
        if(ticks==340)Screenshot.grab(new File("../build"),"visual-doors.png",mc.getMainRenderTarget(),message->{});
        if(ticks==345){
            mc.options.hideGui=true;
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->{
                var level=server.overworld();BlockPos pos=new BlockPos(10,-58,8);
                var state=ResidenciasDosAldeoes.REINFORCED_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH);
                level.setBlock(pos,state,3);level.setBlock(pos.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
                server.getPlayerList().getPlayer(uuid).teleportTo(level,10.5,-58,12,180F,9F);
            });
        }
        if(ticks==395)Screenshot.grab(new File("../build"),"visual-reference-door.png",mc.getMainRenderTarget(),message->{});
        if(ticks==400){
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->server.getPlayerList().getPlayer(uuid).teleportTo(server.overworld(),6.8,-56.3,11.2,194F,46F));
        }
        if(ticks==450)Screenshot.grab(new File("../build"),"visual-chest-cavity.png",mc.getMainRenderTarget(),message->{});
        if(ticks==460){
            mc.options.hideGui=false;
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->{
                var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                // Isolate alpha captures from mobs left by earlier visual checks.
                level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                        new net.minecraft.world.phys.AABB(-2,-64,0,13,-44,14))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                level.setWeatherParameters(6000,0,false,false);
                BlockPos pos=new BlockPos(1,-58,8);
                player.teleportTo(level,1.5,-58,10.5,180F,20F);
                var anchor=(br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity)level.getBlockEntity(pos);
                for(int i=0;i<anchor.getContainerSize();i++)anchor.setOutputItem(i,new net.minecraft.world.item.ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),i+1));
                anchor.setSize(11);
                var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                villager.moveTo(2.5,-58,8.5,0F,0F);villager.setAge(0);level.addFreshEntity(villager);
                br.com.residenciasaldeoes.data.ResidenceData.get(level).link(level,pos,villager);
                ResidenciasDosAldeoes.ANCHOR.get().use(level.getBlockState(pos),level,pos,player,
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
            });
        }
        if(ticks==520){
            if(!(mc.screen instanceof br.com.residenciasaldeoes.client.AnchorScreen)||mc.player.containerMenu.slots.size()!=10)
                throw new IllegalStateException("Anchor screen with ten slots did not open");
            Screenshot.grab(new File("../build"),"visual-anchor-slots.png",mc.getMainRenderTarget(),message->{});
        }
        if(ticks==530){
            mc.player.closeContainer();
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->{
                var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                // High-contrast wall behind the resident makes partial alpha visible in the capture.
                for(int x=0;x<=10;x++)for(int y=-58;y<=-53;y++){
                    var block=((x+y)&1)==0?Blocks.RED_CONCRETE:Blocks.BLUE_CONCRETE;
                    level.setBlock(new BlockPos(x,y,3),block.defaultBlockState(),3);
                }
                var resident=net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                if(resident==null)throw new IllegalStateException("Could not create Steve opacity visual test resident");
                resident.setNoAi(true);resident.setInvulnerable(true);resident.moveTo(5.5,-58,5.5,0F,0F);level.addFreshEntity(resident);
                var data=br.com.residenciasaldeoes.data.ResidenceData.get(level);BlockPos home=new BlockPos(1,-58,8);
                if(!"ok".equals(data.link(level,home,resident)))throw new IllegalStateException("Could not register Steve opacity visual test resident");
                data.setResidentOpacity(resident.getUUID(),50);
                data.setResidentModel(resident.getUUID(),br.com.residenciasaldeoes.data.ResidentModelType.STEVE);
                opacityVillagerEntityId=resident.getId();
                br.com.residenciasaldeoes.network.ModNetwork.sendOpacity(player,resident.getUUID(),50);
                br.com.residenciasaldeoes.network.ModNetwork.sendResidentModel(player,resident.getUUID(),br.com.residenciasaldeoes.data.ResidentModelType.STEVE);
                player.teleportTo(level,5.5,-57,10.5,180F,15F);
            });
        }
        if(ticks==545){
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->{
                var level=server.overworld();BlockPos home=new BlockPos(1,-58,8);
                var data=br.com.residenciasaldeoes.data.ResidenceData.get(level);
                int[] positions={2,4,7,9};int[] ids=new int[MULTI_OPACITY_PERCENTAGES.length];
                for(int i=0;i<MULTI_OPACITY_PERCENTAGES.length;i++){
                    var resident=net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                    if(resident==null)throw new IllegalStateException("Could not create multi-resident Steve opacity fixture "+i);
                    resident.setNoAi(true);resident.setInvulnerable(true);resident.moveTo(positions[i]+.5,-58,5.5,0F,0F);level.addFreshEntity(resident);
                    if(!"ok".equals(data.link(level,home,resident)))throw new IllegalStateException("Could not register multi-resident opacity fixture "+i);
                    int opacity=MULTI_OPACITY_PERCENTAGES[i];data.setResidentOpacity(resident.getUUID(),opacity);
                    data.setResidentModel(resident.getUUID(),br.com.residenciasaldeoes.data.ResidentModelType.STEVE);ids[i]=resident.getId();
                    br.com.residenciasaldeoes.network.ModNetwork.sendOpacity(server.getPlayerList().getPlayer(uuid),resident.getUUID(),opacity);
                    br.com.residenciasaldeoes.network.ModNetwork.sendResidentModel(server.getPlayerList().getPlayer(uuid),resident.getUUID(),br.com.residenciasaldeoes.data.ResidentModelType.STEVE);
                }
                multiOpacityVillagerIds=ids;
            });
        }
        if(ticks==555&&multiOpacityVillagerIds.length==MULTI_OPACITY_PERCENTAGES.length){
            for(int i=0;i<multiOpacityVillagerIds.length;i++){
                var entity=mc.level.getEntity(multiOpacityVillagerIds[i]);
                if(!(entity instanceof net.minecraft.world.entity.npc.Villager resident)
                        ||!br.com.residenciasaldeoes.client.ResidentAppearanceModels.usesSteve(resident)
                        ||br.com.residenciasaldeoes.client.ResidentOpacity.get(resident.getUUID())!=MULTI_OPACITY_PERCENTAGES[i])
                    throw new IllegalStateException("Per-resident Steve opacity was not synchronized for slot "+i);
            }
            System.out.println("STEVE_MULTI_RESIDENT_OPACITY_SYNC_PASSED values=0,25,75,100");
        }
        if(ticks==560&&opacityVillagerEntityId>=0){
            var entity=mc.level.getEntity(opacityVillagerEntityId);
            if(!(entity instanceof net.minecraft.world.entity.npc.Villager resident)
                    ||!br.com.residenciasaldeoes.client.ResidentAppearanceModels.usesSteve(resident)
                    ||br.com.residenciasaldeoes.client.ResidentOpacity.get(resident.getUUID())!=50)
                throw new IllegalStateException("Steve 50% opacity/model data was not synchronized to the client renderer");
            opacityScenePrepared=true;
        }
        if(opacityScenePrepared){
            var resident=(net.minecraft.world.entity.npc.Villager)mc.level.getEntity(opacityVillagerEntityId);
            for(int i=0;i<OPACITY_PERCENTAGES.length;i++){
                int setTick=560+i*3,captureTick=setTick+2;
                if(ticks==setTick)br.com.residenciasaldeoes.client.ResidentOpacity.set(resident.getUUID(),OPACITY_PERCENTAGES[i]);
                if(ticks==captureTick){
                    String name="visual-steve-opacity-"+OPACITY_PERCENTAGES[i]+".png";
                    Screenshot.grab(new File("../build"),name,mc.getMainRenderTarget(),message->{});
                    if(i==3)System.out.println("STEVE_WORLD_OPACITY_50_PASSED");
                    if(i==OPACITY_PERCENTAGES.length-1){System.out.println("STEVE_WORLD_OPACITY_PERCENTAGES_PASSED");opacitySceneCaptured=true;}
                }
            }
        }
        if(ticks==585){
            if(multiOpacityVillagerIds.length!=MULTI_OPACITY_PERCENTAGES.length)throw new IllegalStateException("Multi-resident opacity scene was not created");
            Screenshot.grab(new File("../build"),"visual-steve-opacity-multiple.png",mc.getMainRenderTarget(),message->{});
            multiOpacityCaptured=true;System.out.println("STEVE_MULTI_RESIDENT_OPACITY_CAPTURED");
        }
        if(ticks==590){if(!depositCaptured||!overlapVerified||!closedCaptured||!opacitySceneCaptured||!multiOpacityCaptured)throw new IllegalStateException("Courier/client opening or Steve opacity checks incomplete");mc.stop();}
    }
}
