<template>
  <aside class="board-sidebar">
    <!-- 1. 사이드바 통합 검색창 (최상단) -->
    <div class="sidebar-search-area">
      <div class="sidebar-search-wrapper">
        <span class="s-icon">🔍</span>
        <input 
          type="text" 
          v-model="searchKeyword" 
          placeholder="제목, 본문, 태그, 사용자 검색" 
          @keyup.enter="handleSidebarSearch"
          @keydown.down.prevent="moveFocus(1)"
          @keydown.up.prevent="moveFocus(-1)"
          @focus="showRecentSearches = true"
          @blur="handleBlur"
          class="sidebar-search-input"
        />
        <button v-if="searchKeyword" class="s-clear-btn" @click="searchKeyword = ''">&times;</button>
      </div>

      <!-- 최근 검색어 드롭다운 -->
      <transition name="fade">
        <div v-if="showRecentSearches && recentSearches.length > 0" class="recent-searches-layer">
          <div class="recent-header">
            <span>최근 검색어</span>
            <button class="all-clear-btn" @click="clearAllRecent">모두 삭제</button>
          </div>
          <div class="recent-list">
            <div v-for="(word, idx) in recentSearches" :key="word" 
                 class="recent-item" 
                 :class="{ focused: focusedIndex === idx }"
                 @mousedown="selectRecentSearch(word)">
              <span class="r-icon">🕒</span>
              <span class="r-text">{{ word }}</span>
              <button class="r-delete-btn" @click.stop="deleteRecentSearch(word)">&times;</button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 2. 메인 메뉴 (아코디언) -->
    <nav class="main-menu-nav">
      <div v-for="menu in mainMenus" :key="menu.id" class="menu-group">
        <div class="main-menu-item" 
             :class="{ active: activeMenu === menu.id, 'is-ranking-menu': menu.id === 'RANKING', 'is-messages-menu': menu.id === 'MESSAGES' }"
             @click="handleMainMenuClick(menu.id)">
          <span class="m-icon">{{ menu.icon }}</span>
          <span class="m-text" :class="{ 'is-bold': activeMenu === menu.id || openMenu === menu.id }">{{ menu.name }}</span>
          <span v-if="menu.id === 'MESSAGES' && messageStore.unreadCount > 0" class="unread-count-badge">{{ messageStore.unreadCount }}</span>
          <span v-if="activeMenu === menu.id" class="active-dot-indicator"></span>
        </div>
        
        <transition name="accordion">
          <div v-if="!isMobile && openMenu === menu.id && hasSubCategoriesFn(menu.id)" class="pc-sub-menu">
            <template v-for="sub in subCategories[menu.id]" :key="sub.id || sub.value">
              <button v-if="sub.type === 'ITEM'" 
                      class="sub-item-btn" :class="{ active: activeMenu === menu.id && activeSubCategory === sub.value }"
                      @click.stop="handleSubCategoryClick(sub.value)">
                <span class="sub-text">{{ sub.name }}</span>
              </button>

              <div v-else-if="sub.type === 'GROUP'" class="sub-group-container">
                <div class="sub-group-header" :class="{ 'is-active': isChildActive(sub) }" @click.stop="toggleSubGroup(sub.id)">
                  <span class="group-name">{{ sub.name }}</span>
                  <span class="group-chevron" :class="{ open: isSubGroupOpen(sub.id) }">▾</span>
                </div>
                <transition name="accordion">
                  <div v-if="isSubGroupOpen(sub.id)" class="nested-sub-menu">
                    <button v-for="child in sub.children" :key="child.value"
                            class="nested-item-btn" :class="{ active: activeMenu === menu.id && activeSubCategory === child.value }"
                            @click.stop="handleSubCategoryClick(child.value)">
                      <span class="nested-text">{{ child.name }}</span>
                    </button>
                  </div>
                </transition>
              </div>
            </template>
          </div>
        </transition>
      </div>
    </nav>

    <!-- 3. 모바일 세부 카테고리 -->
    <div v-if="isMobile && hasSubCategoriesFn(activeMenu)" class="mobile-sub-nav">
      <div class="sub-nav-header">
        <span class="filter-icon">🔍</span>
        <span class="filter-label">{{ mainMenus.find(m => m.id === activeMenu)?.name }}</span>
      </div>
      <div class="sub-category-list mobile-scroll">
        <template v-for="sub in subCategories[activeMenu]" :key="sub.id || sub.value">
          <button v-if="sub.type === 'ITEM'" 
                  class="sub-btn-chip" :class="{ active: activeSubCategory === sub.value }"
                  @click="handleSubCategoryClick(sub.value)">
            {{ sub.name }}
          </button>
          <template v-else-if="sub.type === 'GROUP'">
            <button v-for="child in sub.children" :key="child.value"
                    class="sub-btn-chip" :class="{ active: activeSubCategory === child.value }"
                    @click="handleSubCategoryClick(child.value)">
              {{ child.name }}
            </button>
          </template>
        </template>
      </div>
    </div>

    <!-- 4. 통합 트렌딩 위젯 (인기글/태그) -->
    <div class="trending-combined-widget">
      <div class="trending-tabs">
        <button class="t-tab-btn" :class="{ active: activeTrendingTab === 'TAGS' }" @click="activeTrendingTab = 'TAGS'">인기태그</button>
        <button class="t-tab-btn" :class="{ active: activeTrendingTab === 'POSTS' }" @click="activeTrendingTab = 'POSTS'">인기글</button>
      </div>

      <div class="trending-body">
        <div v-if="activeTrendingTab === 'POSTS'" class="trending-meta">
          <span class="t-date-label">{{ todayFormatted }}</span>
        </div>

        <transition name="fade-quick" mode="out-in">
          <div v-if="activeTrendingTab === 'POSTS'" key="posts" class="trending-list">
            <div v-for="(post, idx) in trendingPosts" :key="post.id" class="trending-item" @click="viewPostDetail(post.id)">
              <span class="rank">{{ idx + 1 }}</span>
              <span class="t-title">{{ post.title }}</span>
            </div>
            <div v-if="trendingPosts.length === 0" class="empty-trending">인기글이 없습니다.</div>
          </div>

          <div v-else key="tags" class="tag-cloud">
            <button v-for="(tag, idx) in popularTags" :key="tag.id" 
                    class="trending-tag-btn" @click="searchTag(tag.name)">
              <span class="t-rank highlight-rank">{{ idx + 1 }}</span>
              <span class="t-name">#{{ tag.name }}</span>
            </button>
            <div v-if="popularTags.length === 0" class="empty-trending">인기태그가 없습니다.</div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 5. 공고 정보 슬롯 (최하단 고정) -->
    <div class="sidebar-extra-slot">
      <slot name="extra"></slot>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessageStore } from '@/stores/message'
import axios from '@/plugins/axios'

const props = defineProps({
  activeMenu: { type: String, default: 'GENERAL' },
  activeSubCategory: { type: String, default: 'ALL' }
})

const emit = defineEmits(['update:activeMenu', 'update:activeSubCategory'])

const router = useRouter()
const route = useRoute()
const messageStore = useMessageStore()

const openMenu = ref(props.activeMenu)
const openSubGroups = ref([]) 
const isMobile = ref(window.innerWidth <= 992)
const trendingPosts = ref([])
const popularTags = ref([])
const activeTrendingTab = ref('TAGS')

const searchKeyword = ref(route.query.q || '')
const showRecentSearches = ref(false)
const focusedIndex = ref(-1)
const recentSearches = ref(JSON.parse(localStorage.getItem('recentSearches') || '[]'))

const moveFocus = (delta) => {
  if (!showRecentSearches.value || recentSearches.value.length === 0) return
  const next = focusedIndex.value + delta
  if (next >= 0 && next < recentSearches.value.length) focusedIndex.value = next
  else if (next < 0) focusedIndex.value = recentSearches.value.length - 1
  else focusedIndex.value = 0
}

const handleSidebarSearch = () => {
  if (focusedIndex.value > -1 && showRecentSearches.value) {
    selectRecentSearch(recentSearches.value[focusedIndex.value])
    return
  }
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  const updated = [keyword, ...recentSearches.value.filter(w => w !== keyword)].slice(0, 5)
  recentSearches.value = updated
  localStorage.setItem('recentSearches', JSON.stringify(updated))
  showRecentSearches.value = false
  focusedIndex.value = -1
  router.push({ path: '/search', query: { q: keyword } })
}

const handleBlur = () => { setTimeout(() => { showRecentSearches.value = false; focusedIndex.value = -1 }, 200) }
const selectRecentSearch = (word) => { 
  searchKeyword.value = word
  showRecentSearches.value = false
  focusedIndex.value = -1
  handleSidebarSearch() 
}
const deleteRecentSearch = (word) => { recentSearches.value = recentSearches.value.filter(w => w !== word); localStorage.setItem('recentSearches', JSON.stringify(recentSearches.value)) }
const clearAllRecent = () => { recentSearches.value = []; localStorage.removeItem('recentSearches') }

const todayFormatted = computed(() => {
  const now = new Date(); return `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')}`
})

const mainMenus = [
  { id: 'GENERAL', name: '통합광장', icon: '🏛️' },
  { id: 'NOTICE', name: '공고소통방', icon: '📢' },
  { id: 'REVIEW', name: '당첨후기', icon: '✨' },
  { id: 'PARTNER', name: '파트너스', icon: '🤝' },
  { id: 'RANKING', name: '활동랭킹', icon: '🎖️' }
]

const subCategories = {
  GENERAL: [
    { type: 'ITEM', name: '🌐 전체', value: 'ALL' },
    { type: 'GROUP', id: 'REGION', name: '📍 지역별 소통', children: [
      { name: '서울', value: 'SEOUL' }, { name: '경기/인천', value: 'METRO' }, { name: '충청/강원/세종', value: 'CHUNGCHEONG_GANGWON' }, { name: '경상/부산/대구', value: 'GYEONGSANG' }, { name: '전라/광주/제주', value: 'JEOLLA_JEJU' }
    ]},
    { type: 'GROUP', id: 'INSTITUTE', name: '🏢 기관별 소통', children: [
      { name: 'LH 소통', value: 'LH' }, { name: 'SH 소통', value: 'SH' }, { name: '민간임대 소통', value: 'PRIVATE_RENTAL' }
    ]}
  ],
  NOTICE: [
    { type: 'ITEM', name: '🔒 오픈 준비 소통방', value: 'PREPARING' },
    { type: 'ITEM', name: '🔥 활발한 소통방', value: 'ACTIVE' },
    { type: 'ITEM', name: '😴 보관된 소통방', value: 'ARCHIVED' },
    { type: 'ITEM', name: '❤️ 나의 관심 소통방', value: 'MY' }
  ],
  REVIEW: [
    { type: 'ITEM', name: '🌐 전체', value: 'ALL' }, { type: 'ITEM', name: '🏆 당첨자 인터뷰', value: 'INTERVIEW' }, { type: 'ITEM', name: '📝 서류/청약 팁', value: 'TIPS' }, { type: 'ITEM', name: '🏠 입주/사전점검', value: 'MOVE_IN' }
  ],
  PARTNER: [
    { type: 'ITEM', name: '🌐 전체', value: 'ALL' }, { type: 'ITEM', name: '🚚 이사', value: 'MOVING' }, { type: 'ITEM', name: '🧼 청소', value: 'CLEANING' }, { type: 'ITEM', name: '🛋️ 인테리어', value: 'INTERIOR' }
  ]
}

const hasSubCategoriesFn = (menuId) => !!subCategories[menuId]
const toggleSubGroup = (groupId) => { if (openSubGroups.value.includes(groupId)) openSubGroups.value = openSubGroups.value.filter(id => id !== groupId); else openSubGroups.value.push(groupId) }
const isSubGroupOpen = (groupId) => openSubGroups.value.includes(groupId)
const isChildActive = (group) => group.children?.some(child => child.value === props.activeSubCategory)

const handleMainMenuClick = (menuId) => {
  if (menuId === 'MESSAGES') {
    emit('update:activeMenu', menuId)
    router.push('/keywords?tab=messages')
    return
  }
  const defaultSub = menuId === 'NOTICE' ? 'PREPARING' : 'ALL'
  if (openMenu.value === menuId) openMenu.value = null; else openMenu.value = menuId
  emit('update:activeMenu', menuId); emit('update:activeSubCategory', defaultSub)
  router.push({ path: '/board', query: { menu: menuId, sub: defaultSub } })
}

const handleSubCategoryClick = (subValue) => { emit('update:activeSubCategory', subValue); router.push({ path: '/board', query: { menu: props.activeMenu, sub: subValue } }) }
const viewPostDetail = (postId) => { router.push(`/board/post/${postId}`) }
const searchTag = (tagName) => { router.push({ path: '/search', query: { q: tagName } }) }

const fetchTrendingPosts = async () => { try { const res = await axios.get('/api/posts/trending', { params: { limit: 5 } }); trendingPosts.value = res.data.data } catch (e) {} }
const fetchPopularTags = async () => { try { const res = await axios.get('/api/v1/search/popular-keywords'); popularTags.value = res.data } catch (e) {} }

defineExpose({ fetchTrendingPosts, fetchPopularTags })
const handleResize = () => { isMobile.value = window.innerWidth <= 992 }

onMounted(() => { fetchTrendingPosts(); fetchPopularTags(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })
watch(() => route.query.q, (newQ) => { searchKeyword.value = newQ || '' })
watch(() => props.activeMenu, (newVal) => { 
  if (newVal) {
    openMenu.value = newVal 
    const currentSubs = subCategories[newVal]
    if (currentSubs) {
      currentSubs.forEach(sub => {
        if (sub.type === 'GROUP' && sub.children?.some(c => c.value === props.activeSubCategory)) {
          if (!openSubGroups.value.includes(sub.id)) openSubGroups.value.push(sub.id)
        }
      })
    }
  }
}, { immediate: true })
</script>

<style scoped>
.board-sidebar { 
  width: 260px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; 
  position: sticky; top: 72px; align-self: flex-start; padding-bottom: 40px;
}

.sidebar-search-area { position: relative; margin-bottom: -8px; }
.sidebar-search-wrapper {
  display: flex; align-items: center; background-color: var(--card-bg); 
  border: 1px solid var(--border-color); border-radius: 12px; padding: 0 12px; transition: all 0.2s;
}
.sidebar-search-wrapper:focus-within { border-color: var(--link-color); box-shadow: 0 0 0 3px rgba(0, 149, 246, 0.1); }
.sidebar-search-input { width: 100%; height: 44px; background: none; border: none; color: var(--text-primary); font-size: 0.9rem; font-weight: 500; outline: none; }
.s-icon { font-size: 0.85rem; color: var(--text-secondary); margin-right: 10px; opacity: 0.7; }
.s-clear-btn { background: none; border: none; color: var(--text-secondary); font-size: 1.1rem; cursor: pointer; padding: 0 4px; }

.recent-searches-layer {
  position: absolute; top: calc(100% + 6px); left: 0; width: 100%; background: var(--card-bg); 
  border: 1px solid var(--border-color); border-radius: 12px; z-index: 2000; box-shadow: 0 10px 30px rgba(0,0,0,0.15);
}
.recent-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--divider-color); font-size: 0.75rem; color: var(--text-secondary); font-weight: 700; }
.recent-list { max-height: 200px; overflow-y: auto; }
.recent-item { display: flex; align-items: center; padding: 12px 16px; cursor: pointer; gap: 10px; transition: background 0.2s; }
.recent-item:hover, .recent-item.focused { background-color: var(--hover-bg); }
.r-text { font-size: 0.85rem; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.r-delete-btn { background: none; border: none; color: var(--text-secondary); cursor: pointer; opacity: 0.5; font-size: 1.1rem; }
.all-clear-btn { background: none; border: none; color: var(--text-secondary); cursor: pointer; font-size: 0.75rem; }

.main-menu-nav { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; overflow: hidden; }
.menu-group { border-bottom: 1px solid var(--divider-color); }
.menu-group:last-child { border-bottom: none; }
.main-menu-item { padding: 16px 20px; display: flex; align-items: center; gap: 12px; cursor: pointer; }
.main-menu-item.active { background: rgba(0, 149, 246, 0.05); color: var(--link-color); }

.unread-count-badge {
  background: #e74c3c; color: white; font-size: 0.7rem; font-weight: 900;
  min-width: 18px; height: 18px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 5px; margin-left: auto;
}

/* [시니어] 활동 랭킹 메뉴 전용 빛나는(Bling) 효과 - 타 메뉴에 영향 없도록 엄격히 적용 */
.main-menu-item:not(.is-ranking-menu).active::after { display: none !important; }

.main-menu-item.is-ranking-menu.active {
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.12), rgba(0, 149, 246, 0.05));
  position: relative;
  overflow: hidden;
}
.main-menu-item.is-ranking-menu.active::after {
  content: ""; position: absolute; top: -50%; left: -60%; width: 20%; height: 200%;
  background: linear-gradient(to right, transparent, rgba(255, 255, 255, 0.3), transparent);
  transform: rotate(30deg);
  animation: menu-shine 3s infinite;
}
@keyframes menu-shine {
  0% { left: -60%; }
  20% { left: 120%; }
  100% { left: 120%; }
}

.main-menu-item .m-text { font-weight: 500; font-size: 0.95rem; }
.main-menu-item .m-text.is-bold { font-weight: 800; }

.pc-sub-menu { background-color: var(--hover-bg); padding: 4px 0; display: flex; flex-direction: column; }
.sub-item-btn { padding: 10px 20px 10px 52px; text-align: left; background: none; border: none; font-size: 0.85rem; color: var(--text-secondary); cursor: pointer; }
.sub-item-btn.active { color: var(--link-color); font-weight: 800; }

.sub-group-container { display: flex; flex-direction: column; }
.sub-group-header { padding: 10px 20px 10px 52px; display: flex; align-items: center; justify-content: space-between; cursor: pointer; }
.sub-group-header .group-name { font-size: 0.85rem; font-weight: 700; }
.sub-group-header.is-active .group-name { color: var(--link-color); }
.group-chevron { font-size: 0.7rem; transition: transform 0.3s; opacity: 0.5; }
.group-chevron.open { transform: rotate(180deg); }
.nested-sub-menu { background-color: rgba(0, 0, 0, 0.02); display: flex; flex-direction: column; padding: 4px 0; border-left: 2px solid var(--border-color); margin-left: 56px; }
.nested-item-btn { padding: 8px 12px; text-align: left; background: none; border: none; font-size: 0.8rem; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; }
.nested-item-btn:hover { color: var(--link-color); }
.nested-item-btn.active { color: var(--link-color); font-weight: 800; }
.nested-text::before { content: "•"; margin-right: 8px; opacity: 0.4; }

.trending-combined-widget { 
  background: var(--card-bg); border: 1px solid var(--border-color); 
  border-radius: 16px; overflow: hidden; display: flex; flex-direction: column;
}
.trending-tabs { display: flex; background: var(--hover-bg); padding: 4px; gap: 4px; position: relative; }
.t-tab-btn { 
  flex: 1; border: none; background: none; padding: 8px; border-radius: 10px; 
  font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; transition: all 0.2s;
}
.t-tab-btn.active { background: var(--card-bg); color: var(--link-color); box-shadow: 0 2px 6px rgba(0,0,0,0.05); }

.trending-body { padding: 16px; position: relative; min-height: 180px; }
.trending-meta { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.t-date-label { font-size: 0.65rem; color: var(--text-secondary); font-weight: 700; opacity: 0.6; }

.trending-list { display: flex; flex-direction: column; gap: 12px; }
.trending-item { display: flex; align-items: flex-start; gap: 10px; cursor: pointer; }
.trending-item:hover .t-title { color: var(--link-color); }
.trending-item .rank { font-size: 0.8rem; font-weight: 900; color: var(--link-color); min-width: 14px; }
.trending-item .t-title { font-size: 0.78rem; font-weight: 600; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.tag-cloud { display: flex; flex-direction: column; gap: 6px; }
.trending-tag-btn { 
  display: flex; align-items: center; gap: 8px; background: none; border: none; 
  padding: 6px 0; cursor: pointer; transition: all 0.2s; width: 100%; text-align: left;
}
.trending-tag-btn:hover .t-name { color: var(--link-color); transform: translateX(4px); }
.t-rank.highlight-rank { font-size: 0.75rem; font-weight: 900; color: var(--link-color); opacity: 0.8; min-width: 14px; }
.t-name { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); transition: all 0.2s; }

.empty-trending { font-size: 0.8rem; color: var(--text-secondary); text-align: center; padding: 40px 0; }

.sidebar-extra-slot { width: 100%; }

.fade-quick-enter-active, .fade-quick-leave-active { transition: opacity 0.15s ease; }
.fade-quick-enter-from, .fade-quick-leave-to { opacity: 0; }

.accordion-enter-active, .accordion-leave-active { transition: all 0.3s ease; max-height: 400px; overflow: hidden; }
.accordion-enter-from, .accordion-leave-to { max-height: 0; opacity: 0; }

@media (max-width: 992px) {
  .board-sidebar { width: 100%; position: static; gap: 12px; }
  .sidebar-search-area { margin-bottom: 0; }
  .main-menu-nav { display: grid; grid-template-columns: repeat(2, 1fr); }
  .mobile-sub-nav { display: flex; flex-direction: column; gap: 12px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 16px; }
  .sub-category-list { display: flex; overflow-x: auto; gap: 8px; scrollbar-width: none; padding: 4px 0; }
  .sub-btn-chip { 
    white-space: nowrap; padding: 8px 16px; border-radius: 20px; 
    border: 1px solid var(--border-color); font-size: 0.85rem; 
    background: var(--card-bg); color: var(--text-secondary);
    font-weight: 600; transition: all 0.2s;
  }
  .sub-btn-chip.active { 
    background: var(--link-color); color: white; border-color: var(--link-color);
    box-shadow: 0 4px 10px rgba(0, 149, 246, 0.2);
  }
  .trending-body { min-height: auto; }
}
</style>
