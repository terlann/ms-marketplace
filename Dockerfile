FROM openjdk:11.0.3-jdk-slim-stretch 
ARG DEPLOY_ENV
WORKDIR /app
ENV TZ Asia/Baku
COPY *.*ar .
COPY ./certs/kapitalho.cer .
RUN ls -la && ln -sfn *.*ar app
RUN $JAVA_HOME/bin/keytool -importcert -alias kbsrvpki -keystore  $JAVA_HOME/lib/security/cacerts -storepass changeit -file kapitalho.cer -noprompt
ENTRYPOINT ["java", "-jar", "./app", "--spring.profiles.active=${DEPLOY_ENV}"]
CMD [""]
