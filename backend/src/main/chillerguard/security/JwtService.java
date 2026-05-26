package chillerguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final SecurityProperties securityProperties;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String issueToken(ApiPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwtTtlSeconds());

        return Jwts.builder()
                .subject(principal.name())
                .claim("apiKeyId", principal.apiKeyId().toString())
            .claim("organizationId", principal.organizationId() != null ? principal.organizationId().toString() : null)
                .claim("scopes", principal.scopes())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    public ApiPrincipal parseToken(String jwt) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        String name = claims.getSubject();
        UUID apiKeyId = UUID.fromString(claims.get("apiKeyId", String.class));
        String orgClaim = claims.get("organizationId", String.class);
        UUID orgId = orgClaim == null || orgClaim.isBlank() ? null : UUID.fromString(orgClaim);
        @SuppressWarnings("unchecked")
        Set<String> scopes = Set.copyOf((java.util.List<String>) claims.get("scopes", java.util.List.class));

        return new ApiPrincipal(apiKeyId, name, orgId, scopes);
    }

    private SecretKey signingKey() {
        byte[] keyBytes = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
