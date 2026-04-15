<template>
  <div class="barter-card" @click="handleCardClick">
    <div class="card-image-wrapper">
      <img v-if="post.imageUrls && post.imageUrls.length > 0" :src="post.imageUrls[0]" class="card-image" alt="item" loading="lazy" />
      <div v-else class="no-image">📦 사진 없음</div>
      
      <!-- 상태 뱃지 -->
      <div v-if="statusConfig" class="status-badge" :style="{ backgroundColor: statusConfig.color }">
        {{ statusConfig.label }}
      </div>
    </div>
    
    <div class="card-info">
      <div class="card-badges">
        <span v-if="post.category" class="badge-category">{{ getCategoryLabel(post.category) }}</span>
        <span v-if="conditionConfig" class="badge-condition">{{ conditionConfig.label }}</span>
      </div>

      <h4 class="card-title" :title="post.title">{{ post.title }}</h4>
      <div class="wanted-item">
        <span class="wanted-label">희망 교환:</span>
        <span class="wanted-text" :title="post.wantedItem">{{ post.wantedItem || '자유 교환' }}</span>
      </div>
      
      <div class="card-footer">
        <AnimatedNickname
          :user-public-id="post.authorPublicId"
          :nickname="post.authorName"
          :level="post.authorLevel || 1"
          :exp="post.authorExp || 0"
          :badges="post.authorBadges"
          :equipped-badge-name="post.authorEquippedBadgeName"
          :show-effects="post.showLevelEffects"
          :show-level-effects="post.showLevelEffects"
          :show-equipped-effect="post.showEquippedEffect"
          :equipped-tier="post.authorEquippedTier"
          :author-equipped-effect="post.authorEquippedEffect"
          :show-avatar="true"
          :karma-point="post.authorKarmaPoint"
          :author-active="post.authorActive"
          class="animated-nickname-compact"
        />
        <span class="post-date">{{ formatDate(post.createdAt) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ITEM_CONDITION, BARTER_STATUS, getCategoryLabel } from '@/constants/postConstants'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const props = defineProps({
  post: { type: Object, required: true }
})

const emit = defineEmits(['click'])

const conditionConfig = computed(() => ITEM_CONDITION[props.post.itemCondition])
const statusConfig = computed(() => BARTER_STATUS[props.post.barterStatus])

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  return `${date.getMonth() + 1}.${date.getDate()}`
}

const handleCardClick = (e) => {
  if (e.target.closest('.nickname-wrapper')) return
  emit('click')
}
</script>

<style scoped>
.barter-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  height: 100%; /* 그리드 높이에 맞춤 */
  min-width: 0; /* 콘텐츠 크기로 제약되지 않도록 */
}

.barter-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: var(--hover-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  flex-shrink: 0;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.no-image {
  font-size: 0.9rem;
  color: var(--text-secondary);
  opacity: 0.6;
}

.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 20px;
  color: white;
  font-size: 0.7rem;
  font-weight: 800;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  z-index: 5;
}

.card-info {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
  flex: 1;
  padding-bottom: 60px;
  min-width: 0; /* flex 컨테이너 내에서 콘텐츠로 제약되지 않도록 */
}

.card-badges {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  height: 20px;
  flex-shrink: 0;
  overflow: hidden;
}

.badge-category {
  background: rgba(0, 149, 246, 0.1);
  color: var(--link-color);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
}

.badge-condition {
  background: var(--hover-bg);
  color: var(--text-secondary);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  white-space: nowrap;
}

.card-title {
  font-size: 0.95rem;
  font-weight: 800;
  margin: 0;
  color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 2.8em;
  flex-shrink: 0;
}

.wanted-item {
  display: flex;
  gap: 4px;
  align-items: center;
  font-size: 0.8rem;
  line-height: 1.4;
  height: 1.4em;
  overflow: hidden;
  flex-shrink: 0;
  width: 100%;
  min-width: 0;
}

.wanted-label {
  color: var(--text-secondary);
  flex-shrink: 0;
}

.wanted-text {
  color: var(--link-color);
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  position: absolute;
  bottom: 12px;
  left: 12px;
  right: 12px;
  z-index: 10;
  padding-top: 8px;
  border-top: 1px solid var(--divider-color);
}

:deep(.animated-nickname-compact) {
  font-size: 0.8rem !important;
  min-width: 0;
  flex: 1;
}

:deep(.animated-nickname-compact .nickname-text) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.animated-nickname-compact .nickname-tooltip) {
  z-index: 10000 !important;
}

.post-date {
  font-size: 0.75rem;
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .card-info {
    padding-bottom: 55px;
  }
  :deep(.animated-nickname-compact) {
    font-size: 0.75rem !important;
  }
}
</style>
