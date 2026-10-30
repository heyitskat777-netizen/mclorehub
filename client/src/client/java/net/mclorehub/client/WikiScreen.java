package net.mclorehub.client;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import net.mclorehub.library.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import java.util.*;
import java.util.concurrent.*;

/** Browser consumes immutable worker results; it never reads or indexes files. */
public final class WikiScreen extends Screen {
    private final Screen parent;
    private final LibraryService service;
    private String query="",category="";
    private boolean audit=false,favorites=false;
    private int offset=0;
    private int lastPageSize=0;
    private boolean needsRequest=false;
    private long generation=-1;
    private EditBox search;
    private Button previous,next,resume,categories,auditButton,favoritesButton;
    private final List<Button> rows=new ArrayList<>();
    private LoreSearch.Page results=new LoreSearch.Page(List.of(),0,0);
    private CompletableFuture<LoreSearch.Page> pending;
    private String searchError="";
    public WikiScreen(Screen parent){this(parent,MCLoreHubClient.library());}
    public WikiScreen(Screen parent,LibraryService service){super(Component.translatable("screen.mclorehub.wiki.title"));this.parent=parent;this.service=service;}
    private int pageSize(){return Math.max(1,(height-164)/54);}
    private Button button(String key,int x,int y,int w,Button.OnPress action){
        return addRenderableWidget(Button.builder(Component.translatable(key),action).bounds(x,y,w,20).build());
    }
    @Override protected void init(){
        button("Database",16,7,80,b->minecraft.setScreen(new DatabaseImportScreen(this,service)));
        button("screen.mclorehub.updates",width-80,7,64,b->minecraft.setScreen(new UpdateSettingsScreen(this,MCLoreHubClient.monthly(),service.session())));
        if(lastPageSize!=pageSize()||needsRequest){
            if(lastPageSize!=pageSize())offset=0;
            lastPageSize=pageSize();needsRequest=false;request();
        }
        int col=(width-40)/3;
        if(search==null){
            search=new EditBox(font,16,42,width-32,20,Component.translatable("screen.mclorehub.search"));
            search.setMaxLength(256);search.setHint(Component.translatable("screen.mclorehub.search"));search.setValue(query);
            search.setResponder(value->{query=value;offset=0;request();});
        }
        search.setWidth(width-32);addRenderableWidget(search);
        var view=service.view();
        categories=button("screen.mclorehub.category",16,66,col,b->{
            Map<String,Long> counts=new HashMap<>();service.view().library().ifPresent(l->l.catalogue().documents().stream()
                    .filter(d->audit||LoreSearch.isCurrent(d)).forEach(d->counts.merge(d.category(),1L,Long::sum)));
            minecraft.setScreen(new CategoryScreen(this,counts,key->{category=key;offset=0;}));
        });
        categories.setMessage(Component.literal(BrowseLabels.category(category)));
        auditButton=button(audit?"screen.mclorehub.audit_on":"screen.mclorehub.audit_off",20+col,66,col,b->{
            audit=!audit;category="";offset=0;request();categories.setMessage(Component.literal(BrowseLabels.category(category)));
            auditButton.setMessage(Component.translatable(audit?"screen.mclorehub.audit_on":"screen.mclorehub.audit_off"));
        });
        favoritesButton=button(favorites?"screen.mclorehub.favorites_on":"screen.mclorehub.favorites_off",24+col*2,66,col,b->{
            favorites=!favorites;offset=0;request();favoritesButton.setMessage(Component.translatable(favorites?"screen.mclorehub.favorites_on":"screen.mclorehub.favorites_off"));
        });
        previous=button("screen.mclorehub.previous",16,height-58,80,b->{offset=Math.max(0,offset-pageSize());request();});
        next=button("screen.mclorehub.next",width-96,height-58,80,b->{offset+=pageSize();request();});
        int bottom=(width-44)/4;
        Button reload=button("screen.mclorehub.reload",16,height-28,bottom,b->{service.reload();});
        reload.setTooltip(Tooltip.create(Component.literal(view.message().isEmpty()?service.directory().toString():view.message())));
        resume=button("screen.mclorehub.resume",20+bottom,height-28,bottom,b->resume());
        resume.active=view.personal().reading().flatMap(p->view.library().flatMap(l->l.catalogue().documents().stream().filter(d->d.id().equals(p.documentId())).findFirst())).isPresent();
        button("screen.mclorehub.characters",24+bottom*2,height-28,bottom,b->minecraft.setScreen(new CharacterListScreen(this,MCLoreHubClient.characters())));
        button("gui.back",28+bottom*3,height-28,bottom,b->onClose());
        refreshRows();
    }
    @Override protected void setInitialFocus(){setInitialFocus(search);}
    private void open(Document doc){minecraft.setScreen(new ReaderScreen(this,service,doc,audit,false));}
    private void resume(){service.view().personal().reading().ifPresent(p->service.view().library().ifPresent(l->l.catalogue().documents().stream()
            .filter(d->d.id().equals(p.documentId())).findFirst().ifPresent(d->minecraft.setScreen(new ReaderScreen(this,service,d,!LoreSearch.isCurrent(d),false)))));}
    private void request(){
        if(pending!=null)pending.cancel(false);searchError="";
        rows.forEach(row->row.active=false);
        var v=service.view();pending=service.search(new LoreSearch.Query(query,category,audit,favorites,v.personal().favorites(),offset,pageSize()));
    }
    @Override public void tick(){
        var v=service.view();if(v.generation()!=generation){generation=v.generation();offset=0;results=new LoreSearch.Page(List.of(),0,0);request();}
        if(pending!=null&&pending.isDone()){
            try{results=pending.join();offset=results.offset();}catch(CancellationException e){results=new LoreSearch.Page(List.of(),0,0);}
            catch(CompletionException e){searchError="Search failed: "+e.getCause().getMessage();results=new LoreSearch.Page(List.of(),0,0);}
            pending=null;refreshRows();
        }
    }
    private void refreshRows(){
        rows.forEach(this::removeWidget);rows.clear();int y=100;
        for(var hit:results.hits()){
            Document doc=hit.document();var row=addRenderableWidget(Button.builder(Component.literal(doc.title()),b->open(doc))
                    .bounds(16,y,width-32,20).build());row.active=pending==null;
            row.setTooltip(Tooltip.create(Component.literal(doc.title()+"\n"+doc.id()+" • "+BrowseLabels.category(doc.category())+"\n"+doc.role()+"\n"+hit.snippet())));
            rows.add(row);y+=54;
        }
        if(previous!=null)previous.active=offset>0&&pending==null;
        if(next!=null)next.active=offset+pageSize()<results.total()&&pending==null;
        if(resume!=null)resume.active=service.view().personal().reading().isPresent();
    }
    private String status(){var v=service.view();return v.library().map(l->Component.translatable("screen.mclorehub.coverage",l.catalogue().currentDocuments().size(),
            l.catalogue().documents().size(),l.catalogue().reviewQueue().size()).getString()).orElseGet(()->Component.translatable(
            v.phase()==LibraryService.Phase.LOADING?"screen.mclorehub.loading":"screen.mclorehub.wiki.empty").getString());}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
        g.drawCenteredString(font,title,width/2,10,0xFFFFFFFF);
        String line=service.view().message().isEmpty()?status():service.view().message();
        g.drawCenteredString(font,font.plainSubstrByWidth(line,width-32),width/2,27,service.view().message().isEmpty()?0xFFBAC9D8:0xFFFFC477);
        if(results.hits().isEmpty()&&pending==null){
            Component message=Component.translatable(service.view().library().isEmpty()?"screen.mclorehub.install":audit?"screen.mclorehub.no_results":"screen.mclorehub.no_current");
            g.drawWordWrap(font,message,20,105,width-40,0xFFBAC9D8);
        }
        int y=100;for(var hit:results.hits()){
            g.fill(16,y+20,width-16,y+48,0xD0151B24);
            g.drawCenteredString(font,font.plainSubstrByWidth(BrowseLabels.category(hit.document().category())+" • "+ReaderContent.statusLabel(hit.document().status()),width-48),width/2,y+23,0xFF97BBC9);
            g.drawCenteredString(font,font.plainSubstrByWidth(hit.snippet(),width-48),width/2,y+36,0xFFD1DBE5);y+=54;
        }
        String footer=!searchError.isEmpty()?searchError:pending!=null?Component.translatable("screen.mclorehub.searching").getString():
                Component.translatable("screen.mclorehub.page",results.total()==0?0:offset/pageSize()+1,Math.max(1,(results.total()+pageSize()-1)/pageSize()),results.total()).getString();
        g.drawCenteredString(font,font.plainSubstrByWidth(footer,width-204),width/2,height-51,0xFFFFFFFF);
        super.render(g,mouseX,mouseY,delta);
    }
    @Override public Component getNarrationMessage(){return Component.empty().append(title).append(". ").append(status()).append(" ")
            .append(Component.translatable(audit?"screen.mclorehub.audit_on":"screen.mclorehub.audit_off")).append(". ")
            .append(Component.translatable("screen.mclorehub.page",results.total()==0?0:offset/pageSize()+1,Math.max(1,(results.total()+pageSize()-1)/pageSize()),results.total()))
            .append(" ").append(service.view().message());}
    @Override public void removed(){if(pending!=null)pending.cancel(false);needsRequest=true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
