FROM adoptopenjdk:14-jre-hotspot

ARG JAR_FILE
ARG JVM_OPTS
ARG MODULE
ARG CLASS

ENV JAVA_OPTS=$JVM_OPTS
ENV APP_MODULE=$MODULE
ENV MAIN_CLASS=$CLASS

RUN mkdir /app && mkdir /app/lib && mkdir /app/config

WORKDIR /app

VOLUME /app/config

ENTRYPOINT ["/bin/bash","-c"]
CMD ["java ${JVM_OPTS} --module-path application.jar:lib -m ${APP_MODULE}/${MAIN_CLASS}"]

ADD ./target/lib lib/
ADD ./target/${JAR_FILE} application.jar
