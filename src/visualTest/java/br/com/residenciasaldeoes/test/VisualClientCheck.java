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
    private static boolean requested,scene,depositCaptured,overlapVerified,closedCaptured,courierStarted;
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
            if(depositCaptured&&!overlapVerified&&delivery.getCourierOpeners()==0&&delivery.getCombinedOpeners()>0&&delivery.getOpenNess(0)>.8F){
                overlapVerified=true;System.out.println("COURIER_PLAYER_OVERLAP_PASSED");
                var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
                server.execute(()->{
                    var chest=(br.com.residenciasaldeoes.block.EmeraldChestBlockEntity)server.overworld().getBlockEntity(new BlockPos(4,-58,8));
                    int deposited=0;for(int i=0;i<27;i++)deposited+=chest.getItem(i).getCount();
                    if(deposited!=17)throw new IllegalStateException("Graphical courier delivery lost/duplicated fragments");
                    System.out.println("COURIER_GRAPHICAL_DEPOSIT_PASSED fragments="+deposited);
                    server.getPlayerList().getPlayer(uuid).closeContainer();
                });
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
        if(ticks==550){if(!depositCaptured||!overlapVerified||!closedCaptured)throw new IllegalStateException("Courier/client opening checks incomplete");mc.stop();}
    }
}
