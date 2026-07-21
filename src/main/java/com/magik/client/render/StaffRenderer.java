package com.magik.client.render;

import com.magik.item.StaffItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib item renderer for the staff's Bedrock-geometry model. */
public class StaffRenderer extends GeoItemRenderer<StaffItem> {

    public StaffRenderer() {
        super(new StaffModel());
    }
}
