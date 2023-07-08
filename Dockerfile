# NOTES
# Hyper-V should be enabled
# Virtual Machine Platform and Windows Hypervisor Platform should be DISABLED
# they clash and result in docker not loading properly

# this allows heroku to run both java backend and client frontend without buildpack error

# local deploy
# volume arg requires absolute path
# docker run -v C:\Users\simon\Desktop\Projects\JavKing\src\main\resources\default.properties:/app/src/main/resources/default.properties testapp

# Push to heroku
# 
# docker tag <image name> registry.heroku.com/<app name>/web
# heroku container:login
# docker push registry.heroku.com/<app name>/web
# heroku container:release web -a <app name>

FROM maven:latest AS java-build
WORKDIR /app

COPY . .
RUN mvn clean package

FROM openjdk:latest
WORKDIR /app

COPY --from=java-build /app/target/JavKing-1.1.jar .

EXPOSE 8080 3080
# array ensures correct interpretation and handling of special characters
CMD java -XX:+UseContainerSupport -XX:MaxRAMPercentage=50 $JAVA_OPTS -jar -Dserver.port=$PORT JavKing-1.1.jar

# docker build -t javking .
# docker run -p 8080:8080 javking