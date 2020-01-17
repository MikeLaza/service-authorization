FROM openjdk:8-jre-alpine
LABEL version=5.0.0-RC-7 description="Unified Authorization Trap for all ReportPortal's Services" maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>"
RUN apk add --no-cache ca-certificates
ENV JAVA_OPTS="-Xmx512m -Djava.security.egd=file:/dev/./urandom" ARTIFACT=service-authorization-5.0.0-RC-7-exec.jar APP_DOWNLOAD_URL=https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/5.0.0-RC-7/service-authorization-5.0.0-RC-7-exec.jar
RUN sh -c "echo $'#!/bin/sh \n\
exec java $JAVA_OPTS -jar $ARTIFACT' > /start.sh && chmod +x /start.sh"
VOLUME ["/tmp"]
RUN wget $APP_DOWNLOAD_URL
EXPOSE 8080
ENTRYPOINT ["/start.sh"]
