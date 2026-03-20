package com.habidue.app.domain.tag;

public enum TagType {
    METRO,       // 광역 (서울, 경기 등)
    CITY_COUNTY,  // 시/군/구 (충주, 강남구 등)
    SUBWAY_LINE, // 지하철 호선 (1호선, 신분당선 등)
    STATION,     // 지하철역 (강남역, 판교역 등)
    REGION,      // 기타 지역 (기존 호환용)
    TYPE,        // 공급 유형
    TARGET,      // 모집 대상
    SPECIAL,     // 특수 목적
    PROVIDER,    // 기관
    SYSTEM,      // 시스템 상태 (접수중 등)
    COMPLEX,     // 단지명 (아파트, 빌라 등 특정 주거 단지)
    IGNORE       // 지명 오탐지 방어용 무시 단어
}
