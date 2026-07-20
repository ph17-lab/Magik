package com.magik.registry;

import com.magik.MagikMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The "Magik RPG" creative tab containing every mod item. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagikMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAGIK_TAB = TABS.register("magik",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.magik"))
                    .icon(() -> new ItemStack(ModItems.STAFF.get()))
                    .displayItems((parameters, output) -> {
                        // Swords
                        output.accept(ModItems.SWIFT_BLADE.get());
                        output.accept(ModItems.KNIGHT_SWORD.get());
                        output.accept(ModItems.CRYSTAL_BLADE.get());
                        // Heavy weapons
                        output.accept(ModItems.WARHAMMER.get());
                        output.accept(ModItems.BATTLE_AXE.get());
                        output.accept(ModItems.GIANT_AXE.get());
                        // Bows
                        output.accept(ModItems.SHORT_BOW.get());
                        output.accept(ModItems.LONG_BOW.get());
                        output.accept(ModItems.COMPOSITE_BOW.get());
                        output.accept(ModItems.ELVISH_BOW.get());
                        output.accept(ModItems.ARCANE_BOW.get());
                        output.accept(ModItems.LEGENDARY_BOW.get());
                        // Shields
                        output.accept(ModItems.WOODEN_SHIELD.get());
                        output.accept(ModItems.IRON_SHIELD.get());
                        output.accept(ModItems.GOLDEN_SHIELD.get());
                        output.accept(ModItems.DIAMOND_SHIELD.get());
                        output.accept(ModItems.OBSIDIAN_SHIELD.get());
                        output.accept(ModItems.LEGENDARY_SHIELD.get());
                        // Staff
                        output.accept(ModItems.STAFF.get());
                        // Armor
                        output.accept(ModItems.ARCANITE_HELMET.get());
                        output.accept(ModItems.ARCANITE_CHESTPLATE.get());
                        output.accept(ModItems.ARCANITE_LEGGINGS.get());
                        output.accept(ModItems.ARCANITE_BOOTS.get());
                        // Consumables & materials
                        output.accept(ModItems.MANA_POTION.get());
                        output.accept(ModItems.STAMINA_DRAUGHT.get());
                        output.accept(ModItems.MANA_CRYSTAL.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
