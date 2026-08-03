FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradle ./gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY api ./api
COPY app ./app
COPY application ./application
COPY domain ./domain
COPY infrastructure ./infrastructure
COPY migrations ./migrations
RUN ./gradlew :app:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=8080
COPY --from=build /workspace/app/build/install/app/ ./
EXPOSE 8080
CMD ["./bin/app"]
