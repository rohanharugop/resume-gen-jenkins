# Multi-stage build for full-stack application
# Stage 1: Build React Frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app/frontend
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app/backend
COPY resume-ai-builder/pom.xml .
RUN mvn dependency:go-offline -B
COPY resume-ai-builder/src src
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true

# Stage 3: Runtime - Serve both frontend and backend
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy built backend jar
COPY --from=backend-build /app/backend/target/resume-ai-backend-0.0.1-SNAPSHOT.jar app.jar

# Copy built frontend (if you want to serve static files from Spring Boot)
COPY --from=frontend-build /app/frontend/dist /app/static

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/resume/health || exit 1

# Run the application
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:+UseG1GC", \
     "-XX:+UseStringDeduplication", \
     "-Dserver.port=${PORT:-8080}", \
     "-jar", "app.jar"]