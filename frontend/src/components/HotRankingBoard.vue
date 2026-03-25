<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import axios from '@/plugins/axios'

const hotNotices = ref([])
const updatedAt = ref('')
const loading = ref(false)
const timer = ref(null)
const isExpanded = ref(true)

const fetchHotRankings = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/ranking/hot-notices', { params: { limit: 10 } })
    hotNotices.value = res.data.data.notices
    updatedAt.value = res.data.data.updatedAt
  } catch (e) {
    console.error('급상승 랭킹 로드 실패:', e)
  } finally {
    loading.value = false
  }
}

const calculateDDay = (d) => {
  if (!d) return '상시'
  const diff = Math.ceil((new Date(d).setHours(0,0,0,0) - new Date().setHours(0,0,0,0)) / 86400000)
  return diff === 0 ? 'D-Day' : diff < 0 ? '마감' : `D-${diff}`
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const topOneTitle = computed(() => {
  if (hotNotices.value.length > 0) return hotNotices.value[0].title
  return '집계 중...'
})

onMounted(() => {
  fetchHotRankings()
  timer.value = setInterval(fetchHotRankings, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
})
</script>

<template>
  <div class="hot-ranking-card" :class="{ 'is-collapsed': !isExpanded }">
    <div class="card-header" @click="toggleExpand">
      <div class="header-left">
        <div class="header-title">
          <span class="fire-icon">🔥</span>
          <h3>실시간 급상승 공고 <span class="top-tag">TOP 10</span></h3>
        </div>
        <Transition name="fade-slide">
          <div v-if="!isExpanded && hotNotices.length > 0" class="collapsed-info">
            <span class="c-rank">1위</span>
            <span class="c-title">{{ topOneTitle }}</span>
          </div>
        </Transition>
      </div>
      <div class="header-right">
        <span class="update-time" v-if="updatedAt && isExpanded">{{ updatedAt }} 갱신</span>
        <button class="btn-toggle-expand">
          <span class="arrow-icon">{{ isExpanded ? '▲' : '▼' }}</span>
        </button>
      </div>
    </div>

    <Transition name="expand">
      <div v-show="isExpanded" class="ranking-list-wrapper">
        <div class="ranking-list" :class="{ 'is-loading': loading }">
          <router-link 
            v-for="(notice, index) in hotNotices" 
            :key="notice.id" 
            :to="`/notices?openId=${notice.id}`"
            class="ranking-item"
          >
            <div class="rank-badge" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
            
            <div class="notice-info">
              <div class="info-top">
                <span class="source-tag" :class="notice.source">{{ notice.source === 'PRIVATE' ? '민간' : notice.source }}</span>
                <span class="d-day" :class="{ 'imminent': calculateDDay(notice.deadline).includes('D-') && parseInt(calculateDDay(notice.deadline).replace('D-','')) <= 3 }">
                  {{ calculateDDay(notice.deadline) }}
                </span>
              </div>
              <p class="notice-title">{{ notice.title }}</p>
            </div>

            <!-- 인기도 영역 및 툴팁 -->
            <div class="score-area">
              <div class="score-tooltip-container">
                <span class="score-val">{{ Math.round(notice.score) }}</span>
                <span class="score-label">인기도</span>
                
                <div class="custom-tooltip">
                  <div class="tooltip-row">
                    <span class="t-icon">👀</span>
                    <span class="t-label">조회</span>
                    <span class="t-val">{{ notice.viewCount || 0 }}회</span>
                  </div>
                  <div class="tooltip-row">
                    <span class="t-icon">❤️</span>
                    <span class="t-label">관심</span>
                    <span class="t-val">{{ notice.interestCount || 0 }}명</span>
                  </div>
                  <div class="tooltip-row">
                    <span class="t-icon">💬</span>
                    <span class="t-label">소통</span>
                    <span class="t-val">{{ notice.postCount || 0 }}개</span>
                  </div>
                  <div class="tooltip-arrow"></div>
                </div>
              </div>
            </div>
          </router-link>

          <div v-if="!loading && hotNotices.length === 0" class="empty-state">
            <p>현재 집계 중인 공고가 없습니다.</p>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.hot-ranking-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  margin-bottom: 20px;
  position: relative;
  z-index: 1001; /* 최상위 레이어 경쟁력 확보 */
}

.card-header {
  padding: 15px 18px;
  background: linear-gradient(to right, rgba(231, 76, 60, 0.05), transparent);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  border-radius: 16px 16px 0 0;
  user-select: none;
}
.is-collapsed .card-header { border-bottom-color: transparent; border-radius: 16px; }

.header-left { display: flex; align-items: center; gap: 15px; flex: 1; min-width: 0; }

@media (max-width: 768px) {
  .header-left { flex-direction: column; align-items: flex-start; gap: 4px; }
  .collapsed-info { margin-left: 28px; width: calc(100% - 28px); }
}

.header-title { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.header-title h3 { font-size: 1rem; font-weight: 800; margin: 0; color: var(--text-primary); }
.top-tag { font-size: 0.7rem; color: #e74c3c; background: rgba(231, 76, 60, 0.1); padding: 2px 6px; border-radius: 4px; margin-left: 4px; }

.collapsed-info { display: flex; align-items: center; gap: 8px; min-width: 0; }
.c-rank { font-size: 0.75rem; font-weight: 900; color: #e74c3c; white-space: nowrap; }
.c-title { font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.fire-icon { font-size: 1.2rem; animation: fire-pulse 1.5s infinite alternate; }
@keyframes fire-pulse { from { transform: scale(1); } to { transform: scale(1.15); } }

.header-right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 0.7rem; color: var(--text-secondary); font-weight: 600; opacity: 0.8; }

.btn-toggle-expand {
  background: var(--hover-bg);
  border: 1px solid var(--border-color);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
}

.ranking-list-wrapper { border-radius: 0 0 16px 16px; /* overflow: hidden을 제거하여 툴팁 노출 보장 */ }
.ranking-list { 
  display: grid; 
  grid-template-columns: repeat(2, 1fr); 
  background: var(--border-color);
  gap: 1px; 
}
@media (max-width: 768px) { .ranking-list { grid-template-columns: 1fr; } }

.ranking-item {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  text-decoration: none;
  background: var(--card-bg);
  transition: all 0.2s;
  gap: 12px;
  min-width: 0;
  position: relative;
}
.ranking-item:hover { background: var(--hover-bg); z-index: 1002; }

.rank-badge { font-size: 1.1rem; font-weight: 900; min-width: 28px; text-align: center; color: var(--text-secondary); flex-shrink: 0; }
.rank-1 { color: #e74c3c; } .rank-2 { color: #e67e22; } .rank-3 { color: #f1c40f; }

.notice-info { flex: 1; min-width: 0; }
.info-top { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.source-tag { font-size: 0.65rem; font-weight: 800; padding: 2px 6px; border-radius: 4px; background: var(--hover-bg); color: var(--text-secondary); border: 1px solid var(--border-color); flex-shrink: 0; }
.source-tag.LH { color: #38a169; } .source-tag.SH { color: #3182ce; } .source-tag.PRIVATE { color: #c08457; }
.d-day { font-size: 0.7rem; font-weight: 800; color: var(--text-secondary); flex-shrink: 0; }
.d-day.imminent { color: #e74c3c; }
.notice-title { font-size: 0.88rem; font-weight: 650; color: var(--text-primary); margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.score-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 45px;
  flex-shrink: 0;
  position: relative;
}
.score-tooltip-container { display: flex; flex-direction: column; align-items: center; cursor: help; }
.score-val { font-size: 0.95rem; font-weight: 900; color: var(--link-color); }
.score-label { font-size: 0.6rem; font-weight: 700; color: var(--text-muted); }

.custom-tooltip {
  position: absolute;
  bottom: 135%;
  right: -10px;
  background: rgba(33, 37, 41, 0.98);
  backdrop-filter: blur(8px);
  color: white;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 0.75rem;
  white-space: nowrap;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  z-index: 9999;
  opacity: 0;
  visibility: hidden;
  transform: translateY(10px);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid rgba(255,255,255,0.1);
}

.score-area:hover .custom-tooltip { opacity: 1; visibility: visible; transform: translateY(0); }
.tooltip-row { display: flex; align-items: center; gap: 10px; }
.t-icon { font-size: 0.85rem; width: 16px; text-align: center; }
.t-label { font-weight: 600; color: rgba(255,255,255,0.6); }
.t-val { font-weight: 800; color: #fff; margin-left: auto; }
.tooltip-arrow { position: absolute; top: 100%; right: 25px; border-left: 7px solid transparent; border-right: 7px solid transparent; border-top: 7px solid rgba(33, 37, 41, 0.98); }

.empty-state { grid-column: span 2; padding: 30px; text-align: center; color: var(--text-muted); }

.expand-enter-active, .expand-leave-active { transition: all 0.3s ease; max-height: 800px; }
.expand-enter-from, .expand-leave-to { max-height: 0; opacity: 0; overflow: hidden; }
</style>
