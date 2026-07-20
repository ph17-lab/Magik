package com.magik.registry;

import com.magik.MagikMod;
import com.magik.entity.MagicBoltEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Entity type registry. */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MagikMod.MOD_ID);

    public static final RegistryObject<EntityType<MagicBoltEntity>> MAGIC_BOLT =
            ENTITY_TYPES.register("magic_bolt", () -> EntityType.Builder
                    .<MagicBoltEntity>of(MagicBoltEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("magic_bolt"));

    private ModEntities() {
    }
}
