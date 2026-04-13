-- [시니어 조치] 탈퇴 유저 증거 보존 및 미래 확장성(전화번호 등)을 고려한 아카이브 테이블 생성 (2026-04-13)
CREATE TABLE user_archives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    username VARCHAR(255),
    email VARCHAR(255),
    nickname VARCHAR(255),
    provider VARCHAR(50),
    provider_id VARCHAR(255),
    real_name VARCHAR(100),
    phone_number VARCHAR(50),
    address TEXT,
    raw_attributes TEXT,
    withdrawn_at DATETIME(6) NOT NULL,
    INDEX idx_archive_target_user_id (target_user_id),
    INDEX idx_archive_email (email),
    INDEX idx_archive_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
