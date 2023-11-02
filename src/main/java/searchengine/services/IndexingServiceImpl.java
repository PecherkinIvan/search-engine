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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final UserAgentsCfg agentCfg;
    @Autowired
    private SiteRepository repositorySite;
    @Autowired
    private PageRepository repositoryPage;
    @Autowired
    private LemmaRepository repositoryLemma;
    @Autowired
    private IndexRepository repositoryIndex;

    @Override
    public IndexingResponse startIndexing() {

        if (isIndexing()) {
            return new IndexingResponse("Индексация уже запущена");
        }

        System.out.println("** START INDEXING ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        repositoryIndex.deleteAll();
        repositoryLemma.deleteAll();
        repositoryPage.deleteAll();
        repositorySite.deleteAll();

        for (searchengine.config.Site site : sitesList.getSites()) {
            new Thread(() -> {

                Site modelSite = new Site(Site.Status.INDEXING, new Date(), site.getUrl(), site.getName());
                repositorySite.save(modelSite);

                new ForkJoinPool().invoke(new SiteIndexer(modelSite, repositoryPage, repositorySite,
                                            repositoryLemma, repositoryIndex, agentCfg));


                if (SiteIndexer.isIndexing()) {
                    System.out.println("** SITE " + site.getUrl() + " IS INDEXED ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
                    modelSite.setStatusTime(new Date());
                    modelSite.setStatus(Site.Status.INDEXED);
                    repositorySite.save(modelSite);
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

        System.out.println("** STOP INDEXING ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        SiteIndexer.stopIndexing();
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(() -> {
            List<Site> all = (List<Site>) repositorySite.findAll();
            for (Site site : all) {
                if (site.getStatus() == Site.Status.INDEXING) {
                    site.setStatus(Site.Status.FAILED);
                    site.setStatusTime(new Date());
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


    //** Methods that are used to operate the IndexPage() method  **//

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
            System.out.println("** PAGE " + url + " IS INDEXED ** " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
            modelSite.setStatus(Site.Status.INDEXED);
            modelSite.setLastError("");
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

