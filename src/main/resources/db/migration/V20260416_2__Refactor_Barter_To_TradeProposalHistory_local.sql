-- 개발 환경: 불필요한 컬럼 제거 및 최종 조건 필드 추가
-- 1. posts 테이블: 불필요한 물물교환 필드 제거
ALTER TABLE posts DROP COLUMN item_condition;

-- 2. trade_proposals 테이블: 라운드별 협상 필드 제거 (TradeProposalHistory로 이관)
ALTER TABLE trade_proposals DROP COLUMN location;
ALTER TABLE trade_proposals DROP COLUMN method;
ALTER TABLE trade_proposals DROP COLUMN sender_address;
ALTER TABLE trade_proposals DROP COLUMN trade_date_time;

-- 3. trade_proposals 테이블: 최종 합의 조건 필드 추가
ALTER TABLE trade_proposals ADD COLUMN final_method VARCHAR(50);
ALTER TABLE trade_proposals ADD COLUMN final_location VARCHAR(255);
ALTER TABLE trade_proposals ADD COLUMN final_sender_address VARCHAR(255);
ALTER TABLE trade_proposals ADD COLUMN final_receiver_address VARCHAR(255);
ALTER TABLE trade_proposals ADD COLUMN final_trade_date_time DATETIME(6);

-- 4. TradeProposalHistory 테이블: 협상 라운드별 이력 저장
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
