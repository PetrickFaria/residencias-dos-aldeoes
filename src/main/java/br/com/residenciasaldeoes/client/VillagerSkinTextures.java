package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.SkinApplicationMode;
import br.com.residenciasaldeoes.data.WardrobePackage;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.*;

/** Client-side dynamic textures keyed by villager UUID; no entity model or AI is replaced. */
public final class VillagerSkinTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Integer> SUPPORTED_SIZES = Set.of(64, 128, 256, 512);
    private static final Map<UUID, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<UUID, byte[]> PNGS = new HashMap<>();
    private static final Map<UUID, SkinApplicationMode> MODES = new HashMap<>();
    private static final Map<UUID, WardrobePackage> WARDROBES = new HashMap<>();
    private static final Map<UUID, Map<String, ResourceLocation>> EXTRA_TEXTURES = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> EXTRA_TEXTURE_SCALES = new HashMap<>();
    public record PreparedPart(String anchor, ModelPart model, String texture) {}
    private static final Map<UUID, List<PreparedPart>> OUTER_PARTS = new HashMap<>(), ACCESSORIES = new HashMap<>();

    private VillagerSkinTextures() {}

    private record RegisteredTexture(ResourceLocation location, int scale) {}

    public static void receive(UUID resident, String key, byte[] png) {
        receive(resident, key, png, SkinApplicationMode.COMPLETE);
    }

    public static void receive(UUID resident, String key, byte[] png, SkinApplicationMode mode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (key == null || key.isEmpty() || png == null || png.length == 0) {
            clear(resident);
            return;
        }
        if (isZip(png)) {
            receiveBundle(resident, key, png, mode);
            return;
        }

        NativeImage image = null;
        DynamicTexture dynamic = null;
        try {
            image = readSupportedPng(png);
            boolean completeSkin = mode == null || mode == SkinApplicationMode.COMPLETE;
            NativeImage renderImage = FreshAnimationsFaceTextureAdapter.adaptIfAvailable(image, completeSkin);
            if (renderImage != image) {
                image.close();
                image = renderImage;
            }
            // VillagerModel uses normalized UVs based on a 64x64 atlas. The adapter,
            // when applicable, only fills Fresh Animations' otherwise-empty facial UVs.
            ResourceLocation location = textureLocation(resident);
            dynamic = new DynamicTexture(image);
            image = null; // DynamicTexture owns the rendered RGBA image from here.
            dynamic.setFilter(false, false); // nearest-neighbor and no mipmaps
            minecraft.getTextureManager().register(location, dynamic);
            dynamic = null; // TextureManager owns and replaces a texture at the same ID.
            TEXTURES.put(resident, location);
            PNGS.put(resident, png.clone());
            MODES.put(resident, mode == null ? SkinApplicationMode.COMPLETE : mode);
            WARDROBES.remove(resident);
            releaseExtras(resident);
            OUTER_PARTS.remove(resident);
            ACCESSORIES.remove(resident);
        } catch (Exception error) {
            LOGGER.warn("Não foi possível registrar a skin do aldeão {}. A skin anterior foi mantida.", resident, error);
        } finally {
            if (image != null) image.close();
            if (dynamic != null) dynamic.close();
        }
    }

    private static boolean isZip(byte[] bytes) {
        return bytes != null && bytes.length > 3 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private static NativeImage readSupportedPng(byte[] data) throws Exception {
        NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
        if (image.getWidth() != image.getHeight() || !SUPPORTED_SIZES.contains(image.getWidth())) {
            image.close();
            throw new IllegalArgumentException("PNG precisa medir 64, 128, 256 ou 512 pixels quadrados");
        }
        return image;
    }

    private static void receiveBundle(UUID id, String key, byte[] bytes, SkinApplicationMode mode) {
        try {
            WardrobePackage pack = WardrobePackage.parse(bytes);
            releaseExtras(id);
            Map<String, ResourceLocation> textures = new HashMap<>();
            Map<String, Integer> scales = new HashMap<>();
            RegisteredTexture base = registerPng(id, "base", pack.texture(pack.baseTexture()), mode == null || mode == SkinApplicationMode.COMPLETE);
            for (String path : allTexturePaths(pack)) {
                RegisteredTexture registered = registerPng(id, path, pack.texture(path));
                textures.put(path, registered.location());
                scales.put(path, registered.scale());
            }
            TEXTURES.put(id, base.location());
            PNGS.put(id, bytes.clone());
            MODES.put(id, mode == null ? SkinApplicationMode.COMPLETE : mode);
            WARDROBES.put(id, pack);
            EXTRA_TEXTURES.put(id, textures);
            EXTRA_TEXTURE_SCALES.put(id, scales);
            OUTER_PARTS.put(id, prepare(pack.outerParts(), scales));
            ACCESSORIES.put(id, prepare(pack.accessories(), scales));
        } catch (Exception error) {
            LOGGER.warn("Pacote de personalização inválido para o aldeão {}. A aparência anterior foi mantida.", id, error);
        }
    }

    private static Set<String> allTexturePaths(WardrobePackage pack) {
        Set<String> paths = new HashSet<>();
        if (pack.outerTexture() != null) paths.add(pack.outerTexture());
        for (var part : pack.outerParts()) paths.add(part.texture());
        for (var part : pack.accessories()) paths.add(part.texture());
        return paths;
    }

    private static RegisteredTexture registerPng(UUID id, String path, byte[] data) throws Exception {
        return registerPng(id, path, data, false);
    }

    private static RegisteredTexture registerPng(UUID id, String path, byte[] data, boolean completeSkin) throws Exception {
        NativeImage image = readSupportedPng(data);
        int scale = image.getWidth() / 64;
        DynamicTexture dynamic = null;
        try {
            NativeImage renderImage = FreshAnimationsFaceTextureAdapter.adaptIfAvailable(image, completeSkin);
            if (renderImage != image) {
                image.close();
                image = renderImage;
            }
            dynamic = new DynamicTexture(image);
            image = null; // DynamicTexture owns the image.
            dynamic.setFilter(false, false);
            ResourceLocation location = textureLocation(id, path);
            Minecraft.getInstance().getTextureManager().register(location, dynamic);
            dynamic = null; // TextureManager owns the texture.
            return new RegisteredTexture(location, scale);
        } finally {
            if (image != null) image.close();
            if (dynamic != null) dynamic.close();
        }
    }

    private static List<PreparedPart> prepare(List<WardrobePackage.Part> parts, Map<String, Integer> scales) {
        List<PreparedPart> prepared = new ArrayList<>();
        for (var part : parts) {
            try {
                // Manifest UV coordinates use a 64x64 basis. Scale both the coordinates
                // and baked atlas dimensions when the corresponding PNG is higher-res.
                int scale = scales.getOrDefault(part.texture(), 1);
                MeshDefinition mesh = new MeshDefinition();
                PartDefinition root = mesh.getRoot();
                CubeListBuilder cubes = CubeListBuilder.create();
                for (var cube : part.cubes()) {
                    cubes.texOffs(cube.u() * scale, cube.v() * scale)
                            .addBox(cube.x(), cube.y(), cube.z(), cube.width(), cube.height(), cube.depth(), CubeDeformation.NONE);
                }
                root.addOrReplaceChild("piece", cubes, PartPose.offsetAndRotation(
                        part.px(), part.py(), part.pz(), part.rx(), part.ry(), part.rz()));
                ModelPart model = LayerDefinition.create(mesh, 64 * scale, 64 * scale)
                        .bakeRoot().getChild("piece");
                prepared.add(new PreparedPart(part.anchor(), model, part.texture()));
            } catch (Exception error) {
                LOGGER.warn("Geometria externa inválida em '{}'; peça ignorada.", part.texture(), error);
            }
        }
        return List.copyOf(prepared);
    }

    private static ResourceLocation textureLocation(UUID id, String path) {
        String safe = path.replace('/', '_').replace('.', '_');
        return new ResourceLocation(ResidenciasDosAldeoes.MODID,
                "resident_skins/" + id.toString().replace("-", "") + "_" + safe);
    }

    private static void releaseExtras(UUID id) {
        Map<String, ResourceLocation> textures = EXTRA_TEXTURES.remove(id);
        if (textures != null) {
            for (ResourceLocation location : textures.values()) {
                Minecraft.getInstance().getTextureManager().release(location);
            }
        }
        EXTRA_TEXTURE_SCALES.remove(id);
    }

    public static ResourceLocation get(UUID resident) { return TEXTURES.get(resident); }
    public static SkinApplicationMode mode(UUID resident) { return MODES.getOrDefault(resident, SkinApplicationMode.COMPLETE); }
    public static boolean isComplete(UUID resident) {
        return TEXTURES.containsKey(resident) && mode(resident) == SkinApplicationMode.COMPLETE;
    }
    public static byte[] getPng(UUID resident) {
        byte[] png = PNGS.get(resident);
        return png == null ? null : png.clone();
    }
    public static WardrobePackage wardrobe(UUID resident) { return WARDROBES.get(resident); }
    public static ResourceLocation extraTexture(UUID resident, String name) {
        Map<String, ResourceLocation> textures = EXTRA_TEXTURES.get(resident);
        return textures == null ? null : textures.get(name);
    }
    public static List<PreparedPart> outerParts(UUID id) { return OUTER_PARTS.getOrDefault(id, List.of()); }
    public static List<PreparedPart> accessories(UUID id) { return ACCESSORIES.getOrDefault(id, List.of()); }

    public static void clear(UUID resident) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation texture = TEXTURES.remove(resident);
        if (texture != null) minecraft.getTextureManager().release(texture);
        releaseExtras(resident);
        PNGS.remove(resident);
        MODES.remove(resident);
        WARDROBES.remove(resident);
        OUTER_PARTS.remove(resident);
        ACCESSORIES.remove(resident);
    }

    public static void clearAll() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : TEXTURES.values()) minecraft.getTextureManager().release(texture);
        TEXTURES.clear();
        for (UUID id : new ArrayList<>(EXTRA_TEXTURES.keySet())) releaseExtras(id);
        WARDROBES.clear();
        OUTER_PARTS.clear();
        ACCESSORIES.clear();
        PNGS.clear();
        MODES.clear();
        EXTRA_TEXTURE_SCALES.clear();
    }

    private static ResourceLocation textureLocation(UUID id) { return textureLocation(id, "base"); }
}
