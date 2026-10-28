# Build JAR trước khi build Docker image: mvn -DskipTests package
# JRE runtime không chứa Maven/source code.
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# JAR được tạo từ module eureka-server.
COPY target/eureka-server-1.0-SNAPSHOT.jar app.jar

# Chạy non-root để giảm quyền khi container bị khai thác.
RUN useradd --uid 10001 --gid 0 --create-home appuser \
    && chown -R 10001:0 /app

USER 10001

# Port nội bộ của Eureka Server.
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
