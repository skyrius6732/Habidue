-- 거래 완료 처리를 위한 컬럼 추가
ALTER TABLE trade_proposals
  ADD COLUMN proposer_completed_at DATETIME NULL,
  ADD COLUMN receiver_completed_at DATETIME NULL,
  ADD COLUMN final_completed_at DATETIME NULL;
