package com.agroverse.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scheme_applications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "scheme_id"})
})
public class SchemeApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "scheme_id", nullable = false)
    private GovernmentScheme scheme;

    @Enumerated(EnumType.STRING)
    private SchemeApplicationStatus status;

    private String remarks;
    private String externalApplicationReference;

    @CreationTimestamp
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime lastUpdatedAt;
}
