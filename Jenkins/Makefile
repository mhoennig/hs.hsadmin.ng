DOCKER := docker
SOCKET := /var/run/docker.sock
VOLUME := jenkins_home

.PHONY: build run bash init-pw unprotected protected start stop rm purge

# building the Jenkins image
build:
	$(DOCKER) build -t jenkins-docker .

# initially running the Jenkins container
run:
	$(DOCKER) run --detach \
		--dns 8.8.8.8 \
		--network bridge \
		--publish 8080:8080 --publish 50000:50000 \
		--volume $(SOCKET):/var/run/docker.sock \
		--volume $(VOLUME):/var/jenkins_home \
		--restart unless-stopped \
		--name jenkins jenkins-docker

# (re-) starts the Jenkins container
start:
	$(DOCKER) start jenkins

# opens a bash within the Jenkins container
bash:
	$(DOCKER) exec -it jenkins bash

# prints the inital password of a newly setup Jenkins
init-pw:
	$(DOCKER) exec -it jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# disables security for the Jenkins, allows login without credentials
unprotected:
	docker exec -it jenkins sed -i 's|<useSecurity>true</useSecurity>|<useSecurity>false</useSecurity>|' /var/jenkins_home/config.xml
	docker exec -it jenkins grep useSecurity /var/jenkins_home/config.xml

# enables security for the Jenkins, requires login with credentials
protected:
	docker exec -it jenkins sed -i 's|<useSecurity>true</useSecurity>|<useSecurity>true</useSecurity>|' /var/jenkins_home/config.xml
	docker exec -it jenkins grep useSecurity /var/jenkins_home/config.xml

# stops the Jenkins container
stop:
	$(DOCKER) stop jenkins

# removes the Jenkins container
rm: stop
	$(DOCKER) rm jenkins

# purges the Jenkins volume (finally deletes the configuration)
purge: rm
	$(DOCKER) volume rm $(VOLUME)
