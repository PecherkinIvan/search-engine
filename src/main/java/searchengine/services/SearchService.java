package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.DataSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.RepositoryIndex;
import searchengine.repositories.RepositoryLemma;
import searchengine.repositories.RepositoryPage;
import searchengine.repositories.RepositorySite;
import searchengine.utils.morphology.LemmaFinder;
import searchengine.utils.morphology.LemmaIndexer;
import searchengine.utils.parse.LinkParser;
import searchengine.utils.relevance.RelevancePage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService implements SearchServiceInter {

    private final SitesList sitesList;
    @Autowired
    private RepositorySite repositorySite;
    @Autowired
    private RepositoryPage repositoryPage;
    @Autowired
    private RepositoryLemma repositoryLemma;
    @Autowired
    private RepositoryIndex repositoryIndex;

    @Override
    public SearchResponse getSearch(String query, String siteUrl) {

        if (query.isEmpty()) return new SearchResponse("Запрос не введен");

        Set<String> queryLemmas = LemmaFinder.collectLemmas(query).keySet();
        List<Index> foundIndexes;

        if (siteUrl == null) {
            foundIndexes = searchByAll(queryLemmas);
        }
        else {
            Site site = repositorySite.findEntitySiteByUrl(siteUrl);
            if (site.getStatus() != Site.Status.INDEX)  return new SearchResponse("Выбранный сайт ещё не проиндексирован");
            foundIndexes = searchBySite(queryLemmas, site);
        }
        if (foundIndexes.isEmpty()) return new SearchResponse("Ничего не найдено");

        getRelevantSet(foundIndexes).forEach(t -> {
            System.out.println(t.getPage().getPath() + "  " + t.getRankWords() + t.getRelevance());
        });


        List<DataSearchItem> data = getDataList(getRelevantSet(foundIndexes)) ;
        return new SearchResponse(data.size(), data);
    }


    private List<Index> searchByAll(Set<String> words) {
        List<Lemma> lemmas = repositoryLemma.findByLemmas(words);
        return getIndexesCorrespondingTolLemmas(lemmas);
    }

    private List<Index> searchBySite(Set<String> words, Site site) {
        List<Lemma> lemmas = repositoryLemma.selectLemmasBySite(words, site);
        return getIndexesCorrespondingTolLemmas(lemmas);
    }

    private List<Index> getIndexesCorrespondingTolLemmas(List<Lemma> lemmas) {
        List<Index> foundIndexes = new ArrayList<>();
        Set<Page> foundPages = new HashSet<>();

        if (lemmas.isEmpty()) return foundIndexes;
        lemmas.sort(Comparator.comparingInt(Lemma::getFrequency));

        String rareLemma = lemmas.get(0).getLemma();
        foundIndexes = repositoryIndex.findByLemma(lemmas.get(0));
        foundIndexes.forEach(temp -> foundPages.add(temp.getPage()));

        if (lemmas.size() == 1) return foundIndexes;

        for (Lemma lemma : lemmas.subList(1, lemmas.size())) {
            List<Index> indexesOfLemma = repositoryIndex.findByLemma(lemma);
            List<Index> filteredIndexesOfLemma = new ArrayList<>();

            for (Index index : indexesOfLemma) {
                if (foundPages.contains(index.getPage()) ||
                        index.getLemma().getLemma().equals(rareLemma)) {
                    filteredIndexesOfLemma.add(index);
                }
            }
            foundPages.clear();
            filteredIndexesOfLemma.forEach(temp -> foundPages.add(temp.getPage()));
            foundIndexes.addAll(filteredIndexesOfLemma);
        }

        foundIndexes.removeIf(index -> !foundPages.contains(index.getPage()));

        return foundIndexes;
    }

    private Set<RelevancePage> getRelevantSet(List<Index> indexes) {
        Set<RelevancePage> pageSet = new HashSet<>();

        for (Index index : indexes) {

            RelevancePage existingPage = pageSet.stream().filter(temp -> temp.getPage().equals(index.getPage())).findFirst().orElse(null);
            if (existingPage != null) {
                existingPage.putRankWord(index.getLemma().getLemma(), index.getRank());
                continue;
            }

            RelevancePage page = new RelevancePage(index.getPage());
            page.putRankWord(index.getLemma().getLemma(), index.getRank());
            pageSet.add(page);

        }

        float maxRelevance = 0.0f;

        for (RelevancePage page : pageSet) {
            float absRelevance = page.getAbsRelevance();
            if (absRelevance > maxRelevance) {
                maxRelevance = absRelevance;
            }
        }

        for (RelevancePage page : pageSet) {
            page.setRelevance(page.getAbsRelevance() / maxRelevance);
        }

        return pageSet;
    }

    private List<DataSearchItem> getDataList(Set<RelevancePage> relevancePages) {
        List<DataSearchItem> result = new ArrayList<>();

        for (RelevancePage page : relevancePages) {
            DataSearchItem item = new DataSearchItem();
            item.setSite(page.getPage().getSite().getUrl());
            item.setSiteName(page.getPage().getSite().getName());
            item.setUri(page.getPage().getPath());

            String title = LemmaIndexer.clearContentFromTag(page.getPage().getContent(), "title");
            if (title.length() > 50) {
                title = title.substring(0,50).concat("...");
            }
            item.setTitle(title);
            item.setRelevance(page.getRelevance());

            String titles = LemmaIndexer.clearContentFromTag(page.getPage().getContent(), "title");
            String body = LemmaIndexer.clearContentFromTag(page.getPage().getContent(), "body");
            String text = titles.concat(body);

            item.setSnippet(searchSnippets(text, page.getRankWords().keySet()));

            result.add(item);
        }

        return result;
    }

    private String searchSnippets(String text, Set<String> requiredLemmas) {

        return "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n";
    }


}
