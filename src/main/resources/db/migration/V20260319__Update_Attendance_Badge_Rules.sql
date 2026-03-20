-- 출석 배지(ATTENDANCE) 규칙을 하이브리드 포인트 점수 체계로 전환
UPDATE badge_level_rules SET requiredValue = 5   WHERE badgeType = 'ATTENDANCE' AND level = 1;
UPDATE badge_level_rules SET requiredValue = 15  WHERE badgeType = 'ATTENDANCE' AND level = 5;
UPDATE badge_level_rules SET requiredValue = 40  WHERE badgeType = 'ATTENDANCE' AND level = 10;
UPDATE badge_level_rules SET requiredValue = 100 WHERE badgeType = 'ATTENDANCE' AND level = 20;
UPDATE badge_level_rules SET requiredValue = 250 WHERE badgeType = 'ATTENDANCE' AND level = 50;
UPDATE badge_level_rules SET requiredValue = 500 WHERE badgeType = 'ATTENDANCE' AND level = 70;
UPDATE badge_level_rules SET requiredValue = 800 WHERE badgeType = 'ATTENDANCE' AND level = 90;
UPDATE badge_level_rules SET requiredValue = 1200 WHERE badgeType = 'ATTENDANCE' AND level = 100;
