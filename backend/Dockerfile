# Multi-stage Dockerfile for AgroVerse Spring Boot Backend
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy POM and download dependencies for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package executable fat JAR
COPY src src
RUN mvn clean package -DskipTests=true -B

# Stage 2: Production JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for container health check
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Run as non-root user for security hardening
RUN groupadd -r agroverse && useradd -r -g agroverse -m agroverse

# Copy packaged JAR from builder stage
COPY --from=builder /workspace/target/backend-1.0.0.jar app.jar

# Create uploads directory and set permissions
RUN mkdir -p /app/uploads && chown -R agroverse:agroverse /app

USER agroverse

# Container environment configuration
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
