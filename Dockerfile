# OFFICIAL JDK FOR JAVA 21 RUNTIME AS PARENT IMAGE
FROM eclipse-temurin:21-jre-alpine


# ✅ Install timezone data
RUN apk add --no-cache tzdata

# ✅ Set timezone
ENV TZ=Africa/Nairobi
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR application
ARG JAR_FILE=target/*.jar

# COPY JAR FILE
COPY ${JAR_FILE} application.jar

# CONFIG PROPERTIES | OPTIONAL
COPY config/application.properties application.properties


# RUN APPLICATION WHEN CONTAINER LAUNCHES
CMD ["java","-jar","application.jar","--spring.config.location=file:application.properties"]
RUN mkdir sslcertificates



