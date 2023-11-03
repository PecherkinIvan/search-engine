package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

@Repository
public interface SiteRepository extends CrudRepository<Site, Long> {

    Site findSiteByUrl(String url);

    @Query("select e.status from Site as e where e.url =:url")
    Site.Status findStatusByUrl(String url);
}
