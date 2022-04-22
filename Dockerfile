FROM openjdk:11.0.3-jdk-slim-stretch 
ARG DEPLOY_ENV
WORKDIR /app
ENV TZ Asia/Baku
COPY *.*ar .
COPY srs.cer /app/
RUN ls -la && ln -sfn *.*ar app
RUN keytool -importcert -alias kbsrvpki -keystore  $JAVA_HOME/lib/security/cacerts -storepass changeit -file srs.cer -noprompt
ENTRYPOINT ["java", "-jar", "./app", "--spring.profiles.active=${DEPLOY_ENV}"]
CMD [""]
