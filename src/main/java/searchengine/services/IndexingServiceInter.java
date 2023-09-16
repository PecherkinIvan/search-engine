package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingServiceInter {

    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    IndexingResponse indexPage(String url);
}
