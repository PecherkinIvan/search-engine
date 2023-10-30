package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

@Repository
public interface PageRepository extends CrudRepository<Page, Long> {
    Page findEntityPageByPath(String path);

    @Query("SELECT COUNT(*) FROM Page GROUP BY site HAVING site =:modelSite")
    Integer countBySite(Site modelSite);
}
