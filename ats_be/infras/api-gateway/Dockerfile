# Build JAR trước khi build Docker image: mvn -DskipTests package
# Runtime image chỉ chứa JRE và JAR đã build.
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# JAR được tạo từ module api-gateway.
COPY target/api-gateway-1.0-SNAPSHOT.jar app.jar

# Không chạy API Gateway bằng root.
RUN useradd --uid 10001 --gid 0 --create-home appuser \
    && chown -R 10001:0 /app

USER 10001

# API Gateway là entry point chính của hệ thống.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
