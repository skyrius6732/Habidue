-- 기존 카르마 점수를 10배로 확장하여 0.1 단위 정밀도 확보
UPDATE users SET karma_point = karma_point * 10;
UPDATE karma_history SET point_change = point_change * 10, resulting_point = resulting_point * 10;
