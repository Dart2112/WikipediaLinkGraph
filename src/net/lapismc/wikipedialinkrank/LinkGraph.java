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
    private HashMap<String, Document> documentCache = new HashMap<>();

    LinkGraph() {
        //create a list of the urls to index

        //Currency
        urls.add("https://en.wikipedia.org/wiki/Ethereum");
        urls.add("https://en.wikipedia.org/wiki/United_States_dollar");
        urls.add("https://en.wikipedia.org/wiki/Euro");
        urls.add("https://en.wikipedia.org/wiki/Thai_baht");
        urls.add("https://en.wikipedia.org/wiki/Pound_sterling");

        //TV Shows
        urls.add("https://en.wikipedia.org/wiki/Game_of_Thrones");
        urls.add("https://en.wikipedia.org/wiki/13_Reasons_Why");
        urls.add("https://en.wikipedia.org/wiki/Stranger_Things");
        urls.add("https://en.wikipedia.org/wiki/Mr._Robot");
        urls.add("https://en.wikipedia.org/wiki/The_Big_Bang_Theory");

        //Singers
        urls.add("https://en.wikipedia.org/wiki/Ed_Sheeran");
        urls.add("https://en.wikipedia.org/wiki/Ariana_Grande");
        urls.add("https://en.wikipedia.org/wiki/Taylor_Swift");
        urls.add("https://en.wikipedia.org/wiki/Rita_Ora");
        urls.add("https://en.wikipedia.org/wiki/Dua_Lipa");

        //Boxers
        urls.add("https://en.wikipedia.org/wiki/Floyd_Mayweather_Jr.");
        urls.add("https://en.wikipedia.org/wiki/Tyson_Fury");
        urls.add("https://en.wikipedia.org/wiki/Chris_Eubank_Jr.");
        urls.add("https://en.wikipedia.org/wiki/Sergey_Kovalev_(boxer)");
        urls.add("https://en.wikipedia.org/wiki/Errol_Spence_Jr.");

        //Random
        urls.add("https://en.wikipedia.org/wiki/India");
        urls.add("https://en.wikipedia.org/wiki/Melania_Trump");
        urls.add("https://en.wikipedia.org/wiki/Thor:_Ragnarok");
        urls.add("https://en.wikipedia.org/wiki/Bitcoin");
        urls.add("https://en.wikipedia.org/wiki/Google");

        collectData(0, 5, "Currency");
        collectData(5, 10, "TVShows");
        collectData(10, 15, "Singers");
        collectData(15, 20, "Boxers");
        collectData(20, 25, "Random");
    }

    /**
     * Collects the data for the provided range of URLs and saves them in a graph of name title
     *
     * @param start The index to start at, counts from 0
     * @param limit The number of pages to index, counts from 1
     * @param title The title of the graph output
     */
    private void collectData(int start, int limit, String title) {
        System.out.println("Collecting data for " + title);
        int i = limit;
        for (String url : urls) {
            //stop loading pages if we have reached the limit
            if (i <= 0)
                break;
            if (i < start) {
                continue;
            }
            //loop through the URLs and add the links from them to the list
            addLinksForUrl(url);
            i--;
        }
        addInterconnections();
        saveConnectionsAsGraph(title);
        //clear the lists to stop duplication
        connections.clear();
        titleCache.clear();
    }

    /**
     * Adds connections between non main nodes
     */
    private void addInterconnections() {
        for (String url : titleCache.keySet()) {
            if (urls.contains(url)) {
                continue;
            }
            System.out.println("Getting interconnections for " + titleCache.get(url) + "\n");
            //Use Jsoup to load the URLs document
            Document doc = getDocument(url);
            assert doc != null;
            //Find all the links in the page
            Elements links = doc.select("a[href]");
            //Loop through all these links
            for (Element link : links) {
                //get the url of the link
                String linkURL = link.attr("abs:href");
                //if its to a site we have indexed
                if (titleCache.containsKey(linkURL) && !url.equals(linkURL)) {
                    //get the page titles and add a connection/increase the weight
                    String a = titleCache.get(url);
                    String b = titleCache.get(linkURL);
                    processConnection(a, b);
                }
            }
        }
    }

    /**
     * Gets all wikipedia links on the page and adds their connections
     *
     * @param url The URL to index
     */
    private void addLinksForUrl(String url) {
        //Use Jsoup to load the URLs document
        Document doc = getDocument(url);
        assert doc != null;
        String targetTitle = doc.title().replace(" - Wikipedia", "");
        System.out.println("Getting connections for " + targetTitle);
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
                title = Objects.requireNonNull(getDocument(linkURL)).title();
                //ensure its a wikipedia article by ignoring the link if it doesn't
                //end with " - Wikipedia" or is in fact a category or file
                if (!title.endsWith(" - Wikipedia") || title.startsWith("Category:") || title.startsWith("File:")
                        || title.startsWith("Template:") || title.startsWith("Template talk:")
                        || title.startsWith("Wikipedia:") || title.startsWith("Portal:") || title.startsWith("Talk:")
                        || title.startsWith("User talk:") || title.startsWith("Help:") || title.startsWith("User contributions")
                        || title.startsWith("Pages that link to") || title.contains("Recent changes")
                        || title.contains("Related changes") || title.contains("Special pages")
                        || title.contains("Book sources") || title.contains("Digital object identifier") || title.contains("CITES")) {
                    continue;
                }
                //remove the " - Wikipedia" from the end of the title as its no longer required
                title = title.replace(" - Wikipedia", "");
                titleCache.put(linkURL, title);
            }
            //If the link is already in the list just add to the integer, otherwise add it to the list with a value of 1
            processConnection(targetTitle, title);
        }
    }

    /**
     * Processes a connection between 2 nodes
     *
     * @param a a node
     * @param b another node
     */
    private void processConnection(String a, String b) {
        System.out.println(a + " > " + b);
        if (isConnectionStored(a, b)) {
            Objects.requireNonNull(getConnection(a, b)).increaseWeight();
        } else {
            connections.add(new Connection(a, b));
        }
    }

    /**
     * Gets the document for the url
     *
     * @param url the URL you wish to retrieve
     * @return Returns the document for the provided URL
     */
    private Document getDocument(String url) {
        if (documentCache.containsKey(url)) {
            return documentCache.get(url);
        }
        try {
            Document doc = Jsoup.connect(url).get();
            documentCache.put(url, doc);
            return doc;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves the current connections as a graph
     *
     * @param title The title of the graph file to be exported
     */
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

    /**
     * Check if a connection exists between nodes
     *
     * @param x The name of a node
     * @param y The name of another node
     * @return Returns true if there is a connection between the nodes given
     */
    private boolean isConnectionStored(String x, String y) {
        return getConnection(x, y) != null;
    }

    /**
     * Get a connection
     *
     * @param x The name of a node
     * @param y The name of another node
     * @return Returns the connection object for the nodes given, null if one doesn't exist
     */
    private Connection getConnection(String x, String y) {
        for (Connection c : connections) {
            if (c.isConnecting(x, y)) {
                return c;
            }
        }
        return null;
    }

}
