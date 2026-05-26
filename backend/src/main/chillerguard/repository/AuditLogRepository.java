package chillerguard.repository;

import chillerguard.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

//audit log repo for compliance queries
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEventType(String eventType, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByActorId(String actorId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType " +
           "AND a.eventType = :eventType AND a.createdAt >= :since")
    Page<AuditLog> findByEntityTypeAndEventSince(
            @Param("entityType") String entityType,
            @Param("eventType") String eventType,
            @Param("since") Instant since,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actorId = :actorId " +
           "AND a.eventType = 'AUTH_FAILURE' AND a.createdAt >= :since")
    long countAuthFailuresByActorSince(@Param("actorId") String actorId, @Param("since") Instant since);
}
