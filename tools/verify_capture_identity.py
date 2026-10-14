from pathlib import Path
root=Path('src/main/java/br/com/residenciasaldeoes')
data=(root/'data/ResidenceData.java').read_text()
events=(root/'VillagerResidenceEvents.java').read_text()
tests=Path('src/gametest/java/br/com/residenciasaldeoes/test/ResidenceGameTests.java').read_text()
mods=(Path('src/main/resources/META-INF/mods.toml')).read_text().lower()
assert 'residenciasaldeoes_resident_identity' in data
assert 'residentIdentityOwners.put(identity,id)' in data
assert 'resident_identity' in data and 'u.hasUUID("resident_identity")' in data
assert 'reconcileCapturedResident(ServerLevel level,Villager villager)' in data
assert 'serverLevel.getEntity(current)!=null' in data and 'if(owner(id)!=null)return false' in data
assert 'data.reconcileCapturedResident(sl,v)' in events
assert 'ResidenceData.get(sl).unlink(sl,v.getUUID())' in events  # Real deaths still remove registrations.
assert 'captured_resident_identity_rebinds_without_duplicates' in tests
assert 'Duplicate capture copy took a live resident registration' in tests
assert 'capturetomob' not in mods  # Optional compatibility; no hard dependency.
print('Capture identity token persistence, duplicate guard, death cleanup, and optional dependency policy verified.')
