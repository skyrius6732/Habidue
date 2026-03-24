<template>
  <div class="about-container">
    <PageHeader 
      icon="ℹ️" 
      title="HabiDue 소개" 
      stats-text="현재 버전" 
      :stats-value="latestVersion"
      bio="더 쉽고 편리한 주거 복지 정보의 시작, HabiDue입니다."
    />

    <div class="about-content-inner">
      <!-- 탭 메뉴 -->
      <div class="about-tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.id" 
          class="tab-btn" 
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
        </button>
      </div>

      <div class="tab-content-wrapper">
        <!-- 1. 서비스 소개 탭 -->
        <div v-if="activeTab === 'intro'" class="tab-content fade-in">
          <section class="intro-section">
            <h1 class="logo-font pc-only-title">HabiDue</h1>
            <p class="subtitle">공공 주택 공고, 이제 HabiDue에서 한눈에 확인하세요.</p>
          </section>

          <section class="features-section">
            <div class="feature-card">
              <div class="card-icon">🏠</div>
              <h3>통합 공고 리스트</h3>
              <p>LH, SH 등 여러 기관의 흩어져 있는 공고를 한곳에 모아 피드 형태로 제공합니다.</p>
            </div>
            <div class="feature-card">
              <div class="card-icon">📅</div>
              <h3>스마트 캘린더</h3>
              <p>마감 임박 공고를 달력 형식으로 시각화하여 중요한 신청 기회를 놓치지 않게 도와줍니다.</p>
            </div>
            <div class="feature-card">
              <div class="card-icon">❤️</div>
              <h3>관심 공고 관리</h3>
              <p>마음에 드는 공고는 하트를 눌러 스크랩하고, 나만의 메모를 남겨 효율적으로 관리하세요.</p>
            </div>
            <div class="feature-card">
              <div class="card-icon">🔔</div>
              <h3>키워드 맞춤 알림</h3>
              <p>나의 주거 희망 지역이나 유형을 알림 키워드로 등록하면 매칭되는 공고를 우선적으로 보여줍니다.</p>
            </div>
          </section>

          <section class="mission-section">
            <p>"주거 정보의 불균형을 해소하고, 누구나 평등하게 주거 복지 정보를 누리는 세상을 꿈꿉니다."</p>
          </section>
        </div>

        <!-- 2. 공지사항 탭 -->
        <div v-if="activeTab === 'news'" class="tab-content fade-in">
          <div v-if="announcements.length === 0" class="empty-msg">공지사항이 없습니다.</div>
          <div class="announcement-list">
            <div v-for="news in announcements" :key="news.id" class="news-item">
              <div class="news-header">
                <span class="news-tag" :class="news.type">{{ news.tag }}</span>
                <span class="news-date">{{ news.date }}</span>
              </div>
              <h4 class="news-title">{{ news.title }}</h4>
              <p class="news-content">{{ news.content }}</p>
            </div>
          </div>
        </div>

        <!-- 3. 패치 히스토리 탭 -->
        <div v-if="activeTab === 'patch'" class="tab-content fade-in">
          <div v-if="patchNotes.length === 0" class="empty-msg">패치 내역이 없습니다.</div>
          <div class="timeline">
            <div v-for="patch in patchNotes" :key="patch.id" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="patch-header">
                  <span class="patch-version">{{ patch.version }}</span>
                  <span class="patch-date">{{ formatDateDot(patch.date) }}</span>
                </div>
                <h4 class="patch-title">{{ patch.title }}</h4>
                <ul class="patch-details">
                  <li v-for="(detail, sIdx) in patch.details" :key="sIdx">
                    <span class="detail-bullet">-</span> {{ detail }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import axios from '@/plugins/axios'

const route = useRoute()
const activeTab = ref(route.query.tab || 'intro')
const tabs = [
  { id: 'intro', name: '서비스 소개' },
  { id: 'news', name: '공지사항' },
  { id: 'patch', name: '패치 히스토리' }
]

// [시니어 조치] URL 쿼리 파라미터 감시하여 탭 전환
watch(() => route.query.tab, (newTab) => {
  if (newTab) activeTab.value = newTab
})

const announcements = ref([])
const patchNotes = ref([])

const latestVersion = computed(() => {
  return patchNotes.value.length > 0 ? patchNotes.value[0].version : 'v1.0.0'
})

const formatDateDot = (s) => s ? s.split('T')[0].replace(/-/g, '.') : '-';

const fetchData = async () => {
  try {
    const [newsRes, patchRes] = await Promise.all([
      axios.get('/api/announce-patch/notices'),
      axios.get('/api/announce-patch/patches')
    ])
    announcements.value = newsRes.data.data
    patchNotes.value = patchRes.data.data
  } catch (e) {
    console.error('About 데이터 로드 실패:', e)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.about-container { width: 100%; }
.about-content-inner { max-width: 935px; margin: 40px auto; padding: 0 20px; }

/* 탭 UI */
.about-tabs { display: flex; justify-content: center; gap: 10px; margin-bottom: 50px; border-bottom: 1px solid var(--border-color); padding-bottom: 1px; }
.tab-btn { background: none; border: none; padding: 15px 30px; font-size: 1rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; position: relative; transition: all 0.2s; }
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--link-color); }
.tab-btn.active::after { content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 2px; background-color: var(--link-color); }

.tab-content-wrapper { min-height: 400px; }
.fade-in { animation: fadeIn 0.3s ease-in-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* 1. 소개 섹션 */
.intro-section { text-align: center; margin-bottom: 60px; }
.logo-font { font-family: 'Grand Hotel', cursive; font-size: 4rem; color: var(--text-primary); margin-bottom: 10px; }
.subtitle { font-size: 1.2rem; color: var(--text-secondary); font-weight: 500; }

.features-section { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.feature-card { background: var(--card-bg); padding: 30px 20px; border-radius: 16px; border: 1px solid var(--border-color); text-align: center; transition: all 0.3s ease; }
.feature-card:hover { transform: translateY(-5px); border-color: var(--link-color); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }
.card-icon { font-size: 2.5rem; margin-bottom: 20px; }
.feature-card h3 { font-size: 1.15rem; font-weight: 700; margin-bottom: 12px; color: var(--text-primary); }
.feature-card p { font-size: 0.9rem; color: var(--text-secondary); line-height: 1.6; word-break: keep-all; }

/* 2. 공지사항 */
.announcement-list { display: flex; flex-direction: column; gap: 15px; }
.news-item { background: var(--card-bg); padding: 24px; border-radius: 12px; border: 1px solid var(--border-color); transition: border-color 0.2s; }
.news-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.news-tag { 
  font-size: 0.72rem; 
  font-weight: 800; 
  padding: 4px 10px; 
  border-radius: 6px; 
  display: flex;
  align-items: center;
  gap: 4px;
}
.news-tag.notice { background-color: rgba(237, 73, 86, 0.1); color: #ed4956; border: 1px solid rgba(237, 73, 86, 0.2); }
.news-tag.info { background-color: rgba(0, 149, 246, 0.1); color: var(--link-color); border: 1px solid rgba(0, 149, 246, 0.2); }
.news-date { font-size: 0.8rem; color: var(--text-secondary); font-weight: 500; }
.news-title { font-size: 1.1rem; font-weight: 700; color: var(--text-primary); margin: 0 0 10px 0; }
.news-content { font-size: 0.92rem; color: var(--text-secondary); line-height: 1.6; margin: 0; }

/* 3. 패치 내역 (타임라인) */
.timeline { position: relative; padding-left: 30px; margin-top: 20px; }
.timeline::before { content: ''; position: absolute; left: 7px; top: 10px; bottom: 10px; width: 2px; background-color: var(--border-color); }
.timeline-item { position: relative; margin-bottom: 40px; }
.timeline-item:last-child { margin-bottom: 0; }
.timeline-dot { position: absolute; left: -30px; top: 8px; width: 16px; height: 16px; border-radius: 50%; background-color: var(--card-bg); border: 3px solid var(--link-color); z-index: 1; }
.patch-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 15px; }
.patch-version { font-size: 1.2rem; font-weight: 800; color: var(--link-color); }
.patch-date { font-size: 0.85rem; color: var(--text-secondary); font-weight: 500; }
.patch-title { font-size: 1.05rem; font-weight: 800; color: var(--text-primary); margin: 0 0 12px 0; line-height: 1.4; }
.patch-details { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.patch-details li { font-size: 0.92rem; color: var(--text-primary); line-height: 1.5; display: flex; gap: 8px; }
.detail-bullet { color: var(--link-color); font-weight: bold; }

.empty-msg { text-align: center; padding: 100px 0; color: var(--text-secondary); font-size: 0.95rem; }
.mission-section { text-align: center; padding: 60px 40px; background-color: var(--hover-bg); border-radius: 20px; font-style: italic; font-size: 1.1rem; color: var(--text-primary); line-height: 1.8; margin-top: 60px; }

@media (max-width: 768px) {
  .pc-only-title { display: none; }
  .about-content-inner { margin: 20px auto; padding: 0 15px; }
  .about-tabs { margin-bottom: 30px; }
  .tab-btn { padding: 12px 15px; font-size: 0.9rem; }
  .features-section { grid-template-columns: 1fr; }
  .timeline { padding-left: 25px; }
}
</style>
