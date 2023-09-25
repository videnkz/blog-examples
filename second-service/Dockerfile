FROM maven:3.8.6-amazoncorretto-17 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM amazoncorretto:18-alpine as deps

COPY --from=build /usr/src/app/target/second-service-*.jar /app/app.jar
RUN mkdir /app/unpacked && \
    cd /app/unpacked && \
    unzip ../app.jar && \
    cd .. && \
    $JAVA_HOME/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    -q \
    --recursive \
    --multi-release 17 \
    --class-path="./unpacked/BOOT-INF/lib/*" \
    --module-path="./unpacked/BOOT-INF/lib/*" \
    ./app.jar > /deps.info

FROM amazoncorretto:17-alpine as openjdk

RUN apk add --no-cache binutils

COPY --from=deps /deps.info /deps.info

RUN echo "`cat deps.info`,jdk.crypto.ec,jdk.zipfs" > /deps.info

RUN $JAVA_HOME/bin/jlink \
    --verbose \
    --add-modules $(cat deps.info) \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /customjre

FROM alpine:latest
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=openjdk /customjre $JAVA_HOME

ARG APPLICATION_USER=foobar
RUN adduser --no-create-home -u 1000 -D $APPLICATION_USER

RUN mkdir /app && \
    chown -R $APPLICATION_USER /app

USER 1000

COPY --chown=1000:1000 --from=build /usr/src/app/target/second-service-*.jar /app/app.jar
ADD --chown=1000:1000 https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.42.0/elastic-apm-agent-1.42.0.jar /app/elastic-apm-agent-1.42.0.jar

WORKDIR /app

EXPOSE $SERVER_PORT

CMD /jre/bin/java $JAVA_MEMORY_OPTS $GC_OPTS $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar
