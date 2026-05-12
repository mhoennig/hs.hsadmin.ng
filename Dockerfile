# build using:
# docker build -t postgres-with-contrib:17.7-trixie .

FROM postgres:17.7-trixie

RUN apt-get update && \
    apt-get install -y postgresql-contrib && \
    apt-get clean

COPY etc/postgresql-log-slow-queries.conf /etc/postgresql/postgresql.conf
