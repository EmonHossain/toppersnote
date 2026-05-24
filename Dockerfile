FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# Use the Jammy-based Temurin JRE and apply OS security updates to reduce known CVEs.
FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

# Install minimal packages (ca-certificates) and apply security updates, then clean apt caches.
RUN apt-get update \
	&& apt-get upgrade -y \
	&& apt-get install -y --no-install-recommends ca-certificates \
	&& rm -rf /var/lib/apt/lists/*

# Create a non-root user and ensure app directory permissions.
RUN addgroup --system sharenote \
	&& adduser --system --ingroup sharenote sharenote

COPY --from=build /workspace/target/sharenote-0.0.1-SNAPSHOT.jar /app/sharenote.jar

RUN mkdir -p /app/uploads/notes \
	&& chown -R sharenote:sharenote /app

USER sharenote

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/sharenote.jar"]
