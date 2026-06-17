FROM eclipse-temurin:21-jdk

# create mount point for jenkins_home
RUN mkdir -p /var/jenkins_home && \
    chmod 755 /var/jenkins_home

# install required packages
RUN apt-get update && \
    apt-get install -y \
        postgresql-client \
        bind9-utils \
        docker.io \
        pandoc \
        pandoc-plantuml-filter \
        texlive-latex-base \
        texlive-fonts-recommended \
        texlive-extra-utils \
        texlive-latex-extra \
        texlive-xetex \
        nodejs \
        npm && \
    mkdir -p /tmp/docker-home && \
    HOME=/tmp/docker-home npm install -g mermaid-filter && \
    chmod -R a+rwX /tmp/docker-home && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# continue with the same uid as the user 'jenkins' from the Jenkins Dockerfile
USER 1000
