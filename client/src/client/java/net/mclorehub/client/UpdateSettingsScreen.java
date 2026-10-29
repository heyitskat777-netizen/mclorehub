package net.mclorehub.client;

import net.mclorehub.updates.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.List;
import java.time.ZoneOffset;

/** No credentials are displayed; only an explicitly dropped local session file is read. */
public final class UpdateSettingsScreen extends Screen {
    private final Screen parent;private final MonthlyUpdater monthly;private final ForumSession session;private Button toggle,forget;
    public UpdateSettingsScreen(Screen parent,MonthlyUpdater monthly,ForumSession session){super(Component.literal("Updates and forum session"));this.parent=parent;this.monthly=monthly;this.session=session;}
    @Override protected void init(){int col=(width-36)/2;
        toggle=addRenderableWidget(Button.builder(Component.literal("Monthly checks"),b->monthly.setEnabled(!monthly.view().enabled())).bounds(16,70,col,20).build());
        addRenderableWidget(Button.builder(Component.literal("Check due pages"),b->monthly.runOnce()).bounds(20+col,70,col,20).build());
        addRenderableWidget(Button.builder(Component.literal("Open forum sign-in"),ConfirmLinkScreen.confirmLink(this,"https://www.lordofthecraft.net/login/"))
                .bounds(16,138,col,20).build());
        forget=addRenderableWidget(Button.builder(Component.literal("Forget session"),b->session.forget()).bounds(20+col,138,col,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds((width-120)/2,height-28,120,20).build());update();
    }
    private void update(){toggle.setMessage(Component.literal(monthly.view().enabled()?"Monthly checks: On":"Monthly checks: Off"));toggle.active=monthly.view().writable();forget.active=true;}
    @Override public void tick(){update();}
    @Override public void onFilesDrop(List<Path> paths){if(paths.size()==1)session.importFile(paths.getFirst());}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". "+monthly.view().message()+" "+session.view().message());}
    @Override public void render(GuiGraphics g,int x,int y,float delta){g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);
        var status=monthly.view();String due=status.next().map(t->t.atZone(ZoneOffset.UTC).toLocalDate().toString()).orElse("after library loads");
        g.drawCenteredString(font,font.plainSubstrByWidth("Next due (UTC): "+due+" • "+status.due()+" due",width-32),width/2,30,0xFFBAC9D8);
        g.drawCenteredString(font,font.plainSubstrByWidth(status.message(),width-32),width/2,47,0xFFBAC9D8);
        g.drawWordWrap(font,Component.literal(session.view().message()),16,103,width-32,0xFFBAC9D8);
        g.drawWordWrap(font,Component.literal("Sign in in your browser. Export this forum's session cookies as Netscape .txt, then drop the file here. Treat it like a password; never share it."),16,170,width-32,0xFFFFC477);
        super.render(g,x,y,delta);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
