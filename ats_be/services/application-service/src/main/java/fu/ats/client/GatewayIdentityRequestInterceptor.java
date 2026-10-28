package fu.ats.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Forwards the identity headers added by API Gateway to downstream services.
 */
public class GatewayIdentityRequestInterceptor implements RequestInterceptor {

    private static final List<String> IDENTITY_HEADERS = List.of(
            "X-User-Id",
            "X-User-Email",
            "X-User-Name",
            "X-User-Roles"
    );

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }

        IDENTITY_HEADERS.forEach(header -> {
            String value = servletAttributes.getRequest().getHeader(header);
            if (value != null && !value.isBlank()) {
                template.header(header, value);
            }
        });
    }
}
