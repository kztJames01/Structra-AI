package chillerguard.dto;

import java.time.Instant;
import java.util.List;

//batch ingest response summary
public class BatchSensorReadingResponse {

    private String batchId;
    private int totalReceived;
    private int successful;
    private int failed;
    private String status;
    private Instant processedAt;
    private List<SensorReadingResponse> results;

    public BatchSensorReadingResponse() {
        this.processedAt = Instant.now();
    }

    public BatchSensorReadingResponse(String batchId, int totalReceived, int successful, int failed,
                                       String status, List<SensorReadingResponse> results) {
        this.batchId = batchId;
        this.totalReceived = totalReceived;
        this.successful = successful;
        this.failed = failed;
        this.status = status;
        this.results = results;
        this.processedAt = Instant.now();
    }

    //getters/setters
    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getTotalReceived() {
        return totalReceived;
    }

    public void setTotalReceived(int totalReceived) {
        this.totalReceived = totalReceived;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public List<SensorReadingResponse> getResults() {
        return results;
    }

    public void setResults(List<SensorReadingResponse> results) {
        this.results = results;
    }

    //all ok
    public static BatchSensorReadingResponse success(String batchId, int total, int successful,
                                                      List<SensorReadingResponse> results) {
        return new BatchSensorReadingResponse(batchId, total, successful, total - successful,
                "SUCCESS", results);
    }

    //some failed
    public static BatchSensorReadingResponse partial(String batchId, int total, int successful, int failed,
                                                      List<SensorReadingResponse> results) {
        return new BatchSensorReadingResponse(batchId, total, successful, failed,
                "PARTIAL", results);
    }

    //all failed
    public static BatchSensorReadingResponse failure(String batchId, int total, String errorMessage,
                                                      List<SensorReadingResponse> results) {
        return new BatchSensorReadingResponse(batchId, total, 0, total,
                "FAILED", results);
    }
}
