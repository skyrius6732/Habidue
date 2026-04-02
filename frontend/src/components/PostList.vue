<template>
  <div class="post-list-wrapper" :class="[`type-${type.toLowerCase()}`, { 'is-grid': type === 'PARTNER' }]">
    <!-- 로딩 상태 -->
    <div v-if="loading && posts.length === 0" class="list-loading">
      <div v-for="i in 5" :key="i" class="skeleton-card"></div>
    </div>

    <!-- 데이터가 있는 경우 -->
    <div v-else-if="posts.length > 0" class="posts-container">
      <div v-for="post in posts" :key="post.id" :id="`post-${post.id}`" class="post-item" @click="viewDetail(post.id)">
        <div class="post-item-header">
          <div class="post-main">
            <h4 class="post-title">
              <span v-if="post.category" class="post-category-badge">{{ getCategoryLabel(post.category) }}</span>
              <!-- [시니어] 블라인드 및 삭제 뱃지 (관리자/본인 구분) -->
              <template v-if="post.status === 'BLINDED'">
                <span v-if="isAdmin" class="blinded-status-badge admin-hidden">모니터링: 관리자숨김</span>
                <span v-else class="blinded-status-badge">관리자 숨김</span>
              </template>
              <template v-if="post.status === 'DELETED'">
                <span v-if="isAdmin" class="blinded-status-badge admin-deleted">모니터링: 관리자삭제</span>
              </template>
              <template v-if="post.status === 'USER_DELETED'">
                <span v-if="isAdmin" class="blinded-status-badge admin-user-deleted">모니터링: 사용자삭제</span>
              </template>
              <span class="title-text">{{ post.title }}</span>
            </h4>
            <p class="post-summary">{{ post.content }}</p>
          </div>
          <!-- 이미지 썸네일 (있을 경우 우측 상단 작게 노출) -->
          <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-mini-thumb">
            <img :src="post.imageUrls[0]" alt="thumb" loading="lazy" />
            <span v-if="post.imageUrls.length > 1" class="img-count">+{{ post.imageUrls.length - 1 }}</span>
          </div>
        </div>
        <!-- [시니어 조치] 하단 메타 정보 2행 분리 구조 -->
        <div class="post-meta-v2">
          <!-- 1행: 상호작용 지표 -->
          <div class="meta-row interaction-row">
            <span class="meta-item" :class="{ 'is-liked': post.liked }">
              <span class="m-icon-mini">{{ post.liked ? '❤️' : '🤍' }}</span> 
              {{ post.likeCount || 0 }}
            </span>
            <span class="meta-item"><span class="m-icon-mini">💬</span> {{ post.commentCount || 0 }}</span>
            <span class="meta-item">
              <span class="m-icon-mini white-eye-mini">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
              </span>
              {{ post.viewCount || 0 }}
            </span>
          </div>

          <!-- 2행: 유저 정보 및 시간 -->
          <div class="meta-row info-row">
            <div class="author-group">
              <AnimatedNickname
                :user-id="post.authorId"
                :nickname="post.authorName"
                :level="post.authorLevel || 1"
                :exp="post.authorExp || 0"
                :badges="post.authorBadges"
                :equipped-badge-name="post.authorEquippedBadgeName"
                :show-effects="post.showLevelEffects"
                :show-level-effects="post.showLevelEffects"
                :author-equipped-effect="post.authorEquippedEffect"
                :show-avatar="true"
                :karma-point="post.authorKarmaPoint"
              />
            </div>
            <span class="post-date-v2">{{ formatDate(post.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 더보기 버튼 -->
      <div v-if="hasMore" class="load-more-container">
        <button class="btn-load-more" :disabled="loading" @click="$emit('load-more')">
          {{ loading ? '불러오는 중...' : '더보기 ▾' }}
        </button>
      </div>
    </div>

    <!-- 데이터가 없는 경우 -->
    <div v-else class="empty-state" :class="[`type-${type.toLowerCase()}`]">
      <div class="empty-box">
        <span class="empty-icon">{{ emptyStateInfo.icon }}</span>
        <h3 class="empty-title">{{ emptyStateInfo.title }}</h3>
        <p class="empty-desc">{{ emptyStateInfo.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCategoryLabel } from '@/constants/postConstants'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const props = defineProps({
    posts: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    hasMore: { type: Boolean, default: false },
    type: { type: String, default: 'GENERAL' }, // GENERAL, NOTICE, REVIEW, PARTNER
    noticeId: { type: [String, Number], default: null }
    })

    const emit = defineEmits(['load-more', 'refresh'])
    const router = useRouter()
    const route = useRoute()
    const authStore = useAuthStore()

    const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
    const isAuthor = (authorId) => authStore.user?.id && Number(authorId) === Number(authStore.user.id)

    const emptyStateInfo = computed(() => {
  const configs = {
    GENERAL: { 
      icon: '🏛️', 
      title: '아직 등록된 소통글이 없습니다', 
      desc: '첫 소통의 주인공이 되어 이웃과 대화를 시작해보세요!' 
    },
    NOTICE: { 
      icon: '📢', 
      title: '아직 등록된 소통글이 없습니다', 
      desc: '해당 공고에 대해 궁금한 점이나 정보를 이웃과 나누어 보세요!' 
    },
    REVIEW: { 
      icon: '🏆', 
      title: '당첨의 기쁨을 나누어주세요', 
      desc: '여러분의 생생한 후기가 다른 분들에게 큰 희망과 팁이 됩니다.' 
    },
    PARTNER: { 
      icon: '🚚', 
      title: '아직 등록된 서비스 정보가 없습니다', 
      desc: '전문가나 업체 정보를 기다리거나, 필요한 서비스를 요청해보세요.' 
    }
  }
  return configs[props.type] || configs.GENERAL
})

    const viewDetail = (id) => {
  // [시니어 조치] 상세 페이지로 이동할 때 현재 게시판 맥락(menu, sub)을 함께 전달
  const currentQuery = route.query
  router.push({ 
    path: `/board/post/${id}`, 
    query: { ...currentQuery } 
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000) // seconds

  // [시니어 조치] 오늘 작성된 글 처리 (방금 전 > n분 전 > n시간 전)
  if (diff < 60) return '방금 전'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  
  // [시니어 조치] 하루가 지나면 날짜 형식으로 (YYYY.MM.DD)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}.${m}.${d}`
}

</script>

<style scoped>
.post-list-wrapper { width: 100%; }
.posts-container { display: flex; flex-direction: column; gap: 8px; }

/* [시니어 조치] 공고 소통방 스타일로 전면 통합 */
.post-item { 
  padding: 16px; border: 1px solid var(--border-color); border-radius: 12px; 
  cursor: pointer; transition: all 0.2s; background: var(--card-bg); 
}
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
.post-category-badge { 
  flex-shrink: 0; font-size: 0.72rem; font-weight: 800; color: var(--text-primary); 
  background: var(--hover-bg); padding: 3px 10px; border-radius: 6px; 
  border: 1px solid var(--border-color); display: inline-flex; align-items: center;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}

.blinded-status-badge { 
  flex-shrink: 0; font-size: 0.72rem; font-weight: 850; color: white; 
  background: #ed4956; padding: 3px 10px; border-radius: 6px; 
  display: inline-flex; align-items: center; box-shadow: 0 2px 5px rgba(237, 73, 86, 0.3); 
  margin-right: 6px;
}
.blinded-status-badge.admin-hidden {
  background: #e74c3c; /* 선명한 빨간색 */
}
.blinded-status-badge.admin-deleted {
  background: #8e44ad; /* 보라색 (삭제 상태) */
  box-shadow: 0 2px 5px rgba(142, 68, 173, 0.3);
}
.blinded-status-badge.admin-user-deleted {
  background: #d35400; /* 호박색/브라운 (사용자 삭제) */
  box-shadow: 0 2px 5px rgba(211, 84, 0, 0.3);
}

.post-summary { 
  font-size: 0.9rem; color: var(--text-secondary); margin: 0; 
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box; 
  -webkit-line-clamp: 2; -webkit-box-orient: vertical; line-height: 1.5;
}

.post-mini-thumb { 
  flex-shrink: 0; width: 60px; height: 60px; border-radius: 8px; 
  overflow: hidden; position: relative; border: 1px solid var(--border-color);
  background-color: var(--hover-bg); /* 로딩 전 배경색 확보 */
}
.post-mini-thumb img { width: 100%; height: 100%; object-fit: cover; }
.img-count { 
  position: absolute; bottom: 0; right: 0; background: rgba(0,0,0,0.6); 
  color: white; font-size: 0.6rem; padding: 2px 4px; border-radius: 4px 0 0 0;
}

/* [시니어 조치] 메타 정보 2행 구조 스타일 */
.post-meta-v2 { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.meta-row { display: flex; align-items: center; }

/* 1행: 상호작용 */
.interaction-row { gap: 12px; font-size: 0.8rem; color: var(--text-muted); font-weight: 600; }
.meta-item { display: flex; align-items: center; gap: 4px; }
.meta-item.is-liked { color: #ed4956; }
.m-icon-mini { display: flex; align-items: center; font-size: 0.85rem; }
.white-eye-mini { color: var(--text-muted); opacity: 0.8; }

/* 2행: 유저 및 시간 */
.info-row { justify-content: space-between; padding-top: 8px; border-top: 1px dotted var(--divider-color); }
.author-group { display: flex; align-items: center; gap: 8px; }
.author-name { font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); }
.author-badge-mini {
  cursor: help;
}

.post-date-v2 { font-size: 0.75rem; color: var(--text-muted); font-weight: 500; }

.load-more-container { text-align: center; margin-top: 20px; }

.empty-state { padding: 80px 20px; text-align: center; background: var(--card-bg); border: 1px dashed var(--border-color); border-radius: 20px; }
.empty-box { max-width: 400px; margin: 0 auto; }
.empty-icon { font-size: 3.5rem; display: block; margin-bottom: 20px; }
.empty-title { font-size: 1.25rem; font-weight: 800; margin: 0 0 10px; }
.type-notice .empty-title { color: #38a169; }
.type-review .empty-title { color: #d69e2e; }
.type-partner .empty-title { color: #3182ce; }
.empty-desc { font-size: 0.95rem; color: var(--text-secondary); line-height: 1.6; margin: 0 0 24px; word-break: keep-all; }
.btn-write-first { background: var(--link-color); color: white; border: none; padding: 12px 32px; border-radius: 10px; font-weight: 800; cursor: pointer; }

.btn-load-more { width: 100%; margin-top: 16px; padding: 14px; border-radius: 12px; border: 1px solid var(--border-color); background: var(--card-bg); font-weight: 700; color: var(--text-secondary); cursor: pointer; }

@media (max-width: 768px) {
  .post-title { font-size: 0.95rem; }
  .post-summary { font-size: 0.85rem; }
}
</style>
