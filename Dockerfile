# Stage 1: build
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*

# Create a toolchains.xml pointing to the installed JDK so the maven-toolchains-plugin is satisfied
RUN mkdir -p /root/.m2 && \
    printf '<?xml version="1.0" encoding="UTF-8"?>\n<toolchains>\n  <toolchain>\n    <type>jdk</type>\n    <provides><version>25</version></provides>\n    <configuration><jdkHome>%s</jdkHome></configuration>\n  </toolchain>\n</toolchains>\n' "$JAVA_HOME" \
    > /root/.m2/toolchains.xml

COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: runtime
FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /app/target/illusion-*.war app.war
EXPOSE 8079
ENTRYPOINT ["java", "-jar", "app.war"]
