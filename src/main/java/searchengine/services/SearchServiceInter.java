package searchengine.services;

import searchengine.dto.search.SearchResponse;

public interface SearchServiceInter {
    SearchResponse getSearch(String query, String site);
}
