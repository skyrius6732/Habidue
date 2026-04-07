<template>
  <div class="activity-tab-content fade-in">
    <div class="section-header-flex">
      <div class="text-group">
        <h2 class="section-title">📊 활동 지표</h2>
        <p class="section-desc">habiDue 커뮤니티에서 쌓아온 소중한 기록들입니다.</p>
      </div>
      <button class="btn-sync-test" @click="$emit('sync-activity')" :disabled="isSyncing">
        {{ isSyncing ? '동기화 중...' : '활동 데이터 최신화' }}
      </button>
    </div>

    <!-- 레벨 대시보드 v4 -->
    <div v-if="userProfile" class="level-dashboard-card-v4 fade-in">
      <!-- 메인 영역 (8) -->
      <div class="dash-main-col">
        <div class="dash-integrated-profile">
          <!-- 상단: 닉네임 및 레벨 -->
          <div class="profile-top-info">
            <div class="dash-level-hexagon" :class="`tier-${getAccountTierNumber(userProfile.level)}`">
              <span class="hex-lv">Lv</span>
              <span class="hex-num">{{ userProfile.level }}</span>
            </div>
            <div class="dash-user-details">
              <div class="dash-nickname-group">
                <AnimatedNickname 
                  :nickname="userProfile.nickname || userProfile.username" 
                  :user-id="userProfile.id"
                  :level="userProfile.level"
                  :equipped-effect-code="userProfile.equippedEffectCode"
                  :show-level-effects="userProfile.showLevelEffects"
                  :show-equipped-effect="userProfile.showEquippedEffect"
                />
                <div class="dash-toggle-group">
                  <button class="dash-effects-btn" :class="[{ active: userProfile.showLevelEffects }, `tier-${getAccountTierNumber(userProfile.level)}`]" @click="$emit('toggle-level-effects')" title="닉네임 효과 토글">
                    ✨
                  </button>
                  <button class="dash-effects-btn" :class="[{ active: userProfile.showEquippedEffect }, `tier-${getAccountTierNumber(userProfile.level)}`]" @click="$emit('toggle-equipped-effect')" title="닉네임 장식 토글">
                    🦋
                  </button>
                </div>
              </div>
              <div class="dash-user-action-row">
                <div class="dash-username-sub">@{{ userProfile.username }}</div>
              </div>
            </div>
          </div>

          <!-- 하단: 경험치 바 -->
          <div class="dash-progress-area">
            <div class="progress-header">
              <span class="progress-title">성장 경험치 (EXP)</span>
              <div class="progress-values">
                <span class="curr">{{ formatNumber(userProfile.totalExp) }}</span>
                <span class="sep">/</span>
                <span class="max">{{ formatNumber(nextLevelExp) }}</span>
              </div>
            </div>
            <div class="dash-progress-track">
              <div class="dash-progress-fill" :style="{ width: expPercentage + '%' }">
                <div class="dash-progress-glow"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 사이드 영역 (4) -->
      <div class="dash-side-col">
        <div class="dash-stat-compact-row">
          <div class="dash-stat-compact-label">현재 활동 티어</div>
          <div class="dash-stat-compact-value" :class="`tier-${getAccountTierNumber(userProfile.level)} text-grad`">
            {{ getTierName(userProfile.level) }}
          </div>
        </div>
        <div class="dash-stat-compact-row">
          <div class="dash-stat-compact-label">대표 활동 칭호</div>
          <div class="dash-stat-compact-value">{{ equippedBadgeDisplayName || '장착된 칭호 없음' }}</div>
        </div>
        <div class="dash-stat-compact-row">
          <div class="dash-stat-compact-label">종합 활동 순위</div>
          <div class="dash-stat-compact-value">Top {{ activityData?.rankPercentage || 100 }}% ({{ activityData?.overallRank || 0 }}위)</div>
        </div>
        
        <!-- 활동 신뢰 점수 (Karma) - [시니어] 아코디언 통합 버전 -->
        <div class="dash-karma-box-v2" :class="getKarmaClass(userProfile.karmaPoints)">
          <div class="karma-header-v2" @click="isKarmaHistoryOpen = !isKarmaHistoryOpen">
            <div class="karma-info-v2">
              <span class="karma-label-v2">활동 신뢰 점수</span>
              <div class="karma-score-v2">
                <span class="score-num-v2">{{ userProfile.karmaPoints.toFixed(1) }}</span>
                <span class="score-unit-v2">P</span>
              </div>
            </div>
            <div class="karma-icon-v2">⚖️</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 활동 지표 카드 그리드 -->
    <div class="activity-stats-grid">
      <div v-for="type in badgeTypes" :key="type" class="stat-card-v2" @click="$emit('toggle-tooltip', type)">
        <div class="stat-card-inner-v2">
          <div class="stat-icon-v2">{{ getBadgeIcon(type) }}</div>
          <div class="stat-info-v2">
            <div class="stat-label-v2">{{ getBadgeLabel(type) }}</div>
            <div class="stat-value-v2">{{ getBadgeMetricValue(type) }}</div>
          </div>
          <div class="stat-arrow-v2" :class="{ active: activeStatTooltip === type }">▼</div>
        </div>
        <Transition name="slide-fade">
          <div v-if="activeStatTooltip === type" class="stat-tooltip-v2">
            {{ getStatTooltip(type, getBadgeRawValue(type)) }}
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const props = defineProps({
  userProfile: Object,
  activityData: Object,
  isSyncing: Boolean,
  activeStatTooltip: String,
  nextLevelExp: Number,
  expPercentage: Number,
  equippedBadgeDisplayName: String
})

const emit = defineEmits(['sync-activity', 'toggle-level-effects', 'toggle-equipped-effect', 'toggle-tooltip'])

const isKarmaHistoryOpen = ref(false)

const badgeTypes = ['KNOWLEDGE', 'COLLECTOR', 'REVIEW', 'COMMUNITY', 'COMMUNICATOR', 'ATTENDANCE']

const getAccountTierNumber = (level) => {
  if (level >= 100) return 100
  if (level >= 90) return 90
  if (level >= 70) return 70
  if (level >= 50) return 50
  if (level >= 30) return 30
  if (level >= 10) return 10
  if (level >= 5) return 5
  return 1
}

const getTierName = (level) => {
  if (level >= 100) return 'Seraphim (Legend)'
  if (level >= 90) return 'Imperial (Mythic)'
  if (level >= 70) return 'Grand Master'
  if (level >= 50) return 'Master'
  if (level >= 30) return 'Platinum'
  if (level >= 10) return 'Gold'
  if (level >= 5) return 'Silver'
  return 'Bronze'
}

const getBadgeIcon = (type) => {
  const icons = { KNOWLEDGE: '💡', COLLECTOR: '🎯', REVIEW: '📝', COMMUNITY: '🏢', COMMUNICATOR: '💬', ATTENDANCE: '🔥' }
  return icons[type] || '✨'
}

const getBadgeLabel = (type) => {
  const labels = { KNOWLEDGE: '지식 탐구', COLLECTOR: '공고 수집', REVIEW: '리뷰 기록', COMMUNITY: '광장 활동', COMMUNICATOR: '소통 지수', ATTENDANCE: '열정 출석' }
  return labels[type] || type
}

const getBadgeMetricValue = (type) => {
  if (!props.activityData) return '0'
  switch (type) {
    case 'KNOWLEDGE': return props.activityData.totalLikeReceivedCount + '회'
    case 'COLLECTOR': return props.activityData.totalNoticeInterestCount + '회'
    case 'REVIEW': return props.activityData.reviewPostCount + '건'
    case 'COMMUNITY': return props.activityData.totalPostCount + '건'
    case 'COMMUNICATOR': return props.activityData.totalCommentCount + '개'
    case 'ATTENDANCE': return props.activityData.consecutiveAttendanceDays + '일'
    default: return '0'
  }
}

const getBadgeRawValue = (type) => {
  if (!props.activityData) return 0
  switch (type) {
    case 'KNOWLEDGE': return props.activityData.totalLikeReceivedCount
    case 'COLLECTOR': return props.activityData.totalNoticeInterestCount
    case 'REVIEW': return props.activityData.reviewPostCount
    case 'COMMUNITY': return props.activityData.totalPostCount
    case 'COMMUNICATOR': return props.activityData.totalCommentCount
    case 'ATTENDANCE': return props.activityData.consecutiveAttendanceDays
    default: return 0
  }
}

const formatNumber = (num) => {
  if (num === undefined || num === null) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")
}

const getKarmaClass = (point) => {
  if (point >= 800) return 'safe'
  if (point >= 500) return 'warning'
  return 'danger'
}

const getStatTooltip = (type, value) => {
  if (value === undefined || value === null || !props.activityData?.badgeRules) return '데이터를 불러오는 중...';
  
  const rules = props.activityData.badgeRules
    .filter(r => r.badgeType === type)
    .sort((a, b) => a.requiredValue - b.requiredValue);

  if (rules.length === 0) return '규칙 정보가 없습니다.';

  const currentRule = [...rules].reverse().find(r => value >= r.requiredValue);
  const nextRule = rules.find(r => value < r.requiredValue);
  
  let text = '';
  if (currentRule) {
    text += `현재 등급: ${currentRule.rankTitle} ${currentRule.rankEmoji}`;
  } else {
    text += `현재 등급: 입문자 🔰`;
  }

  if (nextRule) {
    const remaining = nextRule.requiredValue - value;
    if (type === 'ATTENDANCE') {
      const fastPath = Math.ceil(remaining / 2);
      text += ` > 다음 등급(${nextRule.rankTitle} ${nextRule.rankEmoji})까지 ${remaining}개 남았습니다.` ;
      text += `(연속 출석 시 ${fastPath}일, 누적 출석 시 ${remaining}일 기준)`;
    } else {
      text += ` > 다음 등급(${nextRule.rankTitle} ${nextRule.rankEmoji})까지 ${remaining}개 남았습니다.`;
    }
  } else {
    text += ` > 최고 등급 달성! 🎉`;
  }
  
  return text;
}
</script>

<style scoped>
.activity-tab-content { display: flex; flex-direction: column; }

/* 레벨 대시보드 v4 */
.level-dashboard-card-v4 {
  background: var(--card-bg); border: 1.5px solid var(--border-color); border-radius: 28px;
  display: grid; grid-template-columns: 6fr 4fr; margin-bottom: 35px; box-shadow: 0 12px 40px rgba(0,0,0,0.04); overflow: visible;
}
.dash-main-col { padding: 35px; border-right: 1.5px solid var(--border-color); background: linear-gradient(135deg, var(--hover-bg), var(--card-bg)); border-radius: 28px 0 0 28px; }
.dash-side-col { padding: 25px; display: flex; flex-direction: column; gap: 12px; justify-content: center; background: var(--card-bg); border-radius: 0 28px 28px 0; }

.dash-integrated-profile { display: flex; flex-direction: column; gap: 40px; }
.profile-top-info { display: flex; align-items: center; gap: 25px; }

.dash-level-hexagon { width: 80px; height: 80px; background: var(--t-grad); clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%); display: flex; flex-direction: column; align-items: center; justify-content: center; color: white; box-shadow: 0 8px 20px rgba(0,0,0,0.1); flex-shrink: 0; }
.hex-lv { font-size: 0.75rem; font-weight: 800; }
.hex-num { font-size: 1.7rem; font-weight: 950; }

.dash-nickname-group { display: flex; align-items: center; gap: 12px; flex-wrap: nowrap; }
.dash-nickname-group :deep(.animated-nickname) { font-size: 1.1rem !important; font-weight: 900; }
.dash-toggle-group { display: flex; gap: 6px; align-items: center; flex-shrink: 0; }
.dash-effects-btn { background: var(--hover-bg); border: 1px solid var(--border-color); width: 34px; height: 34px; border-radius: 10px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; font-size: 1.1rem; }
.dash-effects-btn.active { background: var(--link-color); color: white; border-color: transparent; }
.dash-username-sub { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; opacity: 0.7; }

.dash-progress-area { background: rgba(0, 0, 0, 0.02); padding: 20px; border-radius: 20px; border: 1px solid var(--border-color); }
:global(.dark-mode) .dash-progress-area { background: rgba(255, 255, 255, 0.05); }
.progress-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.progress-title { font-size: 0.85rem; font-weight: 850; color: var(--text-secondary); }
.progress-values { font-size: 0.9rem; font-weight: 700; }
.dash-progress-track { height: 14px; background: var(--border-color); border-radius: 10px; overflow: hidden; position: relative; }
.dash-progress-fill { height: 100%; background: linear-gradient(90deg, var(--link-color), #60a5fa); border-radius: 10px; position: relative; transition: width 1.2s cubic-bezier(0.34, 1.56, 0.64, 1); }

.dash-stat-compact-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 15px; background: var(--hover-bg); border-radius: 12px; border: 1px solid var(--border-color); }
.dash-stat-compact-label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }
.dash-stat-compact-value { font-size: 0.85rem; font-weight: 800; color: var(--text-primary); }

/* Karma Box */
.dash-karma-box-v2 { border-radius: 18px; overflow: hidden; border: 1.5px solid var(--border-color); transition: all 0.3s; margin-top: 5px; }
.karma-header-v2 { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: var(--card-bg); cursor: pointer; }
.karma-info-v2 { display: flex; flex-direction: column; }
.karma-label-v2 { font-size: 0.7rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
.karma-score-v2 { display: flex; align-items: baseline; gap: 2px; }
.score-num-v2 { font-size: 1.4rem; font-weight: 900; }
.score-unit-v2 { font-size: 0.75rem; font-weight: 700; opacity: 0.7; }
.dash-karma-box-v2.safe .score-num-v2 { color: #10b981; }
.dash-karma-box-v2.warning .score-num-v2 { color: #f59e0b; }
.dash-karma-box-v2.danger .score-num-v2 { color: #ef4444; }

/* Activity Stats Grid */
.activity-stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; }
.stat-card-v2 { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 18px; cursor: pointer; transition: all 0.2s; position: relative; }
.stat-card-v2:hover { border-color: var(--link-color); transform: translateY(-3px); }
.stat-card-inner-v2 { padding: 18px; display: flex; align-items: center; gap: 15px; position: relative; }
.stat-icon-v2 { width: 42px; height: 42px; background: var(--hover-bg); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.3rem; }
.stat-info-v2 { flex: 1; }
.stat-label-v2 { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 2px; }
.stat-value-v2 { font-size: 1rem; font-weight: 900; color: var(--text-primary); }
.stat-arrow-v2 { font-size: 0.6rem; color: var(--text-secondary); opacity: 0.4; transition: transform 0.3s; }
.stat-arrow-v2.active { transform: rotate(180deg); }

.stat-tooltip-v2 { padding: 12px 18px; background: var(--hover-bg); border-top: 1px solid var(--border-color); font-size: 0.8rem; font-weight: 600; color: var(--text-secondary); line-height: 1.5; border-radius: 0 0 18px 18px; }

/* Animation */
.slide-fade-enter-active { transition: all 0.3s ease-out; }
.slide-fade-leave-active { transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1); }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateY(-10px); opacity: 0; }

@media (max-width: 768px) {
  .level-dashboard-card-v4 { grid-template-columns: 1fr; border-radius: 20px; }
  .dash-main-col { border-right: none; border-bottom: 1.5px solid var(--border-color); padding: 25px; border-radius: 20px 20px 0 0; }
  .dash-side-col { padding: 20px; border-radius: 0 0 20px 20px; }
  .activity-stats-grid { grid-template-columns: 1fr; }
}
</style>
