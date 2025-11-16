CREATE TABLE chat_room (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    employee_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'OPEN',
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT REFERENCES chat_room(id),
    sender_id VARCHAR(255) NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) DEFAULT 'TEXT',
    timestamp TIMESTAMP DEFAULT NOW(),
    read BOOLEAN DEFAULT FALSE
);