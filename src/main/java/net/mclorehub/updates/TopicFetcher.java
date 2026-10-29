package net.mclorehub.updates;

import net.mclorehub.catalogue.LoreCatalogue.Document;
import java.io.IOException;

@FunctionalInterface public interface TopicFetcher { TopicSnapshot fetch(Document document)throws IOException,InterruptedException; }
