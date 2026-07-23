package com.magik.registry;

import com.magik.MagikMod;
import com.magik.entity.MagicBoltEntity;
import com.magik.item.ItemRequirements;
import com.magik.item.RpgConsumableItem;
import com.magik.item.StaffItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.magik.player.RpgAttribute.INTELLIGENCE;

/**
 * Item registry. The mod's own gear is now focused entirely on the staves;
 * every melee/ranged weapon and armor piece has been removed. The skill trees
 * empower vanilla and modded weapons instead (see {@code WeaponType}), with
 * first-class support for <b>Spartan Weaponry</b>.
 *
 * <ul>
 *   <li><b>Staff</b> - fires arcane bolts, scales with Intelligence.</li>
 *   <li><b>Advanced Arcane Staff</b> - granted at Intelligence 50.</li>
 *   <li>Mana crystal + mana/stamina consumables.</li>
 * </ul>
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

    private ModItems() {
    }
}
