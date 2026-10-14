package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.data.ResidenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.CatSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents registered residents' claimed home POIs from satisfying village cat population checks. */
@Mixin(CatSpawner.class)
public abstract class CatSpawnerMixin {
    @Inject(method="spawnInHouse",at=@At("HEAD"),cancellable=true)
    private void residenciasaldeoes$excludeResidentBedsFromVillageCatCount(ServerLevel level,BlockPos center,
                                                                           CallbackInfoReturnable<Integer> callback) {
        var homes=(java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.entity.ai.village.poi.PoiType>>)
                type->type.is(PoiTypes.HOME);
        PoiManager manager=level.getPoiManager();
        long occupiedHomes=manager.getCountInRange(homes,center,48,PoiManager.Occupancy.IS_OCCUPIED);
        if(occupiedHomes<=5)return;

        ResidenceData data=ResidenceData.get(level);
        long residentHomes=manager.getInRange(homes,center,48,PoiManager.Occupancy.IS_OCCUPIED)
                .filter(poi->data.bedOwner(level.dimension(),poi.getPos())!=null)
                .count();
        if(occupiedHomes-residentHomes<=5)callback.setReturnValue(0);
    }
}
