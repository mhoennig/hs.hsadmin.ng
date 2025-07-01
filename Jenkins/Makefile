include .env
export

SOCKET := /var/run/docker.sock
VOLUME := jenkins_home

CERTBOT_CONF := $(PWD)/.generated/certbot/lib/conf
CERTBOT_WWW := $(PWD)/.generated/certbot/lib/www
CERTBOT_LOG := $(PWD)/.generated/certbot/log
NGINX_LOG := $(PWD)/.generated/certbot/nginx/log

.PHONY: provision \
	build run bash init-pw unprotected protected start stop rm purge \
	nginx-prepare nginx-proxy nginx-start nginx-letsencrypt-init nginx-letsencrypt-timer nginx-restart nginx-stop

## lists all documented targets
help:
	@awk '/^##/ {sub(/^## /, "", $$0); desc=$$0; next} /^[a-zA-Z0-9][^:]*:/ { \
		print "\033[1m" $$1 "\033[0m"; \
		print "    " desc "\n" \
	}' $(MAKEFILE_LIST)

## initially, run this once to provision te nginx
provision: nginx-prepare nginx-letsencrypt-init nginx-letsencrypt-timer nginx-start build start

## removes all generated files
clean: nginx-stop stop
	rm -rf .generated/

## builds the Jenkins image
build:
	docker build -t jenkins-docker .

## manually running the Jenkins container
run:
	docker run --detach \
		--dns 8.8.8.8 \
		--network bridge \
		--publish 8090:8080 --publish 50000:50000 \
		--volume $(SOCKET):/var/run/docker.sock \
		--volume $(VOLUME):/var/jenkins_home \
		--restart unless-stopped \
		--name jenkins jenkins-docker

## manually starts the Jenkins container (again)
start:
	docker start jenkins

## opens a bash within the Jenkins container
bash:
	docker exec -it jenkins bash

## prints the initial password of a newly setup Jenkins
init-pw:
	docker exec -it jenkins sh -c '\
    		while [ ! -f /var/jenkins_home/secrets/initialAdminPassword ]; do \
    			sleep 1; \
    		done; \
    		cat /var/jenkins_home/secrets/initialAdminPassword \
    	'

## disables security for the Jenkins => allows login to Jenkins without credentials
unprotected:
	docker exec -it jenkins sed -i 's|<useSecurity>true</useSecurity>|<useSecurity>false</useSecurity>|' /var/jenkins_home/config.xml
	docker exec -it jenkins grep useSecurity /var/jenkins_home/config.xml

## enables security for the Jenkins => Jenkins requires login with credentials
protected:
	docker exec -it jenkins sed -i 's|<useSecurity>true</useSecurity>|<useSecurity>true</useSecurity>|' /var/jenkins_home/config.xml
	docker exec -it jenkins grep useSecurity /var/jenkins_home/config.xml

## stops the Jenkins container
stop:
	docker stop jenkins

## removes the Jenkins container
rm: stop
	docker rm jenkins

## purges the Jenkins volume (finally deletes the configuration)
purge: rm
	docker volume rm $(VOLUME)

# (internal) generates the files for nginx-proxy and certbot
nginx-prepare:
	mkdir -p $(CERTBOT_WWW) $(CERTBOT_LOG) $(CERTBOT_CONF)/live/$(SERVER_NAME) $(NGINX_LOG)
	chmod 755 $(CERTBOT_WWW) $(CERTBOT_LOG) $(CERTBOT_CONF)/live/$(SERVER_NAME) $(NGINX_LOG)
	sed -e 's/%SERVER_NAME/$(SERVER_NAME)/g' <nginx-proxy/nginx.conf >.generated/nginx.conf
	cp nginx-proxy/options-ssl-nginx.conf $(CERTBOT_CONF)/options-ssl-nginx.conf
	chmod 644 $(CERTBOT_CONF)/options-ssl-nginx.conf
	test -f $(CERTBOT_CONF)/ssl-dhparams.pem || curl -o $(CERTBOT_CONF)/ssl-dhparams.pem \
		https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem
	chmod 644 $(CERTBOT_CONF)/ssl-dhparams.pem
	openssl req -x509 -nodes -newkey rsa:2048 \
	  -keyout $(CERTBOT_CONF)/live/$(SERVER_NAME)/privkey.pem \
	  -out /$(CERTBOT_CONF)/live/$(SERVER_NAME)/fullchain.pem \
	  -subj "/CN=dummy"

## opens a bash within the Nginx-proxy container
nginx-bash:
	docker exec -it nginx bash

# (internal) fetches an initial certificate from letsencrypt
nginx-letsencrypt-init: nginx-start
	# wait for nginx actually running (could be improved)
	@sleep 5
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
	  certonly --webroot --webroot-path /var/www/certbot \
	  --email $(EMAIL) --cert-name $(SERVER_NAME) \
	  -d $(SERVER_NAME) --rsa-key-size 4096 \
	  --agree-tos --force-renewal
	# restart nginx
	docker stop nginx || true
	docker start nginx

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

## starts the nginx proxy server
nginx-start: nginx-stop
	docker run -d --name nginx \
	  --publish 8080:80 \
	  --publish 8443:443 \
	  --network bridge \
	  -v $(CERTBOT_CONF):/etc/letsencrypt \
	  -v $(CERTBOT_WWW):/var/www/certbot \
	  -v $(NGINX_LOG):/var/log/nginx \
	  -v $(PWD)/.generated/nginx.conf:/etc/nginx/nginx.conf \
	  nginx

## restarts the nginx proxy server
nginx-restart: nginx-stop nginx-start

## stops the nginx proxy server
nginx-stop:
	docker stop nginx || true
	docker rm nginx || true

