package net.lapismc.wikipedialinkgraph;

import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.impl.ImportContainerImpl;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openide.util.Lookup;

import java.io.File;
import java.io.IOException;
import java.util.*;

class LinkGraph {

    private static LinkGraph instance;
    final HashMap<String, String> titleCache = new HashMap<>();
    final List<String> currentDataSet = new ArrayList<>();
    //This HashMap will store the titles of links scraped and the number of times that title occurred
    private final List<String> urls = new ArrayList<>();
    private final HashMap<String, Article> cache = new HashMap<>();
    private final ArrayList<Connection> connections = new ArrayList<>();
    private final ArrayList<FailedArticle> failedArticles = new ArrayList<>();

    LinkGraph() {
        instance = this;
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

        //Test
        urls.add("https://en.wikipedia.org/wiki/WavePad_Audio_Editor");
        urls.add("https://en.wikipedia.org/wiki/Khemarat_District");

        collectData(0, 5, "Currency");
        collectData(5, 5, "TVShows");
        collectData(10, 5, "Singers");
        collectData(15, 5, "Boxers");
        collectData(20, 5, "Random");
        collectData(25, 2, "Test");
    }

    /**
     * Get a static instance of this class for accessing methods externally
     *
     * @return returns a static instance of the {@link LinkGraph} class
     */
    static LinkGraph getInstance() {
        return instance;
    }

    /**
     * Collects the data for the provided range of URLs and saves them in a graph of name title
     *
     * @param start The index to start at, counts from 0
     * @param size  The number of pages to index, counts from 1
     * @param title The title of the graph output
     */
    private void collectData(int start, int size, String title) {
        System.out.println("Collecting data for " + title);
        int i = -1;
        int remaining = size;
        for (String url : urls) {
            i++;
            //stop loading pages if we have reached the limit
            if (remaining <= 0)
                break;
            if (i < start) {
                continue;
            }
            //loop through the URLs and add the links from them to the list
            addLinksForUrl(url);
            currentDataSet.add(url);
            remaining--;
        }
        addInterconnections();
        processFailedArticles();
        saveConnectionsAsGraph(title);
        //clear the list to stop duplication
        connections.clear();
        currentDataSet.clear();
    }

    /**
     * Keeps trying to load pages and process their connections until they are all done
     */
    private void processFailedArticles() {
        System.out.println("Processing failed articles");
        while (!failedArticles.isEmpty())
            failedArticles.removeIf(article -> article.process(this));
        System.out.println("Finished processing failed articles");
    }

    /**
     * Adds connections from non main nodes
     */
    private void addInterconnections() {
        ArrayList<Connection> list = new ArrayList<>();
        for (Connection c : connections) {
            list.add(c.clone());
        }
        Iterator<Connection> it = list.iterator();
        while (it.hasNext()) {
            Connection c = it.next();
            //because title A is always one of the main articles
            String title = c.getTitleB();
            it.remove();
            String url = getUrl(title);
            if (url.equals("")) {
                continue;
            }
            System.out.println("Getting interconnections for " + title + "\n");
            //Use Jsoup to load the URLs document
            Document doc = getDocument(url);
            if (doc == null) {
                failedArticles.add(new FailedArticle(url));
                continue;
            }
            //Find all the links in the page
            Elements links = doc.select("a[href]");
            //Loop through all these links
            for (Element link : links) {
                //get the url of the link
                String linkURL = link.attr("abs:href");
                //if its to a site we have indexed
                if (!url.equals(linkURL) && currentDataSet.contains(linkURL)) {
                    //get the page titles and add a connection/increase the weight
                    String a = getTitle(url).replace(" - Wikipedia", "");
                    String b = getTitle(linkURL).replace(" - Wikipedia", "");
                    processConnection(a, b);
                }
            }
            cleanCache();
        }
    }

    /**
     * Get the url for a title
     *
     * @param title The title of the page
     * @return Returns the URL of the page given from the TitleCache
     */
    private String getUrl(String title) {
        for (String url : titleCache.keySet()) {
            if (titleCache.get(url).equals(title))
                return url;
        }
        return "";
    }

    /**
     * Gets all wikipedia links on the page and adds their connections
     *
     * @param url The URL to index
     */
    private void addLinksForUrl(String url) {
        //Use Jsoup to load the URLs document
        Document doc = getDocument(url);
        while (doc == null) {
            System.out.println("Failed to load " + url + ", Trying again");
            cache.remove(url);
            titleCache.remove(url);
            doc = getDocument(url);
        }
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
            String title = getTitle(linkURL);
            if (title == null || title.equalsIgnoreCase("Error")) {
                failedArticles.add(new FailedArticle(url, targetTitle));
                continue;
            }
            //ensure its a wikipedia article by ignoring the link if it doesn't
            //end with " - Wikipedia" or is in fact a category or file
            if (!isValidTitle(title)) {
                continue;
            }
            //remove the " - Wikipedia" from the end of the title as its no longer required
            title = title.replace(" - Wikipedia", "");
            titleCache.put(linkURL, title);
            //If the link is already in the list just add to the integer, otherwise add it to the list with a value of 1
            processConnection(targetTitle, title);
            cleanCache();
        }
    }

    /**
     * Checks if the title matches the format of a Wikipedia Article
     *
     * @param title The title to test
     * @return Returns true if the title is valid
     */
    private boolean isValidTitle(String title) {
        return title.endsWith(" - Wikipedia") && !title.startsWith("Category:") && !title.startsWith("File:")
                && !title.startsWith("Template:") && !title.startsWith("Template talk:")
                && !title.startsWith("Wikipedia:") && !title.startsWith("Portal:") && !title.startsWith("Talk:")
                && !title.startsWith("User talk:") && !title.startsWith("Help:") && !title.startsWith("User contributions")
                && !title.startsWith("Pages that link to") && !title.contains("Recent changes")
                && !title.contains("Related changes") && !title.contains("Special pages")
                && !title.contains("Book sources") && !title.contains("Digital object identifier") && !title.contains("CITES");
    }

    /**
     * Removes items from cache so it doesn't get so big as to slow the program down
     */
    private void cleanCache() {
        if (cache.size() > 30) {
            ArrayList<String> toRemove = new ArrayList<>();
            Iterator<String> it = cache.keySet().iterator();
            int i = 0;
            while (it.hasNext() && i < 25) {
                i++;
                toRemove.add(it.next());
            }
            for (String s : toRemove) {
                cache.remove(s);
            }
        }
    }

    /**
     * Processes a connection between 2 nodes
     *
     * @param a a node
     * @param b another node
     */
    void processConnection(String a, String b) {
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
    Document getDocument(String url) {
        if (cache.containsKey(url)) {
            return cache.get(url).getDocument();
        }
        Article article = new Article(url);
        if (article.getDocument() == null) {
            return null;
        }
        cache.put(url, article);
        return article.getDocument();
    }

    /**
     * Gets the title of a url from JSoup or Cache
     * @param url The URL to fetch
     * @return Title of the URL requested
     */
    String getTitle(String url) {
        if (cache.containsKey(url)) {
            Article a = cache.get(url);
            if (a.getTitle() != null) {
                return cache.get(url).getTitle();
            } else {
                cache.remove(url);
                return "Error";
            }
        }
        Document doc = getDocument(url);
        if (doc == null) {
            return "Error";
        }
        return doc.title();
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
