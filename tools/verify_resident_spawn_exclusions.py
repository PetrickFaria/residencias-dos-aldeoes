from pathlib import Path

root=Path('src/main/java/br/com/residenciasaldeoes')
villager_mixin=(root/'mixin/VillagerTransportMixin.java').read_text()
cat_mixin=(root/'mixin/CatSpawnerMixin.java').read_text()
config=Path('src/main/resources/residencias.mixins.json').read_text()

assert '@Mixin(Villager.class)' in villager_mixin
assert 'method="golemSpawnConditionsMet"' not in villager_mixin
assert 'excludeResidentsFromGolemPopulation' not in villager_mixin
assert '@Mixin(CatSpawner.class)' in cat_mixin
assert 'method="spawnInVillage"' in cat_mixin
assert 'PoiManager.Occupancy.IS_OCCUPIED' in cat_mixin
assert 'data.bedOwner(level.dimension(),poi.getPos())!=null' in cat_mixin
assert 'occupiedHomes-residentHomes<=5' in cat_mixin
assert 'CatSpawnerMixin' in config
assert 'LivingSpawnEvent' not in villager_mixin+cat_mixin

print('Vanilla golem eligibility is no longer overridden; resident-bed cat threshold and cat mixin remain intact.')
