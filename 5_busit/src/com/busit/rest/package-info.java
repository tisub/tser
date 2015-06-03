/**
 * COMPLETE database setup:
 * <h1>Base</h1>
 * <pre>

CREATE TABLE users (
	user_id			BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	user_name		VARCHAR(100) CHARSET UTF8 NOT NULL,
	user_password	VARCHAR(32) CHARSET UTF8 NOT NULL,
	user_ip			TINYTEXT CHARSET UTF8,
	user_firstname	TINYTEXT CHARSET UTF8,
	user_lastname	TINYTEXT CHARSET UTF8,
	user_mail		TINYTEXT CHARSET UTF8,
	user_date		INT(11) UNSIGNED NOT NULL,
	user_lang		VARCHAR(4) CHARSET UTF8 NOT NULL DEFAULT 'EN',
	user_org		BOOLEAN NOT NULL DEFAULT '0',
	user_origin		INT(11) UNSIGNED NOT NULL DEFAULT '0',
	user_status		BOOLEAN NOT NULL DEFAULT '1',
	user_setup		BOOLEAN NOT NULL DEFAULT '0',
	user_address	TEXT CHARSET UTF8,
	user_public		BOOLEAN NOT NULL DEFAULT '1',
	PRIMARY KEY (user_id),
	UNIQUE (user_name)
) ENGINE=INNODB;

CREATE TABLE tokens (
	token_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	token_value		TINYTEXT CHARSET UTF8 NOT NULL,
	token_lease		INT(11) UNSIGNED NOT NULL,
	token_name		TINYTEXT CHARSET UTF8,
	token_user		BIGINT(20) UNSIGNED NOT NULL,
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

CREATE TABLE quotas (
	quota_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	quota_name		VARCHAR(100) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (quota_id),
	UNIQUE (quota_name)
) ENGINE=INNODB;

CREATE TABLE confirm (
	confirm_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	confirm_email	VARCHAR(250) CHARSET UTF8 NOT NULL,
	confirm_code	VARCHAR(42) CHARSET UTF8 NOT NULL,
	confirm_date	INT(11) UNSIGNED NOT NULL,
	confirm_user	BIGINT(20) DEFAULT NULL,
	PRIMARY KEY (confirm_id),
	FOREIGN KEY (confirm_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE user_group (
	user_id			BIGINT(20) UNSIGNED NOT NULL,
	group_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (user_id, group_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (group_id) REFERENCES groups (group_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE user_grant (
	user_id			BIGINT(20) UNSIGNED NOT NULL,
	grant_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (user_id, grant_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE user_quota (
	user_id			BIGINT(20) UNSIGNED NOT NULL,
	quota_id		INT(11) UNSIGNED NOT NULL,
	quota_used		INT(11) NOT NULL DEFAULT '0',
	quota_max		INT(11) NOT NULL DEFAULT '0',
	quota_min		INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (user_id, quota_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (quota_id) REFERENCES quotas (quota_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE token_grant (
	token_id		INT(11) UNSIGNED NOT NULL,
	grant_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (token_id, grant_id),
	FOREIGN KEY (token_id) REFERENCES tokens (token_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE group_grant (
	group_id		INT(11) UNSIGNED NOT NULL,
	grant_id		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (group_id,grant_id),
	FOREIGN KEY (group_id) REFERENCES groups (group_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

 * </pre>
 *
 * <h1>Identities</h1>
 * <pre>

CREATE TABLE identities (
	identity_id				BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	identity_description	TEXT CHARSET UTF8,
	identity_key_public		BLOB NOT NULL,
	identity_key_private	BLOB NOT NULL,
	identity_key_expire		INT(11) UNSIGNED NOT NULL,
	identity_principal		VARCHAR(250) CHARSET UTF8 NOT NULL,
	identity_birth			INT(11) UNSIGNED NOT NULL,
	identity_death			INT(11) UNSIGNED DEFAULT NULL,
	identity_user			BIGINT(20) UNSIGNED DEFAULT NULL,
	PRIMARY KEY (identity_id),
	UNIQUE (identity_principal),
	FOREIGN KEY (identity_user) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=INNODB;

CREATE TABLE impersonate (
	identity_to		BIGINT(20) UNSIGNED NOT NULL,
	identity_from	BIGINT(20) UNSIGNED NOT NULL,
	PRIMARY KEY (identity_from, identity_to),
	FOREIGN KEY (identity_to) REFERENCES identities (identity_id) ON DELETE CASCADE,
	FOREIGN KEY (identity_from) REFERENCES identities (identity_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE org_member (
	org_id			BIGINT(20) UNSIGNED NOT NULL,
	identity_id		BIGINT(20) UNSIGNED NOT NULL,
	org_admin		BOOLEAN NOT NULL DEFAULT '0',
	PRIMARY KEY (org_id, identity_id),
	FOREIGN KEY (org_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (identity_id) REFERENCES identities (identity_id) ON DELETE CASCADE
) ENGINE=INNODB;

 * </pre>
 *
 * <h1>Translations</h1>
 * <pre>
 
CREATE TABLE translations (
	translation_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	translation_text	TEXT CHARSET UTF8 DEFAULT NULL,
	PRIMARY KEY (translation_id)
) ENGINE=INNODB;
 
 * </pre>
 *
 * <h1>Connectors</h1>
 * <pre>

CREATE TABLE categories (
	category_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	category_name	VARCHAR(30) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (category_id),
	UNIQUE (category_name)
) ENGINE=INNODB;

CREATE TABLE status (
	status_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	status_name		VARCHAR(30) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (status_id),
	UNIQUE (status_name)
) ENGINE=INNODB;

CREATE TABLE tags (
	tag_id			INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	tag_name		VARCHAR(30) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (tag_id),
	UNIQUE (tag_name)
) ENGINE=INNODB;

CREATE TABLE connectors (
	connector_id			BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	connector_user			BIGINT(20) UNSIGNED DEFAULT NULL,
	connector_category		INT(11) UNSIGNED DEFAULT NULL,
	connector_name			VARCHAR(100) CHARSET UTF8 NOT NULL,
	connector_buy_price		INT(11) UNSIGNED NOT NULL DEFAULT '0',
	connector_use_price		INT(11) UNSIGNED NOT NULL DEFAULT '0',
	connector_buy_tax		INT(11) UNSIGNED NOT NULL DEFAULT '0',
	connector_use_tax		INT(11) UNSIGNED NOT NULL DEFAULT '0',
	connector_date			INT(11) UNSIGNED NOT NULL DEFAULT '0',
	connector_user_status	INT(11) UNSIGNED DEFAULT '1',
	connector_intern_status	INT(11) UNSIGNED DEFAULT '2',
	connector_language		BOOLEAN NOT NULL DEFAULT '1',
	connector_rating		FLOAT UNSIGNED NOT NULL DEFAULT '0',
	connector_instances		BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
	PRIMARY KEY (connector_id),
	UNIQUE KEY connector_name (connector_name),
	FOREIGN KEY (connector_user) REFERENCES users (user_id) ON DELETE SET NULL,
	FOREIGN KEY (connector_category) REFERENCES categories (category_id) ON DELETE SET NULL,
	FOREIGN KEY (connector_user_status) REFERENCES status (status_id) ON DELETE SET NULL,
	FOREIGN KEY (connector_intern_status) REFERENCES status (status_id) ON DELETE SET NULL
) ENGINE=INNODB;

CREATE TABLE connector_config (
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	config_key		VARCHAR(100) CHARSET UTF8 NOT NULL,
	config_value	LONGTEXT CHARSET UTF8 DEFAULT NULL,
	config_hidden	BOOLEAN NOT NULL DEFAULT FALSE,
	PRIMARY KEY (connector_id, config_key),
	FOREIGN KEY (connector_id) REFERENCES connectors (connector_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE config_translation (
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	config_key		VARCHAR(100) CHARSET UTF8 NOT NULL,
	translation_id	BIGINT(20) UNSIGNED NOT NULL,
	translation_language VARCHAR(2) CHARSET UTF8 NOT NULL DEFAULT 'EN',
	PRIMARY KEY (connector_id, config_key, translation_id),
	FOREIGN KEY (connector_id, config_key) REFERENCES connector_config (connector_id, config_key) ON DELETE CASCADE,
	FOREIGN KEY (translation_id) REFERENCES translations (translation_id) ON DELETE CASCADE,
	UNIQUE (connector_id, config_key, translation_language)
) ENGINE=INNODB;

CREATE TABLE connector_interface (
	connector_id		BIGINT(20) UNSIGNED NOT NULL,
	interface_key		VARCHAR(50) CHARSET UTF8 NOT NULL,
	interface_type		TINYINT(2) NOT NULL,
	interface_dynamic	BOOLEAN NOT NULL DEFAULT FALSE,
	interface_cron		BOOLEAN NOT NULL DEFAULT FALSE,
	interface_wkt		TEXT CHARSET UTF8 DEFAULT NULL,
	PRIMARY KEY (connector_id, interface_key),
	FOREIGN KEY (connector_id) REFERENCES connectors (connector_id) ON DELETE CASCADE,
) ENGINE=INNODB;

CREATE TABLE interface_translation (
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	interface_key	VARCHAR(50) CHARSET UTF8 NOT NULL,
	translation_id	BIGINT(20) UNSIGNED NOT NULL,
	translation_language VARCHAR(2) CHARSET UTF8 NOT NULL DEFAULT 'EN',
	PRIMARY KEY (connector_id, interface_key, translation_id),
	FOREIGN KEY (connector_id, interface_key) REFERENCES connector_interface (connector_id, interface_key) ON DELETE CASCADE,
	FOREIGN KEY (translation_id) REFERENCES translations (translation_id) ON DELETE CASCADE,
	UNIQUE (connector_id, interface_key, translation_language)
) ENGINE=INNODB;

CREATE TABLE connector_tag (
	tag_id			INT(11) UNSIGNED NOT NULL,
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	PRIMARY KEY (tag_id, connector_id),
	FOREIGN KEY (tag_id) REFERENCES tags (tag_id) ON DELETE CASCADE,
	FOREIGN KEY (connector_id) REFERENCES connectors (connector_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE connector_file (
	file_id			BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	file_date		INT(11) NOT NULL,
	file_content	LONGBLOB NOT NULL,
	PRIMARY KEY (file_id),
	FOREIGN KEY (connector_id) REFERENCES connectors (connector_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE connector_translation (
	connector_id			BIGINT(20) UNSIGNED NOT NULL,
	translation_id			BIGINT(20) UNSIGNED NOT NULL,
	translation_language 	VARCHAR(2) CHARSET UTF8 NOT NULL DEFAULT 'EN',
	PRIMARY KEY (connector_id, translation_id),
	FOREIGN KEY (connector_id) REFERENCES connectors (connector_id) ON DELETE CASCADE,
	FOREIGN KEY (translation_id) REFERENCES translations (translation_id) ON DELETE CASCADE,
	UNIQUE (connector_id, translation_language)
) ENGINE=INNODB;

CREATE TABLE user_rating (
	user_id			BIGINT(20) UNSIGNED NOT NULL,
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	rating_value	INT(2) NOT NULL,
	PRIMARY KEY (user_id, connector_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (connector_id) REFERENCES connectors (connector_id) ON DELETE CASCADE
) ENGINE=INNODB;

 * </pre>
 *
 * <h1>Instances</h1>
 * <pre>

CREATE TABLE spaces (
	space_id			BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	space_user			BIGINT(20) UNSIGNED NOT NULL,
	space_description	TEXT CHARSET UTF8,
	space_date			INT(11) UNSIGNED NOT NULL,
	space_name			TEXT CHARACTER SET utf8,
	space_active		BOOLEAN NOT NULL DEFAULT '1',
	space_showcase		BOOLEAN NOT NULL DEFAULT '0',
	space_category		INT(11) DEFAULT NULL,
	PRIMARY KEY (space_id),
	FOREIGN KEY (space_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE instances (
	instance_id			BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	instance_connector	BIGINT(20) UNSIGNED NOT NULL,
	instance_user		BIGINT(20) UNSIGNED NOT NULL,
	instance_date		INT(11) UNSIGNED NOT NULL,
	instance_name		VARCHAR(150) CHARSET UTF8,
	instance_active		BOOLEAN NOT NULL DEFAULT '1',
	instance_configured	BOOLEAN NOT NULL DEFAULT '0',
	instance_hits		BIGINT(20) NOT NULL DEFAULT '0'
	PRIMARY KEY (instance_id, instance_connector),
	UNIQUE (instance_user, instance_name),
	FOREIGN KEY (instance_connector) REFERENCES connectors (connector_id) ON DELETE CASCADE,
	FOREIGN KEY (instance_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE instance_space (
	space_id 		BIGINT(20) UNSIGNED NOT,
	instance_id 	BIGINT(20) UNSIGNED NOT,
	PRIMARY KEY (space_id, instance_id),
	FOREIGN KEY (space_id) REFERENCES spaces (space_id) ON DELETE CASCADE,
	FOREIGN KEY (instance_id) REFERENCES instances (instance_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE instance_config (
	instance_id		BIGINT(20) UNSIGNED NOT NULL,
	connector_id	BIGINT(20) UNSIGNED NOT NULL,
	config_key		VARCHAR(100) CHARSET UTF8 NOT NULL,
	config_value	LONGTEXT CHARSET UTF8 DEFAULT NULL,
	PRIMARY KEY (instance_id, config_key),
	FOREIGN KEY (instance_id) REFERENCES instances (instance_id) ON DELETE CASCADE,
	FOREIGN KEY (connector_id, config_key) REFERENCES connector_config (connector_id, config_key) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE instance_interface (
	instance_id			BIGINT(20) UNSIGNED NOT NULL,
	connector_id		BIGINT(20) UNSIGNED NOT NULL,
	interface_key		VARCHAR(50) CHARSET UTF8 NOT NULL,
	interface_name		VARCHAR(200) CHARSET UTF8 NOT NULL,
	interface_dynamic_value		LONGTEXT CHARSET UTF8 DEFAULT NULL,
	interface_cron_timer		VARCHAR(50) CHARSET UTF8 DEFAULT NULL,
	interface_cron_identity		BIGINT(20) UNSIGNED DEFAULT NULL,
	interface_public			BOOLEAN NOT NULL DEFAULT FALSE,
	interface_public_identity	BIGINT(20) UNSIGNED DEFAULT NULL,
	interface_shared			BOOLEAN NOT NULL DEFAULT FALSE,
	interface_share_tax			INT(11) NOT NULL DEFAULT 0,
	interface_share_description	TEXT CHARSET UTF8 DEFAULT NULL,
	interface_share_identity	BIGINT(20) UNSIGNED DEFAULT NULL,
	FOREIGN KEY (instance_id, connector_id) REFERENCES instances (instance_id, instance_connector) ON DELETE CASCADE,
	FOREIGN KEY (connector_id, interface_key) REFERENCES connector_interface (connector_id, interface_key) ON DELETE CASCADE,
	FOREIGN KEY (interface_cron_identity) REFERENCES identities (identity_id) ON DELETE SET NULL,
	FOREIGN KEY (interface_public_identity) REFERENCES identities (identity_id) ON DELETE SET NULL,
	FOREIGN KEY (interface_share_identity) REFERENCES identities (identity_id) ON DELETE SET NULL,
	UNIQUE (instance_id, interface_name)
) ENGINE=INNODB;

CREATE TABLE links (
	link_id				BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	instance_from		BIGINT(20) UNSIGNED NOT NULL,
	interface_from		VARCHAR(200) CHARSET UTF8 NOT NULL,
	instance_to			BIGINT(20) UNSIGNED NOT NULL,
	interface_to		VARCHAR(200) CHARSET UTF8 NOT NULL,
	link_active			BOOLEAN NOT NULL DEFAULT TRUE,
	PRIMARY KEY (link_id),
	FOREIGN KEY (instance_from, interface_from) REFERENCES instance_interface (instance_id, interface_name) ON DELETE CASCADE,
	FOREIGN KEY (instance_to, interface_to) REFERENCES instance_interface (instance_id, interface_name) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE link_space (
	link_id 	BIGINT(20) UNSIGNED NOT NULL,
	space_id 	BIGINT(20) UNSIGNED NOT NULL,
	PRIMARY KEY(link_id, space_id),
	FOREIGN KEY (link_id) REFERENCES links (link_id) ON DELETE CASCADE,
	FOREIGN KEY (space_id) REFERENCES spaces (space_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE instance_error (
	error_id			VARCHAR(40) CHARSET UTF8 NOT NULL,
	error_instance		BIGINT(20) UNSIGNED NOT NULL,
	error_interface		VARCHAR(150) CHARSET UTF8 NOT NULL,
	error_date			INT(11) UNSIGNED NOT NULL,
	error_code			INT(2) UNSIGNED NOT NULL DEFAULT '0',
	error_message		TEXT CHARSET UTF8 NOT NULL,
	error_ack			BOOLEAN NOT NULL DEFAULT '0',
	error_read			BOOLEAN NOT NULL DEFAULT '0',
	PRIMARY KEY (error_id),
	FOREIGN KEY (error_instance) REFERENCES instances (instance_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE instance_log (
	log_id				VARCHAR(40) CHARSET UTF8 NOT NULL,
	log_instance		BIGINT(20) UNSIGNED NOT NULL,
	log_interface		VARCHAR(150) CHARSET UTF8 DEFAULT NULL,
	log_date			INT(11) UNSIGNED NOT NULL,
	log_size			INT(11) UNSIGNED NOT NULL,
	log_cost			INT(11) UNSIGNED NOT NULL,
	log_hash			VARCHAR(32) CHARSET UTF8 NOT NULL,
	log_ack				BOOLEAN NOT NULL DEFAULT '0',
	log_read			BOOLEAN NOT NULL DEFAULT '0',
	INDEX (log_id),
	FOREIGN KEY (log_instance) REFERENCES instances (instance_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE user_shared (
	instance_id			BIGINT(20) UNSIGNED NOT NULL,
	interface_name		VARCHAR(200) CHARSET UTF8 NOT NULL,
	space_id			BIGINT(20) UNSIGNED NOT NULL,
	instance_position	TEXT CHARSET UTF8,
	PRIMARY KEY (instance_id, space_id),
	FOREIGN KEY (instance_id, interface_key) REFERENCES instance_interface (instance_id, interface_name) ON DELETE CASCADE,
	FOREIGN KEY (space_id) REFERENCES spaces (space_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE delegates (
	delegate_id			INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	delegate_value		TINYTEXT CHARACTER SET utf8 NOT NULL,
	delegate_lease		INT(11) UNSIGNED NOT NULL,
	delegate_name		TINYTEXT CHARACTER SET utf8,
	delegate_identity	BIGINT(20) UNSIGNED NOT NULL,
	delegate_instance	BIGINT(20) UNSIGNED DEFAULT NULL,
	delegate_space		BIGINT(20) UNSIGNED DEFAULT NULL,
	PRIMARY KEY (delegate_id),
	FOREIGN KEY (delegate_identity) REFERENCES identities (identity_id) ON DELETE CASCADE,
	FOREIGN KEY (delegate_instance) REFERENCES instances (instance_id) ON DELETE CASCADE,
	FOREIGN KEY (delegate_space) REFERENCES spaces (space_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE delegate_grant (
	delegate_id			INT(11) UNSIGNED NOT NULL,
	grant_id			INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (delegate_id, grant_id),
	FOREIGN KEY (delegate_id) REFERENCES delegates (delegate_id) ON DELETE CASCADE,
	FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;

 * </pre>
 *
 * <h1>Accounting</h1>
 * <pre>

CREATE TABLE bills (
	bill_id			INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	bill_real_id	INT(11) DEFAULT NULL,
	bill_user		BIGINT(20) UNSIGNED,
	bill_name		VARCHAR(64) CHARSET UTF8 NOT NULL DEFAULT '0',
	bill_ref		VARCHAR(32) CHARSET UTF8 NOT NULL DEFAULT '0',
	bill_date		INT(11) NOT NULL DEFAULT '0',
	bill_status		BOOLEAN NOT NULL DEFAULT '0',
	bill_amount_et	FLOAT NOT NULL DEFAULT '0',
	bill_amount_ati	FLOAT NOT NULL DEFAULT '0',
	PRIMARY KEY (bill_id),
	FOREIGN KEY (bill_user) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=INNODB;

CREATE TABLE bill_line (
	line_id				BIGINT(20) NOT NULL AUTO_INCREMENT,
	line_bill			INT(11) UNSIGNED NOT NULL,
	line_name			VARCHAR(100) CHARSET UTF8 NOT NULL,
	line_credits		INT(11) NOT NULL DEFAULT '0',
	line_description	TEXT CHARSET UTF8 NOT NULL,
	line_amount_et		FLOAT NOT NULL,
	line_vat			FLOAT NOT NULL,
	line_amount_ati		FLOAT NOT NULL,
	PRIMARY KEY (line_id),
	FOREIGN KEY (line_bill) REFERENCES bills (bill_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE credit_plan (
	plan_user		BIGINT(20) UNSIGNED NOT NULL,
	plan_window		INT(11) UNSIGNED NOT NULL DEFAULT '3600',
	plan_factor		INT(11) UNSIGNED NOT NULL DEFAULT '1',
	plan_root		INT(11) UNSIGNED NOT NULL DEFAULT '2',
	plan_free		INT(11) UNSIGNED NOT NULL DEFAULT '500',
	PRIMARY KEY (plan_user),
	FOREIGN KEY (plan_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE credit_sliding (
	sliding_user	BIGINT(20) UNSIGNED NOT NULL,
	sliding_time	INT(11) UNSIGNED NOT NULL,
	sliding_cost	INT(11) UNSIGNED NOT NULL DEFAULT '0',
	FOREIGN KEY (sliding_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE credit_transaction (
	transaction_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	transaction_from	BIGINT(20) UNSIGNED NOT NULL,
	transaction_to		BIGINT(20) UNSIGNED DEFAULT NULL,
	transaction_price	INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_tax		INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_count	INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_size	INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_qos		INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_date	INT(11) UNSIGNED NOT NULL,
	transaction_ack		BOOLEAN NOT NULL DEFAULT FALSE,
	transaction_type	INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_data	TEXT CHARSET UTF8 DEFAULT NULL,
	PRIMARY KEY (transaction_id),
	FOREIGN KEY (transaction_from) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (transaction_to) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE transaction_history (
	transaction_from		BIGINT(20) UNSIGNED NOT NULL,
	transaction_to			BIGINT(20) UNSIGNED NOT NULL,
	transaction_amount		INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_date		INT(11) UNSIGNED NOT NULL,
	transaction_type		INT(11) UNSIGNED NOT NULL DEFAULT 0,
	transaction_data		TEXT CHARSET UTF8 DEFAULT NULL,
	FOREIGN KEY (transaction_from) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (transaction_to) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE vouchers (
	voucher_id			BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	voucher_credits		INT(11) UNSIGNED NOT NULL DEFAULT 1000,
	voucher_code		VARCHAR(32) CHARSET UTF8 NOT NULL,
	voucher_date_from	INT(11) UNSIGNED DEFAULT NULL,
	voucher_date_to		INT(11) UNSIGNED DEFAULT NULL,
	voucher_user		BIGINT(11) UNSIGNED DEFAULT NULL,
	voucher_quantity	INT(11) UNSIGNED DEFAULT 1,
	voucher_name		TINYTEXT CHARSET UTF8 NOT NULL,
	voucher_internal	TEXT CHARSET UTF8 DEFAULT NULL,
	PRIMARY KEY (voucher_id),
	UNIQUE (voucher_code)
) ENGINE=INNODB;

CREATE TABLE voucher_activated (
	voucher_id			BIGINT(20) UNSIGNED NOT NULL,
	user_id				BIGINT(20) UNSIGNED NOT NULL,
	activated_date		INT(11) UNSIGNED NOT NULL,
	PRIMARY KEY (voucher_id, user_id),
	FOREIGN KEY (voucher_id) REFERENCES vouchers (voucher_id) ON DELETE CASCADE,
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

 * </pre>
 *
 * <h1>Misc</h1>
 * <pre>

CREATE TABLE mails (
	mail_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	mail_user	BIGINT(20) UNSIGNED NOT NULL,
	mail_date	INT(11) NOT NULL,
	mail_to		VARCHAR(200) CHARSET UTF8 NOT NULL,
	mail_text	TEXT CHARSET UTF8 NOT NULL,
	mail_ack	BOOLEAN NOT NULL DEFAULT '0',
	PRIMARY KEY (mail_id),
	FOREIGN KEY (mail_user) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE news (
	news_id				INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	news_title			TEXT CHARSET UTF8 NOT NULL,
	news_description	TEXT CHARSET UTF8 NOT NULL,
	news_content		TEXT CHARSET UTF8 NOT NULL,
	news_author			TEXT CHARSET UTF8 NOT NULL,
	news_date			INT(11) NOT NULL,
	news_language		VARCHAR(2) CHARSET UTF8 NOT NULL,
	PRIMARY KEY (news_id)
) ENGINE=INNODB;

CREATE TABLE ui (
	ui_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	ui_key		VARCHAR(50) CHARSET UTF8 DEFAULT NULL,
	ui_data		LONGBLOB DEFAULT NULL,
	ui_user 	BIGINT(20) UNSIGNED NOT NULL,
	PRIMARY KEY (ui_id),
	FOREIGN KEY (ui_user) REFERENCES users (user_id) ON DELETE CASCADE,
	UNIQUE (ui_key, ui_user)
) ENGINE=INNODB;

CREATE TABLE knowntype (
	knowntype_id		BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	knowntype_name		VARCHAR(50) CHARSET UTF8 DEFAULT NULL,
	knowntype_data		LONGBLOB DEFAULT NULL,
	PRIMARY KEY (knowntype_id),
	UNIQUE (knowntype_name)
) ENGINE=INNODB;
 * </pre>
 */
package com.busit.rest;