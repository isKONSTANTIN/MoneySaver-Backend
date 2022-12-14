FROM gradle:jdk11-alpine AS BUILD_STAGE
COPY . /home/gradle
RUN gradle build || return 1

FROM openjdk:11

MAINTAINER isKONSTANTIN <me@knst.su>

ENV TZ=Europe/Moscow

EXPOSE 8080

WORKDIR /ms
COPY --from=BUILD_STAGE /home/gradle/build/libs/MoneySaver.jar ./
ENTRYPOINT ["java", "-jar", "-Xmx64M", "MoneySaver.jar"]