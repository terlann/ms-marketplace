FROM openjdk:11.0.3-jdk-slim-stretch 
ARG DEPLOY_ENV
WORKDIR /app
ENV TZ Asia/Baku
COPY srs.cer .
COPY *.*ar .
RUN ls -la && ln -sfn *.*ar app
RUN apt-get update && apt-get install -y curl
RUN apt-get update && apt-get install -y telnet
RUN keytool -importcert -alias srsssl -keystore /usr/local/openjdk-11/lib/security/cacerts -storepass changeit -file srs.cer -noprompt
ENTRYPOINT ["java", "-jar", "./app.jar", "--spring.profiles.active=${DEPLOY_ENV}"]
CMD [""]  
