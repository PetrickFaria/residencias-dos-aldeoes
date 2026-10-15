package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only Forge-bus events. Kept separate from mod lifecycle/model registration events. */
@Mod.EventBusSubscriber(modid = ResidenciasDosAldeoes.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSkinEvents {
    private ClientSkinEvents() {}

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VillagerSkinTextures.clearAll();
        ResidentOpacity.clearAll();
    }
}
