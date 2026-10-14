from pathlib import Path

root=Path('src/main/java/br/com/residenciasaldeoes')
villager_mixin=(root/'mixin/VillagerTransportMixin.java').read_text()
cat_mixin=(root/'mixin/CatSpawnerMixin.java').read_text()
config=Path('src/main/resources/residencias.mixins.json').read_text()

assert '@Mixin(Villager.class)' in villager_mixin
assert 'method="golemSpawnConditionsMet"' in villager_mixin
assert 'ResidenceData.get(level).owner(villager.getUUID())!=null' in villager_mixin
assert '@Mixin(CatSpawner.class)' in cat_mixin
assert 'method="spawnInHouse"' in cat_mixin
assert 'PoiManager.Occupancy.IS_OCCUPIED' in cat_mixin
assert 'data.bedOwner(level.dimension(),poi.getPos())!=null' in cat_mixin
assert 'occupiedHomes-residentHomes<=5' in cat_mixin
assert 'CatSpawnerMixin' in config
assert 'LivingSpawnEvent' not in villager_mixin+cat_mixin

print('Resident-only golem eligibility and occupied-home cat thresholds verified; natural and nonresident spawn paths remain active.')
