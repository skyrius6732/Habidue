-- Add message field to trade_proposal_histories table
ALTER TABLE trade_proposal_histories ADD COLUMN message VARCHAR(500) DEFAULT NULL COMMENT '조건 제시 시 메시지';
