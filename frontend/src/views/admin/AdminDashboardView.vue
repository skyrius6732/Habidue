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

    <!-- [시니어 추가] 트렌드 분석 그래프 패널 -->
    <div class="trend-row">
      <div class="trend-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <h3>📈 운영 트렌드 분석</h3>
            <p class="panel-subtitle">공고 등록 및 유저 가입 추이</p>
          </div>
          <div class="trend-filters">
            <button 
              v-for="f in ['daily', 'weekly', 'monthly']" 
              :key="f"
              class="filter-btn"
              :class="{ active: trendFilter === f }"
              @click="trendFilter = f"
            >
              {{ f === 'daily' ? '일별' : f === 'weekly' ? '주별' : '월별' }}
            </button>
          </div>
        </div>
        <div class="chart-container">
          <canvas id="trendChart"></canvas>
        </div>
      </div>
    </div>

    <!-- [시니어 추가] 실시간 DB 커넥션 모니터링 패널 -->
    <div class="trend-row">
      <div class="trend-panel db-monitor-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <h3>🔗 실시간 데이터베이스 커넥션</h3>
            <p class="panel-subtitle">HikariCP 커넥션 풀 상태 (5초 간격 갱신)</p>
          </div>
          <div v-if="dbMetrics" class="db-current-status">
            <div class="status-indicator">
              <span class="dot active"></span> 사용 중: <b>{{ dbMetrics.activeConnections }}</b>
            </div>
            <div class="status-indicator">
              <span class="dot idle"></span> 대기 중: <b>{{ dbMetrics.idleConnections }}</b>
            </div>
            <div class="status-indicator">
              <span class="dot total"></span> 전체: <b>{{ dbMetrics.totalConnections }}</b> / {{ dbMetrics.maxPoolSize }}
            </div>
          </div>
        </div>
        <div class="chart-container">
          <canvas id="dbConnChart"></canvas>
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
              <div class="bar-fill source-fill" :style="{ width: (count / (stats.totalNotices || 1) * 100) + '%' }"></div>
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
              <div class="bar-fill status-fill" :style="{ width: (count / (stats.totalNotices || 1) * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 운영 현황 3패널 -->
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
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from '@/plugins/axios'
import Chart from 'chart.js/auto'

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
  countByPostStatus: {},
  trends: {
    dailyNotices: {}, dailyUsers: {},
    weeklyNotices: {}, weeklyUsers: {},
    monthlyNotices: {}, monthlyUsers: {}
  }
})

// 트렌드 그래프 관련 상태
const trendFilter = ref('daily')
let trendChartInstance = null

// DB 모니터링 관련 상태
const dbMetrics = ref(null)
let dbChartInstance = null
let dbPollingInterval = null
const dbHistoryLimit = 20
const dbHistory = ref({
  labels: [],
  active: [],
  idle: [],
  total: []
})

const fetchStats = async () => {
  try {
    const res = await axios.get('/api/admin/dashboard/stats')
    stats.value = { ...stats.value, ...res.data.data }
    renderTrendChart()
  } catch (e) {
    console.error('통계 로드 실패')
  }
}

const fetchDbMetrics = async () => {
  try {
    const res = await axios.get('/api/admin/system/db-metrics')
    const data = res.data.data
    dbMetrics.value = data

    const now = new Date()
    const timeLabel = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`

    dbHistory.value.labels.push(timeLabel)
    dbHistory.value.active.push(data.activeConnections)
    dbHistory.value.idle.push(data.idleConnections)
    dbHistory.value.total.push(data.totalConnections)

    if (dbHistory.value.labels.length > dbHistoryLimit) {
      dbHistory.value.labels.shift()
      dbHistory.value.active.shift()
      dbHistory.value.idle.shift()
      dbHistory.value.total.shift()
    }

    renderDbChart()
  } catch (e) {
    console.error('DB 지표 로드 실패')
  }
}

const renderTrendChart = async () => {
  await nextTick()
  const ctx = document.getElementById('trendChart')
  if (!ctx) return

  if (trendChartInstance) trendChartInstance.destroy()

  const filter = trendFilter.value
  const noticeData = stats.value.trends[`${filter}Notices`] || {}
  const userData = stats.value.trends[`${filter}Users`] || {}
  
  const allDateKeys = [...new Set([...Object.keys(noticeData), ...Object.keys(userData)])].sort()
  const labels = allDateKeys.map(key => {
    if (filter === 'weekly') return key.replace('-', '년 ') + '주'
    if (filter === 'monthly') return key.replace('-', '년 ') + '월'
    return key
  })

  trendChartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [
        {
          label: '신규 공고',
          data: allDateKeys.map(k => noticeData[k] || 0),
          borderColor: '#38a169',
          backgroundColor: 'rgba(56, 161, 105, 0.1)',
          fill: true,
          tension: 0.4
        },
        {
          label: '신규 유저',
          data: allDateKeys.map(k => userData[k] || 0),
          borderColor: '#3182ce',
          backgroundColor: 'rgba(49, 130, 206, 0.1)',
          fill: true,
          tension: 0.4
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { intersect: false, mode: 'index' },
      plugins: {
        legend: { position: 'top', align: 'end' }
      },
      scales: {
        y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } },
        x: { grid: { display: false } }
      }
    }
  })
}

const renderDbChart = async () => {
  await nextTick()
  const ctx = document.getElementById('dbConnChart')
  if (!ctx) return

  if (!dbChartInstance) {
    dbChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: dbHistory.value.labels,
        datasets: [
          {
            label: '사용 중 (Active)',
            data: dbHistory.value.active,
            borderColor: '#f87171',
            backgroundColor: 'rgba(248, 113, 113, 0.1)',
            fill: true,
            tension: 0.3
          },
          {
            label: '대기 중 (Idle)',
            data: dbHistory.value.idle,
            borderColor: '#fbbf24',
            backgroundColor: 'rgba(251, 191, 36, 0.1)',
            fill: true,
            tension: 0.3
          },
          {
            label: '전체 (Total)',
            data: dbHistory.value.total,
            borderColor: '#60a5fa',
            borderDash: [5, 5],
            fill: false,
            tension: 0.3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 400 },
        scales: {
          y: { beginAtZero: true, suggestedMax: 10, ticks: { stepSize: 1 } },
          x: { grid: { display: false } }
        },
        plugins: {
          legend: { position: 'top', align: 'end', labels: { boxWidth: 10 } }
        }
      }
    })
  } else {
    dbChartInstance.data.labels = dbHistory.value.labels
    dbChartInstance.data.datasets[0].data = dbHistory.value.active
    dbChartInstance.data.datasets[1].data = dbHistory.value.idle
    dbChartInstance.data.datasets[2].data = dbHistory.value.total
    dbChartInstance.update('none')
  }
}

watch(trendFilter, renderTrendChart)

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

onMounted(() => {
  fetchStats()
  fetchDbMetrics()
  dbPollingInterval = setInterval(fetchDbMetrics, 5000)
})

onUnmounted(() => {
  if (dbPollingInterval) clearInterval(dbPollingInterval)
  if (dbChartInstance) dbChartInstance.destroy()
  if (trendChartInstance) trendChartInstance.destroy()
})
</script>

<style scoped>
.admin-dashboard-container { display: flex; flex-direction: column; gap: 24px; padding-bottom: 40px; }
.dashboard-header { display: flex; justify-content: space-between; align-items: center; }
.dashboard-header h2 { margin: 0; font-size: 1.3rem; font-weight: 800; color: var(--text-primary); }
.btn-refresh { padding: 6px 14px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; font-size: 0.8rem; cursor: pointer; color: var(--text-secondary); transition: all 0.2s; }
.btn-refresh:hover { border-color: var(--link-color); color: var(--link-color); }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stats-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; display: flex; align-items: center; gap: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.stats-card.accent-blue { border-color: var(--link-color); background: color-mix(in srgb, var(--link-color) 6%, var(--card-bg)); }
.stats-card.accent-red { border-color: #ef4444; background: color-mix(in srgb, #ef4444 6%, var(--card-bg)); animation: urgent-pulse 2s infinite; }
@keyframes urgent-pulse { 0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); } 50% { box-shadow: 0 0 0 6px rgba(239,68,68,0.12); } }
.card-icon { font-size: 1.6rem; }
.card-body { display: flex; flex-direction: column; gap: 2px; }
.card-label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }
.card-value { font-size: 1.7rem; font-weight: 900; color: var(--text-primary); }
.card-sub { font-size: 0.7rem; font-weight: 600; color: var(--link-color); }

.trend-row { margin-bottom: 8px; }
.trend-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 24px; }
.panel-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; }
.panel-title-group h3 { margin: 0; font-size: 1rem; font-weight: 800; }
.panel-subtitle { font-size: 0.75rem; color: var(--text-secondary); }
.trend-filters { display: flex; gap: 6px; background: var(--hover-bg); padding: 4px; border-radius: 10px; }
.filter-btn { padding: 6px 14px; border: none; background: transparent; border-radius: 8px; font-size: 0.75rem; font-weight: 700; cursor: pointer; }
.filter-btn.active { background: var(--card-bg); color: var(--link-color); }

.chart-container { height: 300px; margin-top: 20px; }

/* DB 모니터 전용 */
.db-monitor-panel { border-color: color-mix(in srgb, var(--link-color) 30%, var(--border-color)); }
.db-current-status { display: flex; gap: 15px; }
.status-indicator { display: flex; align-items: center; gap: 6px; font-size: 0.75rem; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.active { background: #f87171; }
.dot.idle { background: #fbbf24; }
.dot.total { background: #60a5fa; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; }
.bar-list { display: flex; flex-direction: column; gap: 14px; }
.bar-meta { display: flex; justify-content: space-between; font-size: 0.8rem; font-weight: 700; }
.bar-track { height: 7px; background: var(--hover-bg); border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; transition: width 0.6s ease; }
.source-fill { background: var(--link-color); }
.status-fill { background: #10b981; }

.ops-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.ops-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; }
.ops-item { display: flex; justify-content: space-between; padding: 10px; border-radius: 10px; background: var(--hover-bg); margin-bottom: 8px; }
.ops-left { display: flex; align-items: center; gap: 10px; }
.ops-dot { width: 8px; height: 8px; border-radius: 50%; }
.ops-badge { font-size: 0.8rem; font-weight: 900; padding: 3px 10px; border-radius: 20px; }

.quick-links h3 { margin-bottom: 14px; }
.link-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.link-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px; text-align: center; text-decoration: none; color: var(--text-primary); font-weight: 700; }
.link-card:hover { border-color: var(--link-color); transform: translateY(-2px); }

@media (max-width: 1200px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .ops-row { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .chart-row { grid-template-columns: 1fr; } .db-current-status { flex-direction: column; gap: 4px; } }
</style>
