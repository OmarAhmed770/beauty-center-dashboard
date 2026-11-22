CREATE TABLE catalog_categories (
    id BIGSERIAL PRIMARY KEY,
    name_arabic VARCHAR(150) NOT NULL,
    name_english VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_catalog_categories_name_arabic UNIQUE (name_arabic),
    CONSTRAINT uk_catalog_categories_name_english UNIQUE (name_english),
    CONSTRAINT chk_catalog_categories_arabic_not_blank CHECK (length(trim(name_arabic)) >= 1),
    CONSTRAINT chk_catalog_categories_english_not_blank CHECK (length(trim(name_english)) >= 1),
    CONSTRAINT chk_catalog_categories_description_not_blank CHECK (
        description IS NULL OR length(trim(description)) >= 1
    ),
    CONSTRAINT chk_catalog_categories_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_catalog_categories_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE catalog_items (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name_arabic VARCHAR(150) NOT NULL,
    name_english VARCHAR(150) NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    category_id BIGINT NOT NULL,
    current_selling_price NUMERIC(19, 2) NOT NULL,
    current_cost NUMERIC(19, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_catalog_items_code UNIQUE (code),
    CONSTRAINT fk_catalog_items_category
        FOREIGN KEY (category_id) REFERENCES catalog_categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_catalog_items_code_not_blank CHECK (length(trim(code)) >= 1),
    CONSTRAINT chk_catalog_items_code_normalized CHECK (code = upper(trim(code))),
    CONSTRAINT chk_catalog_items_arabic_not_blank CHECK (length(trim(name_arabic)) >= 1),
    CONSTRAINT chk_catalog_items_english_not_blank CHECK (length(trim(name_english)) >= 1),
    CONSTRAINT chk_catalog_items_type CHECK (
        item_type IN ('SERVICE', 'PRODUCT', 'HAIR_PRODUCT', 'HAIR_INSTALLATION')
    ),
    CONSTRAINT chk_catalog_items_selling_price_nonnegative CHECK (current_selling_price >= 0),
    CONSTRAINT chk_catalog_items_cost_nonnegative CHECK (current_cost >= 0),
    CONSTRAINT chk_catalog_items_description_not_blank CHECK (
        description IS NULL OR length(trim(description)) >= 1
    ),
    CONSTRAINT chk_catalog_items_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_catalog_items_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    catalog_item_id BIGINT NOT NULL,
    value_type VARCHAR(20) NOT NULL,
    old_value NUMERIC(19, 2) NOT NULL,
    new_value NUMERIC(19, 2) NOT NULL,
    effective_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    change_reason VARCHAR(500) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_history_catalog_item
        FOREIGN KEY (catalog_item_id) REFERENCES catalog_items (id) ON DELETE RESTRICT,
    CONSTRAINT chk_price_history_type CHECK (value_type IN ('SELLING_PRICE', 'COST')),
    CONSTRAINT chk_price_history_old_nonnegative CHECK (old_value >= 0),
    CONSTRAINT chk_price_history_new_nonnegative CHECK (new_value >= 0),
    CONSTRAINT chk_price_history_changed CHECK (old_value <> new_value),
    CONSTRAINT chk_price_history_reason_not_blank CHECK (length(trim(change_reason)) >= 1),
    CONSTRAINT chk_price_history_actor_not_blank CHECK (length(trim(changed_by)) >= 1)
);

CREATE TABLE discounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(19, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    catalog_item_id BIGINT,
    category_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_discounts_catalog_item
        FOREIGN KEY (catalog_item_id) REFERENCES catalog_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_discounts_category
        FOREIGN KEY (category_id) REFERENCES catalog_categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_discounts_name_not_blank CHECK (length(trim(name)) >= 1),
    CONSTRAINT chk_discounts_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_discounts_value_nonnegative CHECK (discount_value >= 0),
    CONSTRAINT chk_discounts_percentage CHECK (
        discount_type <> 'PERCENTAGE' OR discount_value <= 100
    ),
    CONSTRAINT chk_discounts_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_discounts_single_scope CHECK (
        NOT (catalog_item_id IS NOT NULL AND category_id IS NOT NULL)
    ),
    CONSTRAINT chk_discounts_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_discounts_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE hair_configurations (
    id BIGSERIAL PRIMARY KEY,
    hair_item_id BIGINT NOT NULL,
    brand VARCHAR(150) NOT NULL,
    hair_length VARCHAR(100) NOT NULL,
    color VARCHAR(100),
    unit_type VARCHAR(50) NOT NULL,
    unit_selling_price NUMERIC(19, 2) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    installation_fee NUMERIC(19, 2) NOT NULL,
    minimum_quantity NUMERIC(19, 3),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_hair_configurations_item
        FOREIGN KEY (hair_item_id) REFERENCES catalog_items (id) ON DELETE RESTRICT,
    CONSTRAINT chk_hair_configurations_brand_not_blank CHECK (length(trim(brand)) >= 1),
    CONSTRAINT chk_hair_configurations_length_not_blank CHECK (length(trim(hair_length)) >= 1),
    CONSTRAINT chk_hair_configurations_color_not_blank CHECK (
        color IS NULL OR length(trim(color)) >= 1
    ),
    CONSTRAINT chk_hair_configurations_unit_not_blank CHECK (length(trim(unit_type)) >= 1),
    CONSTRAINT chk_hair_configurations_selling_nonnegative CHECK (unit_selling_price >= 0),
    CONSTRAINT chk_hair_configurations_cost_nonnegative CHECK (unit_cost >= 0),
    CONSTRAINT chk_hair_configurations_fee_nonnegative CHECK (installation_fee >= 0),
    CONSTRAINT chk_hair_configurations_minimum_positive CHECK (
        minimum_quantity IS NULL OR minimum_quantity > 0
    ),
    CONSTRAINT chk_hair_configurations_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_hair_configurations_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE hair_price_override_audits (
    id BIGSERIAL PRIMARY KEY,
    hair_configuration_id BIGINT NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    original_amount NUMERIC(19, 2) NOT NULL,
    overridden_amount NUMERIC(19, 2) NOT NULL,
    override_reason VARCHAR(500) NOT NULL,
    authorized_by VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_hair_override_configuration
        FOREIGN KEY (hair_configuration_id) REFERENCES hair_configurations (id) ON DELETE RESTRICT,
    CONSTRAINT chk_hair_override_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_hair_override_original_nonnegative CHECK (original_amount >= 0),
    CONSTRAINT chk_hair_override_amount_nonnegative CHECK (overridden_amount >= 0),
    CONSTRAINT chk_hair_override_changed CHECK (original_amount <> overridden_amount),
    CONSTRAINT chk_hair_override_reason_not_blank CHECK (length(trim(override_reason)) >= 1),
    CONSTRAINT chk_hair_override_actor_not_blank CHECK (length(trim(authorized_by)) >= 1)
);

CREATE INDEX idx_catalog_categories_active_english
    ON catalog_categories (active, name_english);
CREATE INDEX idx_catalog_items_category_type_active
    ON catalog_items (category_id, item_type, active);
CREATE INDEX idx_catalog_items_type_active_english
    ON catalog_items (item_type, active, name_english);
CREATE INDEX idx_price_history_item_effective
    ON price_history (catalog_item_id, effective_at DESC);
CREATE INDEX idx_price_history_type_effective
    ON price_history (value_type, effective_at DESC);
CREATE INDEX idx_discounts_active_dates
    ON discounts (active, start_date, end_date);
CREATE INDEX idx_discounts_catalog_item
    ON discounts (catalog_item_id, active, start_date, end_date);
CREATE INDEX idx_discounts_category
    ON discounts (category_id, active, start_date, end_date);
CREATE INDEX idx_hair_configurations_item_active_effective
    ON hair_configurations (hair_item_id, active, effective_date DESC);
CREATE INDEX idx_hair_override_configuration_time
    ON hair_price_override_audits (hair_configuration_id, occurred_at DESC);
