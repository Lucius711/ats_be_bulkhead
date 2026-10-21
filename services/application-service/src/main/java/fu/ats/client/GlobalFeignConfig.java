package fu.ats.client;

import feign.codec.ErrorDecoder;
import fu.ats.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Bean;

public class GlobalFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        return (methodKey, response) -> {
            if (response.status() == 404) {
                return new ResourceNotFoundException("Downstream resource not found: " + methodKey);
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
