package com.magik.client.render;

import com.magik.MagikMod;
import com.magik.item.StaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Binds the staff's Bedrock geometry, texture and (empty) animation set. */
public class StaffModel extends GeoModel<StaffItem> {

    private static final ResourceLocation GEO =
            new ResourceLocation(MagikMod.MOD_ID, "geo/staff.geo.json");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(MagikMod.MOD_ID, "animations/staff.animation.json");

    @Override
    public ResourceLocation getModelResource(StaffItem item) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(StaffItem item) {
        return item.getGeoTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(StaffItem item) {
        return ANIMATIONS;
    }
}
