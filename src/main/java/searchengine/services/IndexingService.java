package searchengine.services;

import lombok.RequiredArgsConstructor;

import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.config.UserAgentsCfg;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.*;
import searchengine.utils.index.SiteIndexer;
import searchengine.utils.morphology.LemmaIndexer;
import searchengine.utils.parse.LinkParser;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingService implements IndexingServiceInter {

    private final SitesList sitesList;
    private final UserAgentsCfg agentCfg;
    @Autowired
    private RepositorySite repositorySite;
    @Autowired
    private RepositoryPage repositoryPage;
    @Autowired
    private RepositoryLemma repositoryLemma;
    @Autowired
    private RepositoryIndex repositoryIndex;

    @Override
    public IndexingResponse startIndexing() {

//        if (isIndexing()) {
//            return new IndexingResponse("Индексация уже запущена");
//        }

        repositoryPage.deleteAll();
        repositorySite.deleteAll();
        repositoryLemma.deleteAll();
        repositoryIndex.deleteAll();

        for (searchengine.config.Site site : sitesList.getSites()) {
            new Thread(() -> {
                Site modelSite = new Site(Site.Status.INDEXING, new Date(), site.getUrl(), site.getName());
                repositorySite.save(modelSite);

                new ForkJoinPool().invoke(new SiteIndexer(modelSite, repositoryPage, repositorySite,
                                            repositoryLemma, repositoryIndex, agentCfg));

                if (SiteIndexer.isIndexing()) {
                    modelSite.setStatusTime(new Date());
                    modelSite.setStatus(Site.Status.INDEX);
                    repositorySite.save(modelSite);
                    System.out.println("FINISH");
                }
            }).start();
        }

        return new IndexingResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {

        if (!isIndexing()) {
            return new IndexingResponse("Индексация не запущена");
        }

        SiteIndexer.stopIndexing();
        System.out.println("STOP");
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(() -> {
            List<Site> all = (List<Site>) repositorySite.findAll();
            for (Site site : all) {
                if (site.getStatus() == Site.Status.INDEXING) {
                    site.setStatus(Site.Status.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    repositorySite.save(site);
                }
            }
        }, 3000, TimeUnit.MILLISECONDS);

        return new IndexingResponse();
    }

    @Override
    public synchronized IndexingResponse indexPage(String url) {

        if (url.trim().isEmpty()) {
            return new IndexingResponse("Страница не указана");
        }
        url = normalUrl(url).trim();

        searchengine.config.Site siteCfg = findSiteCfgByUrl(url);
        if (siteCfg == null) {
            return new IndexingResponse("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }

        Site modelSite = repositorySite.findEntitySiteByUrl(siteCfg.getUrl());
        if (modelSite == null) {
            modelSite = new Site(Site.Status.INDEXING, new Date(), siteCfg.getUrl(), siteCfg.getName());
            repositorySite.save(modelSite);
        }

        String path = url.substring(normalUrl(modelSite.getUrl()).length());
        path = path.equals("") ? "/" : path;
        deletePage(path);

        return indexAndSavePage(url, modelSite, path);
    }

    private String normalUrl(String url) {
        return url.replace("www.", "");
    }

    private boolean isIndexing() {
        List<Site> all = (List<Site>) repositorySite.findAll();
        for (Site entitySite : all) {
            if (entitySite.getStatus() == Site.Status.INDEXING) {
                return true;
            }
        }
        return false;
    }

    private searchengine.config.Site findSiteCfgByUrl(String finalUrl) {
        searchengine.config.Site siteCfg;

        siteCfg = sitesList.getSites().stream()
                .filter(site -> finalUrl.contains( normalUrl(site.getUrl())) )
                .findFirst()
                .orElse(null);

        return siteCfg;
    }

    private IndexingResponse indexAndSavePage(String url, Site modelSite, String path) {
        try {
            Connection connection = LinkParser.getConnection(url, agentCfg);
            int statusCode = LinkParser.getStatusCode(connection);
            if (statusCode >= 400 && statusCode <= 599) {
                return new IndexingResponse("Код ответа страницы: " + statusCode);
            }

            String content = LinkParser.getContent(connection);
            Page newPage = new Page(modelSite, path, statusCode, content);
            repositoryPage.save(newPage);
            new LemmaIndexer(modelSite, newPage, repositoryLemma, repositoryIndex).run();
            modelSite.setStatus(Site.Status.INDEX);
            modelSite.setStatusTime(new Date());
            repositorySite.save(modelSite);

            return new IndexingResponse();

        } catch (IOException ex) {
            System.out.println(ex + " -- " + url);
            modelSite.setStatus(Site.Status.FAILED);
            modelSite.setLastError("Ошибка подключения: страница " + modelSite.getUrl() + path);
            modelSite.setStatusTime(new Date());
            repositorySite.save(modelSite);
            return new IndexingResponse("Ошибка подключения");
        }
    }

    private void deletePage(String path) {
        Page oldPage = repositoryPage.findEntityPageByPath(path);
        if (oldPage != null) {
            List<Index> entities = repositoryIndex.findByPageIn(oldPage);
            entities.forEach(entity -> {
                Lemma lemma = entity.getLemma();
                lemma.setFrequency(lemma.getFrequency() - 1);
                repositoryLemma.save(lemma);
            });

            repositoryIndex.deleteAll(entities);
            repositoryPage.delete(oldPage);
        }
    }
}

