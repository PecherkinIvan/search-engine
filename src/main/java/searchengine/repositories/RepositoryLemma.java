package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

@Repository
public interface RepositoryLemma extends CrudRepository<Lemma, Long> {
    Lemma findByLemmaAndSite(String lemma, Site site);

    Integer countBySite(Site site);
}
