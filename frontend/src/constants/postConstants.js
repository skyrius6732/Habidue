export const CATEGORY_MAP = {
  // 1. 통합광장 (GENERAL)
  GENERAL: [
    { label: '📢 동네소식', value: 'LOCAL_NEWS' },
    { label: '📍 장소추천', value: 'PLACE' },
    { label: '🤝 동네번개', value: 'MEETUP' },
    { label: '☕ 자유수다', value: 'FREE' },
    { label: '📊 서류현황', value: 'STATUS' },
    { label: '💡 기관별팁', value: 'INST_TIPS' },
    { label: '📢 기관문의', value: 'INQUIRY' }
  ],
  // 2. 공고소통방 (NOTICE)
  NOTICE: [
    { label: '📢 공고문의', value: 'INQUIRY' },
    { label: '📝 서류준비', value: 'PREPARE' },
    { label: '🏠 현장사진', value: 'PHOTO' },
    { label: '📊 경쟁률', value: 'COMPETE' }
  ],
  // 3. 당첨후기 (REVIEW)
  REVIEW: [
    { label: '🏆 내집마련기', value: 'SUCCESS_STORY' },
    { label: '🤫 당첨비결', value: 'SECRET' },
    { label: '📝 서류팁', value: 'DOCUMENT' },
    { label: '💰 자금계획', value: 'LOAN' },
    { label: '🏠 사전점검', value: 'MOVE_IN' },
    { label: '🚚 입주후기', value: 'PARTNER_REVIEW' },
    { label: '💡 일반팁', value: 'TIPS' },
    { label: '☕ 자유수다', value: 'FREE' }
  ],
  // 4. 파트너스 (PARTNER)
  PARTNER: [
    { label: '✨ 이용후기', value: 'PARTNER_REVIEW' },
    { label: '📝 견적공유', value: 'ESTIMATE' },
    { label: '✅ 체크리스트', value: 'CHECKLIST' },
    { label: '☕ 자유수다', value: 'FREE' }
  ]
};

/**
 * 카테고리 키(예: FREE, LOCAL_NEWS)를 받아서 아이콘과 한글 레이블을 반환하는 유틸리티 함수
 */
export const getCategoryLabel = (value) => {
  if (!value) return '';
  
  // 모든 게시판 타입을 순회하며 매칭되는 라벨 검색
  for (const boardType in CATEGORY_MAP) {
    const found = CATEGORY_MAP[boardType].find(cat => cat.value === value);
    if (found) return found.label;
  }
  
  // 예외 케이스 처리 (이전 버전 호환성)
  const legacyMap = {
    'QUESTION': '💡 질문/답변',
    'REGION': '🏠 우리동네 소식',
    'POLICY': '📢 정책/뉴스',
    'INTERVIEW': '🏆 당첨자 인터뷰',
    'MOVING': '🚚 이사',
    'CLEANING': '🧼 청소',
    'INTERIOR': '🛋️ 인테리어'
  };
  
  return legacyMap[value] || value; 
};
