package net.mclorehub.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

public final class WikiScreen extends Screen {
    private static final Component EMPTY = Component.translatable("screen.mclorehub.wiki.empty");
    private static final Component HINT = Component.translatable("screen.mclorehub.wiki.hint");
    private final Screen parent;
    private List<FormattedCharSequence> emptyLines = List.of(), hintLines = List.of();
    public WikiScreen(Screen parent) {
        super(Component.translatable("screen.mclorehub.wiki.title")); this.parent = parent;
    }
    @Override protected void init() {
        int textWidth = Math.max(100, Math.min(420, width - 48));
        emptyLines = font.split(EMPTY, textWidth); hintLines = font.split(HINT, textWidth);
        int buttonWidth = Math.min(200, Math.max(80, width - 40));
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds((width - buttonWidth) / 2, height - 36, buttonWidth, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.21.11 draws/ blurs the background before render; do not draw it twice.
        int y = Math.max(50, height / 3);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFFFF);
        lines(graphics, emptyLines, y, 0xFFFFFFFF);
        lines(graphics, hintLines, y + emptyLines.size() * font.lineHeight + 12, 0xFFB0BAC5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private void lines(GuiGraphics graphics, List<FormattedCharSequence> lines, int y, int color) {
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, width / 2, y, color); y += font.lineHeight;
        }
    }
    @Override public Component getNarrationMessage() {
        return Component.empty().append(title).append(". ").append(EMPTY).append(" ").append(HINT);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
