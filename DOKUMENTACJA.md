# Email LLM Processor - Dokumentacja

## Spis treści

1. [Wprowadzenie](#wprowadzenie)
2. [Architektura systemu](#architektura-systemu)
3. [Instalacja i uruchomienie](#instalacja-i-uruchomienie)
4. [Konfiguracja](#konfiguracja)
5. [Główne komponenty](#główne-komponenty)
6. [Przepływ danych](#przepływ-danych)
7. [Baza danych](#baza-danych)
8. [Integracja z LLM](#integracja-z-llm)
9. [Testowanie](#testowanie)
10. [Rozszerzanie funkcjonalności](#rozszerzanie-funkcjonalności)
11. [Rozwiązywanie problemów](#rozwiązywanie-problemów)

## Wprowadzenie

Email LLM Processor to minimalistyczna aplikacja napisana w Kotlinie z użyciem Apache Camel, umożliwiająca automatyczne przetwarzanie, analizę i generowanie odpowiedzi na wiadomości e-mail przy użyciu modeli językowych (LLM).

Aplikacja została zaprojektowana w podejściu skryptowym, bez wykorzystania frameworków takich jak Spring Boot, co minimalizuje ilość kodu i zależności, jednocześnie zachowując wszystkie niezbędne funkcjonalności.

### Główne funkcjonalności

- Odbieranie wiadomości e-mail przez IMAP
- Wysyłanie wiadomości e-mail przez SMTP
- Analiza treści wiadomości przy użyciu modeli językowych (LLM)
- Automatyczne generowanie odpowiedzi
- Przechowywanie wiadomości w lokalnej bazie danych SQLite

## Architektura systemu

### Struktura projektu

Projekt jest zorganizowany w następujący sposób:

```
src/main/kotlin/com/emailprocessor/
├── Main.kt                     # Główny punkt wejścia aplikacji
├── model/                     # Modele danych
│   ├── EmailMessage.kt        # Model wiadomości email
│   └── ToneAnalysis.kt        # Model analizy tonu
├── routes/                    # Trasy Apache Camel
│   ├── EmailProcessingRoute.kt # Odbieranie i przetwarzanie emaili
│   └── EmailSendingRoute.kt   # Wysyłanie automatycznych odpowiedzi
├── services/                  # Serwisy biznesowe
│   ├── EmailService.kt        # Logika przetwarzania emaili
│   └── LlmService.kt          # Integracja z modelami językowymi
└── util/                     # Narzędzia pomocnicze
    └── EmailParser.kt         # Parser wiadomości email
```

### Diagram przepływu

```
+---------------+    +-------------------+    +-------------------+
|               |    |                   |    |                   |
| Serwer Email  |--->| EmailProcessing   |--->| LLM Service       |
| (IMAP)        |    | Route            |    | (Analiza tonu)    |
|               |    |                   |    |                   |
+---------------+    +-------------------+    +-------------------+
                                |                       |
                                v                       |
                     +-------------------+              |
                     |                   |              |
                     | Baza danych       |<-------------+
                     | SQLite            |
                     |                   |
                     +-------------------+
                                |
                                v
                     +-------------------+    +-------------------+
                     |                   |    |                   |
                     | Email Service     |--->| EmailSending      |
                     | (Decyzja o        |    | Route             |
                     |  odpowiedzi)      |    | (SMTP)            |
                     +-------------------+    +-------------------+
                                                       |
                                                       v
                                               +---------------+
                                               |               |
                                               | Serwer Email  |
                                               | (Odbiorca)    |
                                               |               |
                                               +---------------+
```

## Instalacja i uruchomienie

### Wymagania

- Java 17 lub nowsza
- Gradle 8.0 lub nowszy
- Docker i Docker Compose (do uruchomienia w kontenerach)

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

### Uruchomienie z użyciem skryptu

Można również użyć dołączonego skryptu do uruchomienia aplikacji:

```
./run.sh
```

## Konfiguracja

### Zmienne środowiskowe

Aplikacja korzysta z następujących zmiennych środowiskowych, które można ustawić w pliku `.env`:

| Zmienna | Opis | Wartość domyślna |
|---------|------|------------------|
| EMAIL_HOST | Host serwera email | mailhog |
| EMAIL_PORT | Port serwera email | 1025 |
| EMAIL_USER | Nazwa użytkownika email | test@example.com |
| EMAIL_PASSWORD | Hasło użytkownika email | password |
| LLM_API_URL | URL API modelu językowego | http://ollama:11434 |
| LLM_MODEL | Nazwa modelu językowego | llama2 |
| APP_PORT | Port aplikacji | 8080 |
| DATABASE_PATH | Ścieżka do bazy danych SQLite | /app/data/emails.db |

## Główne komponenty

### Main.kt

Główny punkt wejścia aplikacji, odpowiedzialny za inicjalizację wszystkich komponentów i uruchomienie Apache Camel. Zawiera konfigurację aplikacji, ładowanie zmiennych środowiskowych i inicjalizację bazy danych.

```kotlin
fun main() {
    // Inicjalizacja i uruchomienie aplikacji
    val app = EmailLlmProcessor()
    app.init()
    app.start()
}
```

### EmailProcessingRoute.kt

Trasa Camel odpowiedzialna za odbieranie i przetwarzanie wiadomości e-mail. Implementuje następujący przepływ:
- Odbieranie wiadomości przez IMAP
- Parsowanie wiadomości do modelu EmailMessage
- Zapisywanie wiadomości w bazie danych SQLite
- Przetwarzanie wiadomości przez EmailService
- Decydowanie o wysłaniu automatycznej odpowiedzi

### EmailSendingRoute.kt

Trasa Camel odpowiedzialna za wysyłanie automatycznych odpowiedzi. Formatuje odpowiedzi na podstawie analizy tonu i wysyła je przez SMTP.

### EmailService.kt

Serwis zawierający logikę biznesową przetwarzania wiadomości. Odpowiada za:
- Przetwarzanie wiadomości email
- Decydowanie o automatycznej odpowiedzi na podstawie analizy tonu
- Generowanie treści odpowiedzi

### LlmService.kt

Serwis do komunikacji z API modelu językowego. Odpowiada za:
- Tworzenie promptów dla modelu LLM
- Wysyłanie zapytań do API modelu (Ollama)
- Parsowanie odpowiedzi i tworzenie obiektów ToneAnalysis

### EmailParser.kt

Narzędzie do parsowania wiadomości e-mail. Obsługuje różne formaty wiadomości (plain text, HTML, multipart) i ekstrahuje załączniki.

## Przepływ danych

### Szczegółowy opis przepływu

1. **Odbieranie wiadomości**:
   - Komponent IMAP Apache Camel regularnie sprawdza skrzynkę pocztową
   - Nowe wiadomości są pobierane i przekazywane do trasy przetwarzania

2. **Parsowanie wiadomości**:
   - `EmailParser` ekstrahuje nadawcę, odbiorcę, temat i treść wiadomości
   - Wiadomość jest konwertowana do modelu `EmailMessage`

3. **Zapisywanie w bazie danych**:
   - Wiadomość jest zapisywana w tabeli `emails` w bazie SQLite
   - Status wiadomości jest ustawiany na `RECEIVED`

4. **Analiza tonu**:
   - `LlmService` wysyła treść wiadomości do API modelu językowego (Ollama)
   - Model analizuje sentyment, emocje, pilność i formalność wiadomości
   - Wyniki są parsowane do modelu `ToneAnalysis`

5. **Decyzja o odpowiedzi**:
   - `EmailService` na podstawie analizy tonu decyduje, czy wysłać automatyczną odpowiedź
   - Wiadomości pilne lub o negatywnym sentymencie otrzymują automatyczną odpowiedź

6. **Generowanie odpowiedzi**:
   - Jeśli zdecydowano o odpowiedzi, `EmailService` generuje odpowiednią treść
   - Treść jest dostosowana do tonu oryginalnej wiadomości

7. **Wysyłanie odpowiedzi**:
   - `EmailSendingRoute` formatuje wiadomość odpowiedzi
   - Odpowiedź jest wysyłana przez SMTP do oryginalnego nadawcy
   - Status wiadomości jest aktualizowany na `REPLIED`

## Baza danych

Aplikacja wykorzystuje prostą bazę danych SQLite do przechowywania wiadomości e-mail i wyników analizy.

### Schemat bazy danych

```sql
CREATE TABLE IF NOT EXISTS emails (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_address TEXT NOT NULL,
    to_address TEXT NOT NULL,
    subject TEXT,
    content TEXT,
    received_date TIMESTAMP,
    processed_date TIMESTAMP,
    tone_analysis TEXT,
    status TEXT
)
```

## Integracja z LLM

### Prompt dla modelu LLM

Aplikacja używa następującego promptu do analizy tonu wiadomości:

```
Przeanalizuj poniższą wiadomość email i podaj:
1. Ogólny sentyment (VERY_NEGATIVE, NEGATIVE, NEUTRAL, POSITIVE, VERY_POSITIVE)
2. Główne emocje (ANGER, FEAR, HAPPINESS, SADNESS, SURPRISE, DISGUST, NEUTRAL) z wartościami od 0 do 1
3. Pilność (LOW, NORMAL, HIGH, CRITICAL)
4. Formalność (VERY_INFORMAL, INFORMAL, NEUTRAL, FORMAL, VERY_FORMAL)
5. Główne tematy (lista słów kluczowych)
6. Krótkie podsumowanie treści

Odpowiedź podaj w formacie JSON.

Wiadomość:
[TREŚĆ WIADOMOŚCI]
```

### Format odpowiedzi

Oczekiwany format odpowiedzi od modelu LLM:

```json
{
    "sentiment": "NEUTRAL",
    "emotions": {
        "NEUTRAL": 0.8,
        "HAPPINESS": 0.2
    },
    "urgency": "NORMAL",
    "formality": "NEUTRAL",
    "topTopics": ["zapytanie", "informacja"],
    "summaryText": "Wiadomość zawiera ogólne zapytanie o informacje."
}
```

## Testowanie

### Testowanie z użyciem MailHog

Po uruchomieniu aplikacji z Docker Compose, można testować ją przy użyciu MailHog:

1. Otwórz interfejs MailHog pod adresem http://localhost:8025
2. Wyślij testową wiadomość e-mail na adres skonfigurowany w zmiennych środowiskowych (domyślnie test@example.com)
3. Aplikacja powinna odebrać wiadomość, przetworzyć ją i wysłać automatyczną odpowiedź (jeśli spełnione są kryteria)
4. Sprawdź w MailHog czy otrzymałeś odpowiedź

### Skrypt testowy

Można również użyć dołączonego skryptu testowego do symulacji wysyłania wiadomości o różnym tonie:

```
./test-email.sh [positive|negative|urgent|neutral]
```

Gdzie:
- `positive` - Wysyła wiadomość o pozytywnym tonie
- `negative` - Wysyła wiadomość o negatywnym tonie
- `urgent` - Wysyła pilną wiadomość
- `neutral` - Wysyła neutralne zapytanie

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

### Dodawanie nowych funkcjonalności

Aby dodać nowe funkcjonalności:
1. Rozszerz istniejące serwisy lub dodaj nowe w katalogu `services/`
2. Dodaj nowe modele danych w katalogu `model/` jeśli są potrzebne
3. Zaktualizuj trasy Camel, aby wykorzystywały nowe funkcjonalności

## Rozwiązywanie problemów

### Problemy z połączeniem do serwera email

Jeśli aplikacja nie może połączyć się z serwerem email:
1. Sprawdź, czy serwer email jest uruchomiony i dostępny
2. Sprawdź, czy dane dostępowe (host, port, użytkownik, hasło) są poprawne
3. Sprawdź, czy firewall nie blokuje połączenia

### Problemy z modelem LLM

Jeśli analiza LLM nie działa poprawnie:
1. Sprawdź, czy serwer Ollama jest uruchomiony i dostępny
2. Sprawdź, czy model językowy jest poprawnie załadowany
3. Sprawdź logi serwera Ollama w poszukiwaniu błędów

### Problemy z bazą danych

Jeśli występują problemy z bazą danych:
1. Sprawdź, czy ścieżka do bazy danych jest poprawna
2. Sprawdź, czy aplikacja ma uprawnienia do zapisu w katalogu bazy danych
3. Sprawdź, czy schemat bazy danych jest poprawny

### Debugowanie

Aby debugować aplikację:
1. Uruchom aplikację z większym poziomem logowania:
   ```
   ./gradlew run --info
   ```
2. Sprawdź logi w poszukiwaniu błędów
3. Użyj narzędzia Adminer do sprawdzenia zawartości bazy danych
