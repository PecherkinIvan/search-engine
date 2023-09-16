package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface RepositoryIndex extends CrudRepository<Index, Long> {

    @Query("SELECT e FROM Index e WHERE e.page = page")
    List<Index> findByPageIn(Page page);
}
