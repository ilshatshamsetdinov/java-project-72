package hexlet.code.domain;

import io.ebean.Model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.List;

import io.ebean.annotation.WhenCreated;

@Entity
public final class Url extends Model {
    @Id
    private long id;

    @WhenCreated
    private Instant createdAt;
    private String name;

    public Url() {
    }

    public Url(String name) {
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
    @OneToMany(cascade = CascadeType.ALL)
    private List<UrlCheck> urlChecks;
    public List<UrlCheck> getUrlChecks() {
        return urlChecks;
    }
}
