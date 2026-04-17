-- 물물교환 시스템 통합 마이그레이션
-- posts 테이블: 불필요 컬럼 제거 및 필드 추가
-- trade_proposals 테이블: JSON 필드 제거 및 최종 합의 조건 필드 추가
-- trade_proposal_histories 테이블: 새로 생성

-- 1. posts 테이블: 물물교환 불필요 필드 제거 (기존 리팩토링)
ALTER TABLE posts DROP COLUMN IF EXISTS barter_status;
ALTER TABLE posts DROP COLUMN IF EXISTS item_condition;
ALTER TABLE posts DROP COLUMN IF EXISTS wanted_item;

-- 2. posts 테이블: item_name 필드 추가
ALTER TABLE posts ADD COLUMN IF NOT EXISTS item_name VARCHAR(255);

-- 3. posts 테이블: 선호 조건 필드 추가
ALTER TABLE posts ADD COLUMN IF NOT EXISTS preferred_method VARCHAR(50);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS preferred_date DATE;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS preferred_time VARCHAR(5);

-- 4. posts 테이블: 선호 조건 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_posts_preferred_method ON posts(preferred_method);

-- 5. trade_proposals 테이블: JSON 협상 필드 제거
ALTER TABLE trade_proposals DROP COLUMN IF EXISTS proposer_schedule_json;
ALTER TABLE trade_proposals DROP COLUMN IF EXISTS receiver_schedule_json;
ALTER TABLE trade_proposals DROP COLUMN IF EXISTS agreed_schedule_json;

-- 6. trade_proposals 테이블: 최종 합의 조건 필드 추가
ALTER TABLE trade_proposals ADD COLUMN IF NOT EXISTS final_method VARCHAR(50);
ALTER TABLE trade_proposals ADD COLUMN IF NOT EXISTS final_location VARCHAR(255);
ALTER TABLE trade_proposals ADD COLUMN IF NOT EXISTS final_sender_address VARCHAR(255);
ALTER TABLE trade_proposals ADD COLUMN IF NOT EXISTS final_receiver_address VARCHAR(255);
ALTER TABLE trade_proposals ADD COLUMN IF NOT EXISTS final_trade_date_time DATETIME(6);

-- 7. TradeProposalHistory 테이블: 협상 라운드별 이력 저장
CREATE TABLE IF NOT EXISTS trade_proposal_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    round INT NOT NULL,
    set_by VARCHAR(50) NOT NULL,
    method VARCHAR(50),
    location VARCHAR(255),
    sender_address VARCHAR(255),
    receiver_address VARCHAR(255),
    trade_date_time DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_history_proposal FOREIGN KEY (proposal_id) REFERENCES trade_proposals(id) ON DELETE CASCADE,
    INDEX idx_proposal_round (proposal_id, round),
    INDEX idx_proposal_set_by (proposal_id, set_by)
);
