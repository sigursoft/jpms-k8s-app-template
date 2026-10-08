FROM gcr.io/distroless/base-debian13:nonroot

ARG JVM_OPTS

# Distroless has no shell, so JVM flags are passed through the variable the JVM reads itself
ENV JAVA_TOOL_OPTIONS=$JVM_OPTS

WORKDIR /app

VOLUME /app/config

# Exec form: java runs as PID 1 and receives SIGTERM directly. Keep in sync with app.main.class in pom.xml
ENTRYPOINT ["/app/jvm/bin/java", "-m", "jpms.k8s.app/com.sigursoft.jpms.k8s.app.Application"]

COPY ./target/jlink jvm
