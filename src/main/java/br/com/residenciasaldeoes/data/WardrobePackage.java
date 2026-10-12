package br.com.residenciasaldeoes.data;

import com.google.gson.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Validated, declarative villager wardrobe package. No code or arbitrary paths are loaded from a pack. */
public final class WardrobePackage {
    public static final int MAX_ARCHIVE_BYTES = 1_500_000;
    private static final int MAX_TOTAL_UNPACKED = 3_000_000;
    private final byte[] archive;
    private final String baseTexture;
    private final String outerTexture;
    private final List<Part> outerParts;
    private final List<Part> accessories;
    private final Map<String, byte[]> textures;

    public record Cube(float x,float y,float z,float width,float height,float depth,int u,int v) {}
    public record Part(String anchor,float px,float py,float pz,float rx,float ry,float rz,String texture,List<Cube> cubes) {}

    private WardrobePackage(byte[] archive,String base,String outer,List<Part> outerParts,List<Part> accessories,Map<String,byte[]> textures){
        this.archive=archive.clone();this.baseTexture=base;this.outerTexture=outer;this.outerParts=List.copyOf(outerParts);this.accessories=List.copyOf(accessories);
        Map<String,byte[]> copy=new HashMap<>();textures.forEach((k,v)->copy.put(k,v.clone()));this.textures=Map.copyOf(copy);
    }
    public byte[] archive(){return archive.clone();} public String baseTexture(){return baseTexture;} public String outerTexture(){return outerTexture;}
    public List<Part> outerParts(){return outerParts;} public List<Part> accessories(){return accessories;}
    public byte[] texture(String name){byte[] value=textures.get(name);return value==null?null:value.clone();}

    public static WardrobePackage parse(byte[] bytes) throws IOException {
        if(bytes==null||bytes.length<4||bytes.length>MAX_ARCHIVE_BYTES||bytes[0]!='P'||bytes[1]!='K')throw new IOException("Pacote ZIP inválido ou acima do limite");
        Map<String,byte[]> files=new HashMap<>();int total=0,entries=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){
            if(++entries>64)throw new IOException("Quantidade de entradas ZIP excedida");
            String name=entry.getName();if(entry.isDirectory())continue;
            if(name.startsWith("/")||name.contains("\\")||Arrays.asList(name.split("/")).contains("..")||files.containsKey(name)||files.size()>=32)throw new IOException("Caminho ou quantidade de arquivos inválida no pacote");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
            while((n=zip.read(buf))!=-1){total+=n;if(total>MAX_TOTAL_UNPACKED)throw new IOException("Conteúdo descompactado acima do limite");out.write(buf,0,n);}files.put(name,out.toByteArray());zip.closeEntry();
        }}catch(ZipException invalid){throw new IOException("Arquivo ZIP inválido",invalid);}
        byte[] json=files.get("manifest.json");if(json==null||json.length>32768)throw new IOException("manifest.json ausente ou excessivo");
        try {
            JsonObject root=JsonParser.parseString(new String(json,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schema") == null || root.get("schema").getAsInt()!=1)throw new IOException("Versão de manifesto não suportada");
            String base=path(root,"base_texture",true),outer=path(root,"outer_texture",false);
            validatePng(files.get(base));if(outer!=null)validatePng(files.get(outer));
            List<Part> outerParts=parseParts(root.getAsJsonArray("outer_parts"),outer,files);
            List<Part> accessories=parseParts(root.getAsJsonArray("accessories"),null,files);
            if(outerParts.size()>24||accessories.size()>24)throw new IOException("Limite de peças excedido");
            for(String name:files.keySet())if(name.toLowerCase(Locale.ROOT).endsWith(".png"))validatePng(files.get(name));
            return new WardrobePackage(bytes,base,outer,outerParts,accessories,files);
        }catch(IOException e){throw e;}catch(RuntimeException e){throw new IOException("Manifesto de personalização inválido",e);}
    }
    private static String path(JsonObject root,String key,boolean required)throws IOException{
        JsonElement e=root.get(key);if(e==null||e.isJsonNull()||e.getAsString().isBlank()){if(required)throw new IOException("Campo obrigatório ausente: "+key);return null;}
        String s=e.getAsString();if(!s.matches("[A-Za-z0-9._/-]{1,96}")||s.startsWith("/")||Arrays.asList(s.split("/")).contains(".."))throw new IOException("Caminho inválido: "+key);return s;
    }
    private static List<Part> parseParts(JsonArray array,String defaultTexture,Map<String,byte[]> files)throws IOException{
        if(array==null)return List.of();if(array.size()>24)throw new IOException("Limite de peças excedido");List<Part> result=new ArrayList<>();
        for(JsonElement el:array){JsonObject o=el.getAsJsonObject();String anchor=o.get("anchor").getAsString().toUpperCase(Locale.ROOT);if(!Set.of("HEAD","BODY","ARMS","RIGHT_LEG","LEFT_LEG").contains(anchor))throw new IOException("Ponto de fixação inválido");
            String texture=o.has("texture")?path(o,"texture",true):defaultTexture;if(texture==null||!files.containsKey(texture))throw new IOException("Textura de peça ausente");
            float[] pivot=triple(o.getAsJsonArray("pivot"),true),rotation=triple(o.getAsJsonArray("rotation"),false);JsonArray ca=o.getAsJsonArray("cubes");if(ca==null||ca.size()==0||ca.size()>32)throw new IOException("Cada peça precisa conter de 1 a 32 cubos");List<Cube> cubes=new ArrayList<>();
            for(JsonElement ce:ca){JsonObject c=ce.getAsJsonObject();float[] from=triple(c.getAsJsonArray("from"),true),size=triple(c.getAsJsonArray("size"),true);JsonArray uv=c.getAsJsonArray("uv");if(uv==null||uv.size()!=2)throw new IOException("UV inválido");int u=uv.get(0).getAsInt(),v=uv.get(1).getAsInt();
                if(size[0]<=0||size[1]<=0||size[2]<=0||size[0]>20||size[1]>20||size[2]>20||Math.abs(from[0])>20||Math.abs(from[1])>20||Math.abs(from[2])>20||u<0||v<0||u>512||v>512)throw new IOException("Geometria fora dos limites");
                cubes.add(new Cube(from[0],from[1],from[2],size[0],size[1],size[2],u,v));}
            result.add(new Part(anchor,pivot[0],pivot[1],pivot[2],rotation[0],rotation[1],rotation[2],texture,List.copyOf(cubes)));
        }return result;
    }
    private static float[] triple(JsonArray a,boolean required)throws IOException{if(a==null||a.size()!=3){if(required)throw new IOException("Vetor 3D inválido");return new float[]{0,0,0};}float[] v=new float[3];for(int i=0;i<3;i++){v[i]=a.get(i).getAsFloat();if(!Float.isFinite(v[i])||Math.abs(v[i])>32)throw new IOException("Vetor fora dos limites");}return v;}
    private static void validatePng(byte[] data)throws IOException{if(data==null||data.length<24||data.length>800_000||data[0]!=(byte)137||data[1]!='P'||data[2]!='N'||data[3]!='G'||data[12]!='I'||data[13]!='H'||data[14]!='D'||data[15]!='R')throw new IOException("Textura PNG inválida");int w=readInt(data,16),h=readInt(data,20);if(w!=h||!Set.of(64,128,256,512).contains(w))throw new IOException("PNG deve medir 64, 128, 256 ou 512 quadrados");java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(new ByteArrayInputStream(data));if(image==null||image.getWidth()!=w||image.getHeight()!=h)throw new IOException("Conteúdo PNG inválido");}
    private static int readInt(byte[] b,int i){return(b[i]&255)<<24|(b[i+1]&255)<<16|(b[i+2]&255)<<8|(b[i+3]&255);}
}
