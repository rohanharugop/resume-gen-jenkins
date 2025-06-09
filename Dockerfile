# Multi-stage build for full-stack application
# Stage 1: Build React Frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app/frontend
COPY resume_frontend/package*.json ./
RUN npm install
COPY resume_frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app/backend
COPY resume-ai-builder/pom.xml .
RUN mvn dependency:go-offline -B
COPY resume-ai-builder/src src
# Copy built frontend into backend static resources
COPY --from=frontend-build /app/frontend/dist src/main/resources/static
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-Dserver.port=8080", "-jar", "app.jar"]