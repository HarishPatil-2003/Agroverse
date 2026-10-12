package com.agroverse.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "government_schemes")
public class GovernmentScheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String shortDescription;

    @Column(length = 5000)
    private String fullDescription;

    private String schemeType;
    private String category;

    @ElementCollection
    @CollectionTable(name = "scheme_applicable_states", joinColumns = @JoinColumn(name = "scheme_id"))
    @Column(name = "state")
    private List<String> applicableStates;

    @ElementCollection
    @CollectionTable(name = "scheme_applicable_crops", joinColumns = @JoinColumn(name = "scheme_id"))
    @Column(name = "crop_type")
    private List<String> applicableCropTypes;

    @Column(length = 2000)
    private String eligibility;

    @Column(length = 2000)
    private String benefits;

    @ElementCollection
    @CollectionTable(name = "scheme_documents", joinColumns = @JoinColumn(name = "scheme_id"))
    @Column(name = "document")
    private List<String> documentsRequired;

    @Column(length = 2000)
    private String applicationProcess;

    private String officialWebsite;
    private String imageUrl;
    private String status;
    private LocalDate deadlineDate;
    private String subsidyAmount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
