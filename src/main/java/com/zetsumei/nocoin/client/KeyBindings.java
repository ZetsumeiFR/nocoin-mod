package com.zetsumei.nocoin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Définition des raccourcis clavier du mod NOCOIN.
 */
public class KeyBindings {
    
    public static final String KEY_CATEGORY_NOCOIN = "key.category.nocoin";
    public static final String KEY_OPEN_MENU = "key.nocoin.open_menu";
    
    /**
     * Touche pour ouvrir le menu NOCOIN (N par défaut).
     */
    public static final KeyMapping OPEN_NOCOIN_MENU = new KeyMapping(
            KEY_OPEN_MENU,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            KEY_CATEGORY_NOCOIN
    );
}
