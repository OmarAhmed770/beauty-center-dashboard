-- Preserve every successful daily close as an immutable financial snapshot.
-- Existing rows become version 1; reopening changes their lifecycle status only.
ALTER TABLE daily_closings
    DROP CONSTRAINT uk_daily_closings_business_date;

ALTER TABLE daily_closings
    ADD COLUMN closing_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE daily_closings
    ADD CONSTRAINT uk_daily_closings_date_version UNIQUE (business_date, closing_version);

CREATE INDEX idx_daily_closings_date_version
    ON daily_closings (business_date, closing_version DESC);
