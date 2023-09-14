package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.RepositoryLemma;
import searchengine.repositories.RepositoryPage;
import searchengine.repositories.RepositorySite;

import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final RepositorySite repositorySite;
    private final RepositoryPage repositoryPage;
    private final RepositoryLemma repositoryLemma;
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
       TotalStatistics total = new TotalStatistics();
       total.setSites(sites.getSites().size());
       total.setIndexing(false);

       List<Site> siteList = sites.getSites();
       List<DetailedStatisticsItem> detailed = new ArrayList<>();
       for (Site site : siteList) {
           searchengine.model.Site.Status statusSite = repositorySite.findStatusByUrl(site.getUrl());
           if (statusSite != null && statusSite.equals(searchengine.model.Site.Status.INDEXING)) {
               total.setIndexing(true);
           }
           DetailedStatisticsItem item = new DetailedStatisticsItem();
           item.setName(site.getName());
           item.setUrl(site.getUrl());
           searchengine.model.Site modelSite = repositorySite.findEntitySiteByUrl(site.getUrl());
           Integer pagesCount = repositoryPage.countBySite(modelSite);
           pagesCount = pagesCount == null ? 0 : pagesCount;
           item.setPages(pagesCount);
           int lemmasCount = repositoryLemma.countBySite(modelSite);
           item.setLemmas(lemmasCount);
           item.setStatus(statusSite == null ? "" : statusSite.toString());
           item.setError(modelSite != null ? modelSite.getLastError() : "");
           item.setStatusTime(modelSite != null ? modelSite.getStatusTime().getTime() : new Date().getTime()); // Обратить внимание!

           total.setPages(total.getPages() + pagesCount);
           total.setLemmas(total.getLemmas() + lemmasCount);
           detailed.add(item);
       }

       return getResponse(total, detailed);
    }

    private StatisticsResponse getResponse(TotalStatistics total, List<DetailedStatisticsItem> items) {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(items);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
