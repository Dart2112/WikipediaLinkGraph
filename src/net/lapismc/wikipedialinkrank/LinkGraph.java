package net.lapismc.wikipedialinkrank;

import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.impl.ImportContainerImpl;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openide.util.Lookup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

class LinkGraph {

    //This HashMap will store the titles of links scraped and the number of times that title occurred
    private ArrayList<Connection> connections = new ArrayList<>();
    private List<String> urls = new ArrayList<>();
    private HashMap<String, String> titleCache = new HashMap<>();

    LinkGraph() {
        //create a list of the urls to index
        urls.add("https://en.wikipedia.org/wiki/Meghan_Markle");
        urls.add("https://en.wikipedia.org/wiki/Elizabeth_II");
        generateData(1, "One");
        generateData(2, "Two");
    }

    private void generateData(int limit, String title) {
        int i = limit;
        for (String url : urls) {
            //stop loading pages if we have reached the limit
            if (i <= 0)
                break;
            //loop through the URLs and add the links from them to the list
            addLinksForUrl(url);
            i--;
        }
        saveConnectionsAsGraph(title);
        //clear the list to stop duplication
        connections.clear();
    }

    private void addLinksForUrl(String url) {
        try {
            //Use Jsoup to load the URLs document
            Document doc = Jsoup.connect(url).get();
            //Find all the links in the page
            Elements links = doc.select("a[href]");
            //Loop through all these links
            for (Element link : links) {
                //get the url of the link
                String linkURL = link.attr("abs:href");
                //ignore pages that aren't on wikipedia, this is to limit the number of sites we have to load to get the title
                if (!linkURL.contains("en.wikipedia.org/wiki") || linkURL.startsWith(url)) {
                    continue;
                }
                String title;
                if (titleCache.containsKey(linkURL)) {
                    title = titleCache.get(linkURL);
                } else {
                    //get the title of the link by loading the target page
                    title = Jsoup.connect(linkURL).get().title();
                    //ensure its a wikipedia article by ignoring the link if it doesn't
                    //end with " - Wikipedia" or is in fact a category or file
                    if (!title.endsWith(" - Wikipedia") || title.startsWith("Category:") || title.startsWith("File:") ||
                            title.startsWith("Template:") || title.startsWith("Template talk:")
                            || title.startsWith("Wikipedia:") || title.startsWith("Portal:") || title.startsWith("Talk:")
                            || title.startsWith("User talk:") || title.startsWith("Help:") || title.startsWith("User contributions")
                            || title.startsWith("Pages that link to") || title.equals("Recent changes")
                            || title.equals("Related changes") || title.equals("Special pages")) {
                        continue;
                    }
                    //remove the " - Wikipedia" from the end of the title as its no longer required
                    title = title.replace(" - Wikipedia", "");
                    titleCache.put(linkURL, title);
                }
                System.out.println(title);
                //If the link is already in the list just add to the integer, otherwise add it to the list with a value of 1
                if (isConnectionStored(doc.title(), title)) {
                    Objects.requireNonNull(getConnection(doc.title(), title)).increaseWeight();
                } else {
                    connections.add(new Connection(doc.title(), title));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConnectionsAsGraph(String title) {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();
        GraphGenerator generator = new GraphGenerator();
        generator.setConnections(connections);
        ImportContainerImpl container = new ImportContainerImpl();
        generator.generate(container);
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        importController.process(container, new DefaultProcessor(), workspace);
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("output" + File.separator + title + ".gexf"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isConnectionStored(String x, String y) {
        return getConnection(x, y) != null;
    }

    private Connection getConnection(String x, String y) {
        for (Connection c : connections) {
            if (c.isConnecting(x, y)) {
                return c;
            }
        }
        return null;
    }

}
