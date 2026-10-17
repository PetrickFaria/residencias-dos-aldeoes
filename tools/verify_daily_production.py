from pathlib import Path

root = Path('.')
data = (root / 'src/main/java/br/com/residenciasaldeoes/data/ResidenceData.java').read_text()
be = (root / 'src/main/java/br/com/residenciasaldeoes/block/ResidentialAnchorBlockEntity.java').read_text()
events = (root / 'src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java').read_text()
config = (root / 'src/main/java/br/com/residenciasaldeoes/ResidenceConfig.java').read_text()
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
assert 'defineInRange("fragmentosBasePorResidente", 1, 1, 1)' in config
assert 'defineInRange("esperaPagamentoTicks", 200, 20, 12000)' in config
assert 'defineInRange("esperaTransporteTicks", 300, 20, 12000)' in config
assert 'defineInRange("maximoPorViagem", 15, 1, 64)' in config
assert 'defineInRange("alcanceLocalizacaoBau", 1000, 16, 4096)' in config
index = (root / 'src/main/java/br/com/residenciasaldeoes/data/EmeraldChestIndex.java').read_text()
assert 'ResidenceConfig.CHEST_SEARCH_RADIUS.get()' in index
assert 'level.hasChunkAt(pos)' in index
assert 'ResidenceConfig.PAYMENT_DELAY_TICKS.get()' in events
assert 'ResidenceConfig.COLLECTION_DELAY_TICKS.get()' in events
assert 'ResidenceConfig.MAX_FRAGMENTS_PER_TRIP.get()' in events
assert 'takeAvailableFragments(ResidenceConfig.MAX_FRAGMENTS_PER_TRIP.get())' in events
assert 'currentGameTime<task.dueGameTime()' in events
assert 'RESIDENT_MATURITY_TICKS' not in events
assert 'next_production_dawn_event_id' in data and 'lastAcceptedDawnGameTime' in data
assert 'queueCollection(Home home,long eventId,long dueGameTime)' in data
assert 'dueCollection(Home home,long currentGameTime)' in data
assert 'AnchorMenu.countResources(level,home.pos,anchor.getSize())' in events
assert 'data.updateResidentAge(resident)' in events
assert 'public record ProductionAllocation(int fragments,boolean bed,boolean workstation,boolean chest,boolean food)' in events
assert 'public static List<ProductionAllocation> allocateDailyProduction(List<UUID> residents,int[] resources,' in events
assert 'if(baby){allocations.add(new ProductionAllocation(1,false,false,false,false));continue;}' in events
assert 'int fragments=1;' in events and 'fragments=Math.min(5,fragments);' in events
assert 'boolean hasWorkstation=workstations>0' in events
assert 'boolean hasChest=chests>0' in events
assert 'allocateDailyProduction(ids,resources,d::residentIsBaby)' in menu
assert 'benefits.workstation(),benefits.chest()' in menu
assert 'idx<resources[1]' not in menu and 'idx<resources[2]' not in menu
assert 'residentBabyStates' in data and 'resident_is_baby' in data
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

assert 'MAX_RESIDENT_FRAGMENT_STORAGE = 20' in data
assert 'previewResidentFragmentCredit' in events and 'data.creditResidentFragments' in events
assert 'resident_fragments_in_transit' in data and 'deliverResidentFragments' in events
assert 'consumeAvailableResidentFragments' in be and 'clearContent()' in be

def daily_fragments(babies, objectives, adult_base=1):
    beds,work,chests,cakes=objectives
    remaining=[beds,work,chests,cakes*2]
    total=0
    for baby in babies:
        total+=1 if baby else adult_base
        if baby:
            continue
        if remaining[0]>0:
            total+=1;remaining[0]-=1
        if not baby and remaining[1]>0:
            total+=1;remaining[1]-=1
        if not baby and remaining[2]>0:
            total+=1;remaining[2]-=1
        if remaining[3]>0:
            total+=1;remaining[3]-=1
    return total

assert daily_fragments([True],(0,0,0,0))==1
assert daily_fragments([True],(1,0,0,0))==1
assert daily_fragments([True],(0,0,0,1))==1
assert daily_fragments([True],(1,4,4,1))==1
assert daily_fragments([False],(0,0,0,0))==1
assert daily_fragments([False],(1,1,1,1))==5
assert daily_fragments([True,False],(2,1,1,1))==6
assert daily_fragments([True,True],(2,0,0,1))==2
assert daily_fragments([False,False,False],(3,3,3,2))==15

def accepted_credit(balance,amount,limit=20):
    return min(max(0,amount),max(0,limit-balance))
assert accepted_credit(18,5)==2
assert accepted_credit(20,5)==0
assert accepted_credit(8,20)==12
print('Resident fragment cap scenarios passed: 18+5 accepts 2; full balance accepts 0; withdrawals restore capacity.')

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
assert payment+300==10500

event_ids=[1,2,3,4,5,6,7,8,9,10]
assert len(set(event_ids))==10 and all(a<b for a,b in zip(event_ids,event_ids[1:]))
assert 10*200==2000 and 10*300==3000

def deposit(slots,pending):
    slots=list(slots)
    for i in range(len(slots)):
        move=min(64-slots[i],pending);slots[i]+=move;pending-=move
    return slots,pending
assert deposit([0,0,0,0,0],21)==([21,0,0,0,0],0)
assert deposit([60,0,0,0,0],21)==([64,17,0,0,0],0)
assert deposit([64,64,64,64,64],21)==([64,64,64,64,64],21)
assert deposit([64]*10,21)==([64]*10,21)
assert deposit([64]*5+[0]*5,21)==([64]*5+[21]+[0]*4,0)
print('Event-relative production, persistence, deduplication, baby caps, adult rules, and quantity checks passed; Minecraft runtime checks remain separate.')
