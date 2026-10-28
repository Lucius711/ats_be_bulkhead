# Build JAR trước khi build Docker image:
# mvn -DskipTests package
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

COPY target/cv-parse-service-1.0-SNAPSHOT.jar app.jar

RUN useradd --uid 10001 --gid 0 --create-home appuser \
    && chown -R 10001:0 /app

USER 10001

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
