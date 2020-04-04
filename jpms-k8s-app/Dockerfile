FROM debian:10-slim

ARG JVM_OPTS
ARG MODULE
ARG CLASS

ENV JAVA_OPTS=$JVM_OPTS
ENV APP_MODULE=$MODULE
ENV MAIN_CLASS=$CLASS

RUN mkdir /app && mkdir /app/config

WORKDIR /app

VOLUME /app/config

ENTRYPOINT ["/bin/bash","-c"]
CMD ["/app/jvm/bin/java ${JVM_OPTS} -m ${APP_MODULE}/${MAIN_CLASS}"]

ADD ./target/jlink jvm
