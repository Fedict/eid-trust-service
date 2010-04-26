-- Account initialisation script for Oracle Trust Service tablespace
-- <![CDATA[Usage: sqlplus / as sysdba @oracle-create-account.sql ]]>
drop user trust cascade;
create user trust identified by trust
default tablespace TRUST_SERVICE
temporary tablespace temp
quota unlimited on TRUST_SERVICE
;
grant connect to trust;
grant create sequence to trust;
grant create view to trust;
grant alter session to trust;
grant create table to trust;
