-- PostgreSQL script to init the database user account

-- psql -U postgres < postgresql-create-account.sql
CREATE USER trust WITH PASSWORD 'trust';
CREATE DATABASE trust;
GRANT ALL PRIVILEGES ON DATABASE trust TO trust;
