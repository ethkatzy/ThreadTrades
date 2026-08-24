# Production image: builds a runnable jar, then runs it on a slim JRE.
# Dockerfile.dev (mvnw spring-boot:run against a bind-mounted source tree) is
# for local Docker Compose only -- this is what Render deploys.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render assigns the listen port via $PORT at runtime; SERVER_PORT already
# drives server.port in application.yml (see ROADMAP.md #2).
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-${SERVER_PORT}}"]
