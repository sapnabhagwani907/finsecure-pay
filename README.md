# FinSecure Pay - Transaction Validation API

Spring Boot 3 / Java 21 service that validates payment transactions.

## Endpoints

| Method | Path        | Description                                  |
|--------|-------------|----------------------------------------------|
| GET    | `/health`   | Liveness check, returns status and version   |
| POST   | `/validate` | Validates a transaction (200 valid / 400 invalid) |
| GET    | `/actuator/prometheus` | Prometheus metrics (used in Task 6) |

### Validation rules

1. `transactionId` is required
2. `sourceAccount` and `destinationAccount` are required, 8-20 alphanumeric characters
3. Source and destination accounts must differ
4. `amount` is required, > 0, at most 2 decimal places
5. `amount` must not exceed `app.validation.max-amount` (default 100000)
6. `currency` must be one of `USD, EUR, GBP, INR`

All violations are returned together in the `errors` array.

## Build and test

Requires JDK 21 and Maven 3.9+.

```bash
mvn clean verify          # compile, run unit tests, package the jar
mvn test                  # tests only
```

Test reports: `target/surefire-reports/`

## Run locally

```bash
java -jar target/finsecure-pay-1.0.0.jar
# or
mvn spring-boot:run
```

```bash
curl http://localhost:8080/health

# valid -> 200
curl -i -X POST http://localhost:8080/validate -H "Content-Type: application/json" -d '{
  "transactionId": "TX-1001",
  "sourceAccount": "ACC12345678",
  "destinationAccount": "ACC87654321",
  "amount": 250.50,
  "currency": "USD"
}'

# invalid -> 400 with errors
curl -i -X POST http://localhost:8080/validate -H "Content-Type: application/json" -d '{
  "transactionId": "TX-1002",
  "sourceAccount": "ACC12345678",
  "destinationAccount": "ACC12345678",
  "amount": -10,
  "currency": "XYZ"
}'
```

## Configuration

Override via environment variables (these will come from a Kubernetes ConfigMap in Task 3):

| Variable                  | Default | Purpose                        |
|---------------------------|---------|--------------------------------|
| `APP_VALIDATION_MAX_AMOUNT` | 100000  | Per-transaction amount limit   |
| `APP_VERSION`             | 1.0.0   | Version string shown in /health |
| `SERVER_PORT`             | 8080    | HTTP port                      |
