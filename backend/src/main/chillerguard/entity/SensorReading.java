package chillerguard.entity;

import chillerguard.crypto.AesGcmStringConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

//sensor reading time series row
@Entity
@Table(name = "sensor_readings", indexes = {
    @Index(name = "idx_reading_unit_id", columnList = "chillerUnitId"),
    @Index(name = "idx_reading_timestamp", columnList = "timestamp"),
    @Index(name = "idx_reading_unit_timestamp", columnList = "chillerUnitId, timestamp"),
    @Index(name = "idx_reading_source", columnList = "dataSource")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chiller_unit_id", nullable = false)
    private UUID chillerUnitId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chiller_unit_id", insertable = false, updatable = false)
    private ChillerUnit chillerUnit;

    @Column(nullable = false)
    private Instant timestamp;

    //temps celsius
    @Column(precision = 8, scale = 3)
    private java.math.BigDecimal inletTemp;

    @Column(precision = 8, scale = 3)
    private java.math.BigDecimal outletTemp;

    @Column(precision = 8, scale = 3)
    private java.math.BigDecimal ambientTemp;

    //pressure kpa
    @Column(precision = 10, scale = 3)
    private java.math.BigDecimal suctionPressure;

    @Column(precision = 10, scale = 3)
    private java.math.BigDecimal dischargePressure;

    @Column(precision = 10, scale = 3)
    private java.math.BigDecimal condenserPressure;

    //power kw
    @Column(precision = 10, scale = 3)
    private java.math.BigDecimal powerConsumption;

    @Column(precision = 10, scale = 3)
    private java.math.BigDecimal compressorCurrent;

    //vibration mm/s
    @Column(precision = 8, scale = 4)
    private java.math.BigDecimal vibrationX;

    @Column(precision = 8, scale = 4)
    private java.math.BigDecimal vibrationY;

    @Column(precision = 8, scale = 4)
    private java.math.BigDecimal vibrationZ;

    //flow l/min
    @Column(precision = 10, scale = 3)
    private java.math.BigDecimal coolantFlowRate;

    //runtime state
    @Column(length = 50)
    private String operationalMode;

    @Column
    private Boolean compressorRunning;

    @Column
    private Integer runtimeHours;

    //source + quality
    @Column(nullable = false, length = 50)
    private String dataSource;

    @Column(length = 100)
    @Convert(converter = AesGcmStringConverter.class)
    private String bmsPointIds;

    @Column
    private Boolean isValid = true;

    @Column(length = 500)
    @Convert(converter = AesGcmStringConverter.class)
    private String validationErrors;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.receivedAt == null) {
            this.receivedAt = now;
        }
    }

    //getters/setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getChillerUnitId() {
        return chillerUnitId;
    }

    public void setChillerUnitId(UUID chillerUnitId) {
        this.chillerUnitId = chillerUnitId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public ChillerUnit getChillerUnit() {
        return chillerUnit;
    }

    public void setChillerUnit(ChillerUnit chillerUnit) {
        this.chillerUnit = chillerUnit;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public java.math.BigDecimal getInletTemp() {
        return inletTemp;
    }

    public void setInletTemp(java.math.BigDecimal inletTemp) {
        this.inletTemp = inletTemp;
    }

    public java.math.BigDecimal getOutletTemp() {
        return outletTemp;
    }

    public void setOutletTemp(java.math.BigDecimal outletTemp) {
        this.outletTemp = outletTemp;
    }

    public java.math.BigDecimal getAmbientTemp() {
        return ambientTemp;
    }

    public void setAmbientTemp(java.math.BigDecimal ambientTemp) {
        this.ambientTemp = ambientTemp;
    }

    public java.math.BigDecimal getSuctionPressure() {
        return suctionPressure;
    }

    public void setSuctionPressure(java.math.BigDecimal suctionPressure) {
        this.suctionPressure = suctionPressure;
    }

    public java.math.BigDecimal getDischargePressure() {
        return dischargePressure;
    }

    public void setDischargePressure(java.math.BigDecimal dischargePressure) {
        this.dischargePressure = dischargePressure;
    }

    public java.math.BigDecimal getCondenserPressure() {
        return condenserPressure;
    }

    public void setCondenserPressure(java.math.BigDecimal condenserPressure) {
        this.condenserPressure = condenserPressure;
    }

    public java.math.BigDecimal getPowerConsumption() {
        return powerConsumption;
    }

    public void setPowerConsumption(java.math.BigDecimal powerConsumption) {
        this.powerConsumption = powerConsumption;
    }

    public java.math.BigDecimal getCompressorCurrent() {
        return compressorCurrent;
    }

    public void setCompressorCurrent(java.math.BigDecimal compressorCurrent) {
        this.compressorCurrent = compressorCurrent;
    }

    public java.math.BigDecimal getVibrationX() {
        return vibrationX;
    }

    public void setVibrationX(java.math.BigDecimal vibrationX) {
        this.vibrationX = vibrationX;
    }

    public java.math.BigDecimal getVibrationY() {
        return vibrationY;
    }

    public void setVibrationY(java.math.BigDecimal vibrationY) {
        this.vibrationY = vibrationY;
    }

    public java.math.BigDecimal getVibrationZ() {
        return vibrationZ;
    }

    public void setVibrationZ(java.math.BigDecimal vibrationZ) {
        this.vibrationZ = vibrationZ;
    }

    public java.math.BigDecimal getCoolantFlowRate() {
        return coolantFlowRate;
    }

    public void setCoolantFlowRate(java.math.BigDecimal coolantFlowRate) {
        this.coolantFlowRate = coolantFlowRate;
    }

    public String getOperationalMode() {
        return operationalMode;
    }

    public void setOperationalMode(String operationalMode) {
        this.operationalMode = operationalMode;
    }

    public Boolean getCompressorRunning() {
        return compressorRunning;
    }

    public void setCompressorRunning(Boolean compressorRunning) {
        this.compressorRunning = compressorRunning;
    }

    public Integer getRuntimeHours() {
        return runtimeHours;
    }

    public void setRuntimeHours(Integer runtimeHours) {
        this.runtimeHours = runtimeHours;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getBmsPointIds() {
        return bmsPointIds;
    }

    public void setBmsPointIds(String bmsPointIds) {
        this.bmsPointIds = bmsPointIds;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
