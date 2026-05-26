package chillerguard.security;

import chillerguard.audit.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyAuthService apiKeyAuthService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final SecurityProperties securityProperties;

    public ApiKeyAuthFilter(ApiKeyAuthService apiKeyAuthService,
                            JwtService jwtService,
                            AuditService auditService,
                            SecurityProperties securityProperties) {
        this.apiKeyAuthService = apiKeyAuthService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!securityProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ApiPrincipal principal = resolvePrincipal(request);
            if (principal != null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.scopes().stream()
                                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                                .collect(Collectors.toSet())
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        } catch (SecurityException ex) {
            auditService.log("AUTH_FAILURE", "api_key", null,
                    request.getHeader(SecurityConstants.API_KEY_NAME_HEADER), "API_KEY",
                    request.getRemoteAddr(), ex.getMessage(), "{}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: " + ex.getMessage());
        }
    }

    private ApiPrincipal resolvePrincipal(HttpServletRequest request) {
        String authorization = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authorization.substring(SecurityConstants.BEARER_PREFIX.length());
            return jwtService.parseToken(token);
        }

        String apiKeyName = request.getHeader(SecurityConstants.API_KEY_NAME_HEADER);
        String apiKeySecret = request.getHeader(SecurityConstants.API_KEY_HEADER);
        if (apiKeyName != null && apiKeySecret != null) {
            ApiPrincipal principal = apiKeyAuthService.authenticate(apiKeyName, apiKeySecret);
            auditService.log("AUTH_SUCCESS", "api_key", principal.apiKeyId(), principal.name(),
                    "API_KEY", request.getRemoteAddr(), "API key authentication succeeded", "{}");
            return principal;
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/health")
                || path.startsWith("/actuator/")
                || path.startsWith("/api/v1/auth/");
    }
}
