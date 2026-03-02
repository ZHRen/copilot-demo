# copilot-demo

A minimal Spring Boot REST API demonstrating file downloads, health checking, and IP-based rate limiting.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health` | Returns `{"status":"UP"}` — used by load balancers and monitoring tools. |
| `GET` | `/api/files/download?filename=<name>` | Downloads a file from the configured upload directory. |

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP port the application listens on. |
| `app.upload-dir` | `uploads` | Directory from which files are served (relative to the working directory). |
| `app.rate-limit.max-requests` | `10` | Maximum requests per IP per time window for the download endpoint. |
| `app.rate-limit.window-seconds` | `60` | Length of the rate-limit time window in seconds. |

## Security

- **Filename validation** – the `filename` query parameter must match `[\w.\-]+`, blocking path-traversal characters such as `/`, `\`, and `..`.
- **Path-traversal guard** – even after the regex check, the resolved file path is verified to remain inside the upload directory.
- **Rate limiting** – the `RateLimitInterceptor` enforces a per-IP sliding-window rate limit on the download endpoint. Requests that exceed the limit receive HTTP 429.

## Building and Running

```bash
# Compile and package (skip tests)
mvn package -DskipTests

# Run all tests
mvn test

# Start the application (available on http://localhost:8080)
mvn spring-boot:run
```

## Project Structure

```
src/main/java/com/example/demo/
  DemoApplication.java          Spring Boot entry point
  HealthController.java         GET /api/health
  FileDownloadController.java   GET /api/files/download
  RateLimitInterceptor.java     Per-IP rate limiting (HandlerInterceptor)
  WebMvcConfig.java             Registers the rate-limit interceptor

src/test/java/com/example/demo/
  HealthControllerTest.java          Tests for HealthController
  FileDownloadControllerTest.java    Tests for FileDownloadController
  RateLimitInterceptorTest.java      Tests for rate-limit behaviour
```