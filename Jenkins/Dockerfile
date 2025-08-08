FROM jenkins/jenkins:lts-jdk21

USER root

# Docker CLI installieren
RUN apt-get update && apt-get install -y docker.io && usermod -aG docker jenkins

# Create workspace directory with correct owner and permissions
RUN mkdir -p /var/jenkins_home/workspace && \
    chown -R jenkins:jenkins /var /var/jenkins_home && \
    chmod -R 755 /var /var/jenkins_home

# grant user jenkins access to /var/run/docker.sock
RUN usermod -aG messagebus jenkins

# install plugins
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false
COPY Jenkins.plugins /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt && \
    chown -R jenkins:jenkins /var/jenkins_home

USER jenkins
