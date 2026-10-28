package fu.ats.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SecurityFilter implements GlobalFilter, Ordered {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_EMAIL = "X-User-Email";
    private static final String USER_NAME = "X-User-Name";
    private static final String USER_ROLES = "X-User-Roles";


//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // Always strip identity headers first so clients can never forge them
//        ServerWebExchange sanitized = exchange.mutate()
//                .request(r -> r.headers(h -> {
//                    h.remove(USER_ID);
//                    h.remove(USER_EMAIL);
//                }))
//                .build();
//
//        return ReactiveSecurityContextHolder.getContext()
//                .map(ctx -> ctx.getAuthentication())
//                .filter(auth -> auth instanceof JwtAuthenticationToken)
//                .map(auth -> ((JwtAuthenticationToken) auth).getToken())
//                .map(jwt -> {
//                    ServerHttpRequest request = sanitized.getRequest().mutate()
//                            .headers(headers -> {
//                                // Downstream services do not need the raw token
//                                headers.remove(HttpHeaders.AUTHORIZATION);
//                                log.info(headers.toString());
//                                log.info("Email {}", jwt.getSubject());
//                                headers.set(USER_ID, jwt.getSubject());
//                                String email = jwt.getClaimAsString("email");
//                                if (email != null) {
//                                    headers.set(USER_EMAIL, email);
//                                }
//                            })
//                            .build();
//                    return sanitized.mutate().request(request).build();
//                })
//                .defaultIfEmpty(sanitized)
//                .flatMap(chain::filter);
//    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(USER_ID);
                    headers.remove(USER_EMAIL);
                    headers.remove(USER_NAME);
                    headers.remove(USER_ROLES);
                }))
                .build();

        if (HttpMethod.OPTIONS.equals(method)
                || path.equals("/actuator/health")) {
            return chain.filter(sanitizedExchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    Jwt jwt = authentication.getToken();

                    ServerHttpRequest request = sanitizedExchange.getRequest()
                            .mutate()
                            .headers(headers -> {
                                // Gateway đã xác thực token
                                headers.remove(HttpHeaders.AUTHORIZATION);

                                // 1. User ID
                                // Một số token hiện tại không có claim "sub";
                                // dùng preferred_username làm định danh dự phòng.
                                String userId = jwt.getSubject();
                                if (userId == null || userId.isBlank()) {
                                    userId = jwt.getClaimAsString(
                                            "preferred_username"
                                    );
                                }

                                if (userId != null && !userId.isBlank()) {
                                    headers.set(USER_ID, userId);
                                }

                                // 2. Email
                                String email =
                                        jwt.getClaimAsString("email");

                                if (email != null) {
                                    headers.set(USER_EMAIL, email);
                                }

                                // 3. Username
                                String username =
                                        jwt.getClaimAsString(
                                                "preferred_username"
                                        );

                                if (username != null) {
                                    headers.set(USER_NAME, username);
                                }

                                // 4. Keycloak realm roles and client roles
                                String roleValue = extractKeycloakRoles(jwt)
                                        .stream()
                                        .collect(Collectors.joining(","));

                                if (!roleValue.isBlank()) {
                                    log.info("Roles {}", roleValue);
                                    headers.set(USER_ROLES, roleValue);
                                }
                            })
                            .build();

                    ServerWebExchange newExchange =
                            sanitizedExchange.mutate()
                                    .request(request)
                                    .build();

                    /*
                     * thenReturn rất quan trọng.
                     * Nếu không có dòng này, switchIfEmpty có thể bị
                     * kích hoạt sau khi chain.filter hoàn tất.
                     */
                    return chain.filter(newExchange)
                            .thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Không tìm thấy JwtAuthenticationToken");

                    sanitizedExchange.getResponse()
                            .setStatusCode(HttpStatus.UNAUTHORIZED);

                    return sanitizedExchange.getResponse()
                            .setComplete()
                            .thenReturn(Boolean.FALSE);
                }))
                .then();
    }

    @Override
    public int getOrder() {
        return 200;
    }

    private Set<String> extractKeycloakRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        addRoles(jwt.getClaim("realm_access"), roles);

        Object resourceAccessClaim = jwt.getClaim("resource_access");
        if (resourceAccessClaim instanceof Map<?, ?> resourceAccess) {
            resourceAccess.values().forEach(clientAccess -> addRoles(clientAccess, roles));
        }

        return roles;
    }

    private void addRoles(Object accessClaim, Set<String> roles) {
        if (!(accessClaim instanceof Map<?, ?> access)) {
            return;
        }

        Object rawRoles = access.get("roles");
        if (rawRoles instanceof Collection<?> roleValues) {
            roleValues.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !role.isBlank())
                    .forEach(roles::add);
        }
    }

}
