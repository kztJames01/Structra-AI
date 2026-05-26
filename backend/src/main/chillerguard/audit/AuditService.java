package chillerguard.audit;

import chillerguard.entity.AuditLog;
import chillerguard.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String eventType, String entityType, UUID entityId, String actorId, String actorType, String ipAddress, String description, String metadataJson) {
        AuditLog entry = new AuditLog();
        entry.setEventType(eventType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setActorId(actorId);
        entry.setActorType(actorType == null ? "SYSTEM" : actorType);
        entry.setIpAddress(ipAddress);
        entry.setDescription(description);
        entry.setMetadata(metadataJson == null ? "{}" : metadataJson);
        auditLogRepository.save(entry);
    }
}
