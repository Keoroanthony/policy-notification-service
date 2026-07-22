# =============================================================================
# Stage 1: builder
# =============================================================================
# Same base image family as the rule engine for consistency. JDK required
# here because we need the Kotlin compiler (invoked via Maven) to compile
# .kt source files. The JRE in the runtime stage does not include it.
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# ── Dependency caching layer ──────────────────────────────────────────────────
# Copy the Maven wrapper files and pom.xml BEFORE copying src/.
# Docker will cache the `dependency:go-offline` layer as long as pom.xml
# does not change. A code-only edit will skip straight to the `package` step.
#
# .mvn/ contains maven-wrapper.properties which tells the wrapper which
# Maven version to download. It must be present before running ./mvnw.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# The Maven wrapper downloads Maven as a .zip archive and extracts it with
# 'unzip'. eclipse-temurin:21-jdk-jammy is a minimal Ubuntu image that does
# not include unzip by default — install it before the wrapper runs.
# Cleaning apt lists immediately keeps this layer small.
RUN apt-get update -qq \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/* \
    && chmod +x mvnw

# Download all declared dependencies into the local Maven repository.
# -B: batch mode — disables interactive prompts and the progress spinner,
#     which is important for clean Docker build logs.
# -q: quiet — suppresses informational output, shows only warnings/errors.
# dependency:go-offline populates ~/.m2/repository so that the subsequent
# `package` step can run without network access.
RUN ./mvnw dependency:go-offline -B -q

# ── Source compile ────────────────────────────────────────────────────────────
COPY src/ src/

# package: compiles Kotlin sources, runs tests (skipped here), and produces
# the Spring Boot fat JAR via the spring-boot-maven-plugin.
# -DskipTests: we are building an image, not running a test suite. Tests
#              should be run in a separate CI step before the image build.
RUN ./mvnw package -DskipTests -B -q

# =============================================================================
# Stage 2: runtime
# =============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Create a non-root system user. Same rationale as the rule engine Dockerfile:
# principle of least privilege, reducing blast radius of a container escape.
# curl is installed for the Docker Compose health check.
RUN groupadd --system appgroup \
    && useradd --system --gid appgroup appuser \
    && apt-get update -qq \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy only the fat JAR. The glob *.jar matches
# policy-notification-service-0.0.1-SNAPSHOT.jar regardless of version string.
COPY --from=builder /app/target/*.jar app.jar

USER appuser

# This service listens on 8081 (configured in application.properties).
EXPOSE 8081

# -XX:MaxRAMPercentage=75.0: makes the JVM heap proportional to the container
# memory limit rather than host RAM. See rule engine Dockerfile for full rationale.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
