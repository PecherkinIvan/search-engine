package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String url, name;

    public Site() {
    }

    public Site(Status status, Date statusTime, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = "";
        this.url = url;
        this.name = name;
    }

    public enum Status {
        INDEXING, INDEXED, FAILED
    }
}
