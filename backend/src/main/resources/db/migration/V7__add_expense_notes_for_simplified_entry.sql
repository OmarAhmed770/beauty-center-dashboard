ALTER TABLE expenses
    ADD COLUMN notes VARCHAR(2000);

ALTER TABLE expenses
    ADD CONSTRAINT chk_expenses_notes_not_blank
        CHECK (notes IS NULL OR length(trim(notes)) >= 1);
