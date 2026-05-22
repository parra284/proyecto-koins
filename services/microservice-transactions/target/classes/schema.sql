-- ═══════════════════════════════════════════════════════
-- Microservice Transactions — MariaDB Schema
-- ═══════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS transactions_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE transactions_db;

-- ── Transactions Ledger ────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id          BINARY(16)      NOT NULL,
    user_id     VARCHAR(255)    NOT NULL,
    amount      DECIMAL(19, 4)  NOT NULL,
    type        VARCHAR(20)     NOT NULL,
    category    VARCHAR(100)    NOT NULL,
    date        DATETIME(6)     NOT NULL,
    description VARCHAR(500)    NULL,

    PRIMARY KEY (id),
    INDEX idx_transactions_user_id (user_id),
    INDEX idx_transactions_date (date),
    INDEX idx_transactions_user_date (user_id, date DESC)
) ENGINE=InnoDB;

-- ── Transactional Outbox ───────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id    BINARY(16)      NOT NULL,
    event_type  VARCHAR(100)    NOT NULL,
    occurred_on TIMESTAMP(6)    NOT NULL,
    payload     TEXT            NOT NULL,
    published   BOOLEAN         NOT NULL DEFAULT FALSE,

    PRIMARY KEY (event_id),
    INDEX idx_outbox_pending (published, occurred_on)
) ENGINE=InnoDB;
