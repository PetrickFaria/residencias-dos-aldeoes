from pathlib import Path

root = Path('.')
data = (root / 'src/main/java/br/com/residenciasaldeoes/data/ResidenceData.java').read_text()
be = (root / 'src/main/java/br/com/residenciasaldeoes/block/ResidentialAnchorBlockEntity.java').read_text()
events = (root / 'src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java').read_text()
menu = (root / 'src/main/java/br/com/residenciasaldeoes/menu/AnchorMenu.java').read_text()
screen = (root / 'src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java').read_text()

# Source-level invariants: persistent period deduplication, sleep handling,
# unloaded-home queueing, and output-only ordered slot storage.
for token in ('linked_at_game_time', 'last_accepted_production_period', 'pending_production_dawns'):
    assert token in data
assert 'MIN_DAWN_INTERVAL_TICKS' not in data
assert 'SleepFinishedTimeEvent' in events and 'event.getNewTime()' in events
assert 'crossedTarget(previous.dayTime(),dayTime,PRODUCTION_TIME_OF_DAY)' in events
assert 'crossedTarget(previous.dayTime(),dayTime,COLLECTION_TIME_OF_DAY)' in events
assert 'PRODUCTION_TIME_OF_DAY=200L' in events
assert 'COLLECTION_TIME_OF_DAY=3_000L' in events
assert 'RESIDENT_MATURITY_TICKS' not in events
assert 'period<=previous' in data
assert 'lastAcceptedDawnGameTime' in data  # legacy save field is retained without governing eligibility
assert 'AnchorMenu.countResources(level,home.pos,anchor.getSize())' in events
assert 'if(index<resources[0])total++' in events
assert 'if(index<resources[1])total++' in events
assert 'if(index<resources[2])total++' in events
assert 'if(index<resources[3]*2)total++' in events
assert 'level.hasChunkAt(home.pos)' in events and 'pendingProductionDawns' in events
assert 'recorded&&fragments>0' in events
assert 'level.getEntity(resident)' in events and 'SoundEvents.BELL_BLOCK' in events
assert '0.65D/Math.sqrt(Math.max(1L,nearby))' in events
assert 'pending_fragments' in be and 'last_production_game_time' in be
assert 'ContainerHelper.saveAllItems(tag,outputItems)' in be
assert 'ContainerHelper.loadAllItems(tag,outputItems)' in be
assert 'for(int slot=0;slot<OUTPUT_SLOTS&&pendingFragments>0;slot++)' in be
assert 'mayPlace(ItemStack stack){return false;}' in menu
assert 'countResources(ServerLevel l,BlockPos a,int size)' in menu
assert 'imageWidth=272;imageHeight=232' in screen
assert '0xFF342419' in screen and '0xFFB18A5E' in screen

# Every linked resident contributes immediately; objectives are the same counts as the UI.
def daily_fragments(resident_count, objectives):
    return sum(3 + int(i < objectives[0]) + int(i < objectives[1]) + int(i < objectives[2]) + int(i < objectives[3] * 2)
               for i in range(resident_count))

assert daily_fragments(1, (0, 0, 0, 0)) == 3
assert [daily_fragments(1, (int(goal >= 1), int(goal >= 2), int(goal >= 3), int(goal >= 4))) for goal in range(5)] == [3, 4, 5, 6, 7]
assert daily_fragments(3, (3, 3, 3, 2)) == 21
assert daily_fragments(5, (5, 5, 5, 3)) == 35

# The supplied minimap examples agree with 20 game ticks per displayed minute:
# day=1000 -> 00:50, night=13000 -> 10:50, 23000 -> 19:10.
assert 1000 // 20 == 50  # vanilla day=1000 corresponds to 00:50 in displayed clock minutes
assert 13000 // 20 == 650
assert 23000 // 20 == 1150
assert 10 * 50 == 500
# Target points: 00:10 is tick 200 and 02:30 is tick 3000.
assert 10 * 20 == 200
assert (2 * 60 + 30) * 20 == 3000

def crosses(before, after, target):
    old, new = before % 24000, after % 24000
    if after > before:
        return old < target and new >= target
    return before > after and before - after > 12000 and old > target and new >= target
assert crosses(199, 200, 200)
assert crosses(0, 13000, 200)  # a forward command jump is caught once
assert not crosses(13000, 23000, 200)  # switching from night to late evening does not re-pay
assert crosses(23000, 1000, 200)  # moving through midnight is recognized
assert not crosses(1000, 1200, 200)

# A game day is accepted once; switching world time cannot make duplicate entries in that day.
def accept(last_period, period):
    return last_period is None or period > last_period
assert accept(None, 10)
assert not accept(10, 10)
assert not accept(10, 9)
assert accept(10, 11)

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
print('Daily production and collection schedule invariants passed; these are source-level checks, not Minecraft runtime tests.')
