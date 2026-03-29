<template>
  <div class="feed-container" @click="activeMenuId = null">
    <div ref="topAnchor" class="scroll-anchor"></div>
    
    <PageHeader 
      icon="🏠" 
      title="공고 리스트" 
      stats-text="전체 공고" 
      :stats-value="totalElements"
      bio="나의 키워드와 매칭되는 최신 공고를 확인하세요."
    />

    <div class="tab-divider"></div>

    <div class="top-controls">
      <!-- 통합 서클 필터 바 (상단) -->
      <div class="unified-filter-bar">
        <div class="filter-scroll" @click.stop>
          <div v-for="f in allFilterOptions" :key="f.value" 
               class="filter-item" :class="{ active: iFilterActive(f) }" 
               @click="toggleUnifiedFilter(f)">
            <div class="filter-circle" :style="{ backgroundColor: f.color, borderColor: iFilterActive(f) ? f.color : 'var(--border-color)' }">
              {{ f.label }}
            </div>
            <span class="filter-label">{{ f.name }}</span>
          </div>
        </div>
      </div>

      <!-- 검색 및 정렬 행 (하단) -->
      <div class="search-sort-row">
        <div class="search-container-v2">
          <div class="search-input-wrapper">
            <span class="search-icon-inside">🔍</span>
            <input 
              type="text"
              v-model="searchKeyword" 
              @keyup.enter="handleSearch" 
              @click.stop 
              placeholder="공고 검색 (예: 행복주택, 지역명)" 
              class="input-field-search-v2" 
              autocomplete="off"
              spellcheck="false"
            />
            <button v-if="searchKeyword" class="btn-clear-search-v2" @click.stop="clearSearch">×</button>
          </div>
        </div>
        
        <div class="sort-container-v2">
          <div class="sort-wrapper-v2" @click.stop>
            <select v-model="sortOrder" @change="handleSortChange" class="sort-select-v2">
              <option value="alarm">🔔 알림순</option>
              <option value="newest">✅ 최신순</option>
              <option value="deadline">⏳ 마감순</option>
              <option value="interest">❤️ 인기순</option>
            </select>
          </div>
        </div>
      </div>
    </div>

    <div class="notice-disclaimer">
      <div class="disclaimer-content">
        <span class="light-bulb">💡</span>
        <p class="text">
          자동 수집된 일정은 실제와 다를 수 있으니, 
          <span class="highlight">정확한 정보는 반드시 원본 공고문을 확인해 주세요.</span>
        </p>
      </div>
    </div>

    <!-- 실시간 급상승 공고 랭킹 (시니어 위치 조정) -->
    <div class="ranking-section">
      <HotRankingBoard />
    </div>

    <!-- [PC 레이아웃] -->
    <div v-if="!isMobile" class="pc-list-container">
      <table class="notice-table">
        <thead>
          <tr>
            <th class="col-source">기관</th>
            <th class="col-region">지역</th>
            <th class="col-title">공고명</th>
            <th class="col-status">상태</th>
            <th class="col-dday">디데이</th>
            <th class="col-dates">공고 기간</th>
            <th class="col-interest">관심 공고</th>
            <th class="col-tags">공고 태그</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="notice in notices" :key="notice.id" @click="openDetail(notice)" class="table-row">
            <td><span class="table-source-tag" :class="notice.source">{{ getSourceLabel(notice.source) }}</span></td>
            <td><span class="region-text">{{ notice.region }}</span></td>
            <td class="table-title-cell">
              <div class="title-wrapper">
                <span v-if="isReallyNew(notice.createdAt)" class="new-tag-mini">NEW</span>
                <span class="title-text" :title="notice.title">{{ notice.title }}</span>
              </div>
            </td>
            <td><span class="status-badge" :class="getStatus(notice).class">{{ getStatus(notice).text }}</span></td>
            <td><span class="dday-text" :class="{ 'expired': isReallyExpired(notice.deadline) }">{{ calculateDDay(notice) }}</span></td>
            <td class="table-date-cell">
              <span class="date-range">
                {{ formatDateDot(notice.announcementDate || notice.createdAt) }}
                <template v-if="notice.deadline || notice.resultDate"> ~ {{ formatDateDot(notice.deadline || notice.resultDate) }}</template>
              </span>
            </td>
            <td @click.stop="handleToggleFavorite(notice)">
              <div class="interest-cell">
                <span class="heart-icon" :class="{ filled: notice.isFavorite }">
                  {{ notice.isFavorite ? '❤️' : '🤍' }}
                </span>
                <span class="interest-count">{{ notice.interestCount || 0 }}</span>
              </div>
            </td>
            <td>
              <div class="tags-wrapper">
                <span v-for="tag in getDisplayTags(notice)" :key="tag.id" 
                      class="mini-tag" :class="{ matched: userKeywords.includes(tag.name) }"
                      @click.stop="toggleAlertKeyword(tag)">
                  #{{ tag.name }}
                </span>
                <span v-if="notice.tags?.length > 4" class="more-tag-count">+{{ notice.tags.length - 4 }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- PC 페이지네이션 -->
      <div v-if="totalPages > 1" class="pagination-container">
        <button class="page-btn" :disabled="isFirstBlock" @click="prevBlock">이전</button>
        <button v-for="p in visiblePages" :key="p" 
                class="page-num" :class="{ active: currentPage + 1 === p }"
                @click="changePage(p - 1)">{{ p }}</button>
        <button class="page-btn" :disabled="isLastBlock" @click="nextBlock">다음</button>
      </div>
    </div>

    <!-- [모바일 레이아웃] -->
    <div v-else class="mobile-feed-container">
      <div class="feed-grid">
        <NoticeCard 
          v-for="notice in notices" 
          :key="notice.id" 
          :notice="notice" 
          :user-keywords="userKeywords"
          @click="openDetail(notice)"
          @toggle-favorite="handleToggleFavorite(notice)"
          @toggle-keyword="toggleAlertKeyword"
        />
      </div>
      <div ref="infiniteScrollTrigger" class="scroll-status-container">
        <div v-if="loading" class="loading-spinner-v2">
          <div class="spinner-dot"></div>
          <span>공고를 불러오는 중입니다...</span>
        </div>
        <div v-else-if="isLastPage && totalElements > 0" class="no-more-data-v2">
          <div class="end-content">
            <div class="end-check-icon">✓</div>
            <p class="end-text">모든 공고를 확인했습니다.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 상세 모달 -->
    <div v-if="selectedNotice" class="modal-overlay" @mousedown="handleOverlayMouseDown" @mouseup="handleOverlayMouseUp">
      <div class="detail-modal" @mousedown.stop @mouseup.stop>
        <div class="modal-header">
          <div class="header-badges">
            <span class="modal-source-tag" :class="selectedNotice.source">{{ getSourceLabel(selectedNotice.source) }}</span>
            <span class="status-badge" :class="getStatus(selectedNotice).class">{{ getStatus(selectedNotice).text }}</span>
            <span v-if="isReallyNew(selectedNotice.createdAt)" class="new-tag-modal">NEW</span>
          </div>
          <div class="header-actions">
            <button class="modal-favorite-btn" @click="handleToggleFavorite(selectedNotice)">
              {{ selectedNotice.isFavorite ? '❤️' : '🤍' }}
              <span>{{ selectedNotice.interestCount || 0 }}</span>
            </button>
            <button class="btn-close-modal" @click="closeDetail">✕</button>
          </div>
        </div>

        <div class="modal-tabs">
          <button class="tab-item" :class="{ active: activeTab === 'info' }" @click="activeTab = 'info'">🏠 공고 정보</button>
          <button class="tab-item" :class="{ active: activeTab === 'board' }" @click="activeTab = 'board'">💬 소통방</button>
        </div>

        <div class="modal-body">
          <div v-if="activeTab === 'info'" class="tab-content info-tab">
            <div class="info-scroll-area">
              <div class="title-area" :style="{ '--source-color': sourceColor }">
                <h2 class="detail-title">{{ selectedNotice.title }}</h2>
              </div>
              <div class="detail-grid">
                <div class="detail-info-item"><span class="info-label">기관</span><span class="info-value">{{ getSourceLabel(selectedNotice.source) }}</span></div>
                <div class="detail-info-item"><span class="info-label">지역</span><span class="info-value">{{ selectedNotice.region }}</span></div>
                <div class="detail-info-item"><span class="info-label">공고일</span><span class="info-value">{{ formatDateDot(selectedNotice.announcementDate) }}</span></div>
                <div class="detail-info-item"><span class="info-label">마감일</span><span class="info-value">{{ formatDateDot(selectedNotice.deadline) || '-' }}</span></div>
              </div>
              <div class="modal-tag-section">
                <h4 class="section-title">관련 키워드</h4>
                <div class="detail-tags-cloud">
                  <span v-for="tag in getDisplayTags(selectedNotice, 20)" :key="tag.id" 
                        class="clickable-tag" :class="{ matched: userKeywords.includes(tag.name) }"
                        @click="toggleAlertKeyword(tag)">
                    #{{ tag.name }}
                  </span>
                </div>
              </div>
            </div>
            <div class="info-footer">
              <a :href="selectedNotice.link" target="_blank" class="main-apply-btn" :style="{ backgroundColor: sourceColor }">🔗 원문 공고 바로가기</a>
            </div>
          </div>
          <div v-else class="tab-content board-tab">
            <NoticeBoard 
              :notice-id="selectedNotice.id" 
              :notice-title="selectedNotice.title"
              :is-preview="true"
              :is-board-active="selectedNotice.isBoardActive"
              :is-revived="selectedNotice.isRevived"
              :is-dormant="selectedNotice.isDormant"
              :notice-status="selectedNotice.status"
              :source="selectedNotice.source"
              :wakeup-status="wakeupStatus"
              :current-interest-count="selectedNotice.interestCount"
              :is-favorite="selectedNotice.isFavorite"
              @toggle-favorite="handleToggleFavorite(selectedNotice)"
              @toggle-wake-up="handleToggleWakeUp"
              @go-to-hub="handleGoToHub"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/plugins/axios'
import NoticeCard from '@/components/NoticeCard.vue'
import HotRankingBoard from '@/components/HotRankingBoard.vue'
import PageHeader from '@/components/PageHeader.vue'
import NoticeBoard from '@/components/NoticeBoard.vue'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const notices = ref([])
const userKeywords = ref([])
const loading = ref(false)
const searchKeyword = ref(route.query.keyword || '')
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const isLastPage = ref(false)
const activeMenuId = ref(null)
const selectedNotice = ref(null)
const wakeupStatus = ref(null) // [시니어 추가] 깨우기 상태
const isMouseDownOnOverlay = ref(false)
const activeTab = ref('info')

// [시니어 추가] 깨우기 상태 조회
const fetchWakeupStatus = async (noticeId) => {
  if (!noticeId) return
  try {
    const res = await axios.get(`/api/notices/${noticeId}/wakeup-status`)
    wakeupStatus.value = res.data.data
  } catch (e) {
    console.error('깨우기 상태 조회 실패:', e)
  }
}

// [시니어 추가] 소통방 깨우기 실행
const handleToggleWakeUp = async () => {
  if (!selectedNotice.value) return
  try {
    await axios.post(`/api/notices/${selectedNotice.value.id}/wakeup`)
    await fetchWakeupStatus(selectedNotice.value.id)
    
    // 깨우기 성공 시(방이 살아남) selectedNotice 정보 갱신
    if (wakeupStatus.value?.isRevived || wakeupStatus.value?.revived) {
      await uiStore.showAlert('소통방이 깨어났습니다!\n이제 자유롭게 대화하실 수 있습니다.', '알림')
      const res = await axios.get(`/api/notices/${selectedNotice.value.id}`)
      selectedNotice.value = res.data.data
    }
  } catch (e) {
    if (e.response?.data?.message) await uiStore.showAlert(e.response.data.message, '오류')
  }
}

// [시니어 추가] 소통방 허브로 이동 (자동 검색 연동)
const handleGoToHub = (title, sub) => {
  closeDetail() // 상세 모달 닫기
  router.push({
    path: '/board',
    query: { menu: 'NOTICE', sub: sub, hubSearch: title }
  })
}

const activeSources = ref(route.query.sources ? route.query.sources.split(',') : ['ALL'])
const activeStatuses = ref(route.query.statuses ? route.query.statuses.split(',') : (Object.keys(route.query).length === 0 ? ['RECRUITING'] : ['ALL']))
const sortOrder = ref(route.query.sortOrder || 'alarm')
const isNewFilter = ref(route.query.isNew === 'true')

const updateUrlParams = () => {
  const query = { ...route.query }; query.sources = activeSources.value.join(','); query.statuses = activeStatuses.value.join(',');
  if (sortOrder.value !== 'alarm') query.sortOrder = sortOrder.value; else delete query.sortOrder;
  if (searchKeyword.value) query.keyword = searchKeyword.value; else delete query.keyword;
  if (isNewFilter.value) query.isNew = 'true'; else delete query.isNew;
  router.replace({ query });
}

const sourceColor = computed(() => {
  if (!selectedNotice.value) return 'var(--link-color)';
  const colors = { 'LH': '#38a169', 'SH': '#3182ce', 'PRIVATE': '#c08457', 'private': '#c08457', '민간': '#c08457' };
  return colors[selectedNotice.value.source] || 'var(--link-color)';
});

const closeDetail = () => { selectedNotice.value = null; activeTab.value = 'info'; const query = { ...route.query }; delete query.openId; router.replace({ query }); }
const handleOverlayMouseDown = (e) => { isMouseDownOnOverlay.value = e.target.classList.contains('modal-overlay'); }
const handleOverlayMouseUp = (e) => { if (isMouseDownOnOverlay.value && e.target.classList.contains('modal-overlay')) { closeDetail(); } isMouseDownOnOverlay.value = false; }

const infiniteScrollTrigger = ref(null)
const topAnchor = ref(null)
const isMobile = ref(false)
let mediaQuery = null
let observer = null

const allFilterOptions = [
  { name: '전체', value: 'ALL', label: 'ALL', type: 'GLOBAL', color: '#8e8e8e' },
  { name: 'LH 공사', value: 'LH', label: 'LH', type: 'SOURCE', color: '#38a169' },
  { name: 'SH 공사', value: 'SH', label: 'SH', type: 'SOURCE', color: '#3182ce' },
  { name: '민간임대', value: 'PRIVATE', label: '민간', type: 'SOURCE', color: '#c08457' },
  { name: '접수중', value: 'RECRUITING', label: '접수', type: 'STATUS', color: '#38a169' },
  { name: '마감', value: 'CLOSED', label: '마감', type: 'STATUS', color: '#ed4956' },
  { name: '결과발표', value: 'RESULT', label: '결과', type: 'STATUS', color: '#6b46c1' },
  { name: '안내', value: 'INFO', label: '안내', type: 'STATUS', color: '#3182ce' }
]

const getSourceLabel = (source) => (source === 'PRIVATE' || source === 'private' || source === '민간') ? '민간' : source;
const iFilterActive = (f) => { if (f.value === 'ALL') return activeSources.value.includes('ALL') && activeStatuses.value.includes('ALL'); if (f.type === 'SOURCE') return activeSources.value.includes(f.value); if (f.type === 'STATUS') return activeStatuses.value.includes(f.value); return false; }

const toggleUnifiedFilter = (f) => {
  if (f.value === 'ALL') { activeSources.value = ['ALL']; activeStatuses.value = ['ALL']; } 
  else {
    const targetArr = f.type === 'SOURCE' ? activeSources : activeStatuses;
    const allIdx = targetArr.value.indexOf('ALL'); if (allIdx !== -1) targetArr.value.splice(allIdx, 1);
    const idx = targetArr.value.indexOf(f.value); if (idx === -1) targetArr.value.push(f.value); else { targetArr.value.splice(idx, 1); if (activeSources.value.length === 0 && activeStatuses.value.length === 0) { activeSources.value = ['ALL']; activeStatuses.value = ['ALL']; } else if (targetArr.value.length === 0) targetArr.value.push('ALL'); }
  }
  updateUrlParams(); handleSearch();
}

const fetchNotices = async (page, reset = false) => {
  if (loading.value && !reset) return
  loading.value = true
  try {
    const sourcesParam = activeSources.value.includes('ALL') ? null : activeSources.value.join(',');
    const statusesParam = activeStatuses.value.includes('ALL') ? null : activeStatuses.value.join(',');
    const res = await axios.get('/api/notices', { 
      params: { 
        page, 
        size: isMobile.value ? 12 : 15, 
        keyword: searchKeyword.value, 
        sources: sourcesParam, 
        statuses: statusesParam, 
        sortOrder: sortOrder.value,
        isNew: isNewFilter.value || null
      } 
    })
    const data = res.data.data
    if (reset) notices.value = data.content; else notices.value = [...notices.value, ...data.content]
    currentPage.value = data.number; totalElements.value = data.totalElements; totalPages.value = data.totalPages; isLastPage.value = data.last
  } catch (error) { console.error('공고 로드 실패:', error) } finally { loading.value = false }
}

const handleSearch = () => { isLastPage.value = false; fetchNotices(0, true).then(() => { if (isMobile.value) initInfiniteScroll() }) }
const clearSearch = () => { searchKeyword.value = ''; updateUrlParams(); handleSearch(); }
const handleSortChange = () => { updateUrlParams(); handleSearch(); }
const changePage = (p) => { if (p >= 0 && p < totalPages.value) fetchNotices(p, true).then(() => topAnchor.value?.scrollIntoView({ behavior: 'smooth' })) }

const openDetail = (notice) => { 
  selectedNotice.value = notice; 
  fetchWakeupStatus(notice.id); // [시니어 추가] 깨우기 상태 정보 로드
  router.replace({ query: { ...route.query, openId: notice.id } });
}
const getStatus = (notice) => {
  const systemTag = notice.tags?.find(t => ['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내'].includes(t.name));
  const statusText = systemTag ? systemTag.name : '안내';
  let statusClass = 'info'; if (statusText === '접수중') statusClass = 'open'; else if (statusText === '마감' || statusText === '발표완료' || statusText === '이전안내') statusClass = 'closed'; else if (statusText === '결과발표') statusClass = 'result';
  return { text: statusText, class: statusClass };
}
const calculateDDay = (notice) => { if (notice.status === 'CLOSED' || notice.status === 'RESULT' || notice.status === 'INFO' || !notice.deadline) return '-'; const target = new Date(notice.deadline); const today = new Date(); target.setHours(0,0,0,0); today.setHours(0,0,0,0); const diff = Math.ceil((target - today) / 86400000); return diff < 0 ? '-' : (diff === 0 ? 'D-Day' : `D-${diff}`); }
const isReallyExpired = (deadline) => { if (!deadline) return false; const now = new Date(); const end = new Date(deadline); end.setHours(23, 59, 59, 999); return now > end; }

const isReallyNew = (createdAt) => {
  if (!createdAt) return false;
  const created = new Date(createdAt);
  const now = new Date();
  return (now - created) < (48 * 60 * 60 * 1000);
}

const getDisplayTags = (notice, limit = 4) => { if (!notice.tags || notice.tags.length === 0) return []; const systemTags = ['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내']; let allFiltered = notice.tags.filter(tag => !systemTags.includes(tag.name)); allFiltered.sort((a, b) => { const aSpecial = a.type === 'SPECIAL'; const bSpecial = b.type === 'SPECIAL'; if (aSpecial && !bSpecial) return -1; if (!aSpecial && bSpecial) return 1; return 0; }); return allFiltered.slice(0, limit); }

const toggleAlertKeyword = async (tag) => { const isRegistered = userKeywords.value.includes(tag.name); try { if (isRegistered) { await axios.delete('/api/user-tags', { params: { name: tag.name, type: tag.type } }); userKeywords.value = userKeywords.value.filter(k => k !== tag.name); } else { await axios.post('/api/user-tags', { name: tag.name, type: tag.type }); userKeywords.value.push(tag.name); } } catch (e) { console.error('키워드 처리 실패:', e); } }

const formatDateDot = (s) => s ? s.split('T')[0].replace(/-/g, '.') : '-';

const handleToggleFavorite = async (notice) => { try { if (!notice.isFavorite) { await axios.post('/api/user-notices', { noticeId: notice.id, memo: '' }); notice.isFavorite = true; notice.interestCount++ } else { await axios.delete(`/api/user-notices/notice/${notice.id}`); notice.isFavorite = false; notice.interestCount-- } } catch (e) { console.error('관심 등록 실패:', e); } }

const updateMobileStatus = (e) => { isMobile.value = e.matches; handleSearch(); }
const initInfiniteScroll = () => { if (observer) observer.disconnect(); observer = new IntersectionObserver((entries) => { if (entries[0].isIntersecting && !loading.value && !isLastPage.value && isMobile.value) { fetchNotices(currentPage.value + 1); } }, { threshold: 0.1, rootMargin: '100px' }); nextTick(() => { if (infiniteScrollTrigger.value) observer.observe(infiniteScrollTrigger.value); }); }

const visiblePages = computed(() => { const range = []; const pageSize = 5; const currentBlock = Math.floor(currentPage.value / pageSize); const startPage = currentBlock * pageSize + 1; const endPage = Math.min(totalPages.value, (currentBlock + 1) * pageSize); for (let i = startPage; i <= endPage; i++) range.push(i); return range; });
const isFirstBlock = computed(() => Math.floor(currentPage.value / 5) === 0);
const isLastBlock = computed(() => Math.floor(currentPage.value / 5) >= Math.floor((totalPages.value - 1) / 5));
const prevBlock = () => { if (!isFirstBlock.value) changePage(Math.floor(currentPage.value / 5) * 5 - 1) };
const nextBlock = () => { if (!isLastPage.value) changePage((Math.floor(currentPage.value / 5) + 1) * 5) };

const handleKeyDown = (e) => { if (e.key === 'Escape' && selectedNotice.value) { closeDetail(); } }

watch(() => route.query.openId, async (newOpenId) => {
  if (newOpenId) {
    try {
      const res = await axios.get(`/api/notices/${newOpenId}`);
      selectedNotice.value = res.data.data;
      fetchWakeupStatus(newOpenId); // [시니어 추가] 깨우기 상태 정보 로드
      activeTab.value = 'info';
    } catch (e) {
      console.warn('알림 대상 공고 조회 실패:', e);
    }
  } else {
    selectedNotice.value = null;
  }
});

watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword !== undefined) {
    searchKeyword.value = newKeyword;
    handleSearch();
  }
});

onMounted(async () => {
  window.addEventListener('keydown', handleKeyDown);
  mediaQuery = window.matchMedia('(max-width: 768px)'); isMobile.value = mediaQuery.matches;
  mediaQuery.addEventListener('change', updateMobileStatus);
  try { const kwRes = await axios.get('/api/user-tags'); userKeywords.value = kwRes.data.data.map(k => k.name) } catch (e) {}
  
  if (route.query.sources) activeSources.value = route.query.sources.split(',');
  if (route.query.statuses) activeStatuses.value = route.query.statuses.split(',');
  
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword;
  }
  
  await fetchNotices(0, true)
  if (isMobile.value) initInfiniteScroll()
  
  const openId = route.query.openId;
  if (openId) { 
    try { 
      const res = await axios.get(`/api/notices/${openId}`); 
      selectedNotice.value = res.data.data; 
      activeTab.value = 'info'; 
    } catch (e) {
      console.warn('자동 오픈 대상 공고를 불러오지 못했습니다:', e);
    } 
  }
})

onUnmounted(() => { window.removeEventListener('keydown', handleKeyDown); if (mediaQuery) mediaQuery.removeEventListener('change', updateMobileStatus); if (observer) observer.disconnect(); })
</script>

<style scoped>
.feed-container { width: 100%; min-height: 100vh; position: relative; }

.notice-disclaimer { max-width: 1200px; margin: 0 auto 1.5rem; padding: 0 20px; }
.disclaimer-content { background-color: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 12px 16px; display: flex; align-items: flex-start; gap: 10px; }
.light-bulb { font-size: 1.1rem; line-height: 1.4; }
.disclaimer-content .text { font-size: 0.82rem; color: var(--text-secondary); line-height: 1.5; margin: 0; word-break: keep-all; }
.disclaimer-content .highlight { color: var(--text-primary); font-weight: 700; display: inline-block; margin-left: 2px; }
.scroll-anchor { height: 0; position: relative; top: -40px; visibility: hidden; }
.top-controls { max-width: 1200px; margin: 0 auto; padding-bottom: 0.5rem; }
.unified-filter-bar { margin-top: 1.5rem; margin-bottom: 1.5rem; }
.filter-scroll { display: flex; gap: 1.2rem; overflow-x: auto; padding: 10px 20px; justify-content: center; scrollbar-width: none; }
.filter-scroll::-webkit-scrollbar { display: none; }
.filter-item { display: flex; flex-direction: column; align-items: center; gap: 8px; cursor: pointer; color: var(--text-primary); opacity: 0.4; transition: all 0.2s; }
.filter-item.active { opacity: 1; }
.filter-circle { width: 52px; height: 52px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: 800; font-size: 0.65rem; border: 2px solid transparent; box-shadow: 0 0 0 2px var(--border-color); transition: all 0.2s; }
.filter-item.active .filter-circle { box-shadow: 0 0 0 2px var(--link-color); transform: scale(1.05); }
.filter-label { font-size: 0.7rem; font-weight: 600; color: var(--text-primary); }
.search-sort-row { display: flex; align-items: center; gap: 12px; padding: 0 20px; margin-bottom: 1.5rem; }
.search-container-v2 { flex: 1; min-width: 0; }
.search-input-wrapper { position: relative; display: flex; align-items: center; }
.search-icon-inside { position: absolute; left: 14px; color: var(--text-secondary); font-size: 0.9rem; opacity: 0.7; pointer-events: none; }
.input-field-search-v2 { width: 100%; border: 1px solid var(--border-color); border-radius: 12px; padding: 10px 40px 10px 38px; outline: none; background-color: var(--card-bg); color: var(--text-primary); font-size: 0.95rem; transition: all 0.2s; }
.btn-clear-search-v2 { position: absolute; right: 12px; background: none; border: none; color: var(--text-secondary); font-size: 1.3rem; cursor: pointer; padding: 0; display: flex; align-items: center; justify-content: center; }
.sort-wrapper-v2 { background-color: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 0 12px; display: flex; align-items: center; height: 44px; }
.sort-select-v2 { border: none; background: var(--card-bg); font-size: 0.9rem; font-weight: 700; color: var(--text-primary); cursor: pointer; outline: none; height: 100%; }
.pc-list-container { max-width: 1200px; margin: 0 auto; padding: 0 20px 50px; }
.notice-table { width: 100%; border-collapse: collapse; background-color: var(--card-bg); border-radius: 8px; border: 1px solid var(--border-color); table-layout: fixed; }
.notice-table th { background-color: var(--hover-bg); padding: 15px; text-align: center; font-size: 0.9rem; color: var(--text-secondary); border-bottom: 1px solid var(--border-color); }
.notice-table td { padding: 15px; border-bottom: 1px solid var(--border-color); cursor: pointer; text-align: center; font-size: 0.9rem; color: var(--text-primary); }
.table-row:hover { background-color: var(--hover-bg); }
.col-source { width: 80px; } .col-region { width: 90px; } .col-title { width: auto; } .col-status { width: 110px; } .col-dday { width: 90px; } .col-dates { width: 200px; } .col-interest { width: 100px; } .col-tags { width: 180px; }
.table-source-tag { padding: 4px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; color: white; display: inline-block; min-width: 45px; }

.title-wrapper { display: flex; align-items: center; gap: 6px; }
.new-tag-mini { background-color: #ed4956; color: white; font-size: 0.6rem; font-weight: 900; padding: 1px 4px; border-radius: 3px; flex-shrink: 0; }
.new-tag-modal { background-color: #ed4956; color: white; font-size: 0.7rem; font-weight: 900; padding: 4px 8px; border-radius: 6px; box-shadow: 0 2px 8px rgba(237, 73, 86, 0.3); }

.table-source-tag.LH { background-color: #38a169; } .table-source-tag.SH { background-color: #3182ce; } .table-source-tag.PRIVATE { background-color: #c08457; }
.title-text { font-size: 0.95rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; text-align: left; }
.status-badge { padding: 4px 12px; border-radius: 4px; font-size: 0.75rem; font-weight: 700; }
.status-badge.info { background-color: rgba(49, 130, 206, 0.1); color: #3182ce; } .status-badge.result { background-color: rgba(107, 70, 193, 0.1); color: #6b46c1; } .status-badge.open { background-color: rgba(56, 161, 105, 0.1); color: #38a169; } .status-badge.closed { background-color: rgba(237, 73, 86, 0.1); color: #ed4956; }
.interest-cell { display: flex; align-items: center; justify-content: flex-start; gap: 8px; width: 50px; margin: 0 auto; }
.heart-icon { font-size: 1.1rem; transition: transform 0.1s; cursor: pointer; width: 20px; display: flex; justify-content: center; }
.heart-icon.filled { color: #ed4956; }
.interest-count { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); min-width: 20px; text-align: left; }
.tags-wrapper { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-start; }
.mini-tag { 
  font-size: 0.75rem; 
  color: var(--text-secondary); 
  font-weight: 500; 
  cursor: pointer; 
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.2s;
}
.mini-tag:hover {
  background-color: var(--hover-bg);
  color: var(--link-color);
}
.mini-tag.matched { color: var(--link-color); font-weight: 700; }
.pagination-container { display: flex; justify-content: center; align-items: center; gap: 8px; margin-top: 40px; padding-bottom: 20px; }
.page-btn, .page-num { background: var(--card-bg); border: 1px solid var(--border-color); padding: 8px 14px; border-radius: 6px; cursor: pointer; color: var(--text-primary); font-size: 0.9rem; font-weight: 600; }
.page-num.active { background-color: var(--link-color); color: white; border-color: var(--link-color); }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(4px); z-index: 2000; display: flex; align-items: center; justify-content: center; padding: 20px; }
.detail-modal { background: var(--card-bg); width: 100%; max-width: 550px; border-radius: 20px; overflow: hidden; height: 560px; border: 1px solid var(--border-color); display: flex; flex-direction: column; box-shadow: 0 30px 60px rgba(0,0,0,0.4); }
.modal-header { padding: 24px 24px 16px; display: flex; justify-content: space-between; align-items: center; background-color: var(--hover-bg); flex-shrink: 0; }
.header-badges { display: flex; flex-direction: row; align-items: center; gap: 8px; }
.modal-source-tag { padding: 0 14px; border-radius: 6px; font-size: 0.75rem; font-weight: 900; color: white; display: flex; align-items: center; justify-content: center; height: 30px; white-space: nowrap; }
.modal-source-tag.LH { background-color: #38a169; } .modal-source-tag.SH { background-color: #3182ce; } .modal-source-tag.PRIVATE { background-color: #c08457; }
.status-badge { padding: 0 12px; border-radius: 6px; font-size: 0.75rem; font-weight: 700; display: flex; align-items: center; justify-content: center; height: 30px; white-space: nowrap; }
.header-actions { display: flex; align-items: center; gap: 12px; }

.btn-close-modal {
  background: var(--hover-bg);
  border: 1px solid var(--border-color);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
  line-height: 1;
}
.btn-close-modal:hover {
  background-color: var(--border-color);
  color: var(--text-primary);
  transform: rotate(90deg);
}

.modal-favorite-btn { background: var(--card-bg); border: 1px solid var(--border-color); padding: 6px 14px; border-radius: 20px; cursor: pointer; display: flex; align-items: center; gap: 6px; color: var(--text-primary); transition: all 0.2s; }
.modal-favorite-btn:hover { background-color: var(--border-color); }
.modal-tabs { display: flex; background-color: var(--hover-bg); border-bottom: 1px solid var(--divider-color); border-top: 1px solid var(--divider-color); flex-shrink: 0; }
.tab-item { flex: 1; padding: 12px; border: none; background: none; font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; position: relative; }
.tab-item.active { color: var(--link-color); }
.tab-item.active::after { content: ""; position: absolute; bottom: 0; left: 0; width: 100%; height: 2px; background-color: var(--link-color); }

.modal-body { padding: 16px 24px; overflow: hidden; flex: 1; display: flex; flex-direction: column; }
.tab-content { flex: 1; display: flex; flex-direction: column; padding: 0; min-height: 0; }
.tab-content.board-tab { overflow: hidden; }

.info-tab { display: flex; flex-direction: column; height: 100%; }
.info-scroll-area { flex: 1; overflow-y: auto; padding-right: 4px; scrollbar-width: thin; }

.title-area { background-color: var(--hover-bg); padding: 12px 16px; border-radius: 12px; border-left: 4px solid var(--source-color); margin-top: 10px; margin-bottom: 12px; }
.detail-title { font-size: 1.1rem; font-weight: 800; line-height: 1.4; margin: 0; word-break: keep-all; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 12px; }
.detail-info-item { display: flex; flex-direction: column; gap: 2px; padding: 8px 12px; background-color: var(--hover-bg); border-radius: 10px; }
.info-label { font-size: 0.65rem; color: var(--text-secondary); font-weight: 700; } .info-value { font-size: 0.85rem; font-weight: 800; }
.modal-tag-section { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.section-title { font-size: 0.85rem; font-weight: 800; margin: 0; color: var(--text-primary); }
.detail-tags-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.clickable-tag { font-size: 0.75rem; color: var(--text-primary); background-color: var(--hover-bg); padding: 4px 10px; border-radius: 20px; border: 1px solid var(--border-color); cursor: pointer; transition: all 0.2s; }
.clickable-tag:hover { border-color: var(--link-color); color: var(--link-color); }
.clickable-tag.matched { 
  color: white; 
  background-color: var(--link-color); 
  border-color: var(--link-color); 
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
}

.info-footer { flex-shrink: 0; padding-top: 20px; }
.main-apply-btn { 
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 52px; color: white; text-align: center; border-radius: 12px; text-decoration: none; font-weight: 800; font-size: 0.95rem; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); transition: all 0.2s;
  margin: 0;
}
.main-apply-btn:hover { transform: translateY(-1px); filter: brightness(1.1); }

.scroll-status-container {
  padding: 40px 0 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.loading-spinner-v2 {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 0.85rem;
  font-weight: 600;
}

.spinner-dot {
  width: 24px;
  height: 24px;
  border: 3px solid var(--border-color);
  border-top-color: var(--link-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.no-more-data-v2 {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 400px;
}

.end-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  opacity: 0.8;
}

.end-check-icon {
  width: 44px;
  height: 44px;
  background-color: var(--hover-bg);
  border: 2px solid var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--link-color);
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
}

.end-text {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--text-secondary);
  margin: 0;
}

@media (max-width: 768px) { 
  .search-sort-row { gap: 8px; padding: 0 15px; } 
  .filter-scroll { justify-content: flex-start; } 
  .detail-modal { width: 95%; height: 90vh; max-height: 700px; } 
}

.ranking-section {
  max-width: 1200px;
  margin: 0 auto 25px;
  padding: 0 20px;
}

@media (min-width: 992px) {
  .ranking-section {
    max-width: 1200px; /* 아래 리스트와 너비 동일하게 설정 */
  }
}
</style>
