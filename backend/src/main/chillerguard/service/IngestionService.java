package chillerguard.service;

import chillerguard.dto.*;
import chillerguard.entity.ChillerUnit;
import chillerguard.entity.SensorReading;
import chillerguard.repository.ChillerUnitRepository;
import chillerguard.repository.SensorReadingRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//ingest and validate sensor readings
@Service
public class IngestionService {

    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);

    private final SensorReadingRepository sensorReadingRepository;
    private final ChillerUnitRepository chillerUnitRepository;
    private final Counter ingestionCounter;
    private final Counter ingestionErrorCounter;
    private final Timer ingestionTimer;

    public IngestionService(SensorReadingRepository sensorReadingRepository,
                            ChillerUnitRepository chillerUnitRepository,
                            MeterRegistry meterRegistry) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.chillerUnitRepository = chillerUnitRepository;
        this.ingestionCounter = Counter.builder("sensor.ingestion.total")
                .description("Total number of sensor readings ingested")
                .register(meterRegistry);
        this.ingestionErrorCounter = Counter.builder("sensor.ingestion.errors")
                .description("Total number of ingestion errors")
                .register(meterRegistry);
        this.ingestionTimer = Timer.builder("sensor.ingestion.duration")
                .description("Time taken to ingest sensor readings")
                .register(meterRegistry);
    }

    //sync single reading ingest
    @Transactional
    public SensorReadingResponse ingestReading(SensorReadingRequest request) {
        return ingestionTimer.recordCallable(() -> {
            try {
                //find chiller unit
                Optional<ChillerUnit> unitOpt = chillerUnitRepository.findByExternalId(request.getChillerUnitExternalId());
                if (unitOpt.isEmpty()) {
                    logger.warn("Chiller unit not found: {}", request.getChillerUnitExternalId());
                    ingestionErrorCounter.increment();
                    return SensorReadingResponse.error(request.getChillerUnitExternalId(),
                            "Chiller unit not found: " + request.getChillerUnitExternalId());
                }

                ChillerUnit unit = unitOpt.get();

                //validate reading
                ValidationResult validation = validateReading(request);

                //save reading
                SensorReading reading = mapToEntity(request, unit.getId(), unit.getOrganizationId());
                reading.setIsValid(validation.isValid());
                reading.setValidationErrors(validation.getErrors());

                SensorReading saved = sensorReadingRepository.save(reading);
                ingestionCounter.increment();

                logger.debug("Ingested reading for unit {} at {}", unit.getExternalId(), request.getTimestamp());

                return SensorReadingResponse.success(saved.getId(), unit.getId(),
                        unit.getExternalId(), saved.getTimestamp());

            } catch (Exception e) {
                logger.error("Error ingesting reading for unit {}: {}",
                        request.getChillerUnitExternalId(), e.getMessage(), e);
                ingestionErrorCounter.increment();
                return SensorReadingResponse.error(request.getChillerUnitExternalId(),
                        "Internal error: " + e.getMessage());
            }
        });
    }

    //async single reading ingest
    @Async("ingestionExecutor")
    public CompletableFuture<SensorReadingResponse> ingestReadingAsync(SensorReadingRequest request) {
        return CompletableFuture.completedFuture(ingestReading(request));
    }

    //sync batch ingest
    @Transactional
    public BatchSensorReadingResponse ingestBatch(BatchSensorReadingRequest request) {
        Instant startTime = Instant.now();
        List<SensorReadingResponse> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        String batchId = request.getBatchId() != null ? request.getBatchId() : UUID.randomUUID().toString();

        logger.info("Processing batch {} with {} readings", batchId, request.getReadings().size());

        for (SensorReadingRequest readingRequest : request.getReadings()) {
            SensorReadingResponse response = ingestReading(readingRequest);
            results.add(response);

            if ("SUCCESS".equals(response.getStatus())) {
                successCount++;
            } else {
                failCount++;
            }
        }

        Instant endTime = Instant.now();
        logger.info("Batch {} completed: {}/{} successful in {}ms",
                batchId, successCount, request.getReadings().size(),
                endTime.toEpochMilli() - startTime.toEpochMilli());

        if (failCount == 0) {
            return BatchSensorReadingResponse.success(batchId, request.getReadings().size(), successCount, results);
        } else if (successCount == 0) {
            return BatchSensorReadingResponse.failure(batchId, request.getReadings().size(), "All readings failed", results);
        } else {
            return BatchSensorReadingResponse.partial(batchId, request.getReadings().size(), successCount, failCount, results);
        }
    }

    //async batch ingest
    @Async("ingestionExecutor")
    public CompletableFuture<BatchSensorReadingResponse> ingestBatchAsync(BatchSensorReadingRequest request) {
        return CompletableFuture.completedFuture(ingestBatch(request));
    }

    //basic range checks on reading fields
    private ValidationResult validateReading(SensorReadingRequest request) {
        List<String> errors = new ArrayList<>();

        //temp range check
        if (request.getInletTemp() != null) {
            if (request.getInletTemp().compareTo(new BigDecimal("-50")) < 0 ||
                request.getInletTemp().compareTo(new BigDecimal("100")) > 0) {
                errors.add("Inlet temperature out of range: " + request.getInletTemp());
            }
        }

        if (request.getOutletTemp() != null) {
            if (request.getOutletTemp().compareTo(new BigDecimal("-50")) < 0 ||
                request.getOutletTemp().compareTo(new BigDecimal("100")) > 0) {
                errors.add("Outlet temperature out of range: " + request.getOutletTemp());
            }
        }

        //pressure check
        if (request.getSuctionPressure() != null && request.getSuctionPressure().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Suction pressure cannot be negative: " + request.getSuctionPressure());
        }

        if (request.getDischargePressure() != null && request.getDischargePressure().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Discharge pressure cannot be negative: " + request.getDischargePressure());
        }

        //power check
        if (request.getPowerConsumption() != null && request.getPowerConsumption().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Power consumption cannot be negative: " + request.getPowerConsumption());
        }

        //timestamp check
        if (request.getTimestamp() != null) {
            Instant now = Instant.now();
            if (request.getTimestamp().isAfter(now.plusSeconds(60))) {
                errors.add("Timestamp is in the future: " + request.getTimestamp());
            }
            if (request.getTimestamp().isBefore(now.minusSeconds(86400 * 30))) {
                errors.add("Timestamp is older than 30 days: " + request.getTimestamp());
            }
        }

        return new ValidationResult(errors.isEmpty(), String.join("; ", errors));
    }

    //dto to entity mapping
    private SensorReading mapToEntity(SensorReadingRequest request, UUID chillerUnitId, UUID organizationId) {
        SensorReading reading = new SensorReading();
        reading.setChillerUnitId(chillerUnitId);
        reading.setOrganizationId(organizationId);
        reading.setTimestamp(request.getTimestamp());
        reading.setInletTemp(request.getInletTemp());
        reading.setOutletTemp(request.getOutletTemp());
        reading.setAmbientTemp(request.getAmbientTemp());
        reading.setSuctionPressure(request.getSuctionPressure());
        reading.setDischargePressure(request.getDischargePressure());
        reading.setCondenserPressure(request.getCondenserPressure());
        reading.setPowerConsumption(request.getPowerConsumption());
        reading.setCompressorCurrent(request.getCompressorCurrent());
        reading.setVibrationX(request.getVibrationX());
        reading.setVibrationY(request.getVibrationY());
        reading.setVibrationZ(request.getVibrationZ());
        reading.setCoolantFlowRate(request.getCoolantFlowRate());
        reading.setOperationalMode(request.getOperationalMode());
        reading.setCompressorRunning(request.getCompressorRunning());
        reading.setRuntimeHours(request.getRuntimeHours());
        reading.setDataSource(request.getDataSource());
        reading.setBmsPointIds(request.getBmsPointIds());
        reading.setReceivedAt(Instant.now());
        return reading;
    }

    //validation result holder
    private static class ValidationResult {
        private final boolean valid;
        private final String errors;

        public ValidationResult(boolean valid, String errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrors() {
            return errors;
        }
    }
}
