-- =====================================================
-- V8: Stored Functions and Views
-- NOTE: PostgreSQL 10 has no PROCEDURE – use FUNCTION RETURNS void
-- =====================================================

-- Function: Generate monthly bills for all active meters
CREATE OR REPLACE FUNCTION sp_generate_monthly_bills(p_year INT, p_month INT)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_cycle_id      BIGINT;
    v_due_date      DATE;
    v_bill_number   VARCHAR(30);
    v_meter         RECORD;
    v_consumption   NUMERIC;
    v_total         NUMERIC;
    v_bills_created INT := 0;
BEGIN
    v_due_date := (DATE(p_year::TEXT || '-' || LPAD(p_month::TEXT, 2, '0') || '-01')
                   + INTERVAL '1 month' + INTERVAL '15 days')::DATE;

    INSERT INTO billing_cycles (billing_year, billing_month, start_date, end_date, due_date, created_by)
    VALUES (
        p_year,
        p_month,
        DATE(p_year::TEXT || '-' || LPAD(p_month::TEXT, 2, '0') || '-01'),
        (DATE(p_year::TEXT || '-' || LPAD(p_month::TEXT, 2, '0') || '-01')
            + INTERVAL '1 month' - INTERVAL '1 day')::DATE,
        v_due_date,
        'SYSTEM'
    )
    ON CONFLICT (billing_year, billing_month)
    DO UPDATE SET updated_at = CURRENT_TIMESTAMP
    RETURNING id INTO v_cycle_id;

    FOR v_meter IN
        SELECT m.id, m.meter_type, m.customer_id, m.tariff_id
        FROM meters m
        JOIN customers c ON c.id = m.customer_id
        WHERE m.status = 'ACTIVE' AND c.status = 'ACTIVE'
    LOOP
        IF EXISTS (
            SELECT 1 FROM bills
            WHERE meter_id = v_meter.id AND billing_cycle_id = v_cycle_id
        ) THEN
            CONTINUE;
        END IF;

        SELECT COALESCE(consumption, 0) INTO v_consumption
        FROM meter_readings
        WHERE meter_id = v_meter.id
          AND reading_year = p_year
          AND reading_month = p_month
        LIMIT 1;

        v_total := COALESCE(v_consumption, 0) * COALESCE(
            (SELECT flat_rate FROM tariffs
             WHERE id = v_meter.tariff_id AND active = TRUE),
            200
        );

        v_bill_number :=
            CASE WHEN v_meter.meter_type = 'WATER' THEN 'WB' ELSE 'EB' END
            || '-' || p_year::TEXT || LPAD(p_month::TEXT, 2, '0')
            || '-' || LPAD((EXTRACT(EPOCH FROM NOW())::BIGINT % 999999)::TEXT, 6, '0');

        INSERT INTO bills (
            bill_number, customer_id, meter_id, billing_cycle_id,
            utility_type, consumption_amount, service_charge_amount, tax_amount,
            penalty_amount, total_amount, paid_amount, balance_amount,
            status, bill_date, due_date, created_by
        ) VALUES (
            v_bill_number, v_meter.customer_id, v_meter.id, v_cycle_id,
            v_meter.meter_type, v_total, 0, 0, 0, v_total, 0, v_total,
            'PENDING', CURRENT_DATE, v_due_date, 'SYSTEM'
        );

        v_bills_created := v_bills_created + 1;
    END LOOP;

    RAISE NOTICE 'Generated % bills for %/%', v_bills_created, p_year, p_month;
END;
$$;


-- Function: Apply penalties to all overdue bills
CREATE OR REPLACE FUNCTION sp_apply_penalties()
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_bill      RECORD;
    v_penalty   NUMERIC;
    v_rate      NUMERIC;
BEGIN
    FOR v_bill IN
        SELECT b.id, b.utility_type, b.balance_amount
        FROM bills b
        WHERE b.status NOT IN ('PAID', 'CANCELLED')
          AND b.due_date < CURRENT_DATE
    LOOP
        SELECT COALESCE(rate, 1.5) INTO v_rate
        FROM penalties
        WHERE utility_type = v_bill.utility_type AND active = TRUE
        LIMIT 1;

        v_penalty := v_bill.balance_amount * (v_rate / 100.0);

        UPDATE bills
        SET penalty_amount = penalty_amount + v_penalty,
            total_amount   = total_amount   + v_penalty,
            balance_amount = balance_amount + v_penalty,
            status         = 'OVERDUE',
            updated_at     = CURRENT_TIMESTAMP,
            updated_by     = 'SYSTEM'
        WHERE id = v_bill.id;
    END LOOP;
END;
$$;


-- Function: Monthly billing report (returns a set of rows)
CREATE OR REPLACE FUNCTION fn_monthly_billing_report(p_year INT, p_month INT)
RETURNS TABLE (
    customer_name   TEXT,
    meter_number    TEXT,
    utility_type    TEXT,
    consumption     NUMERIC,
    total_amount    NUMERIC,
    paid_amount     NUMERIC,
    balance_amount  NUMERIC,
    status          TEXT,
    due_date        DATE
) LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.full_name::TEXT,
        m.meter_number::TEXT,
        b.utility_type::TEXT,
        COALESCE(mr.consumption, 0),
        b.total_amount,
        b.paid_amount,
        b.balance_amount,
        b.status::TEXT,
        b.due_date
    FROM bills b
    JOIN customers c       ON c.id = b.customer_id
    JOIN meters m          ON m.id = b.meter_id
    JOIN billing_cycles bc ON bc.id = b.billing_cycle_id
    LEFT JOIN meter_readings mr ON mr.id = b.meter_reading_id
    WHERE bc.billing_year = p_year AND bc.billing_month = p_month
    ORDER BY c.full_name, b.utility_type;
END;
$$;


-- Function: Customer outstanding balance
CREATE OR REPLACE FUNCTION fn_customer_outstanding(p_customer_id BIGINT)
RETURNS NUMERIC LANGUAGE plpgsql AS $$
DECLARE
    v_balance NUMERIC;
BEGIN
    SELECT COALESCE(SUM(balance_amount), 0)
    INTO v_balance
    FROM bills
    WHERE customer_id = p_customer_id
      AND status NOT IN ('PAID', 'CANCELLED');
    RETURN v_balance;
END;
$$;


-- View: Active unpaid bills summary
CREATE OR REPLACE VIEW vw_unpaid_bills AS
SELECT
    b.bill_number,
    c.full_name       AS customer_name,
    c.email           AS customer_email,
    m.meter_number,
    b.utility_type,
    b.total_amount,
    b.paid_amount,
    b.balance_amount,
    b.due_date,
    b.status,
    CURRENT_DATE - b.due_date AS days_overdue
FROM bills b
JOIN customers c ON c.id = b.customer_id
JOIN meters m    ON m.id = b.meter_id
WHERE b.status NOT IN ('PAID', 'CANCELLED')
ORDER BY b.due_date ASC;
