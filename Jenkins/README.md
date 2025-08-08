# Jenkins Build+Test-Pipeline with NGINX HTTPS-Proxy and Letsencrypt

The scripts work in a Hostsharing Managed Docker environment.

Requires a .env file like this in the current directory: 

```
DOCKER_SOCKET=/var/run/docker.sock
DOCKER_HOST=unix:///var/run/docker.soc
SERVER_NAME=jenkins.example.org
JENKINS_VOLUME=jenkins_home
JENKINS_ADMIN_PASSWORD=password-for-initial-user-admin
GIT_USERNAME=git-username
GIT_PASSWORD=git-password
CERTBOT_ENV=--staging # leave empty for real certificates or --staging for test certificates
```

Then run `make provision` to initialize everything.

To completely start over again, run `make jenkins-purge clean provision`.
This will also remove all Jenkins configurations!

Once everything works, you can remove `--staging` from `.env`
and run `make clean provision`.
Now, a *letsencrypt* is asked to issue a real certificate.
Beware, this is only possible 5 times per 24h.

Run `make help` for more information.
