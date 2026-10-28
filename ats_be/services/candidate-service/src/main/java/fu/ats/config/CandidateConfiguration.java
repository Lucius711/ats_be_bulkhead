package fu.ats.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
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

    /**
     * Tạo bucket chứa CV nếu chưa có. Trước đây không có code nào tạo bucket
     * nên mọi lần upload CV đều lỗi NoSuchBucket trên môi trường mới.
     */
    @Bean
    ApplicationRunner ensureCvBucket(MinioClient minioClient, CandidateProperties properties) {
        return args -> {
            String bucket = properties.minio().bucket();
            try {
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket {}", bucket);
                }
            } catch (Exception exception) {
                // MinIO chưa sẵn sàng thì không chặn service khởi động; upload sẽ báo lỗi rõ ràng.
                log.warn("Cannot ensure MinIO bucket {}: {}", bucket, exception.getMessage());
            }
        };
    }
}
