package searchengine.services;

import lombok.RequiredArgsConstructor;

import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.*;
import searchengine.services.index.SiteIndexer;
import searchengine.services.morphology.LemmaIndexer;
import searchengine.services.parse.LinkParser;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    @Autowired
    private RepositorySite repositorySite;
    @Autowired
    private RepositoryPage repositoryPage;
    @Autowired
    private RepositoryLemma repositoryLemma;
    @Autowired
    private RepositoryIndex repositoryIndex;
    private ForkJoinPool fjp;

    @Override
    public IndexingResponse startIndexing() {

        repositoryPage.deleteAll();
        repositorySite.deleteAll();

//        if (isIndexing()) {
//            return new IndexingResponse(false, "Индексация уже запущена");
//        }

        new Thread(() -> {
            for (searchengine.config.Site site : sitesList.getSites()) {
                Site modelSite = new Site(Site.Status.INDEXING, new Date(), site.getUrl(), site.getName());
                repositorySite.save(modelSite);

                fjp = new ForkJoinPool();
                fjp.invoke(new SiteIndexer(modelSite, repositoryPage, repositorySite, repositoryLemma, repositoryIndex));


                modelSite.setStatusTime(new Date());
                modelSite.setStatus(Site.Status.INDEX);
                repositorySite.save(modelSite);
                System.out.println("FINISH");
            }
        }).start();

        return new IndexingResponse(true, null);
    }

    @Override
    public IndexingResponse stopIndexing() {
        fjp.shutdownNow();
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

        return new IndexingResponse(true, null);
    }

    @Override
    public IndexingResponse indexPage(String url) {

        if (url.trim().isEmpty()) {
            return new IndexingResponse(false, "Страница не указана");
        }
        url = normalUrl(url).trim();

        for (searchengine.config.Site siteCgf : sitesList.getSites()) {
            String siteCgfUrl = normalUrl(siteCgf.getUrl());
            if (url.contains(siteCgfUrl)) {

                Site modelSite = repositorySite.findEntitySiteByUrl(siteCgf.getUrl());
                String path = url.substring(modelSite.getUrl().replace("www.", "").length());
                path = path.equals("") ? "/" : path;

                if (url.contains(modelSite.getUrl())) {
                    Page oldPage = repositoryPage.findEntityPageByPath(path);
                    if (oldPage != null) {
                        repositoryPage.delete(oldPage);
                    }
                }
                else {
                    modelSite = new Site(Site.Status.INDEXING, new Date(), siteCgf.getUrl(), siteCgf.getName());
                    repositorySite.save(modelSite);
                }

                try {

                    Connection connection = LinkParser.getConnection(url);
                    int statusCode = LinkParser.getStatusCode(connection);
                    if (statusCode != 200) {
                        return new IndexingResponse(false, "Код ответа страницы: " + statusCode);
                    }
                    String content = LinkParser.getContent(connection);
                    Page newPage = new Page(modelSite, path, statusCode, content);
                    repositoryPage.save(newPage);
                    new Thread(new LemmaIndexer(modelSite, newPage, repositoryLemma, repositoryIndex)).start();
                    return new IndexingResponse(true, null);

                } catch (IOException ex) {
                    System.out.println(ex + " -- " + url);
                    return new IndexingResponse(false, "Ошибка подключения");
                }

            }
        }

        return new IndexingResponse(false, "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
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

    private String normalUrl(String url) {
        return url.replace("www.", "");
    }

    private boolean isUrlSiteContains(String url) {
        return sitesList.getSites().stream().anyMatch(site -> site.getUrl().equals(url));
    }
}

