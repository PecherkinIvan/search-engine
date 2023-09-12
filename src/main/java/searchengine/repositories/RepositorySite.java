package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

@Repository
public interface RepositorySite extends CrudRepository<Site, Long> {

    Site findEntitySiteByUrl(String url);

}
