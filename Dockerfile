# Multi-stage build for agentic workflow engine
FROM maven:3.9-eclipse-temurin-24-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Copy the built jar
COPY --from=build /app/target/agentic-workflow-engine-0.0.1-SNAPSHOT.jar /app/app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Change ownership of the app directory
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with preview features enabled
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/app.jar"]