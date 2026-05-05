-- Création de la table principale des jobs de provisioning
CREATE TABLE IF NOT EXISTS provisioning_jobs (
    id              UUID PRIMARY KEY,
    action          VARCHAR(20)  NOT NULL,
    identity_id     VARCHAR(255) NOT NULL,
    identity_name   VARCHAR(255) NOT NULL,
    target_system   VARCHAR(100) NOT NULL,
    entitlement_id  VARCHAR(255),
    entitlement_name VARCHAR(255),
    scope           VARCHAR(255),
    execute_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    correlation_id  VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
);

-- Table des attributs additionnels (map clé/valeur)
CREATE TABLE IF NOT EXISTS provisioning_job_attributes (
    job_id     UUID         NOT NULL REFERENCES provisioning_jobs(id) ON DELETE CASCADE,
    attr_key   VARCHAR(100) NOT NULL,
    attr_value VARCHAR(500),
    PRIMARY KEY (job_id, attr_key)
);

-- Index pour la requête des jobs dus
CREATE INDEX IF NOT EXISTS idx_jobs_status_execute_at
    ON provisioning_jobs (status, execute_at)
    WHERE status = 'PENDING';

-- Fonction de notification PostgreSQL
CREATE OR REPLACE FUNCTION notify_job_created()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('job_created', NEW.id::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger : émet NOTIFY à chaque insertion d'un job PENDING
CREATE TRIGGER trg_job_created
AFTER INSERT ON provisioning_jobs
FOR EACH ROW
WHEN (NEW.status = 'PENDING')
EXECUTE FUNCTION notify_job_created();
