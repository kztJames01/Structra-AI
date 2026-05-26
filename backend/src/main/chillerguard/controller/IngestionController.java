package chillerguard.controller;

import chillerguard.dto.*;
import chillerguard.service.IngestionService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

//sensor ingestion endpoints
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    //ingest one reading
    @PostMapping("/readings")
    @Timed(value = "ingestion.single", description = "Time taken to ingest a single reading")
    public ResponseEntity<SensorReadingResponse> ingestReading(
            @Valid @RequestBody SensorReadingRequest request) {

        logger.debug("Received single reading ingestion request for unit: {}", request.getChillerUnitExternalId());

        SensorReadingResponse response = ingestionService.ingestReading(request);

        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    //ingest one reading async
    @PostMapping("/readings/async")
    @Timed(value = "ingestion.single.async", description = "Time taken to ingest a single reading asynchronously")
    public CompletableFuture<ResponseEntity<SensorReadingResponse>> ingestReadingAsync(
            @Valid @RequestBody SensorReadingRequest request) {

        logger.debug("Received async single reading ingestion request for unit: {}",
                request.getChillerUnitExternalId());

        return ingestionService.ingestReadingAsync(request)
                .thenApply(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                });
    }

    //ingest batch
    @PostMapping("/readings/batch")
    @Timed(value = "ingestion.batch", description = "Time taken to ingest a batch of readings")
    public ResponseEntity<BatchSensorReadingResponse> ingestBatch(
            @Valid @RequestBody BatchSensorReadingRequest request) {

        logger.info("Received batch ingestion request with {} readings", request.getReadings().size());

        BatchSensorReadingResponse response = ingestionService.ingestBatch(request);

        HttpStatus status = switch (response.getStatus()) {
            case "SUCCESS" -> HttpStatus.CREATED;
            case "PARTIAL" -> HttpStatus.MULTI_STATUS;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }

    //ingest batch async
    @PostMapping("/readings/batch/async")
    @Timed(value = "ingestion.batch.async", description = "Time taken to ingest a batch of readings asynchronously")
    public CompletableFuture<ResponseEntity<BatchSensorReadingResponse>> ingestBatchAsync(
            @Valid @RequestBody BatchSensorReadingRequest request) {

        logger.info("Received async batch ingestion request with {} readings", request.getReadings().size());

        return ingestionService.ingestBatchAsync(request)
                .thenApply(response -> {
                    HttpStatus status = switch (response.getStatus()) {
                        case "SUCCESS" -> HttpStatus.CREATED;
                        case "PARTIAL" -> HttpStatus.MULTI_STATUS;
                        default -> HttpStatus.BAD_REQUEST;
                    };
                    return ResponseEntity.status(status).body(response);
                });
    }

    //ingestion health check
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ingestion service is running");
    }
}
