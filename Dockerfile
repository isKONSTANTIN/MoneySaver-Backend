FROM openjdk:11

MAINTAINER isKONSTANTIN <me@knst.su>

ENV TZ=Europe/Moscow

EXPOSE 8080

WORKDIR /ms

COPY MoneySaver.jar ./

ENTRYPOINT ["java", "-jar", "-Xmx64M", "MoneySaver.jar"]