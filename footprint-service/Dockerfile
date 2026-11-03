# syntax=docker/dockerfile:1
# ------------------------------------------------------------------------------
# Build Stage: Compile and package Spring Boot JAR
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper / build descriptor first for layer caching
COPY pom.xml .

# Download dependencies
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B || true

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Runtime Stage: Minimal Alpine JRE container with non-root security context
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/footprint-service-*.jar /app/app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose HTTP port
EXPOSE 8080

# Configure JVM memory limits for container environments
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="docker"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q 'UP' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
