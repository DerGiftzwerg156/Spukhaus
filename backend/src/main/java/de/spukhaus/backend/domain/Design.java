package de.spukhaus.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "designs")
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_from_design_id")
    private Design forkedFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_published_version_id")
    private DesignVersion currentPublishedVersion;

    @Column(name = "next_version_number", nullable = false)
    private int nextVersionNumber = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Design() {
    }

    public Design(User owner, Design forkedFrom) {
        this.owner = owner;
        this.forkedFrom = forkedFrom;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public int allocateNextVersionNumber() {
        return nextVersionNumber++;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Design getForkedFrom() {
        return forkedFrom;
    }

    public DesignVersion getCurrentPublishedVersion() {
        return currentPublishedVersion;
    }

    public void setCurrentPublishedVersion(DesignVersion currentPublishedVersion) {
        this.currentPublishedVersion = currentPublishedVersion;
    }

    public int getNextVersionNumber() {
        return nextVersionNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
