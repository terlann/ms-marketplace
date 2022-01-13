FROM openjdk:11.0.3-jdk-slim-stretch 
ARG DEPLOY_ENV
WORKDIR /app
ENV TZ Asia/Baku
COPY *.*ar .
RUN ls -la && ln -sfn *.*ar app
ENTRYPOINT ["java", "-jar", "./app", "--spring.profiles.active=${DEPLOY_ENV}"]
CMD [""]
