package br.com.residenciasaldeoes.network;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.block.ResidentialAnchorBlockEntity;
import br.com.residenciasaldeoes.data.ResidenceData;
import br.com.residenciasaldeoes.data.SkinApplicationMode;
import br.com.residenciasaldeoes.data.WardrobePackage;
import br.com.residenciasaldeoes.menu.AnchorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.function.Supplier;

public final class ModNetwork {
 private static final String VERSION="9";
 public static final int MAX_SKIN_BYTES=WardrobePackage.MAX_ARCHIVE_BYTES;
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ResidenciasDosAldeoes.MODID,"main"),()->VERSION,VERSION::equals,VERSION::equals);
 private static int id; private ModNetwork(){}
 public static void register(){
  CHANNEL.messageBuilder(Action.class,id++).encoder(Action::encode).decoder(Action::decode).consumerMainThread(Action::handle).add();
  CHANNEL.messageBuilder(Previews.class,id++).encoder(Previews::encode).decoder(Previews::decode).consumerMainThread(Previews::handle).add();
  CHANNEL.messageBuilder(Residents.class,id++).encoder(Residents::encode).decoder(Residents::decode).consumerMainThread(Residents::handle).add();
  CHANNEL.messageBuilder(AssignSkin.class,id++).encoder(AssignSkin::encode).decoder(AssignSkin::decode).consumerMainThread(AssignSkin::handle).add();
  CHANNEL.messageBuilder(SkinSync.class,id++).encoder(SkinSync::encode).decoder(SkinSync::decode).consumerMainThread(SkinSync::handle).add();
  CHANNEL.messageBuilder(OpacitySync.class,id++).encoder(OpacitySync::encode).decoder(OpacitySync::decode).consumerMainThread(OpacitySync::handle).add();
 }
 public static void action(BlockPos p,int a,int v){CHANNEL.sendToServer(new Action(p,a,v,null,""));}
 public static void unlinkResident(BlockPos p,UUID id){CHANNEL.sendToServer(new Action(p,4,0,id,""));}
 public static void renameResident(BlockPos p,UUID id,String name){CHANNEL.sendToServer(new Action(p,5,0,id,name));}
 public static void unlinkAll(BlockPos p){CHANNEL.sendToServer(new Action(p,6,0,null,""));}
 public static void setPreview(BlockPos p,boolean enabled){CHANNEL.sendToServer(new Action(p,7,enabled?1:0,null,""));}
 public static void requestResidents(BlockPos p){CHANNEL.sendToServer(new Action(p,8,0,null,""));}
 public static void assignSkin(BlockPos pos,UUID resident,String key,byte[] png,SkinApplicationMode mode){CHANNEL.sendToServer(new AssignSkin(pos,resident,key,png,mode));}
 public static void requestSkin(BlockPos pos,UUID resident){CHANNEL.sendToServer(new Action(pos,9,0,resident,""));}
 public static void setResidentOpacity(BlockPos pos,UUID resident,int opacity){CHANNEL.sendToServer(new Action(pos,10,opacity,resident,""));}
 public static void broadcastSkin(UUID id,ResidenceData.ResidentSkin skin){if(skin==null){clearSkin(id);return;}CHANNEL.send(PacketDistributor.ALL.noArg(),new SkinSync(id,skin.key(),skin.png(),skin.mode()));}
 public static void clearSkin(UUID id){CHANNEL.send(PacketDistributor.ALL.noArg(),new SkinSync(id,"",new byte[0],SkinApplicationMode.COMPLETE));}
 public static void clearResidentAppearance(UUID id){clearSkin(id);CHANNEL.send(PacketDistributor.ALL.noArg(),new OpacitySync(id,-1));}
 public static void sendOpacity(ServerPlayer player,UUID id,int opacity){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new OpacitySync(id,opacity));}
 public static void broadcastOpacity(ServerLevel level,ServerPlayer sender,UUID id,int opacity){net.minecraft.world.entity.Entity entity=level.getEntity(id);if(entity!=null)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),new OpacitySync(id,opacity));else sendOpacity(sender,id,opacity);}
 public static void sendSkin(ServerPlayer player,UUID id,ResidenceData.ResidentSkin skin){if(skin!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new SkinSync(id,skin.key(),skin.png(),skin.mode()));}
 public static void sendResidents(ServerPlayer p,BlockPos pos,List<ResidentEntry> e){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Residents(p.level.dimension().location(),pos,e,previewEnabled(p,p.level.dimension().location(),pos)));}
 public static void sendPreviews(ServerPlayer p,List<PreviewEntry> e){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Previews(e));}
 public record ResidentEntry(UUID id,String name,boolean bed,boolean work,boolean chest,boolean food,String skinKey,SkinApplicationMode skinMode,int opacity){
  public ResidentEntry(UUID id,String name,boolean bed,boolean work,boolean chest){this(id,name,bed,work,chest,false,"",SkinApplicationMode.COMPLETE,100);}
  public ResidentEntry(UUID id,String name,boolean bed,boolean work,boolean chest,String skinKey){this(id,name,bed,work,chest,false,skinKey,SkinApplicationMode.COMPLETE,100);}
  public ResidentEntry(UUID id,String name,boolean bed,boolean work,boolean chest,String skinKey,SkinApplicationMode skinMode){this(id,name,bed,work,chest,false,skinKey,skinMode,100);}
 }
 public record PreviewEntry(ResourceLocation dimension,BlockPos pos,int size,boolean permanent){}
 public record Action(BlockPos pos,int action,int value,UUID resident,String text){
  void encode(FriendlyByteBuf b){b.writeBlockPos(pos);b.writeVarInt(action);b.writeVarInt(value);b.writeBoolean(resident!=null);if(resident!=null)b.writeUUID(resident);b.writeUtf(text,128);}
  static Action decode(FriendlyByteBuf b){BlockPos p=b.readBlockPos();int a=b.readVarInt(),v=b.readVarInt();UUID u=b.readBoolean()?b.readUUID():null;return new Action(p,a,v,u,b.readUtf(128));}
  void handle(Supplier<NetworkEvent.Context> c){ServerPlayer p=c.get().getSender();if(p==null)return;c.get().enqueueWork(()->{
   if(!(p.containerMenu instanceof AnchorMenu m)||!m.getPos().equals(pos)||!(p.level.getBlockEntity(pos) instanceof ResidentialAnchorBlockEntity))return;
   ResidentialAnchorBlockEntity be=(ResidentialAnchorBlockEntity)p.level.getBlockEntity(pos);ServerLevel sl=(ServerLevel)p.level;ResidenceData d=ResidenceData.get(sl);
   switch(action){
    case 0->{if(be.setSize(value))p.displayClientMessage(Component.translatable("message.residenciasaldeoes.size_set",value,value,value),true);else p.displayClientMessage(Component.translatable("message.residenciasaldeoes.size_blocked"),true);}
    case 1->{var t=p.getPersistentData();boolean on=t.getBoolean("residenciasaldeoes_register");t.putBoolean("residenciasaldeoes_register",!on);t.putBoolean("residenciasaldeoes_unlink",false);t.putLong("residenciasaldeoes_anchor",pos.asLong());t.putString("residenciasaldeoes_dimension",p.level.dimension().location().toString());p.displayClientMessage(Component.translatable(!on?"message.residenciasaldeoes.register_on":"message.residenciasaldeoes.register_off"),true);}
    case 2->{var t=p.getPersistentData();boolean on=t.getBoolean("residenciasaldeoes_unlink");t.putBoolean("residenciasaldeoes_unlink",!on);t.putBoolean("residenciasaldeoes_register",false);t.putLong("residenciasaldeoes_anchor",pos.asLong());t.putString("residenciasaldeoes_dimension",p.level.dimension().location().toString());p.displayClientMessage(Component.translatable(!on?"message.residenciasaldeoes.unlink_on":"message.residenciasaldeoes.unlink_off"),true);}
    case 4->{if(resident!=null){ResidenceData.Home h=d.owner(resident);if(h!=null&&h.dimension.equals(sl.dimension())&&h.pos.equals(pos)){d.unlink(sl,resident);p.displayClientMessage(Component.translatable("message.residenciasaldeoes.unlinked_one"),true);}}}
    case 5->{if(resident!=null){ResidenceData.Home h=d.owner(resident);if(h!=null&&h.dimension.equals(sl.dimension())&&h.pos.equals(pos)){String n=text.trim();if(!n.isEmpty()){d.renameResident(resident,n);if(sl.getEntity(resident) instanceof Villager v){v.setCustomName(Component.literal(n));v.setCustomNameVisible(true);}}}}}
    case 6->{d.unlinkResidents(sl,sl.dimension(),pos);p.displayClientMessage(Component.translatable("message.residenciasaldeoes.unlinked_all"),true);}
    case 7->{String key=previewKey(sl.dimension().location(),pos);p.getPersistentData().putBoolean(key,value!=0);br.com.residenciasaldeoes.VillagerResidenceEvents.syncPreviews(p);}
    case 8->m.sendResidentSnapshot(p);
    case 9->{if(resident!=null){ResidenceData.Home h=d.owner(resident);if(h!=null&&h.dimension.equals(sl.dimension())&&h.pos.equals(pos))sendSkin(p,resident,d.residentSkin(resident));}}
    case 10->{if(resident!=null){ResidenceData.Home h=d.owner(resident);if(h!=null&&h.dimension.equals(sl.dimension())&&h.pos.equals(pos)){int opacity=Math.max(0,Math.min(100,value));d.setResidentOpacity(resident,opacity);broadcastOpacity(sl,p,resident,opacity);m.sendResidentSnapshot(p);}}}
   }
  });c.get().setPacketHandled(true);}
 }
 private static String previewKey(ResourceLocation d,BlockPos p){return "residenciasaldeoes_preview_"+d.toString().replace(':','_')+"_"+p.asLong();}
 public static boolean previewEnabled(ServerPlayer p,ResourceLocation d,BlockPos pos){return p.getPersistentData().getBoolean(previewKey(d,pos));}
 public record Previews(List<PreviewEntry> entries){void encode(FriendlyByteBuf b){b.writeVarInt(entries.size());for(var e:entries){b.writeResourceLocation(e.dimension());b.writeBlockPos(e.pos());b.writeVarInt(e.size());b.writeBoolean(e.permanent());}}static Previews decode(FriendlyByteBuf b){int n=Math.min(256,b.readVarInt());List<PreviewEntry> a=new ArrayList<>(n);for(int i=0;i<n;i++)a.add(new PreviewEntry(b.readResourceLocation(),b.readBlockPos(),b.readVarInt(),b.readBoolean()));return new Previews(List.copyOf(a));}void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->{br.com.residenciasaldeoes.client.ResidencePreview.setEntries(entries);br.com.residenciasaldeoes.client.AnchorScreen.receivePreviewStates(entries);}));c.get().setPacketHandled(true);}}
 public record Residents(ResourceLocation dimension,BlockPos pos,List<ResidentEntry> entries,boolean preview){void encode(FriendlyByteBuf b){b.writeResourceLocation(dimension);b.writeBlockPos(pos);b.writeVarInt(entries.size());for(var e:entries){b.writeUUID(e.id());b.writeUtf(e.name(),128);b.writeBoolean(e.bed());b.writeBoolean(e.work());b.writeBoolean(e.chest());b.writeBoolean(e.food());b.writeUtf(e.skinKey(),64);b.writeBoolean(e.skinMode()==SkinApplicationMode.COMPLETE);b.writeByte(e.opacity());}b.writeBoolean(preview);}static Residents decode(FriendlyByteBuf b){ResourceLocation dimension=b.readResourceLocation();BlockPos p=b.readBlockPos();int n=Math.min(64,b.readVarInt());List<ResidentEntry> e=new ArrayList<>(n);for(int i=0;i<n;i++)e.add(new ResidentEntry(b.readUUID(),b.readUtf(128),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readUtf(64),b.readBoolean()?SkinApplicationMode.COMPLETE:SkinApplicationMode.PARTIAL,b.readUnsignedByte()));return new Residents(dimension,p,List.copyOf(e),b.readBoolean());}void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->br.com.residenciasaldeoes.client.AnchorScreen.receiveResidents(dimension,pos,entries,preview)));c.get().setPacketHandled(true);}}
 public record AssignSkin(BlockPos pos,UUID resident,String key,byte[] png,SkinApplicationMode mode){
  void encode(FriendlyByteBuf b){b.writeBlockPos(pos);b.writeUUID(resident);b.writeUtf(key,64);b.writeByteArray(png);b.writeBoolean(mode==SkinApplicationMode.COMPLETE);}
  static AssignSkin decode(FriendlyByteBuf b){BlockPos p=b.readBlockPos();UUID id=b.readUUID();String key=b.readUtf(64);byte[] png=b.readByteArray(MAX_SKIN_BYTES);return new AssignSkin(p,id,key,png,b.readBoolean()?SkinApplicationMode.COMPLETE:SkinApplicationMode.PARTIAL);}
  void handle(Supplier<NetworkEvent.Context> c){ServerPlayer p=c.get().getSender();if(p==null)return;c.get().enqueueWork(()->{
   if(!(p.containerMenu instanceof AnchorMenu m)||!m.getPos().equals(pos)||!(p.level.getBlockEntity(pos) instanceof ResidentialAnchorBlockEntity))return;
   ServerLevel level=(ServerLevel)p.level;ResidenceData data=ResidenceData.get(level);ResidenceData.Home home=data.owner(resident);
   if(home==null||!home.dimension.equals(level.dimension())||!home.pos.equals(pos))return;
   if(key.isEmpty()&&png.length==0){data.setResidentSkin(resident,"",new byte[0],mode);clearSkin(resident);m.sendResidentSnapshot(p);return;}
   if(!key.matches("[A-Za-z0-9._-]{1,64}"))return;
   if(png.length==0){if(!data.setResidentSkinMode(resident,key,mode))return;}
   else {if(!validSkinPayload(png))return;data.setResidentSkin(resident,key,png,mode);}
   broadcastSkin(resident,data.residentSkin(resident));m.sendResidentSnapshot(p);
  });c.get().setPacketHandled(true);}
 }
 private static boolean validSkinPayload(byte[] png){
  if(png!=null&&png.length>3&&png[0]=='P'&&png[1]=='K'){try{WardrobePackage.parse(png);return true;}catch(Exception invalid){return false;}}
  if(png==null||png.length<24||png.length>MAX_SKIN_BYTES)return false;
  byte[] signature={(byte)137,80,78,71,13,10,26,10};for(int i=0;i<8;i++)if(png[i]!=signature[i])return false;
  int w=readInt(png,16),h=readInt(png,20);if(!(png[12]=='I'&&png[13]=='H'&&png[14]=='D'&&png[15]=='R'&&w==h&&Set.of(64,128,256,512).contains(w)))return false;
  try{var image=ImageIO.read(new ByteArrayInputStream(png));return image!=null&&image.getWidth()==w&&image.getHeight()==h;}catch(Exception invalid){return false;}
 }
 private static int readInt(byte[] b,int i){return (b[i]&255)<<24|(b[i+1]&255)<<16|(b[i+2]&255)<<8|(b[i+3]&255);}
 public record OpacitySync(UUID resident,int opacity){
  void encode(FriendlyByteBuf b){b.writeUUID(resident);b.writeByte(opacity);}
  static OpacitySync decode(FriendlyByteBuf b){return new OpacitySync(b.readUUID(),b.readByte());}
  void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->{if(opacity<0)br.com.residenciasaldeoes.client.ResidentOpacity.clear(resident);else br.com.residenciasaldeoes.client.ResidentOpacity.set(resident,opacity);}));c.get().setPacketHandled(true);}
 }

 public record SkinSync(UUID resident,String key,byte[] png,SkinApplicationMode mode){
  void encode(FriendlyByteBuf b){b.writeUUID(resident);b.writeUtf(key,64);b.writeByteArray(png);b.writeBoolean(mode==SkinApplicationMode.COMPLETE);}
  static SkinSync decode(FriendlyByteBuf b){return new SkinSync(b.readUUID(),b.readUtf(64),b.readByteArray(MAX_SKIN_BYTES),b.readBoolean()?SkinApplicationMode.COMPLETE:SkinApplicationMode.PARTIAL);}
  void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->{br.com.residenciasaldeoes.client.VillagerSkinTextures.receive(resident,key,png,mode);br.com.residenciasaldeoes.client.AnchorScreen.skinReceived(resident);}));c.get().setPacketHandled(true);}
 }
}
