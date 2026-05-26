package chillerguard.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

//log controller calls for audit
@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditService auditService;
    private final HttpServletRequest request;

    public AuditLoggingAspect(AuditService auditService, HttpServletRequest request) {
        this.auditService = auditService;
        this.request = request;
    }

    @Around("within(chillerguard.controller..*)")
    public Object auditControllerCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String actor = request.getHeader("X-API-Key-Name");
        String ip = request.getRemoteAddr();
        String endpoint = request.getMethod() + " " + request.getRequestURI();

        try {
            Object result = joinPoint.proceed();
            auditService.log("READ", "api", null, actor, "API_KEY", ip,
                    "Controller call success: " + endpoint,
                    "{\"method\":\"" + joinPoint.getSignature().toShortString() + "\"}");
            return result;
        } catch (Throwable t) {
            auditService.log("ERROR", "api", null, actor, "API_KEY", ip,
                    "Controller call failure: " + endpoint,
                    "{\"error\":\"" + sanitize(t.getMessage()) + "\"}");
            throw t;
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'");
    }
}
