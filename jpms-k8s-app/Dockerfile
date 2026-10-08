# Build from the repository root so the parent pom is part of the context:
#   podman build -f jpms-k8s-app/Dockerfile -t jpms-k8s-app .

# Only used as a source of the Maven distribution, the JDK in this image is not used
FROM docker.io/library/maven:3.9-eclipse-temurin-25 AS maven

# Build the runtime image inside the container. jlink copies the JDK's native libraries, so it has to run against a
# glibc that is not newer than the one in the final image (Ubuntu 22.04 here, Debian 13 below).
FROM docker.io/azul/zulu-openjdk:25.0.4.1 AS build
# binutils: jlink needs objcopy to strip native debug symbols, and we strip the libraries ourselves below
RUN apt-get update \
    && apt-get install -y --no-install-recommends binutils \
    && rm -rf /var/lib/apt/lists/*
COPY --from=maven /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven PATH=/usr/share/maven/bin:$PATH
WORKDIR /build
COPY pom.xml .
COPY jpms-k8s-app/pom.xml jpms-k8s-app/
RUN mvn -B -ntp dependency:go-offline
COPY jpms-k8s-app/src jpms-k8s-app/src
# Tests run in CI / locally; this stage only needs the jlink image.
# Dropping the symbol tables of the native libraries (libjvm.so alone is ~5 MB smaller) only affects the symbol names
# in JVM crash reports.
RUN mvn -B -ntp -DskipTests package \
    && find jpms-k8s-app/target/jlink -name '*.so' -exec strip --strip-unneeded {} +

FROM gcr.io/distroless/base-debian13:nonroot

ARG JVM_OPTS

# Distroless has no shell, so JVM flags are passed through the variable the JVM reads itself
ENV JAVA_TOOL_OPTIONS=$JVM_OPTS

WORKDIR /app

COPY --from=build /build/jpms-k8s-app/target/jlink /app/jvm

# Listens on $PORT (default 9000)
EXPOSE 9000

# Exec form: java runs as PID 1 and receives SIGTERM directly. Keep in sync with app.main.class in pom.xml
ENTRYPOINT ["/app/jvm/bin/java", "-m", "jpms.k8s.app/com.sigursoft.jpms.k8s.app.Application"]
