package chillerguard.security;

import java.util.Set;
import java.util.UUID;

//auth context from api key or jwt
public record ApiPrincipal(
        UUID apiKeyId,
        String name,
        UUID organizationId,
        Set<String> scopes
) {
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
}
