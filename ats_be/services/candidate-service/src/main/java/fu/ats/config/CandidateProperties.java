package fu.ats.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "candidate")
public record CandidateProperties(MinioProperties minio) {

    public record MinioProperties(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {
    }
}
