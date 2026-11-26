# ---------------------------------------------------------------------
# Imagen del backend para Render / Railway (multi-stage build)
#   1) Etapa "build": compila el .jar con Gradle y JDK 21
#   2) Etapa final: solo el JRE + el .jar (imagen más liviana y segura)
# Las variables (DB, JWT_SECRET, CORS...) se configuran en el panel del PaaS.
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
# Usuario sin privilegios
RUN useradd --system --uid 1001 motorsport
COPY --from=build /app/build/libs/*.jar app.jar
USER motorsport
EXPOSE 8080
# Render/Railway inyectan PORT; application.properties usa server.port=${PORT:8080}
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
