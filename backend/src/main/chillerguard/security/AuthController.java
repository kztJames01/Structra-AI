package chillerguard.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ApiKeyAuthService apiKeyAuthService;
    private final JwtService jwtService;

    public AuthController(ApiKeyAuthService apiKeyAuthService, JwtService jwtService) {
        this.apiKeyAuthService = apiKeyAuthService;
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> issueToken(
            @RequestHeader(SecurityConstants.API_KEY_NAME_HEADER) String apiKeyName,
            @RequestHeader(SecurityConstants.API_KEY_HEADER) String apiKeySecret
    ) {
        ApiPrincipal principal = apiKeyAuthService.authenticate(apiKeyName, apiKeySecret);
        String token = jwtService.issueToken(principal);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "token_type", "Bearer",
                "expires_in", 3600,
                "api_key", principal.name()
        ));
    }
}
