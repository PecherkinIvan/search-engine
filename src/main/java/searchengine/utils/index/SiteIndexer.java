package searchengine.utils.index;

import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.UserAgentsCfg;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.morphology.LemmaIndexer;
import searchengine.utils.parse.LinkParser;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.*;

public class SiteIndexer extends RecursiveAction {

    private String url;
    private final Site modelSite;
    private final Set<String> fullUrls;
    @Autowired
    private static PageRepository repositoryPage;
    @Autowired
    private static SiteRepository repositorySite;
    @Autowired
    private static LemmaRepository repositoryLemma;
    @Autowired
    private static IndexRepository repositoryIndex;
    private static UserAgentsCfg userAgents;
    private static boolean isIndexing = true;



    public SiteIndexer(Site site, PageRepository repositoryPage, SiteRepository repositorySite,
                       LemmaRepository repositoryLemma, IndexRepository repositoryIndex, UserAgentsCfg userAgents) {
        modelSite = site;
        url = site.getUrl().replace("www.", "");
        url = !url.endsWith("/") ? (url + '/') : url;
        SiteIndexer.repositoryPage = repositoryPage;
        SiteIndexer.repositorySite = repositorySite;
        SiteIndexer.repositoryLemma = repositoryLemma;
        SiteIndexer.repositoryIndex = repositoryIndex;
        SiteIndexer.userAgents = userAgents;
        fullUrls = ConcurrentHashMap.newKeySet();
        fullUrls.add(url);
        isIndexing = true;
    }

    private SiteIndexer(String url, Site modelSite, Set<String> fullUrls) {
        this.url = url;
        this.modelSite = modelSite;
        this.fullUrls = fullUrls;
    }

    @Override
    protected void compute() {

        if (!isIndexing) return;

        CopyOnWriteArrayList<SiteIndexer> taskList = new CopyOnWriteArrayList<>();
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();
        Connection connection = LinkParser.getConnection(url, userAgents);
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

        if (!isIndexing) {
            modelSite.setStatus(Site.Status.FAILED);
            modelSite.setLastError("Индексация остановлена пользователем");
        }
        modelSite.setStatusTime(new Date());
        repositorySite.save(modelSite);


        // Создание и сохранение лемм, индексов
        new LemmaIndexer(modelSite, pageNew, repositoryLemma, repositoryIndex).run();

        for (String link : links) {
            if (!isIndexing) return;
            if (link.contains(url) && !fullUrls.contains(link)) {
                SiteIndexer task = new SiteIndexer(link, modelSite, fullUrls);
                fullUrls.add(link);
                task.fork();
                taskList.add(task);
            }
        }
        taskList.forEach(ForkJoinTask::join);
    }

    public static void stopIndexing() {
        isIndexing = false;
    }

    public static boolean isIndexing() {
        return isIndexing;
    }

    private String getPath(String url) {
        String path = url.substring(modelSite.getUrl().replace("www.", "").length());
        return path.equals("") ? "/" : path;
    }
}
