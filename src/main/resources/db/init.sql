-- Création de la table principale des orders de provisioning
CREATE TABLE IF NOT EXISTS provisioning_orders (
    id              UUID PRIMARY KEY,
    action          VARCHAR(20)  NOT NULL,
    target_type     VARCHAR(20)  NOT NULL,
    identity_id     VARCHAR(255) NOT NULL,
    identity_name   VARCHAR(255) NOT NULL,
    target_system   VARCHAR(100) NOT NULL,
    entitlement_id  VARCHAR(255),
    entitlement_name VARCHAR(255),
    scope           VARCHAR(255),
    account_type    VARCHAR(255),
    source          VARCHAR(255),
    execute_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    correlation_id  VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
);

-- Table des attributs additionnels (map clé/valeur)
CREATE TABLE IF NOT EXISTS provisioning_order_attributes (
    order_id     UUID         NOT NULL REFERENCES provisioning_orders(id) ON DELETE CASCADE,
    attr_key   VARCHAR(100) NOT NULL,
    attr_value VARCHAR(500),
    PRIMARY KEY (order_id, attr_key)
);

-- Index pour la requête des orders dus
CREATE INDEX IF NOT EXISTS idx_orders_status_execute_at
    ON provisioning_orders (status, execute_at)
    WHERE status = 'PENDING';

-- Fonction de notification PostgreSQL
CREATE OR REPLACE FUNCTION notify_order_created()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('order_created', NEW.id::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger : émet NOTIFY à chaque insertion d'un order PENDING
CREATE TRIGGER trg_order_created
AFTER INSERT ON provisioning_orders
FOR EACH ROW
WHEN (NEW.status = 'PENDING')
EXECUTE FUNCTION notify_order_created();
