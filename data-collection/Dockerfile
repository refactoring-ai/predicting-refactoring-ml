WORKDIR /data-collection
VOLUME /data-collection
VOLUME /storage

#build
FROM maven
COPY pom.xml .
RUN mvn clean compile package
COPY . .

#run
FROM openjdk:11
ENTRYPOINT ["sh", "run-cloud.sh"]
CMD []
