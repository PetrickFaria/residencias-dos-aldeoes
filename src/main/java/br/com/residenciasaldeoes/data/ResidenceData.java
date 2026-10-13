package br.com.residenciasaldeoes.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Persisted links for all residential anchors in one world. */
public class ResidenceData extends SavedData {
    public static final String NAME = "residenciasaldeoes_residences";
    public static final int[] DIMENSIONS = {3,5,7,9,11};
    public static final int[] CAPACITIES = {1,3,5,7,9};
    public static final long MIN_DAWN_INTERVAL_TICKS = 12_000L;
    public static final class Home {
        public ResourceKey<Level> dimension;
        public BlockPos pos;
        public int size = 3;
        public final LinkedHashSet<UUID> residents = new LinkedHashSet<>();
        public final List<Long> pendingProductionDawns = new ArrayList<>();
        Home(ResourceKey<Level> dim, BlockPos p) { dimension=dim; pos=p; }
    }
    private final Map<String, Home> homes = new LinkedHashMap<>();
    private final Map<UUID, String> owners = new HashMap<>();
    private final Map<UUID, BedClaim> bedsByVillager = new HashMap<>();
    private final Map<String, UUID> bedOwners = new HashMap<>();
    private final Map<UUID, String> residentNames = new HashMap<>();
    /** Name state captured at link time; managedName is null until the mod assigns a name. */
    private final Map<UUID, ResidentNameState> residentNameStates = new HashMap<>();
    /** Restores a resident's pre-link name when an unloaded villager next joins the level. */
    private final Map<UUID, ResidentNameState> pendingNameRestorations = new HashMap<>();
    /** Legacy queue from saves created before name-state records were introduced. */
    private final Set<UUID> pendingDefaultNameRemoval = new HashSet<>();
    /** PNG textures are persisted by villager UUID and removed whenever the resident is unlinked. */
    private final Map<UUID, ResidentSkin> residentSkins = new HashMap<>();
    private final Map<UUID, Long> residentLinkedAtGameTime = new HashMap<>();
    private final Map<String, Long> lastAcceptedDawnGameTime = new HashMap<>();
    public record BedClaim(ResourceKey<Level> dimension, BlockPos home, BlockPos bed) {}
    public record ResidentNameState(String originalName, boolean originalVisible, String managedName) {}
    public record ResidentSkin(String key, byte[] png, SkinApplicationMode mode) {
        public ResidentSkin(String key, byte[] png) { this(key, png, SkinApplicationMode.COMPLETE); }
        public ResidentSkin { png = png.clone(); }
        @Override public byte[] png() { return png.clone(); }
    }
    private static String key(ResourceKey<Level> dim, BlockPos pos) { return dim.location()+"|"+pos.asLong(); }
    public BedClaim bedClaim(UUID villager){return bedsByVillager.get(villager);}
    public boolean isBedReserved(ResourceKey<Level> dim,BlockPos bed,UUID villager){UUID owner=bedOwners.get(key(dim,bed));return owner!=null&&!owner.equals(villager);}
    public boolean reserveBed(UUID villager,ResourceKey<Level> dim,BlockPos home,BlockPos bed){String k=key(dim,bed);UUID owner=bedOwners.get(k);if(owner!=null&&!owner.equals(villager))return false;releaseBed(villager);bedsByVillager.put(villager,new BedClaim(dim,home.immutable(),bed.immutable()));bedOwners.put(k,villager);setDirty();return true;}
    public void releaseBed(UUID villager){BedClaim old=bedsByVillager.remove(villager);if(old!=null){bedOwners.remove(key(old.dimension(),old.bed()),villager);setDirty();}}
    public static ResidenceData get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(ResidenceData::load, ResidenceData::new, NAME); }
    public Home home(ResourceKey<Level> dim, BlockPos pos) {
        String k=key(dim,pos); Home h=homes.get(k);
        if(h==null){h=new Home(dim,pos);homes.put(k,h);setDirty();}
        return h;
    }
    public Home home(ServerLevel level, BlockPos pos) { return home(level.dimension(),pos); }
    public Home owner(UUID villager) { String k=owners.get(villager); return k==null?null:homes.get(k); }
    public int size(Home h) { return h.size; }
    public String residentName(UUID id) { return residentNames.getOrDefault(id, "Aldeão"); }
    public ResidentSkin residentSkin(UUID id) { ResidentSkin skin=residentSkins.get(id);return skin==null?null:new ResidentSkin(skin.key(),skin.png(),skin.mode()); }
    public void setResidentSkin(UUID id,String key,byte[] png,SkinApplicationMode mode) {
        if(key==null||key.isBlank()||png==null||png.length==0)residentSkins.remove(id);
        else residentSkins.put(id,new ResidentSkin(key,png,mode==null?SkinApplicationMode.COMPLETE:mode));
        setDirty();
    }
    /** Changes the mode without retransmitting an existing image. */
    public boolean setResidentSkinMode(UUID id,String key,SkinApplicationMode mode) {
        ResidentSkin existing=residentSkins.get(id);
        if(existing==null||!existing.key().equals(key))return false;
        residentSkins.put(id,new ResidentSkin(existing.key(),existing.png(),mode==null?SkinApplicationMode.COMPLETE:mode));
        setDirty();
        return true;
    }
    public void applyPendingNameRestoration(Villager villager) {
        UUID id=villager.getUUID();ResidentNameState state=pendingNameRestorations.remove(id);
        if(state!=null){restoreName(villager,state);setDirty();}
        // Older saves queued only IDs known to have received a mod-generated name.
        if(pendingDefaultNameRemoval.remove(id)){villager.setCustomName(null);villager.setCustomNameVisible(false);setDirty();}
    }
    public void applyResidentName(Villager villager) {
        UUID id=villager.getUUID();
        if(owner(id)==null)return;
        ResidentNameState state=residentNameStates.get(id);
        String name=state!=null&&state.managedName()!=null?state.managedName():residentName(id);
        if(!villager.hasCustomName()||!name.equals(villager.getCustomName().getString()))villager.setCustomName(net.minecraft.network.chat.Component.literal(name));
        villager.setCustomNameVisible(true);
    }
    private void setResidentName(UUID id, String name) { residentNames.put(id,cleanName(name));setDirty(); }
    public void renameResident(UUID id,String name){String clean=cleanName(name);residentNames.put(id,clean);ResidentNameState state=residentNameStates.get(id);if(state!=null)residentNameStates.put(id,new ResidentNameState(state.originalName(),state.originalVisible(),clean));setDirty();}
    private static String cleanName(String name){String clean=name==null?"":name.trim();if(clean.isEmpty())clean="Aldeão";return clean.length()>64?clean.substring(0,64):clean;}
    public boolean setSize(Home h, int size) {
        int i=index(size); if(i<0 || h.residents.size()>CAPACITIES[i]) return false;
        if(h.size!=size)for(UUID id:new ArrayList<>(h.residents))releaseBed(id); h.size=size; setDirty(); return true;
    }
    public int capacity(Home h) { int i=index(h.size); return i<0?1:CAPACITIES[i]; }
    private static int index(int size){for(int i=0;i<DIMENSIONS.length;i++)if(DIMENSIONS[i]==size)return i;return -1;}
    public String link(ServerLevel level, BlockPos pos, Villager v) {
        Home h=home(level,pos); Home existing=owner(v.getUUID());
        if(existing!=null) return "already";
        if(h.residents.size()>=capacity(h)) return "full";
        UUID id=v.getUUID();h.residents.add(id);owners.put(id,key(level.dimension(),pos));residentLinkedAtGameTime.put(id,level.getServer().overworld().getGameTime());
        String originalName=v.hasCustomName()?v.getCustomName().getString():null;boolean originalVisible=v.isCustomNameVisible();
        String displayName=originalName==null?"Aldeão":originalName;
        residentNameStates.put(id,new ResidentNameState(originalName,originalVisible,originalName==null?displayName:null));
        setResidentName(id,displayName);
        if(originalName==null)v.setCustomName(net.minecraft.network.chat.Component.literal(displayName));
        v.setCustomNameVisible(true);
        setDirty(); return "ok";
    }
    public void unlink(ServerLevel level, UUID id) {
        unlink(level,id,level.getEntity(id) instanceof LivingEntity living?living:null);
    }
    /** Removes a resident after vanilla entity conversion, applying only its tracked mod name to the replacement. */
    public void unlinkConverted(ServerLevel level,Villager original,LivingEntity replacement){
        unlink(level,original.getUUID(),replacement);
    }
    private void unlink(ServerLevel level,UUID id,LivingEntity entity) {
        ResidentNameState state=residentNameStates.get(id);
        if(state!=null){if(entity!=null)restoreName(entity,state);else pendingNameRestorations.put(id,state);}
        else if(pendingDefaultNameRemoval.remove(id)){
            ResidentNameState legacyDefault=new ResidentNameState(null,false,residentName(id));
            if(entity!=null)restoreName(entity,legacyDefault);else pendingNameRestorations.put(id,legacyDefault);
        }
        if(residentSkins.remove(id)!=null)br.com.residenciasaldeoes.network.ModNetwork.clearSkin(id);
        unlink(id);
    }
    public void unlink(UUID id) { releaseBed(id);residentSkins.remove(id);residentLinkedAtGameTime.remove(id);Home h=owner(id); if(h!=null){h.residents.remove(id);owners.remove(id);residentNames.remove(id);residentNameStates.remove(id);setDirty();} }
    private static void restoreName(LivingEntity villager,ResidentNameState state){
        String currentName=villager.hasCustomName()?villager.getCustomName().getString():null;
        String expected=state.managedName()!=null?state.managedName():state.originalName();
        if(!Objects.equals(currentName,expected))return;
        if(state.originalName()!=null){villager.setCustomName(net.minecraft.network.chat.Component.literal(state.originalName()));villager.setCustomNameVisible(state.originalVisible());}
        else{villager.setCustomName(null);villager.setCustomNameVisible(false);}
    }
    public void unlinkAll(ServerLevel level, ResourceKey<Level> dim, BlockPos pos) {
        Home h=homes.get(key(dim,pos)); if(h!=null) for(UUID id:new ArrayList<>(h.residents)) unlink(level,id);
        unlinkAll(dim,pos);
    }
    /** Removes resident links while preserving this anchor's identity and configuration. */
    public void unlinkResidents(ServerLevel level, ResourceKey<Level> dim, BlockPos pos) {
        Home h=homes.get(key(dim,pos));
        if(h==null)return;
        for(UUID id:new ArrayList<>(h.residents))unlink(level,id);
        // Keep the Home record (including size) in SavedData. Only resident-related
        // records are changed; the anchor block entity and per-player preview stay intact.
        setDirty();
    }
    public void unlinkAll(ResourceKey<Level> dim, BlockPos pos) { Home h=homes.remove(key(dim,pos)); if(h!=null){for(UUID id:new ArrayList<>(h.residents)){owners.remove(id);releaseBed(id);residentNames.remove(id);residentNameStates.remove(id);residentLinkedAtGameTime.remove(id);if(residentSkins.remove(id)!=null)br.com.residenciasaldeoes.network.ModNetwork.clearSkin(id);}setDirty();} }
    public int count(ResourceKey<Level> dim, BlockPos pos) { Home h=homes.get(key(dim,pos)); return h==null?0:h.residents.size(); }
    public Set<UUID> residents(ResourceKey<Level> dim, BlockPos pos) { Home h=homes.get(key(dim,pos)); return h==null?Set.of():Collections.unmodifiableSet(h.residents); }
    public Collection<Home> homes(){return homes.values();}
    public long residentLinkedAtGameTime(UUID id,long currentGameTime){Long linked=residentLinkedAtGameTime.get(id);if(linked==null){residentLinkedAtGameTime.put(id,currentGameTime);setDirty();return currentGameTime;}return linked;}
    public boolean acceptProductionDawn(ResourceKey<Level> dimension,long gameTime){String dimensionKey=dimension.location().toString();Long previous=lastAcceptedDawnGameTime.get(dimensionKey);if(previous!=null&&(gameTime<=previous||gameTime-previous<MIN_DAWN_INTERVAL_TICKS))return false;lastAcceptedDawnGameTime.put(dimensionKey,gameTime);setDirty();return true;}
    public void queueProductionDawn(Home home,long gameTime){if(!home.pendingProductionDawns.contains(gameTime)){home.pendingProductionDawns.add(gameTime);home.pendingProductionDawns.sort(Long::compare);setDirty();}}
    public void completeProductionDawn(Home home,long gameTime){if(home.pendingProductionDawns.remove(gameTime))setDirty();}
    @Override public CompoundTag save(CompoundTag root) {
        ListTag list=new ListTag();
        for(var e:homes.entrySet()){Home h=e.getValue();CompoundTag t=new CompoundTag();t.putString("dimension",h.dimension.location().toString());t.putLong("pos",h.pos.asLong());t.putInt("size",h.size);ListTag production=new ListTag();for(long dawn:h.pendingProductionDawns)production.add(net.minecraft.nbt.LongTag.valueOf(dawn));t.put("pending_production_dawns",production);ListTag ids=new ListTag();for(UUID id:h.residents){CompoundTag u=new CompoundTag();u.putUUID("id",id);u.putString("name",residentName(id));Long linkedAt=residentLinkedAtGameTime.get(id);if(linkedAt!=null)u.putLong("linked_at_game_time",linkedAt);ResidentNameState state=residentNameStates.get(id);if(state!=null){CompoundTag ns=new CompoundTag();ns.putBoolean("has_original_name",state.originalName()!=null);if(state.originalName()!=null)ns.putString("original_name",state.originalName());ns.putBoolean("original_visible",state.originalVisible());ns.putBoolean("has_managed_name",state.managedName()!=null);if(state.managedName()!=null)ns.putString("managed_name",state.managedName());u.put("name_state",ns);}u.putBoolean("mod_default_name",state!=null&&state.originalName()==null&&state.managedName()!=null);ResidentSkin skin=residentSkins.get(id);if(skin!=null){u.putString("skin_key",skin.key());if(isZip(skin.png()))u.putByteArray("skin_bundle",skin.png());else u.putByteArray("skin_png",skin.png());u.putString("skin_mode",skin.mode().name());}ids.add(u);}t.put("residents",ids);list.add(t);}
        root.put("homes",list);CompoundTag dawns=new CompoundTag();lastAcceptedDawnGameTime.forEach(dawns::putLong);root.put("last_accepted_production_dawn",dawns);ListTag claims=new ListTag();for(var e:bedsByVillager.entrySet()){BedClaim c=e.getValue();CompoundTag t=new CompoundTag();t.putUUID("villager",e.getKey());t.putString("dimension",c.dimension().location().toString());t.putLong("home",c.home().asLong());t.putLong("bed",c.bed().asLong());claims.add(t);}root.put("bed_claims",claims);ListTag pending=new ListTag();for(var e:pendingNameRestorations.entrySet()){CompoundTag t=new CompoundTag();t.putUUID("id",e.getKey());writeNameState(t,e.getValue());pending.add(t);}root.put("pending_name_restorations",pending);ListTag legacyPending=new ListTag();for(UUID id:pendingDefaultNameRemoval){CompoundTag t=new CompoundTag();t.putUUID("id",id);legacyPending.add(t);}root.put("pending_name_removal",legacyPending);return root;
    }
    private static void writeNameState(CompoundTag t,ResidentNameState state){t.putBoolean("has_original_name",state.originalName()!=null);if(state.originalName()!=null)t.putString("original_name",state.originalName());t.putBoolean("original_visible",state.originalVisible());t.putBoolean("has_managed_name",state.managedName()!=null);if(state.managedName()!=null)t.putString("managed_name",state.managedName());}
    private static ResidentNameState readNameState(CompoundTag t){String original=t.getBoolean("has_original_name")?t.getString("original_name"):null;String managed=t.getBoolean("has_managed_name")?t.getString("managed_name"):null;return new ResidentNameState(original,t.getBoolean("original_visible"),managed);}
    public static ResidenceData load(CompoundTag root) {
        ResidenceData d=new ResidenceData(); ListTag list=root.getList("homes",Tag.TAG_COMPOUND);
        for(int n=0;n<list.size();n++){CompoundTag t=list.getCompound(n);try{ResourceKey<Level> dim=ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY,new net.minecraft.resources.ResourceLocation(t.getString("dimension")));Home h=new Home(dim,BlockPos.of(t.getLong("pos")));h.size=index(t.getInt("size"))<0?3:t.getInt("size");ListTag production=t.getList("pending_production_dawns",Tag.TAG_LONG);for(int j=0;j<production.size();j++)h.pendingProductionDawns.add(((net.minecraft.nbt.LongTag)production.get(j)).getAsLong());ListTag ids=t.getList("residents",Tag.TAG_COMPOUND);for(int i=0;i<ids.size();i++){CompoundTag u=ids.getCompound(i);UUID id=u.getUUID("id");h.residents.add(id);d.owners.put(id,key(dim,h.pos));if(u.contains("linked_at_game_time",Tag.TAG_LONG))d.residentLinkedAtGameTime.put(id,u.getLong("linked_at_game_time"));String name=u.contains("name")?u.getString("name"):"Aldeão";d.residentNames.put(id,name);if(u.contains("name_state",Tag.TAG_COMPOUND))d.residentNameStates.put(id,readNameState(u.getCompound("name_state")));else if(u.getBoolean("mod_default_name"))d.residentNameStates.put(id,new ResidentNameState(null,false,name));else d.residentNameStates.put(id,new ResidentNameState(name,true,null));String skinKey=u.getString("skin_key");byte[] payload=u.contains("skin_bundle",Tag.TAG_BYTE_ARRAY)?u.getByteArray("skin_bundle"):u.getByteArray("skin_png");if(!skinKey.isEmpty()&&payload.length>0&&payload.length<=br.com.residenciasaldeoes.data.WardrobePackage.MAX_ARCHIVE_BYTES)d.residentSkins.put(id,new ResidentSkin(skinKey,payload,SkinApplicationMode.COMPLETE));}d.homes.put(key(dim,h.pos),h);}catch(Exception ignored){}}
        CompoundTag dawns=root.getCompound("last_accepted_production_dawn");for(String dimension:dawns.getAllKeys())d.lastAcceptedDawnGameTime.put(dimension,dawns.getLong(dimension));
        ListTag claims=root.getList("bed_claims",Tag.TAG_COMPOUND);for(int i=0;i<claims.size();i++){CompoundTag t=claims.getCompound(i);try{UUID id=t.getUUID("villager");ResourceKey<Level> dim=ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY,new net.minecraft.resources.ResourceLocation(t.getString("dimension")));BlockPos home=BlockPos.of(t.getLong("home")),bed=BlockPos.of(t.getLong("bed"));if(d.owner(id)!=null&&d.bedOwners.putIfAbsent(key(dim,bed),id)==null)d.bedsByVillager.put(id,new BedClaim(dim,home,bed));}catch(Exception ignored){}}ListTag pending=root.getList("pending_name_restorations",Tag.TAG_COMPOUND);for(int i=0;i<pending.size();i++)try{CompoundTag t=pending.getCompound(i);d.pendingNameRestorations.put(t.getUUID("id"),readNameState(t));}catch(Exception ignored){}ListTag legacy=root.getList("pending_name_removal",Tag.TAG_COMPOUND);for(int i=0;i<legacy.size();i++)try{d.pendingDefaultNameRemoval.add(legacy.getCompound(i).getUUID("id"));}catch(Exception ignored){}return d;
    }
    private static boolean isZip(byte[] data){return data.length>3&&data[0]=='P'&&data[1]=='K';}
}
