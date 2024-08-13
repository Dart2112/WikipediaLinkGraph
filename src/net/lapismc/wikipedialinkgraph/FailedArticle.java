package net.lapismc.wikipedialinkgraph;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class FailedArticle {

    private final String url;
    private String title;

    /**
     * This is for a failed connection to a primary connection
     *
     * @param url   The URL of the page that failed to load
     * @param title The title of the page it should be connected to
     */
    FailedArticle(String url, String title) {
        this.url = url;
        this.title = title;
    }

    /**
     * This should be used if the connection failed during interconnection
     *
     * @param url The URL of the page that failed to load
     */
    FailedArticle(String url) {
        this.url = url;
    }

    boolean process(LinkGraph graph) {
        System.out.println("Attempting to get page at " + url + " as it failed previously");
        if (title == null) {
            //Its an interconnection
            Document doc = graph.getDocument(url);
            if (doc == null)
                return false;
            //Find all the links in the page
            Elements links = doc.select("a[href]");
            //Loop through all these links
            int progress = 0;
            int total = links.size();
            for (Element link : links) {
                progress++;
                //get the url of the link
                String linkURL = link.attr("abs:href");
                //if its to a site we have indexed
                if (graph.titleCache.containsKey(linkURL) && !url.equals(linkURL)) {
                    //get the page titles and add a connection/increase the weight
                    String a = graph.getTitle(url).replace(" - Wikipedia", "");
                    String b = graph.titleCache.get(linkURL);
                    graph.processConnection(a, b, progress, total);
                }
            }
        } else {
            //it's a normal connection
            String a = graph.getTitle(url);
            if (a.equalsIgnoreCase("Error")) {
                return false;
            }
            String b = title;
            graph.processConnection(a, b, 0, 0);
        }
        return true;
    }

}
