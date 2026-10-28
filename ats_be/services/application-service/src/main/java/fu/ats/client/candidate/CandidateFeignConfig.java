package fu.ats.client.candidate;

import feign.codec.ErrorDecoder;
import feign.RequestInterceptor;
import fu.ats.client.GatewayIdentityRequestInterceptor;
import org.springframework.context.annotation.Bean;

public class CandidateFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CandidateErrorDecoder();
    }

    @Bean
    public RequestInterceptor gatewayIdentityRequestInterceptor() {
        return new GatewayIdentityRequestInterceptor();
    }
}
