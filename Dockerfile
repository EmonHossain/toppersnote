FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN addgroup --system sharenote && adduser --system --ingroup sharenote sharenote

COPY --from=build /workspace/target/sharenote-0.0.1-SNAPSHOT.jar /app/sharenote.jar

RUN mkdir -p /app/uploads/notes && chown -R sharenote:sharenote /app

USER sharenote

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/sharenote.jar"]
