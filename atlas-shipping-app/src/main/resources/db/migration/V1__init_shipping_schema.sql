CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    tracking_number VARCHAR(100) UNIQUE,
    carrier VARCHAR(50) NOT NULL,
    service_type VARCHAR(100) NOT NULL,

    -- Sender Address
    sender_contact_name VARCHAR(150),
    sender_company_name VARCHAR(150),
    sender_street1 VARCHAR(255),
    sender_street2 VARCHAR(255),
    sender_city VARCHAR(100),
    sender_state VARCHAR(100),
    sender_postal_code VARCHAR(20),
    sender_country_code VARCHAR(10),
    sender_phone VARCHAR(50),

    -- Recipient Address
    recipient_contact_name VARCHAR(150),
    recipient_company_name VARCHAR(150),
    recipient_street1 VARCHAR(255),
    recipient_street2 VARCHAR(255),
    recipient_city VARCHAR(100),
    recipient_state VARCHAR(100),
    recipient_postal_code VARCHAR(20),
    recipient_country_code VARCHAR(10),
    recipient_phone VARCHAR(50),

    status VARCHAR(50) NOT NULL,
    total_cost NUMERIC(19, 2),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE
);

CREATE TABLE shipment_packages (
    shipment_id BIGINT NOT NULL,
    weight_in_kg DOUBLE PRECISION NOT NULL,
    length_cm DOUBLE PRECISION NOT NULL,
    width_cm DOUBLE PRECISION NOT NULL,
    height_cm DOUBLE PRECISION NOT NULL,
    description VARCHAR(255),
    declared_value NUMERIC(19, 2),
    CONSTRAINT fk_shipment_packages_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
);
