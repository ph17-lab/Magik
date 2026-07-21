package com.magik.client.render;

import net.minecraft.resources.ResourceLocation;

/**
 * Implemented by GeckoLib-rendered items so a single shared model/renderer can
 * pull each item's geometry, texture and animation without a class per weapon.
 */
public interface GeoAsset {

    ResourceLocation geoModel();

    ResourceLocation geoTexture();

    ResourceLocation geoAnimation();
}
