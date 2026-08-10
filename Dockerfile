FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY config ./config
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests package \
    && cp target/internal-outsourcing-management-0.0.1-SNAPSHOT.jar app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV JAVA_OPTS=""
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /workspace/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
