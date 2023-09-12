package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.context.annotation.DependsOn;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)  //  cascade = CascadeType.MERGE
    @JoinColumn(name="site_id", nullable=false, foreignKey = @ForeignKey(name = "FK_page_site"))
    private Site site;

    @Column(columnDefinition = "TEXT NOT NULL, index(path(512))")
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    public Page() {

    }

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

}
