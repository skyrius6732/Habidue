-- [시니어 조치] 기존 사용자 대상 공개 ID(publicId) 마이그레이션 스크립트
-- 이 작업은 보안 강화를 위해 숫자 PK 노출을 차단하는 publicId 체계 도입의 필수 단계입니다.

-- 1. 아직 public_id 값이 부여되지 않은 유저들을 대상으로 무작위 ID(u_ + 10자리 난수)를 부여합니다.
-- MD5(RAND()) 조합을 통해 고유성을 확보합니다.
UPDATE users 
SET public_id = CONCAT('u_', SUBSTR(MD5(RAND()), 1, 10)) 
WHERE public_id IS NULL;

-- 2. (선택 사항) 데이터 무결성 확인을 위해 중복된 public_id가 있는지 체크합니다.
-- 결과가 0건이어야 정상입니다.
SELECT public_id, COUNT(*) 
FROM users 
GROUP BY public_id 
HAVING COUNT(*) > 1;

-- 3. (필수) 운영 환경에서 UNIQUE 인덱스가 설정되어 있지 않다면 수동으로 추가하는 것을 권장합니다.
-- (Hibernate 설정에 따라 이미 생성되었을 수 있으므로 확인 후 실행하세요)
-- ALTER TABLE users ADD UNIQUE INDEX uk_users_public_id (public_id);
