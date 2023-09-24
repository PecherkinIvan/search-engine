package searchengine.utils.morphology;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.RepositoryIndex;
import searchengine.repositories.RepositoryLemma;

import java.util.Map;
import java.util.stream.Collectors;

public record LemmaIndexer(Site modelSite, Page modelPage,
                           RepositoryLemma repositoryLemma, RepositoryIndex repositoryIndex) {

    public void run() {
        String content = modelPage.getContent();
        String title = clearContentFromTag(content, "title");
        String body = clearContentFromTag(content, "body");
        String text = title.concat(" " + body);

        Map<String, Integer> lemmas = LemmaFinder.collectLemmas(text);
        lemmas.forEach(this::saveLemmaAndIndex);
    }

    public static String clearContentFromTag(String content, String tag) {
        Document document = Jsoup.parse(content);
        Elements elements = document.select(tag);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }

    private void saveLemmaAndIndex(String lemma, Integer count) {
        synchronized (modelSite) {
            Lemma lemmaDB = repositoryLemma.findByLemmaAndSite(lemma, modelSite);
            if (lemmaDB == null) {
                Lemma lemmaNew = new Lemma();
                lemmaNew.setSite(modelSite);
                lemmaNew.setLemma(lemma);
                lemmaNew.setFrequency(1);
                lemmaDB = repositoryLemma.save(lemmaNew);
            }
            else {
                lemmaDB.setFrequency(lemmaDB.getFrequency() + 1);
                repositoryLemma.save(lemmaDB);
            }
            Index index = new Index();
            index.setPage(modelPage);
            index.setRank(count);
            index.setLemma(lemmaDB);
            repositoryIndex.save(index);
        }
    }
}
