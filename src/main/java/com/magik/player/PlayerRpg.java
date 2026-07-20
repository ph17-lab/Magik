package com.magik.player;

import com.magik.config.MagikServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The RPG state attached to every player as a Forge capability.
 *
 * <p>This is a pure data holder: it knows how to store, serialize and clamp its
 * values. Gameplay formulas live in {@link RpgStats}, lifecycle management in
 * {@link RpgEvents}, and networking in {@link com.magik.network.MagikNetwork}.</p>
 *
 * <p>Everything here is persisted to the player's NBT and synchronized to the
 * owning client, so progress survives death, relog and server restarts.</p>
 */
public class PlayerRpg {

    public static final int SKILL_SLOTS = 4;

    private int level = 1;
    private int xp = 0;
    private int attributePoints = 0;
    private int skillPoints = 0;

    private final EnumMap<RpgAttribute, Integer> attributes = new EnumMap<>(RpgAttribute.class);

    private float mana = 100.0F;
    private float stamina = 100.0F;
    /** Game time of the last stamina expenditure; regen pauses briefly after use. */
    private long lastStaminaUse = 0L;

    /** Ids of unlocked skill tree nodes. */
    private final Set<String> unlockedSkills = new HashSet<>();
    /** skillId -> game time at which the skill becomes usable again. */
    private final Map<String, Long> cooldowns = new HashMap<>();
    /** Active skills assigned to the HUD hotkeys (null = empty slot). */
    private final String[] skillSlots = new String[SKILL_SLOTS];

    // ------------------------------------------------------------------
    // Transient combat state (server-side, never persisted):
    // powers combo, parry/riposte, charged strikes, arrow enchant buffs,
    // the summoned-weapons aura and out-of-combat regeneration.
    // ------------------------------------------------------------------

    /** Special effect applied to the next arrow the player fires. */
    public enum ArrowEffect {NONE, EXPLOSIVE, FROST, ELECTRIC}

    private int comboStacks;
    private long comboUntil;
    private long chargedStrikeUntil;
    private long parryUntil;
    private long riposteUntil;
    private long weaponSummonUntil;
    private long lastCombatTime;
    private ArrowEffect nextArrowEffect = ArrowEffect.NONE;
    private long nextArrowUntil;

    public PlayerRpg() {
        for (RpgAttribute attribute : RpgAttribute.values()) {
            attributes.put(attribute, 0);
        }
    }

    // --- Level & XP ---

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Mth.clamp(level, 1, MagikServerConfig.MAX_LEVEL.get());
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    /**
     * Adds RPG XP and performs any pending level ups.
     *
     * @return the number of levels gained.
     */
    public int addXp(int amount) {
        if (amount <= 0 || level >= MagikServerConfig.MAX_LEVEL.get()) {
            return 0;
        }
        xp += amount;
        int gained = 0;
        int needed = RpgStats.xpForNextLevel(level);
        while (xp >= needed && level < MagikServerConfig.MAX_LEVEL.get()) {
            xp -= needed;
            level++;
            gained++;
            attributePoints += MagikServerConfig.ATTRIBUTE_POINTS_PER_LEVEL.get();
            skillPoints += MagikServerConfig.SKILL_POINTS_PER_LEVEL.get();
            needed = RpgStats.xpForNextLevel(level);
        }
        return gained;
    }

    // --- Points ---

    public int getAttributePoints() {
        return attributePoints;
    }

    public void setAttributePoints(int points) {
        this.attributePoints = Math.max(0, points);
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int points) {
        this.skillPoints = Math.max(0, points);
    }

    // --- Attributes ---

    public int getAttribute(RpgAttribute attribute) {
        return attributes.getOrDefault(attribute, 0);
    }

    public void setAttribute(RpgAttribute attribute, int value) {
        attributes.put(attribute, Math.max(0, value));
    }

    /** Spends one attribute point if available. @return true on success. */
    public boolean spendAttributePoint(RpgAttribute attribute) {
        if (attributePoints <= 0) {
            return false;
        }
        attributePoints--;
        attributes.merge(attribute, 1, Integer::sum);
        return true;
    }

    // --- Mana & Stamina ---

    public float getMana() {
        return mana;
    }

    public void setMana(float value) {
        this.mana = Math.max(0.0F, value);
    }

    public boolean consumeMana(float amount) {
        if (mana < amount) {
            return false;
        }
        mana -= amount;
        return true;
    }

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float value) {
        this.stamina = Math.max(0.0F, value);
    }

    public boolean consumeStamina(float amount, long gameTime) {
        if (stamina < amount) {
            return false;
        }
        stamina -= amount;
        lastStaminaUse = gameTime;
        return true;
    }

    public long getLastStaminaUse() {
        return lastStaminaUse;
    }

    public void setLastStaminaUse(long gameTime) {
        this.lastStaminaUse = gameTime;
    }

    // --- Skills ---

    public Set<String> getUnlockedSkills() {
        return unlockedSkills;
    }

    public boolean hasSkill(String skillId) {
        return unlockedSkills.contains(skillId);
    }

    public void unlockSkill(String skillId) {
        unlockedSkills.add(skillId);
    }

    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }

    public boolean isOnCooldown(String skillId, long gameTime) {
        Long readyAt = cooldowns.get(skillId);
        return readyAt != null && readyAt > gameTime;
    }

    public void setCooldown(String skillId, long readyAtGameTime) {
        cooldowns.put(skillId, readyAtGameTime);
    }

    public String[] getSkillSlots() {
        return skillSlots;
    }

    public void setSkillSlot(int slot, String skillId) {
        if (slot >= 0 && slot < SKILL_SLOTS) {
            skillSlots[slot] = skillId;
        }
    }

    // --- Transient combat state accessors ---

    public int getComboStacks() {
        return comboStacks;
    }

    public long getComboUntil() {
        return comboUntil;
    }

    public void startCombo(long until) {
        this.comboStacks = 0;
        this.comboUntil = until;
    }

    public void incrementCombo(int maxStacks) {
        this.comboStacks = Math.min(maxStacks, comboStacks + 1);
    }

    public long getChargedStrikeUntil() {
        return chargedStrikeUntil;
    }

    public void setChargedStrikeUntil(long until) {
        this.chargedStrikeUntil = until;
    }

    public long getParryUntil() {
        return parryUntil;
    }

    public void setParryUntil(long until) {
        this.parryUntil = until;
    }

    public long getRiposteUntil() {
        return riposteUntil;
    }

    public void setRiposteUntil(long until) {
        this.riposteUntil = until;
    }

    public long getWeaponSummonUntil() {
        return weaponSummonUntil;
    }

    public void setWeaponSummonUntil(long until) {
        this.weaponSummonUntil = until;
    }

    public long getLastCombatTime() {
        return lastCombatTime;
    }

    public void setLastCombatTime(long gameTime) {
        this.lastCombatTime = gameTime;
    }

    public ArrowEffect getNextArrowEffect(long gameTime) {
        return gameTime <= nextArrowUntil ? nextArrowEffect : ArrowEffect.NONE;
    }

    public void setNextArrowEffect(ArrowEffect effect, long until) {
        this.nextArrowEffect = effect;
        this.nextArrowUntil = until;
    }

    public void clearNextArrowEffect() {
        this.nextArrowEffect = ArrowEffect.NONE;
        this.nextArrowUntil = 0L;
    }

    // --- Persistence ---

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level);
        tag.putInt("Xp", xp);
        tag.putInt("AttributePoints", attributePoints);
        tag.putInt("SkillPoints", skillPoints);
        tag.putFloat("Mana", mana);
        tag.putFloat("Stamina", stamina);

        CompoundTag attrTag = new CompoundTag();
        for (Map.Entry<RpgAttribute, Integer> entry : attributes.entrySet()) {
            attrTag.putInt(entry.getKey().getId(), entry.getValue());
        }
        tag.put("Attributes", attrTag);

        ListTag skillsTag = new ListTag();
        for (String skill : unlockedSkills) {
            skillsTag.add(StringTag.valueOf(skill));
        }
        tag.put("Skills", skillsTag);

        CompoundTag cooldownTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            cooldownTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("Cooldowns", cooldownTag);

        ListTag slotsTag = new ListTag();
        for (String slot : skillSlots) {
            slotsTag.add(StringTag.valueOf(slot == null ? "" : slot));
        }
        tag.put("Slots", slotsTag);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        level = Math.max(1, tag.getInt("Level"));
        xp = tag.getInt("Xp");
        attributePoints = tag.getInt("AttributePoints");
        skillPoints = tag.getInt("SkillPoints");
        mana = tag.getFloat("Mana");
        stamina = tag.getFloat("Stamina");

        CompoundTag attrTag = tag.getCompound("Attributes");
        for (RpgAttribute attribute : RpgAttribute.values()) {
            attributes.put(attribute, Math.max(0, attrTag.getInt(attribute.getId())));
        }

        unlockedSkills.clear();
        for (Tag element : tag.getList("Skills", Tag.TAG_STRING)) {
            unlockedSkills.add(element.getAsString());
        }

        cooldowns.clear();
        CompoundTag cooldownTag = tag.getCompound("Cooldowns");
        for (String key : cooldownTag.getAllKeys()) {
            cooldowns.put(key, cooldownTag.getLong(key));
        }

        ListTag slotsTag = tag.getList("Slots", Tag.TAG_STRING);
        for (int i = 0; i < SKILL_SLOTS; i++) {
            String value = i < slotsTag.size() ? slotsTag.getString(i) : "";
            skillSlots[i] = value.isEmpty() ? null : value;
        }
    }

    /** Copies persistent progress from another instance (used on death/clone). */
    public void copyFrom(PlayerRpg other) {
        deserializeNBT(other.serializeNBT());
    }
}
