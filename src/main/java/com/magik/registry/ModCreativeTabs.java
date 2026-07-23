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
                        // Staves
                        output.accept(ModItems.STAFF.get());
                        output.accept(ModItems.ADVANCED_STAFF.get());
                        // Consumables & materials
                        output.accept(ModItems.MANA_POTION.get());
                        output.accept(ModItems.STAMINA_DRAUGHT.get());
                        output.accept(ModItems.MANA_CRYSTAL.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
