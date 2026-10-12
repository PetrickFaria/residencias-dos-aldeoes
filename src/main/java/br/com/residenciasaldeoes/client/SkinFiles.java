package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.WardrobePackage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** File gallery for compatible villager PNGs and validated wardrobe ZIPs. */
public final class SkinFiles {
    public static final int MAX_BYTES=WardrobePackage.MAX_ARCHIVE_BYTES;
    private SkinFiles(){}
    public record SkinFile(String id,String label,Path path,ResourceLocation resource){}
    public static Path directory(){return FMLPaths.CONFIGDIR.get().resolve(ResidenciasDosAldeoes.MODID).resolve("skins");}
    public static List<SkinFile> list(){
        try{Path dir=directory();Files.createDirectories(dir);Map<String,SkinFile> found=new LinkedHashMap<>();
            try(var paths=Files.list(dir)){paths.filter(Files::isRegularFile).filter(p->{String n=p.toString().toLowerCase(Locale.ROOT);return n.endsWith(".png")||n.endsWith(".zip");}).forEach(p->{String filename=p.getFileName().toString();String id=filename.substring(0,filename.lastIndexOf('.'));if(id.matches("[A-Za-z0-9._-]{1,64}")&&validFile(p))found.put(id,new SkinFile(id,id.replace('_',' '),p,null));});}
            try{Minecraft.getInstance().getResourceManager().listResources("skins",loc->loc.getNamespace().equals(ResidenciasDosAldeoes.MODID)&&loc.getPath().matches("skins/[A-Za-z0-9._-]{1,64}\\.png")).forEach((loc,res)->{String id=loc.getPath().substring(6,loc.getPath().length()-4);if(!found.containsKey(id)&&validPng(res))found.put(id,new SkinFile(id,id.replace('_',' '),null,loc));});}catch(RuntimeException ignored){}
            List<SkinFile> result=new ArrayList<>(found.values());result.sort(Comparator.comparing(SkinFile::label,String.CASE_INSENSITIVE_ORDER));return List.copyOf(result);
        }catch(IOException e){return List.of();}
    }
    public static byte[] read(SkinFile file)throws IOException{byte[] bytes;if(file.path()!=null){long n=Files.size(file.path());if(n<=0||n>MAX_BYTES)throw new IOException("Arquivo acima do limite de 1,5 MB");bytes=Files.readAllBytes(file.path());}else if(file.resource()!=null){Resource r=Minecraft.getInstance().getResourceManager().getResource(file.resource()).orElseThrow(()->new IOException("Skin não encontrada"));try(InputStream in=r.open()){bytes=in.readNBytes(MAX_BYTES+1);}}else throw new IOException("Origem do arquivo indisponível");
        if(bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("Arquivo acima do limite de 1,5 MB");if(isZip(bytes)){WardrobePackage.parse(bytes);return bytes;}if(!validPng(bytes))throw new IOException("Use PNG quadrado 64, 128, 256 ou 512");try(var img=com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(bytes))){if(img.getWidth()!=img.getHeight()||!Set.of(64,128,256,512).contains(img.getWidth()))throw new IOException("Dimensões PNG inválidas");}return bytes;}
    private static boolean validFile(Path p){try{if(p.toString().toLowerCase(Locale.ROOT).endsWith(".zip")){long n=Files.size(p);if(n<4||n>MAX_BYTES)return false;WardrobePackage.parse(Files.readAllBytes(p));return true;}try(InputStream in=Files.newInputStream(p)){return validPng(in.readNBytes(24));}}catch(Exception e){return false;}}
    private static boolean validPng(Resource r){try(InputStream in=r.open()){return validPng(in.readNBytes(24));}catch(IOException e){return false;}}
    private static boolean validPng(byte[] b){if(b.length<24||b[0]!=(byte)137||b[1]!=80||b[2]!=78||b[3]!=71||b[12]!='I'||b[13]!='H'||b[14]!='D'||b[15]!='R')return false;int w=readInt(b,16),h=readInt(b,20);return w==h&&Set.of(64,128,256,512).contains(w);}
    private static boolean isZip(byte[] b){return b.length>3&&b[0]=='P'&&b[1]=='K';}
    private static int readInt(byte[] b,int i){return(b[i]&255)<<24|(b[i+1]&255)<<16|(b[i+2]&255)<<8|(b[i+3]&255);}
}
