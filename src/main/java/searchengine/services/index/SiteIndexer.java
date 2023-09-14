package searchengine.services.index;

import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.RepositoryIndex;
import searchengine.repositories.RepositoryLemma;
import searchengine.repositories.RepositoryPage;
import searchengine.repositories.RepositorySite;
import searchengine.services.morphology.LemmaIndexer;
import searchengine.services.parse.LinkParser;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.*;

public class SiteIndexer extends RecursiveAction {

    private String url;
    private static Site modelSite;
    private static Set<String> fullUrls;
    @Autowired
    private static RepositoryPage repositoryPage;
    @Autowired
    private static RepositorySite repositorySite;
    @Autowired
    private static RepositoryLemma repositoryLemma;
    @Autowired
    private static RepositoryIndex repositoryIndex;
    private final ExecutorService executorService = Executors.newFixedThreadPool(16);


    public SiteIndexer(Site site, RepositoryPage repositoryPage, RepositorySite repositorySite,
                       RepositoryLemma repositoryLemma, RepositoryIndex repositoryIndex) {
        modelSite = site;
        url = site.getUrl().replace("www.", "");
        url = !url.endsWith("/") ? (url + '/') : url;
        SiteIndexer.repositoryPage = repositoryPage;
        SiteIndexer.repositorySite = repositorySite;
        SiteIndexer.repositoryLemma = repositoryLemma;
        SiteIndexer.repositoryIndex = repositoryIndex;
        fullUrls = ConcurrentHashMap.newKeySet();
        fullUrls.add(url);
    }

    private SiteIndexer(String url) {
        this.url = url;
    }

    @Override
    protected void compute() {
        CopyOnWriteArrayList<SiteIndexer> taskList = new CopyOnWriteArrayList<>();
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();
        Connection connection = LinkParser.getConnection(url);
        int statusCode = 0;
        String content = "";

        try {
            statusCode = LinkParser.getStatusCode(connection);
            content = LinkParser.getContent(connection);
            links = LinkParser.getLinks(connection);
        } catch (IOException ex) {
            System.out.println(ex + " -- " + url);
        }

        // Создание и сохранение page
        System.out.println(Thread.currentThread().getName() + "  -- " + url);
        Page pageNew = new Page(modelSite, getPath(url), statusCode, content);
        repositoryPage.save(pageNew);
        modelSite.setStatusTime(new Date());
        repositorySite.save(modelSite);


        // Создание и сохранение лемм, индексов
        try {
            executorService.submit(new LemmaIndexer(modelSite, pageNew, repositoryLemma, repositoryIndex)).get();
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }

        for (String link : links) {
            if (link.contains(url) && !fullUrls.contains(link)) {
                SiteIndexer task = new SiteIndexer(link);
                fullUrls.add(link);
                task.fork();
                taskList.add(task);
            }
        }
        taskList.forEach(ForkJoinTask::join);

    }


    private String getPath(String url) {
        String path = url.substring(modelSite.getUrl().replace("www.", "").length());
        return path.equals("") ? "/" : path;
    }
}
