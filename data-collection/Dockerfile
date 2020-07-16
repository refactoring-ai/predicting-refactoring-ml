# prepare the image
FROM maven:3-openjdk-11
WORKDIR /data-collection

# build the maven project
COPY . .
RUN mvn -DskipTests clean compile package

# start the data-collection
ENTRYPOINT ["sh", "run-cloud.sh"]
