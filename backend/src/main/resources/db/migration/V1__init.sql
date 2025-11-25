CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pid BIGINT NOT NULL UNIQUE,
    secret_key VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS pay_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pid BIGINT NOT NULL,
    platform VARCHAR(64) NOT NULL,
    account VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    state TINYINT NOT NULL DEFAULT 1,
    pattern TINYINT NOT NULL DEFAULT 1,
    params TEXT
);

CREATE TABLE IF NOT EXISTS pay_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    channel VARCHAR(255) NOT NULL,
    qrcode VARCHAR(512),
    last_time VARCHAR(64),
    state TINYINT NOT NULL DEFAULT 1,
    type VARCHAR(64),
    CONSTRAINT fk_channel_account FOREIGN KEY (account_id) REFERENCES pay_account(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(64) NOT NULL UNIQUE,
    pid BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    out_trade_no VARCHAR(128) NOT NULL,
    notify_url VARCHAR(512) NOT NULL,
    return_url VARCHAR(512),
    name VARCHAR(255) NOT NULL,
    money DOUBLE NOT NULL,
    really_price DOUBLE NOT NULL,
    clientip VARCHAR(64),
    device VARCHAR(64),
    param TEXT,
    state TINYINT NOT NULL DEFAULT 0,
    patt TINYINT,
    create_time DATETIME,
    close_time DATETIME,
    pay_time DATETIME,
    aid BIGINT,
    cid BIGINT,
    platform_order VARCHAR(128),
    CONSTRAINT fk_order_account FOREIGN KEY (aid) REFERENCES pay_account(id),
    CONSTRAINT fk_order_channel FOREIGN KEY (cid) REFERENCES pay_channel(id)
);
