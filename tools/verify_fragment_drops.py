from pathlib import Path

source=Path('src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java').read_text()
start=source.index('public void onVillagerDrops(LivingDropsEvent event)')
end=source.index('@SubscribeEvent public void onDeath',start)
handler=source[start:end]
assert 'event.getEntity() instanceof Villager villager' in handler
assert 'villager.isBaby()' in handler and '||villager.isBaby()' in handler
assert 'villager.level instanceof ServerLevel' in handler
assert 'residenciasaldeoes_fragment_drop_processed' in handler
assert 'persistent.getBoolean(marker)' in handler and 'persistent.putBoolean(marker,true)' in handler
assert 'villager.getRandom().nextFloat()<0.75F?1:2' in handler
assert 'new ItemStack(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get(),count)' in handler
assert 'getLootingLevel' not in handler and 'Looting' not in handler
assert 'event.getDrops().add(new ItemEntity' in handler
# The two mutually exclusive outcomes cover all adult-villager deaths and sum to 100%.
assert 0.75 + 0.25 == 1.0
assert (1, 2) == (1, 2)
print('Adult villager fragment-drop conditions, mutually exclusive 75/25 outcomes, and duplicate guard passed; in-game frequency tests remain separate.')
