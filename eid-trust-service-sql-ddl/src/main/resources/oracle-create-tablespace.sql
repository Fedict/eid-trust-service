-- Oracle script to create the Trust Service tablespace
-- <![CDATA[Usage: sqlplus / as sysdba @oracle-create-tablespace.sql ]]>
prompt drop tablespace TRUST_SERVICE
drop tablespace TRUST_SERVICE including contents and datafiles;
prompt create tablespace TRUST_SERVICE
create tablespace TRUST_SERVICE
   logging
   datafile '/usr/lib/oracle/xe/oradata/XE/XE_trust_data.dbf'
   size 5M autoextend on next 5M maxsize 2048M
   extent management local uniform size 1M
   segment space management auto
  ;
