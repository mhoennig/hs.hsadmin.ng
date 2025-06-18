FROM jenkins/jenkins:lts-jdk21

USER root

# Docker CLI installieren
RUN apt-get update && apt-get install -y docker.io && usermod -aG docker jenkins

# grant user jenkins access to /var/run/docker.sock
RUN usermod -aG messagebus jenkins

# install plugins
COPY Jenkins.plugins /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins
