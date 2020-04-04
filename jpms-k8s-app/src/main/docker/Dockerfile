FROM adoptopenjdk:14-jre-hotspot

RUN mkdir /app && mkdir /app/lib && mkdir /app/config

WORKDIR /app

COPY ./jpms-k8s-app/target/lib lib/
COPY ./jpms-k8s-app/target/jpms-k8s-app-0.1-SNAPSHOT.jar application.jar

VOLUME /app/config

CMD ["java", "-jar", "application.jar"]
