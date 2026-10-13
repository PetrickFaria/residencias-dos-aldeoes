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
    private static boolean requested,scene;
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("residencias.visualCheck"))return;
        Minecraft mc=Minecraft.getInstance();
        if(!requested&&mc.screen instanceof TitleScreen){requested=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"visual-check");}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
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
        if(ticks==250)Screenshot.grab(new File("../build"),"visual-chests.png",mc.getMainRenderTarget(),message->{});
        if(ticks==270){
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(()->server.getPlayerList().getPlayer(uuid).teleportTo(server.overworld(),14,-56,18.5,180F,22F));
        }
        if(ticks==340)Screenshot.grab(new File("../build"),"visual-doors.png",mc.getMainRenderTarget(),message->{});
        if(ticks==370)mc.stop();
    }
}
