-- =====================================================
-- V7: PostgreSQL Triggers
-- =====================================================

-- Trigger 1: Auto-create notification when a bill is generated
CREATE OR REPLACE FUNCTION fn_notify_bill_generated()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notifications (user_id, customer_id, type, title, message, status, created_by)
    SELECT u.id, NEW.customer_id,
           'BILL_GENERATED',
           'New Utility Bill – ' || NEW.bill_number,
           'Your utility bill ' || NEW.bill_number || ' amounting to ' ||
           NEW.total_amount || ' RWF has been generated. Due: ' || NEW.due_date,
           'UNREAD',
           'SYSTEM'
    FROM customers c
    LEFT JOIN users u ON u.id = c.user_id
    WHERE c.id = NEW.customer_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notify_bill_generated ON bills;
CREATE TRIGGER trg_notify_bill_generated
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE fn_notify_bill_generated();


-- Trigger 2: Auto-mark bill as PAID when balance reaches zero
CREATE OR REPLACE FUNCTION fn_auto_mark_bill_paid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.balance_amount <= 0 AND NEW.status NOT IN ('PAID', 'CANCELLED') THEN
        NEW.status := 'PAID';
        NEW.paid_at := CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auto_mark_bill_paid ON bills;
CREATE TRIGGER trg_auto_mark_bill_paid
    BEFORE UPDATE ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE fn_auto_mark_bill_paid();


-- Trigger 3: Auto-create notification when payment is received
CREATE OR REPLACE FUNCTION fn_notify_payment_received()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notifications (user_id, customer_id, type, title, message, status, created_by)
    SELECT u.id, NEW.customer_id,
           'PAYMENT_RECEIVED',
           'Payment Confirmed – Receipt ' || NEW.receipt_number,
           'Your payment of ' || NEW.amount || ' RWF for bill has been received. Receipt: ' || NEW.receipt_number,
           'UNREAD',
           'SYSTEM'
    FROM customers c
    LEFT JOIN users u ON u.id = c.user_id
    WHERE c.id = NEW.customer_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notify_payment_received ON payments;
CREATE TRIGGER trg_notify_payment_received
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE PROCEDURE fn_notify_payment_received();


-- Trigger 4: Audit log on bill status changes
CREATE OR REPLACE FUNCTION fn_audit_bill_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO audit_logs (performed_by, performed_at, action, entity_name, entity_id, old_values, new_values, description)
        VALUES (
            COALESCE(NEW.updated_by, 'SYSTEM'),
            CURRENT_TIMESTAMP,
            'UPDATE',
            'Bill',
            NEW.id::TEXT,
            'status=' || OLD.status,
            'status=' || NEW.status,
            'Bill status changed: ' || OLD.status || ' → ' || NEW.status
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_bill_status ON bills;
CREATE TRIGGER trg_audit_bill_status
    AFTER UPDATE ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE fn_audit_bill_status();
