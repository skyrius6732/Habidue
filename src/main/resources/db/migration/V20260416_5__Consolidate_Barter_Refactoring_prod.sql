-- 물물교환 시스템 통합 마이그레이션
-- trade_proposals 테이블: JSON 필드 제거 및 최종 합의 조건 필드 추가
-- trade_proposal_histories 테이블: 새로 생성


-- 4. posts 테이블: 선호 조건 인덱스 추가
CREATE INDEX idx_posts_preferred_method ON posts(preferred_method);

-- 5. trade_proposals 테이블: JSON 협상 필드 제거
ALTER TABLE trade_proposals DROP proposer_schedule_json;
ALTER TABLE trade_proposals DROP receiver_schedule_json;
ALTER TABLE trade_proposals DROP agreed_schedule_json;

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
