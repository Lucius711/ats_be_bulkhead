package fu.ats.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CandidateProperties.class)
public class CandidateConfiguration {

    @Bean
    MinioClient minioClient(CandidateProperties properties) {
        CandidateProperties.MinioProperties minio = properties.minio();

        return MinioClient.builder()
                .endpoint(minio.endpoint())
                .credentials(minio.accessKey(), minio.secretKey())
                .build();
    }
}
