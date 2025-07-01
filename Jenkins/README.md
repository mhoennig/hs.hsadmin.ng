# Jenkins Build+Test-Pipeline with NGINX HTTPS-Proxy and Letsencrypt

The scripts work in a Hostsharing Managed Docker environment.

Requires a .env file like this in the current directory: 

```
SERVER_NAME=jenkins.example.org
EMAIL=contact@example.org
```

Then run `make provision` to initialize everything.

Run `make help` for more information.

WARNING: Provisioning does not really work yet, needs some manual restarts.
