package re.deluxe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Darkness {
    public static boolean enabled = false;
    private static final float[][] LUMINANCE = new float[16][16];

    private static boolean darkOverworld = true;
    private static boolean darkNether = true;
    private static boolean darkEnd = true;
    private static boolean blockLightOnly = false;
    private static boolean ignoreMoonPhase = false;
    private static float darknessLevel = 1.0f; // 1.0 = oscuridad total

    static {
        loadConfig();
    }

    // Lee o crea el archivo de configuración
    private static void loadConfig() {
        try {
            File configDir = new File("config");
            if (!configDir.exists()) configDir.mkdirs();

            File file = new File(configDir, "darkness.properties");
            Properties prop = new Properties();

            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    prop.load(in);
                }
            }

            darkOverworld = Boolean.parseBoolean(prop.getProperty("dark_overworld", "true"));
            darkNether = Boolean.parseBoolean(prop.getProperty("dark_nether", "true"));
            darkEnd = Boolean.parseBoolean(prop.getProperty("dark_end", "true"));
            blockLightOnly = Boolean.parseBoolean(prop.getProperty("only_affect_block_light", "false"));
            ignoreMoonPhase = Boolean.parseBoolean(prop.getProperty("ignore_moon_phase", "false"));
            darknessLevel = Float.parseFloat(prop.getProperty("darkness_level", "1.0"));

            // Guarda si no existía
            try (FileOutputStream out = new FileOutputStream(file)) {
                prop.setProperty("dark_overworld", Boolean.toString(darkOverworld));
                prop.setProperty("dark_nether", Boolean.toString(darkNether));
                prop.setProperty("dark_end", Boolean.toString(darkEnd));
                prop.setProperty("only_affect_block_light", Boolean.toString(blockLightOnly));
                prop.setProperty("ignore_moon_phase", Boolean.toString(ignoreMoonPhase));
                prop.setProperty("darkness_level", Float.toString(darknessLevel));
                prop.store(out, "Darkness configuration");
            }
        } catch (IOException e) {
            System.err.println("[Darkness] Error loading configuration: " + e.getMessage());
        }
    }

    // --- LÓGICA PRINCIPAL --- //

    private static boolean isDark(ClientLevel world) {
        if (world.dimension() == Level.OVERWORLD) return darkOverworld;
        if (world.dimension() == Level.NETHER) return darkNether;
        if (world.dimension() == Level.END) return darkEnd;
        return true; // por defecto sí
    }

    public static int darken(int c, int blockIndex, int skyIndex) {
        final float lTarget = LUMINANCE[blockIndex][skyIndex];
        final float r = (c & 0xFF) / 255f;
        final float g = ((c >> 8) & 0xFF) / 255f;
        final float b = ((c >> 16) & 0xFF) / 255f;
        final float l = luminance(r, g, b);
        final float f = l > 0 ? Math.min(1, lTarget / l) : 0;
        return f == 1f ? c : 0xFF000000 | Math.round(f * r * 255) | (Math.round(f * g * 255) << 8) | (Math.round(f * b * 255) << 16);
    }

    public static float luminance(float r, float g, float b) {
        return r * 0.2126f + g * 0.7152f + b * 0.0722f;
    }

    public static void updateLuminance(float tickDelta, Minecraft client, GameRenderer renderer, float prevFlicker) {
        final ClientLevel world = client.level;

        if (world == null) {
            enabled = false;
            return;
        }

        // Evita oscuridad si el jugador tiene visión especial
        assert client.player != null;
        if (client.player.hasEffect(MobEffects.NIGHT_VISION)
                || (client.player.hasEffect(MobEffects.CONDUIT_POWER) && client.player.getWaterVision() > 0)
                || world.getSkyFlashTime() > 0) {
            enabled = false;
            return;
        }

        if (!isDark(world)) {
            enabled = false;
            return;
        }

        enabled = true;
        float darkness = Math.max(0f, Math.min(1f, darknessLevel));

        for (int skyIndex = 0; skyIndex < 16; ++skyIndex) {
            for (int blockIndex = 0; blockIndex < 16; ++blockIndex) {
                float base = blockLightOnly ? (blockIndex / 15f) : 0;
                LUMINANCE[blockIndex][skyIndex] = base * (1.0f - darkness);
            }
        }
    }
}