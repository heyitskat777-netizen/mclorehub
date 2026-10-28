package net.mclorehub.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client initialization and key handling perform no disk or network work. */
public final class MCLoreHubClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mclorehub", "wiki"));
        KeyMapping openWiki = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mclorehub.open_wiki", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openWiki.consumeClick()) {
                if (client.player != null && client.screen == null) client.setScreen(new WikiScreen(null));
            }
        });
    }
}
