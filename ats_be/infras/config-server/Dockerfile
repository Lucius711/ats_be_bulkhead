# Build JAR trước khi build Docker image: mvn -DskipTests package
# Chỉ dùng JRE ở runtime, không đưa Maven/source code vào image.
FROM eclipse-temurin:21-jre

# Thư mục làm việc bên trong container.
WORKDIR /app

# Giới hạn heap theo memory container.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# JAR phải được tạo ở thư mục target trước bước docker compose build.
COPY target/config-server-1.0-SNAPSHOT.jar app.jar

# Không chạy ứng dụng bằng root.
RUN useradd --uid 10001 --gid 0 --create-home appuser \
    && chown -R 10001:0 /app

USER 10001

# Port nội bộ của Config Server; Compose sẽ map ra host.
EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
