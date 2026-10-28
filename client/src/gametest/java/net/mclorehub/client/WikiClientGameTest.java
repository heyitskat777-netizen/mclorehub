package net.mclorehub.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

@SuppressWarnings("UnstableApiUsage")
public final class WikiClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(GLFW.GLFW_KEY_K);
        context.runOnClient(c -> check(c.screen instanceof TitleScreen, "Do not replace title screen"));
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitFor(c -> c.player != null && c.screen == null); world.getClientWorld().waitForChunksRender();
            KeyMapping mapping = context.computeOnClient(c -> Arrays.stream(c.options.keyMappings)
                    .filter(k -> k.getName().equals("key.mclorehub.open_wiki")).findFirst().orElseThrow());
            context.runOnClient(c -> {
                check(mapping.getDefaultKey().getValue() == GLFW.GLFW_KEY_K, "Default K missing");
                check(mapping.getCategory().label().getString().equals("MCLoreHub"), "Category translation missing");
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_K); context.waitForScreen(WikiScreen.class); readable(context);
            context.takeScreenshot("mclorehub-default"); context.clickScreenButton("gui.back"); context.waitForScreen(null);
            context.getInput().pressKey(GLFW.GLFW_KEY_K); context.waitForScreen(WikiScreen.class);
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE); context.waitForScreen(null);
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE); context.waitForScreen(PauseScreen.class);
            context.getInput().pressKey(GLFW.GLFW_KEY_K);
            context.runOnClient(c -> check(c.screen instanceof PauseScreen, "Do not replace active screens"));
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE); context.waitForScreen(null);
            int scale = context.computeOnClient(c -> c.options.guiScale().get());
            try {
                context.runOnClient(c -> { mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J)); KeyMapping.resetMapping(); });
                context.getInput().pressKey(GLFW.GLFW_KEY_K);
                context.runOnClient(c -> check(c.screen == null, "Old key still active after rebinding"));
                context.getInput().pressKey(GLFW.GLFW_KEY_J); context.waitForScreen(WikiScreen.class);
                for (int target : new int[]{1, 2}) {
                    context.runOnClient(c -> { c.options.guiScale().set(target); c.resizeDisplay(); });
                    context.waitTick(); readable(context); context.takeScreenshot("mclorehub-scale-" + target);
                }
                context.clickScreenButton("gui.back"); context.waitForScreen(null);
            } finally {
                context.runOnClient(c -> { mapping.setKey(mapping.getDefaultKey()); KeyMapping.resetMapping(); c.options.guiScale().set(scale); c.resizeDisplay(); });
            }
        }
        TitleScreen parent = context.computeOnClient(c -> (TitleScreen) c.screen);
        context.setScreen(() -> new WikiScreen(parent)); context.waitForScreen(WikiScreen.class);
        context.clickScreenButton("gui.back"); context.runOnClient(c -> check(c.screen == parent, "Parent not restored"));
        LoggerFactory.getLogger("mclorehub-test").info("CLIENT_SCENARIO_PASS: hotkey, navigation, rebind, guards, scaling, parent");
    }
    private static void readable(ClientGameTestContext context) {
        context.runOnClient(c -> {
            check(c.screen instanceof WikiScreen, "Wiki not open"); WikiScreen s = (WikiScreen) c.screen;
            check(s.getTitle().getString().equals("MCLoreHub"), "Title translation missing");
            check(s.getNarrationMessage().getString().contains("Your lore library is empty."), "Narration missing");
            Button b = s.children().stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow();
            check(b.active && b.visible && b.getX() >= 0 && b.getY() >= 0
                    && b.getX() + b.getWidth() <= s.width && b.getY() + b.getHeight() <= s.height, "Back outside screen");
        });
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
