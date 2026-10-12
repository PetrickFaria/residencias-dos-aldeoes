package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.SkinApplicationMode;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.geom.ModelPart;
import br.com.residenciasaldeoes.data.WardrobePackage;
import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client-side dynamic textures keyed by villager UUID; no entity model or AI is replaced. */
public final class VillagerSkinTextures {
    private static final Map<UUID, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<UUID, byte[]> PNGS = new HashMap<>();
    private static final Map<UUID, SkinApplicationMode> MODES = new HashMap<>();
    private static final Map<UUID, WardrobePackage> WARDROBES = new HashMap<>();
    private static final Map<UUID, Map<String,ResourceLocation>> EXTRA_TEXTURES = new HashMap<>();
    public record PreparedPart(String anchor,ModelPart model,String texture) {}
    private static final Map<UUID,List<PreparedPart>> OUTER_PARTS=new HashMap<>(), ACCESSORIES=new HashMap<>();
    private VillagerSkinTextures() {}

    public static void receive(UUID resident, String key, byte[] png) {
        receive(resident, key, png, SkinApplicationMode.COMPLETE);
    }

    public static void receive(UUID resident, String key, byte[] png, SkinApplicationMode mode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (key == null || key.isEmpty() || png == null || png.length == 0) {
            clear(resident);
            return;
        }
        if (isZip(png)) { receiveBundle(resident,key,png,mode); return; }
        NativeImage image = null;
        DynamicTexture dynamic = null;
        try {
            image = NativeImage.read(new ByteArrayInputStream(png));
            if (image.getWidth() != image.getHeight() || !java.util.Set.of(64,128,256,512).contains(image.getWidth())) return;
            if(image.getWidth()!=64){NativeImage scaled=new NativeImage(64,64,true);for(int y=0;y<64;y++)for(int x=0;x<64;x++)scaled.setPixelRGBA(x,y,image.getPixelRGBA(x*image.getWidth()/64,y*image.getHeight()/64));image.close();image=scaled;}
            // Keep the source RGBA values unchanged. Transparent atlas pixels must stay
            // transparent so they neither turn black nor erase the vanilla base texture.
            ResourceLocation location = textureLocation(resident);
            dynamic = new DynamicTexture(image);
            image = null; // DynamicTexture owns the image from this point.
            dynamic.setFilter(false, false); // nearest-neighbor, no mipmaps: preserve 64x64 pixel art.
            minecraft.getTextureManager().register(location, dynamic);
            dynamic = null; // TextureManager owns it and replaces/closes a prior texture at this ID.
            TEXTURES.put(resident, location);
            PNGS.put(resident, png.clone());
            MODES.put(resident, mode == null ? SkinApplicationMode.COMPLETE : mode);
            WARDROBES.remove(resident);releaseExtras(resident);OUTER_PARTS.remove(resident);ACCESSORIES.remove(resident);
        } catch (Exception ignored) {
            // Keep the last valid skin if a new PNG is corrupt or unsupported.
        } finally {
            if (image != null) image.close();
            if (dynamic != null) dynamic.close();
        }
    }

    private static boolean isZip(byte[] b){return b!=null&&b.length>3&&b[0]=='P'&&b[1]=='K';}
    private static void receiveBundle(UUID id,String key,byte[] bytes,SkinApplicationMode mode){
        try{
            WardrobePackage pack=WardrobePackage.parse(bytes);releaseExtras(id);Map<String,ResourceLocation> textures=new HashMap<>();
            ResourceLocation base=registerPng(id,"base",pack.texture(pack.baseTexture()),true);
            for(String path:allTexturePaths(pack))textures.put(path,registerPng(id,path,pack.texture(path),false));
            TEXTURES.put(id,base);PNGS.put(id,bytes.clone());MODES.put(id,mode==null?SkinApplicationMode.COMPLETE:mode);WARDROBES.put(id,pack);EXTRA_TEXTURES.put(id,textures);
            OUTER_PARTS.put(id,prepare(pack.outerParts(),textures));ACCESSORIES.put(id,prepare(pack.accessories(),textures));
        }catch(Exception ignored){/* Invalid package leaves the last valid appearance untouched. */}
    }
    private static java.util.Set<String> allTexturePaths(WardrobePackage p){java.util.Set<String> out=new java.util.HashSet<>();if(p.outerTexture()!=null)out.add(p.outerTexture());for(var part:p.outerParts())out.add(part.texture());for(var part:p.accessories())out.add(part.texture());return out;}
    private static ResourceLocation registerPng(UUID id,String path,byte[] data,boolean base)throws Exception{
        NativeImage image=NativeImage.read(new ByteArrayInputStream(data));if(image.getWidth()!=image.getHeight()||!java.util.Set.of(64,128,256,512).contains(image.getWidth())){image.close();throw new IllegalArgumentException("texture dimensions");}
        if(image.getWidth()!=64){NativeImage scaled=new NativeImage(64,64,true);for(int y=0;y<64;y++)for(int x=0;x<64;x++)scaled.setPixelRGBA(x,y,image.getPixelRGBA(x*image.getWidth()/64,y*image.getHeight()/64));image.close();image=scaled;}
        DynamicTexture dynamic=new DynamicTexture(image);dynamic.setFilter(false,false);ResourceLocation loc=textureLocation(id,base?"base":path);Minecraft.getInstance().getTextureManager().register(loc,dynamic);return loc;
    }
    private static List<PreparedPart> prepare(List<WardrobePackage.Part> parts,Map<String,ResourceLocation> textures){List<PreparedPart> out=new ArrayList<>();int n=0;for(var p:parts){byte[] png=null;try{ // dimensions are read from the already registered PNG resource via package textures in the caller
                MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();CubeListBuilder cubes=CubeListBuilder.create();
                for(var c:p.cubes()){int scale=1; cubes.texOffs(c.u()*scale,c.v()*scale).addBox(c.x(),c.y(),c.z(),c.width(),c.height(),c.depth(),CubeDeformation.NONE);}
                root.addOrReplaceChild("piece",cubes,PartPose.offsetAndRotation(p.px(),p.py(),p.pz(),p.rx(),p.ry(),p.rz()));
                ModelPart model=LayerDefinition.create(mesh,64,64).bakeRoot().getChild("piece");out.add(new PreparedPart(p.anchor(),model,p.texture()));
            }catch(Exception ignored){}n++;}return List.copyOf(out);}
    private static ResourceLocation textureLocation(UUID id,String path){String safe=path.replace('/','_').replace('.','_');return new ResourceLocation(ResidenciasDosAldeoes.MODID,"resident_skins/"+id.toString().replace("-","")+"_"+safe);}
    private static void releaseExtras(UUID id){var map=EXTRA_TEXTURES.remove(id);if(map!=null)for(var loc:map.values())Minecraft.getInstance().getTextureManager().release(loc);}

    public static ResourceLocation get(UUID resident) { return TEXTURES.get(resident); }
    public static SkinApplicationMode mode(UUID resident) { return MODES.getOrDefault(resident, SkinApplicationMode.COMPLETE); }
    public static boolean isComplete(UUID resident) { return TEXTURES.containsKey(resident) && mode(resident) == SkinApplicationMode.COMPLETE; }
    public static byte[] getPng(UUID resident) { byte[] png=PNGS.get(resident);return png==null?null:png.clone(); }
    public static WardrobePackage wardrobe(UUID resident){return WARDROBES.get(resident);}
    public static ResourceLocation extraTexture(UUID resident,String name){Map<String,ResourceLocation> m=EXTRA_TEXTURES.get(resident);return m==null?null:m.get(name);}
    public static List<PreparedPart> outerParts(UUID id){return OUTER_PARTS.getOrDefault(id,List.of());} public static List<PreparedPart> accessories(UUID id){return ACCESSORIES.getOrDefault(id,List.of());}
    public static void clear(UUID resident) { Minecraft minecraft=Minecraft.getInstance();ResourceLocation texture=TEXTURES.remove(resident);if(texture!=null)minecraft.getTextureManager().release(texture);releaseExtras(resident);PNGS.remove(resident);MODES.remove(resident);WARDROBES.remove(resident);OUTER_PARTS.remove(resident);ACCESSORIES.remove(resident); }

    public static void clearAll() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : TEXTURES.values()) minecraft.getTextureManager().release(texture);
        TEXTURES.clear();
        for(var id:new java.util.ArrayList<>(EXTRA_TEXTURES.keySet()))releaseExtras(id);
        WARDROBES.clear();OUTER_PARTS.clear();ACCESSORIES.clear();
        PNGS.clear();
        MODES.clear();
    }

    private static ResourceLocation textureLocation(UUID id) {return textureLocation(id,"base");}
}
