# Congestion Tax Calculator

A Spring Boot REST API for calculating congestion tax fees for vehicles in the Gothenburg area (2013).

## Requirements

- Java 17+
- Maven 3.8+

## Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

## API Usage

### Calculate Tax

**POST** `/api/tax/calculate?city=gothenburg`

```json
{
  "vehicleType": "CAR",
  "dates": [
    "2013-02-08 06:20:27",
    "2013-02-08 06:27:00",
    "2013-02-08 14:35:00",
    "2013-02-08 15:29:00",
    "2013-02-08 15:47:00",
    "2013-02-08 16:01:00",
    "2013-02-08 16:48:00",
    "2013-02-08 17:49:00",
    "2013-02-08 18:29:00",
    "2013-02-08 18:35:00"
  ]
}
```

**Response:**

```json
{
  "vehicleType": "CAR",
  "totalTax": 52,
  "taxByDate": {
    "2013-02-08": 52
  },
  "tollFree": false
}
```

### Vehicle Types

`CAR`, `MOTORCYCLE`, `BUS`, `EMERGENCY`, `DIPLOMAT`, `MILITARY`, `FOREIGN`

Toll-free types: `MOTORCYCLE`, `BUS`, `EMERGENCY`, `DIPLOMAT`, `MILITARY`, `FOREIGN`

## Architecture

- **Controller layer** — REST endpoint with validation
- **Service layer** — Core calculation logic, toll-free date detection
- **Config layer** — Rule loading from external JSON
- **Model layer** — Request/response DTOs, rule config, enums

Tax rules are externalized to `src/main/resources/data/gothenburg-tax-rules.json`. To add a new city, add a new JSON file and configure `tax.rules.path` or extend `TaxRuleLoader` to scan a directory.

## Running Tests

```bash
mvn test
```
