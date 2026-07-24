package com.magik.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** All key mappings of the mod (category "Magik RPG" in the controls screen). */
public final class KeyBindings {

    public static final String CATEGORY = "key.categories.magik";

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.magik.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    public static final KeyMapping DASH = new KeyMapping(
            "key.magik.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.magik.open_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);

    public static final KeyMapping ADD_WAYPOINT = new KeyMapping(
            "key.magik.add_waypoint", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    public static final KeyMapping MINIMAP_ZOOM = new KeyMapping(
            "key.magik.minimap_zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);

    public static final KeyMapping[] SKILL_SLOTS = {
            new KeyMapping("key.magik.skill_slot_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.magik.skill_slot_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.magik.skill_slot_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.magik.skill_slot_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY),
    };

    private KeyBindings() {
    }
}
