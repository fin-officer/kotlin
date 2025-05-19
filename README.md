# Email LLM Processor - Dokumentacja

## Przegląd

Email LLM Processor to minimalistyczna aplikacja napisana w Kotlinie z użyciem Apache Camel, umożliwiająca automatyczne przetwarzanie, analizę i generowanie odpowiedzi na wiadomości e-mail przy użyciu modeli językowych (LLM).

Aplikacja została zaprojektowana w podejściu skryptowym, bez wykorzystania frameworków takich jak Spring Boot, co minimalizuje ilość kodu i zależności.

## Funkcjonalności

- Odbieranie wiadomości e-mail przez IMAP
- Wysyłanie wiadomości e-mail przez SMTP
- Analiza treści wiadomości przy użyciu modeli językowych (LLM)
- Automatyczne generowanie odpowiedzi
- Przechowywanie wiadomości w lokalnej bazie danych SQLite

## Architektura

### Główne komponenty

1. **Main.kt** - punkt wejścia aplikacji, odpowiedzialny za inicjalizację wszystkich komponentów i uruchomienie Apache Camel
2. **EmailProcessingRoute.kt** - trasa Camel odpowiedzialna za odbieranie i przetwarzanie wiadomości e-mail
3. **EmailSendingRoute.kt** - trasa Camel odpowiedzialna za wysyłanie automatycznych odpowiedzi
4. **EmailService.kt** - serwis zawierający logikę biznesową przetwarzania wiadomości
5. **LlmService.kt** - serwis do komunikacji z API modelu językowego
6. **EmailParser.kt** - narzędzie do parsowania wiadomości e-mail

### Model danych

1. **EmailMessage.kt** - model reprezentujący wiadomość e-mail
2. **ToneAnalysis.kt** - model reprezentujący analizę tonu wiadomości

### Baza danych

Aplikacja wykorzystuje prostą bazę danych SQLite do przechowywania wiadomości e-mail i wyników analizy.

## Wymagania

- Java 17 lub nowsza
- Gradle 8.0 lub nowszy
- Docker i Docker Compose (do uruchomienia w kontenerach)

## Uruchomienie

### Przygotowanie środowiska

1. Sklonuj repozytorium:
   ```
   git clone https://github.com/yourusername/email-llm-processor.git
   cd email-llm-processor
   ```

2. Skopiuj plik przykładowy .env.example do .env:
   ```
   cp .env.example .env
   ```

3. Dostosuj zmienne w pliku .env według potrzeb.

### Uruchomienie z Docker Compose

```
docker-compose up -d
```

To polecenie uruchomi:
- Aplikację Email LLM Processor
- Serwer testowy MailHog do odbioru/wysyłania wiadomości
- Serwer Ollama do hostowania modelu LLM
- Adminer do zarządzania bazą danych SQLite

### Uruchomienie lokalne bez Dockera

1. Zbuduj projekt:
   ```
   ./gradlew build
   ```

2. Uruchom aplikację:
   ```
   ./gradlew run
   ```

## Dostęp do usług

Po uruchomieniu docker-compose, następujące usługi będą dostępne:

| Usługa | URL | Opis |
|--------|-----|------|
| MailHog | http://localhost:8025 | Interfejs testowy do przeglądania wiadomości |
| Adminer | http://localhost:8081 | Panel zarządzania bazą danych |

## Testowanie

Aby przetestować aplikację:

1. Otwórz interfejs MailHog pod adresem http://localhost:8025
2. Wyślij testową wiadomość e-mail na adres skonfigurowany w zmiennych środowiskowych (domyślnie test@example.com)
3. Aplikacja powinna odebrać wiadomość, przetworzyć ją i wysłać automatyczną odpowiedź (jeśli spełnione są kryteria)
4. Sprawdź w MailHog czy otrzymałeś odpowiedź
5. Możesz również sprawdzić bazę danych przez Adminer, aby zobaczyć zapisane wiadomości i wyniki analizy

## Rozszerzanie funkcjonalności

### Dodawanie nowych tras Camel

Aby dodać nową trasę Camel:
1. Utwórz nową klasę implementującą RouteBuilder w katalogu `src/main/kotlin/com/emailprocessor/routes/`
2. Zaimplementuj metodę `configure()`
3. Zarejestruj trasę w `Main.kt` dodając ją do `camelMain.configure().addRoutesBuilder()`

### Dostosowanie analizy LLM

Aby dostosować analizę LLM:
1. Zmodyfikuj prompt w metodzie `createAnalysisPrompt()` w klasie `LlmService.kt`
2. Dostosuj parsowanie odpowiedzi w metodzie `parseAnalysisResponse()`
3. Rozszerz model `ToneAnalysis.kt` o dodatkowe pola, jeśli są potrzebne

## Ograniczenia

Ta minimalistyczna wersja ma pewne ograniczenia:
- Brak interfejsu użytkownika/API REST
- Brak zaawansowanego zarządzania błędami
- Uproszczona obsługa załączników
- Brak systemu uwierzytelniania i autoryzacji
- Zaślepki w komunikacji z LLM (w pełnej implementacji należałoby zintegrować się z rzeczywistym API)

## Informacje dodatkowe

### Dlaczego Apache Camel?

Apache Camel został wybrany jako główny komponent integracyjny ze względu na:
- Prostotę w definiowaniu tras przetwarzania
- Gotowe komponenty do obsługi poczty (IMAP, SMTP)
- Elastyczność w konfiguracji
- Niski narzut w porównaniu do pełnych frameworków

### Dlaczego SQLite?

SQLite zostało wybrane jako baza danych ze względu na:
- Brak potrzeby instalacji osobnego serwera
- Przechowywanie danych w jednym pliku
- Prostota konfiguracji i użycia
- Wystarczająca wydajność dla małych i średnich obciążeń