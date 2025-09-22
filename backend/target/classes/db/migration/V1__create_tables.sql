CREATE TABLE room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    location VARCHAR(255),
    status VARCHAR(50) DEFAULT 'AVAILABLE'
);

CREATE TABLE equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE'
);

CREATE TABLE room_equipment (
    room_id BIGINT,
    equipment_id BIGINT,
    PRIMARY KEY (room_id, equipment_id),
    FOREIGN KEY (room_id) REFERENCES room(id),
    FOREIGN KEY (equipment_id) REFERENCES equipment(id)
);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    attendees_count INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority INT DEFAULT 3,
    notes TEXT,
    ai_decision_status VARCHAR(50),
    ai_decision_confidence DOUBLE,
    ai_decision_rationale TEXT,
    FOREIGN KEY (room_id) REFERENCES room(id)
);

CREATE TABLE booking_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    changed_by BIGINT NOT NULL,
    notes TEXT,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE approval_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    action_time TIMESTAMP NOT NULL,
    comments TEXT,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);