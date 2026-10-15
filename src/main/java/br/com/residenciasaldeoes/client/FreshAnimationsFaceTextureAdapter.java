package br.com.residenciasaldeoes.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the facial atlas used by Fresh Animations 1.8 to complete custom skins
 * whose corresponding UV island is transparent. The imported PNG is never changed:
 * this creates a render-only texture copy for Skin Completa.
 */
final class FreshAnimationsFaceTextureAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation VILLAGER_MODEL =
            new ResourceLocation("minecraft", "optifine/cem/villager.jem");
    private static final ResourceLocation VILLAGER_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/villager/villager.png");

    // FA 1.8 uses a 256x256 CEM UV basis on a normalized 64x64 texture.
    // Its eye whites/pupils, eyelids and brow occupy U 56..80 / V 80..92,
    // which resolves to this 6x3 region on the resident's atlas.
    private static final int FACE_U = 14;
    private static final int FACE_V = 20;
    private static final int FACE_WIDTH = 6;
    private static final int FACE_HEIGHT = 3;

    // The resident's standard villager head-front UV area provides its palette.
    private static final int HEAD_U = 8;
    private static final int HEAD_V = 8;
    private static final int HEAD_SIZE = 8;

    private FreshAnimationsFaceTextureAdapter() {}

    static NativeImage adaptIfAvailable(NativeImage skin, boolean completeSkin) {
        if (!completeSkin || skin.getWidth() != skin.getHeight()
                || skin.getWidth() < 64 || skin.getWidth() > 1024
                || skin.getWidth() % 64 != 0 || !freshAnimations18IsActive()) {
            return skin;
        }

        int scale = skin.getWidth() / 64;
        if (!faceIslandIsEmpty(skin, scale)) return skin;

        NativeImage template = null;
        NativeImage adapted = null;
        try {
            Resource texture = Minecraft.getInstance().getResourceManager()
                    .getResource(VILLAGER_TEXTURE).orElse(null);
            if (texture == null) return skin;

            try (InputStream stream = texture.open()) {
                template = NativeImage.read(stream);
            }
            if (template.getWidth() != 64 || template.getHeight() != 64) return skin;

            List<Integer> facePalette = collectFacePalette(skin, scale);
            if (facePalette.isEmpty()) return skin;

            adapted = new NativeImage(NativeImage.Format.RGBA, skin.getWidth(), skin.getHeight(), false);
            copyPixels(skin, adapted);

            for (int v = 0; v < FACE_HEIGHT; v++) {
                for (int u = 0; u < FACE_WIDTH; u++) {
                    int feature = template.getPixelRGBA(FACE_U + u, FACE_V + v);
                    int alpha = alpha(feature);
                    if (alpha == 0) continue;

                    int faceColor = nearestFaceColor(feature, facePalette);
                    int adaptedPixel = (alpha << 24) | (faceColor & 0x00FFFFFF);
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            adapted.setPixelRGBA(FACE_U * scale + u * scale + dx,
                                    FACE_V * scale + v * scale + dy, adaptedPixel);
                        }
                    }
                }
            }

            LOGGER.info("Adapted the empty Fresh Animations 1.8 face UV island for a complete resident skin.");
            NativeImage result = adapted;
            adapted = null;
            return result;
        } catch (Exception error) {
            LOGGER.warn("Could not prepare Fresh Animations face UVs; keeping the resident PNG unchanged.", error);
            return skin;
        } finally {
            if (template != null) template.close();
            if (adapted != null) adapted.close();
        }
    }

    private static boolean freshAnimations18IsActive() {
        if (!ModList.get().isLoaded("entity_model_features")
                || !ModList.get().isLoaded("entity_texture_features")) return false;
        try {
            Resource model = Minecraft.getInstance().getResourceManager()
                    .getResource(VILLAGER_MODEL).orElse(null);
            if (model == null) return false;
            String json;
            try (InputStream stream = model.open()) {
                json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            return json.contains("\"right_eye_white\"")
                    && json.contains("\"right_blink\"")
                    && json.contains("\"right_brow\"")
                    && json.contains("\"textureSize\":[256,256]");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean faceIslandIsEmpty(NativeImage skin, int scale) {
        for (int y = FACE_V * scale; y < (FACE_V + FACE_HEIGHT) * scale; y++) {
            for (int x = FACE_U * scale; x < (FACE_U + FACE_WIDTH) * scale; x++) {
                if (alpha(skin.getPixelRGBA(x, y)) != 0) return false;
            }
        }
        return true;
    }

    private static List<Integer> collectFacePalette(NativeImage skin, int scale) {
        List<Integer> palette = new ArrayList<>();
        for (int y = HEAD_V * scale; y < (HEAD_V + HEAD_SIZE) * scale; y++) {
            for (int x = HEAD_U * scale; x < (HEAD_U + HEAD_SIZE) * scale; x++) {
                int pixel = skin.getPixelRGBA(x, y);
                if (alpha(pixel) != 0) {
                    int color = pixel & 0x00FFFFFF;
                    if (!palette.contains(color)) palette.add(color);
                }
            }
        }
        return palette;
    }

    private static int nearestFaceColor(int feature, List<Integer> palette) {
        int featureLuminance = luminance(feature);
        int closest = palette.get(0);
        int closestDistance = Math.abs(luminance(closest) - featureLuminance);
        for (int i = 1; i < palette.size(); i++) {
            int candidate = palette.get(i);
            int distance = Math.abs(luminance(candidate) - featureLuminance);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static int luminance(int rgba) {
        int red = rgba & 0xFF;
        int green = (rgba >>> 8) & 0xFF;
        int blue = (rgba >>> 16) & 0xFF;
        return (2126 * red + 7152 * green + 722 * blue) / 10000;
    }

    private static int alpha(int rgba) {
        return (rgba >>> 24) & 0xFF;
    }

    private static void copyPixels(NativeImage source, NativeImage target) {
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                target.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
    }
}
