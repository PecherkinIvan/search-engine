package searchengine.services.parse;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.RepositoryPage;
import searchengine.repositories.RepositorySite;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class LinkParserRecursiveAction extends RecursiveAction {

    private String url;
    private static Site modelSite;
    private static Set<String> fullUrls;
    @Autowired
    private static RepositoryPage repositoryPage;
    @Autowired
    private static RepositorySite repositorySite;
    private static boolean isStop = false;

    public LinkParserRecursiveAction(String url) {
        this.url = url;
    }

    public LinkParserRecursiveAction(String url, Site site, RepositoryPage repositoryPage, RepositorySite repositorySite) {
        this.url = !url.endsWith("/") ? (url + '/') : url;
        modelSite = site;
        this.repositoryPage = repositoryPage;
        this.repositorySite = repositorySite;
        fullUrls = ConcurrentHashMap.newKeySet();
        fullUrls.add(this.url);
        isStop = false;
    }

    @Override
    protected void compute() {
        if (isStop) return;
        List<LinkParserRecursiveAction> taskList = new ArrayList<>();
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();
        Connection connection = LinkParser.getConnection(url);
        int status = 0;
        String content = null;

        try {
            status = LinkParser.getStatusCode(connection);
            content = LinkParser.getContent(connection);
            links = LinkParser.getLinks(connection);
        } catch (IOException ex) {
            System.out.println(ex + " -- " + url);
        }

        System.out.println(Thread.currentThread().getName() + "  -- " + url);
        repositoryPage.save(new Page(modelSite, getPath(url), status, content));
        modelSite.setStatusTime(new Date());
        repositorySite.save(modelSite);


        for(String link : links) {
            if (link.contains(url) && !fullUrls.contains(link)) {
                LinkParserRecursiveAction task = new LinkParserRecursiveAction(link);
                fullUrls.add(link);
                task.fork();
                taskList.add(task);
            }
        }
        taskList.forEach(ForkJoinTask::join);

    }


    public static void stop() {
        isStop = true;
    }

    public static void start() {
        isStop = false;
    }

    public static boolean getIsStop() {
        return isStop;
    }

    private String getPath(String url) {
        String path = url.substring(modelSite.getUrl().length());  // Убрал www
        return path.equals("") ? "/" : path;
    }
}