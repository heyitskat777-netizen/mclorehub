package net.mclorehub.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mclorehub.library.LibraryService;
import net.mclorehub.character.CharacterService;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client initialization and key handling perform no disk or network work. */
public final class MCLoreHubClient implements ClientModInitializer {
    private static LibraryService library;
    private static CharacterService characters;
    public static LibraryService library() { return library; }
    public static CharacterService characters() { return characters; }
    @Override
    public void onInitializeClient() {
        library = new LibraryService(FabricLoader.getInstance().getConfigDir().resolve("mclorehub"));
        library.reload();
        characters=new CharacterService(library.directory());characters.reload();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {library.close();characters.close();});
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mclorehub", "wiki"));
        KeyMapping openWiki = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mclorehub.open_wiki", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openWiki.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    WikiScreen browser = new WikiScreen(null);
                    var reading = library.view().personal().reading();
                    var doc = library.view().library().stream().flatMap(l -> l.catalogue().documents().stream())
                            .filter(d -> reading.map(p -> p.documentId().equals(d.id()) && (p.audit() || net.mclorehub.library.LoreSearch.isCurrent(d))).orElse(false))
                            .findFirst();
                    if (doc.isPresent()) client.setScreen(new ReaderScreen(browser,library,doc.get(),reading.orElseThrow().audit(),false));
                    else client.setScreen(browser);
                }
            }
        });
    }
}
