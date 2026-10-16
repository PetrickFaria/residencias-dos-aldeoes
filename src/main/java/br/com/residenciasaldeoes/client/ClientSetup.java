package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.block.EmeraldChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
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
    @SubscribeEvent public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(ResidentAppearanceModels.STEVE_LAYER,SteveVillagerModel::createLayer);}
    @SubscribeEvent public static void addVillagerSkinLayer(EntityRenderersEvent.AddLayers event){VillagerRenderer renderer=event.getRenderer(EntityType.VILLAGER);if(renderer!=null)VillagerSkinLayer.installBeforeVanillaLayers(renderer);}
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ResidenciasDosAldeoes.EMERALD_CHEST_BE.get(), EmeraldChestRenderer::new);}
    @SubscribeEvent public static void stitchChestTextures(TextureStitchEvent.Pre event){
        if(event.getAtlas().location().equals(Sheets.CHEST_SHEET)){
            event.addSprite(new ResourceLocation(ResidenciasDosAldeoes.MODID,"entity/chest/emerald_chest"));
            event.addSprite(new ResourceLocation(ResidenciasDosAldeoes.MODID,"entity/chest/emerald_chest_left"));
            event.addSprite(new ResourceLocation(ResidenciasDosAldeoes.MODID,"entity/chest/emerald_chest_right"));
        }
    }
}
