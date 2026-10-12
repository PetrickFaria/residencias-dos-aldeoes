package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=ResidenciasDosAldeoes.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup(){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(ResidenciasDosAldeoes.ANCHOR_MENU.get(),AnchorScreen::new));}
    @SubscribeEvent public static void addVillagerSkinLayer(EntityRenderersEvent.AddLayers event){VillagerRenderer renderer=event.getRenderer(EntityType.VILLAGER);if(renderer!=null)VillagerSkinLayer.installBeforeVanillaLayers(renderer);}
}
