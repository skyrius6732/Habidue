-- posts 테이블의 type 컬럼 길이 확대 (VARCHAR(255) -> VARCHAR(20))
ALTER TABLE posts MODIFY COLUMN type VARCHAR(20) NOT NULL;
