<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import axios from '@/plugins/axios'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const loading = ref(false)
const activePeriod = ref('DAILY')
const activeCategory = ref('TOTAL')
const allRankers = ref([])
const lastUpdatedAt = ref('')
const isMobile = ref(window.innerWidth <= 992)

const handleResize = () => { isMobile.value = window.innerWidth <= 992 }

const periods = [
  { name: '일간', value: 'DAILY' },
  { name: '주간', value: 'WEEKLY' },
  { name: '월간', value: 'MONTHLY' },
  { name: '전체', value: 'ALL' }
]

const categories = [
  { name: '🎖️ 종합', value: 'TOTAL' },
  { name: '💡 지식인', value: 'KNOWLEDGE' },
  { name: '✍️ 리뷰왕', value: 'REVIEW' },
  { name: '🔥 열정파', value: 'SINCERITY' }
]

const topRankers = computed(() => allRankers.value.slice(0, 3))
const generalRankers = computed(() => allRankers.value.slice(3, 100))

const fetchRanking = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/ranking', {
      params: { period: activePeriod.value, category: activeCategory.value, limit: 100 }
    });
    const data = res.data.data;
    allRankers.value = (data.rankers || []).filter(r => r.exp > 0);
    lastUpdatedAt.value = data.updatedAt || '';
  } catch (e) {
    console.error('랭킹 로드 실패:', e);
  } finally {
    loading.value = false;
  }
}

watch([activePeriod, activeCategory], fetchRanking)

const getDiffClass = (diff) => {
  if (diff === 'NEW') return 'diff-new'
  const d = parseInt(diff)
  if (d > 0) return 'diff-up'
  if (d < 0) return 'diff-down'
  return 'diff-none'
}

const getDiffText = (diff) => {
  if (diff === 'NEW') return 'NEW'
  const d = parseInt(diff)
  if (d > 0) return `▲${d}`
  if (d < 0) return `▼${Math.abs(d)}`
  return '-'
}

onMounted(() => {
  fetchRanking()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="ranking-board-container">
    <div class="ranking-header">
      <div class="title-area">
        <h2 class="title">🎖️ 활동 랭킹</h2>
        <div class="ranking-update-info">
          <p class="desc">habiDue 커뮤니티의 명예로운 주인공들을 소개합니다.</p>
          <div v-if="lastUpdatedAt" class="update-time-tag">
            <span class="refresh-icon">🔄</span>
            <span class="update-label">10분마다 자동 갱신</span>
            <span class="update-val">({{ lastUpdatedAt }} 기준)</span>
          </div>
        </div>
      </div>
      <div class="filter-controls">
        <div class="period-tabs">
          <button v-for="period in periods" :key="period.value"
            class="p-tab-btn" :class="{ active: activePeriod === period.value }"
            @click="activePeriod = period.value">{{ period.name }}</button>
        </div>
      </div>
    </div>

    <div class="category-tabs-container">
      <button v-for="cat in categories" :key="cat.value"
        class="c-tab-btn" :class="{ active: activeCategory === cat.value }"
        @click="activeCategory = cat.value">{{ cat.name }}</button>
    </div>

    <!-- 랭킹 메인 컨텐츠 -->
    <div class="ranking-main-content" :class="{ 'is-loading': loading, 'glow-active': !loading }">
      <div v-if="loading" class="top-loading-bar">
        <div class="progress-fill"></div>
      </div>

      <!-- 공통 효과 레이어 -->
      <div class="effects-overlay" v-if="!loading">
        <div class="confetti-container">
          <div v-for="n in (isMobile ? 150 : 100)" :key="n" class="confetti" 
               :class="`c-${n%8}`" 
               :style="{ 
                 left: Math.random() * 100 + '%', 
                 animationDelay: (Math.random() * 10) + 's',
                 animationDuration: (10 + Math.random() * 5) + 's'
               }"></div>
        </div>
        <div class="glass-shimmer"></div>
      </div>

      <!-- [PC 전용 시상대] -->
      <div class="pc-ranking-showcase pc-only" :class="{ 'glow-active': !loading }" v-if="(topRankers.length > 0)">
        <div class="podium-wrapper">
            <!-- 2등 -->
            <div class="podium-column rank-2" v-if="topRankers[1]">
              <div class="podium-card">
                <div class="medal-circle">🏆</div>
                <div class="user-avatar-wrap">
                  <AnimatedNickname 
                    :user-id="topRankers[1].userId" 
                    :nickname="topRankers[1].nickname" 
                    :level="topRankers[1].level" 
                    :exp="topRankers[1].exp" 
                    :karma-point="topRankers[1].karmaPoint" 
                    :equipped-badge-name="topRankers[1].equippedBadgeName" 
                    :equipped-effect="topRankers[1].equippedEffect"
                    :show-equipped-effect="topRankers[1].showEquippedEffect"
                    :equipped-tier="topRankers[1].equippedTier"
                    :show-level-effects="topRankers[1].showLevelEffects"                    tooltip-direction="top" 
                  />
                </div>
                <div class="unified-exp-badge rank-2">
                  <span class="val">{{ topRankers[1].exp.toLocaleString() }}</span>
                  <span class="unit">EXP</span>
                </div>
              </div>
              <div class="podium-base base-2">2ND</div>
            </div>

            <!-- 1등 -->
            <div class="podium-column rank-1 main-z-index" v-if="topRankers[0]">
              <div class="podium-card">
                <div class="medal-circle gold">👑</div>
                <div class="user-avatar-wrap main">
                  <AnimatedNickname 
                    :user-id="topRankers[0].userId" 
                    :nickname="topRankers[0].nickname" 
                    :level="topRankers[0].level" 
                    :exp="topRankers[0].exp" 
                    :karma-point="topRankers[0].karmaPoint" 
                    :equipped-badge-name="topRankers[0].equippedBadgeName" 
                    :equipped-effect="topRankers[0].equippedEffect"
                    :show-equipped-effect="topRankers[0].showEquippedEffect"
                    :equipped-tier="topRankers[0].equippedTier"
                    :show-level-effects="topRankers[0].showLevelEffects"                    tooltip-direction="top" 
                  />
                </div>
                <div class="unified-exp-badge rank-1">
                  <span class="val">{{ topRankers[0].exp.toLocaleString() }}</span>
                  <span class="unit">EXP</span>
                </div>
              </div>
              <div class="podium-base base-1">1ST</div>
            </div>

            <!-- 3등 -->
            <div class="podium-column rank-3" v-if="topRankers[2]">
              <div class="podium-card">
                <div class="medal-circle">🏆</div>
                <div class="user-avatar-wrap">
                  <AnimatedNickname 
                    :user-id="topRankers[2].userId" 
                    :nickname="topRankers[2].nickname" 
                    :level="topRankers[2].level" 
                    :exp="topRankers[2].exp" 
                    :karma-point="topRankers[2].karmaPoint" 
                    :equipped-badge-name="topRankers[2].equippedBadgeName" 
                    :equipped-effect="topRankers[2].equippedEffect"
                    :show-equipped-effect="topRankers[2].showEquippedEffect"
                    :equipped-tier="topRankers[2].equippedTier"
                    :show-level-effects="topRankers[2].showLevelEffects"                    tooltip-direction="top" 
                  />
                </div>
                <div class="unified-exp-badge rank-3">
                  <span class="val">{{ topRankers[2].exp.toLocaleString() }}</span>
                  <span class="unit">EXP</span>
                </div>
              </div>
              <div class="podium-base base-3">3RD</div>
            </div>
        </div>
      </div>

      <!-- [모바일 전용 하이라이트 리스트] -->
      <div class="mobile-ranking-top mobile-only" v-if="(topRankers.length > 0)">
        <div v-for="(ranker, idx) in topRankers" :key="idx" class="mobile-top-card" :class="`rank-${idx+1}`">
          <div class="m-rank-num">
            <template v-if="idx === 0">👑</template>
            <template v-else-if="idx === 1">🏆</template>
            <template v-else-if="idx === 2">🏆</template>
            <template v-else>{{ idx + 1 }}</template>
          </div>
          <div class="m-user-info">
            <AnimatedNickname 
              :user-id="ranker.userId" 
              :nickname="ranker.nickname" 
              :level="ranker.level" 
              :exp="ranker.exp" 
              :karma-point="ranker.karmaPoint" 
              :equipped-badge-name="ranker.equippedBadgeName" 
              :equipped-effect="ranker.equippedEffect"
              :show-equipped-effect="ranker.showEquippedEffect"
              :equipped-tier="ranker.equippedTier"
              :show-level-effects="ranker.showLevelEffects"              tooltip-direction="top" 
            />
          </div>
          <div class="unified-exp-badge" :class="`rank-${idx+1}`">
            <span class="val">{{ ranker.exp.toLocaleString() }}</span>
            <span class="unit">EXP</span>
          </div>
        </div>
      </div>

      <!-- 4위 이하 공통 리스트 -->
      <div class="general-ranking-list" v-if="(generalRankers.length > 0)">
        <div v-for="(ranker, idx) in generalRankers" :key="idx" 
             class="ranking-item-row" :class="idx + 4 <= 10 ? 'tier-elite' : (idx + 4 <= 30 ? 'tier-rising' : 'tier-normal')">
          <div class="rank-pos-group">
            <span class="rank-pos">{{ idx + 4 }}</span>
            <span class="rank-diff" :class="getDiffClass(ranker.rankDiff)">{{ getDiffText(ranker.rankDiff) }}</span>
          </div>
          <div class="user-cell">
            <AnimatedNickname 
              :user-id="ranker.userId" 
              :nickname="ranker.nickname" 
              :level="ranker.level" 
              :exp="ranker.exp" 
              :karma-point="ranker.karmaPoint" 
              :equipped-badge-name="ranker.equippedBadgeName" 
              :show-effects="idx + 4 <= 30" 
              :equipped-effect="ranker.equippedEffect"
              :show-equipped-effect="ranker.showEquippedEffect"
              :equipped-tier="ranker.equippedTier"
              :show-level-effects="ranker.showLevelEffects"              :tooltip-direction="isMobile ? 'top' : 'right'" 
            />
          </div>
          <div class="exp-cell">
            <div class="unified-exp-badge" :class="idx + 4 <= 10 ? 'elite' : (idx + 4 <= 30 ? 'rising' : 'normal')">
              <span class="val">{{ ranker.exp.toLocaleString() }}</span>
              <span class="unit">EXP</span>
            </div>
            <span class="exp-label">{{ activePeriod === 'DAILY' ? '오늘' : activePeriod === 'WEEKLY' ? '주간' : activePeriod === 'MONTHLY' ? '월간' : '누적' }}</span>
          </div>
        </div>
      </div>

      <!-- 데이터 없음 -->
      <div v-if="(!loading && allRankers.length === 0)" class="empty-ranking">
        <span class="empty-icon">🎖️</span>
        <p>이 기간에는 아직 활동한 유저가 없습니다.<br>여러분이 주인공이 되어보세요!</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ranking-board-container { display: flex; flex-direction: column; gap: 20px; width: 100%; min-height: 600px; }
.ranking-header { display: flex; justify-content: space-between; align-items: flex-end; padding: 0 5px 15px; border-bottom: 2px solid var(--border-color); flex-wrap: wrap; gap: 15px; }
.title-area .title { font-size: 1.6rem; font-weight: 950; color: var(--text-primary); margin: 0; }
.ranking-update-info { display: flex; flex-direction: column; gap: 4px; }
.title-area .desc { font-size: 0.95rem; color: var(--text-secondary); margin: 6px 0 0; font-weight: 500; }

.update-time-tag { 
  display: inline-flex; align-items: center; gap: 6px; 
  background: var(--hover-bg); padding: 4px 10px; border-radius: 8px; 
  font-size: 0.72rem; color: var(--link-color); font-weight: 800;
  width: fit-content;
}
.refresh-icon { font-size: 0.8rem; animation: rotate-spin 10s linear infinite; }
@keyframes rotate-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.update-val { opacity: 0.7; font-weight: 600; margin-left: 2px; }

.pc-only { display: block !important; }
.mobile-only { display: none !important; }

.period-tabs { display: flex; background: var(--hover-bg); padding: 4px; border-radius: 14px; gap: 6px; }
.p-tab-btn { border: none; background: none; padding: 8px 20px; border-radius: 10px; font-size: 0.85rem; font-weight: 800; color: var(--text-secondary); cursor: pointer; transition: all 0.25s; }
.p-tab-btn.active { background: var(--card-bg); color: var(--link-color); box-shadow: 0 4px 15px rgba(0,0,0,0.1); transform: translateY(-1px); }

.category-tabs-container { display: flex; gap: 12px; padding: 15px 5px; overflow-x: auto; scrollbar-width: none; margin-bottom: 5px; -webkit-overflow-scrolling: touch; }
.c-tab-btn { border: 1.5px solid var(--border-color); background: var(--card-bg); padding: 10px 22px; border-radius: 25px; font-size: 0.9rem; font-weight: 850; color: var(--text-secondary); cursor: pointer; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); white-space: nowrap; box-shadow: 0 2px 8px rgba(0,0,0,0.03); }
.c-tab-btn.active { background: #3498db; color: white; border-color: #3498db; box-shadow: 0 6px 16px rgba(52, 152, 219, 0.35); transform: translateY(-3px); }

.ranking-main-content { background: var(--card-bg); border-radius: 30px; position: relative; padding-bottom: 40px; transition: all 0.3s; }
.ranking-main-content.is-loading { opacity: 0.6; pointer-events: none; }
.top-loading-bar { position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: rgba(52, 152, 219, 0.1); z-index: 100; overflow: hidden; }
.progress-fill { width: 40%; height: 100%; background: #3498db; animation: loading-slide 1s infinite ease-in-out; border-radius: 2px; }

.glow-active { animation: glow-pulse 2s infinite ease-in-out; }
@keyframes glow-pulse {
  0% { box-shadow: 0 0 5px rgba(52, 152, 219, 0.1); }
  50% { box-shadow: 0 0 25px rgba(52, 152, 219, 0.2); background-color: rgba(52, 152, 219, 0.01); }
  100% { box-shadow: 0 0 5px rgba(52, 152, 219, 0.1); }
}

.effects-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  pointer-events: none; z-index: 5;
  overflow: hidden; border-radius: 30px;
}

.confetti-container { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.confetti {
  position: absolute; width: 10px; height: 10px;
  top: -30px; opacity: 0;
  animation: confetti-fall linear infinite;
}
@keyframes confetti-fall {
  0% { transform: translateY(0) rotate(0deg); opacity: 0; }
  5% { opacity: 1; }
  95% { opacity: 1; }
  100% { transform: translateY(4000px) rotate(720deg); opacity: 0; }
}

.glass-shimmer {
  position: absolute; top: 0; left: -150%; width: 100%; height: 100%;
  background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.02), transparent);
  transform: skewX(-25deg);
  animation: shimmer-swipe 7s infinite ease-in-out;
  z-index: 10;
}
@keyframes shimmer-swipe { 0% { left: -150%; } 25% { left: 150%; } 100% { left: 150%; } }

.c-0 { background: #f1c40f; }
.c-1 { background: #e74c3c; }
.c-2 { background: #3498db; }
.c-3 { background: #2ecc71; }
.c-4 { background: #9b59b6; }
.c-5 { background: #FFD700; }
.c-6 { background: #FF69B4; }
.c-7 { background: #00FFFF; }

.pc-ranking-showcase { padding: 180px 20px 40px; background: linear-gradient(to bottom, var(--hover-bg), transparent); border-radius: 30px 30px 0 0; position: relative; z-index: 20; }
.pc-ranking-showcase :deep(.nickname-tooltip) { z-index: 999999 !important; }
.rank-1.main-z-index { z-index: 100 !important; position: relative; }

.podium-wrapper { display: flex; justify-content: center; align-items: flex-end; position: relative; z-index: 30; }
.podium-column { display: flex; flex-direction: column; align-items: center; width: 220px; }
.podium-card { display: flex; flex-direction: column; align-items: center; gap: 15px; margin-bottom: 10px; }
.medal-circle { font-size: 3rem; filter: drop-shadow(0 5px 10px rgba(0,0,0,0.15)); }
.user-avatar-wrap { transform: scale(1.3); margin: 10px 0; }
.user-avatar-wrap.main { transform: scale(1.6); }

.podium-base { width: 100%; display: flex; align-items: center; justify-content: center; font-weight: 950; font-size: 1.2rem; color: rgba(255,255,255,0.8); border-radius: 15px 15px 0 0; }
.base-1 { height: 160px; background: linear-gradient(to bottom, #ffd700, #b8860b); font-size: 2rem; }
.base-2 { height: 110px; background: linear-gradient(to bottom, #c0c0c0, #708090); }
.base-3 { height: 80px; background: linear-gradient(to bottom, #cd7f32, #8b4513); }

.general-ranking-list { position: relative; z-index: 20; display: flex; flex-direction: column; padding: 10px 40px; gap: 0; }
.ranking-item-row { position: relative; z-index: 25; display: flex; align-items: center; padding: 12px 20px; border-bottom: 1px solid var(--border-color); }
.ranking-item-row:last-child { border-bottom: none; }

.user-cell { flex: 1; min-width: 0; padding-left: 10px; }
.exp-cell { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; width: 130px; }
.exp-label { font-size: 0.62rem; font-weight: 850; color: var(--text-muted); }

.unified-exp-badge { 
  display: flex; align-items: baseline; gap: 4px; white-space: nowrap;
  padding: 4px 14px; border-radius: 20px; border: 1.5px solid var(--border-color); 
  background: var(--hover-bg); transition: all 0.3s;
}
.unified-exp-badge .val { font-size: 0.9rem; font-weight: 950; color: inherit; }
.unified-exp-badge .unit { font-size: 0.65rem; font-weight: 850; color: inherit; opacity: 0.8; }

.unified-exp-badge.rank-1 { background: #FFD700 !important; border-color: #DAA520 !important; color: #000000 !important; }
.unified-exp-badge.rank-2 { background: #C0C0C0 !important; border-color: #A9A9A9 !important; color: #000000 !important; }
.unified-exp-badge.rank-3 { background: #CD7F32 !important; border-color: #8B4513 !important; color: #FFFFFF !important; }
.unified-exp-badge.elite { background: #F1C40F !important; border-color: #D4AC0D !important; color: #000000 !important; }
.unified-exp-badge.rising { background: #3498DB !important; border-color: #2980B9 !important; color: #FFFFFF !important; }

.rank-pos-group { width: 80px; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.rank-pos { font-weight: 950; line-height: 1; font-size: 1.1rem; color: var(--text-primary); }
.rank-diff { font-size: 0.7rem; font-weight: 850; padding: 2px 6px; border-radius: 6px; }
.diff-up { color: #e74c3c; background: rgba(231, 76, 60, 0.08); }
.diff-down { color: #3498db; background: rgba(52, 152, 219, 0.08); }
.diff-new { background: linear-gradient(45deg, #f1c40f, #e67e22); color: white; transform: scale(0.9); font-weight: 950; }

@media (max-width: 992px) {
  .confetti { width: 3.5px; height: 3.5px; }
  .pc-only { display: none !important; }
  .mobile-only { display: block !important; }
  .pc-ranking-showcase { padding-top: 50px; }
  .mobile-ranking-top { position: relative; z-index: 20; display: flex; flex-direction: column !important; gap: 20px !important; padding: 35px 15px; background: linear-gradient(to bottom, var(--hover-bg), transparent); border-radius: 30px 30px 0 0; }
  .mobile-top-card { position: relative; z-index: 25; display: flex; align-items: center; gap: 15px; border-radius: 24px; background: var(--card-bg); border: 2.5px solid var(--border-color); transition: all 0.3s; }
  .mobile-top-card.rank-1 { padding: 28px 18px; border-color: #ffd700; box-shadow: 0 15px 35px rgba(255,215,0,0.2); }
  .mobile-top-card.rank-2 { padding: 22px 18px; border-color: #c0c0c0; }
  .mobile-top-card.rank-3 { padding: 18px 18px; border-color: #cd7f32; }
  .m-rank-num { font-weight: 950; min-width: 40px; text-align: center; }
  .mobile-top-card.rank-1 .m-rank-num { color: #b8860b; font-size: 2.2rem; }
  .mobile-top-card.rank-2 .m-rank-num { color: #7f8c8d; font-size: 1.8rem; }
  .mobile-top-card.rank-3 .m-rank-num { color: #a0522d; font-size: 1.5rem; }
  .m-user-info { flex: 1; min-width: 0; }
  .general-ranking-list { padding: 10px 10px; }
  .rank-pos-group { width: 45px; }
  
  .unified-exp-badge { padding: 1px 4px; gap: 2px; border-width: 1px; }
  .unified-exp-badge .val { font-size: 0.45rem; }
  .unified-exp-badge .unit { font-size: 0.3rem; }
  .exp-cell { width: 60px; }
}

.empty-ranking {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 14px;
  color: #9ca3af;
}
.empty-ranking .empty-icon {
  font-size: 3rem;
  line-height: 1;
  filter: grayscale(20%);
}
.empty-ranking p {
  margin: 0;
  text-align: center;
  font-size: 0.95rem;
  line-height: 1.7;
  color: #6b7280;
}
</style>
