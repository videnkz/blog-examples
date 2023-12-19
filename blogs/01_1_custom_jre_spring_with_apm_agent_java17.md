## Build minified java 17 web application with Elastic Apm Agent
### Intro
Elastic APM such another APM tools provides monitoring and troubleshooting of application performance.
You can find more details about this here [link](https://www.elastic.co/blog/monitoring-java-applications-and-getting-started-with-the-elastic-apm-java-agent).

In this article we will create a minified Docker image for a 
Spring application with a custom Java Runtime Environment (JRE) 
based on Java modularity, that involves several steps to optimize 
the image size and include only the necessary dependencies.

### Prerequisites

1. Create a simple spring application that you can find here
   https://github.com/videnkz/blog-examples/tree/master/spring-java17

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
   FROM amazoncorretto:18-alpine as deps

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
   FROM amazoncorretto:17-alpine as minified

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
   
   COPY --from=minified /customjre $JAVA_HOME
   
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
   docker-compose -f blogs/composes/base/docker-compose.yaml -f blogs/composes/01_custom_jre_spring_java17/docker-compose.yaml up -d
   ```
   And make a test request to your service:
   ```
   curl http://localhost:8080/ping

   or
   
   run request from `ping.http` with IDE
   ```

### Summary

1. **Reduced image size**:
   
   The size of the Docker image is significantly
   decreased when using a custom JRE, as it only
   includes the necessary Java modules needed to 
   run your application.
   A full JDK, on the other hand, includes development
   tools and libraries that might not be needed in a
   production environment.
   A smaller image results in reduced storage needs,
   more efficient distribution of the image and
   faster start times.

   In `spring-java17` provided 2 version of Dockerfile:
   - Dockerfile - with customer JRE
   - Dockerfile - based on `amazoncorretto:17-alpine` image
   
   If we build images based on these files:
   ```
   docker build -t videnkz/spring-java17-minified:2.0.0 . 
   docker build --file=Dockerfile_v2 -t videnkz/spring-java17-full:2.0.0 .
   
   REPOSITORY                                      TAG                       IMAGE ID       CREATED         SIZE
   videnkz/spring-java17-full                      2.0.0                     ba34af90bf57   6 seconds ago   320MB
   videnkz/spring-java17-minified                  2.0.0                     b38e36c861e6   2 minutes ago   95.9MB
   ```
   we'll see that the size of the minified image is ~3.3 lower.   

2. **Improved Security**:
   
   By minimizing the surface area of your Java 
   runtime environment, you're limiting the potential
   avenues of attack for malicious entities. 
   This is a security concept known as the Principle 
   of Least Privilege.

3. **Faster Build and Deployment**:
   
   Less data to transfer over the network might lead 
   to faster build and deployment times. This can 
   speed up your Continuous Integration / 
   Continuous Deployment (CI/CD) pipelines, and save 
   on bandwidth costs.

4. **Greater Control and Customization**:

   With a custom JRE, you have precise control over 
   what's included in your runtime environment. You 
   can include only the modules that your application 
   needs, which might lead to improved performance 
   and compatibility.

5. **Advantage of using built into a Docker image Elatic Apm Agent**:

   - **Runtime Performance Insights**: 
   
   Elastic APM collects real-time data about your application's performance, including transaction times, system load, error rates, and more. You can use this information to understand where and when performance bottlenecks occur, and proactively fix these issues.
   - **Error Tracking and Analysis**: 
   
   The agent automatically collects uncaught exceptions and errors, presenting them in a consolidated view in the Elastic APM dashboard. Structure of these errors and their occurrences can give you insights into potential areas of instability in your application.
   - **Traceability**: 
   
   The Distributed Tracing feature gives you a detailed trace of each transaction as it passes through your distributed systems. This can greatly aid in debugging and troubleshooting by showing you the exact path and timing of a transaction.
   - **Custom Metrics and Events**: 
   
   Elastic APM allows you to record custom metrics and events, tailoring your monitoring to your application's specific needs.
   - **Integration with Elastic Stack**: 
   
   The APM agent's data integrates seamlessly with Elasticsearch, Logstash, and Kibana (also known as the Elastic Stack), providing powerful search, visualization, and log processing capabilities.
   - **Resource Efficiency**: 
   
   By incorporating the Elastic APM agent directly into the Docker image, you're able to track the performance of your application from the moment it's launched, without a need for additional setup or configuration, leading to seamless deployment.
 
### Example

   In `APM` tab you can now find list of services:   

   ![01_1_0_services.png](images%2F01_1%2F01_1_0_services.png)
   You can discover service information:

   ![01_1_1_service_metadata.png](images%2F01_1%2F01_1_1_service_metadata.png)

   And get all transaction, error details:

   ![01_1_2_data.png](images%2F01_1%2F01_1_2_data.png)
