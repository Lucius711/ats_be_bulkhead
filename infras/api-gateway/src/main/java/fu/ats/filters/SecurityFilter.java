package fu.ats.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SecurityFilter implements GlobalFilter, Ordered {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_EMAIL = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Always strip identity headers first so clients can never forge them
        ServerWebExchange sanitized = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove(USER_ID);
                    h.remove(USER_EMAIL);
                }))
                .build();

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken())
                .map(jwt -> {
                    ServerHttpRequest request = sanitized.getRequest().mutate()
                            .headers(headers -> {
                                // Downstream services do not need the raw token
                                headers.remove(HttpHeaders.AUTHORIZATION);
                                headers.set(USER_ID, jwt.getSubject());
                                String email = jwt.getClaimAsString("email");
                                if (email != null) {
                                    headers.set(USER_EMAIL, email);
                                }
                            })
                            .build();
                    return sanitized.mutate().request(request).build();
                })
                .defaultIfEmpty(sanitized)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return 200;
    }
}