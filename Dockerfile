FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY backend/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system studenttest \
    && useradd --system --gid studenttest --home-dir /app --shell /usr/sbin/nologin studenttest \
    && mkdir -p /app/uploads/lecture-materials \
    && chown -R studenttest:studenttest /app

COPY --from=build /workspace/target/*.jar ./app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

USER studenttest

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:8080/api/v1/status >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
