# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:25-jre-jammy AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends ca-certificates curl \
	&& rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid "${APP_GID}" sharenote \
	&& useradd --system --uid "${APP_UID}" --gid sharenote --home-dir /app --shell /usr/sbin/nologin sharenote

COPY --from=build /workspace/target/sharenote-0.0.1-SNAPSHOT.jar /app/sharenote.jar

RUN mkdir -p /app/uploads/notes \
	&& chown -R sharenote:sharenote /app

USER sharenote

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
	CMD curl -fsS "http://localhost:${SERVER_PORT:-8080}/actuator/health/readiness" >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/sharenote.jar"]
