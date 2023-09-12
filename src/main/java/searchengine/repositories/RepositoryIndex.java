package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

@Repository
public interface RepositoryIndex extends CrudRepository<Index, Long> {
}
