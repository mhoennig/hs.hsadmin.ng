FROM eclipse-temurin:21-jdk
RUN apt-get update && \
    apt-get install -y bind9-utils pandoc && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

