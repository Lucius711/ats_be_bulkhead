package fu.ats.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {
    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("Request {} {} {}", request.getMethod(), request.getURI(), request.getHeaders().get("X-Web-Client"));

        String correlationId = request.getHeaders().getFirst(CORRELATION_ID);

        log.info("Correlation Id {}", correlationId);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
