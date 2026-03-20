<template>
  <div class="board-view-container">
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
            <a :href="noticeInfo.link" target="_blank" class="btn-origin">🔗 원문 공고보기</a>
          </div>
        </template>
      </CommunitySidebar>

      <main class="board-main-content">
        <div v-if="activeMenu === 'NOTICE' && !noticeId" class="notice-hub-content">
          <div v-if="filteredNoticeBoards.length > 0" class="notice-grid">
            <div v-for="board in filteredNoticeBoards" :key="board.id" 
                 class="notice-board-card" @click="enterNoticeBoard(board.id)">
              <div class="card-top">
                <div class="badge-group">
                  <span class="source-tag" :class="board.source">{{ getSourceLabel(board.source) }}</span>
                </div>
                <!-- [시니어 조치] 상태별 우측 상단 배지 추가 -->
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

          <!-- [시니어 조치] 소통방이 하나도 없는 경우의 안내 화면 -->
          <div v-else class="notice-hub-empty">
            <div class="empty-state type-hub">
              <span class="empty-icon">📢</span>
              <h3 class="empty-title">
                {{ 
                  activeSubCategory === 'MY' ? '관심 있는 소통방이 없습니다' :
                  activeSubCategory === 'CLOSED' ? '마감된 소통방 기록이 없습니다' :
                  '현재 활성화된 소통방이 없습니다'
                }}
              </h3>
              <p class="empty-desc">
                {{
                  activeSubCategory === 'MY' ? '공고 리스트에서 관심 있는 공고에 하트를 눌러보세요!' :
                  '이웃들과의 소통을 위해 새로운 소통방이 열리기를 기다려주세요.'
                }}
              </p>
            </div>
          </div>
        </div>

        <div v-else class="post-list-section">
          <RankingBoard v-if="activeMenu === 'RANKING'" />

          <template v-else-if="activeMenu === 'NOTICE' && noticeId && noticeInfo">
            <!-- [시니어 조치] 소통방 상태 안내 배너 (잠김/휴면/활성 통합 v3) -->
            <div v-if="noticeInfo && wakeupStatus" class="wakeup-notice-banner-hub" :class="{ 'is-active-banner': noticeInfo.isBoardActive && !noticeInfo.isDormant }">
              <div class="w-content">
                <span class="w-icon">
                  {{ !noticeInfo.isBoardActive ? '🔒' : (noticeInfo.isDormant ? '😴' : '✨') }}
                </span>
                <div class="w-text">
                  <!-- 1. 활성화 상태 -->
                  <template v-if="noticeInfo.isBoardActive && !noticeInfo.isDormant">
                    <strong>소통방이 활발하게 운영 중입니다 ✨</strong>
                    <p>이웃들과 함께 해당 공고의 실시간 정보를 자유롭게 공유해 주세요!</p>
                  </template>
                  <!-- 2. 휴면 상태 (보관중) -->
                  <template v-else-if="noticeInfo.isBoardActive && noticeInfo.isDormant">
                    <strong>이 소통방은 보관중입니다 😴</strong>
                    <div class="w-stats-detail">
                      현재 <b>관심 {{ noticeInfo.interestCount }}명</b> · <b>깨우기 {{ wakeupStatus.currentCount }}명</b> 참여 중
                    </div>
                    <p class="w-guide-text">목표 인원({{ wakeupStatus.targetCount }}명) 달성 시 다시 활성화됩니다!</p>
                  </template>
                  <!-- 3. 잠김 상태 -->
                  <template v-else>
                    <strong>이 소통방은 아직 잠겨있어요</strong>
                    <p>이웃들의 관심을 기다리고 있습니다. (관심 유저 10명이 되면 소통방이 열립니다)</p>
                  </template>
                </div>
              </div>
              <!-- 깨우기 액션: 휴면 상태일 때만 노출 -->
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

    <Transition name="fade">
      <button v-if="showTopBtn" class="btn-scroll-top" @click="scrollToTop">
        <span class="up-arrow">↑</span>
      </button>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/plugins/axios'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'
import NoticeBoard from '@/components/NoticeBoard.vue'
import CommunitySidebar from '@/components/CommunitySidebar.vue'
import PostList from '@/components/PostList.vue'
import RankingBoard from '@/components/RankingBoard.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const sidebarRef = ref(null)
const loadMoreTrigger = ref(null)

// --- 상태 초기화 (URL 쿼리 우선 로직) ---
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

// [시니어 조치] 소통방 깨우기 상태 관리
const wakeupStatus = ref(null)
const fetchWakeupStatus = async () => {
  if (!noticeId.value) return
  try {
    const res = await axios.get(`/api/notices/${noticeId.value}/wakeup-status`)
    wakeupStatus.value = res.data.data
  } catch (e) {}
}

const handleToggleWakeUp = async () => {
  if (!authStore.isAuthenticated) { alert('로그인이 필요한 서비스입니다.'); return }
  console.log("[DEBUG] 깨우기 버튼 클릭됨 - 공고 ID:", noticeId.value);
  try {
    await axios.post(`/api/notices/${noticeId.value}/wakeup`)
    const oldStatus = { ...wakeupStatus.value };
    await fetchWakeupStatus()
    console.log("[DEBUG] 서버 응답 wakeupStatus:", wakeupStatus.value);

    // [시니어 조치] 필드명 유연성 확보 (isRevived 또는 revived)
    const isNowRevived = wakeupStatus.value?.isRevived || wakeupStatus.value?.revived;

    if (isNowRevived) {
      console.log("[DEBUG] 깨우기 성공 조건 충족!");
      alert('소통방이 깨어났습니다! 이제 자유롭게 대화하실 수 있습니다.');
      if (noticeInfo.value) {
        // [시니어 조치] isDormant를 false로 명시적 교체하여 배너 즉시 전환 유도
        noticeInfo.value = { 
          ...noticeInfo.value, 
          isRevived: true, 
          isBoardActive: true,
          isDormant: false 
        };
        console.log("[DEBUG] noticeInfo 최종 업데이트 (Dormant 해제):", noticeInfo.value);
      }
      if (wakeupStatus.value) {
        wakeupStatus.value = { ...wakeupStatus.value, isRevived: true, revived: true };
      }
    } else {
      console.log("[DEBUG] 아직 인원이 부족합니다. 현재:", wakeupStatus.value?.currentCount);
    }
  } catch (e) {
    console.error("[DEBUG] 깨우기 통신 에러:", e);
    if (e.response?.data?.message) alert(e.response.data.message)
  }
}

// [시니어 조치] 마감 판정 로직 (isDormant 필드 직접 활용)
const isBoardClosed = computed(() => {
  return noticeInfo.value?.isDormant === true
})

const mainMenus = [
  { id: 'GENERAL', name: '통합광장', icon: '🏛️' },
  { id: 'NOTICE', name: '공고소통방', icon: '📢' },
  { id: 'REVIEW', name: '당첨후기', icon: '✨' },
  { id: 'PARTNER', name: '파트너스', icon: '🤝' },
  { id: 'RANKING', name: '활동랭킹', icon: '🎖️' }
]

const currentMenu = computed(() => mainMenus.find(m => m.id === activeMenu.value))
const currentMenuTitle = computed(() => noticeId.value ? noticeInfo.value?.title : currentMenu.value?.name)
const currentMenuIcon = computed(() => noticeId.value ? '📢' : currentMenu.value?.icon)
const currentMenuBio = computed(() => noticeId.value ? '이 공고에 관심 있는 분들과 대화해보세요.' : 'habiDue 커뮤니티 공간입니다.')

const currentStatsValue = computed(() => {
  if (noticeId.value) return `참여유저 ${noticeInfo.value?.interestCount || 0}`
  if (activeMenu.value === 'NOTICE') {
    return `소통방 ${filteredNoticeBoards.value.length}`
  }
  return `게시글 ${totalElements.value}`
})

const filteredNoticeBoards = computed(() => {
  if (activeSubCategory.value === 'MY') return myNoticeBoards.value
  if (activeSubCategory.value === 'PREPARING') return preparingNoticeBoards.value
  if (activeSubCategory.value === 'ACTIVE') return activeNoticeBoards.value
  if (activeSubCategory.value === 'ARCHIVED') return archivedNoticeBoards.value
  return activeNoticeBoards.value // 기본값
})

const isRestoring = ref(false)

// [전문가 조치] 정밀 스크롤 복구 (찾을 때까지 자동 로딩 포함)
const scrollToPost = (postId) => {
  if (!postId) return
  isRestoring.value = true
  
  const attemptScroll = () => {
    const element = document.getElementById(`post-${postId}`)
    
    if (element) {
      // 1. 게시글을 찾은 경우: 즉시 위치 계산 후 이동
      const headerOffset = 145 
      const elementPosition = element.getBoundingClientRect().top + window.pageYOffset
      const offsetPosition = elementPosition - headerOffset

      window.scrollTo({ top: offsetPosition, behavior: 'instant' })
      
      // 파라미터 삭제 및 플래그 해제
      const query = { ...route.query }; delete query.lastPostId
      router.replace({ query }).finally(() => {
        setTimeout(() => { isRestoring.value = false }, 500)
      })
    } else if (hasMore.value && !loading.value) {
      // 2. 아직 리스트에 없는 경우: 다음 페이지 로드 후 다시 시도
      fetchPosts(true).then(() => {
        // 데이터가 로드되고 DOM이 갱신될 시간을 충분히 줌
        setTimeout(attemptScroll, 100)
      })
    } else {
      isRestoring.value = false
    }
  }

  // 첫 시도
  requestAnimationFrame(() => requestAnimationFrame(attemptScroll))
}

const fetchPosts = async (isMore = false) => {
  if (activeMenu.value === 'NOTICE' && !noticeId.value) return
  if (activeMenu.value === 'RANKING') return // 랭킹 메뉴는 게시글 조회를 하지 않음
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
      // 데이터 로드 완료 후 스크롤 복구 시도
      if (route.query.lastPostId) scrollToPost(route.query.lastPostId)
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
    fetchWakeupStatus() // [시니어 조치] 깨우기 상태 조회 추가
    
    // [시니어 조치] 모바일에서 공고 상세 진입 시 요약 정보로 자동 스크롤
    if (isMobile.value) {
      nextTick(() => {
        const infoCard = document.querySelector('.notice-info-card')
        if (infoCard) {
          infoCard.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      })
    }
  } catch (e) { router.push('/board') }
}

const fetchNoticeHubData = async () => {
  if (loading.value) return
  loading.value = true
  try {
    // 1. 내 관심 공고 가져오기
    const myRes = await axios.get('/api/user-notices', { params: { size: 100 } })
    myNoticeBoards.value = myRes.data.data.content.map(un => ({ 
      id: un.noticeId, title: un.noticeTitle, source: un.noticeSource, 
      deadline: un.noticeDeadline, interestCount: un.interestCount || 0, 
      isBoardActive: !!un.isBoardActive,
      isDormant: !!un.isDormant,
      status: un.noticeStatus 
    }))

    // 2. 전체 공고 가져와서 상태별로 분류
    const allRes = await axios.get('/api/notices', { params: { size: 200, sortOrder: 'interest' } })
    const allData = allRes.data.data.content

    preparingNoticeBoards.value = allData.filter(b => !b.isBoardActive)
    activeNoticeBoards.value = allData.filter(b => b.isBoardActive && !b.isDormant)
    archivedNoticeBoards.value = allData.filter(b => b.isBoardActive && b.isDormant)

  } catch (e) {
    console.error('소통방 데이터 로드 실패:', e)
  } finally {
    loading.value = false
  }
}

const enterNoticeBoard = (id) => {
  // [시니어 조치] 상세 진입 시 현재 쿼리(menu, sub 등)를 유지하여 사이드바 상태 보존
  router.push({ 
    path: `/board/${id}`,
    query: { ...route.query }
  })
}
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
  // [시니어 조치] 바닥에 닿기 200px 전에 미리 다음 데이터 호출 (부드러운 경험)
  scrollObserver = new IntersectionObserver((entries) => { 
    if (entries[0].isIntersecting && !loading.value && hasMore.value) fetchPosts(true) 
  }, { threshold: 0.1, rootMargin: '200px' })
  if (loadMoreTrigger.value) scrollObserver.observe(loadMoreTrigger.value)
}

// [시니어 조치] URL 파라미터 강제 동기화 (최초 로드 시점)
onMounted(async () => {
  if (route.query.menu) activeMenu.value = route.query.menu
  if (route.query.sub) activeSubCategory.value = route.query.sub

  await fetchNoticeInfo()
  setupScrollObserver()
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll)
  
  // [시니어 조치] 초기 로딩 시 랭킹 메뉴가 아니면 포스트 조회
  if (activeMenu.value !== 'RANKING' && !noticeInfo.value && activeMenu.value !== 'NOTICE') {
    fetchPosts()
  }
})

// [시니어 조치] 통합 감시자 - 라우트 변경 시 데이터 로드 및 옵저버 재연결
watch(() => [route.params.noticeId, route.query.menu, route.query.sub], async ([newId, newMenu, newSub]) => {
  if (isRestoring.value) return 

  const resolvedMenu = newMenu || (newId ? 'NOTICE' : (route.path.startsWith('/board') && activeMenu.value === 'NOTICE' ? 'NOTICE' : 'GENERAL'))
  const resolvedSub = newSub || 'ALL'
  
  activeMenu.value = resolvedMenu
  activeSubCategory.value = resolvedSub
  
  posts.value = []
  if (resolvedMenu === 'NOTICE') {
    if (newId) {
      await fetchNoticeInfo()
      await fetchPosts()
    } else {
      noticeInfo.value = null
      await fetchNoticeHubData()
    }
  } else if (resolvedMenu === 'RANKING') {
    // 랭킹 메뉴는 RankingBoard 컴포넌트가 자체적으로 데이터를 로드하므로 별도 게시글 호출 안함
    activeTagName.value = ''
  } else {
    activeTagName.value = ''
    await fetchPosts()
  }

  // [핵심] 메뉴나 게시판 전환으로 DOM이 변경되었으므로, 옵저버를 다시 연결하여 무한 스크롤을 살림
  nextTick(() => {
    setupScrollObserver()
  })
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
.s-item.danger .value { color: #ed4956; }
.btn-origin { display: block; text-align: center; background: var(--hover-bg); padding: 10px; border-radius: 8px; text-decoration: none; font-size: 0.85rem; font-weight: 700; color: var(--text-primary); }
.mobile-notice-title-box { margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid var(--divider-color); }
.m-notice-title { font-size: 1.1rem; font-weight: 800; color: var(--text-primary); line-height: 1.4; margin: 0; word-break: keep-all; text-align: center; }
.notice-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.notice-board-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; cursor: pointer; transition: all 0.2s; display: flex; flex-direction: column; gap: 12px; }
.notice-board-card:hover { transform: translateY(-4px); border-color: var(--link-color); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.badge-group { display: flex; gap: 6px; align-items: center; }
.source-tag { font-size: 0.65rem; font-weight: 900; color: white; padding: 4px 8px; border-radius: 6px; }
.source-tag.LH { background: #38a169; }
.source-tag.SH { background: #3182ce; }
.source-tag.PRIVATE, .source-tag.private, .source-tag.민간 { background: #c08457; }

/* [시니어 조치] 소통방 카드 상태 배지 스타일 */
.status-badge-modern { display: flex; align-items: center; }
.s-badge { 
  font-size: 0.68rem; font-weight: 800; padding: 4px 10px; border-radius: 8px;
  background: var(--hover-bg); border: 1px solid var(--border-color); color: var(--text-secondary);
}
.s-badge.active { color: #ed4956; border-color: rgba(237, 73, 86, 0.3); background: rgba(237, 73, 86, 0.05); }
.s-badge.archived { color: #3182ce; border-color: rgba(49, 130, 206, 0.3); background: rgba(49, 130, 206, 0.05); }
.s-badge.preparing { color: var(--text-muted); border-color: var(--border-color); }

.notice-board-card .card-title { font-size: 1rem; font-weight: 700; margin: 0; line-height: 1.4; height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.card-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: auto; padding-top: 10px; border-top: 1px solid var(--divider-color); font-size: 0.85rem; }
.interest-count { font-weight: 700; color: var(--text-secondary); }
.d-day { color: var(--link-color); font-weight: 800; }
.general-board-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid var(--divider-color); flex-shrink: 0; }
.general-board-header .post-count { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; }
.general-board-header .header-action-group { display: flex; gap: 8px; align-items: center; }
.general-board-header .btn-write { background: none; border: 1px solid var(--link-color); color: var(--link-color); padding: 6px 12px; border-radius: 6px; font-size: 0.85rem; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.filter-indicator { display: flex; justify-content: space-between; align-items: center; background: rgba(0, 149, 246, 0.05); padding: 12px 20px; border-radius: 12px; margin-bottom: 16px; border: 1px solid rgba(0, 149, 246, 0.1); }
.btn-clear-filter { background: none; border: none; color: var(--text-secondary); font-size: 0.8rem; cursor: pointer; font-weight: 700; }
.btn-scroll-top { display: flex; position: fixed; bottom: 50px; left: calc(50% + 480px); width: 45px; height: 45px; border-radius: 50%; background: var(--card-bg); border: 1px solid var(--border-color); box-shadow: 0 4px 12px rgba(0,0,0,0.1); cursor: pointer; z-index: 100; align-items: center; justify-content: center; transition: all 0.2s; }
.btn-scroll-top:hover { transform: translateY(-5px); border-color: var(--link-color); }
.up-arrow { font-size: 1.2rem; color: var(--link-color); font-weight: bold; }

/* [시니어 조치] 소통방 허브 안내 화면 스타일 */
.notice-hub-empty { width: 100%; margin-top: 12px; }
.empty-state.type-hub { 
  padding: 80px 20px; text-align: center; 
  background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 24px;
}
.empty-state.type-hub .empty-icon { font-size: 3.5rem; display: block; margin-bottom: 20px; opacity: 0.8; }
.empty-state.type-hub .empty-title { font-size: 1.3rem; font-weight: 800; margin-bottom: 12px; color: var(--text-primary); }
.empty-state.type-hub .empty-desc { font-size: 0.95rem; color: var(--text-secondary); line-height: 1.6; margin: 0; word-break: keep-all; }

/* [시니어 조치] 소통방 깨우기 배너 스타일 (BoardView 통합) */
.wakeup-notice-banner-hub {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px 25px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.02);
}

.w-content { display: flex; gap: 15px; align-items: center; }
.w-icon { font-size: 1.8rem; }
.w-text strong { display: block; font-size: 1rem; color: var(--text-primary); margin-bottom: 4px; }
.w-text p { font-size: 0.85rem; color: var(--text-secondary); margin: 0; }
.w-stats-detail { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 4px; }
.w-stats-detail b { color: var(--link-color); font-weight: 800; }
.w-guide-text { font-size: 0.75rem; color: var(--text-muted); margin: 0; font-weight: 600; }

.w-action { display: flex; flex-direction: column; gap: 10px; min-width: 180px; }
.w-gauge-container { 
  height: 8px; background: var(--border-color); border-radius: 4px; 
  position: relative; overflow: hidden; 
}
.w-gauge-fill { height: 100%; background: var(--link-color); border-radius: 4px; transition: width 0.5s ease; }
.w-gauge-text { position: absolute; right: 0; top: -18px; font-size: 0.7rem; font-weight: 800; color: var(--text-muted); }

.btn-wakeup-action {
  background: var(--link-color); color: white; border: none; padding: 10px 15px; 
  border-radius: 8px; font-size: 0.85rem; font-weight: 800; cursor: pointer;
  transition: all 0.2s; box-shadow: 0 4px 10px rgba(0,149,246,0.2);
}
.btn-wakeup-action:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(0,149,246,0.3); }
.btn-wakeup-action.is-clicked { background: #38a169; box-shadow: 0 4px 10px rgba(56,161,105,0.2); }

/* 활성화 상태 배너 강조 스타일 */
.wakeup-notice-banner-hub.is-active-banner {
  border-color: var(--link-color);
  background: linear-gradient(to right, var(--hover-bg), var(--card-bg));
}
.wakeup-notice-banner-hub.is-active-banner strong { color: var(--link-color); }

@media (max-width: 768px) {
  .wakeup-notice-banner-hub { flex-direction: column; padding: 15px; text-align: center; gap: 15px; }
  .w-content { flex-direction: column; gap: 8px; }
  .w-action { width: 100%; min-width: 0; }
  .w-gauge-text { position: static; display: block; margin-bottom: 5px; text-align: right; }
}

.load-more-trigger { height: 20px; margin-top: 10px; }
@media (max-width: 992px) { .board-layout { flex-direction: column; gap: 8px; } .btn-scroll-top { display: none; } }
</style>
