# build
FROM maven
WORKDIR /data-collection
COPY pom.xml .
COPY /lib/RefactoringMiner-5e9b5f1131f0908c6bb82f9ae95a4c5723551bcd.jar /data-collection/lib/RefactoringMiner-5e9b5f1131f0908c6bb82f9ae95a4c5723551bcd.jar 
RUN mvn -DskipTests clean compile package
COPY . .

CMD []
