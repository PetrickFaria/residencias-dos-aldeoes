from pathlib import Path

root = Path('.')
data = (root / 'src/main/java/br/com/residenciasaldeoes/data/ResidenceData.java').read_text()
be = (root / 'src/main/java/br/com/residenciasaldeoes/block/ResidentialAnchorBlockEntity.java').read_text()
events = (root / 'src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java').read_text()
menu = (root / 'src/main/java/br/com/residenciasaldeoes/menu/AnchorMenu.java').read_text()
screen = (root / 'src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java').read_text()

# Source-level invariants: persistent per-resident server ticks, accepted dawns,
# unloaded-home queueing, and output-only ordered slot storage.
for token in ('linked_at_game_time', 'last_accepted_production_dawn', 'pending_production_dawns'):
    assert token in data
assert 'MIN_DAWN_INTERVAL_TICKS = 12_000L' in data
assert 'SleepFinishedTimeEvent' in events and 'gameDelta==1L&&dayDelta==1L' in events
assert 'dawnGameTime-linkedAt<RESIDENT_MATURITY_TICKS' in events
assert 'AnchorMenu.countResources(level,home.pos,anchor.getSize())' in events
assert 'if(index<resources[0])total++' in events
assert 'if(index<resources[1])total++' in events
assert 'if(index<resources[2])total++' in events
assert 'if(index<resources[3]*2)total++' in events
assert 'level.hasChunkAt(home.pos)' in events and 'pendingProductionDawns' in events
assert 'pending_fragments' in be and 'last_production_game_time' in be
assert 'ContainerHelper.saveAllItems(tag,outputItems)' in be
assert 'ContainerHelper.loadAllItems(tag,outputItems)' in be
assert 'for(int slot=0;slot<OUTPUT_SLOTS&&pendingFragments>0;slot++)' in be
assert 'mayPlace(ItemStack stack){return false;}' in menu
assert 'countResources(ServerLevel l,BlockPos a,int size)' in menu
assert 'imageWidth=272;imageHeight=232' in screen
assert '0xFF342419' in screen and '0xFFB18A5E' in screen

# Contract cases for eligibility, objective bonuses, and threshold boundaries.
def daily_fragments(linked_times, dawn, objectives):
    return sum(3 + sum((i < objectives[0], i < objectives[1], i < objectives[2], i < objectives[3] * 2))
               for i, linked in enumerate(linked_times) if dawn >= linked and dawn - linked >= 6000)

assert daily_fragments([0], 5999, (1, 1, 1, 1)) == 0
assert daily_fragments([0], 6000, (0, 0, 0, 0)) == 3
assert [daily_fragments([0], 6000, (int(goal >= 1), int(goal >= 2), int(goal >= 3), int(goal >= 4))) for goal in range(5)] == [3, 4, 5, 6, 7]
assert daily_fragments([0, 0, 0], 6000, (3, 3, 3, 2)) == 21
assert daily_fragments([0, 0, 0, 0, 0], 6000, (5, 5, 5, 3)) == 35
assert daily_fragments([0, 1000], 6000, (2, 2, 2, 1)) == 7
assert daily_fragments([0, 1000], 6999, (2, 2, 2, 1)) == 7
assert daily_fragments([0, 1000], 7000, (2, 2, 2, 1)) == 14

# Repeated / rapidly alternating dawn signals are rejected by the persistent interval.
def accept(last, now):
    return last is None or (now > last and now - last >= 12000)
assert accept(None, 100)
assert not accept(100, 101)
assert not accept(100, 12099)
assert accept(100, 12100)
assert not accept(12100, 12099)

# First-fit output contract: merge/fill slot 1 before later slots; retain overflow.
def deposit(slots, pending):
    slots = list(slots)
    for i in range(5):
        move = min(64 - slots[i], pending)
        slots[i] += move
        pending -= move
    return slots, pending
assert deposit([0, 0, 0, 0, 0], 21) == ([21, 0, 0, 0, 0], 0)
assert deposit([60, 0, 0, 0, 0], 21) == ([64, 17, 0, 0, 0], 0)
assert deposit([64, 64, 64, 64, 64], 21) == ([64, 64, 64, 64, 64], 21)
assert deposit([64, 64, 64, 64, 64], 321) == ([64, 64, 64, 64, 64], 321)
assert deposit([0, 0, 0, 0, 0], 321) == ([64, 64, 64, 64, 64], 1)
print('Daily production source invariants and contract cases passed; these are not Minecraft runtime tests.')
