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

-- XA DataSource support
GRANT SELECT ON sys.dba_pending_transactions TO trust;
GRANT SELECT ON sys.pending_trans$ TO trust;
GRANT SELECT ON sys.dba_2pc_pending TO trust;
-- for Oracle 10g R2 with patch for bug 5945463 applied and higher:
-- GRANT EXECUTE ON sys.dbms_xa TO trust;
-- else
GRANT EXECUTE ON sys.dbms_system TO trust;
