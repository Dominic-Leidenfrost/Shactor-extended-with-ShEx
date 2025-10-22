# Multi-stage Dockerfile for SHACTOR application

# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Prevent any interactive/browser prompts in CI environments
ENV CI=true
# Disable Vaadin Pro license validation for local research project
ENV VAADIN_OFFLINE_KEY=
ENV vaadin.productionMode=false

# Copy pom.xml and libs directory (for system-scoped dependencies)
COPY pom.xml .
COPY libs ./libs

# Copy source code
COPY src ./src
COPY frontend ./frontend
COPY package.json package-lock.json ./
COPY tsconfig.json types.d.ts ./
COPY vite.config.ts vite.config.js vite.config.js.map ./

# Build the application WITHOUT production profile (avoids Vaadin Charts license validation)
# For local research project, development mode is sufficient
# Run prepare-frontend to copy frontend resources into the JAR
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/demoshactor-1.0-SNAPSHOT.jar /app/shactor.jar

# Copy complete libs directory (includes resources, config, etc.)
# QSE library expects this structure for reading query files and configurations
COPY --from=build /app/libs /app/libs

# Copy frontend resources (needed in development mode)
COPY --from=build /app/frontend /app/frontend
COPY --from=build /app/target/classes /app/classes

# Create directory for datasets (optional, can be mounted)
RUN mkdir -p /app/datasets
RUN mkdir -p /app/evaluation
RUN mkdir -p /app/libs/Output

# Expose the application port
EXPOSE 8080

# Set default environment variables
ENV SPARQL_ENDPOINT_URL=http://graphdb:7200/
ENV SPARQL_REPOSITORY=LUBM-ScaleFactor-1
ENV REPO_LUBM_MINI=LUBM-ScaleFactor-1
ENV REPO_DBPEDIA=DBPEDIA_ML
ENV REPO_LUBM=LUBM
ENV REPO_YAGO=Yago_EngWiki
ENV POSTPROCESSING_ENABLED=true
ENV PORT=8080
# Set resources path for QSE library
ENV QSE_RESOURCES_PATH=/app/libs/resources/
ENV QSE_CONFIG_PATH=/app/libs/config/
ENV QSE_OUTPUT_PATH=/app/libs/Output/

# Run the application
ENTRYPOINT ["java", "-jar", "/app/shactor.jar"]
