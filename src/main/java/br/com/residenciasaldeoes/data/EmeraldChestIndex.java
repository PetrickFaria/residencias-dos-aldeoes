package br.com.residenciasaldeoes.data;

import br.com.residenciasaldeoes.block.EmeraldChestBlockEntity;
import br.com.residenciasaldeoes.ResidenceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Loaded-world index: never enumerates blocks or creates chunk tickets. */
@Mod.EventBusSubscriber(modid="residenciasaldeoes")
public final class EmeraldChestIndex {
    private static final Map<ServerLevel,Map<Long,Set<BlockPos>>> LEVELS=new WeakHashMap<>();
    private EmeraldChestIndex(){}
    public static void register(ServerLevel level,BlockPos pos){
        LEVELS.computeIfAbsent(level,l->new HashMap<>()).computeIfAbsent(ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4),c->new HashSet<>()).add(pos.immutable());
    }
    public static void unregister(ServerLevel level,BlockPos pos){
        var chunks=LEVELS.get(level);if(chunks==null)return;
        long key=ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);var positions=chunks.get(key);
        if(positions!=null){positions.remove(pos);if(positions.isEmpty())chunks.remove(key);}
    }
    public static List<BlockPos> registeredPositions(ServerLevel level){
        List<BlockPos> result=new ArrayList<>();var chunks=LEVELS.get(level);
        if(chunks!=null)for(var positions:chunks.values())result.addAll(positions);
        return result;
    }
    public static List<BlockPos> nearby(ServerLevel level,BlockPos center){
        int searchRadius=ResidenceConfig.CHEST_SEARCH_RADIUS.get();
        List<BlockPos> result=new ArrayList<>();var chunks=LEVELS.get(level);if(chunks==null)return result;
        int minX=(center.getX()-searchRadius)>>4,maxX=(center.getX()+searchRadius)>>4;
        int minZ=(center.getZ()-searchRadius)>>4,maxZ=(center.getZ()+searchRadius)>>4;
        for(var entry:new ArrayList<>(chunks.entrySet())){
            ChunkPos chunk=new ChunkPos(entry.getKey());if(chunk.x<minX||chunk.x>maxX||chunk.z<minZ||chunk.z>maxZ)continue;
            for(BlockPos pos:new ArrayList<>(entry.getValue())){
                if(!level.hasChunkAt(pos)){unregister(level,pos);continue;}
                if(!(level.getBlockEntity(pos) instanceof EmeraldChestBlockEntity)){unregister(level,pos);continue;}
                if(pos.distSqr(center)<=(double)searchRadius*searchRadius)result.add(pos);
            }
        }
        result.sort(Comparator.<BlockPos>comparingDouble(p->p.distSqr(center)).thenComparing(Comparator.naturalOrder()));
        return result;
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk chunk)
            for(var be:chunk.getBlockEntities().values())if(be instanceof EmeraldChestBlockEntity)register(level,be.getBlockPos());
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){
        if(event.getLevel() instanceof ServerLevel level){var chunks=LEVELS.get(level);if(chunks!=null)chunks.remove(event.getChunk().getPos().toLong());}
    }
}
