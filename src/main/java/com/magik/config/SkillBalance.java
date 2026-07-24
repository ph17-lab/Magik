package com.magik.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.magik.MagikMod;
import com.magik.skills.Skill;
import com.magik.skills.SkillTrees;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON balance configuration ({@code config/magik/balance.json}).
 *
 * <p>Lets server owners retune every skill (mana/stamina cost, cooldown,
 * required level) and global regeneration/XP multipliers without touching
 * code. A fully populated default file is generated on first launch. Values
 * are enforced server-side, so clients cannot cheat by editing theirs.</p>
 */
public final class SkillBalance {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record Override(Float mana, Float stamina, Integer cooldown, Integer level) {
    }

    private static final Map<String, Override> OVERRIDES = new ConcurrentHashMap<>();

    private static float xpMultiplier = 1.0F;
    private static float manaRegenMultiplier = 1.0F;
    private static float staminaRegenMultiplier = 1.0F;

    private SkillBalance() {
    }

    // --- Lookups used by Skill / RpgStats ---

    public static float manaCost(String skillId, float fallback) {
        Override override = OVERRIDES.get(skillId);
        return override != null && override.mana() != null ? override.mana() : fallback;
    }

    public static float staminaCost(String skillId, float fallback) {
        Override override = OVERRIDES.get(skillId);
        return override != null && override.stamina() != null ? override.stamina() : fallback;
    }

    public static int cooldownTicks(String skillId, int fallback) {
        Override override = OVERRIDES.get(skillId);
        return override != null && override.cooldown() != null ? override.cooldown() : fallback;
    }

    public static int requiredLevel(String skillId, int fallback) {
        Override override = OVERRIDES.get(skillId);
        return override != null && override.level() != null ? override.level() : fallback;
    }

    public static float xpMultiplier() {
        return xpMultiplier;
    }

    public static float manaRegenMultiplier() {
        return manaRegenMultiplier;
    }

    public static float staminaRegenMultiplier() {
        return staminaRegenMultiplier;
    }

    // --- Loading ---

    /** Loads (or creates) the balance file. Called during common setup. */
    public static void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("magik").resolve("balance.json");
        try {
            if (!Files.exists(file)) {
                writeDefault(file);
            }
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (root == null) {
                return;
            }
            xpMultiplier = optFloat(root, "xpMultiplier", 1.0F);
            manaRegenMultiplier = optFloat(root, "manaRegenMultiplier", 1.0F);
            staminaRegenMultiplier = optFloat(root, "staminaRegenMultiplier", 1.0F);

            OVERRIDES.clear();
            JsonObject skills = root.has("skills") ? root.getAsJsonObject("skills") : new JsonObject();
            for (Map.Entry<String, JsonElement> entry : skills.entrySet()) {
                JsonObject skill = entry.getValue().getAsJsonObject();
                OVERRIDES.put(entry.getKey(), new Override(
                        skill.has("manaCost") ? skill.get("manaCost").getAsFloat() : null,
                        skill.has("staminaCost") ? skill.get("staminaCost").getAsFloat() : null,
                        skill.has("cooldownTicks") ? skill.get("cooldownTicks").getAsInt() : null,
                        skill.has("requiredLevel") ? skill.get("requiredLevel").getAsInt() : null));
            }
            MagikMod.LOGGER.info("Loaded balance.json ({} skill overrides)", OVERRIDES.size());
        } catch (Exception e) {
            MagikMod.LOGGER.error("Failed to load balance.json, using defaults", e);
        }
    }

    private static float optFloat(JsonObject object, String key, float fallback) {
        return object.has(key) ? object.get(key).getAsFloat() : fallback;
    }

    /** Writes a complete default file so every knob is discoverable. */
    private static void writeDefault(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("xpMultiplier", 1.0F);
        root.addProperty("manaRegenMultiplier", 1.0F);
        root.addProperty("staminaRegenMultiplier", 1.0F);
        JsonObject skills = new JsonObject();
        for (Skill skill : SkillTrees.all().values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("requiredLevel", skill.getRequiredLevel());
            skills.add(skill.getId(), entry);
        }
        root.add("skills", skills);
        Files.writeString(file, GSON.toJson(root));
    }
}
