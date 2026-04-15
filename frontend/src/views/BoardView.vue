<template>
  <div class="board-view-container">
    <!-- 스크롤 복원 중 토스트 -->
    <Transition name="restore-toast">
      <div v-if="isRestoring" class="restore-toast-bar">
        <span class="restore-dots">
          <span></span><span></span><span></span>
        </span>
        이전 게시글로 이동 중...
      </div>
    </Transition>
    <PageHeader 
      :icon="currentMenuIcon" 
      :title="isMobile && noticeId ? '공고 커뮤니티' : currentMenuTitle" 
      :stats-text="null" 
      :stats-value="currentStatsValue"
      :bio="isMobile && noticeId ? '' : currentMenuBio"
    />

    <div class="board-layout">
      <CommunitySidebar 
        ref="sidebarRef"
        v-model:activeMenu="activeMenu"
        v-model:activeSubCategory="activeSubCategory"
      >
        <template #extra>
          <div v-if="noticeId && noticeInfo" class="notice-info-card">
            <div v-if="isMobile" class="mobile-notice-title-box">
              <h3 class="m-notice-title">{{ noticeInfo.title }}</h3>
            </div>
            <h4 class="card-title">📌 공고 요약</h4>
            <div class="summary-list">
              <div class="s-item"><span class="label">기관</span><span class="value">{{ getSourceLabel(noticeInfo.source) }}</span></div>
              <div class="s-item"><span class="label">지역</span><span class="value">{{ noticeInfo.region }}</span></div>
              <div class="s-item danger"><span class="label">마감</span><span class="value">{{ formatDateDot(noticeInfo.deadline) || '확인필요' }}</span></div>
            </div>
            <button @click="openOriginalLink(noticeInfo)" class="btn-origin">🔗 원문 공고보기</button>
          </div>
        </template>
      </CommunitySidebar>

      <main class="board-main-content">
        <div v-if="activeMenu === 'NOTICE' && !noticeId" class="notice-hub-content">
          <!-- [시니어 조치] 상태 가이드 및 기간 안내 통합 배너 -->
          <div class="hub-info-banner">
            <div class="hub-lifecycle-info">
              <span v-if="activeSubCategory === 'MY'" class="lifecycle-tip">⭐️ 공고 리스트에서 하트(❤️)를 누르면 내 관심방에 추가돼요!</span>
              <span v-else-if="activeSubCategory === 'PREPARING'" class="lifecycle-tip">🔒 관심(❤️) 10명이 모이면 소통방이 열려요!</span>
              <span v-else-if="activeSubCategory === 'ACTIVE'" class="lifecycle-tip">✨ 현재 자유로운 대화가 가능한 활발한 방입니다.</span>
              <span v-else-if="activeSubCategory === 'ARCHIVED'" class="lifecycle-tip">😴 7일간 글이 없으면 보관돼요. (⚡관심 인원의 10%가 깨워야 함)</span>
              
              <span class="info-divider">|</span>
              <span class="info-text">💡 최근 1년 이내의 공고만 표시됩니다.</span>
            </div>
          </div>

          <!-- [시니어 추가] 실시간 소통방 검색바 -->
          <div class="hub-search-area">
            <div class="hub-search-wrapper">
              <span class="h-search-icon">🔍</span>
              <input 
                type="text" 
                v-model="hubSearchKeyword" 
                placeholder="현재 탭에서 공고명으로 실시간 검색..." 
                class="hub-search-input"
                autocomplete="off"
              />
              <button v-if="hubSearchKeyword" class="h-clear-btn" @click="clearHubSearch">&times;</button>
            </div>
          </div>

          <div v-if="filteredNoticeBoards.length > 0" class="notice-grid">
            <div v-for="board in filteredNoticeBoards" :key="board.id" 
                 class="notice-board-card" @click="enterNoticeBoard(board.id)">
              <div class="card-top">
                <div class="badge-group">
                  <span class="source-tag" :class="board.source">{{ getSourceLabel(board.source) }}</span>
                </div>
                <div class="status-badge-modern">
                  <template v-if="!board.isBoardActive">
                    <span class="s-badge preparing">🔒 오픈 준비</span>
                  </template>
                  <template v-else-if="board.isDormant">
                    <span class="s-badge archived">😴 보관된</span>
                  </template>
                  <template v-else>
                    <span class="s-badge active">🔥 활발한</span>
                  </template>
                </div>
              </div>
              <h3 class="card-title">{{ board.title }}</h3>
              <div class="card-bottom">
                <span class="interest-count">👥 {{ board.interestCount }}명 참여 중</span>
                <span class="d-day" v-if="board.deadline">{{ calculateDDay(board.deadline) }}</span>
              </div>
            </div>
          </div>

          <div v-else class="notice-hub-empty">
            <div class="empty-state type-hub">
              <span class="empty-icon">
                {{ hubSearchKeyword ? '🔎' : '📢' }}
              </span>
              <h3 class="empty-title">
                {{ 
                  hubSearchKeyword ? `'${hubSearchKeyword}' 검색 결과가 없습니다` :
                  (activeSubCategory === 'MY' ? '관심 있는 소통방이 없습니다' :
                  activeSubCategory === 'CLOSED' ? '마감된 소통방 기록이 없습니다' :
                  '현재 활성화된 소통방이 없습니다')
                }}
              </h3>
              <p class="empty-desc">
                {{
                  hubSearchKeyword ? '검색어를 확인하시거나 다른 키워드로 검색해 보세요.' :
                  (activeSubCategory === 'MY' ? '공고 리스트에서 관심 있는 공고에 하트를 눌러보세요!' :
                  '이웃들과의 소통을 위해 새로운 소통방이 열리기를 기다려주세요.')
                }}
              </p>
              <button v-if="hubSearchKeyword" class="btn-reset-search" @click="clearHubSearch">검색 초기화</button>
            </div>
          </div>
        </div>

        <div v-else class="post-list-section">
          <RankingBoard v-if="activeMenu === 'RANKING'" />

          <template v-else-if="activeMenu === 'NOTICE' && noticeId && noticeInfo">
            <div v-if="noticeInfo && wakeupStatus" class="wakeup-notice-banner-hub" :class="{ 'is-active-banner': noticeInfo.isBoardActive && !noticeInfo.isDormant }">
              <div class="w-content">
                <span class="w-icon">
                  {{ !noticeInfo.isBoardActive ? '🔒' : (noticeInfo.isDormant ? '😴' : '✨') }}
                </span>
                <div class="w-text">
                  <template v-if="noticeInfo.isBoardActive && !noticeInfo.isDormant">
                    <strong>소통방이 활발하게 운영 중입니다 ✨</strong>
                    <p>이웃들과 함께 해당 공고의 실시간 정보를 자유롭게 공유해 주세요!</p>
                  </template>
                  <template v-else-if="noticeInfo.isBoardActive && noticeInfo.isDormant">
                    <strong>이 소통방은 보관중입니다 😴</strong>
                    <div class="w-stats-detail">
                      현재 <b>관심 {{ noticeInfo.interestCount }}명</b> · <b>깨우기 {{ wakeupStatus.currentCount }}명</b> 참여 중
                    </div>
                    <p class="w-guide-text">목표 인원({{ wakeupStatus.targetCount }}명) 달성 시 다시 활성화됩니다!</p>
                  </template>
                  <template v-else>
                    <strong>이 소통방은 아직 잠겨있어요</strong>
                    <p>이웃들의 관심을 기다리고 있습니다. (관심 유저 10명이 되면 소통방이 열립니다)</p>
                  </template>
                </div>
              </div>
              <div v-if="noticeInfo.isBoardActive && noticeInfo.isDormant" class="w-action">
                <div class="w-gauge-container">
                  <div class="w-gauge-fill" :style="{ width: (wakeupStatus.currentCount / wakeupStatus.targetCount * 100) + '%' }"></div>
                  <span class="w-gauge-text">{{ wakeupStatus.currentCount }} / {{ wakeupStatus.targetCount }}</span>
                </div>
                <button class="btn-wakeup-action" :class="{ 'is-clicked': wakeupStatus.isClicked }" @click="handleToggleWakeUp">
                  {{ wakeupStatus.isClicked ? '깨우기 완료 ✨' : '소통방 깨우기 ⚡' }}
                </button>
              </div>
            </div>

            <NoticeBoard 
              :key="`${noticeId}-${noticeInfo?.isBoardActive}-${noticeInfo?.isRevived}-${noticeInfo?.isDormant}`"
              :notice-id="Number(noticeId)" 
              :is-board-active="noticeInfo?.isBoardActive || false"
              :is-revived="noticeInfo?.isRevived || false"
              :is-dormant="noticeInfo?.isDormant || false"
              :current-interest-count="noticeInfo?.interestCount || 0"
              :is-favorite="noticeInfo?.isFavorite || false"
              :source="noticeInfo?.source"
              :notice-status="noticeInfo?.status"
              :active-sub-category="activeSubCategory"
              :wakeup-status="wakeupStatus"
              @toggle-favorite="handleToggleFavorite"
              @post-created="handlePostCreated"
            />
          </template>

          <div v-else-if="activeMenu !== 'NOTICE' || !noticeId" class="general-board-container">
            <div class="general-board-header">
              <div class="board-info">
                <span class="post-count">게시글 {{ totalElements }}개</span>
              </div>
              <div class="header-action-group">
                <button class="btn-write" @click="goToGeneralWrite">📝 글쓰기</button>
              </div>
            </div>

            <div v-if="activeTagName" class="filter-indicator">
              <span>🏷️ <b>#{{ activeTagName }}</b> 검색 결과</span>
              <button class="btn-clear-filter" @click="clearTagNameFilter">초기화 ✕</button>
            </div>
            
            <PostList 
              :posts="posts" 
              :loading="loading" 
              :has-more="hasMore" 
              :type="activeMenu"
              :notice-id="noticeId"
              @load-more="fetchPosts(true)"
            />
            <div ref="loadMoreTrigger" class="load-more-trigger"></div>
          </div>
        </div>
      </main>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/plugins/axios'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import PageHeader from '@/components/PageHeader.vue'
import NoticeBoard from '@/components/NoticeBoard.vue'
import CommunitySidebar from '@/components/CommunitySidebar.vue'
import PostList from '@/components/PostList.vue'
import RankingBoard from '@/components/RankingBoard.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const sidebarRef = ref(null)
const loadMoreTrigger = ref(null)

const activeMenu = ref(route.query.menu || (route.params.noticeId ? 'NOTICE' : 'GENERAL'))
const activeSubCategory = ref(route.query.sub || (activeMenu.value === 'NOTICE' ? 'PREPARING' : 'ALL'))
const noticeId = computed(() => route.params.noticeId)
const noticeInfo = ref(null)
const loading = ref(false)
const showTopBtn = ref(false)
const isMobile = ref(window.innerWidth <= 992)

const posts = ref([])
const totalElements = ref(0) 
const currentPage = ref(0)
const hasMore = ref(false)
const activeTagName = ref('')

const myNoticeBoards = ref([])
const activeNoticeBoards = ref([])
const preparingNoticeBoards = ref([])
const archivedNoticeBoards = ref([])
const hubSearchKeyword = ref('') // [시니어 추가] 실시간 검색어

const clearHubSearch = () => { hubSearchKeyword.value = '' }

const wakeupStatus = ref(null)
const fetchWakeupStatus = async () => {
  if (!noticeId.value) return
  try {
    const res = await axios.get(`/api/notices/${noticeId.value}/wakeup-status`)
    wakeupStatus.value = res.data.data
  } catch (e) {}
}

const handleToggleWakeUp = async () => {
  if (!authStore.isAuthenticated) { await uiStore.showAlert('로그인이 필요한 서비스입니다.', '안내'); return }
  try {
    await axios.post(`/api/notices/${noticeId.value}/wakeup`)
    await fetchWakeupStatus()
    const isNowRevived = wakeupStatus.value?.isRevived || wakeupStatus.value?.revived;
    if (isNowRevived) {
      await uiStore.showAlert('소통방이 깨어났습니다!\n이제 자유롭게 대화하실 수 있습니다.', '알림');
      if (noticeInfo.value) {
        noticeInfo.value = { ...noticeInfo.value, isRevived: true, isBoardActive: true, isDormant: false };
      }
    }
  } catch (e) {
    if (e.response?.data?.message) await uiStore.showAlert(e.response.data.message, '오류')
  }
}

const isBoardClosed = computed(() => noticeInfo.value?.isDormant === true)
const mainMenus = [
  { id: 'GENERAL', name: '통합광장', icon: '🏛️' },
  { id: 'NOTICE', name: '공고소통방', icon: '📢' },
  { id: 'REVIEW', name: '당첨후기', icon: '✨' },
  { id: 'BARTER', name: '물물교환', icon: '🔄' },
  { id: 'PARTNER', name: '파트너스', icon: '🤝' },
  { id: 'RANKING', name: '활동랭킹', icon: '🎖️' }
]
const currentMenu = computed(() => mainMenus.find(m => m.id === activeMenu.value))
const currentMenuTitle = computed(() => noticeId.value ? noticeInfo.value?.title : currentMenu.value?.name)
const currentMenuIcon = computed(() => noticeId.value ? '📢' : currentMenu.value?.icon)
const currentMenuBio = computed(() => {
  if (noticeId.value && noticeInfo.value) return noticeInfo.value.title
  const bios = {
    GENERAL: '우리 동네 이웃들과 나누는 자유로운 이야기 공간입니다.',
    NOTICE: '청약 공고별 전용 소통방에서 실시간 정보를 나눠보세요.',
    REVIEW: '내 집 마련의 기쁨과 생생한 당첨 노하우를 공유합니다.',
    PARTNER: '이사, 청소, 인테리어 등 믿을 수 있는 업체 정보를 확인하세요.',
    BARTER: '잠자고 있는 물건을 이웃과 가치 있게 나누는 신뢰 기반 물물교환입니다.',
    RANKING: '커뮤니티 활동을 통해 명예의 전당에 도전해 보세요!'
  }
  return bios[activeMenu.value] || '이웃과 함께 나누는 따뜻한 정보 공간입니다.'
})

const currentStatsValue = computed(() => {
  if (noticeId.value) return `참여유저 ${noticeInfo.value?.interestCount || 0}`
  if (activeMenu.value === 'NOTICE') return `소통방 ${filteredNoticeBoards.value.length}`
  return `게시글 ${totalElements.value}`
})

const filteredNoticeBoards = computed(() => {
  let baseList = []
  if (activeSubCategory.value === 'MY') baseList = myNoticeBoards.value
  else if (activeSubCategory.value === 'PREPARING') baseList = preparingNoticeBoards.value
  else if (activeSubCategory.value === 'ACTIVE') baseList = activeNoticeBoards.value
  else if (activeSubCategory.value === 'ARCHIVED') baseList = archivedNoticeBoards.value
  else baseList = activeNoticeBoards.value

  if (!hubSearchKeyword.value.trim()) return baseList
  const kw = hubSearchKeyword.value.toLowerCase()
  return baseList.filter(b => b.title.toLowerCase().includes(kw))
})

const isRestoring = ref(false)
const scrollToPost = (postId) => {
  if (!postId) return
  isRestoring.value = true
  const attemptScroll = () => {
    const element = document.getElementById(`post-${postId}`)
    if (element) {
      const headerOffset = 145 
      const elementPosition = element.getBoundingClientRect().top + window.pageYOffset
      const offsetPosition = elementPosition - headerOffset
      window.scrollTo({ top: offsetPosition, behavior: 'instant' })
      const query = { ...route.query }; delete query.lastPostId
      router.replace({ query }).finally(() => { setTimeout(() => { isRestoring.value = false }, 500) })
    } else if (hasMore.value && !loading.value) {
      fetchPosts(true).then(() => { setTimeout(attemptScroll, 100) })
    } else { isRestoring.value = false }
  }
  requestAnimationFrame(() => requestAnimationFrame(attemptScroll))
}

const fetchPosts = async (isMore = false) => {
  if (activeMenu.value === 'NOTICE' && !noticeId.value) return
  if (activeMenu.value === 'RANKING') return
  if (loading.value && !isMore) return
  loading.value = true
  if (!isMore) { posts.value = []; currentPage.value = 0 }
  try {
    const params = {
      type: activeMenu.value,
      subCategory: activeSubCategory.value === 'ALL' ? null : activeSubCategory.value,
      tagName: activeTagName.value || null,
      page: currentPage.value, size: 10, sort: 'createdAt,desc'
    }
    if (activeMenu.value === 'NOTICE' && noticeId.value) params.noticeId = noticeId.value
    const res = await axios.get('/api/posts', { params })
    const data = res.data.data
    totalElements.value = data.totalElements || 0
    if (isMore) posts.value = [...posts.value, ...data.content]
    else {
      posts.value = data.content
      const targetId = route.query.lastPostId || sessionStorage.getItem('lastViewedPostId')
      if (targetId) {
        sessionStorage.removeItem('lastViewedPostId')
        scrollToPost(targetId)
      }
    }
    hasMore.value = !data.last; currentPage.value++
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const clearTagNameFilter = () => { activeTagName.value = ''; fetchPosts() }
const goToGeneralWrite = () => router.push({ path: '/board/write', query: { type: activeMenu.value, sub: activeSubCategory.value } })

const fetchNoticeInfo = async () => {
  if (!noticeId.value) { if (activeMenu.value === 'NOTICE') fetchNoticeHubData(); else fetchPosts(); return }
  try {
    const res = await axios.get(`/api/notices/${noticeId.value}`)
    noticeInfo.value = res.data.data
    fetchWakeupStatus()
    if (isMobile.value) {
      nextTick(() => {
        const infoCard = document.querySelector('.notice-info-card')
        if (infoCard) infoCard.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
    }
  } catch (e) { router.push('/board') }
}

const fetchNoticeHubData = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const myRes = await axios.get('/api/user-notices', { params: { size: 100 } })
    myNoticeBoards.value = myRes.data.data.content.map(un => ({ 
      id: un.noticeId, title: un.noticeTitle, source: un.noticeSource, 
      deadline: un.noticeDeadline, interestCount: un.interestCount || 0, 
      isBoardActive: !!un.isBoardActive, isDormant: !!un.isDormant, status: un.noticeStatus 
    }))
    
    // [시니어 조치] 탭에 따라 필요한 상태값을 동적으로 결정
    let statusFilter = 'RECRUITING';
    if (activeSubCategory.value === 'ARCHIVED') {
      statusFilter = 'CLOSED,RESULT,INFO'; // 보관된 탭은 마감 위주
    } else if (activeSubCategory.value === 'ACTIVE') {
      statusFilter = 'RECRUITING,CLOSED,RESULT'; // 활성 탭은 마감되었어도 활성 상태라면 포함
    }

    const allRes = await axios.get('/api/notices', { 
      params: { 
        size: 1000, 
        sortOrder: 'interest', 
        statuses: statusFilter 
      } 
    })
    const allData = allRes.data.data.content
    preparingNoticeBoards.value = allData.filter(b => !b.isBoardActive)
    activeNoticeBoards.value = allData.filter(b => b.isBoardActive && !b.isDormant)
    archivedNoticeBoards.value = allData.filter(b => b.isBoardActive && b.isDormant)
  } catch (e) { console.error('소통방 데이터 로드 실패:', e) } finally { loading.value = false }
}

const openOriginalLink = (notice) => {
  if (!notice.link) return;
  // 모든 기관 공통: Referer 차단(noreferrer)을 통해 보안 에러를 우회
  const link = document.createElement('a');
  link.href = notice.link;
  link.target = '_blank';
  link.rel = 'noreferrer';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

const enterNoticeBoard = (id) => { router.push({ path: `/board/${id}`, query: { ...route.query } }) }
const handlePostCreated = () => { if (sidebarRef.value) sidebarRef.value.fetchTrendingPosts(); fetchPosts() }
const getSourceLabel = (s) => (s === 'PRIVATE' || s === 'private' || s === '민간') ? '민간' : s
const formatDateDot = (s) => s ? s.split('T')[0].replace(/-/g, '.') : null
const calculateDDay = (deadline) => {
  const target = new Date(deadline); const today = new Date(); target.setHours(0,0,0,0); today.setHours(0,0,0,0);
  const diff = Math.ceil((target - today) / 86400000); return diff < 0 ? '마감' : (diff === 0 ? 'D-Day' : `D-${diff}`);
}

const handleResize = () => { isMobile.value = window.innerWidth <= 992 }
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })
const handleScroll = () => { showTopBtn.value = window.pageYOffset > 400 }

const handleToggleFavorite = async () => {
  if (!noticeInfo.value) return
  try {
    if (!noticeInfo.value.isFavorite) { await axios.post('/api/user-notices', { noticeId: noticeInfo.value.id, memo: '' }); noticeInfo.value.isFavorite = true; noticeInfo.value.interestCount++ } 
    else { await axios.delete(`/api/user-notices/notice/${noticeInfo.value.id}`); noticeInfo.value.isFavorite = false; noticeInfo.value.interestCount-- }
  } catch (e) {}
}

let scrollObserver = null
const setupScrollObserver = () => {
  if (scrollObserver) scrollObserver.disconnect()
  scrollObserver = new IntersectionObserver((entries) => { if (entries[0].isIntersecting && !loading.value && hasMore.value) fetchPosts(true) }, { threshold: 0.1, rootMargin: '200px' })
  if (loadMoreTrigger.value) scrollObserver.observe(loadMoreTrigger.value)
}

onMounted(async () => {
  if (route.query.menu) activeMenu.value = route.query.menu
  if (route.query.sub) activeSubCategory.value = route.query.sub
  if (route.query.hubSearch) hubSearchKeyword.value = route.query.hubSearch
  await fetchNoticeInfo()
  setupScrollObserver()
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll)
  if (activeMenu.value !== 'RANKING' && !noticeInfo.value && activeMenu.value !== 'NOTICE') fetchPosts()
})

watch(() => [route.params.noticeId, route.query.menu, route.query.sub, route.query.hubSearch], async ([newId, newMenu, newSub, newHubSearch]) => {
  if (isRestoring.value) return 
  const resolvedMenu = newMenu || (newId ? 'NOTICE' : (route.path.startsWith('/board') && activeMenu.value === 'NOTICE' ? 'NOTICE' : 'GENERAL'))
  const resolvedSub = newSub || 'ALL'
  activeMenu.value = resolvedMenu; activeSubCategory.value = resolvedSub
  
  if (newHubSearch !== undefined) hubSearchKeyword.value = newHubSearch || ''
  
  posts.value = []
  if (resolvedMenu === 'NOTICE') {
    if (newId) { await fetchNoticeInfo(); await fetchPosts() } 
    else { noticeInfo.value = null; await fetchNoticeHubData() }
  } else if (resolvedMenu === 'RANKING') { activeTagName.value = '' } 
  else { activeTagName.value = ''; await fetchPosts() }
  nextTick(() => { setupScrollObserver() })
}, { deep: true })
</script>

<style scoped>
.board-view-container { max-width: 1200px; margin: 0 auto; padding-bottom: 50px; }
.board-layout { display: flex; gap: 24px; padding: 0 20px; margin-top: 24px; }
.board-main-content { flex: 1; min-width: 0; }
.notice-info-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; }
.notice-info-card .card-title { font-size: 0.95rem; font-weight: 800; margin: 0 0 12px; }
.summary-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.s-item { display: flex; justify-content: space-between; font-size: 0.85rem; }
.s-item .label { color: var(--text-secondary); }
.s-item .value { font-weight: 700; }
.s-item danger .value { color: #ed4956; }
.btn-origin { display: block; width: 100%; text-align: center; background: var(--hover-bg); padding: 10px; border-radius: 8px; text-decoration: none; font-size: 0.85rem; font-weight: 700; color: var(--text-primary); border: none; cursor: pointer; box-sizing: border-box; }
.btn-origin:hover { background: var(--border-color); }
.mobile-notice-title-box { margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid var(--divider-color); }
.m-notice-title { font-size: 1.1rem; font-weight: 800; color: var(--text-primary); line-height: 1.4; margin: 0; word-break: keep-all; text-align: center; }

/* [시니어 추가] 소통방 허브 검색창 스타일 */
.hub-search-area { margin-bottom: 20px; }
.hub-search-wrapper {
  display: flex; align-items: center; background-color: var(--card-bg); 
  border: 1px solid var(--border-color); border-radius: 14px; padding: 0 16px; 
  transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}
.hub-search-wrapper:focus-within { border-color: var(--link-color); box-shadow: 0 0 0 3px rgba(0, 149, 246, 0.1); }
.hub-search-input { width: 100%; height: 48px; background: none; border: none; color: var(--text-primary); font-size: 0.95rem; font-weight: 600; outline: none; }
.h-search-icon { font-size: 1rem; color: var(--text-secondary); margin-right: 12px; opacity: 0.7; }
.h-clear-btn { background: var(--hover-bg); border: none; color: var(--text-secondary); font-size: 1.2rem; cursor: pointer; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; padding: 0; transition: all 0.2s; }
.h-clear-btn:hover { background: var(--border-color); color: var(--text-primary); }
.btn-reset-search { margin-top: 15px; background: var(--hover-bg); border: 1px solid var(--border-color); padding: 8px 20px; border-radius: 10px; font-size: 0.85rem; font-weight: 700; color: var(--text-primary); cursor: pointer; }

.notice-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.notice-board-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; cursor: pointer; transition: all 0.2s; display: flex; flex-direction: column; gap: 12px; }
.notice-board-card:hover { transform: translateY(-4px); border-color: var(--link-color); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.badge-group { display: flex; gap: 6px; align-items: center; }
.source-tag { font-size: 0.65rem; font-weight: 900; color: white; padding: 4px 8px; border-radius: 6px; }
.source-tag.LH { background: #38a169; } .source-tag.SH { background: #3182ce; } .source-tag.PRIVATE { background: #c08457; }
.status-badge-modern { display: flex; align-items: center; }
.s-badge { font-size: 0.68rem; font-weight: 800; padding: 4px 10px; border-radius: 8px; background: var(--hover-bg); border: 1px solid var(--border-color); color: var(--text-secondary); }
.s-badge.active { color: #ed4956; border-color: rgba(237, 73, 86, 0.3); background: rgba(237, 73, 86, 0.05); }
.s-badge.archived { color: #3182ce; border-color: rgba(49, 130, 206, 0.3); background: rgba(49, 130, 206, 0.05); }
.notice-board-card .card-title { font-size: 1rem; font-weight: 700; margin: 0; line-height: 1.4; height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.card-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: auto; padding-top: 10px; border-top: 1px solid var(--divider-color); font-size: 0.85rem; }
.interest-count { font-weight: 700; color: var(--text-secondary); }
.d-day { color: var(--link-color); font-weight: 800; }
.general-board-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid var(--divider-color); flex-shrink: 0; }
.general-board-header .post-count { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; }
.general-board-header .btn-write { background: none; border: 1px solid var(--link-color); color: var(--link-color); padding: 6px 12px; border-radius: 6px; font-size: 0.85rem; font-weight: 700; cursor: pointer; }
.filter-indicator { display: flex; justify-content: space-between; align-items: center; background: rgba(0, 149, 246, 0.05); padding: 12px 20px; border-radius: 12px; margin-bottom: 16px; border: 1px solid rgba(0, 149, 246, 0.1); }
.btn-clear-filter { background: none; border: none; color: var(--text-secondary); font-size: 0.8rem; cursor: pointer; font-weight: 700; }
.up-arrow { font-size: 1.2rem; color: var(--link-color); font-weight: bold; }
.notice-hub-empty { width: 100%; margin-top: 12px; }
.empty-state.type-hub { padding: 80px 20px; text-align: center; background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 24px; }
.empty-state.type-hub .empty-icon { font-size: 3.5rem; display: block; margin-bottom: 20px; opacity: 0.8; }
.empty-state.type-hub .empty-title { font-size: 1.3rem; font-weight: 800; margin-bottom: 12px; color: var(--text-primary); }
.empty-state.type-hub .empty-desc { font-size: 0.95rem; color: var(--text-secondary); line-height: 1.6; margin: 0; word-break: keep-all; }
.wakeup-notice-banner-hub { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px 25px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; gap: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
.w-content { display: flex; gap: 15px; align-items: center; }
.w-icon { font-size: 1.8rem; }
.w-text strong { display: block; font-size: 1rem; color: var(--text-primary); margin-bottom: 4px; }
.w-text p { font-size: 0.85rem; color: var(--text-secondary); margin: 0; }
.w-stats-detail { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 4px; }
.w-stats-detail b { color: var(--link-color); font-weight: 800; }
.w-guide-text { font-size: 0.75rem; color: var(--text-muted); margin: 0; font-weight: 600; }
.w-action { display: flex; flex-direction: column; gap: 10px; min-width: 180px; }
.w-gauge-container { height: 8px; background: var(--border-color); border-radius: 4px; position: relative; overflow: hidden; }
.w-gauge-fill { height: 100%; background: var(--link-color); border-radius: 4px; transition: width 0.5s ease; }
.w-gauge-text { position: absolute; right: 0; top: -18px; font-size: 0.7rem; font-weight: 800; color: var(--text-muted); }
.btn-wakeup-action { background: var(--link-color); color: white; border: none; padding: 10px 15px; border-radius: 8px; font-size: 0.85rem; font-weight: 800; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 10px rgba(0,149,246,0.2); }
.btn-wakeup-action.is-clicked { background: #38a169; box-shadow: 0 4px 10px rgba(56,161,105,0.2); }
.wakeup-notice-banner-hub.is-active-banner { border-color: var(--link-color); background: linear-gradient(to right, var(--hover-bg), var(--card-bg)); }
.wakeup-notice-banner-hub.is-active-banner strong { color: var(--link-color); }
@media (max-width: 768px) {
  .wakeup-notice-banner-hub { flex-direction: column; padding: 15px; text-align: center; gap: 15px; }
  .w-content { flex-direction: column; gap: 8px; }
  .w-action { width: 100%; min-width: 0; }
  .w-gauge-text { position: static; display: block; margin-bottom: 5px; text-align: right; }
}
.load-more-trigger { height: 20px; margin-top: 10px; }
@media (max-width: 992px) { .board-layout { flex-direction: column; gap: 8px; } }

/* [시니어 추가] 소통방 허브 기간 및 라이프사이클 안내 통합형 */
.hub-info-banner {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
  padding: 0 10px; /* 모바일 양옆 여백 */
}
.hub-lifecycle-info {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(var(--secondary-rgb), 0.05);
  padding: 8px 18px;
  border-radius: 20px;
  border: 1px dashed var(--border-color);
  max-width: 100%;
}
.lifecycle-tip, .info-text {
  font-size: 12px;
  font-weight: 700;
  color: var(--primary-color);
  white-space: nowrap;
}
.info-text {
  color: var(--secondary-text);
  font-weight: 500;
}
.info-divider {
  color: var(--border-color);
  font-size: 10px;
  margin: 0 2px;
}

/* 모바일 대응 (반응형) */
@media (max-width: 768px) {
  .hub-lifecycle-info {
    flex-direction: column; /* 세로로 쌓기 */
    gap: 6px;
    padding: 12px 16px;
    border-radius: 14px;
    align-items: center;
    text-align: center;
  }
  .info-divider {
    display: none; /* 세로 배치 시 구분선 숨김 */
  }
  .lifecycle-tip, .info-text {
    white-space: normal; /* 자동 줄바꿈 허용 */
    line-height: 1.4;
  }
}
</style>
