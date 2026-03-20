<template>
  <div class="notice-board-container" :class="isPreview ? 'preview-mode' : 'full-page-mode'">
    <!-- 1. 초기 로딩 영역 (중앙 정렬 보장) -->
    <div v-if="isInitialLoading" class="initial-loading-area">
      <div class="spinner-box">
        <div class="spinner"></div>
        <p class="loading-text">소통방을 불러오고 있습니다...</p>
      </div>
    </div>

    <template v-else>
      <!-- 2. 잠금 상태 UI (Lock) -->
      <div v-if="!isBoardActive" class="locked-board-content">
        <div class="lock-visual">
          <span class="lock-icon">🔒</span>
          <h3 class="lock-title">이 소통방은 아직 잠겨있어요</h3>
          
          <p class="lock-desc">
            관심을 표현한 이웃이 10명이 되면 소통방이 열립니다.<br>
            현재 <b>{{ currentInterestCount }}명</b> 참여 중
          </p>

          <button v-if="!isFavorite" class="btn-join-interest" @click="emit('toggleFavorite')">
            🙋 나도 관심있어요!
          </button>
          <p v-else class="already-joined-msg">✅ 관심을 표현하셨습니다!</p>
        </div>
      </div>

      <!-- 3. 해금 상태 UI (Unlock) -->
      <div v-else class="unlocked-board-content" :class="sourceThemeClass">
        <div class="board-content-wrapper">
          
          <div class="board-main-area">
            <!-- 헤더 스타일 통합광장과 일치 (전체페이지에서는 항상 노출, 미리보기에서는 글이 있을 때만 노출) -->
            <div v-if="!isPreview || posts.length > 0" class="general-board-header notice-style">
              <div class="board-info">
                <span class="post-count">{{ isPreview ? '🔥 실시간 인기 게시글' : `게시글 ${totalPosts}개` }}</span>
              </div>
              <div v-if="!isPreview && !isBoardClosed" class="header-action-group">
                <button class="btn-write outlined" :class="sourceThemeClass" @click="goToWrite">📝 글쓰기</button>
              </div>
            </div>

            <!-- 데이터 노출 영역 -->
            <div class="scroll-content" :class="{ 'scrollable': isPreview }">
              <!-- 게시글이 하나도 없는 경우 -->
              <div v-if="posts.length === 0" class="empty-only-container">
                <div class="empty-state type-general">
                  <div class="empty-box">
                    <span class="empty-icon">📢</span>
                    <h3 class="empty-title">아직 등록된 소통글이 없습니다</h3>
                    <p class="empty-desc">해당 공고에 대해 궁금한 점이나 정보를<br>이웃과 나누어 보세요!</p>
                  </div>
                </div>
              </div>

              <!-- 게시글이 있는 경우 -->
              <div v-else class="post-list-area">
                <PostList 
                  :posts="posts" 
                  :loading="loading" 
                  :has-more="false" 
                  type="NOTICE"
                  :notice-id="noticeId"
                />
                <!-- 무한 스크롤 트리거 (전체 페이지용) -->
                <div v-if="!isPreview && hasMore" ref="loadMoreTrigger" class="load-more-trigger">
                  <div v-if="loading" class="mini-spinner"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 하단 푸터 (모달에서 무조건 바닥에 박제됨) -->
          <footer v-if="isPreview && !isBoardClosed" class="preview-footer">
            <button class="btn-primary-action theme-bg" @click="goToWrite">📝 소통글 작성하기</button>
          </footer>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from '@/plugins/axios'
import PostList from '@/components/PostList.vue'

const props = defineProps({
  noticeId: { type: Number, required: true },
  isBoardActive: { type: Boolean, default: false },
  isRevived: { type: Boolean, default: false }, // [시니어 추가] 깨우기 성공 여부
  isDormant: { type: Boolean, default: false }, // [시니어 추가] 휴면 여부
  currentInterestCount: { type: Number, default: 0 },
  isFavorite: { type: Boolean, default: false },
  isPreview: { type: Boolean, default: false },
  source: { type: String, default: 'LH' },
  noticeStatus: { type: String, default: 'RECRUITING' }, // [시니어 조치] 공고 상태 수신
  activeSubCategory: { type: String, default: 'ALL' },
  wakeupStatus: { type: Object, default: null } // [시니어 추가] 깨우기 현황 정보 수신
})

const emit = defineEmits(['toggleFavorite', 'postCreated'])
const router = useRouter()
const route = useRoute()

const posts = ref([])
const totalPosts = ref(0)
const loading = ref(false)
const isInitialLoading = ref(true)
const currentPage = ref(0)
const hasMore = ref(false)
const loadMoreTrigger = ref(null)

const sourceThemeClass = computed(() => {
  const s = props.source?.toUpperCase()
  if (s === 'LH') return 'theme-lh'
  if (s === 'SH') return 'theme-sh'
  return 'theme-private'
})

// [시니어 조치] 공고 마감 상태 정밀 판별 (사용자 요청 정책 반영)
const isBoardClosed = computed(() => {
  // [시니어 조치] 깨우기에 성공한 소통방은 마감 상태를 해제함
  if (props.isRevived) return false

  const closedStatuses = ['CLOSED', 'EXPIRED_INFO', 'RESULT_COMPLETED']
  return closedStatuses.includes(props.noticeStatus?.toUpperCase())
})

const goToReviewBoard = () => {
  router.push({ path: '/board', query: { menu: 'REVIEW', sub: 'ALL' } })
}

const scrollToPost = (postId) => {
  if (!postId || props.isPreview) return
  const attemptScroll = () => {
    const element = document.getElementById(`post-${postId}`)
    if (element) {
      const headerOffset = 145 
      const elementPosition = element.getBoundingClientRect().top + window.pageYOffset
      window.scrollTo({ top: elementPosition - headerOffset, behavior: 'instant' })
      const query = { ...route.query }; delete query.lastPostId
      router.replace({ query })
    } else if (hasMore.value && !loading.value) {
      fetchPosts(true).then(() => { setTimeout(attemptScroll, 100) })
    }
  }
  setTimeout(attemptScroll, 150)
}

const fetchPosts = async (isMore = false) => {
  if (loading.value) return
  loading.value = true
  try {
    if (props.isPreview) {
      const res = await axios.get(`/api/posts/notice/${props.noticeId}/top`, { params: { limit: 5 } })
      posts.value = res.data.data
      totalPosts.value = posts.value.length
      hasMore.value = false
    } else {
      if (!isMore) { posts.value = []; currentPage.value = 0 }
      const params = { type: 'NOTICE', noticeId: props.noticeId, page: currentPage.value, size: 10, sort: 'createdAt,desc' }
      const res = await axios.get('/api/posts', { params })
      const data = res.data.data
      totalPosts.value = data.totalElements
      if (isMore) posts.value = [...posts.value, ...data.content]; else posts.value = data.content
      hasMore.value = !data.last; currentPage.value++
    }
  } catch (e) { console.error(e) } finally {
    loading.value = false
    isInitialLoading.value = false
  }
}

const goToWrite = () => {
  router.push({ path: '/board/write', query: { ...route.query, type: 'NOTICE', noticeId: props.noticeId, sub: 'ALL' } })
}

let observer = null
const setupObserver = () => {
  if (observer) observer.disconnect()
  if (props.isPreview) return
  nextTick(() => {
    if (!loadMoreTrigger.value) return
    observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !loading.value && hasMore.value) fetchPosts(true)
    }, { threshold: 0.1, rootMargin: '400px' })
    observer.observe(loadMoreTrigger.value)
  })
}

watch(isInitialLoading, (isDone) => {
  if (!isDone) {
    nextTick(() => {
      if (route.query.lastPostId) scrollToPost(route.query.lastPostId)
      if (!props.isPreview) setupObserver()
    })
  }
})

onMounted(async () => {
  if (props.isBoardActive) await fetchPosts()
  else setTimeout(() => { isInitialLoading.value = false }, 400)
})

onUnmounted(() => { if (observer) observer.disconnect() })

watch(() => props.noticeId, async (newId) => {
  if (!newId) return
  isInitialLoading.value = true
  posts.value = []
  if (props.isBoardActive) await fetchPosts()
  else setTimeout(() => { isInitialLoading.value = false }, 400)
})

// [시니어 조치] 게시판 활성화 또는 깨우기 성공(isRevived) 시 즉시 데이터 로드
watch(() => [props.isBoardActive, props.isRevived], ([active, revived]) => {
  console.log("[DEBUG] NoticeBoard Props 변경 감지 - Active:", active, "Revived:", revived);
  if (active || revived) {
    console.log("[DEBUG] 활성화 조건 충족 - fetchPosts() 실행");
    fetchPosts()
  }
}, { deep: true })
</script>

<style scoped>
/* [핵심] 레이아웃 수직 체인 완성 - 1px의 오차도 없는 Flexbox 구조 */
.notice-board-container { display: flex; flex-direction: column; width: 100%; height: 100%; min-height: 0; }
.notice-board-container.preview-mode { overflow: hidden; } 
.notice-board-container.full-page-mode { min-height: 400px; height: auto; }

.unlocked-board-content { flex: 1; display: flex; flex-direction: column; min-height: 0; width: 100%; }
.board-content-wrapper { flex: 1; display: flex; flex-direction: column; min-height: 0; width: 100%; }
.board-main-area { flex: 1; display: flex; flex-direction: column; min-height: 0; width: 100%; }

/* 데이터 영역 스크롤 제어 */
.scroll-content { flex: 1; display: flex; flex-direction: column; min-height: 0; width: 100%; }
.scroll-content.scrollable { overflow-y: auto; scrollbar-width: thin; padding-right: 4px; }

/* 로딩 UI */
.initial-loading-area { flex: 1; display: flex; align-items: center; justify-content: center; }
.spinner-box { text-align: center; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--hover-bg); border-top: 3px solid var(--link-color); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.loading-text { font-size: 0.85rem; color: var(--text-secondary); font-weight: 700; }

/* 게시판 헤더 (스크롤 밖 고정) */
.general-board-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--divider-color); flex-shrink: 0; }
.general-board-header .post-count { font-size: 0.85rem; color: var(--text-secondary); font-weight: 800; }

.btn-write.outlined {
  background: none; border: 1px solid var(--link-color); color: var(--link-color);
  padding: 6px 14px; border-radius: 6px; font-size: 0.8rem; font-weight: 800; cursor: pointer; transition: all 0.2s;
}
.theme-lh .btn-write.outlined { border-color: #38a169; color: #38a169; }
.theme-sh .btn-write.outlined { border-color: #3182ce; color: #3182ce; }
.theme-private .btn-write.outlined { border-color: #c08457; color: #c08457; }

/* 안내 화면 */
.empty-only-container { flex: 1; display: flex; align-items: flex-start; justify-content: center; padding: 0; min-height: 0; }
.empty-state { 
  width: 100%; padding: 60px 20px 80px; text-align: center; 
  background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 20px;
  margin-top: 12px; /* 아주 살짝 내림 */
}

/* [시니어 조치] 미리보기(모달) 모드일 때 스크롤 방지를 위한 컴팩트 스타일 */
.preview-mode .empty-only-container { align-items: center; padding: 10px 0; }
.preview-mode .empty-state { padding: 40px 20px; margin-top: 0; }
.preview-mode .empty-icon { font-size: 2.5rem; margin-bottom: 12px; }
.preview-mode .empty-title { font-size: 1.1rem; }
.preview-mode .empty-desc { font-size: 0.85rem; }
.empty-box { max-width: 400px; margin: 0 auto; }
.empty-icon { font-size: 3.5rem; display: block; margin-bottom: 20px; }
.empty-title { font-size: 1.25rem; font-weight: 800; margin: 0 0 10px; }
.empty-desc { font-size: 0.95rem; color: var(--text-secondary); line-height: 1.6; margin: 0; word-break: keep-all; }

/* [시니어 조치] 마감 안내 상태 (규격 및 점선 테두리 통일) */
.empty-state.type-archived { border-style: dashed; }
.empty-state.type-archived .empty-desc { margin-bottom: 24px; }
.empty-state.type-archived .btn-primary-action { max-width: 240px; margin: 0 auto; height: 48px; }

.locked-board-content { 
  flex: 1; display: flex; align-items: flex-start; justify-content: center; padding: 0; min-height: 0;
}
/* 일반 모드 잠금 스타일 */
.locked-board-content .lock-visual {
  width: 100%; padding: 60px 20px 80px; text-align: center; 
  background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 20px;
  margin-top: 12px;
}

/* [시니어 조치] 미리보기(모달) 모드일 때 잠금 화면 컴팩트 스타일 */
.preview-mode .locked-board-content { align-items: center; padding: 10px 0; }
.preview-mode .lock-visual { padding: 40px 20px; margin-top: 0; }
.preview-mode .lock-icon { font-size: 2.5rem; margin-bottom: 12px; }
.preview-mode .lock-title { font-size: 1.1rem; }
.preview-mode .lock-desc { font-size: 0.85rem; margin-bottom: 16px; }

.lock-visual { text-align: center; }
.lock-icon { font-size: 3.5rem; display: block; margin-bottom: 20px; }
.lock-title { font-size: 1.25rem; font-weight: 800; margin-bottom: 10px; }
.lock-desc { font-size: 0.95rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 20px; }
.btn-join-interest { 
  display: block; /* 버튼도 block으로 변경 */
  margin: 0 auto; /* 중앙 정렬 */
  background: var(--link-color); 
  color: white; 
  border: none; 
  padding: 12px 28px; 
  border-radius: 10px; 
  font-weight: 800; 
  cursor: pointer; 
  transition: transform 0.2s; 
}
.btn-join-interest:hover { transform: scale(1.05); }
.already-joined-msg { font-size: 0.85rem; font-weight: 700; color: #38a169; }

/* 마감 안내 배너 */
.archived-banner {
  display: flex; align-items: center; justify-content: space-between; gap: 15px;
  background: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 12px;
  padding: 16px 20px; margin-bottom: 20px;
}
.banner-icon { font-size: 2rem; }
.banner-text { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.banner-text strong { font-size: 0.95rem; color: var(--text-primary); font-weight: 800; }
.banner-text p { font-size: 0.8rem; color: var(--text-secondary); margin: 0; line-height: 1.4; }
.btn-go-review { background: var(--card-bg); border: 1px solid var(--border-color); color: var(--text-primary); padding: 8px 16px; border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-go-review:hover { color: var(--link-color); border-color: var(--link-color); }

.empty-state.type-archived .empty-icon { font-size: 3rem; margin-bottom: 15px; }

/* 리스트 및 무한 스크롤 트리거 */
.post-list-area { width: 100%; }
.load-more-trigger { height: 50px; width: 100%; display: flex; align-items: center; justify-content: center; }
.mini-spinner { width: 20px; height: 20px; border: 2px solid var(--hover-bg); border-top: 2px solid var(--link-color); border-radius: 50%; animation: spin 1s linear infinite; }

/* [핵심] 푸터 하단 박제 */
.preview-footer { flex-shrink: 0; padding-top: 20px; }
.btn-primary-action {
  width: 100%; height: 52px; display: flex; align-items: center; justify-content: center;
  color: white; border: none; border-radius: 12px; font-size: 0.95rem; font-weight: 800;
  cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin: 0;
}
.btn-primary-action:hover { transform: translateY(-1px); filter: brightness(1.1); }

.theme-lh .btn-primary-action { background: #38a169; }
.theme-sh .btn-primary-action { background: #3182ce; }
.theme-private .btn-primary-action { background: #c08457; }
</style>
