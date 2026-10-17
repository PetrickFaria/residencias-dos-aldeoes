package br.com.residenciasaldeoes.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Population filters for the vanilla village spawners. */
public final class ResidentSpawnRules {
    private static final int CAT_MINIMUM_OCCUPIED_HOMES = 5;

    private ResidentSpawnRules() {}

    /**
     * Vanilla's village cat spawner requires at least five occupied HOME POIs.
     * Only occupied POIs which remain after removing resident-owned beds may count.
     */
    public static boolean hasEnoughNonResidentHomesForVillageCats(long nonResidentHomes) {
        return nonResidentHomes >= CAT_MINIMUM_OCCUPIED_HOMES;
    }

    /** Counts occupied HOME POIs not currently owned by a linked resident. */
    public static long countNonResidentOccupiedHomes(ServerLevel level, BlockPos center, int radius) {
        ResidenceData data = ResidenceData.get(level);
        Set<BlockPos> residentBeds = residentBedPositions(level, data, center, radius);
        Predicate<Holder<PoiType>> homes = type -> type.is(PoiTypes.HOME);
        return level.getPoiManager()
                .getInRange(homes, center, radius, PoiManager.Occupancy.IS_OCCUPIED)
                .filter(poi -> !residentBeds.contains(poi.getPos()))
                .count();
    }

    /**
     * Removes registered residents from the local villager population used by
     * Villager.spawnGolemIfNeeded, without changing the level's entity query.
     */
    public static <T extends Entity> List<T> withoutResidents(ServerLevel level, Class<T> type, List<T> entities) {
        if (type != Villager.class) return entities;
        ResidenceData data = ResidenceData.get(level);
        return entities.stream()
                .filter(entity -> !(entity instanceof Villager villager) || data.owner(villager.getUUID()) == null)
                .toList();
    }

    private static Set<BlockPos> residentBedPositions(ServerLevel level, ResidenceData data, BlockPos center, int radius) {
        Set<BlockPos> beds = new HashSet<>();
        long radiusSquared = (long) radius * radius;
        ResourceKey<Level> dimension = level.dimension();
        for (ResidenceData.Home home : data.homes()) {
            if (!home.dimension.equals(dimension)) continue;
            for (var residentId : home.residents) {
                ResidenceData.BedClaim claim = data.bedClaim(residentId);
                if (claim != null && claim.dimension().equals(dimension)
                        && claim.bed().distSqr(center) <= radiusSquared) beds.add(claim.bed());

                if (!(level.getEntity(residentId) instanceof Villager resident)) continue;
                GlobalPos homeMemory = resident.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
                addNearbyBed(beds, homeMemory, dimension, center, radiusSquared);
                resident.getSleepingPos().ifPresent(pos -> {
                    if (pos.distSqr(center) <= radiusSquared) beds.add(pos.immutable());
                });
            }
        }
        return beds;
    }

    private static void addNearbyBed(Set<BlockPos> beds, GlobalPos memory, ResourceKey<Level> dimension,
                                     BlockPos center, long radiusSquared) {
        if (memory != null && memory.dimension().equals(dimension)
                && memory.pos().distSqr(center) <= radiusSquared) beds.add(memory.pos().immutable());
    }
}
