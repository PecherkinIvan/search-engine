package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

@Repository
public interface RepositoryPage extends CrudRepository<Page, Long> {
    Page findEntityPageByPath(String path);
}
