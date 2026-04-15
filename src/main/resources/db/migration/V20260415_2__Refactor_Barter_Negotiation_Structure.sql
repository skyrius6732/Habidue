-- 1. 기존 trade_schedules 테이블 제거 (데이터 통합을 위해)
DROP TABLE IF EXISTS trade_schedules;

-- 2. trade_proposals 테이블에 협상 및 물류 필드 추가
ALTER TABLE trade_proposals 
    ADD COLUMN trade_method VARCHAR(50),
    ADD COLUMN meeting_location VARCHAR(255),
    ADD COLUMN proposer_address VARCHAR(255),
    ADD COLUMN receiver_address VARCHAR(255),
    ADD COLUMN trade_date_time DATETIME(6),
    ADD COLUMN last_negotiated_by_id BIGINT,
    ADD COLUMN negotiation_round INT DEFAULT 0,
    ADD COLUMN proposer_completed_at DATETIME(6),
    ADD COLUMN receiver_completed_at DATETIME(6),
    ADD COLUMN final_completed_at DATETIME(6),
    ADD COLUMN proposer_schedule_json LONGTEXT,
    ADD COLUMN receiver_schedule_json LONGTEXT,
    ADD COLUMN agreed_schedule_json LONGTEXT;

-- 3. 외래 키 제약 조건 추가 (마지막 협상자 추적용)
ALTER TABLE trade_proposals 
    ADD CONSTRAINT fk_proposal_last_negotiated_by 
    FOREIGN KEY (last_negotiated_by_id) REFERENCES users(id);
