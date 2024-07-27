# build using:
# docker build -t postgres-with-contrib:15.5-bookworm .

FROM postgres:15.5-bookworm

RUN apt-get update && \
    apt-get install -y postgresql-contrib && \
    apt-get clean

COPY etc/postgresql-log-slow-queries.conf /etc/postgresql/postgresql.conf
