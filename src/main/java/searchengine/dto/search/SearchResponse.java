package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<DataSearchItem> data;
    private String error;

    public SearchResponse(String error) {
        this.result = false;
        this.error = error;
        data = null;
        count = null;
    }

    public SearchResponse(int count, List<DataSearchItem> data) {
        this.result = true;
        this.count = count;
        this.data = data;
        error = null;
    }
}
