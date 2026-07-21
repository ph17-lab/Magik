package com.magik.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * Shared GeckoLib model for every {@link GeoAsset} weapon: it reads the geo,
 * texture and animation paths straight off the item instance, so one class
 * serves the dagger, both axes and the warhammer.
 */
public class WeaponGeoModel<T extends Item & GeoAnimatable & GeoAsset> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T item) {
        return item.geoModel();
    }

    @Override
    public ResourceLocation getTextureResource(T item) {
        return item.geoTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(T item) {
        return item.geoAnimation();
    }
}
