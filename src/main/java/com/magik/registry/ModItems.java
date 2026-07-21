package com.magik.registry;

import com.magik.MagikMod;
import com.magik.entity.MagicBoltEntity;
import com.magik.item.ArcaniteArmorItem;
import com.magik.item.DaggerItem;
import com.magik.item.HeavyWeaponItem;
import com.magik.item.ItemRequirements;
import com.magik.item.RpgAxeItem;
import com.magik.item.RpgBowItem;
import com.magik.item.RpgConsumableItem;
import com.magik.item.RpgShieldItem;
import com.magik.item.RpgSwordItem;
import com.magik.item.StaffItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.magik.player.RpgAttribute.AGILITY;
import static com.magik.player.RpgAttribute.INTELLIGENCE;
import static com.magik.player.RpgAttribute.PRECISION;
import static com.magik.player.RpgAttribute.STRENGTH;

/**
 * Item registry: the full RPG arsenal.
 *
 * <ul>
 *   <li><b>Swords</b> - fast, scale with Agility/Precision and Espadachim.</li>
 *   <li><b>Axes/Hammers</b> - heavy weapons for the Armamento Pesado tree.</li>
 *   <li><b>Bows</b> - six tiers with distinct draw speed/velocity/damage.</li>
 *   <li><b>Shields</b> - six tiers with armor bonuses and blocking perks.</li>
 *   <li><b>Staff</b> - consumes mana, scaling with Intelligence.</li>
 *   <li><b>Arcanite armor</b> - caster set with a full-set mana bonus.</li>
 * </ul>
 *
 * Higher tiers carry level/attribute requirements and higher rarity colors
 * (up to the custom Legendary and Mythic rarities).
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MagikMod.MOD_ID);

    // ------------------------------------------------------------------
    // Materials & consumables
    // ------------------------------------------------------------------

    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> MANA_POTION = ITEMS.register("mana_potion",
            () -> new RpgConsumableItem(RpgConsumableItem.Resource.MANA, 50.0F,
                    new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> STAMINA_DRAUGHT = ITEMS.register("stamina_draught",
            () -> new RpgConsumableItem(RpgConsumableItem.Resource.STAMINA, 60.0F,
                    new Item.Properties().stacksTo(16)));

    // ------------------------------------------------------------------
    // Swords
    // ------------------------------------------------------------------

    /** Fast dueling blade for Agility/Precision builds. */
    public static final RegistryObject<Item> SWIFT_BLADE = ITEMS.register("swift_blade",
            () -> new RpgSwordItem(Tiers.IRON, 1, -1.0F,
                    ItemRequirements.of(5, AGILITY, 3), 0.05F, new Item.Properties()));

    /** Balanced knight's blade. */
    public static final RegistryObject<Item> KNIGHT_SWORD = ITEMS.register("knight_sword",
            () -> new RpgSwordItem(Tiers.IRON, 4, -2.4F,
                    ItemRequirements.of(10, STRENGTH, 5), 0.0F, new Item.Properties()));

    /** Crystal-edged sword with high crit chance. */
    public static final RegistryObject<Item> CRYSTAL_BLADE = ITEMS.register("crystal_blade",
            () -> new RpgSwordItem(Tiers.DIAMOND, 3, -2.2F,
                    ItemRequirements.of(25, AGILITY, 10, PRECISION, 5), 0.10F,
                    new Item.Properties().rarity(Rarity.RARE)));

    // ------------------------------------------------------------------
    // Daggers (rogue - meant to be dual-wielded, powers the Adaga tree)
    // ------------------------------------------------------------------

    /** Shadow Dagger: fast, light, high crit; wield one in each hand. */
    public static final RegistryObject<Item> SHADOW_DAGGER = ITEMS.register("shadow_dagger",
            () -> new DaggerItem(Tiers.NETHERITE, 2, 2.4F,
                    ItemRequirements.of(8, AGILITY, 6), 0.12F,
                    new Item.Properties().rarity(Rarity.RARE)));

    // ------------------------------------------------------------------
    // Heavy weapons (hammers & axes)
    // ------------------------------------------------------------------

    /** Slow, brutal warhammer. */
    public static final RegistryObject<Item> WARHAMMER = ITEMS.register("warhammer",
            () -> new HeavyWeaponItem(Tiers.IRON, 7, -3.2F,
                    ItemRequirements.of(15, STRENGTH, 10), new Item.Properties()));

    /** Combat axe with elevated crit chance. */
    public static final RegistryObject<Item> BATTLE_AXE = ITEMS.register("battle_axe",
            () -> new RpgAxeItem(Tiers.IRON, 8.0F, -3.1F,
                    ItemRequirements.of(12, STRENGTH, 8), 0.10F, new Item.Properties()));

    /** Colossal two-handed axe. */
    public static final RegistryObject<Item> GIANT_AXE = ITEMS.register("giant_axe",
            () -> new RpgAxeItem(Tiers.DIAMOND, 9.0F, -3.4F,
                    ItemRequirements.of(30, STRENGTH, 20), 0.15F,
                    new Item.Properties().rarity(Rarity.EPIC)));

    // ------------------------------------------------------------------
    // Bows (drawTicks, velocity, damage bonus)
    // ------------------------------------------------------------------

    public static final RegistryObject<Item> SHORT_BOW = ITEMS.register("short_bow",
            () -> new RpgBowItem(14, 0.90F, 0.0F, ItemRequirements.NONE,
                    new Item.Properties().durability(320)));

    public static final RegistryObject<Item> LONG_BOW = ITEMS.register("long_bow",
            () -> new RpgBowItem(24, 1.25F, 0.10F, ItemRequirements.of(10, PRECISION, 5),
                    new Item.Properties().durability(448)));

    public static final RegistryObject<Item> COMPOSITE_BOW = ITEMS.register("composite_bow",
            () -> new RpgBowItem(18, 1.15F, 0.15F, ItemRequirements.of(20, PRECISION, 10, STRENGTH, 5),
                    new Item.Properties().durability(512).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> ELVISH_BOW = ITEMS.register("elvish_bow",
            () -> new RpgBowItem(12, 1.20F, 0.20F, ItemRequirements.of(35, AGILITY, 15, PRECISION, 10),
                    new Item.Properties().durability(640).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ARCANE_BOW = ITEMS.register("arcane_bow",
            () -> new RpgBowItem(16, 1.30F, 0.30F, ItemRequirements.of(45, INTELLIGENCE, 15, PRECISION, 15),
                    new Item.Properties().durability(768).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LEGENDARY_BOW = ITEMS.register("legendary_bow",
            () -> new RpgBowItem(12, 1.50F, 0.40F, ItemRequirements.of(60, PRECISION, 30),
                    new Item.Properties().durability(1024).rarity(ModRarities.LEGENDARY)));

    // ------------------------------------------------------------------
    // Shields (armor bonus, flat block reduction, knockback resist, perk)
    // ------------------------------------------------------------------

    public static final RegistryObject<Item> WOODEN_SHIELD = ITEMS.register("wooden_shield",
            () -> new RpgShieldItem(0.0D, 1.0F, 0.0D, null,
                    () -> Ingredient.of(Items.OAK_PLANKS), ItemRequirements.NONE,
                    new Item.Properties().durability(336)));

    public static final RegistryObject<Item> IRON_SHIELD = ITEMS.register("iron_shield",
            () -> new RpgShieldItem(1.0D, 2.0F, 0.0D, null,
                    () -> Ingredient.of(Items.IRON_INGOT), ItemRequirements.level(10),
                    new Item.Properties().durability(500)));

    public static final RegistryObject<Item> GOLDEN_SHIELD = ITEMS.register("golden_shield",
            () -> new RpgShieldItem(0.0D, 2.0F, 0.0D, () -> MobEffects.MOVEMENT_SPEED,
                    () -> Ingredient.of(Items.GOLD_INGOT), ItemRequirements.level(15),
                    new Item.Properties().durability(250).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> DIAMOND_SHIELD = ITEMS.register("diamond_shield",
            () -> new RpgShieldItem(2.0D, 3.0F, 0.1D, null,
                    () -> Ingredient.of(Items.DIAMOND), ItemRequirements.level(30),
                    new Item.Properties().durability(1024).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> OBSIDIAN_SHIELD = ITEMS.register("obsidian_shield",
            () -> new RpgShieldItem(2.0D, 3.0F, 0.2D, () -> MobEffects.FIRE_RESISTANCE,
                    () -> Ingredient.of(Items.OBSIDIAN), ItemRequirements.level(40),
                    new Item.Properties().durability(1536).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LEGENDARY_SHIELD = ITEMS.register("legendary_shield",
            () -> new RpgShieldItem(3.0D, 4.0F, 0.3D, () -> MobEffects.DAMAGE_RESISTANCE,
                    () -> Ingredient.of(Items.NETHERITE_INGOT), ItemRequirements.level(60),
                    new Item.Properties().durability(2048).rarity(ModRarities.LEGENDARY)));

    // ------------------------------------------------------------------
    // Staves (element, damage, mana cost, cast cooldown)
    // ------------------------------------------------------------------

    /** The caster's staff: fires arcane bolts, damage scales with Intelligence. */
    public static final RegistryObject<Item> STAFF = ITEMS.register("staff",
            () -> new StaffItem(MagicBoltEntity.Variant.ARCANE, 6.0F, 12.0F, 14,
                    ItemRequirements.of(5, INTELLIGENCE, 3),
                    MagikMod.id("textures/item/staff.png"),
                    new Item.Properties().durability(512).rarity(Rarity.RARE)));

    /**
     * The Advanced Arcanist's staff: purple-and-black, fires the purple orb.
     * Granted automatically (once) when a player reaches Intelligence 50.
     */
    public static final RegistryObject<Item> ADVANCED_STAFF = ITEMS.register("advanced_staff",
            () -> new StaffItem(MagicBoltEntity.Variant.SUPREME, 9.0F, 14.0F, 12,
                    ItemRequirements.of(20, INTELLIGENCE, 50),
                    MagikMod.id("textures/item/advanced_staff.png"),
                    new Item.Properties().durability(1200).rarity(ModRarities.MYTHIC)));

    // ------------------------------------------------------------------
    // Arcanite armor set (full set: +max mana)
    // ------------------------------------------------------------------

    public static final RegistryObject<Item> ARCANITE_HELMET = ITEMS.register("arcanite_helmet",
            () -> new ArcaniteArmorItem(ModArmorMaterials.ARCANITE, ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ARCANITE_CHESTPLATE = ITEMS.register("arcanite_chestplate",
            () -> new ArcaniteArmorItem(ModArmorMaterials.ARCANITE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ARCANITE_LEGGINGS = ITEMS.register("arcanite_leggings",
            () -> new ArcaniteArmorItem(ModArmorMaterials.ARCANITE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ARCANITE_BOOTS = ITEMS.register("arcanite_boots",
            () -> new ArcaniteArmorItem(ModArmorMaterials.ARCANITE, ArmorItem.Type.BOOTS,
                    new Item.Properties().rarity(Rarity.EPIC)));

    private ModItems() {
    }
}
