package com.agroverse.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_items")
public class CropItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String field;

    @Column(nullable = false)
    private String status;

    @Column(name = "last_checked")
    private String lastChecked;

    private String image;

    @Column(length = 100)
    private String variety;

    @Column(name = "sowing_date")
    private java.time.LocalDate sowingDate;

    @Column(name = "growth_stage", length = 50)
    private String growthStage;

    @Column(name = "expected_harvest_date")
    private java.time.LocalDate expectedHarvestDate;

    @Column(name = "irrigation_notes", columnDefinition = "TEXT")
    private String irrigationNotes;

    @Column(name = "fertilizer_notes", columnDefinition = "TEXT")
    private String fertilizerNotes;

    @Column(name = "treatment_notes", columnDefinition = "TEXT")
    private String treatmentNotes;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CropItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.time.LocalDate getSowingDate() { return sowingDate; }
    public void setSowingDate(java.time.LocalDate sowingDate) { this.sowingDate = sowingDate; }

    public String getGrowthStage() { return growthStage; }
    public void setGrowthStage(String growthStage) { this.growthStage = growthStage; }

    public java.time.LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
    public void setExpectedHarvestDate(java.time.LocalDate expectedHarvestDate) { this.expectedHarvestDate = expectedHarvestDate; }

    public String getIrrigationNotes() { return irrigationNotes; }
    public void setIrrigationNotes(String irrigationNotes) { this.irrigationNotes = irrigationNotes; }

    public String getFertilizerNotes() { return fertilizerNotes; }
    public void setFertilizerNotes(String fertilizerNotes) { this.fertilizerNotes = fertilizerNotes; }

    public String getTreatmentNotes() { return treatmentNotes; }
    public void setTreatmentNotes(String treatmentNotes) { this.treatmentNotes = treatmentNotes; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public String getLastChecked() { return lastChecked; }
    public void setLastChecked(String lastChecked) { this.lastChecked = lastChecked; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
