package chillerguard.repository;

import chillerguard.entity.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

//sensor reading repo
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    //readings for unit newest first
    List<SensorReading> findByChillerUnitIdOrderByTimestampDesc(UUID chillerUnitId);

    //readings for unit with paging
    Page<SensorReading> findByChillerUnitIdOrderByTimestampDesc(UUID chillerUnitId, Pageable pageable);

    //readings in time range
    List<SensorReading> findByChillerUnitIdAndTimestampBetweenOrderByTimestampAsc(
            UUID chillerUnitId, Instant startTime, Instant endTime);

    //readings in time range with paging
    Page<SensorReading> findByChillerUnitIdAndTimestampBetweenOrderByTimestampAsc(
            UUID chillerUnitId, Instant startTime, Instant endTime, Pageable pageable);

    //latest reading for unit
    @Query("SELECT sr FROM SensorReading sr WHERE sr.chillerUnitId = :chillerUnitId ORDER BY sr.timestamp DESC LIMIT 1")
    SensorReading findLatestByChillerUnitId(@Param("chillerUnitId") UUID chillerUnitId);

    //count by unit
    long countByChillerUnitId(UUID chillerUnitId);

    //count in time range
    long countByChillerUnitIdAndTimestampBetween(UUID chillerUnitId, Instant startTime, Instant endTime);

    //readings by data source
    List<SensorReading> findByDataSourceOrderByTimestampDesc(String dataSource);

    //invalid readings
    List<SensorReading> findByIsValidFalse();

    //invalid readings for unit
    List<SensorReading> findByChillerUnitIdAndIsValidFalse(UUID chillerUnitId);

    //avg power in range
    @Query("SELECT AVG(sr.powerConsumption) FROM SensorReading sr WHERE sr.chillerUnitId = :chillerUnitId AND sr.timestamp BETWEEN :startTime AND :endTime")
    java.math.BigDecimal getAveragePowerConsumption(@Param("chillerUnitId") UUID chillerUnitId,
                                                       @Param("startTime") Instant startTime,
                                                       @Param("endTime") Instant endTime);

    //min/max temps in range
    @Query("SELECT MIN(sr.inletTemp), MAX(sr.inletTemp), MIN(sr.outletTemp), MAX(sr.outletTemp) " +
           "FROM SensorReading sr WHERE sr.chillerUnitId = :chillerUnitId AND sr.timestamp BETWEEN :startTime AND :endTime")
    List<Object[]> getTemperatureStats(@Param("chillerUnitId") UUID chillerUnitId,
                                        @Param("startTime") Instant startTime,
                                        @Param("endTime") Instant endTime);

    //delete old readings
    void deleteByTimestampBefore(Instant cutoffTime);
}
