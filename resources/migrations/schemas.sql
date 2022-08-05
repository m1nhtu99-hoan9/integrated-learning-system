CREATE TABLE IF NOT EXISTS account_user
(
    account_id     UUID        NOT NULL PRIMARY KEY,
    first_name     VARCHAR(50) NOT NULL,
    last_name      VARCHAR(50) NOT NULL,
    date_of_birth  DATE,
    personal_email VARCHAR(64),
    phone_number   VARCHAR(16),
    UNIQUE (account_id)
);
--;;
CREATE TABLE IF NOT EXISTS student
(
    id         UUID        NOT NULL PRIMARY KEY,
    account_id UUID        NOT NULL REFERENCES account_user (account_id) ON DELETE CASCADE,
    code       VARCHAR(12) NOT NULL,
    UNIQUE (id, code)
);
