package net.lapismc.wikipedialinkrank;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

class Article {

    private String title;
    private Document doc;

    Article(String url) {
        try {
            Thread.sleep(50);
            this.doc = Jsoup.connect(url).timeout(5000).get();
            this.title = doc.title();
        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to get doc for " + url);
        }
    }

    String getTitle() {
        return title;
    }

    Document getDocument() {
        return doc;
    }
}
