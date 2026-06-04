# syntax=docker/dockerfile:1
# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Cache dependencies first for faster rebuilds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# curl is only needed for the compose healthcheck; keep the layer tiny
RUN apk add --no-cache curl \
 && addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
USER app
# Memory optimization (Zadatak 2): honour the container cgroup limit, cap heap %,
# use the low-overhead SerialGC, bound metaspace and thread stacks, fail fast on OOM.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
