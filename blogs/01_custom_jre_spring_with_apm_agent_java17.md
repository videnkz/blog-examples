## Build minified java web application with Elastic Apm Agent
### Intro
Elastic APM such another APM tools provides monitoring and troubleshooting of application performance.
You can find more details about this here [link](https://www.elastic.co/blog/monitoring-java-applications-and-getting-started-with-the-elastic-apm-java-agent).

In this article we will create a minified Docker image for a 
Spring application with a custom Java Runtime Environment (JRE) 
based on Java modularity, that involves several steps to optimize 
the image size and include only the necessary dependencies.

### Prerequisites

1. Create a simple spring application that you can find here
   https://github.com/videnkz/blog-examples/spring-java17-service

### Write a Dockerfile
   We use multi-stage builds:

1. First, we are preparing a maven build environment,
   copying a project files into the image, and running a Maven build.
   The result of this build stage will be a built application

   ```
   FROM maven:3.8.6-amazoncorretto-17 AS build
   COPY src /usr/src/app/src
   COPY pom.xml /usr/src/app
   RUN mvn -f /usr/src/app/pom.xml clean package
   ```

2. Next, we set `Amazon Corretto JDK 18` as base image, this image is 
   based on the alpine version, making it lightweight.
   We need use `jdeps` tool - a Java tool that shows the package-level or class-level dependencies
   of Java class files to identify all java modules used by our application.
   From the previous build stage we take a built application and copy it to
   `/app/app.jar`.
   After we unzip our `app.jar` file and using `jdeps` tools, we scan all
   dependencies and print an aggregated comma-separated list of module dependencies.
   We write list of modules to `deps.info` file.
   ```
   COPY --from=build /usr/src/app/target/spring-java17-*.jar /app/app.jar
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
   ```
   `jdeps` options meanings:
   ```
   --ignore-missing-deps: Ignores any errors that might occur due to a missing dependency.
   --print-module-deps: Prints an aggregated, comma-separated list of module dependencies.
   -q: Sets the tool to quiet mode.
   --recursive: Recursively scans all dependencies.
   --multi-release 17: Interpret a multi-release jar file as version 17.
   --class-path and --module-path: These options specify the classpath and the module path that jdeps should use when analyzing the jar files.
   ```
3. We set `Amazon Correto 17` as base image.
   Now, we can create a tailored Java Runtime Environment(JRE)
   with only the necessary modules.
   For this purpose we use `jlink` tool, that used to generate
   a custom JRE with a reduced size, containing only specified 
   modules.
   We add all modules that were scanned with `jdeps` tool, from 
   previous stage. Note, we add `jdk.zipfs` module, this module
   used by the `apm-agent-java` and is required.

   ```
   FROM amazoncorretto:17-alpine as openjdk

   RUN apk add --no-cache binutils
   
   COPY --from=deps /deps.info /deps.info
   
   RUN echo "`cat deps.info`,jdk.zipfs" > /deps.info
   
   RUN $JAVA_HOME/bin/jlink \
   --verbose \
   --add-modules $(cat deps.info) \
   --strip-debug \
   --no-man-pages \
   --no-header-files \
   --compress=2 \
   --output /customjre
   ```
   
   `jlink` options meanings:
   ```
   --verbose: Output additional details about the linking process.
   --add-modules $(cat deps.info): Specifies which modules to add to the JRE. It will read the modules from the deps.info file.
   --strip-debug: Removes debug symbols to reduce the size of the generated JRE.
   --no-man-pages and --no-header-files: Don't include man pages or header files in the generated JRE.
   --compress=2: Apply ZIP compression to reduce the size of the generated JRE further.
   --output /customjre: The directory where the custom JRE will be generated.
   ```

4. We use a lightweight linux distribution as base image - `alpine:latest`.
   Next we create two ENV: `JAVA_HOME`, `PATH` and placed
   built from previous step custom JRE to `JAVA_HOME` path.
   After, we create a user under a which will work our application
   in docker container.
   
   Then we copy our application's jar file from `build` stage
   and download latest version of `elastic-apm-agent` to image's filesystem.
   Expose port on which a container will listen for connections.
   And finally, the CMD instruction provides defaults for
   executing container:

   ```
   FROM alpine:latest
   ENV JAVA_HOME=/jre
   ENV PATH="${JAVA_HOME}/bin:${PATH}"
   
   COPY --from=openjdk /customjre $JAVA_HOME
   
   ARG APPLICATION_USER=foobar
   RUN adduser --no-create-home -u 1000 -D $APPLICATION_USER
   
   USER 1000
   
   COPY --chown=1000:1000 --from=build /usr/src/app/target/spring-java17-*.jar /app/app.jar
   ADD --chown=1000:1000 https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.45.0/elastic-apm-agent-1.45.0.jar /app/elastic-apm-agent-1.45.0.jar
   
   WORKDIR /app
   
   EXPOSE $SERVER_PORT
   
   CMD /jre/bin/java $JAVA_MEMORY_OPTS $GC_OPTS $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar
   ```

4. Now, we can up our service with elastic apm components.
   Run docker-compose 
   ```
   docker-compose -f composes/base/docker-compose.yaml -f composes/01_custom_jre_spring_java17/docker-compose.yaml up -d
   ```
   And make a test request to your service:
   ```
   curl http://localhost:8080/ping

   or
   
   run request from `ping.http` with IDE
   ```

