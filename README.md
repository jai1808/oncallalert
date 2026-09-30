# OnCall Alert System

Intelligent on-call alerting microservice. When triggered by xMatters (or any webhook), it:

1. Queries **OpenSearch** for recent `Error during Document Generation` logs  
2. Aggregates and deduplicates errors  
3. Cross-references a **known-errors** repository  
4. Asks **DeepSeek LLM** for remediation suggestions  
5. Emails a consolidated HTML report to the on-call engineer  

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Access to OpenSearch, DeepSeek API, and SMTP

---

## Quick Start

### 1. Fill in environment variables

Copy the example env file (or edit the existing `.env`):

```bash
cp .env.example .env
```

Open `.env` and replace every placeholder with your real values, for example:

```env
DEEPSEEK_API_KEY=<Enter your LLM key here>
OPENSEARCH_URL=<Enter your OpenSearch URL here>
SMTP_PASSWORD=<Enter your SMTP password here>
```

### 2. Required keys in `.env`

| Variable | Description | Example placeholder |
|---|---|---|
| `OPENSEARCH_URL` | OpenSearch cluster URL | `<Enter your OpenSearch URL here>` |
| `OPENSEARCH_USERNAME` | OpenSearch username | `<Enter your OpenSearch username here>` |
| `OPENSEARCH_PASSWORD` | OpenSearch password | `<Enter your OpenSearch password here>` |
| `OPENSEARCH_INDEX_NAME` | Log index to query | `<Enter your OpenSearch index name here>` |
| `DEEPSEEK_API_KEY` | DeepSeek LLM API key | `<Enter your LLM key here>` |
| `SMTP_HOST` | SMTP hostname | `<Enter your SMTP host here>` |
| `SMTP_PORT` | SMTP port | `587` |
| `SMTP_USERNAME` | SMTP username | `<Enter your SMTP username here>` |
| `SMTP_PASSWORD` | SMTP password | `<Enter your SMTP password here>` |
| `ONCALL_DEFAULT_EMAIL` | Fallback on-call email | `<Enter your on-call email here>` |
| `ALERT_FROM_EMAIL` | Sender email address | `<Enter your from email here>` |

Optional:

| Variable | Default |
|---|---|
| `ALERT_LOG_WINDOW_MINUTES` | `15` |
| `ALERT_COOLDOWN_SECONDS` | `300` |
| `DEEPSEEK_MODEL` | `deepseek-chat` |
| `DEEPSEEK_MAX_TOKENS` | `2048` |
| `DEEPSEEK_TEMPERATURE` | `0.3` |
| `SERVER_PORT` | `8080` |

> Spring Boot reads these as **OS environment variables**. On Windows PowerShell you can load `.env` into the session, or export them manually before starting the app. Docker Compose loads `.env` automatically.

### 3. Run locally

```bash
# Load env vars (PowerShell example — set each key from .env)
# Then:
mvn spring-boot:run
```

Or with Docker:

```bash
mvn clean package -DskipTests
docker compose up --build
```

### 4. Trigger an alert

```bash
curl -X POST http://localhost:8080/api/v1/alerts/trigger \
  -H "Content-Type: application/json" \
  -d "{
    \"oncallEmail\": \"john.doe@company.com\",
    \"source\": \"xmatters\",
    \"incidentId\": \"INC-12345\",
    \"message\": \"Document generation errors detected\"
  }"
```

Example response:

```json
{
  "status": "SENT",
  "message": "Alert report emailed to john.doe@company.com",
  "errorsFound": 47,
  "reportId": "A3F2B1C8"
}
```

Health check:

```bash
curl http://localhost:8080/api/v1/alerts/health
```

---

## Project Structure

```
src/main/java/com/oncall/alert/
├── OnCallAlertApplication.java
├── config/          # OpenSearch, DeepSeek, AppProperties
├── controller/      # POST /api/v1/alerts/trigger
├── service/         # Orchestration, OpenSearch, DeepSeek, KnownError, Email
├── model/           # DTOs
└── exception/       # Global error handling

src/main/resources/
├── application.yml
├── known-errors.json
└── templates/alert-email.html
```

---

## Known Errors

Edit `src/main/resources/known-errors.json` to flag ignorable / warning errors. Matching is by `errorCode` (and optional `documentTypes`) or by `errorMessagePattern` substring.

---

## Tests

```bash
mvn test
```

---

## Notes

- OpenSearch auth is **Basic Auth**. Adjust `OpenSearchConfig` if you use IAM Sig-v4 or API keys.
- TLS currently trusts all certs for local/dev convenience — replace with a proper trust store for production.
- Alert cooldown defaults to **300 seconds** per `source` to reduce alert storms.
