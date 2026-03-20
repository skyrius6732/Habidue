-- 1. 공지사항 테이블 생성
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag VARCHAR(20),
    type VARCHAR(20),
    date VARCHAR(20),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    created_at DATETIME,
    updated_at DATETIME
);

-- 2. 패치 노트 테이블 생성
CREATE TABLE IF NOT EXISTS patch_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL,
    date VARCHAR(20),
    created_at DATETIME,
    updated_at DATETIME
);

-- 3. 패치 노트 상세 항목 테이블 생성
CREATE TABLE IF NOT EXISTS patch_note_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patch_note_id BIGINT,
    content VARCHAR(255),
    FOREIGN KEY (patch_note_id) REFERENCES patch_notes(id) ON DELETE CASCADE
);

-- 초기 데이터 삽입
INSERT INTO announcements (tag, type, date, title, content, created_at, updated_at) VALUES
('중요', 'notice', '2026-03-09', '태깅 시스템 및 UI 고도화 업데이트 안내', '공고의 특징을 더 잘 파악할 수 있도록 역세권, 신축 등 특수 태그 노출 로직이 강화되었습니다.', NOW(), NOW()),
('안내', 'info', '2026-03-05', 'SH 서울주택도시공사 공고 수집 개시', '기존 LH 공고에 이어 SH 공사에서 제공하는 서울 지역 공고들도 실시간으로 확인하실 수 있습니다.', NOW(), NOW());

INSERT INTO patch_notes (id, version, date, created_at, updated_at) VALUES
(1, 'v1.1.0', '2026-03-09', NOW(), NOW()),
(2, 'v1.0.5', '2026-03-05', NOW(), NOW()),
(3, 'v1.0.0', '2026-02-23', NOW(), NOW());

INSERT INTO patch_note_details (patch_note_id, content) VALUES
(1, '태깅 엔진 고도화: 복합어(예: 군산신역세권)에서 키워드 추출 정확도 향상'),
(1, 'UI 개선: 공고 리스트에서 미노출 태그 개수 표시 (외 N개)'),
(1, '디테일링: 상세 모달 내 기관명 한글화 (PRIVATE -> 민간)'),
(1, '버그 수정: 메타데이터 비교 시 발생하던 간헐적 오류 해결'),
(2, '태그 시스템 전면 개편: Enum 방식에서 DB 메타데이터 기반으로 전환'),
(2, 'SH 공사 및 민간임대 크롤러 정식 도입'),
(2, '다크 모드 색상 대비 최적화'),
(3, 'HabiDue MVP 버전 런칭'),
(3, 'LH 공고 수집 및 관심 등록 기능'),
(3, '맞춤형 키워드 필터링 시스템');
