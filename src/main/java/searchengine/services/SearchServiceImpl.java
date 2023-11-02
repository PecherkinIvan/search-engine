package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.DataSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.morphology.LemmaFinder;
import searchengine.utils.morphology.LemmaIndexer;
import searchengine.utils.relevance.RelevancePage;
import searchengine.utils.snippet.SnippetSearch;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SiteRepository repositorySite;
    @Autowired
    private PageRepository repositoryPage;
    @Autowired
    private LemmaRepository repositoryLemma;
    @Autowired
    private IndexRepository repositoryIndex;

    private static String lastQuery;
    private static List<DataSearchItem> data;


    @Override
    public SearchResponse getSearch(String query, String siteUrl, Integer offset, Integer limit) {

        if (query.isEmpty()) return new SearchResponse("Запрос не введен");

        offset = (offset == null) ? 0 : offset;
        limit = (limit == null) ? 20 : limit;

        if (query.equals(lastQuery)) {
            buildResponse(offset, limit);
        }

        Set<String> queryLemmas = LemmaFinder.collectLemmas(query).keySet();
        List<Index> foundIndexes;

        if (siteUrl == null) {
            foundIndexes = searchByAll(queryLemmas);
        }
        else {
            Site site = repositorySite.findEntitySiteByUrl(siteUrl);
            if (site.getStatus() != Site.Status.INDEXED)  return new SearchResponse("Выбранный сайт ещё не проиндексирован");
            foundIndexes = searchBySite(queryLemmas, site);
        }
        if (foundIndexes.isEmpty()) return new SearchResponse("Ничего не найдено");

        lastQuery = query;
        data = getDataList(getRelevantList(foundIndexes));

        return buildResponse(offset, limit);
    }

    private List<Index> searchByAll(Set<String> words) {
        List<Index> indexList = new ArrayList<>();
        List<Site> sites = (List<Site>) repositorySite.findAll();

        for (Site site : sites) {
            if (site.getStatus() != Site.Status.INDEXING) {
                indexList.addAll(searchBySite(words, site));
            }
        }

        return indexList;
    }

    private List<Index> searchBySite(Set<String> words, Site site) {
        List<Lemma> lemmas = repositoryLemma.selectLemmasBySite(words, site);
        return lemmas.stream()
                .map(Lemma::getLemma)
                .collect(Collectors.toSet())
                .equals(words)
                ? getIndexesCorrespondingTolLemmas(lemmas)
                : new ArrayList<>();
    }

    private List<Index> getIndexesCorrespondingTolLemmas(List<Lemma> lemmas) {
        List<Index> foundIndexes = new ArrayList<>();
        Set<Page> foundPages = new HashSet<>();

        if (lemmas.isEmpty()) return foundIndexes;
        lemmas.sort(Comparator.comparingInt(Lemma::getFrequency));

        foundIndexes = repositoryIndex.findByLemma(lemmas.get(0));
        foundIndexes.forEach(temp -> foundPages.add(temp.getPage()));
        if (lemmas.size() == 1) return foundIndexes;

        for (Lemma lemma : lemmas.subList(1, lemmas.size())) {
            List<Index> indexesOfLemma = repositoryIndex.findByLemma(lemma);
            List<Index> filteredIndexesOfLemma = new ArrayList<>();

            for (Index index : indexesOfLemma) {
                if (foundPages.contains(index.getPage())) {
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

    private List<RelevancePage> getRelevantList(List<Index> indexes) {
        List<RelevancePage> pageSet = new ArrayList<>();

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

        pageSet.sort(Comparator.comparingDouble(RelevancePage::getRelevance).reversed());
        return pageSet;
    }

    private List<DataSearchItem> getDataList(List<RelevancePage> relevancePages) {
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
            item.setSnippet( SnippetSearch.find(text, page.getRankWords().keySet()) );

            result.add(item);
        }

        return result;
    }

    private SearchResponse buildResponse(Integer offset, Integer limit) {
        if (offset + limit >= data.size()) {
            limit = data.size() - offset;
        }
        return new SearchResponse(data.size(), data.subList(offset, offset + limit));
    }
}
