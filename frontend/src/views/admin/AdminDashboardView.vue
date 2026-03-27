<template>
  <div class="admin-dashboard-container">
    <div class="dashboard-header">
      <h2>📊 운영 대시보드</h2>
      <button @click="fetchStats" class="btn-refresh">🔄 갱신</button>
    </div>

    <!-- 요약 카드 -->
    <div class="stats-grid">
      <div class="stats-card">
        <div class="card-icon">🏠</div>
        <div class="card-body">
          <div class="card-label">전체 공고</div>
          <div class="card-value">{{ stats.totalNotices.toLocaleString() }}</div>
          <div class="card-sub">오늘 +{{ stats.todayNotices }}</div>
        </div>
      </div>
      <div class="stats-card accent-blue">
        <div class="card-icon">📋</div>
        <div class="card-body">
          <div class="card-label">접수중 공고</div>
          <div class="card-value">{{ stats.recruitingNotices.toLocaleString() }}</div>
          <div class="card-sub">실시간 모집 중</div>
        </div>
      </div>
      <div class="stats-card">
        <div class="card-icon">👥</div>
        <div class="card-body">
          <div class="card-label">전체 유저</div>
          <div class="card-value">{{ stats.totalUsers.toLocaleString() }}</div>
          <div class="card-sub">오늘 +{{ stats.todayUsers }}</div>
        </div>
      </div>
      <div class="stats-card" :class="(stats.pendingReports ?? 0) > 0 ? 'accent-red' : ''">
        <div class="card-icon">🚨</div>
        <div class="card-body">
          <div class="card-label">신고 대기</div>
          <div class="card-value">{{ (stats.pendingReports ?? 0).toLocaleString() }}</div>
          <div class="card-sub">{{ (stats.pendingReports ?? 0) > 0 ? '즉시 처리 필요' : '처리 완료' }}</div>
        </div>
      </div>
    </div>

    <!-- 공고 분포 패널 (기존) -->
    <div class="chart-row">
      <div class="chart-panel">
        <div class="panel-header">
          <h3>🏠 출처별 공고 분포</h3>
          <span class="panel-total">총 {{ stats.totalNotices.toLocaleString() }}건</span>
        </div>
        <div class="bar-list">
          <div v-for="(count, source) in stats.countBySource" :key="source" class="bar-item">
            <div class="bar-meta">
              <span class="bar-label">{{ formatSource(source) }}</span>
              <span class="bar-count">{{ count.toLocaleString() }}건</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill source-fill" :style="{ width: (count / stats.totalNotices * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-panel">
        <div class="panel-header">
          <h3>🔔 공고 상태별 현황</h3>
          <span class="panel-total">총 {{ stats.totalNotices.toLocaleString() }}건</span>
        </div>
        <div class="bar-list">
          <div v-for="(count, status) in stats.countByStatus" :key="status" class="bar-item">
            <div class="bar-meta">
              <span class="bar-label">{{ formatStatus(status) }}</span>
              <span class="bar-count">{{ count.toLocaleString() }}건</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill status-fill" :style="{ width: (count / stats.totalNotices * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 운영 현황 3패널 (신규) -->
    <div class="ops-row">
      <!-- 신고 처리 현황 -->
      <div class="ops-panel">
        <div class="panel-header">
          <h3>🚨 신고 처리 현황</h3>
        </div>
        <div class="ops-list">
          <div
            v-for="item in reportItems"
            :key="item.key"
            class="ops-item"
            :class="item.urgent ? 'ops-item--urgent' : ''"
          >
            <div class="ops-left">
              <span class="ops-dot" :style="{ background: item.color }"></span>
              <div class="ops-labels">
                <span class="ops-type">{{ item.key }}</span>
                <span class="ops-desc">{{ item.label }}</span>
              </div>
            </div>
            <span class="ops-badge" :style="{ background: item.color + '22', color: item.color }">
              {{ (stats.countByReportStatus?.[item.key] || 0).toLocaleString() }}
            </span>
          </div>
        </div>
      </div>

      <!-- 유저 상태 현황 -->
      <div class="ops-panel">
        <div class="panel-header">
          <h3>👥 유저 상태 현황</h3>
        </div>
        <div class="ops-list">
          <div
            v-for="item in userStatusItems"
            :key="item.key"
            class="ops-item"
          >
            <div class="ops-left">
              <span class="ops-dot" :style="{ background: item.color }"></span>
              <div class="ops-labels">
                <span class="ops-type">{{ item.key }}</span>
                <span class="ops-desc">{{ item.label }}</span>
              </div>
            </div>
            <span class="ops-badge" :style="{ background: item.color + '22', color: item.color }">
              {{ (stats.countByUserStatus?.[item.key] || 0).toLocaleString() }}
            </span>
          </div>
        </div>
      </div>

      <!-- 게시글 상태 현황 -->
      <div class="ops-panel">
        <div class="panel-header">
          <h3>📝 게시글 상태 현황</h3>
        </div>
        <div class="ops-list">
          <div
            v-for="item in postStatusItems"
            :key="item.key"
            class="ops-item"
          >
            <div class="ops-left">
              <span class="ops-dot" :style="{ background: item.color }"></span>
              <div class="ops-labels">
                <span class="ops-type">{{ item.key }}</span>
                <span class="ops-desc">{{ item.label }}</span>
              </div>
            </div>
            <span class="ops-badge" :style="{ background: item.color + '22', color: item.color }">
              {{ (stats.countByPostStatus?.[item.key] || 0).toLocaleString() }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 퀵 액션 -->
    <div class="quick-links">
      <h3>⚡ 퀵 액션</h3>
      <div class="link-grid">
        <RouterLink to="/admin/notices" class="link-card">🏠 공고 관리</RouterLink>
        <RouterLink to="/admin/community" class="link-card">💬 커뮤니티 관리</RouterLink>
        <RouterLink to="/admin/users" class="link-card">👥 사용자 관리</RouterLink>
        <RouterLink to="/admin/simulation" class="link-card">🔬 보상 시뮬레이션</RouterLink>
        <RouterLink to="/admin/tags" class="link-card">🏷️ 태그 관리</RouterLink>
        <RouterLink to="/admin/metadata" class="link-card">🗂️ 메타데이터 관리</RouterLink>
        <RouterLink to="/admin/badges" class="link-card">🎖️ 배지 마스터리 관리</RouterLink>
        <RouterLink to="/admin/about" class="link-card">📢 공지 및 패치 관리</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from '@/plugins/axios'

const stats = ref({
  totalNotices: 0,
  todayNotices: 0,
  recruitingNotices: 0,
  countBySource: {},
  countByStatus: {},
  totalUsers: 0,
  todayUsers: 0,
  pendingReports: 0,
  countByReportStatus: {},
  countByUserStatus: {},
  countByPostStatus: {}
})

const fetchStats = async () => {
  try {
    const res = await axios.get('/api/admin/dashboard/stats')
    stats.value = { ...stats.value, ...res.data.data }
  } catch (e) {
    console.error('통계 로드 실패')
  }
}

const STATUS_LABELS = {
  RECRUITING: '접수중', CLOSED: '마감', UPCOMING: '예정', INFO: '안내',
  EXPIRED_INFO: '이전안내', RESULT: '결과발표', RESULT_COMPLETED: '발표완료',
  RECRUITING_NON_STOP: '원문확인'
}
const SOURCE_LABELS = { LH: 'LH공사', SH: 'SH공사', PRIVATE: '민간임대' }
const formatStatus = (k) => STATUS_LABELS[k] ? `${k}(${STATUS_LABELS[k]})` : k
const formatSource = (k) => SOURCE_LABELS[k] ? `${k}(${SOURCE_LABELS[k]})` : k

const reportItems = [
  { key: 'WAITING',         label: '처리 대기',   color: '#ef4444', urgent: true },
  { key: 'BLIND_COMPLETE',  label: '블라인드 완료', color: '#f59e0b' },
  { key: 'DELETE_COMPLETE', label: '삭제 완료',   color: '#6b7280' },
  { key: 'REJECTED',        label: '반려',        color: '#10b981' }
]

const userStatusItems = [
  { key: 'ACTIVE',     label: '정상',    color: '#10b981' },
  { key: 'RESTRICTED', label: '활동 제한', color: '#ef4444' },
  { key: 'BLOCKED',    label: '차단됨',  color: '#f59e0b' },
  { key: 'WITHDRAWN',  label: '탈퇴',    color: '#6b7280' }
]

const postStatusItems = [
  { key: 'ACTIVE',       label: '정상',      color: '#10b981' },
  { key: 'BLINDED',      label: '블라인드',   color: '#f59e0b' },
  { key: 'USER_DELETED', label: '사용자 삭제', color: '#6b7280' },
  { key: 'DELETED',      label: '관리자 삭제', color: '#ef4444' }
]

onMounted(fetchStats)
</script>

<style scoped>
.admin-dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 40px;
}

/* 헤더 */
.dashboard-header { display: flex; justify-content: space-between; align-items: center; }
.dashboard-header h2 { margin: 0; font-size: 1.3rem; font-weight: 800; color: var(--text-primary); }
.btn-refresh { padding: 6px 14px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; font-size: 0.8rem; cursor: pointer; color: var(--text-secondary); transition: all 0.2s; }
.btn-refresh:hover { border-color: var(--link-color); color: var(--link-color); }

/* 요약 카드 */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stats-card {
  background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px;
  padding: 20px; display: flex; align-items: center; gap: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: box-shadow 0.2s;
}
.stats-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.stats-card.accent-blue { border-color: var(--link-color); background: color-mix(in srgb, var(--link-color) 6%, var(--card-bg)); }
.stats-card.accent-red { border-color: #ef4444; background: color-mix(in srgb, #ef4444 6%, var(--card-bg)); animation: urgent-pulse 2s infinite; }
@keyframes urgent-pulse { 0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); } 50% { box-shadow: 0 0 0 6px rgba(239,68,68,0.12); } }
.card-icon { font-size: 1.6rem; flex-shrink: 0; }
.card-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.card-label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }
.card-value { font-size: 1.7rem; font-weight: 900; color: var(--text-primary); line-height: 1; }
.card-sub { font-size: 0.7rem; font-weight: 600; color: var(--link-color); }
.accent-red .card-sub { color: #ef4444; }

/* 공고 차트 행 */
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-panel {
  background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px;
}
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.panel-header h3 { margin: 0; font-size: 0.9rem; font-weight: 800; color: var(--text-primary); }
.panel-total { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }

.bar-list { display: flex; flex-direction: column; gap: 14px; }
.bar-item { display: flex; flex-direction: column; gap: 6px; }
.bar-meta { display: flex; justify-content: space-between; font-size: 0.8rem; font-weight: 700; }
.bar-label { color: var(--text-primary); }
.bar-count { color: var(--text-secondary); }
.bar-track { height: 7px; background: var(--hover-bg); border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.source-fill { background: var(--link-color); }
.status-fill { background: #10b981; }

/* 운영 현황 3패널 행 */
.ops-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.ops-panel {
  background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px;
}
.ops-list { display: flex; flex-direction: column; gap: 10px; }
.ops-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; border-radius: 10px; background: var(--hover-bg);
  transition: background 0.2s;
}
.ops-item:hover { background: color-mix(in srgb, var(--border-color) 40%, var(--hover-bg)); }
.ops-item--urgent { background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.2); }
.ops-left { display: flex; align-items: center; gap: 10px; }
.ops-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ops-labels { display: flex; flex-direction: column; gap: 1px; }
.ops-type { font-size: 0.7rem; font-weight: 700; color: var(--text-secondary); font-family: monospace; }
.ops-desc { font-size: 0.78rem; font-weight: 800; color: var(--text-primary); }
.ops-badge {
  font-size: 0.8rem; font-weight: 900; padding: 3px 10px; border-radius: 20px; min-width: 36px;
  text-align: center; flex-shrink: 0;
}

/* 퀵 액션 */
.quick-links h3 { margin: 0 0 14px 0; font-size: 1rem; font-weight: 800; color: var(--text-primary); }
.link-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.link-card {
  background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px;
  text-align: center; text-decoration: none; color: var(--text-primary); font-weight: 700; font-size: 0.85rem;
  transition: all 0.2s;
}
.link-card:hover { border-color: var(--link-color); color: var(--link-color); transform: translateY(-2px); }

/* 반응형 */
@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .ops-row { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .stats-card { padding: 14px; gap: 10px; }
  .card-icon { font-size: 1.3rem; }
  .card-value { font-size: 1.3rem; }
  .chart-row { grid-template-columns: 1fr; }
  .ops-row { grid-template-columns: 1fr; }
  .link-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
