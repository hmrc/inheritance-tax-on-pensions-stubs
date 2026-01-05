
# Inheritance Tax on Pensions Stubs

Microservice to provide endpoints to replicate request and response from the IHTP API. Inheritance Tax on Pensions is a feature on manage your pension (MPS) service. Pension Scheme Administrators (PSA) and/or Pension Scheme Practitioners use this service for reporting IHT due on unused pension funds and retrieving payment reference.

Description of the API TBC

Test data description TBC

## Running the service

1. Make sure you run all the dependant services through the service manager:

   > `sm2 --start IHTP_ALL`

2. Stop the frontend microservice from the service manager and run it locally:

   > `sm2 --stop INHERITANCE_TAX_ON_PENSIONS_STUBS`

   > `sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes`

The service runs on port `10712` by default. E.g:  http://localhost:10712/ping/ping


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