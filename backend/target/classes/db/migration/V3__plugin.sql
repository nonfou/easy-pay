CREATE TABLE IF NOT EXISTS plugin_definition (
    platform VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    class_name VARCHAR(128) NOT NULL,
    price VARCHAR(32),
    describe_text TEXT,
    website VARCHAR(255),
    state TINYINT NOT NULL DEFAULT 1,
    install TINYINT NOT NULL DEFAULT 1,
    query TEXT
);
