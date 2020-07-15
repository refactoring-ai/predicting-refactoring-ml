# build
FROM maven:3-openjdk-11
WORKDIR /data-collection
COPY . .
RUN mvn -DskipTests clean compile package

VOLUME /data-collection
VOLUME /storage

CMD []
