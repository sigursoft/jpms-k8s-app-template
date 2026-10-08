# jpms-k8s-app-template
Java Platform Module System (JPMS) Kubernetes Application Template

Experimental project with the aim to create the smallest working Java client/server application suitable for Kubernetes environment. We use pure JPMS approach with minimal dependencies and a custom build JVM binary (via `jlink`) as a target artifact.  


## Build and run

Requires JDK 25 and Maven 3.9+ (checked by the build).

```shell
mvn clean package          # compiles, runs the tests and builds the jlink runtime image in jpms-k8s-app/target/jlink
```

Build the container from the repository root (the build runs Maven inside the container, so it needs no local JDK):

```shell
podman build -f jpms-k8s-app/Dockerfile -t jpms-k8s-app .
podman run --rm -p 9000:9000 jpms-k8s-app
```

The final image is based on `gcr.io/distroless/base-debian13:nonroot`: it has no shell, runs as a non-root user and
starts Java as PID 1, so it receives SIGTERM directly and drains in-flight requests (up to 5 seconds) before exiting.

## Configuration

| Setting | Default | Description |
|---|---|---|
| `PORT` environment variable | `9000` | Port the server listens on |
| `LOG_LEVEL` environment variable | `INFO` | Root log level (`DEBUG` also logs every HTTP exchange) |
| `--build-arg JVM_OPTS=...` | empty | JVM flags, e.g. `-Xmx128m`; exposed to the JVM as `JAVA_TOOL_OPTIONS` |
| `-Dsun.net.httpserver.maxReqTime` / `maxRspTime` | `30` s | Time a client may take to send a request / read a response |

Sensors are kept in memory (at most 10 000, descriptions up to 1024 characters), so every replica has its own state.

## API

| Method and path | Result |
|---|---|
| `GET /` | `{"message":"Hello World!"}` |
| `GET /sensors` | All sensors ordered by id |
| `POST /sensors` | Creates a sensor from `{"id": "s-001", "description": "..."}` (`Content-Type: application/json`, body up to 64 KB). `201` with a `Location` header; `400` invalid; `409` duplicate; `415` wrong content type; `507` sensor limit reached |
| `GET /sensors/{id}` | The sensor, or `404` |

An `id` matches `[A-Za-z0-9._-]{1,64}`. Errors are returned as `{"error": "..."}`. Other methods get `405` with an
`Allow` header; `OPTIONS` lists the allowed methods.
