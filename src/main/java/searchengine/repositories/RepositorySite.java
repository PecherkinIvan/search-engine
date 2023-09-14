package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

@Repository
public interface RepositorySite extends CrudRepository<Site, Long> {

    Site findEntitySiteByUrl(String url);

    @Query("select e.status from Site as e where e.url =:url")
    Site.Status findStatusByUrl(String url);

//    @Query("select e.last_error from Site as e where e.url=:url")
//    String findLastErrorByUrl(String url);

}
