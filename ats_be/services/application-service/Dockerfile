# Build JAR trước khi build Docker image: mvn -DskipTests package
# Dockerfile này chỉ đóng gói JAR đã build.
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# JAR được tạo từ module application-service.
COPY target/application-service-1.0-SNAPSHOT.jar app.jar

# Chạy non-root với UID cố định.
RUN useradd --uid 10001 --gid 0 --create-home appuser \
    && chown -R 10001:0 /app

USER 10001

# Port nội bộ của Application Service.
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
