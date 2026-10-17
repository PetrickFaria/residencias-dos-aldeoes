from pathlib import Path

root=Path('src/main/java/br/com/residenciasaldeoes')
villager_mixin=(root/'mixin/VillagerTransportMixin.java').read_text()
cat_mixin=(root/'mixin/CatSpawnerMixin.java').read_text()
config=Path('src/main/resources/residencias.mixins.json').read_text()
rules=(root/'data/ResidentSpawnRules.java').read_text()
events=(root/'VillagerResidenceEvents.java').read_text()
visual=Path('src/visualTest/java/br/com/residenciasaldeoes/test/VisualClientCheck.java').read_text()
tests=Path('src/gametest/java/br/com/residenciasaldeoes/test/ResidenceGameTests.java').read_text()

assert '@Mixin(Villager.class)' in villager_mixin
assert 'method="spawnGolemIfNeeded"' in villager_mixin
assert 'target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"' in villager_mixin
assert 'residenciasaldeoes$residentDoesNotInitiateGolemSpawn' in villager_mixin
assert 'residenciasaldeoes$excludeResidentsFromGolemPopulation' in villager_mixin
assert 'ResidentSpawnRules.withoutResidents(level,entityType,entities)' in villager_mixin
assert '@Mixin(CatSpawner.class)' in cat_mixin
assert 'method="spawnInVillage"' in cat_mixin
assert 'countNonResidentOccupiedHomes(level, center, 48)' in cat_mixin
assert 'PoiManager.Occupancy.IS_OCCUPIED' in rules
assert 'MemoryModuleType.HOME' in rules and 'getSleepingPos()' in rules and 'data.bedClaim(residentId)' in rules
assert 'CAT_MINIMUM_OCCUPIED_HOMES = 5' in rules
assert 'Math.max(0L, occupiedHomes - residentHomes)' not in rules
assert 'hasEnoughNonResidentHomesForVillageCats' in rules
assert 'linked_residents_do_not_count_for_natural_golem_or_cat_population' in tests
assert 'residential_door_reopened_during_rain_is_closed_again' in tests
assert 'daytime_residential_doors_reopen_after_rain_ends' in tests
assert 'residential_rain_is_detected_at_exposed_perimeter_under_roofed_anchor' in tests
assert 'CatSpawnerMixin' in config
assert 'onVillageCatSpawn' not in events and 'LivingSpawnEvent' not in events
assert 'courierInFlight' in visual and 'getPendingFragments()==0' in visual
assert 'maximoPorViagem' not in visual
assert 'LivingSpawnEvent' not in villager_mixin+cat_mixin

print('Linked residents are filtered from vanilla golem and village-cat population checks; cat threshold edge cases and rain door regressions are covered.')
