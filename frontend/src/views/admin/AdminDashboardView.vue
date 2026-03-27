<template>
  <div class="admin-dashboard-container">
    <div class="dashboard-header">
      <h2>📊 운영 대시보드</h2>
      <button @click="fetchStats" class="btn-refresh">🔄 운영정보 갱신</button>
    </div>

    <!-- 상단 요약 카드 -->
    <div class="stats-grid">
      <div class="stats-card">
        <div class="card-label">전체 공고</div>
        <div class="card-value">{{ stats.totalNotices.toLocaleString() }}</div>
        <div class="card-sub">오늘 +{{ stats.todayNotices }}</div>
      </div>
      <div class="stats-card highlight">
        <div class="card-label">접수중 공고</div>
        <div class="card-value">{{ stats.recruitingNotices.toLocaleString() }}</div>
        <div class="card-sub">실시간 모집 중</div>
      </div>
      <div class="stats-card">
        <div class="card-label">전체 유저</div>
        <div class="card-value">{{ stats.totalUsers.toLocaleString() }}</div>
        <div class="card-sub">오늘 +{{ stats.todayUsers }}</div>
      </div>
    </div>

    <div class="dashboard-charts">
      <!-- 출처별 분포 -->
      <div class="chart-panel">
        <h3>🏠 출처별 공고 분포</h3>
        <div class="chart-list">
          <div v-for="(count, source) in stats.countBySource" :key="source" class="chart-item">
            <div class="item-info">
              <span class="item-name">{{ source }}</span>
              <span class="item-count">{{ count.toLocaleString() }}건</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: (count / stats.totalNotices * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 상태별 분포 -->
      <div class="chart-panel">
        <h3>🔔 상태별 현황</h3>
        <div class="chart-list">
          <div v-for="(count, status) in stats.countByStatus" :key="status" class="chart-item">
            <div class="item-info">
              <span class="item-name">{{ status }}</span>
              <span class="item-count">{{ count.toLocaleString() }}건</span>
            </div>
            <div class="progress-bar status-bar">
              <div class="progress-fill" :style="{ width: (count / stats.totalNotices * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 하단 빠른 관리 링크 -->
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
import { ref, onMounted } from 'vue'
import axios from '@/plugins/axios'

const stats = ref({
  totalNotices: 0,
  todayNotices: 0,
  recruitingNotices: 0,
  countBySource: {},
  countByStatus: {},
  totalUsers: 0,
  todayUsers: 0
})

const fetchStats = async () => {
  try {
    const res = await axios.get('/api/admin/dashboard/stats')
    stats.value = res.data.data
  } catch (e) {
    console.error('통계 로드 실패')
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 25px;
  padding-bottom: 40px;
}

.dashboard-header { display: flex; justify-content: space-between; align-items: center; }
.dashboard-header h2 { margin: 0; font-size: 1.3rem; font-weight: 800; color: var(--text-primary); }
.btn-refresh { padding: 6px 12px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; font-size: 0.8rem; cursor: pointer; color: var(--text-secondary); }

/* 요약 카드 */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.stats-card { 
  background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 25px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.stats-card.highlight { border-color: var(--link-color); background: rgba(var(--link-color-rgb), 0.05); }
.card-label { font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 8px; }
.card-value { font-size: 2rem; font-weight: 900; color: var(--text-primary); margin-bottom: 5px; }
.card-sub { font-size: 0.75rem; font-weight: 600; color: var(--link-color); }

/* 차트 패널 */
.dashboard-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; }
.chart-panel h3 { margin: 0 0 20px 0; font-size: 0.95rem; font-weight: 800; color: var(--text-primary); }

.chart-list { display: flex; flex-direction: column; gap: 15px; }
.chart-item { display: flex; flex-direction: column; gap: 6px; }
.item-info { display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: 700; }
.item-name { color: var(--text-primary); }
.item-count { color: var(--text-secondary); }

.progress-bar { height: 8px; background: var(--hover-bg); border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--link-color); border-radius: 4px; }
.status-bar .progress-fill { background: #2ecc71; }

/* 퀵 액션 */
.quick-links h3 { margin-bottom: 15px; font-size: 1rem; font-weight: 800; }
.link-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
.link-card { 
  background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 15px;
  text-align: center; text-decoration: none; color: var(--text-primary); font-weight: 700; font-size: 0.9rem;
  transition: all 0.2s;
}
.link-card:hover { border-color: var(--link-color); color: var(--link-color); transform: translateY(-3px); }

@media (max-width: 992px) {
  .dashboard-charts { grid-template-columns: 1fr; }
  
  /* [시니어 조치] 모바일 요약 카드 3열 강제 배치 */
  .stats-grid { 
    grid-template-columns: repeat(3, 1fr); 
    gap: 8px; 
  }
  .stats-card { padding: 12px 8px; text-align: center; }
  .card-label { font-size: 0.65rem; margin-bottom: 4px; }
  .card-value { font-size: 1.1rem; }
  .card-sub { font-size: 0.6rem; }

  /* [시니어 조치] 모바일 퀵 액션 2열 배치 */
  .link-grid { 
    grid-template-columns: repeat(2, 1fr); 
    gap: 10px; 
  }
  .link-card { padding: 12px 8px; font-size: 0.8rem; }
}
</style>
