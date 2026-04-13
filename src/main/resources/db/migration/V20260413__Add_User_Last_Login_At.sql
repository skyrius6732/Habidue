-- [시니어 조치] 유저 마지막 로그인 시점 기록을 위한 컬럼 추가 (2026-04-13)
-- Hibernate ddl-auto: update와 별개로 스키마 변경 이력 관리를 위해 명시적으로 작성함.
ALTER TABLE users ADD COLUMN last_login_at DATETIME NULL;
