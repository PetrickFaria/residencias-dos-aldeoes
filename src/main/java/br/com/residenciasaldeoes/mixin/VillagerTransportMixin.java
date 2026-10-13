package br.com.residenciasaldeoes.mixin;

import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Delivery owns navigation while active; ordinary Brain activities resume afterward. */
@Mixin(Villager.class)
public abstract class VillagerTransportMixin {
    @Inject(method="customServerAiStep",at=@At("HEAD"),cancellable=true)
    private void residenciasaldeoes$deliveryOwnsNavigation(CallbackInfo callback) {
        Villager villager=(Villager)(Object)this;
        if(villager.level instanceof net.minecraft.server.level.ServerLevel
                &&villager.getPersistentData().contains("residenciasaldeoes_emerald_transfer"))
            callback.cancel();
    }
}
