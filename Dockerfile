FROM gradle:8.0-jdk17 as builder

WORKDIR /app

# Kopiowanie plików projektu
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Budowanie aplikacji
RUN gradle build --no-daemon

FROM openjdk:17-slim

WORKDIR /app

# Kopiowanie zbudowanej aplikacji z etapu budowania
COPY --from=builder /app/build/libs/*.jar ./app.jar
COPY --from=builder /app/build/libs/dependencies ./libs

# Tworzenie katalogu dla danych
RUN mkdir -p /app/data

# Ustawianie zmiennych środowiskowych
ENV JAVA_OPTS=""

# Uruchamianie aplikacji
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp app.jar:libs/* com.emailprocessor.MainKt"]