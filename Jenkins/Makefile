include .env
export

CERTBOT_CONF := $(PWD)/.generated/certbot/lib/conf
CERTBOT_WWW := $(PWD)/.generated/certbot/lib/www
CERTBOT_LOG := $(PWD)/.generated/certbot/log
NGINX_LOG := $(PWD)/.generated/certbot/nginx/log

.PHONY: provision clean \
	jenkins-build jenkins-run jenkins-bash jenkins-init-pw jenkins-unprotected jenkins-protected jenkins-start jenkins-stop jenkins-rm jenkins-purge \
	nginx-prepare nginx-proxy nginx-run nginx-start nginx-letsencrypt-init nginx-letsencrypt-timer nginx-restart nginx-stop \
	jenkins-security

## lists all documented targets
help:
	@awk '/^##/ {sub(/^## /, "", $$0); desc=$$0; next} /^[a-zA-Z0-9][^:]*:/ { \
		print "\033[1m" $$1 "\033[0m"; \
		print "    " desc "\n" \
	}' $(MAKEFILE_LIST)

## uploads to hs.hsadmin.ng/Jenkins/ on the server for testing purposes
upload:
	scp -r * .env .gitignore tallyman@$(SERVER_NAME):hs.hsadmin.ng/Jenkins/


## initially, run this once to provision te nginx
provision: nginx-prepare nginx-letsencrypt-init nginx-letsencrypt-timer jenkins-build jenkins-run nginx-restart
	@echo "now you can start nginx: make nginx-start"

## removes all generated files
clean: nginx-stop jenkins-rm
	rm -rf .generated/

## builds the Jenkins image
jenkins-build:
	docker build -t jenkins-docker .

# initially runs the Jenkins container during provisioning, later use `make jenkins-start`
jenkins-run:
	$(eval DOCKER_SOCKET_MOUNT := $(if $(DOCKER_SOCKET),$(DOCKER_SOCKET):/var/run/docker.sock,/dev/null:/var/run/docker.no-socket))
	docker run --detach \
		--dns 8.8.8.8 \
		--network bridge \
		--publish 8090:8080 --publish 50000:50000 \
		--volume $(DOCKER_SOCKET_MOUNT) \
		--volume $(JENKINS_VOLUME):/var/jenkins_home \
		--volume $(PWD)/jenkins.yaml:/var/jenkins_home/jenkins.yaml \
		--restart unless-stopped \
		--env-file .env \
		--name jenkins jenkins-docker

## manually starts the Jenkins container (again)
jenkins-start:
	docker start jenkins

## opens a bash within the Jenkins container
jenkins-bash:
	docker exec -it jenkins bash

## prints the Jenkins log
jenkins-log:
	docker logs jenkins 2>&1

## prints the initial password of a newly setup Jenkins
jenkins-init-pw:
	docker exec -it jenkins sh -c '\
    		while [ ! -f /var/jenkins_home/secrets/initialAdminPassword ]; do \
    			sleep 1; \
    		done; \
    		cat /var/jenkins_home/secrets/initialAdminPassword \
    	'

## disables security for the Jenkins => allows login to Jenkins without credentials
jenkins-unprotected:
	docker exec -it jenkins sed -i 's|<useSecurity>true</useSecurity>|<useSecurity>false</useSecurity>|' /var/jenkins_home/config.xml
	docker exec -it jenkins grep useSecurity /var/jenkins_home/config.xml

## enables security for the Jenkins => Jenkins requires login with credentials
jenkins-protected:
	docker exec -it jenkins sed -i 's|<useSecurity>true</useSecurity>|<useSecurity>true</useSecurity>|' /var/jenkins_home/config.xml
	docker exec -it jenkins grep useSecurity /var/jenkins_home/config.xml

## stops the Jenkins container
jenkins-stop:
	docker stop jenkins || true

## removes the Jenkins container
jenkins-rm: jenkins-stop
	docker rm jenkins || true

## purges the Jenkins volume (finally deletes the configuration)
jenkins-purge: jenkins-rm
	docker volume rm $(JENKINS_VOLUME) || true

# (internal) generates the files for nginx-proxy and certbot
nginx-prepare:
	mkdir -p $(CERTBOT_WWW) $(CERTBOT_LOG) $(CERTBOT_CONF)/live/$(SERVER_NAME) $(NGINX_LOG)
	chmod 755 $(CERTBOT_WWW) $(CERTBOT_LOG) $(CERTBOT_CONF)/live/$(SERVER_NAME) $(NGINX_LOG)
	sed -e 's/%SERVER_NAME/$(SERVER_NAME)/g' <nginx-proxy/nginx-init.conf >.generated/nginx.conf
	cp nginx-proxy/options-ssl-nginx.conf $(CERTBOT_CONF)/options-ssl-nginx.conf
	chmod 644 $(CERTBOT_CONF)/options-ssl-nginx.conf
	test -f $(CERTBOT_CONF)/ssl-dhparams.pem || curl -o $(CERTBOT_CONF)/ssl-dhparams.pem \
		https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem
	chmod 644 $(CERTBOT_CONF)/ssl-dhparams.pem

## opens a bash within the Nginx-proxy container
nginx-bash:
	docker exec -it nginx bash

# (internal) fetches an initial certificate from letsencrypt
nginx-letsencrypt-init: nginx-run
	# delete the previous (dummy) config to avoid file creation with suffix -0001 etc.
	rm -rf $(CERTBOT_CONF)/etc/letsencrypt/live/$(SERVER_NAME) \
       $(CERTBOT_CONF)/etc/letsencrypt/archive/$(SERVER_NAME) \
       $(CERTBOT_CONF)/etc/letsencrypt/renewal/$(SERVER_NAME).conf
    # request the certificate via letsencrypt
	docker run --rm \
	  -v $(CERTBOT_CONF):/etc/letsencrypt \
	  -v $(CERTBOT_WWW):/var/www/certbot \
	  -v $(CERTBOT_LOG):/var/log/letsencrypt \
	  certbot/certbot \
	  certonly --webroot --webroot-path /var/www/certbot --cert-name $(SERVER_NAME) \
	  -d $(SERVER_NAME) --rsa-key-size 4096 \
	  --non-interactive --agree-tos --force-renewal $(CERTBOT_ENV)
	# from now on, start nginx including https
	sed -e 's/%SERVER_NAME/$(SERVER_NAME)/g' <nginx-proxy/nginx.conf >.generated/nginx.conf
	docker stop nginx || true

## opens a shell in the letsencrypt certbot
nginx-letsencrypt-sh:
	docker run -it --rm \
	  -v $(CERTBOT_CONF):/etc/letsencrypt \
	  -v $(CERTBOT_WWW):/var/www/certbot \
	  -v $(CERTBOT_LOG):/var/log/letsencrypt \
	  --entrypoint /bin/sh \
	  certbot/certbot

# (internal) installs the letsencrypt certbot timer for automatic renewal
nginx-letsencrypt-timer:
	@mkdir -p $(HOME)/.config/systemd/user
	@cp nginx-proxy/nginx-letsencrypt-renew.timer $(HOME)/.config/systemd/user/nginx-letsencrypt-renew.timer
	@cp nginx-proxy/nginx-letsencrypt-renew.service $(HOME)/.config/systemd/user/nginx-letsencrypt-renew.service
	systemctl --user daemon-reload
	systemctl --user enable --now nginx-letsencrypt-renew.timer

## renews the cert, if already renewable - this is also called from the timer
nginx-letsencrypt-renew:
	docker run --rm \
	  -v $(CERTBOT_CONF):/etc/letsencrypt \
	  -v $(CERTBOT_WWW):/var/www/certbot \
	  -v $(CERTBOT_LOG):/var/log/letsencrypt \
	  certbot/certbot renew -q

## initially runs the nginx proxy server
nginx-run: nginx-stop
	docker run -d --name nginx \
	  --publish 8080:80 \
	  --publish 8443:443 \
	  --network bridge \
	  -v $(CERTBOT_CONF):/etc/letsencrypt \
	  -v $(CERTBOT_WWW):/var/www/certbot \
	  -v $(NGINX_LOG):/var/log/nginx \
	  -v $(PWD)/.generated/nginx.conf:/etc/nginx/nginx.conf \
	  --health-cmd="curl -kfs https://localhost:8443/ || exit 1" \
	  --health-interval=5s \
	  --health-timeout=3s \
	  --health-retries=3 \
	  nginx

## starts the nginx proxy server again
nginx-start:
	docker start nginx

## restarts the nginx proxy server
nginx-restart: nginx-stop nginx-start

## stops the nginx proxy server
nginx-stop:
	docker stop nginx || true
	docker rm nginx || true

## remove the nginx container
nginx-rm: nginx-stop
	docker rm nginx || true

## check security status
jenkins-security:
	@curl --insecure -s -o /dev/null -w "%{http_code}\n" https://localhost:8443/script

## fix access rights in workspaces
jenkins-fix:
	@docker run --rm -it -v $(JENKINS_VOLUME):/mnt alpine chown 1000:1000 -R /mnt/workspace

