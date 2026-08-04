# Employee API

My solution for the ReliaQuest entry-level Java challenge. Three REST endpoints that expose employee
data, held in memory instead of a database.

## Requirements

JDK 17. The Gradle wrapper is 7.6.4, which does not run on newer versions.

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew build
```

## Running it

```bash
./gradlew bootRun
```

Starts on port 8080. Every request needs an API key in the `X-API-Key` header. Locally the key is
`local-development-key`, set in `application.yml` and replaceable with the `API_KEY` environment
variable.

## Endpoints

Everything sits under `/api/v1/employee`. Three employees are seeded at startup so there is
something to read right away.

### Get all employees

```bash
curl -H 'X-API-Key: local-development-key' http://localhost:8080/api/v1/employee
```

### Get one employee

```bash
curl -H 'X-API-Key: local-development-key' \
  http://localhost:8080/api/v1/employee/<uuid>
```

404 if no employee has that UUID. 400 if the UUID is malformed.

### Create an employee

```bash
curl -X POST http://localhost:8080/api/v1/employee \
  -H 'X-API-Key: local-development-key' \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Ada",
    "lastName": "Byron",
    "salary": 95000,
    "age": 30,
    "jobTitle": "Software Engineer",
    "email": "ada.byron@example.com"
  }'
```

Returns 201 with the new employee and the UUID the service assigned. Bad input returns 400 listing
each field that failed:

```json
{
  "salary": "must be greater than 0",
  "age": "must be greater than or equal to 16",
  "email": "must be a well-formed email address"
}
```

## Tests

```bash
./gradlew test
```

13 tests. The service tests are plain JUnit with no Spring. The endpoint tests use MockMvc and cover
the success cases, the 404 and 400 responses, and both API key failures.

## Design notes

**The request body has its own type.** The stub was `createEmployee(Object requestBody)`, so I
replaced `Object` with a `CreateEmployeeRequest` record holding only what a caller should send. UUID,
full name and the dates are left out on purpose. A caller should not get to pick an employee's UUID
or backdate a hire date.

**Employees live in a map, not a list.** Lookup is by UUID, so a map does it directly instead of
walking the whole list. It is a `ConcurrentHashMap` because Spring serves requests on more than one
thread.

**The service returns an Optional and the controller picks the status code.** The service should not
need to know it is sitting behind a REST API. Deciding that "not found" means 404 is the controller's
job.

**Full name is stored, not computed.** The `Employee` interface has both a getter and a setter for
it, so I kept it as a real field. Working it out in the getter would leave the setter doing nothing
while still being part of the contract. The service builds it when an employee is created.

**Seed data goes through the same code as the API.** The three starting employees are created by
calling `createEmployee`, the same method the POST endpoint uses, so they cannot drift apart. The
cost is that all three share a hire date of whenever the app started. I preferred that over a second
way to build an employee.

**Validation errors say what actually went wrong.** By default Spring returns `Bad Request` without
mentioning which field failed. Employees-R-US is an automated consumer and cannot go read the source
to work it out, so a small handler returns each bad field with its message.

**A filter instead of Spring Security.** Everything requires an `X-API-Key` header, and one filter
covers that. With a single shared key there are no users or roles to keep track of, so Spring
Security would mostly be setup for something nothing uses. If this needed to tell clients apart, that
answer changes.

The check uses `MessageDigest.isEqual` rather than `equals`. Normal string comparison stops at the
first character that differs, so how long it takes hints at how much of a guessed key was right.

## What I would change for production

The API key would come from a secrets manager. The default in `application.yml` only exists so the
app runs without setup.

The map would become a real database. The service's public methods would stay the same.

Employees-R-US is an outside consumer, so I would add rate limiting and request logging before this
touched real employee data.
