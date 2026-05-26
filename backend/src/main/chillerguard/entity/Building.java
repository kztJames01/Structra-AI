package chillerguard.entity;

import chillerguard.crypto.AesGcmStringConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//building entity
@Entity
@Table(name = "buildings", indexes = {
    @Index(name = "idx_building_external_id", columnList = "externalId", unique = true),
    @Index(name = "idx_building_status", columnList = "status")
})
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    @Convert(converter = AesGcmStringConverter.class)
    private String address;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false, unique = true, length = 100)
    private String externalId;

    @Column(length = 50)
    private String bmsType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BuildingStatus status = BuildingStatus.ACTIVE;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChillerUnit> chillerUnits = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum BuildingStatus {
        ACTIVE, INACTIVE, MAINTENANCE, DECOMMISSIONED
    }

    //getters/setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getExternalId() {
        return externalId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getBmsType() {
        return bmsType;
    }

    public void setBmsType(String bmsType) {
        this.bmsType = bmsType;
    }

    public BuildingStatus getStatus() {
        return status;
    }

    public void setStatus(BuildingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ChillerUnit> getChillerUnits() {
        return chillerUnits;
    }

    public void setChillerUnits(List<ChillerUnit> chillerUnits) {
        this.chillerUnits = chillerUnits;
    }

    public void addChillerUnit(ChillerUnit unit) {
        chillerUnits.add(unit);
        unit.setBuilding(this);
    }

    public void removeChillerUnit(ChillerUnit unit) {
        chillerUnits.remove(unit);
        unit.setBuilding(null);
    }
}
