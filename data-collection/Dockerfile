# prepare the image
FROM maven AS builder
WORKDIR /data-collection

# build the maven project
COPY pom.xml .
COPY /lib /data-collection/lib/
RUN mvn -DskipTests clean compile package

# copy only the neccessary files into the image to keep it small
FROM openjdk:11
WORKDIR /data-collection
COPY --from=builder /data-collection/lib /data-collection/lib/
COPY --from=builder /data-collection/target/*.jar /data-collection/lib/
COPY run-cloud.sh /data-collection/
COPY *.csv /data-collection/

# start the data-collection
# ENTRYPOINT ["sh", "run-cloud.sh"]
CMD []
