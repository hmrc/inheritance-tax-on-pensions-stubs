
# Inheritance Tax on Pensions Stubs

Microservice to provide endpoints to replicate request and response from the IHTP API.
Inheritance Tax on Pensions is a feature on manage your pension (MPS) service. Pension Scheme Administrators (PSA) and/or
Pension Scheme Practitioners use this service for reporting IHT due on unused pension funds and retrieving payment reference.

## Endpoints

### Submit IHTP report

- **URL**: `/etmp/RESTAdapter/pods/reports/ihtp`
- **Method**: `POST`

The body of the payload is the report details built from user answers to be submitted down to ETMP.

#### Example individual payload

```json
{
  "reportDetails": {
    "pstr": "S2400000001"
  },
  "deceasedDetails": {
    "inheritanceTaxReference": "A123456/25A",
    "title": "Mr",
    "firstForename": "John",
    "secondForename": "William",
    "surname": "Doe",
    "dateOfBirth": "1950-01-01",
    "dateOfDeath": "2026-01-01",
    "nino": null,
    "reasonForNoNino": "Reason for no national insurance number"
  },
  "lprDetails": {
    "individual": {
      "title": "Mr",
      "firstForename": "John",
      "secondForename": "William",
      "surname": "Doe"
    }
  }
}
```

#### Example organisation payload

```json
{
  "reportDetails": {
    "pstr": "S2400000001"
  },
  "deceasedDetails": {
    "inheritanceTaxReference": "A123456/25A",
    "title": "Mr",
    "firstForename": "John",
    "secondForename": "William",
    "surname": "Doe",
    "dateOfBirth": "1950-01-01",
    "dateOfDeath": "2026-01-01",
    "nino": null,
    "reasonForNoNino": "Reason for no national insurance number"
  },
  "lprDetails": {
    "organisation": {
      "organisationName": "Doe Incorporated"
    }
  }
}
```

#### Submit report stub scenarios

The last character of the `inheritanceTaxReference` is used to return specific error scenarios.

| Scenario | `inheritanceTaxReference` suffix | Response |
| --- | --- | --- |
| Success | `A123456/25A` | `200 OK` |
| Bad request | `A123456/25B` | `400 Bad Request` |
| Internal server error | `A123456/25C` | `500 Internal Server Error` |
| Service unavailable | `A123456/25D` | `503 Service Unavailable` |
| Unprocessable entity | `A123456/25E` | `422 Unprocessable Entity` |

### Get IHTP overview

- **URL**: `/etmp/RESTAdapter/pods/reports/ihtp-overview`
- **Method**: `GET`

**Query parameters**:

- `pstr` - required
- `dateFrom` - required, for example `2026-01-01`
- `dateTo` - required, for example `2026-12-31`
- `status` - optional, for example `Processed`

#### Overview stub scenarios

The overview endpoint can return different responses by changing query parameter values. This is intentionally deterministic so
that Bruno and frontend/backend tests can exercise success and error paths without needing realistic ETMP data.

Known PSTRs:

- `24000001IN`
- `24000002IN`

| Scenario | Query values | Response |
| --- | --- | --- |
| Successful overview list | Known `pstr`, matching `dateFrom` / `dateTo`, no `status` | `200 OK` with all matching overview records |
| Successful filtered overview list | Known `pstr`, matching `dateFrom` / `dateTo`, `status=Processed` | `200 OK` with matching processed overview records |
| Successful filtered overview list | Known `pstr`, matching `dateFrom` / `dateTo`, `status=Submitted` | `200 OK` with matching submitted overview records |
| No records found | Unknown `pstr` | `422 Unprocessable Entity` |
| No records found | Known `pstr`, date range with no matching overview records | `422 Unprocessable Entity` |
| No records found | Known `pstr`, normal `status` value with no matching overview records | `422 Unprocessable Entity` |
| Forced no records response | `status=NO_RECORDS` | `422 Unprocessable Entity` |
| Forced bad request response | `status=BAD_REQUEST` | `400 Bad Request` |
| Forced internal server error response | `status=SERVER_ERROR` | `500 Internal Server Error` |
| Forced service unavailable response | `status=SERVICE_UNAVAILABLE` | `503 Service Unavailable` |

### Get IHTP report

- **URL**: `/etmp/RESTAdapter/pods/reports/ihtp`
- **Method**: `GET`

**Query parameters**:

- `pstr` - required
- `fbNumber` - optional, used for specific record retrieval (12-digit pattern: `^[0-9]{12}$`)
- `paymentReferenceNumber` - optional, must be used with `versionNumber`
- `versionNumber` - optional, must be used with `paymentReferenceNumber` (3-digit pattern: `^[0-9]{3}$`)

**Parameter combinations**:
- `pstr` + `fbNumber` - retrieve by form bundle number
- `pstr` + `paymentReferenceNumber` + `versionNumber` - retrieve by payment reference and version

#### Retrieve stub scenarios

The retrieve endpoint can return different responses by changing query parameter values. This is intentionally deterministic so
that Bruno and frontend/backend tests can exercise success and error paths without needing realistic ETMP data.

Known fbNumbers:

- `119000004320` (PSTR: 24000001IN)
- `119000004322` (PSTR: 24000002IN)

Known paymentReference + version combinations:

- `PR000000001` + `001` (PSTR: 24000001IN)

| Scenario | Query values | Response |
| --- | --- | --- |
| Successful retrieve by fbNumber | Known `pstr`, known `fbNumber` | `200 OK` with full report payload |
| Successful retrieve by payment reference | Known `pstr`, known `paymentReferenceNumber` + `versionNumber` | `200 OK` with full report payload |
| No records found | Known `pstr`, unknown `fbNumber` | `422 Unprocessable Entity` |
| Bad request | Invalid parameter combination (e.g., fbNumber with paymentReferenceNumber) | `400 Bad Request` |
| Bad request | Missing required parameters (no fbNumber or paymentReferenceNumber + versionNumber) | `400 Bad Request` |

## Running the service

1. Make sure you run all the dependant services through the service manager:

   > `sm2 --start IHTP_ALL`

2. Stop the frontend microservice from the service manager and run it locally:

   > `sm2 --stop INHERITANCE_TAX_ON_PENSIONS_STUBS`

   > `sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes`

The service runs on port `10712` by default. E.g:  http://localhost:10712/ping/ping

## Testing with Bruno

A Bruno collection is available in `test/resources/IHTP Stubs`.

The stubs collection does not require a login request. Requests are sent directly to the stubbed ETMP-style endpoints and use
`auth: none`.

To use it:

1. Open Bruno.
2. Select **Open Collection**.
3. Open the `test/resources/IHTP Stubs` folder.
4. Select the `LocalHost - IHTP Stubs` environment.
5. Run the stubs service locally on port `10712`.
6. Run one of the requests listed below.

Useful requests:

- `Ping` - checks the stubs service is running
- `Submit - Success` - exercises the successful submit report response
- `Overview - Success` - exercises the successful overview response
- `Overview - No Records 422` - exercises the no records overview response
- `Overview - Bad Request 400` - exercises the forced bad request overview response
- `Overview - Server Error 500` - exercises the forced internal server error overview response
- `Overview - Service Unavailable 503` - exercises the forced service unavailable overview response
- `Retrieve - Success (fbNumber)` - exercises the successful retrieve by fbNumber response
- `Retrieve - Success (paymentReference + version)` - exercises the successful retrieve by payment reference response
- `Retrieve - No Records 422` - exercises the no records retrieve response
- `Retrieve - Bad Request 400` - exercises the bad request retrieve response

### Unit tests

> `sbt test`

### Integration tests

> `sbt it/test`

You can also execute the [runtests.sh](runtests.sh) file to run both unit and integration tests and generate coverage report easily.

```bash
/bin/bash ./runtests.sh
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
