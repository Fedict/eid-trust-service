-- Account initialisation script for MySQL Trust Service database
-- <![CDATA[Usage: mysql -u root -p < mysql-create-account.sql ]]>
USE mysql;
DELETE FROM user WHERE User = 'trust';
DELETE FROM db WHERE User = 'trust';
INSERT INTO user (Host, User, Password) VALUES ('localhost', 'trust', PASSWORD('trust'));
INSERT INTO user (Host, User, Password) VALUES ('%', 'trust', PASSWORD('trust'));
INSERT INTO db (Host, Db, User, Select_priv, Insert_priv, Update_priv, Delete_priv, Create_priv, Drop_priv, Alter_priv, Index_priv) 
 VALUES ('localhost', 'trust', 'trust', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y');
INSERT INTO db (Host, Db, User, Select_priv, Insert_priv, Update_priv, Delete_priv, Create_priv, Drop_priv, Alter_priv, Index_priv) 
 VALUES ('%', 'trust', 'trust', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y');
FLUSH PRIVILEGES;