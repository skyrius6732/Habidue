-- ============================================================
-- 개발 DB 초기화 스크립트
-- 보존: notice_keyword_metadata, badges, badge_level_rules
-- badges / badge_level_rules 는 서버 재시작 시 자동 복구됨
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 유저 활동 관련
TRUNCATE TABLE user_badges;
TRUNCATE TABLE user_notices;
TRUNCATE TABLE user_tag;
TRUNCATE TABLE user_blocks;
TRUNCATE TABLE user_activity_stats;
TRUNCATE TABLE attendance;
TRUNCATE TABLE exp_history;
TRUNCATE TABLE karma_history;

-- 공고 관련
TRUNCATE TABLE notice_wakeups;
TRUNCATE TABLE notice_tag;
TRUNCATE TABLE notices;

-- 커뮤니티
TRUNCATE TABLE comment_likes;
TRUNCATE TABLE comments;
TRUNCATE TABLE post_likes;
TRUNCATE TABLE post_tags;
TRUNCATE TABLE post_images;
TRUNCATE TABLE posts;

-- 메시지
TRUNCATE TABLE message_files;
TRUNCATE TABLE messages;

-- 알림
TRUNCATE TABLE notifications;
TRUNCATE TABLE device_tokens;

-- 기타
TRUNCATE TABLE reports;
TRUNCATE TABLE inquiries;
TRUNCATE TABLE tag;

-- 운영 데이터
TRUNCATE TABLE patch_note_details;
TRUNCATE TABLE patch_notes;
TRUNCATE TABLE announcements;

-- 유저 (마지막)
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;
