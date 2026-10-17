package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.data.ResidentSpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.CatSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Excludes linked residents' occupied beds from vanilla village cat population checks. */
@Mixin(CatSpawner.class)
public abstract class CatSpawnerMixin {
    @Inject(method="spawnInVillage",at=@At("HEAD"),cancellable=true)
    private void residenciasaldeoes$excludeResidentBedsFromVillageCatCount(ServerLevel level,BlockPos center,
                                                                           CallbackInfoReturnable<Integer> callback) {
        if (!ResidentSpawnRules.hasEnoughNonResidentHomesForVillageCats(
                ResidentSpawnRules.countNonResidentOccupiedHomes(level, center, 48))) {
            callback.setReturnValue(0);
        }
    }
}
