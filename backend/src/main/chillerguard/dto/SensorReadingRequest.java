package chillerguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

//single reading ingest payload
public class SensorReadingRequest {

    @NotBlank(message = "Chiller unit external ID is required")
    @Size(max = 100, message = "External ID must not exceed 100 characters")
    private String chillerUnitExternalId;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    //temps
    private BigDecimal inletTemp;
    private BigDecimal outletTemp;
    private BigDecimal ambientTemp;

    //pressure
    private BigDecimal suctionPressure;
    private BigDecimal dischargePressure;
    private BigDecimal condenserPressure;

    //power
    private BigDecimal powerConsumption;
    private BigDecimal compressorCurrent;

    //vibration
    private BigDecimal vibrationX;
    private BigDecimal vibrationY;
    private BigDecimal vibrationZ;

    //flow
    private BigDecimal coolantFlowRate;

    //ops state
    @Size(max = 50, message = "Operational mode must not exceed 50 characters")
    private String operationalMode;

    private Boolean compressorRunning;
    private Integer runtimeHours;

    //metadata
    @NotBlank(message = "Data source is required")
    @Size(max = 50, message = "Data source must not exceed 50 characters")
    private String dataSource;

    @Size(max = 100, message = "BMS point IDs must not exceed 100 characters")
    private String bmsPointIds;

    //getters/setters
    public String getChillerUnitExternalId() {
        return chillerUnitExternalId;
    }

    public void setChillerUnitExternalId(String chillerUnitExternalId) {
        this.chillerUnitExternalId = chillerUnitExternalId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getInletTemp() {
        return inletTemp;
    }

    public void setInletTemp(BigDecimal inletTemp) {
        this.inletTemp = inletTemp;
    }

    public BigDecimal getOutletTemp() {
        return outletTemp;
    }

    public void setOutletTemp(BigDecimal outletTemp) {
        this.outletTemp = outletTemp;
    }

    public BigDecimal getAmbientTemp() {
        return ambientTemp;
    }

    public void setAmbientTemp(BigDecimal ambientTemp) {
        this.ambientTemp = ambientTemp;
    }

    public BigDecimal getSuctionPressure() {
        return suctionPressure;
    }

    public void setSuctionPressure(BigDecimal suctionPressure) {
        this.suctionPressure = suctionPressure;
    }

    public BigDecimal getDischargePressure() {
        return dischargePressure;
    }

    public void setDischargePressure(BigDecimal dischargePressure) {
        this.dischargePressure = dischargePressure;
    }

    public BigDecimal getCondenserPressure() {
        return condenserPressure;
    }

    public void setCondenserPressure(BigDecimal condenserPressure) {
        this.condenserPressure = condenserPressure;
    }

    public BigDecimal getPowerConsumption() {
        return powerConsumption;
    }

    public void setPowerConsumption(BigDecimal powerConsumption) {
        this.powerConsumption = powerConsumption;
    }

    public BigDecimal getCompressorCurrent() {
        return compressorCurrent;
    }

    public void setCompressorCurrent(BigDecimal compressorCurrent) {
        this.compressorCurrent = compressorCurrent;
    }

    public BigDecimal getVibrationX() {
        return vibrationX;
    }

    public void setVibrationX(BigDecimal vibrationX) {
        this.vibrationX = vibrationX;
    }

    public BigDecimal getVibrationY() {
        return vibrationY;
    }

    public void setVibrationY(BigDecimal vibrationY) {
        this.vibrationY = vibrationY;
    }

    public BigDecimal getVibrationZ() {
        return vibrationZ;
    }

    public void setVibrationZ(BigDecimal vibrationZ) {
        this.vibrationZ = vibrationZ;
    }

    public BigDecimal getCoolantFlowRate() {
        return coolantFlowRate;
    }

    public void setCoolantFlowRate(BigDecimal coolantFlowRate) {
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
}
