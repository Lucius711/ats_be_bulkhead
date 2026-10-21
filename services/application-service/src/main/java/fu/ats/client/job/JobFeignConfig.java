package fu.ats.client.job;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class JobFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new JobErrorDecoder();
    }

    @Bean
    public Retryer retryer() {
        // period 200 ms, max interval 1 s, 3 attempts in total
        return new Retryer.Default(200, 1000, 3);
    }
}
