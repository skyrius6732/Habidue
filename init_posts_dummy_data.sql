-- ============================================================
-- 게시글 더미 데이터 - 베타테스트 자연스러운 분위기
-- 임대주택 30%, 테스트/뻘글 40%, 앱평가 30%
-- 총 140개 게시글, 20명의 테스터
-- ============================================================

-- 1. 테스터 사용자 20명 (자연스러운 닉네임)
INSERT IGNORE INTO users (username, nickname, email, provider, provider_id, role, total_exp, karma_point, status, created_at, updated_at) VALUES
('test_01', '아지764', 'test01@test.com', 'google', 'gid_01', 'USER', 50, 1000, 'ACTIVE', NOW(), NOW()),
('test_02', '달콤한밤', 'test02@test.com', 'google', 'gid_02', 'USER', 120, 1000, 'ACTIVE', NOW(), NOW()),
('test_03', '수박수박', 'test03@test.com', 'google', 'gid_03', 'USER', 30, 1000, 'ACTIVE', NOW(), NOW()),
('test_04', '빛나는별', 'test04@test.com', 'google', 'gid_04', 'USER', 80, 1000, 'ACTIVE', NOW(), NOW()),
('test_05', '라이언22', 'test05@test.com', 'google', 'gid_05', 'USER', 95, 1000, 'ACTIVE', NOW(), NOW()),
('test_06', '우주선', 'test06@test.com', 'google', 'gid_06', 'USER', 200, 1000, 'ACTIVE', NOW(), NOW()),
('test_07', '그린애플', 'test07@test.com', 'google', 'gid_07', 'USER', 40, 1000, 'ACTIVE', NOW(), NOW()),
('test_08', '초코칩', 'test08@test.com', 'google', 'gid_08', 'USER', 15, 1000, 'ACTIVE', NOW(), NOW()),
('test_09', '야행성인', 'test09@test.com', 'google', 'gid_09', 'USER', 70, 1000, 'ACTIVE', NOW(), NOW()),
('test_10', '별빛속으로', 'test10@test.com', 'google', 'gid_10', 'USER', 60, 1000, 'ACTIVE', NOW(), NOW()),
('test_11', '돌고래', 'test11@test.com', 'google', 'gid_11', 'USER', 45, 1000, 'ACTIVE', NOW(), NOW()),
('test_12', '하늘색', 'test12@test.com', 'google', 'gid_12', 'USER', 110, 1000, 'ACTIVE', NOW(), NOW()),
('test_13', '왕딱지', 'test13@test.com', 'google', 'gid_13', 'USER', 65, 1000, 'ACTIVE', NOW(), NOW()),
('test_14', '가을바람', 'test14@test.com', 'google', 'gid_14', 'USER', 180, 1000, 'ACTIVE', NOW(), NOW()),
('test_15', '따뜻한손', 'test15@test.com', 'google', 'gid_15', 'USER', 130, 1000, 'ACTIVE', NOW(), NOW()),
('test_16', '밤하늘', 'test16@test.com', 'google', 'gid_16', 'USER', 50, 1000, 'ACTIVE', NOW(), NOW()),
('test_17', '무지개색', 'test17@test.com', 'google', 'gid_17', 'USER', 75, 1000, 'ACTIVE', NOW(), NOW()),
('test_18', '신나는금요일', 'test18@test.com', 'google', 'gid_18', 'USER', 140, 1000, 'ACTIVE', NOW(), NOW()),
('test_19', '매콤한맛', 'test19@test.com', 'google', 'gid_19', 'USER', 35, 1000, 'ACTIVE', NOW(), NOW()),
('test_20', '조용한산책', 'test20@test.com', 'google', 'gid_20', 'USER', 85, 1000, 'ACTIVE', NOW(), NOW());

 INSERT INTO user_activity_stats (user_id, total_post_count, total_comment_count,
  post_like_received_count, comment_like_received_count, total_like_received_count,
  total_view_received_count, total_notice_interest_count, consecutive_attendance_days,
  max_consecutive_attendance_days, total_attendance_count, review_post_count)
  SELECT id, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 FROM users WHERE id BETWEEN 115 AND 134 AND id NOT IN
  (SELECT user_id FROM user_activity_stats);

-- ============================================================
-- GENERAL (통합광장)
-- ============================================================

-- 임대주택 관련 (생활정보) - 5개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('전세 자금대출 받으면서 공공주택 신청해도 되나?', '지금 전세 구하려고 대출 받는 중인데 공공주택도 신청할 수 있을까? 불이익 있나요?', (SELECT id FROM users WHERE username='test_01'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사비 겁나비싸다는게 왜 지금 느껴지냐', '집 구하니까 인테리어, 이사, 청소 다 돈이 드네 어디서 줄일 수 있을까?', (SELECT id FROM users WHERE username='test_03'), 'GENERAL', '생활정보', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('전세 vs 월세 뭐가 나을까', '공공주택 떨어진 지 2년... 결국 선택해야 하는데 전세가 나을까?', (SELECT id FROM users WHERE username='test_09'), 'GENERAL', '생활정보', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('집계약 전에 꼭 확인할 것들!', '처음 집 구하는거라 떨렸는데 계약 전에 확인할 게 뭐가 있을까?', (SELECT id FROM users WHERE username='test_17'), 'GENERAL', '생활정보', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('인테리어 비용 어떻게 줄였어?', '기숙사에서 나와서 처음 인테리어 하는데 너무 비싼데 팁 있을까?', (SELECT id FROM users WHERE username='test_11'), 'GENERAL', '생활정보', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 테스트/뻘글 (생활정보) - 10개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('우리동네 카페 존좋음 ㅋㅋ', '강남역 7번출구 나와서 5분정도 걸어가면 있는 카페 ☕ 분위기 좋고 커피도 맛있음!', (SELECT id FROM users WHERE username='test_05'), 'GENERAL', '생활정보', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('혼자 이사하려다가 포기했어 ㅋㅋ', '유튜브로 이사팁 봤는데 이거 진짜 힘들긴 힘드네 ㅠㅠ', (SELECT id FROM users WHERE username='test_08'), 'GENERAL', '생활정보', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('마이페이지 기능 좋네욧!', '오 이 앱 진짜 아이디어 괜찮은듯 ㅋㅋ 나의 관심공고 저장이 되다니!!', (SELECT id FROM users WHERE username='test_01'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사후 짐정리 팁 있을까?', '이사는 했는데 짐정리가 답답해서 뭔가 비법이 있나 궁금해', (SELECT id FROM users WHERE username='test_12'), 'GENERAL', '생활정보', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('공과금 절약하는 방법들', '전기세 수도세가 생각보다 많이 나왔어 뭘 할 수 있을까', (SELECT id FROM users WHERE username='test_18'), 'GENERAL', '생활정보', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('새집이니까 그린 식물 들였어 ㅋㅋ', '이사해서 산 첫 식물인데 어떻게 키워야 하지?', (SELECT id FROM users WHERE username='test_16'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('냉장고 선택 어떻게 했어?', '원래 있던 거 놔두고 새 거 살까 고민 중...', (SELECT id FROM users WHERE username='test_20'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('아파트 tv 몇 인치 달아놨어?', '거실이 좀 넓은데 tv 사이즈를 뭘로 할까', (SELECT id FROM users WHERE username='test_10'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('주방용품 뭐 쀼 추천해줘?', '처음으로 주방 차려야 하는데 기본적인 거 뭐가 있어야 할까', (SELECT id FROM users WHERE username='test_13'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('침대 사이즈 뭘로 했어?', '싱글? 더블? 어떤 게 나을까 고민 중이야', (SELECT id FROM users WHERE username='test_07'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 앱평가 (생활정보) - 5개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('이 앱 UI가 진짜 깔끔하네', '기존 사이트보다 훨씬 보기 좋아! 계속 개선되는 것 같고 좋아', (SELECT id FROM users WHERE username='test_06'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('검색 기능이 정말 필요한데', '앱에서 특정 지역만 검색하는 기능이 있으면 좋겠어', (SELECT id FROM users WHERE username='test_14'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('알림 기능은 언제 나오나요?', '공고가 뜨면 자동으로 알려줬으면 좋을 텐데...', (SELECT id FROM users WHERE username='test_19'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('댓글 이모지 반응 추가하면 좋을 것 같은데', '다른 앱처럼 빠른 반응이 가능하면 좋겠어', (SELECT id FROM users WHERE username='test_15'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('다크모드 언제 나오나요?', '밤에 볼 때 눈이 피로해서 다크모드 있으면 좋겠어', (SELECT id FROM users WHERE username='test_04'), 'GENERAL', '생활정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 공고관련 질문 - 3개 (임대주택)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('공고 자격조건 정확히 뭐야?', '무주택인데 왜 자격이 없다고 뜨지? 무조건 자격 있어야 하는거 아이니?', (SELECT id FROM users WHERE username='test_08'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('신청 후 결과까지 얼마나 걸려?', '공고 신청했는데 결과는 언제 나와? 이것도 시간 오래 걸려?', (SELECT id FROM users WHERE username='test_02'), 'GENERAL', '공고관련질문', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('가족수로 공고 자격이 달라져?', '아 공고마다 가족수 조건이 다르네.. 뭐가 뭔지 모르겠어', (SELECT id FROM users WHERE username='test_12'), 'GENERAL', '공고관련질문', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 테스트/뻘글 (공고관련질문) - 4개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('이 앱 버그 자주 있어??', '나만 그런가 자꾸만 로딩이 안 되네? 뭔가 불안해', (SELECT id FROM users WHERE username='test_20'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('여러분은 몇 년 전부터 신청했어요?', '나는 작년부터 시작했는데 다들 언제부터 했어?', (SELECT id FROM users WHERE username='test_02'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('당첨되면 뭐부터 할거야?', '혹시 모르니 당첨되면 뭘 할지 미리 생각해봤어 ㅋㅋ', (SELECT id FROM users WHERE username='test_16'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이앱 오신 지 얼마나 되셨어요?', '최근에 나온 거 같은데 언제 오픈한 거야?', (SELECT id FROM users WHERE username='test_11'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 앱평가 (공고관련질문) - 3개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('서버 안정성 좀 더 개선되길', '가끔 느려지는데 서버 업그레이드 계획 있나요?', (SELECT id FROM users WHERE username='test_09'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('필터링 옵션이 더 많으면 좋을 것 같아', '역세권/신축 외에 다른 조건으로도 필터링하고 싶은데', (SELECT id FROM users WHERE username='test_14'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('비교 기능 있으면 좋을 텐데', '여러 공고를 한눈에 비교할 수 있는 기능이 있으면', (SELECT id FROM users WHERE username='test_03'), 'GENERAL', '공고관련질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 지역정보 - 2개 (임대주택)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('강남 어디가 제일 살기 좋아?', '강남에 살고 싶은데 강남역 서초역 신논현역 뭐가 제일 좋아?', (SELECT id FROM users WHERE username='test_01'), 'GENERAL', '지역정보', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('강동구 교통 어때?', '강동구 신축이 떴는데 강동구는 좀 먼 것 같은데 사는게 가능할까?', (SELECT id FROM users WHERE username='test_10'), 'GENERAL', '지역정보', '서울 강동구', 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 테스트/뻘글 (지역정보) - 4개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('여기 날씨 어떤가요?', '그 지역 날씨는 어떤지 궁금하네', (SELECT id FROM users WHERE username='test_19'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('핫플 추천 많은 지역?', '놀 곳이 많은 지역이 어디야?', (SELECT id FROM users WHERE username='test_15'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('대중교통 좋은 곳이 최고지', '지하철이 많은 곳이 확실히 편해', (SELECT id FROM users WHERE username='test_07'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('시골이랑 도시 뭐가 낫지?', '조용하고 싶긴 한데 불편할까봐 걱정돼', (SELECT id FROM users WHERE username='test_13'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 앱평가 (지역정보) - 4개
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('지역별 상세 정보가 있으면 좋겠어요', '각 지역의 평점, 리뷰 같은 게 있으면 결정하기 쉬울 텐데', (SELECT id FROM users WHERE username='test_06'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('지도 기능이 있으면 좋을 것 같은데', '공고 위치를 지도에서 바로 볼 수 있으면 좋겠어', (SELECT id FROM users WHERE username='test_14'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('편의점, 병원 위치도 표시해주면', '생활 인프라 위치를 한눈에 볼 수 있으면 편할 듯', (SELECT id FROM users WHERE username='test_11'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('날씨 정보도 같이 보여줬으면', '지역 정보할 때 날씨도 함께 표시되면 좋겠어', (SELECT id FROM users WHERE username='test_04'), 'GENERAL', '지역정보', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- ============================================================
-- NOTICE (공고게시판)
-- ============================================================

-- 공고정보 알림 (10개 - 모두 임대주택)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('강동구 신축 공고 떴어!!!', '지금 강동구 신축 아파트 신청기간이래!! 내가 놓치기 싫어서 올림', (SELECT id FROM users WHERE username='test_10'), 'NOTICE', '공고정보', '서울 강동구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('서초동 전세임대 공고 공지', '서초에 전세임대 공고 나왔어!! 관심있으면 지금 신청!', (SELECT id FROM users WHERE username='test_05'), 'NOTICE', '공고정보', '서울 서초구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('LH 행복주택 신청 기간 알림!', '이번주 LH 행복주택 신청시작이라는데 놓치면 안돼!!', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '공고정보', '전국', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('강남역세권 신축 뜼!! 급올림', '강남 역세권 신축이 떴는데 진짜 이런거 자주 안 나와 ㅠ', (SELECT id FROM users WHERE username='test_01'), 'NOTICE', '공고정보', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('마포구 신축 공고 나왔음!', '마포에 신축 단지 공고가 올라왔대!! 확인해봐야겠다', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '공고정보', '서울 마포구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('인천 남동구 전세임대!', '인천 남동구에 전세임대 공고도 떴어요 경기도 분들은 확인!', (SELECT id FROM users WHERE username='test_03'), 'NOTICE', '공고정보', '인천', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('부산 해운대 신축 정보!', '부산 해운대 신축이 뜼!! 역세권이래!!!', (SELECT id FROM users WHERE username='test_14'), 'NOTICE', '공고정보', '부산 해운대구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('경기도 성남 행복주택!', '성남시 신축 행복주택 공고 확인하세요!', (SELECT id FROM users WHERE username='test_17'), 'NOTICE', '공고정보', '경기 성남시', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('서울 동대문 신축 뜼!!', '동대문에도 신축이 나왔네!! 다들 확인했어?', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '공고정보', '서울 동대문구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('송파구 공고 오늘 마감!', '송파 공고가 오늘 마감이래!! 신청 안 했으면 지금!!', (SELECT id FROM users WHERE username='test_18'), 'NOTICE', '공고정보', '서울 송파구', 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 신청팁 - 8개 (임대주택), 2개 (앱평가)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('신청서 작성 팁 좀 알려줘', '신청서가 의외로 복잡하네 뭐부터 해야 하지?', (SELECT id FROM users WHERE username='test_08'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('무주택 증명서는 어디서 받아?', '공고마다 필요한 서류가 다르던데 뭐가 필수야?', (SELECT id FROM users WHERE username='test_12'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('소득증명서 준비하는데 뭘 갖춰야 해?', '급여통장? 세금신고서? 뭐가 필요해?', (SELECT id FROM users WHERE username='test_16'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('신청전 꼭 체크할 것들!', '나는 이것들만 확인하고 신청했어', (SELECT id FROM users WHERE username='test_06'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('서류 준비는 미리 해놓자!', '마감 1주일 전부터 준비해야 한다는 거 알았어?', (SELECT id FROM users WHERE username='test_18'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('온라인 신청 방법 설명해줄래?', '사이트마다 다르더라구 어떻게 해야 하지?', (SELECT id FROM users WHERE username='test_11'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('신청확인증은 꼭 출력하자!', '신청됐는지 확인하는 방법 있나요?', (SELECT id FROM users WHERE username='test_03'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('여러 공고 동시 신청 가능?', '여러 공고에 동시에 신청하면 문제가 되나?', (SELECT id FROM users WHERE username='test_19'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('앱에서 신청 자동저장 되나?', '신청 중에 나갔다가 다시 들어가면 어떻게 되지?', (SELECT id FROM users WHERE username='test_09'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('앱 내에서 신청까지 가능하면 좋겠어', '지금은 외부 사이트로 가야 하는데 앱 내에서 가능하면 편할 듯', (SELECT id FROM users WHERE username='test_14'), 'NOTICE', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 마감확인 (10개 - 모두 임대주택)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('공고 신청 마감 확인했어? 저건 내일이라더..', '여기 공고들 마감시간 조금씩 다르던데 다들 확인했어?', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '마감확인', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('강남 공고 마감 오늘 자정이야!!', '우리 다 신청했어? 남은거 있으면 지금 해!', (SELECT id FROM users WHERE username='test_01'), 'NOTICE', '마감확인', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('서초 전세임대 마감 1주일 남았어!', '놓치면 후회할 공고! 미리 준비하자!', (SELECT id FROM users WHERE username='test_05'), 'NOTICE', '마감확인', '서울 서초구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('LH 신청 이번주 마감 조심!', '이번주 금요일까지니까 준비 미리미리!', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '마감확인', '전국', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('강동 신축 신청 언제까지?', '강동 공고가 언제 마감이야? 문의 많으니까 올려!', (SELECT id FROM users WHERE username='test_10'), 'NOTICE', '마감확인', '서울 강동구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('부산 공고 마감 정보 공유!', '부산 몇 개 공고가 이번주 마감이래', (SELECT id FROM users WHERE username='test_14'), 'NOTICE', '마감확인', '부산', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('인천 공고 신청기간 남은거 있음?', '인천에서 신청할 게 남아있나 확인해봐요', (SELECT id FROM users WHERE username='test_03'), 'NOTICE', '마감확인', '인천', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('서울 공고 한 번에 정리했어!', '현재 신청 가능한 서울 공고들 정리해봤음', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '마감확인', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('경기도 공고 마감 리스트!', '경기도에서 신청할 수 있는 공고들 마감 정보', (SELECT id FROM users WHERE username='test_17'), 'NOTICE', '마감확인', '경기도', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('전국 공고 마감 한번에 보기!', '모든 공고 마감 정보를 모아봤습니다!', (SELECT id FROM users WHERE username='test_04'), 'NOTICE', '마감확인', '전국', 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 조건질문 (10개 - 모두 임대주택)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('이 공고 자격 있을까?', '신혼부부인데 소득이 약간 높은데 되나?', (SELECT id FROM users WHERE username='test_12'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('생애최초 조건 뭐예요?', '생애최초라는게 뭔데 나도 해당되나?', (SELECT id FROM users WHERE username='test_08'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('가구원수 기준이 뭐야?', '가구원수에 부모님도 포함돼?', (SELECT id FROM users WHERE username='test_11'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('자산보유액 제한 있어?', '예금이 많으면 안 되나?', (SELECT id FROM users WHERE username='test_16'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이혼한 사람도 자격있어?', '이혼 후 무주택인데 신청 가능?', (SELECT id FROM users WHERE username='test_09'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('외국인도 신청 가능해?', '한국 시민권이 없는데 신청할 수 있나?', (SELECT id FROM users WHERE username='test_19'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('채무가 있으면 자격이 없어?', '신용불량이면 신청 못 하나?', (SELECT id FROM users WHERE username='test_17'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('면적 기준이 뭐야?', '평수가 작으면 뭐라고 하나?', (SELECT id FROM users WHERE username='test_03'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('나이 제한 있는 공고 있어?', '40대인데 신청할 수 있나?', (SELECT id FROM users WHERE username='test_18'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('유주택인 가족이 있으면 안돼?', '부모님이 집을 가지고 있으면 자격이 없어?', (SELECT id FROM users WHERE username='test_14'), 'NOTICE', '조건질문', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- ============================================================
-- REVIEW (당첨후기)
-- ============================================================

-- 당첨후기 - 8개 (임대주택), 2개 (테스트)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('우와.. 서초에 당첨됐어!!!', '내가 당첨될줄 몰랐는데 진짜 떨렸음 ㅠㅠ 지금 계약금 모으는중..', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '당첨후기', '서울 서초구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('강남역 신축 당첨!! 꿈이 현실이 되다', '3년을 기다렸는데 정말 믿기지 않는다!! 입주예정은 2027년이지만', (SELECT id FROM users WHERE username='test_01'), 'REVIEW', '당첨후기', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('전세임대 당첨!!! 이제 이사가다!', '오 드디어 당첨되니까 심장이 철렁했어 ㅠㅠ 다음달에 이사 가려고 준비중이야', (SELECT id FROM users WHERE username='test_12'), 'REVIEW', '당첨후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('부산 해운대 신축 당첨자 여기요!!', '부산에서 살고 싶었는데 정말 다행이야!! 모두 화이팅!!', (SELECT id FROM users WHERE username='test_14'), 'REVIEW', '당첨후기', '부산 해운대구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('행복주택 당첨!! 꼭 너희도 당첨되길!', '정말 기뻐서 후기 올려봤어 모두 화이팅!!!', (SELECT id FROM users WHERE username='test_16'), 'REVIEW', '당첨후기', '경기도', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('5년 기다렸는데 드디어!!!', '5년을 신청했는데 당첨되니까 모든게 보상받는 기분이야', (SELECT id FROM users WHERE username='test_09'), 'REVIEW', '당첨후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('마포 당첨!!! 축제분위기네요!!', '마포에 당첨돼서 너무 신나요 근처분들 반갑습니다', (SELECT id FROM users WHERE username='test_04'), 'REVIEW', '당첨후기', '서울 마포구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('인천 당첨!! 이제 인천사람이 됩니다!!', '인천에서 살게 되니까 인천 정보도 궁금하네요', (SELECT id FROM users WHERE username='test_03'), 'REVIEW', '당첨후기', '인천', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('떨어졌네.. 다음 기회에 ㅠㅠ', '3개월 신청했는데 벌써 3개 떨어졌어 ㅠ 다음주에 또 나온다던데..', (SELECT id FROM users WHERE username='test_02'), 'REVIEW', '당첨후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('신청 기록 어떻게 정리해요?', '지금까지 몇 개 했는지 정리하는 게 복잡해', (SELECT id FROM users WHERE username='test_20'), 'REVIEW', '당첨후기', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 거주후기 - 8개 (임대주택), 2개 (앱평가)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('23년 당첨 후 1년 거주 후기', '지난 23년도에 당첨됐던 사람인데 물어봐줄게 있으면 댓글 달아줄게', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '거주후기', '경기도', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('행복주택 2년 거주중!! 실제 모습 공개', '요즘 행복주택 신청 많던데 실제로 어떤지 물어보는 사람 많으니 후기', (SELECT id FROM users WHERE username='test_18'), 'REVIEW', '거주후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('공공주택 거주 후 생각 정리', '1년이 지나니까 좋은 점 나쁜 점이 다 보이네요', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '거주후기', '경기도', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('아파트 실제 시설은 어떨까?', '공고로는 알 수 없는 실제 시설들을 소개합니다', (SELECT id FROM users WHERE username='test_15'), 'REVIEW', '거주후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이웃들은 어떤 사람들일까?', '공공주택이라고 하면 궁금한게 이웃 구성인데 실제로는 어떨까요?', (SELECT id FROM users WHERE username='test_18'), 'REVIEW', '거주후기', '경기도', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('관리사무실은 친절할까?', '아파트 관리사무실 서비스 후기 공유합니다', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '거주후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('아이들이 있으면 학교는?', '아이들 학군이 중요한데 실제로 어떨까요?', (SELECT id FROM users WHERE username='test_12'), 'REVIEW', '거주후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('진짜 살기 좋나? 후회 없나?', '공공주택 거주자의 솔직한 감정', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '거주후기', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('앱에서 거주자 커뮤니티 있으면 좋을 것 같은데', '당첨자들끼리 정보 교환할 수 있는 커뮤니티가 있으면', (SELECT id FROM users WHERE username='test_11'), 'REVIEW', '거주후기', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('나의 거주 일기를 기록할 수 있는 기능', '이사 후 적응 기간을 기록하고 싶은데 이런 기능이 있으면', (SELECT id FROM users WHERE username='test_19'), 'REVIEW', '거주후기', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 신청팁 - 8개 (임대주택), 2개 (앱평가)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('당첨 확률을 높이는 방법들!', '내가 당첨된 비결을 알려주겠습니다', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('공고 신청할때 이것만 기억하자!', '내가 당첨되고 깨달은 점들을 정리했어', (SELECT id FROM users WHERE username='test_02'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('떨어지지 않으려면 이렇게 해!', '당첨자들이 공통적으로 하는 것들', (SELECT id FROM users WHERE username='test_14'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('서류 준비는 이렇게 하자!', '당첨 후 필요한 서류들을 미리 준비하면 좋아', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('신청 실수하지 않으려면!', '많은 사람들이 실수하는 부분들', (SELECT id FROM users WHERE username='test_18'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이 공고들은 꼭 신청하세요!', '당첨 확률 높은 공고의 특징', (SELECT id FROM users WHERE username='test_04'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('마지막 순간에 하면 안 돼!', '마감 임박해서 신청하면 안 되는 이유', (SELECT id FROM users WHERE username='test_09'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('자격 다시 한번 체크하자!', '자격 확인 방법 상세 설명', (SELECT id FROM users WHERE username='test_06'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('앱 북마크 기능으로 자주 봐요', '자주 읽는 팁들을 북마크할 수 있으면 좋겠어', (SELECT id FROM users WHERE username='test_14'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('팁 공유 섹션이 따로 있으면 좋을 것 같은데', '당첨팁들을 한 곳에 모아서 볼 수 있으면', (SELECT id FROM users WHERE username='test_07'), 'REVIEW', '신청팁', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- ============================================================
-- PARTNER (하비 파트너스)
-- ============================================================

-- 이사서비스 - 2개 (임대주택), 6개 (테스트), 2개 (앱평가)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('이사비 겁나쌀게 해드릴게요!', '강남서초 전문으로 하고있는데 소형이사부터 대형까지 다 가능합니다', (SELECT id FROM users WHERE username='test_07'), 'PARTNER', '이사서비스', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('전문 이사 업체입니다 - 신축 당첨자들 환영!', '당첨되신 분들 이사 도와드리겠습니다 010-1234-5678', (SELECT id FROM users WHERE username='test_07'), 'PARTNER', '이사서비스', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사할 때 뭐가 제일 중요해?', '처음 이사 준비하는데 우선순위가 뭘까?', (SELECT id FROM users WHERE username='test_11'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사 비용 어디서 받나요?', '이사비 제일 싸게 받는 방법 뭐야?', (SELECT id FROM users WHERE username='test_13'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('박스는 어디서 구해?', '이사 박스가 생각보다 비싸네', (SELECT id FROM users WHERE username='test_15'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('혼자 이사할 수 있을까?', '작은 사이즈라 혼자 할 수 있을까?', (SELECT id FROM users WHERE username='test_08'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사 후 처리할 것들', '버릴 물건들은 어디로 버려?', (SELECT id FROM users WHERE username='test_17'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('경기도 이사 업체 있어?', '경기도 지역 이사 도와주실 분 있나요?', (SELECT id FROM users WHERE username='test_09'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사 후기 기록하는 앱 기능 있으면', '이사 계획과 진행 상황을 기록할 수 있으면 좋을 것 같아', (SELECT id FROM users WHERE username='test_14'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사 비용 비교 기능 있으면 좋을텐데', '여러 업체의 견적을 한눈에 비교할 수 있으면', (SELECT id FROM users WHERE username='test_20'), 'PARTNER', '이사서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 청소서비스 - 2개 (임대주택), 6개 (테스트), 2개 (앱평가)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('홈 클리닝 전문 - 빈집청소 전문입니다', '이사 후 세팅청소 빠르고 꼼꼼하게 도와드려요!', (SELECT id FROM users WHERE username='test_13'), 'PARTNER', '청소서비스', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사 후 청소비 얼마야?', '아무것도 안 한 빈집 청소비는?', (SELECT id FROM users WHERE username='test_10'), 'PARTNER', '청소서비스', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('청소기 추천 해줄래?', '이사 후 청소할 거 사야 하는데 뭘 써야 해?', (SELECT id FROM users WHERE username='test_19'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('화장실 청소 가장 어려운데', '욕실 곰팡이 제거 어떻게 하지?', (SELECT id FROM users WHERE username='test_04'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('새집 냄새 제거하는 방법', '이사 후 새 아파트 냄새가 자꾸만..', (SELECT id FROM users WHERE username='test_06'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('에어컨 청소 꼭 해야 하나?', '에어컨 청소비가 생각보다 비싸네', (SELECT id FROM users WHERE username='test_12'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('청소 용품 어디서 사?', '효율적인 청소 용품 추천 해줄래?', (SELECT id FROM users WHERE username='test_03'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('청소 서비스 비용이 정해져 있나?', '청소비가 왜 다들 다르지?', (SELECT id FROM users WHERE username='test_18'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('청소 업체 평가 기능이 있으면', '실제 이용한 사람들의 후기를 볼 수 있으면', (SELECT id FROM users WHERE username='test_05'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('청소 스케줄 관리 기능', '청소 일정을 앱에서 미리 예약하고 관리할 수 있으면', (SELECT id FROM users WHERE username='test_16'), 'PARTNER', '청소서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

-- 기타서비스 - 2개 (임대주택), 8개 (테스트)
INSERT INTO posts (title, content, user_id, type, category, region_tag, status, view_count, comment_count, like_count, created_at, updated_at) VALUES
('짐 정리 전문가 - 공간 활용 컨설팅', '이사 후 짐 정리로 고민이 많으신가요? 상담은 무료입니다!', (SELECT id FROM users WHERE username='test_13'), 'PARTNER', '기타서비스', '서울 강남구', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('인테리어 상담 도와드려요', '이사 후 인테리어 계획 세우는 거 도와드립니다', (SELECT id FROM users WHERE username='test_11'), 'PARTNER', '기타서비스', '서울', 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('가구 배치 팁 알려줄래?', '좁은 원룸인데 어떻게 배치해야 넓어 보일까?', (SELECT id FROM users WHERE username='test_14'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('세탁기 설치 어렵지 않나요?', '세탁기 설치 비용이 따로 드나요?', (SELECT id FROM users WHERE username='test_08'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('곰팡이 막는 방법', '새집인데 곰팡이가 피면 뭘 해야 하지?', (SELECT id FROM users WHERE username='test_15'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('인터넷 설치 빨리 되나요?', '이사 첫날부터 인터넷 써야 하는데', (SELECT id FROM users WHERE username='test_02'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('베란다 결로 어떻게 하지?', '신축인데 베란다 물이 맺히네', (SELECT id FROM users WHERE username='test_20'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('이사 후 하자 보수', '벽에 흠집이 있으면 뭐라고 하나요?', (SELECT id FROM users WHERE username='test_12'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('중고 가구 어디서 사?', '이사할 때 쓸 저렴한 가구 뭐 있을까?', (SELECT id FROM users WHERE username='test_19'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW()),
('원룸 살리는 팁 알려줄래?', '원룸인데 좀 더 쾌적하게 꾸미려고', (SELECT id FROM users WHERE username='test_06'), 'PARTNER', '기타서비스', NULL, 'ACTIVE', 0, 0, 0, NOW(), NOW());

COMMIT;
