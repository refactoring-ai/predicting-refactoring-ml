# build
FROM maven
WORKDIR /data-collection
COPY pom.xml .
COPY /lib/RefactoringMiner-5e9b5f1131f0908c6bb82f9ae95a4c5723551bcd.jar /data-collection/lib/RefactoringMiner-5e9b5f1131f0908c6bb82f9ae95a4c5723551bcd.jar 
RUN mvn -Dskiptests clean compile package
COPY . .

# package without maven
FROM openjdk:11
VOLUME /data-collection
VOLUME /storage
WORKDIR /data-collection

COPY --from=0 /data-collection/target/*.jar ./
COPY --from=0 /data-collection/lib/*.jar ./

ENTRYPOINT ["sh", "run-cload.sh"]
