package chillerguard.security;

import chillerguard.entity.ApiKey;
import chillerguard.repository.ApiKeyRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApiKeyAuthService {

    private final ApiKeyRepository apiKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ApiKeyAuthService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public ApiPrincipal authenticate(String apiKeyName, String rawSecret) {
        ApiKey key = apiKeyRepository.findByNameAndIsActiveTrue(apiKeyName)
                .orElseThrow(() -> new SecurityException("API key not found or inactive"));

        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            throw new SecurityException("API key expired");
        }

        if (!passwordEncoder.matches(rawSecret, key.getKeyHash())) {
            throw new SecurityException("Invalid API key secret");
        }

        apiKeyRepository.updateLastUsedAt(key.getId(), Instant.now());

        Set<String> scopes = parseScopes(key.getScopes());
        UUID orgId = key.getOrganizationId();
        return new ApiPrincipal(key.getId(), key.getName(), orgId, scopes);
    }

    private Set<String> parseScopes(String scopesJson) {
        if (scopesJson == null || scopesJson.isBlank()) {
            return Set.of("read");
        }
        String normalized = scopesJson
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        Set<String> scopes = java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        return scopes.isEmpty() ? Set.of("read") : scopes;
    }
}
