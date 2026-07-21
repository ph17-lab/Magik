package com.magik.client.render;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Shared GeckoLib item renderer for every {@link GeoAsset} weapon. */
public class WeaponGeoRenderer<T extends Item & GeoAnimatable & GeoAsset> extends GeoItemRenderer<T> {

    public WeaponGeoRenderer() {
        super(new WeaponGeoModel<>());
    }
}
