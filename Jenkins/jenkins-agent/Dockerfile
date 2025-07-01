FROM eclipse-temurin:21-jdk
RUN apt-get update && \
    apt-get install -y \
        postgresql-client \
        bind9-utils \
        docker.io \
        pandoc && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
