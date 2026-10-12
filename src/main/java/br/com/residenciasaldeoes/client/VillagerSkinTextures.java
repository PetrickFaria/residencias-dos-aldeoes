package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.SkinApplicationMode;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client-side dynamic textures keyed by villager UUID; no entity model or AI is replaced. */
public final class VillagerSkinTextures {
    private static final Map<UUID, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<UUID, byte[]> PNGS = new HashMap<>();
    private static final Map<UUID, SkinApplicationMode> MODES = new HashMap<>();
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
        NativeImage image = null;
        DynamicTexture dynamic = null;
        try {
            image = NativeImage.read(new ByteArrayInputStream(png));
            if (image.getWidth() != 64 || image.getHeight() != 64) return;
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
        } catch (Exception ignored) {
            // Keep the last valid skin if a new PNG is corrupt or unsupported.
        } finally {
            if (image != null) image.close();
            if (dynamic != null) dynamic.close();
        }
    }

    public static ResourceLocation get(UUID resident) { return TEXTURES.get(resident); }
    public static SkinApplicationMode mode(UUID resident) { return MODES.getOrDefault(resident, SkinApplicationMode.COMPLETE); }
    public static boolean isComplete(UUID resident) { return TEXTURES.containsKey(resident) && mode(resident) == SkinApplicationMode.COMPLETE; }
    public static byte[] getPng(UUID resident) { byte[] png=PNGS.get(resident);return png==null?null:png.clone(); }
    public static void clear(UUID resident) { Minecraft minecraft=Minecraft.getInstance();ResourceLocation texture=TEXTURES.remove(resident);if(texture!=null)minecraft.getTextureManager().release(texture);PNGS.remove(resident);MODES.remove(resident); }

    public static void clearAll() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : TEXTURES.values()) minecraft.getTextureManager().release(texture);
        TEXTURES.clear();
        PNGS.clear();
        MODES.clear();
    }

    private static ResourceLocation textureLocation(UUID id) {
        return new ResourceLocation(ResidenciasDosAldeoes.MODID, "resident_skins/" + id.toString().replace("-", ""));
    }
}
