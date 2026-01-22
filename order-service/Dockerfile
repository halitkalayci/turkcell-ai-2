# Multi-stage Dockerfile for order-service
# This approach separates build and runtime stages
# Pros: Smaller final image, better security (no build tools in production)
# Cons: Slightly more complex

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml
COPY pom.xml ./

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Extract layers for better caching
RUN mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/order-service-1.0.0.jar extract --destination target/extracted

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy extracted layers from builder stage
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

# Expose application port
EXPOSE 8081

# Set environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8081
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application using Spring Boot's layered approach
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
