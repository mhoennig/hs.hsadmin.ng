FROM eclipse-temurin:21-jdk
RUN apt-get update && \
    apt-get install -y bind9-utils && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


# RUN mkdir /opt/app
# COPY japp.jar /opt
# CMD ["java", "-jar", "/opt/app/japp.jar"]
