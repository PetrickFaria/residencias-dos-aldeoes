package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.data.ResidentSpawnRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Bounded resident jobs temporarily own navigation; ordinary Brain activities resume afterward. */
@Mixin(Villager.class)
public abstract class VillagerTransportMixin {
    @Inject(method="customServerAiStep",at=@At("HEAD"),cancellable=true)
    private void residenciasaldeoes$deliveryOwnsNavigation(CallbackInfo callback) {
        Villager villager=(Villager)(Object)this;
        if(villager.level instanceof net.minecraft.server.level.ServerLevel
                &&(villager.getPersistentData().contains("residenciasaldeoes_emerald_transfer")
                ||villager.getPersistentData().contains("residenciasaldeoes_night_door_task")))
            callback.cancel();
    }

    @Inject(method="customServerAiStep",at=@At("TAIL"))
    private void residenciasaldeoes$holdSleepingResidentStill(CallbackInfo callback) {
        Villager villager=(Villager)(Object)this;
        if(villager.level instanceof ServerLevel level
                &&villager.isSleeping()
                &&ResidenceData.get(level).owner(villager.getUUID())!=null)
            br.com.residenciasaldeoes.VillagerResidenceEvents.stabilizeSleepingResident(villager);
    }

    /** Linked residents neither initiate a golem check nor count as its nearby village population. */
    @Inject(method="spawnGolemIfNeeded",at=@At("HEAD"),cancellable=true)
    private void residenciasaldeoes$residentDoesNotInitiateGolemSpawn(ServerLevel level,long gameTime,
                                                                     int requiredVillagers,CallbackInfo callback) {
        Villager villager=(Villager)(Object)this;
        if(ResidenceData.get(level).owner(villager.getUUID())!=null)callback.cancel();
    }

    @Redirect(method="spawnGolemIfNeeded",at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private <T extends Entity> List<T> residenciasaldeoes$excludeResidentsFromGolemPopulation(
            ServerLevel level,Class<T> entityType,AABB bounds) {
        List<T> entities=level.getEntitiesOfClass(entityType,bounds);
        return ResidentSpawnRules.withoutResidents(level,entityType,entities);
    }
}
