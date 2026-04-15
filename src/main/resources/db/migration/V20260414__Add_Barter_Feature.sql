-- posts 테이블에 물물교환 필드 추가
ALTER TABLE posts ADD COLUMN wanted_item VARCHAR(255);
ALTER TABLE posts ADD COLUMN item_condition VARCHAR(50);
ALTER TABLE posts ADD COLUMN barter_status VARCHAR(50);

-- trade_proposals 테이블 생성
CREATE TABLE trade_proposals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposer_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    barter_post_id BIGINT NOT NULL,
    offered_post_id BIGINT NOT NULL,
    message VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    question_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_proposal_proposer FOREIGN KEY (proposer_id) REFERENCES users(id),
    CONSTRAINT fk_proposal_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT fk_proposal_barter_post FOREIGN KEY (barter_post_id) REFERENCES posts(id),
    CONSTRAINT fk_proposal_offered_post FOREIGN KEY (offered_post_id) REFERENCES posts(id)
);

-- trade_schedules 테이블 생성
CREATE TABLE trade_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL UNIQUE,
    method VARCHAR(50) NOT NULL,
    location VARCHAR(255),
    trade_date_time DATETIME(6),
    CONSTRAINT fk_schedule_proposal FOREIGN KEY (proposal_id) REFERENCES trade_proposals(id)
);
