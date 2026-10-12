package com.agroverse.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_analysis_history")
public class CropAnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "farmer_id", nullable = false)
    private String farmerId;

    @Column(name = "file_name")
    private String fileName;

    @Column(nullable = false)
    private String crop;

    @Column(nullable = false)
    private String disease;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "diagnostic_certainty", nullable = false)
    private String diagnosticCertainty;

    @Column(nullable = false)
    private String severity;

    @Column(length = 2000)
    private String description;

    @Column(name = "treatments_json", columnDefinition = "TEXT")
    private String treatmentsJson;

    @Column(name = "pesticide_warning", length = 1000)
    private String pesticideWarning;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CropAnalysisHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }

    public String getDisease() { return disease; }
    public void setDisease(String disease) { this.disease = disease; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getDiagnosticCertainty() { return diagnosticCertainty; }
    public void setDiagnosticCertainty(String diagnosticCertainty) { this.diagnosticCertainty = diagnosticCertainty; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTreatmentsJson() { return treatmentsJson; }
    public void setTreatmentsJson(String treatmentsJson) { this.treatmentsJson = treatmentsJson; }

    public String getPesticideWarning() { return pesticideWarning; }
    public void setPesticideWarning(String pesticideWarning) { this.pesticideWarning = pesticideWarning; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
