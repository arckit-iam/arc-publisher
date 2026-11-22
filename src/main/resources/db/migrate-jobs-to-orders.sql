-- Migration d'une base arc-publisher existante : jobs -> orders (+ type de compte).
-- A exécuter une seule fois, publisher arrêté. Les nouvelles installations utilisent init.sql.

ALTER TABLE IF EXISTS provisioning_jobs RENAME TO provisioning_orders;
ALTER TABLE IF EXISTS provisioning_job_attributes RENAME TO provisioning_order_attributes;
ALTER TABLE IF EXISTS provisioning_order_attributes RENAME COLUMN job_id TO order_id;
ALTER INDEX IF EXISTS idx_jobs_status_execute_at RENAME TO idx_orders_status_execute_at;

ALTER TABLE provisioning_orders ADD COLUMN IF NOT EXISTS account_type VARCHAR(255);

DROP TRIGGER IF EXISTS trg_job_created ON provisioning_orders;
DROP FUNCTION IF EXISTS notify_job_created();

CREATE OR REPLACE FUNCTION notify_order_created()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('order_created', NEW.id::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_created ON provisioning_orders;
CREATE TRIGGER trg_order_created
AFTER INSERT ON provisioning_orders
FOR EACH ROW
WHEN (NEW.status = 'PENDING')
EXECUTE FUNCTION notify_order_created();
