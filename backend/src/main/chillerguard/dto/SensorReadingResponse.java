package chillerguard.dto;

import java.time.Instant;
import java.util.UUID;

//single reading ingest response
public class SensorReadingResponse {

    private UUID id;
    private UUID chillerUnitId;
    private String chillerUnitExternalId;
    private Instant timestamp;
    private String status;
    private String message;
    private Instant processedAt;

    public SensorReadingResponse() {
    }

    public SensorReadingResponse(UUID id, UUID chillerUnitId, String chillerUnitExternalId,
                                  Instant timestamp, String status, String message) {
        this.id = id;
        this.chillerUnitId = chillerUnitId;
        this.chillerUnitExternalId = chillerUnitExternalId;
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.processedAt = Instant.now();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    //success helper
    public static SensorReadingResponse success(UUID id, UUID chillerUnitId, String externalId, Instant timestamp) {
        return new SensorReadingResponse(id, chillerUnitId, externalId, timestamp, "SUCCESS", "Reading ingested successfully");
    }

    //error helper
    public static SensorReadingResponse error(String externalId, String errorMessage) {
        return new SensorReadingResponse(null, null, externalId, null, "ERROR", errorMessage);
    }
}
