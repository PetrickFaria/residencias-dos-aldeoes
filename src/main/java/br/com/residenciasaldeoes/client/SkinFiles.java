package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local PNG gallery. A selected file is uploaded once and then persisted by the world/server. */
public final class SkinFiles {
    public static final int MAX_BYTES = 262144;
    private static final String[] EXAMPLES = {"aldeao_aventureiro", "aldea_artesa"};
    private SkinFiles() {}

    /** A skin can come from the writable config folder or an enabled resource pack. */
    public record SkinFile(String id, String label, Path path, ResourceLocation resource) {}

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(ResidenciasDosAldeoes.MODID).resolve("skins");
    }

    public static List<SkinFile> list() {
        Path dir = directory();
        try {
            Files.createDirectories(dir);
            for (String sample : EXAMPLES) {
                Path target = dir.resolve(sample + ".png");
                if (!Files.exists(target)) {
                    try (InputStream input = SkinFiles.class.getResourceAsStream("/assets/" + ResidenciasDosAldeoes.MODID + "/skins/" + sample + ".png")) {
                        if (input != null) Files.copy(input, target);
                    }
                }
            }
            Map<String, SkinFile> found = new LinkedHashMap<>();
            try (var paths = Files.list(dir)) {
                paths.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                        .forEach(p -> {
                            String filename = p.getFileName().toString();
                            String id = filename.substring(0, filename.length() - 4);
                            if (id.matches("[A-Za-z0-9._-]{1,64}") && validPng64(p))
                                found.put(id, new SkinFile(id, id.replace('_', ' '), p, null));
                        });
            }
            // Resource packs use assets/residenciasaldeoes/skins/<id>.png. Config files
            // take precedence when an ID exists in both places.
            try {
                Minecraft.getInstance().getResourceManager().listResources("skins", location ->
                        location.getNamespace().equals(ResidenciasDosAldeoes.MODID)
                                && location.getPath().matches("skins/[A-Za-z0-9._-]{1,64}\\.png"))
                        .forEach((location, resource) -> {
                            String filename = location.getPath().substring("skins/".length());
                            String id = filename.substring(0, filename.length() - 4);
                            if (!found.containsKey(id) && validPng64(resource))
                                found.put(id, new SkinFile(id, id.replace('_', ' '), null, location));
                        });
            } catch (RuntimeException ignored) {
                // The config directory remains usable if a resource manager is unavailable.
            }
            List<SkinFile> result = new ArrayList<>(found.values());
            result.sort(Comparator.comparing(SkinFile::label, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(result);
        } catch (IOException e) {
            return List.of();
        }
    }

    public static byte[] read(SkinFile file) throws IOException {
        byte[] data;
        if (file.path() != null) {
            long size = Files.size(file.path());
            if (size <= 0 || size > MAX_BYTES) throw new IOException("PNG fora do limite de tamanho");
            data = Files.readAllBytes(file.path());
        } else if (file.resource() != null) {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(file.resource()).orElseThrow(() -> new IOException("Skin não encontrada no pacote de recursos"));
            try (InputStream input = resource.open()) {
                data = input.readNBytes(MAX_BYTES + 1);
                if (data.length == 0 || data.length > MAX_BYTES) throw new IOException("PNG fora do limite de tamanho");
            }
        } else {
            throw new IOException("Origem da skin indisponível");
        }
        if (!validPng64(data)) throw new IOException("A skin deve ser um PNG válido de 64×64 pixels");
        try (var image = com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(data))) {
            if (image.getWidth() != 64 || image.getHeight() != 64) throw new IOException("Dimensões PNG inválidas");
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Arquivo PNG inválido", e); }
        return data;
    }

    private static boolean validPng64(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] header = in.readNBytes(24);
            return validPng64(header);
        } catch (IOException e) { return false; }
    }

    private static boolean validPng64(Resource resource) {
        try (InputStream in = resource.open()) {
            byte[] header = in.readNBytes(24);
            return validPng64(header);
        } catch (IOException e) { return false; }
    }

    private static boolean validPng64(byte[] b) {
        if (b.length < 24 || b[0] != (byte) 137 || b[1] != 80 || b[2] != 78 || b[3] != 71) return false;
        return b[12] == 'I' && b[13] == 'H' && b[14] == 'D' && b[15] == 'R'
                && readInt(b, 16) == 64 && readInt(b, 20) == 64;
    }

    private static int readInt(byte[] b, int i) { return (b[i] & 255) << 24 | (b[i+1] & 255) << 16 | (b[i+2] & 255) << 8 | (b[i+3] & 255); }
}
