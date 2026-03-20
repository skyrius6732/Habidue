-- Notices 테이블에 마지막 게시글 시각 및 재활성화 시각 컬럼 추가
ALTER TABLE notices ADD COLUMN last_post_at DATETIME;
ALTER TABLE notices ADD COLUMN revived_at DATETIME;

-- 기존 데이터 보정: 마지막 게시글 시각이 없을 경우 업데이트 시각으로 초기화 (즉시 닫힘 방지)
UPDATE notices SET last_post_at = updated_at WHERE last_post_at IS NULL;
