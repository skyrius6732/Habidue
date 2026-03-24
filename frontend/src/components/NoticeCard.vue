<script setup>
import { computed } from 'vue'

const props = defineProps({
  notice: Object,
  userKeywords: Array,
  activeMenuId: Number
})

const emit = defineEmits(['toggle-favorite', 'open-detail', 'toggle-menu', 'copy-link', 'share-link'])

const sourceColor = computed(() => {
  switch (props.notice.source) {
    case 'LH': return '#38a169';
    case 'SH': return '#3182ce';
    case 'PRIVATE': return '#c08457';
    default: return '#4c51bf';
  }
})

const formatDateShortDot = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return `${String(date.getFullYear()).slice(-2)}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

const getAllTags = (notice) => {
  if (!notice.tags || notice.tags.length === 0) return [];
  
  // 1. 시스템 태그(상태값) 제외
  const systemTags = ['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내'];
  let allFiltered = notice.tags.filter(tag => !systemTags.includes(tag.name));

  // 2. 우선순위 기반 정렬
  allFiltered.sort((a, b) => {
    // 우선순위 1: 사용자 관심 키워드(userKeywords) 매칭 여부
    const aMatch = props.userKeywords?.includes(a.name);
    const bMatch = props.userKeywords?.includes(b.name);
    if (aMatch && !bMatch) return -1;
    if (!aMatch && bMatch) return 1;

    // 우선순위 2: '역세권', '신축' 등 SPECIAL 타입 태그
    const aSpecial = a.type === 'SPECIAL';
    const bSpecial = b.type === 'SPECIAL';
    if (aSpecial && !bSpecial) return -1;
    if (!aSpecial && bSpecial) return 1;

    // 우선순위 3: 지하철역(STATION) 태그 또는 명칭이 '역'으로 끝나는 경우
    const aStation = a.type === 'STATION' || a.name?.endsWith('역');
    const bStation = b.type === 'STATION' || b.name?.endsWith('역');
    if (aStation && !bStation) return -1;
    if (!aStation && bStation) return 1;

    return 0;
  });
  
  return allFiltered.slice(0, 6); // 최대 6개까지 노출
}

const getStatus = (notice) => {
  const systemTag = notice.tags?.find(t => ['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내', '예정'].includes(t.name));
  const tagName = systemTag ? systemTag.name : '';

  // [시니어 조치] 태그 정보를 최우선으로 반영 (데이터 일관성 확보)
  if (tagName === '접수중') return { text: '접수중', class: 'open' };
  if (tagName === '마감') return { text: '마감', class: 'closed' };
  if (tagName === '결과발표') return { text: '결과발표', class: 'result' };
  if (tagName === '발표완료') return { text: '발표완료', class: 'closed' };
  if (tagName === '안내') return { text: '안내', class: 'info' };
  if (tagName === '이전안내') return { text: '이전안내', class: 'closed' };
  if (tagName === '예정') return { text: '예정', class: 'upcoming' };

  // 태그가 없는 경우 기존 status 필드 백업 로직 유지
  switch (notice.status) {
    case 'RECRUITING': return { text: '접수중', class: 'open' };
    case 'RESULT': return { text: '결과발표', class: 'result' };
    case 'INFO': return { text: '안내', class: 'info' };
    case 'CLOSED': return { text: '마감', class: 'closed' };
    case 'UPCOMING': return { text: '예정', class: 'upcoming' };
    default: return { text: '안내', class: 'info' };
  }
}

const calculateDDay = (notice) => {
  if (notice.status === 'RESULT' || notice.status === 'INFO' || !notice.deadline) {
    return null;
  }
  const target = new Date(notice.deadline);
  const today = new Date();
  target.setHours(0,0,0,0); today.setHours(0,0,0,0);
  const diff = Math.ceil((target - today) / 86400000);
  return diff === 0 ? 'D-Day' : diff < 0 ? `D+${Math.abs(diff)}` : `D-${diff}`;
}

const isNew = computed(() => {
  if (!props.notice.createdAt) return false;
  const created = new Date(props.notice.createdAt);
  const now = new Date();
  return (now - created) < (48 * 60 * 60 * 1000); // 48시간 이내
});
</script>

<template>
  <div class="card-item" :style="{ borderLeftColor: sourceColor }" @click="$emit('open-detail', notice)">
    <div v-if="isNew" class="new-badge">NEW</div>
    <div class="card-header">
      <div class="header-left-group">
        <div class="source-badge" :class="notice.source">{{ notice.source === 'PRIVATE' ? '민간' : notice.source }}</div>
        <div class="status-group">
          <span class="status-badge" :class="getStatus(notice).class">{{ getStatus(notice).text }}</span>
          <span v-if="calculateDDay(notice)" class="sep">|</span>
          <span class="dday-text" v-if="calculateDDay(notice)">{{ calculateDDay(notice) }}</span>
        </div>
      </div>
      
      <div class="header-right-actions" @click.stop>
        <button @click="$emit('toggle-favorite', notice)" class="pill-favorite-btn">
          <span v-if="notice.isFavorite" class="heart-icon filled">❤️</span>
          <span v-else class="heart-icon">🤍</span>
          <span class="count">{{ notice.interestCount }}</span>
        </button>
        <div class="menu-container">
          <button @click="$emit('toggle-menu', notice.id)" class="menu-btn">•••</button>
          <div v-if="activeMenuId === notice.id" class="dropdown-menu">
            <button @click="$emit('copy-link', notice.link)">공고 링크 복사</button>
            <button @click="$emit('share-link', notice)">공고 공유하기</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card-body">
      <h3 class="notice-title">{{ notice.title }}</h3>
      <div class="tag-cloud">
        <span v-for="tag in getAllTags(notice)" :key="tag.name" class="tag-pill" :class="{ matched: userKeywords.includes(tag.name) }">
          #{{ tag.name }}
        </span>
        <span v-if="notice.tags && notice.tags.filter(t => !['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내'].includes(t.name)).length > 6" class="more-tag-count">
          외 {{ notice.tags.filter(t => !['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내'].includes(t.name)).length - 6 }}개
        </span>
      </div>
    </div>

    <div class="card-footer">
      <div class="date-info">
        <span class="date-item" :class="{ bold: !(notice.deadline || notice.resultDate) }">
          {{ formatDateShortDot(notice.announcementDate || notice.createdAt) }}
        </span>
        <template v-if="notice.deadline || notice.resultDate">
          <span class="date-sep">~</span>
          <span class="date-item deadline">
            {{ formatDateShortDot(notice.deadline || notice.resultDate) }}
          </span>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-item {
  background-color: var(--card-bg);
  border: 1px solid var(--border-color);
  border-left: 4px solid #ccc;
  border-radius: 12px;
  padding: 14px;
  margin: 6px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s ease;
  cursor: pointer;
  position: relative;
  overflow: visible;
}

.new-badge {
  position: absolute;
  top: -8px;
  left: -8px;
  background-color: #ed4956;
  color: white;
  font-size: 0.6rem;
  font-weight: 900;
  padding: 2px 6px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(237, 73, 86, 0.3);
  z-index: 10;
}

.card-header { display: flex; justify-content: space-between; align-items: center; position: relative; }
.header-left-group { display: flex; align-items: center; gap: 8px; }

.source-badge { font-size: 0.65rem; font-weight: 900; padding: 2px 6px; border-radius: 4px; color: white; }
.source-badge.LH { background-color: #38a169; }
.source-badge.SH { background-color: #3182ce; }
.source-badge.PRIVATE { background-color: #c08457; }

.status-group { display: flex; align-items: center; gap: 5px; }
.status-badge { font-size: 0.65rem; font-weight: 700; padding: 2px 8px; border-radius: 20px; }
.status-badge.open { background-color: rgba(56, 161, 105, 0.1); color: #38a169; }
.status-badge.result { background-color: rgba(107, 70, 193, 0.1); color: #6b46c1; }
.status-badge.info { background-color: rgba(49, 130, 206, 0.1); color: #3182ce; }
.status-badge.closed { background-color: rgba(237, 73, 86, 0.1); color: #ed4956; }
.status-badge.upcoming { background-color: #efefef; color: #8e8e8e; }

.dday-text { font-size: 0.7rem; font-weight: 800; color: var(--text-primary); }
.sep { font-size: 0.65rem; color: var(--border-color); }

.header-right-actions { display: flex; align-items: center; gap: 6px; position: relative; }
.pill-favorite-btn { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 3px 8px; border-radius: 20px; cursor: pointer; display: flex; align-items: center; gap: 4px; color: var(--text-primary); }
.heart-icon { font-size: 0.9rem; }
.pill-favorite-btn .count { font-size: 0.7rem; font-weight: 700; }

.menu-container { position: relative; }
.menu-btn { background: none; border: none; padding: 4px; color: var(--text-secondary); cursor: pointer; font-size: 0.9rem; }

.dropdown-menu {
  position: absolute;
  bottom: 100%;
  right: 0;
  background: var(--header-bg);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 -4px 24px rgba(0,0,0,0.2);
  z-index: 9999;
  width: 130px; /* 110px에서 130px로 확대 */
  overflow: hidden;
  margin-bottom: 8px;
}
.dropdown-menu button {
  width: 100%; padding: 12px 8px; background: none; border: none; border-bottom: 1px solid var(--divider-color);
  font-size: 0.75rem; color: var(--text-primary); text-align: center; cursor: pointer; font-weight: 600;
}
.dropdown-menu button:last-child { border-bottom: none; }

.card-body { flex-grow: 1; display: flex; flex-direction: column; gap: 8px; }
.notice-title { font-size: 0.95rem; font-weight: 700; line-height: 1.4; color: var(--text-primary); margin: 0; word-break: keep-all; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 5px; }
.tag-pill { font-size: 0.7rem; color: var(--text-secondary); background-color: var(--hover-bg); padding: 1px 6px; border-radius: 4px; }
.tag-pill.matched { color: var(--link-color); font-weight: 700; }
.more-tag-count { font-size: 0.65rem; color: var(--text-secondary); opacity: 0.7; align-self: center; margin-left: 2px; }

.card-footer { margin-top: 2px; padding-top: 10px; border-top: 1px solid var(--divider-color); }
.date-info { display: flex; align-items: center; gap: 4px; font-size: 0.75rem; color: var(--text-secondary); }
.date-item.bold { font-weight: 800; color: var(--text-primary); }
.date-item.deadline { font-weight: 700; color: var(--text-primary); }
.date-sep { opacity: 0.5; }

@media (max-width: 768px) {
  .card-item { padding: 12px; margin: 6px 12px; }
}
</style>
