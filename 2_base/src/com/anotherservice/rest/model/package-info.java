/**
 * This is a sample implempentation base for a REST service.
 * <br />The database schema is as follows :
 * <pre>
CREATE TABLE users (
	user_id			INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	user_name		VARCHAR(100) CHARSET UTF8 NOT NULL,
	user_password	VARCHAR(32) CHARSET UTF8 NOT NULL,
	user_ip			TINYTEXT CHARSET UTF8 DEFAULT NULL,
	user_firstname	TINYTEXT CHARSET UTF8 DEFAULT NULL,
	user_lastname	TINYTEXT CHARSET UTF8 DEFAULT NULL,
	user_mail		TINYTEXT CHARSET UTF8 DEFAULT NULL,
	user_date		INT(11) UNSIGNED NOT NULL,
	user_lang		VARCHAR(4) CHARSET UTF8 NOT NULL DEFAULT 'EN',
	PRIMARY KEY (user_id),
	UNIQUE (user_name),
	UNIQUE (user_mail)
) ENGINE=INNODB, AUTO_INCREMENT=1000;

CREATE TABLE tokens (
	token_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	token_value		TINYTEXT CHARSET UTF8 NOT NULL,
	token_lease		INT(11) UNSIGNED NOT NULL,
	token_name		TINYTEXT CHARSET UTF8 DEFAULT NULL,
	token_user		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (token_id),
	FOREIGN KEY (token_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE groups (
	group_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	group_name		VARCHAR(100) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (group_id),
	UNIQUE (group_name)
) ENGINE=INNODB;

CREATE TABLE grants (
	grant_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	grant_name		VARCHAR(100) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (grant_id),
	UNIQUE (grant_name)
) ENGINE=INNODB;

CREATE TABLE user_group (
	user_id			INT(11) UNSIGNED NOT NULL,
	group_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (user_id, group_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (group_id) REFERENCES groups (group_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE user_grant (
	user_id			INT(11) UNSIGNED NOT NULL,
	grant_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (user_id, grant_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE group_grant (
	group_id		INT(11) UNSIGNED NOT NULL,
	grant_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (group_id, grant_id),
	FOREIGN KEY (group_id) REFERENCES groups (group_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE token_grant (
	token_id		INT(11) UNSIGNED NOT NULL,
	grant_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (token_id, grant_id),
	FOREIGN KEY (token_id) REFERENCES tokens (token_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE quotas (
	quota_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	quota_name		VARCHAR(100) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (quota_id),
	UNIQUE (quota_name)
) ENGINE=INNODB;

CREATE TABLE user_quota (
	user_id			INT(11) UNSIGNED NOT NULL,
	quota_id		INT(11) UNSIGNED NOT NULL,
	quota_used		INT(11) NOT NULL DEFAULT 0,
	quota_max		INT(11) NOT NULL DEFAULT 0,
	quota_min		INT(11) NOT NULL DEFAULT 0,
	PRIMARY KEY (user_id, quota_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (quota_id) REFERENCES quotas (quota_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE confirm (
	confirm_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	confirm_code	VARCHAR(32) CHARSET UTF8 NOT NULL,
	confirm_date	INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (confirm_id)
) ENGINE=INNODB;

INSERT INTO grants (grant_name) VALUES ('SUPERADMIN'), ('ACCESS'), 
('USER_INSERT'), ('USER_SELECT'), ('USER_UPDATE'), ('USER_DELETE'), 
('TOKEN_INSERT'), ('TOKEN_SELECT'), ('TOKEN_UPDATE'), ('TOKEN_DELETE'), 
('GROUP_INSERT'), ('GROUP_SELECT'), ('GROUP_UPDATE'), ('GROUP_DELETE'), 
	('GROUP_USER_INSERT'), ('GROUP_USER_SELECT'), ('GROUP_USER_DELETE'), 
('GRANT_INSERT'), ('GRANT_SELECT'), ('GRANT_UPDATE'), ('GRANT_DELETE'), 
	('GRANT_GROUP_INSERT'), ('GRANT_GROUP_SELECT'), ('GRANT_GROUP_DELETE'), 
	('GRANT_TOKEN_INSERT'), ('GRANT_TOKEN_SELECT'), ('GRANT_TOKEN_DELETE'), 
	('GRANT_USER_INSERT'), ('GRANT_USER_SELECT'), ('GRANT_USER_DELETE'), 
('SELF_ACCESS'), ('SELF_USER_SELECT'), ('SELF_USER_UPDATE'), ('SELF_USER_DELETE'), ('SELF_GRANT_USER_SELECT'), ('SELF_GROUP_USER_SELECT'), ('SELF_GROUP_USER_DELETE'), 
	('SELF_TOKEN_INSERT'), ('SELF_TOKEN_SELECT'), ('SELF_TOKEN_UPDATE'), ('SELF_TOKEN_DELETE'), ('SELF_GRANT_TOKEN_DELETE'), ('SELF_GRANT_TOKEN_INSERT'),
('QUOTA_INSERT'), ('QUOTA_SELECT'), ('QUOTA_UPDATE'), ('QUOTA_DELETE'), 
	('QUOTA_USER_INSERT'), ('QUOTA_USER_SELECT'), ('QUOTA_USER_UPDATE'), ('QUOTA_USER_DELETE'), 
('SELF_QUOTA_USER_SELECT'),
('CONFIRM_INSERT'), ('CONFIRM_SELECT'), ('CONFIRM_DELETE');
INSERT INTO groups (group_name) VALUES ('ADMIN_USER'), ('ADMIN_TOKEN'), ('ADMIN_GROUP'), ('ADMIN_GRANT'), ('ADMIN_QUOTA'), ('ADMIN_CONFIRM'), ('USERS');
INSERT INTO users (user_name, user_password, user_date) VALUES ('admin', MD5(UNIX_TIMESTAMP()), UNIX_TIMESTAMP());
INSERT INTO tokens (token_value, token_lease, token_user) VALUES (MD5(UNIX_TIMESTAMP()), 0, (SELECT user_id FROM users where user_name = 'admin'));
INSERT INTO user_grant (user_id, grant_id) SELECT user_id, grant_id FROM users, grants WHERE user_name='admin' AND grant_name='SUPERADMIN';
INSERT INTO token_grant (token_id, grant_id) SELECT token_id, grant_id FROM tokens, grants WHERE grant_name='SUPERADMIN';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='ADMIN_USER' AND grant_name LIKE 'USER%';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='ADMIN_TOKEN' AND grant_name LIKE 'TOKEN%';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='ADMIN_GROUP' AND grant_name LIKE 'GROUP%';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='ADMIN_GRANT' AND grant_name LIKE 'GRANT%';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='ADMIN_QUOTA' AND grant_name LIKE 'QUOTA%';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='ADMIN_CONFIRM' AND grant_name LIKE 'CONFIRM%';
INSERT INTO group_grant (group_id, grant_id) SELECT group_id, grant_id FROM groups, grants WHERE group_name='USERS' AND (grant_name LIKE 'SELF%' OR grant_name='ACCESS');
 * </pre>
 */
package com.anotherservice.rest.model;
