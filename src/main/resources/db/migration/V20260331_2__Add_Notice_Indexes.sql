-- [시니어 조치] 공고 검색 및 캘린더 조회 성능 최적화를 위한 인덱스 추가
-- 1. 날짜 기반 검색(announcement_date, deadline, result_date) 속도 향상
CREATE INDEX idx_notice_dates ON notices (announcement_date, deadline, result_date);

-- 2. 상태 및 출처 필터링 성능 향상
CREATE INDEX idx_notice_status_source ON notices (status, source);
