package chillerguard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

//batch reading ingest payload
public class BatchSensorReadingRequest {

    @NotEmpty(message = "At least one reading is required")
    @Size(max = 1000, message = "Batch size must not exceed 1000 readings")
    @Valid
    private List<SensorReadingRequest> readings;

    //optional batch metadata
    private String batchId;
    private String sourceSystem;

    public BatchSensorReadingRequest() {
    }

    public BatchSensorReadingRequest(List<SensorReadingRequest> readings) {
        this.readings = readings;
    }

    //getters/setters
    public List<SensorReadingRequest> getReadings() {
        return readings;
    }

    public void setReadings(List<SensorReadingRequest> readings) {
        this.readings = readings;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
}
