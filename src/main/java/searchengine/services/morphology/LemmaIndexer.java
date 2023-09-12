package searchengine.services.morphology;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LemmaIndexer {
    private final LemmaFinder lemmaFinder;

    public LemmaIndexer() {
        lemmaFinder = new LemmaFinder();
    }

    public Map<String, Integer> getLemmaMap(String content) {
        Map<String, Integer> lemmas = new HashMap<>();
        String text = clearContentFromTag(content, "body");
        lemmas = lemmaFinder.collectLemmas(text);
        return lemmas;
    }

    private String clearContentFromTag(String content, String tag) {
        Document document = Jsoup.parse(content);
        Elements elements = document.select(tag);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }
}
