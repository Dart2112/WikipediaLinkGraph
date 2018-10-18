package net.lapismc.wikipedialinkrank;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

class Article {

    private String title;
    private Document doc;

    Article(String url) {
        try {
            this.doc = Jsoup.connect(url).get();
            this.title = doc.title();
        } catch (IOException e) {
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
