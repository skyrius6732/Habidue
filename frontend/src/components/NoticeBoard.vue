<template>
  <!-- [시니어 조치] 루트 요소에 테마 클래스 추가하여 하단 버튼까지 색상 제어 -->
  <div class="notice-board-container" :class="[isPreview ? 'preview-mode' : 'full-page-mode', sourceThemeClass]">
    <!-- 1. 초기 로딩 영역 -->
    <div v-if="isInitialLoading" class="initial-loading-area">
      <div class="spinner-box">
        <div class="spinner"></div>
        <p class="loading-text">소통방을 불러오고 있습니다...</p>
      </div>
    </div>

    <template v-else>
      <!-- 메인 콘텐츠 영역 (Scroll Area) -->
      <div class="board-body-area" :class="{ 'scrollable': isPreview }">
        <!-- 2. 잠금 상태 UI (Lock) -->
        <div v-if="!isBoardActive" class="locked-board-content">
          <div class="lock-visual">
            <span class="lock-icon">🔒</span>
            <h3 class="lock-title">이 소통방은 아직 잠겨있어요</h3>
            <p class="lock-desc">
              해당 공고에 관심 있는 이웃이 <b>10명</b> 모이면 소통방이 열립니다.<br>
              현재 <b>{{ currentInterestCount }}명</b> 참여 중
            </p>

            <div class="lock-action-area">
              <button v-if="!isFavorite" class="btn-join-interest" @click="emit('toggleFavorite')">
                ❤️ 나도 관심있어요! (활성화 참여)
              </button>
              <p v-else class="already-joined-msg">✅ 참여 완료! 조금만 더 기다려 주세요.</p>
            </div>
          </div>
        </div>

        <!-- 3. 해금 상태 UI (Unlock) -->
        <div v-else class="unlocked-board-content">
          <!-- 보관된(휴면) 소통방 전용 UI (높이 압축 버전) -->
          <div v-if="isPreview && isDormant" class="dormant-preview-content">
            <div class="dormant-visual">
              <span class="dormant-icon">😴</span>
              <h3 class="dormant-title">이 소통방은 보관중입니다 😴</h3>
              <div class="dormant-stats-box">
                <p class="dormant-stats-detail">
                  현재 <b>관심 {{ currentInterestCount }}명</b> · <b>깨우기 {{ wakeupStatus?.currentCount || 0 }}명</b> 참여 중
                </p>
                <div class="wakeup-gauge-area">
                  <div class="gauge-meta-row">
                    <span class="dormant-guide-text">목표 인원({{ wakeupStatus?.targetCount || 5 }}명) 달성 시 다시 활성화됩니다!</span>
                    <span class="gauge-count-text">{{ wakeupStatus?.currentCount || 0 }} / {{ wakeupStatus?.targetCount || 5 }}</span>
                  </div>
                  <div class="gauge-track-modern">
                    <div class="gauge-fill-modern" :style="{ width: ((wakeupStatus?.currentCount || 0) / (wakeupStatus?.targetCount || 5) * 100) + '%' }"></div>
                  </div>
                </div>
              </div>
              <button 
                class="btn-wakeup-large" 
                :class="{ 'is-clicked': wakeupStatus?.isClicked }"
                @click="emit('toggleWakeUp')"
              >
                {{ wakeupStatus?.isClicked ? '깨우기 완료 ✨' : '소통방 깨우기 ⚡' }}
              </button>
            </div>
          </div>

          <!-- 활성 소통방 게시글 리스트 -->
          <div v-else class="board-main-area">
            <div v-if="!isPreview || posts.length > 0" class="general-board-header notice-style">
              <div class="board-info">
                <span class="post-count">{{ isPreview ? '🔥 실시간 인기 게시글' : `게시글 ${totalPosts}개` }}</span>
              </div>
              <div v-if="!isPreview && !isBoardClosed" class="header-action-group">
                <button class="btn-write outlined" @click="goToWrite">📝 글쓰기</button>
              </div>
            </div>

            <div v-if="posts.length === 0" class="empty-only-container">
              <div class="empty-state type-general">
                <div class="empty-box">
                  <span class="empty-icon">📢</span>
                  <h3 class="empty-title">아직 등록된 소통글이 없습니다</h3>
                  <p class="empty-desc">해당 공고에 대해 궁금한 점이나 정보를<br>이웃과 나누어 보세요!</p>
                </div>
              </div>
            </div>

            <div v-else class="post-list-area">
              <PostList :posts="posts" :loading="loading" :has-more="false" type="NOTICE" :notice-id="noticeId" />
              <div v-if="!isPreview && hasMore" ref="loadMoreTrigger" class="load-more-trigger">
                <div v-if="loading" class="mini-spinner"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- [시니어 조치] 통합 하단 푸터 (테마 클래스에 의한 배경색 자동 적용) -->
      <footer v-if="isPreview" class="preview-footer">
        <button v-if="isBoardActive && !isDormant && !isBoardClosed" class="btn-primary-action theme-bg" @click="goToWrite">
          📝 소통글 작성하기
        </button>
        <button v-else class="btn-primary-action theme-bg" @click="emit('goToHub', noticeTitle, isBoardActive ? 'ARCHIVED' : 'PREPARING')">
          📋 소통방 리스트 보기
        </button>
      </footer>
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
  noticeTitle: { type: String, default: '' },
  isBoardActive: { type: Boolean, default: false },
  isRevived: { type: Boolean, default: false },
  isDormant: { type: Boolean, default: false },
  currentInterestCount: { type: Number, default: 0 },
  isFavorite: { type: Boolean, default: false },
  isPreview: { type: Boolean, default: false },
  source: { type: String, default: 'LH' },
  noticeStatus: { type: String, default: 'RECRUITING' },
  activeSubCategory: { type: String, default: 'ALL' },
  wakeupStatus: { type: Object, default: null }
})

const emit = defineEmits(['toggleFavorite', 'postCreated', 'toggleWakeUp', 'goToHub'])
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

const isBoardClosed = computed(() => {
  if (props.isRevived) return false
  if (props.isDormant) return true // [시니어 조치] 보관된 소통방은 무조건 글쓰기 제한
  const closedStatuses = ['CLOSED', 'EXPIRED_INFO', 'RESULT_COMPLETED', 'RESULT'] // RESULT 상태도 포함
  return closedStatuses.includes(props.noticeStatus?.toUpperCase())
})

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

watch(() => [props.isBoardActive, props.isRevived], ([active, revived]) => {
  if (active || revived) fetchPosts()
}, { deep: true })
</script>

<style scoped>
.notice-board-container { display: flex; flex-direction: column; width: 100%; height: 100%; min-height: 0; }
.notice-board-container.preview-mode { height: 100%; }

.board-body-area { flex: 1; display: flex; flex-direction: column; min-height: 0; width: 100%; }
.board-body-area.scrollable { overflow-y: auto; scrollbar-width: thin; }

.unlocked-board-content { display: flex; flex-direction: column; min-height: 0; flex: 1; }
.board-main-area { display: flex; flex-direction: column; min-height: 0; flex: 1; }

.initial-loading-area { flex: 1; display: flex; align-items: center; justify-content: center; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--hover-bg); border-top: 3px solid var(--link-color); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.loading-text { font-size: 0.85rem; color: var(--text-secondary); font-weight: 700; text-align: center; }

.general-board-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--divider-color); flex-shrink: 0; }
.general-board-header .post-count { font-size: 0.85rem; color: var(--text-secondary); font-weight: 800; }

.btn-write.outlined {
  background: none; border: 1px solid var(--link-color); color: var(--link-color);
  padding: 6px 14px; border-radius: 6px; font-size: 0.8rem; font-weight: 800; cursor: pointer; transition: all 0.2s;
}
.theme-lh .btn-write.outlined { border-color: #38a169; color: #38a169; }
.theme-sh .btn-write.outlined { border-color: #3182ce; color: #3182ce; }
.theme-private .btn-write.outlined { border-color: #c08457; color: #c08457; }

.empty-only-container { flex: 1; display: flex; align-items: center; justify-content: center; padding: 20px 0; }
.empty-state { width: 100%; padding: 40px 20px; text-align: center; background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 20px; }
.empty-icon { font-size: 2.2rem; display: block; margin-bottom: 10px; }
.empty-title { font-size: 1.05rem; font-weight: 800; margin: 0 0 8px; }
.empty-desc { font-size: 0.82rem; color: var(--text-secondary); line-height: 1.5; margin: 0; }

.locked-board-content { flex: 1; display: flex; align-items: center; justify-content: center; padding: 10px 0; }
.lock-visual { width: 100%; padding: 25px 20px; text-align: center; background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 20px; }
.lock-icon { font-size: 2.2rem; display: block; margin-bottom: 10px; }
.lock-title { font-size: 1.05rem; font-weight: 800; margin-bottom: 8px; }
.lock-desc { font-size: 0.82rem; color: var(--text-secondary); line-height: 1.4; margin-bottom: 12px; }
.btn-join-interest { background: var(--link-color); color: white; border: none; padding: 10px 24px; border-radius: 10px; font-weight: 800; cursor: pointer; transition: transform 0.2s; }
.already-joined-msg { font-size: 0.82rem; font-weight: 700; color: #38a169; }

/* [시니어 조치] 보관된 소통방 UI 압축 (스크롤 제거 핵심) */
.dormant-preview-content { flex: 1; display: flex; align-items: center; justify-content: center; padding: 5px 0; }
.dormant-visual { width: 100%; text-align: center; background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 20px; padding: 20px 15px; }
.dormant-icon { font-size: 2.2rem; display: block; margin-bottom: 10px; }
.dormant-title { font-size: 1.05rem; font-weight: 800; margin-bottom: 12px; }
.dormant-stats-box { margin-bottom: 15px; width: 100%; max-width: 280px; margin-left: auto; margin-right: auto; }
.dormant-stats-detail { font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 8px; }
.dormant-stats-detail b { color: var(--link-color); font-weight: 800; }
.gauge-meta-row { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 4px; }
.dormant-guide-text { font-size: 0.68rem; color: var(--text-muted); font-weight: 600; text-align: left; flex: 1; }
.gauge-count-text { font-size: 0.68rem; font-weight: 800; color: var(--text-secondary); }
.gauge-track-modern { height: 6px; background: var(--hover-bg); border-radius: 3px; overflow: hidden; border: 1px solid var(--border-color); }
.gauge-fill-modern { height: 100%; background: var(--link-color); border-radius: 3px; transition: width 0.5s ease; }

.btn-wakeup-large { background: var(--link-color); color: white; border: none; padding: 10px 24px; border-radius: 10px; font-size: 0.85rem; font-weight: 800; cursor: pointer; transition: all 0.2s; }
.btn-wakeup-large.is-clicked { background: #38a169; }

/* [핵심] 통합 하단 푸터 및 버튼 스타일 (기관별 색상 동기화) */
.preview-footer { flex-shrink: 0; padding-top: 15px; }
.btn-primary-action {
  width: 100%; height: 52px; display: flex; align-items: center; justify-content: center;
  color: white; border: none; border-radius: 12px; font-size: 0.95rem; font-weight: 800;
  cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.btn-primary-action:hover { transform: translateY(-1px); filter: brightness(1.1); }

/* [시니어 조치] 상세 모달 상단 배지와 버튼 색상 완벽 일치 (글자 가려짐 버그 수정) */
.theme-lh .btn-primary-action.theme-bg,
.theme-lh .gauge-fill-modern { background-color: #38a169 !important; color: white !important; }
.theme-lh .dormant-stats-detail b { color: #38a169 !important; background: none !important; }

.theme-sh .btn-primary-action.theme-bg,
.theme-sh .gauge-fill-modern { background-color: #3182ce !important; color: white !important; }
.theme-sh .dormant-stats-detail b { color: #3182ce !important; background: none !important; }

.theme-private .btn-primary-action.theme-bg,
.theme-private .gauge-fill-modern { background-color: #c08457 !important; color: white !important; }
.theme-private .dormant-stats-detail b { color: #c08457 !important; background: none !important; }

.post-list-area { width: 100%; }
.mini-spinner { width: 20px; height: 20px; border: 2px solid var(--hover-bg); border-top: 2px solid var(--link-color); border-radius: 50%; animation: spin 1s linear infinite; }
</style>
