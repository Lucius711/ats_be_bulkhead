package fu.ats.client.cvparse;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class CvParseFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CvParseErrorDecoder();
    }
}
