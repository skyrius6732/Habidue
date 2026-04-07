<template>
  <div class="my-activity-tab-content fade-in">
    <div class="section-header-flex">
      <div class="text-group">
        <h2 class="section-title">📝 내 활동 기록</h2>
        <p class="section-desc">내가 작성한 게시글과 댓글들을 모아볼 수 있습니다.</p>
      </div>
      <div class="activity-sub-tabs">
        <button class="sub-tab-btn" :class="{ active: activeSubTab === 'posts' }" @click="$emit('update:activeSubTab', 'posts')">작성한 글</button>
        <button class="sub-tab-btn" :class="{ active: activeSubTab === 'comments' }" @click="$emit('update:activeSubTab', 'comments')">작성한 댓글</button>
      </div>
    </div>

    <!-- 작성한 게시글 그리드 -->
    <div v-if="activeSubTab === 'posts'" class="my-posts-grid-wrapper">
      <div v-if="myPosts.length > 0" class="insta-grid">
        <div v-for="post in myPosts" :key="post.id" :id="`posts-${post.id}`" class="insta-grid-item" @click="$emit('go-to-detail', post.id)">
          <div class="insta-item-content">
            <div class="insta-thumbnail-placeholder">
              <img v-if="post.thumbnailUrl" :src="post.thumbnailUrl" class="insta-thumb-img" />
              <div v-else class="insta-no-thumb">
                <span class="no-thumb-emoji">📄</span>
              </div>
            </div>
            <div class="insta-item-overlay">
              <div class="insta-stats">
                <span class="insta-stat"><i class="stat-icon">❤️</i> {{ post.likeCount }}</span>
                <span class="insta-stat"><i class="stat-icon">💬</i> {{ post.commentCount }}</span>
              </div>
            </div>
          </div>
          <div class="insta-item-info">
            <div class="insta-item-title">{{ post.title }}</div>
            <div class="insta-item-meta">
              <span class="meta-date">{{ formatDateDot(post.createdAt) }}</span>
              <span class="meta-view">조회 {{ post.viewCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="!postsLoading" class="empty-activity">
        <div class="empty-icon">🏜️</div>
        <p>아직 작성한 게시글이 없습니다.</p>
      </div>
      <div v-if="postsHasMore" class="load-more-container">
        <button class="btn-load-more" @click="$emit('fetch-more-posts')" :disabled="postsLoading">
          {{ postsLoading ? '로딩 중...' : '더 보기' }}
        </button>
      </div>
    </div>

    <!-- 작성한 댓글 리스트 -->
    <div v-if="activeSubTab === 'comments'" class="my-comments-list-wrapper">
      <div v-if="myComments.length > 0" class="comment-history-list">
        <div v-for="comment in myComments" :key="comment.id" :id="`comments-${comment.id}`" class="comment-history-item" @click="$emit('go-to-comment', comment.postId, comment.id)">
          <div class="comment-history-content">
            <div class="comment-text-bubble">
              <span class="quote-mark">"</span>
              {{ comment.content }}
              <span class="quote-mark">"</span>
            </div>
            <div class="comment-target-post">
              <span class="target-label">원문:</span>
              <span class="target-title">{{ comment.postTitle }}</span>
            </div>
            <div class="comment-history-footer">
              <span class="history-date">{{ formatDateDot(comment.createdAt) }}</span>
              <span class="history-like" v-if="comment.likeCount > 0">❤️ {{ comment.likeCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="!commentsLoading" class="empty-activity">
        <div class="empty-icon">💬</div>
        <p>아직 작성한 댓글이 없습니다.</p>
      </div>
      <div v-if="commentsHasMore" class="load-more-container">
        <button class="btn-load-more" @click="$emit('fetch-more-comments')" :disabled="commentsLoading">
          {{ commentsLoading ? '로딩 중...' : '더 보기' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  activeSubTab: String,
  myPosts: Array,
  myComments: Array,
  postsLoading: Boolean,
  commentsLoading: Boolean,
  postsHasMore: Boolean,
  commentsHasMore: Boolean
})

const emit = defineEmits(['update:activeSubTab', 'go-to-detail', 'go-to-comment', 'fetch-more-posts', 'fetch-more-comments'])

const formatDateDot = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}.${m}.${d}`;
}
</script>

<style scoped>
.my-activity-tab-content { display: flex; flex-direction: column; }
.activity-sub-tabs { display: flex; gap: 8px; background: var(--hover-bg); padding: 5px; border-radius: 12px; border: 1px solid var(--border-color); }
.sub-tab-btn { padding: 8px 20px; border: none; background: transparent; color: var(--text-secondary); font-size: 0.85rem; font-weight: 700; cursor: pointer; border-radius: 8px; transition: all 0.2s; }
.sub-tab-btn.active { background: var(--card-bg); color: var(--text-primary); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

/* Insta Grid Style */
.insta-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 10px; }
.insta-grid-item { cursor: pointer; border-radius: 16px; overflow: hidden; background: var(--card-bg); border: 1px solid var(--border-color); transition: transform 0.2s; }
.insta-grid-item:hover { transform: translateY(-5px); }
.insta-item-content { position: relative; aspect-ratio: 1 / 1; overflow: hidden; }
.insta-thumbnail-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: var(--hover-bg); }
.insta-thumb-img { width: 100%; height: 100%; object-fit: cover; }
.no-thumb-emoji { font-size: 2.5rem; opacity: 0.3; }
.insta-item-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s; }
.insta-grid-item:hover .insta-item-overlay { opacity: 1; }
.insta-stats { display: flex; gap: 15px; color: white; font-weight: 800; font-size: 1rem; }
.insta-stat { display: flex; align-items: center; gap: 5px; }
.insta-item-info { padding: 15px; }
.insta-item-title { font-size: 0.9rem; font-weight: 800; color: var(--text-primary); margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.insta-item-meta { display: flex; justify-content: space-between; font-size: 0.75rem; color: var(--text-secondary); font-weight: 600; }

/* Comment List Style */
.comment-history-list { display: flex; flex-direction: column; gap: 12px; }
.comment-history-item { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; cursor: pointer; transition: all 0.2s; }
.comment-history-item:hover { border-color: var(--link-color); background: var(--hover-bg); }
.comment-text-bubble { font-size: 1rem; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; line-height: 1.5; position: relative; }
.quote-mark { font-family: serif; font-size: 1.4rem; color: var(--link-color); opacity: 0.4; }
.comment-target-post { font-size: 0.85rem; color: var(--text-secondary); display: flex; gap: 6px; margin-bottom: 12px; background: var(--hover-bg); padding: 8px 12px; border-radius: 8px; }
.target-label { font-weight: 800; opacity: 0.6; }
.target-title { font-weight: 700; color: var(--link-color); }
.comment-history-footer { display: flex; justify-content: space-between; font-size: 0.75rem; color: var(--text-secondary); font-weight: 600; }

.load-more-container { display: flex; justify-content: center; margin-top: 30px; }
.btn-load-more { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 10px 30px; border-radius: 12px; color: var(--text-secondary); font-weight: 700; cursor: pointer; }
.empty-activity { text-align: center; padding: 80px 0; color: var(--text-secondary); }
.empty-icon { font-size: 3rem; margin-bottom: 15px; opacity: 0.4; }

@media (max-width: 768px) {
  .insta-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
}
</style>
