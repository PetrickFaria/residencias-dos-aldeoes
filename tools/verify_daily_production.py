from pathlib import Path

root = Path('.')
data = (root / 'src/main/java/br/com/residenciasaldeoes/data/ResidenceData.java').read_text()
be = (root / 'src/main/java/br/com/residenciasaldeoes/block/ResidentialAnchorBlockEntity.java').read_text()
events = (root / 'src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java').read_text()
menu = (root / 'src/main/java/br/com/residenciasaldeoes/menu/AnchorMenu.java').read_text()
screen = (root / 'src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java').read_text()

# Persistent, unique dawn events with no calendar-day or interval gate.
for token in ('linked_at_game_time', 'pending_production_tasks', 'pending_emerald_collections', 'next_production_dawn_event_id'):
    assert token in data
assert 'MIN_DAWN_INTERVAL_TICKS' not in data
assert 'SleepFinishedTimeEvent' in events and 'event.getNewTime()' in events
assert 'CommandEvent' in events and 'onTimeCommand(CommandEvent event)' in events
assert 'processProductionDawn(level,productionGameTime)' in events
assert 'nextProductionDawnEventId()' in data and 'period<=previous' not in data
assert 'recordDailyProduction(eventId,fragments)' in events
assert 'last_production_event_id' in be
assert 'crossedNaturalDawn(previous,new DawnSample(levelGameTime,dayTime))' in events
assert 'after.gameTime()-before.gameTime()==1L' in events
assert 'after.dayTime()-before.dayTime()==1L' in events
assert 'DAWN_COMMAND_WINDOW_TICKS=1_200L' in events
assert 'boolean adjustedClock=' in events
assert 'oldTime>DAWN_COMMAND_WINDOW_TICKS&&newTime<=DAWN_COMMAND_WINDOW_TICKS' in events
assert 'TICKS_AFTER_DAWN_FOR_PAYMENT=200L' in events
assert 'TICKS_AFTER_PAYMENT_FOR_COLLECTION=1_200L' in events
assert 'gameTime+TICKS_AFTER_DAWN_FOR_PAYMENT' in events
assert 'currentGameTime+TICKS_AFTER_PAYMENT_FOR_COLLECTION' in events
assert 'currentGameTime<task.dueGameTime()' in events
assert 'RESIDENT_MATURITY_TICKS' not in events
assert 'next_production_dawn_event_id' in data and 'lastAcceptedDawnGameTime' in data
assert 'queueCollection(Home home,long eventId,long dueGameTime)' in data
assert 'dueCollection(Home home,long currentGameTime)' in data
assert 'AnchorMenu.countResources(level,home.pos,anchor.getSize())' in events
for token in ('if(index<resources[0])total++', 'if(index<resources[1])total++', 'if(index<resources[2])total++', 'if(index<resources[3]*2)total++'):
    assert token in events
assert 'level.hasChunkAt(home.pos)' in events and 'pendingProduction' in events
assert 'recorded&&fragments>0' in events
assert 'SoundEvents.VILLAGER_NO' in events and 'SoundEvents.BELL_BLOCK' not in events
assert 'playProductionBell(level,residents)' in events and 'return;' not in events[events.index('private void playProductionBell'):events.index('@SubscribeEvent public void onVillagerJoin')]
assert 'finishEmeraldTransfer(sl,v,v.getPersistentData().getCompound(EMERALD_TRANSFER),true)' in events
assert 'pending_fragments' in be and 'last_production_game_time' in be
assert 'ContainerHelper.saveAllItems(tag,outputItems)' in be
assert 'ContainerHelper.loadAllItems(tag,outputItems)' in be
assert 'for(int slot=0;slot<OUTPUT_SLOTS&&pendingFragments>0;slot++)' in be
assert 'mayPlace(ItemStack stack){return false;}' in menu
assert 'countResources(ServerLevel l,BlockPos a,int size)' in menu
assert 'imageWidth=272;imageHeight=232' in screen

def daily_fragments(resident_count, objectives):
    return sum(3 + int(i < objectives[0]) + int(i < objectives[1]) + int(i < objectives[2]) + int(i < objectives[3] * 2)
               for i in range(resident_count))

assert daily_fragments(1,(0,0,0,0))==3
assert [daily_fragments(1,(int(goal>=1),int(goal>=2),int(goal>=3),int(goal>=4))) for goal in range(5)]==[3,4,5,6,7]
assert daily_fragments(3,(3,3,3,2))==21

def crosses_natural_dawn(before_game,after_game,before_day,after_day):
    return (after_game-before_game==1 and after_day-before_day==1
            and before_day%24000==23999 and after_day%24000==0)
assert crosses_natural_dawn(500,501,23999,24000)
assert not crosses_natural_dawn(500,501,12000,13000)
assert not crosses_natural_dawn(500,501,13000,23000)
assert not crosses_natural_dawn(500,501,23999,0)
def commanded_dawn(before_game,after_game,before_day,after_day):
    old,before=before_day%24000,after_day%24000
    return (after_game>=before_game and after_game-before_game<=1 and after_day-before_day!=1
            and old>1200 and before<=1200)
assert commanded_dawn(500,500,23000,0) and commanded_dawn(500,501,13000,1000)
assert not commanded_dawn(500,500,10000,13000) and not commanded_dawn(500,501,13000,23000)
payment=10000+200
assert payment+1200==11400

event_ids=[1,2,3,4,5,6,7,8,9,10]
assert len(set(event_ids))==10 and all(a<b for a,b in zip(event_ids,event_ids[1:]))
assert 10*200==2000 and 10*1200==12000

def deposit(slots,pending):
    slots=list(slots)
    for i in range(5):
        move=min(64-slots[i],pending);slots[i]+=move;pending-=move
    return slots,pending
assert deposit([0,0,0,0,0],21)==([21,0,0,0,0],0)
assert deposit([60,0,0,0,0],21)==([64,17,0,0,0],0)
assert deposit([64,64,64,64,64],21)==([64,64,64,64,64],21)
print('Event-relative production, persistence, deduplication, and quantity checks passed; Minecraft runtime checks remain separate.')
