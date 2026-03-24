<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/plugins/axios'
import PageHeader from '@/components/PageHeader.vue'
import CommunitySidebar from '@/components/CommunitySidebar.vue'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const keyword = ref('')
const loading = ref(true)
const searchResult = ref({
  totalCount: 0,
  categoryCounts: {},
  results: []
})

const popularKeywords = ref([])
const currentTab = ref('ALL')
const activeMenu = ref('GENERAL')
const activeSubCategory = ref('ALL')
const searchContentRef = ref(null)
const loadMoreTrigger = ref(null)

const pageSize = 10
const currentPage = ref(0)
const isFetchingMore = ref(false)
const allDataLoaded = ref(false)

const hasMore = computed(() => {
  if (allDataLoaded.value) return false
  return (searchResult.value.results?.length || 0) < (searchResult.value.totalCount || 0)
})

const categoryInfo = {
  'ALL': { name: '전체', icon: '🌐' },
  'GENERAL': { name: '통합광장', icon: '🏛️' },
  'NOTICE': { name: '공고 소통방', icon: '📢' },
  'REVIEW': { name: '당첨후기', icon: '✨' },
  'PARTNER': { name: '파트너스', icon: '🤝' }
}

const fetchSearchResults = async (isMore = false) => {
  const q = route.query.q
  if (q === undefined || q.trim() === '') {
    router.push('/board')
    return
  }
  
  keyword.value = q
  
  if (!isMore) {
    loading.value = true
    currentPage.value = 0
    allDataLoaded.value = false
    searchResult.value = { totalCount: 0, categoryCounts: {}, results: [] }
  } else {
    isFetchingMore.value = true
  }

  try {
    const params = {
      keyword: q,
      page: currentPage.value,
      size: pageSize
    }
    const searchRes = await axios.get(`/api/v1/search`, { params })
    const newData = searchRes.data.data
    
    if (!isMore) {
      searchResult.value = newData
      if (newData.totalCount === 0) fetchPopularKeywords()
    } else {
      const newItems = newData.results || []
      if (newItems.length === 0) {
        allDataLoaded.value = true
      } else {
        const existingIds = new Set(searchResult.value.results.map(item => item.post.id))
        const uniqueNewItems = newItems.filter(item => !existingIds.has(item.post.id))
        if (uniqueNewItems.length > 0) {
          searchResult.value.results = [...searchResult.value.results, ...uniqueNewItems]
        } else {
          allDataLoaded.value = true
        }
      }
      searchResult.value.totalCount = newData.totalCount 
      searchResult.value.categoryCounts = newData.categoryCounts
    }

    if (!isMore && window.innerWidth <= 992) {
      nextTick(() => {
        if (searchContentRef.value) {
          searchContentRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      })
    }

  } catch (error) {
    console.error('검색 결과 로딩 실패:', error)
  } finally {
    loading.value = false
    isFetchingMore.value = false
    nextTick(() => {
      setupScrollObserver()
    })
  }
}

const loadMore = async () => {
  if (hasMore.value && !isFetchingMore.value) {
    currentPage.value++
    await fetchSearchResults(true)
  }
}

const fetchPopularKeywords = async () => {
  try {
    const res = await axios.get('/api/v1/search/popular-keywords')
    popularKeywords.value = res.data.data
  } catch (e) {}
}

const getCount = (category) => {
  if (category === 'ALL') return searchResult.value.totalCount || 0
  return searchResult.value.categoryCounts?.[category] || 0
}

const filteredResults = computed(() => {
  if (!searchResult.value.results) return []
  if (currentTab.value === 'ALL') return searchResult.value.results
  return searchResult.value.results.filter(item => item.post.type === currentTab.value)
})

let scrollObserver = null
const setupScrollObserver = () => {
  if (scrollObserver) scrollObserver.disconnect()
  if (!loadMoreTrigger.value) return

  scrollObserver = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasMore.value && !isFetchingMore.value) {
      loadMore()
    }
  }, { threshold: 0.1, rootMargin: '300px' })
  
  scrollObserver.observe(loadMoreTrigger.value)
}

const highlightKeyword = (text) => {
  if (!text || !keyword.value) return text
  const escapedKeyword = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedKeyword})`, 'gi')
  return text.replace(regex, '<mark class="highlight">$1</mark>')
}

const viewDetail = (id) => { router.push(`/board/post/${id}`) }

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '방금 전'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}.${m}.${d}`
}

watch(currentTab, () => {
  nextTick(() => setupScrollObserver())
})

onMounted(async () => {
  await fetchSearchResults()
})

onUnmounted(() => {
  if (scrollObserver) scrollObserver.disconnect()
})

watch(() => route.query.q, () => fetchSearchResults())
</script>

<template>
  <div class="search-view-container">
    <PageHeader 
      icon="🔍" 
      :title="keyword ? `'${keyword}' 검색 결과` : '통합 검색'" 
      :stats-text="keyword ? '검색 결과' : null" 
      :stats-value="searchResult.totalCount + '건'"
      bio="habiDue 커뮤니티 전체를 대상으로 검색한 결과입니다."
    />

    <div class="board-layout">
      <CommunitySidebar v-model:activeMenu="activeMenu" v-model:activeSubCategory="activeSubCategory" />

      <main ref="searchContentRef" class="search-main-content">
        <div class="search-tabs-container">
          <div class="search-tabs">
            <button 
              v-for="(info, key) in categoryInfo" :key="key"
              class="tab-btn" :class="{ active: currentTab === key, disabled: getCount(key) === 0 }"
              @click="getCount(key) > 0 && (currentTab = key)"
            >
              <span class="tab-icon">{{ info.icon }}</span>
              <span class="tab-name">{{ info.name }}</span>
              <span class="tab-count">{{ getCount(key) }}</span>
            </button>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <div class="loader"></div>
          <p>정보를 찾고 있어요...</p>
        </div>

        <div v-else-if="searchResult.totalCount === 0" class="no-result-state-v2">
          <div class="no-result-main">
            <div class="no-result-text">
              <span class="empty-icon-mini">🔍</span>
              <h3 class="no-result-title">검색 결과가 없습니다</h3>
              <p class="no-result-desc">입력하신 키워드와 일치하는 게시글이 없네요.<br class="desktop-only">단어의 철자가 정확한지 확인하시거나, 아래의 추천 키워드로 다시 검색해보세요.</p>
              <button @click="$router.push('/board')" class="back-link-btn">← 커뮤니티 홈으로 돌아가기</button>
            </div>
          </div>
          <div v-if="popularKeywords && popularKeywords.length > 0" class="recommend-section">
            <h4 class="recommend-title">이런 키워드는 어떠세요?</h4>
            <div class="recommend-tag-cloud">
              <button v-for="tag in popularKeywords" :key="tag" class="tag-chip-v2" @click="$router.push(`/search?q=${tag}`)">#{{ tag }}</button>
            </div>
          </div>
        </div>

        <div v-else class="search-results-content">
          <div class="posts-container">
            <div v-for="item in filteredResults" :key="item.post.id" class="post-item" @click="viewDetail(item.post.id)">
              <div class="post-item-header">
                <div class="post-main">
                  <h4 class="post-title">
                    <span class="category-badge">{{ categoryInfo[item.post.type]?.icon }} {{ categoryInfo[item.post.type]?.name }}</span>
                    <template v-if="item.post.status === 'BLINDED'">
                      <span v-if="isAdmin" class="blinded-status-badge admin-hidden">모니터링: 숨김</span>
                    </template>
                    <template v-if="item.post.status === 'DELETED'">
                      <span v-if="isAdmin" class="blinded-status-badge admin-deleted">모니터링: 삭제</span>
                    </template>
                    <span class="title-text" v-html="highlightKeyword(item.post.title)"></span>
                  </h4>
                  <p class="post-summary" v-html="highlightKeyword(item.snippet)"></p>
                </div>
                <div v-if="item.post.imageUrls && item.post.imageUrls.length > 0" class="post-mini-thumb">
                  <img :src="item.post.imageUrls[0]" alt="thumb" loading="lazy" />
                  <span v-if="item.post.imageUrls.length > 1" class="img-count">+{{ item.post.imageUrls.length - 1 }}</span>
                </div>
              </div>

              <div class="post-meta-v2">
                <div class="meta-row interaction-row">
                  <span class="meta-item" :class="{ 'is-liked': item.post.liked }">
                    <span class="m-icon-mini">{{ item.post.liked ? '❤️' : '🤍' }}</span> 
                    {{ item.post.likeCount || 0 }}
                  </span>
                  <span class="meta-item"><span class="m-icon-mini">💬</span> {{ item.post.commentCount || 0 }}</span>
                  <span class="meta-item">
                    <span class="m-icon-mini white-eye-mini">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                    </span>
                    {{ item.post.viewCount || 0 }}
                  </span>
                </div>

                <div class="meta-row info-row">
                  <div class="author-group">
                    <AnimatedNickname 
                      :user-id="item.post.authorId"
                      :nickname="item.post.authorName" 
                      :level="item.post.authorLevel || 1" 
                      :exp="item.post.authorExp || 0" 
                      :badges="item.post.authorBadges" 
                      :karma-point="item.post.authorKarmaPoint"
                    />
                  </div>
                  <span class="post-date-v2">{{ formatDate(item.post.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div ref="loadMoreTrigger" class="load-more-trigger">
            <div v-if="hasMore" class="scroll-loading">
              <div class="mini-loader"></div>
              <span>결과를 더 불러오는 중...</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.search-view-container { max-width: 1200px; margin: 0 auto; padding-bottom: 50px; }
.board-layout { display: flex; gap: 24px; padding: 0 20px; margin-top: 24px; }
.search-main-content { flex: 1; min-width: 0; scroll-margin-top: 120px; }

.search-tabs-container { margin-bottom: 24px; overflow-x: auto; scrollbar-width: none; }
.search-tabs-container::-webkit-scrollbar { display: none; }
.search-tabs { display: flex; gap: 12px; border-bottom: 1px solid var(--border-color); padding-bottom: 10px; }

.tab-btn { display: flex; align-items: center; gap: 8px; background: none; border: none; padding: 10px 16px; border-radius: 12px; cursor: pointer; white-space: nowrap; transition: all 0.2s; color: var(--text-secondary); font-weight: 600; font-size: 0.9rem; }
.tab-btn:hover:not(.disabled) { background-color: var(--hover-bg); color: var(--text-primary); }
.tab-btn.active { background-color: var(--link-color); color: white; }
.tab-btn.disabled { opacity: 0.4; cursor: default; }
.tab-count { background-color: rgba(0, 0, 0, 0.1); padding: 2px 8px; border-radius: 10px; font-size: 0.75rem; }
.tab-btn.active .tab-count { background-color: rgba(255, 255, 255, 0.2); }

.posts-container { display: flex; flex-direction: column; gap: 8px; }
.post-item { padding: 16px; border: 1px solid var(--border-color); border-radius: 12px; cursor: pointer; transition: all 0.2s; background: var(--card-bg); }
.post-item:hover { background: var(--hover-bg); border-color: var(--link-color); }
.post-item-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.post-main { flex: 1; min-width: 0; }
.post-title { 
  font-size: 1rem; font-weight: 800; margin: 0 0 8px; color: var(--text-primary); 
  line-height: 1.4; display: flex; align-items: center; gap: 8px;
  overflow: hidden;
}
.title-text {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0;
}
.category-badge { flex-shrink: 0; font-size: 0.72rem; font-weight: 800; color: var(--text-primary); background: var(--hover-bg); padding: 3px 10px; border-radius: 6px; border: 1px solid var(--border-color); display: inline-flex; align-items: center; gap: 4px; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }

.blinded-status-badge { 
  flex-shrink: 0; font-size: 0.72rem; font-weight: 850; color: white; 
  background: #ed4956; padding: 3px 10px; border-radius: 6px; 
  display: inline-flex; align-items: center; box-shadow: 0 2px 5px rgba(237, 73, 86, 0.3); 
  margin-right: 6px;
}
.blinded-status-badge.admin-hidden { background: #e74c3c; }
.blinded-status-badge.admin-deleted { background: #8e44ad; box-shadow: 0 2px 5px rgba(142, 68, 173, 0.3); }

.post-summary { font-size: 0.9rem; color: var(--text-secondary); margin: 0; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; line-height: 1.6; }
.post-mini-thumb { flex-shrink: 0; width: 60px; height: 60px; border-radius: 8px; overflow: hidden; position: relative; border: 1px solid var(--border-color); background-color: var(--hover-bg); }
.post-mini-thumb img { width: 100%; height: 100%; object-fit: cover; }
.img-count { position: absolute; bottom: 0; right: 0; background: rgba(0,0,0,0.6); color: white; font-size: 0.6rem; padding: 2px 4px; border-radius: 4px 0 0 0; }

.post-meta-v2 { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.meta-row { display: flex; align-items: center; }
.interaction-row { gap: 12px; font-size: 0.8rem; color: var(--text-muted); font-weight: 600; }
.meta-item { display: flex; align-items: center; gap: 4px; }
.meta-item.is-liked { color: #ed4956; }
.m-icon-mini { display: flex; align-items: center; font-size: 0.85rem; }
.white-eye-mini { color: var(--text-muted); opacity: 0.8; }
.info-row { justify-content: space-between; padding-top: 8px; border-top: 1px dotted var(--divider-color); }
.author-group { display: flex; align-items: center; gap: 8px; }
.post-date-v2 { font-size: 0.75rem; color: var(--text-muted); font-weight: 500; }

.no-result-state-v2 { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 24px; padding: 60px; text-align: left; }
.no-result-main { margin-bottom: 48px; }
.empty-icon-mini { font-size: 2.5rem; display: block; margin-bottom: 20px; opacity: 0.3; }
.no-result-title { font-size: 1.6rem; font-weight: 800; margin-bottom: 12px; color: var(--text-primary); }
.no-result-desc { color: var(--text-secondary); margin-bottom: 24px; font-size: 1rem; line-height: 1.6; }
.back-link-btn { background: none; border: none; color: var(--link-color); font-weight: 700; font-size: 0.95rem; cursor: pointer; padding: 0; transition: transform 0.2s; }
.back-link-btn:hover { text-decoration: underline; transform: translateX(-4px); }
.recommend-section { border-top: 1px solid var(--divider-color); padding-top: 40px; }
.recommend-title { font-size: 1.1rem; font-weight: 800; margin-bottom: 20px; color: var(--text-primary); }
.recommend-tag-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.tag-chip-v2 { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 8px 16px; border-radius: 12px; font-size: 0.9rem; color: var(--text-primary); cursor: pointer; transition: all 0.2s; font-weight: 600; }
.tag-chip-v2:hover { background: var(--link-color); color: white; border-color: var(--link-color); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 149, 246, 0.2); }

:deep(.highlight) { background-color: rgba(0, 149, 246, 0.1); color: var(--link-color); font-weight: 800; padding: 0 2px; border-radius: 4px; }
.loading-state { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 100px 20px; text-align: center; }
.loader { border: 3px solid var(--border-color); border-top: 3px solid var(--link-color); border-radius: 50%; width: 32px; height: 32px; animation: spin 1s linear infinite; margin-bottom: 20px; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.load-more-trigger { height: 100px; display: flex; align-items: center; justify-content: center; margin-top: 20px; }
.scroll-loading { display: flex; flex-direction: column; align-items: center; gap: 10px; color: var(--text-secondary); font-size: 0.85rem; font-weight: 600; }
.mini-loader { border: 2px solid var(--border-color); border-top: 2px solid var(--link-color); border-radius: 50%; width: 20px; height: 20px; animation: spin 1s linear infinite; }

@media (max-width: 992px) {
  .board-layout { flex-direction: column; gap: 12px; }
  .no-result-state-v2 { padding: 40px 24px; }
  .no-result-title { font-size: 1.3rem; }
  .tag-chip-v2 { padding: 6px 14px; font-size: 0.85rem; }
}
</style>
