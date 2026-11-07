# Lifting Club Fitness — container image for Vercel Container Runtime.
# The image listens on $PORT (Vercel injects it; defaults to 8080 locally).

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/gimnasio-web-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

# Vercel sends SIGTERM and gives 30s to exit when the instance scales down.
ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar --server.port=${PORT}"]
