<template>
  <div class="board-view-container post-detail-page">
    <PageHeader 
      :icon="postTypeIcon" 
      :title="boardTypeLabel" 
      :stats-text="null" 
      :stats-value="currentStatsDisplay"
      :bio="post?.noticeTitle || 'habiDue 커뮤니티'"
    />

    <div class="board-layout">
      <CommunitySidebar 
        v-model:activeMenu="activeMenu"
        v-model:activeSubCategory="activeSubCategory"
      />

      <main class="board-main-content">
        <div v-if="post" class="post-container" :class="post.noticeSource">
          
          <!-- [시니어] 관리자 전용 액션 툴바 -->
          <div v-if="isAdmin" class="admin-top-action-bar">
            <div class="admin-status-info">
              <span class="a-label">🛠️ 운영자 모드</span>
              <span class="a-status" :class="post.status">{{ post.status }}</span>
            </div>
            <div class="admin-btns">
              <button v-if="post.status === 'BLINDED'" class="btn-admin-restore" @click="handleRestorePost">✅ 게시글 복구</button>
              <button class="btn-admin-nav" @click="router.push({ name: 'adminCommunity', query: { postId: post.id } })">관리자 메뉴</button>
            </div>
          </div>

          <div class="system-notice-bar-soft">
            <p class="system-msg">📢 <b>공지:</b> 깨끗한 소통 문화를 위해 비방, 욕설, 개인정보 노출은 삼가해 주세요. 위반 시 정책에 따라 조치될 수 있습니다.</p>
          </div>

          <article class="post-card" :class="{ 'is-blinded-author': post.status === 'BLINDED' }">
            <!-- [시니어] 본인 블라인드 게시글 안내 배너 -->
            <div v-if="post.status === 'BLINDED' && isAuthor && !isAdmin" class="blinded-author-banner">
              <span class="b-icon">🚫</span>
              <div class="b-text">
                <strong>관리자에 의해 숨김 처리된 게시글입니다.</strong>
                <p>커뮤니티 가이드라인 위반 사유로 타인에게는 노출되지 않습니다. 자세한 내용은 마이페이지 카르마 히스토리를 확인해 주세요.</p>
              </div>
            </div>

            <div class="card-top-control-bar-compact">
              <div class="nav-action-group">
                <button class="btn-nav-mini" @click="goToPrevPost" :disabled="!post.prevId" title="이전글">
                  <span class="nav-icon-mini-thin">︿</span>
                </button>
                <button class="btn-nav-mini" @click="goToNextPost" :disabled="!post.nextId" title="다음글">
                  <span class="nav-icon-mini-thin">﹀</span>
                </button>
                <button class="btn-nav-mini text-btn" @click="handleGoBack">목록</button>
                <button class="btn-nav-mini text-btn" @click="scrollToComments">댓글</button>
              </div>
              
              <div class="more-menu-container">
                <button class="btn-more-icon-mini" @click.stop="toggleMoreMenu">···</button>
                <Transition name="pop">
                  <div v-if="isMoreMenuOpen" class="more-dropdown-compact" v-click-outside="() => isMoreMenuOpen = false">
                    <template v-if="isAuthor && !isBoardClosed">
                      <button class="dropdown-item-mini" @click="goToEdit">게시글 수정</button>
                      <button class="dropdown-item-mini text-danger" @click="handleDeletePost">게시글 삭제</button>
                      <div class="dropdown-divider"></div>
                    </template>
                    <template v-else-if="!isAuthor">
                      <button class="dropdown-item-mini text-danger" @click="openReportModal('POST', post.id, post.status)">게시글 신고</button>
                      <div class="dropdown-divider"></div>
                    </template>
                    <button class="dropdown-item-mini" @click="copyUrl">URL 복사</button>
                    <button v-if="canShare" class="dropdown-item-mini" @click="shareUrl">URL 공유</button>
                  </div>
                </Transition>
              </div>
            </div>

            <header class="post-header">
              <h1 class="post-title">
                <span v-if="post?.category" class="post-category-label-detail">{{ getCategoryLabel(post.category) }}</span>
                {{ post?.title }}
              </h1>
              <div class="post-meta-info">
                <div class="author-block">
                  <AnimatedNickname
                    :user-id="post.authorId"
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
                  />
                </div>
                <div class="post-info-right">                  <span class="info-item date">{{ formatFullDate(post.createdAt) }}</span>
                </div>
              </div>
            </header>

            <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-image-section">
              <div class="slider-container">
                <div class="slider-wrapper" ref="sliderRef" @scroll="handleSliderScroll">
                  <div v-for="(img, idx) in post.imageUrls" :key="idx" class="slider-item">
                    <img :src="getFullImageUrl(img)" alt="post content" @click="openImageModal(img)" />
                  </div>
                </div>
                <template v-if="post.imageUrls.length > 1">
                  <button class="slider-nav prev" @click="scrollSlider(-1)">❮</button>
                  <button class="slider-nav next" @click="scrollSlider(1)">❯</button>
                </template>
              </div>
              <div v-if="post.imageUrls.length > 1" class="slider-pagination">
                <span v-for="(_, idx) in post.imageUrls" :key="idx" class="p-dot" :class="{ active: currentImgIdx === idx }" @click="goToSlide(idx)"></span>
              </div>
            </div>

            <div class="post-content-area">{{ post.content }}</div>

            <div class="post-tags-footer" v-if="post.tags && post.tags.length > 0">
              <span v-for="tag in post.tags" :key="tag.id" class="footer-tag">#{{ tag.name }}</span>
            </div>

            <footer class="post-footer-bar">
              <div class="footer-left">
                <button class="btn-interaction like" :class="{ active: post.liked }" @click="handleToggleLike">
                  <span class="i-icon">{{ post.liked ? '❤️' : '🤍' }}</span>
                  <span class="i-count">{{ post.likeCount }}</span>
                </button>
                <div class="btn-interaction-no-click">
                  <span class="i-icon">💬</span>
                  <span class="i-count">{{ totalCommentCount }}</span>
                </div>
                <div class="btn-interaction-no-click">
                  <span class="i-icon white-eye-large">
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                  </span>
                  <span class="i-count">{{ post.viewCount?.toLocaleString() }}</span>
                </div>
              </div>
              <div class="footer-right">
              </div>
            </footer>
          </article>

          <!-- 댓글 섹션 -->
          <section id="comment-section" class="comment-section">
            <h3 class="section-title">댓글 <span>{{ totalCommentCount }}</span></h3>
            
            <div class="comment-input-box" :class="(isBoardClosed || isPostBlinded) ? 'closed-notice' : 'main-input'">
              <template v-if="!isBoardClosed && !isPostBlinded">
                <textarea v-model="newComment" placeholder="함께 정보를 나누고 따뜻한 응원의 메시지를 남겨주세요." class="comment-textarea"></textarea>
                <div class="input-footer">
                  <button @click="handleCreateComment(null)" class="btn-comment-submit" :disabled="!newComment.trim()">등록</button>
                </div>
              </template>
              <template v-else>
                <p class="closed-msg">{{ isPostBlinded ? '관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.' : '마감된 공고 게시판에서는 새로운 댓글을 작성할 수 없습니다.' }}</p>
              </template>
            </div>

            <div class="comment-list-container">
              <div v-for="comment in comments" :key="comment.id" class="comment-group">
                <div class="comment-item main-item" :id="`comment-${comment.id}`" :class="{ 'highlight': highlightedCommentId === comment.id }">
                  <div class="c-body">
                    <div class="c-header">
                      <AnimatedNickname
                        :user-id="comment.authorId"
                        :nickname="comment.authorName || '익명'"
                        :level="comment.authorLevel || 1"
                        :exp="comment.authorExp || 0"
                        :badges="comment.authorBadges"
                        :equipped-badge-name="comment.authorEquippedBadgeName"
                        :show-effects="comment.showLevelEffects"
                        :show-level-effects="comment.showLevelEffects"
                        :show-equipped-effect="comment.showEquippedEffect"
                        :equipped-tier="comment.authorEquippedTier"
                        :author-equipped-effect="comment.authorEquippedEffect"
                        :show-avatar="true"
                        :karma-point="comment.authorKarmaPoint"
                      />
                      
                      <!-- [시니어] 운영자 전용 댓글 관리 버튼 -->
                      <div v-if="isAdmin" class="admin-comment-controls">
                        <button v-if="comment.status === 'BLINDED'" class="btn-admin-c-restore" @click="handleRestoreComment(comment.id)">복구</button>
                        <button class="btn-admin-c-nav" @click="router.push({ name: 'adminCommunity', query: { commentId: comment.id } })">관리자 메뉴</button>
                      </div>

                      <div class="c-more-container">
                        <button class="btn-c-more" @click.stop="toggleCommentMenu(comment.id)">···</button>
                        <Transition name="pop">
                          <div v-if="activeCommentMenuId === comment.id" class="c-dropdown-compact" v-click-outside="() => activeCommentMenuId = null">
                            <template v-if="isMyComment(comment.authorId) && !isBoardClosed">
                              <template v-if="comment.status === 'ACTIVE'">
                                <button class="dropdown-item-mini" @click="startEditComment(comment)">수정</button>
                                <button class="dropdown-item-mini text-danger" @click="handleDeleteComment(comment.id)">삭제</button>
                              </template>
                              <template v-else>
                                <div class="dropdown-notice-mini">조치된 댓글</div>
                              </template>
                            </template>
                            <template v-else-if="!isMyComment(comment.authorId)">
                              <button class="dropdown-item-mini text-danger" @click="openReportModal('COMMENT', comment.id, comment.status)">신고</button>
                            </template>
                          </div>
                        </Transition>
                      </div>
                    </div>
                    
                    <template v-if="editingCommentId !== comment.id">
                      <p class="c-text" :class="{ 'is-blinded': comment.status === 'BLINDED', 'is-deleted': comment.status === 'DELETED' }">
                        <template v-if="comment.status === 'BLINDED' && !isAdmin">🚫 관리자에 의해 차단된 댓글입니다.</template>
                        <template v-else-if="comment.status === 'DELETED' && !isAdmin">🗑️ 운영 정책 위반으로 영구 삭제된 댓글입니다.</template>
                        <template v-else>{{ comment.content }}</template>
                      </p>
                      <div class="c-meta-footer" v-if="comment.status === 'ACTIVE' || isAdmin">
                        <span class="c-date-small">{{ formatFullDate(comment.createdAt) }}</span>
                        <button v-if="!isBoardClosed" class="btn-reply-action" @click="toggleReplyInput(comment.id)">답글</button>
                        <button class="btn-c-like" :class="{ 'is-liked': comment.liked }" @click="handleToggleCommentLike(comment)">
                          <span class="l-icon">{{ comment.liked ? '❤️' : '🤍' }}</span>
                          <span class="l-count">{{ comment.likeCount }}</span>
                        </button>
                      </div>
                    </template>
                    <template v-else>
                      <div class="c-edit-box">
                        <textarea v-model="editingContent" class="c-edit-textarea"></textarea>
                        <div class="c-edit-actions">
                          <button class="btn-save-mini" @click="saveEditComment(comment.id)">저장</button>
                          <button class="btn-cancel-mini" @click="editingCommentId = null">취소</button>
                        </div>
                      </div>
                    </template>

                    <div v-if="replyingToId === comment.id" class="reply-input-box-inline">
                      <textarea v-model="replyContent" class="reply-textarea-inline"></textarea>
                      <div class="reply-footer-inline">
                        <button @click="handleCreateComment(comment.id)" class="btn-reply-submit-inline" :disabled="!replyContent.trim()">등록</button>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="comment.children && comment.children.length > 0" class="replies-area-modern">
                  <div v-for="reply in getFlattenedReplies(comment)" :key="reply.id" 
                       class="reply-item-modern" :id="`comment-${reply.id}`" :class="{ 'highlight': highlightedCommentId === reply.id }">
                    <div class="c-body">
                      <div class="c-header">
                        <AnimatedNickname
                          :user-id="reply.authorId"
                          :nickname="reply.authorName || '익명'"
                          :level="reply.authorLevel || 1"
                          :exp="reply.authorExp || 0"
                          :badges="reply.authorBadges"
                          :equipped-badge-name="reply.authorEquippedBadgeName"
                          :show-effects="reply.showLevelEffects"
                          :show-level-effects="reply.showLevelEffects"
                          :show-equipped-effect="reply.showEquippedEffect"
                          :equipped-tier="reply.authorEquippedTier"
                          :author-equipped-effect="reply.authorEquippedEffect"
                          :show-avatar="true"
                          :karma-point="reply.authorKarmaPoint"
                        />

                        <!-- [시니어] 운영자 전용 답글 관리 버튼 -->
                        <div v-if="isAdmin" class="admin-comment-controls">
                          <button v-if="reply.status === 'BLINDED'" class="btn-admin-c-restore" @click="handleRestoreComment(reply.id)">복구</button>
                          <button class="btn-admin-c-nav" @click="$router.push({ name: 'adminCommunity', query: { commentId: reply.id } })">관리자 메뉴</button>
                        </div>

                        <div class="c-more-container">
                        <button class="btn-c-more" @click.stop="toggleCommentMenu(reply.id)">···</button>
                        <Transition name="pop">
                          <div v-if="activeCommentMenuId === reply.id" class="c-dropdown-compact" v-click-outside="() => activeCommentMenuId = null">
                            <template v-if="isMyComment(reply.authorId) && !isBoardClosed">
                              <template v-if="reply.status === 'ACTIVE'">
                                <button class="dropdown-item-mini" @click="startEditComment(reply)">수정</button>
                                <button class="dropdown-item-mini text-danger" @click="handleDeleteComment(reply.id)">삭제</button>
                              </template>
                              <template v-else>
                                <div class="dropdown-notice-mini">조치된 댓글</div>
                              </template>
                            </template>
                            <template v-else-if="!isMyComment(reply.authorId)">
                              <button class="dropdown-item-mini text-danger" @click="openReportModal('COMMENT', reply.id, reply.status)">신고</button>
                            </template>
                          </div>
                        </Transition>
                        </div>
                        </div>

                        <template v-if="editingCommentId !== reply.id">
                        <p class="c-text" :class="{ 'is-blinded': reply.status === 'BLINDED', 'is-deleted': reply.status === 'DELETED' }">
                        <template v-if="reply.status === 'ACTIVE' || isAdmin">
                          <span v-if="reply.targetAuthorName" class="mention-tag clickable" @click="scrollToComment(reply.parentId)">
                            @{{ reply.targetAuthorName }}
                          </span>
                          {{ reply.content }}
                        </template>
                        <template v-else-if="reply.status === 'BLINDED'">
                          🚫 관리자에 의해 차단된 댓글입니다.
                        </template>
                        <template v-else-if="reply.status === 'DELETED'">
                          🗑️ 운영 정책 위반으로 영구 삭제된 댓글입니다.
                        </template>
                        </p>
                        <div class="c-meta-footer" v-if="reply.status === 'ACTIVE' || isAdmin">
                        <span class="c-date-small">{{ formatFullDate(reply.createdAt) }}</span>
                        <button v-if="!isBoardClosed" class="btn-reply-action" @click="toggleReplyInput(reply.id)">답글</button>
                        <button class="btn-c-like mini" :class="{ 'is-liked': reply.liked }" @click="handleToggleCommentLike(reply)">
                          <span class="l-icon">{{ reply.liked ? '❤️' : '🤍' }}</span>
                          <span class="l-count">{{ reply.likeCount }}</span>
                        </button>
                        </div>
                        </template>                      <template v-else>
                        <div class="c-edit-box">
                          <textarea v-model="editingContent" class="c-edit-textarea"></textarea>
                          <div class="c-edit-actions">
                            <button class="btn-save-mini" @click="saveEditComment(reply.id)">저장</button>
                            <button class="btn-cancel-mini" @click="editingCommentId = null">취소</button>
                          </div>
                        </div>
                      </template>

                      <div v-if="replyingToId === reply.id" class="reply-input-box-inline inner">
                        <textarea v-model="replyContent" class="reply-textarea-inline"></textarea>
                        <div class="reply-footer-inline">
                          <button @click="handleCreateComment(reply.id, comment.id)" class="btn-reply-submit-inline" :disabled="!replyContent.trim()">등록</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>

    <!-- 신고 모달 -->
    <Transition name="fade">
      <div v-if="reportTarget" class="report-modal-overlay" @click="closeReportModal">
        <div class="report-modal-content" @click.stop>
          <div class="report-modal-header">
            <h3>신고하기</h3>
            <button class="btn-close-modal" @click="closeReportModal">✕</button>
          </div>
          <div class="report-modal-body">
            <p class="report-guide">깨끗한 커뮤니티를 위해 사유를 선택해 주세요.</p>
            <div class="report-options">
              <label v-for="reason in reportReasons" :key="reason" class="report-option">
                <input type="radio" v-model="selectedReportReason" :value="reason">
                <span>{{ reason }}</span>
              </label>
              <div class="report-option other">
                <input type="radio" v-model="selectedReportReason" value="기타">
                <span>기타</span>
                <input v-if="selectedReportReason === '기타'" type="text" v-model="customReportReason" placeholder="직접 입력" class="report-custom-input">
              </div>
            </div>
          </div>
          <div class="report-modal-footer">
            <button class="btn-report-cancel" @click="closeReportModal">취소</button>
            <button class="btn-report-submit" :disabled="!isReportValid" @click="submitReport">신고 제출</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade"><div v-if="selectedFullImg" class="image-modal-overlay" @click="selectedFullImg = null"><div class="modal-content" @click.stop><img :src="selectedFullImg" class="modal-img" /><button class="btn-close-img" @click="selectedFullImg = null">✕</button></div></div></Transition>
    <Transition name="fade"><button v-if="showTopBtn && !isMobile" class="btn-scroll-top" @click="scrollToTop"><span class="up-arrow">↑</span></button></Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, reactive, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import axios from '@/plugins/axios'
import PageHeader from '@/components/PageHeader.vue'
import CommunitySidebar from '@/components/CommunitySidebar.vue'
import AnimatedNickname from '@/components/AnimatedNickname.vue'
import { getCategoryLabel } from '@/constants/postConstants'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const post = ref(null)
const comments = ref([])
const newComment = ref('')
const loading = ref(true)
const showTopBtn = ref(false)
const selectedFullImg = ref(null)
const isMobile = ref(window.innerWidth <= 992)
const isMoreMenuOpen = ref(false)
const activeCommentMenuId = ref(null)

const sliderRef = ref(null)
const currentImgIdx = ref(0)
const visibleCount = ref(3)
const replyingToId = ref(null)
const replyContent = ref('')
const visibleRepliesMap = reactive({}) 
const highlightedCommentId = ref(null)
const editingCommentId = ref(null)
const editingContent = ref('')

const reportTarget = ref(null)
const selectedReportReason = ref('')
const customReportReason = ref('')
const reportReasons = ['부적절한 홍보', '스팸/낚시성', '욕설/비하 발언', '개인정보 노출', '음란물']

const activeMenu = ref(route.query.menu || 'NOTICE')
const activeSubCategory = ref(route.query.sub || 'ALL')

// [시니어 조치] 상세 뷰에서도 공통 마감 정책 반영 (연결된 공고가 있고, 해당 공고가 마감된 경우)
const isBoardClosed = computed(() => {
  // 게시글이 없거나 연결된 공고 ID가 없으면 마감 아님
  if (!post.value || !post.value.noticeId) return false

  // [시니어 조치] 깨우기에 성공한 소통방(isRevived가 true)이라면 마감 여부와 상관없이 무조건 '활성'
  if (post.value.isRevived === true) return false
  
  // 1. 공고 상태값 기준 판정
  const closedStatuses = ['CLOSED', 'EXPIRED_INFO', 'RESULT_COMPLETED']
  const isStatusClosed = post.value.noticeStatus && closedStatuses.includes(post.value.noticeStatus.toUpperCase())
  
  // 2. 마감일 기준 판정
  const isDeadlinePassed = post.value.deadline && new Date(post.value.deadline) < new Date()
  
  return isStatusClosed || isDeadlinePassed
})

const isPostBlinded = computed(() => post.value?.status === 'BLINDED')
const boardTypeLabel = computed(() => {
  if (!post.value?.type) return '커뮤니티'
  const labels = { 'GENERAL': '통합광장', 'NOTICE': '공고소통방', 'REVIEW': '당첨후기', 'PARTNER': '파트너스' }
  return labels[post.value.type] || '커뮤니티'
})

const isAuthor = computed(() => post.value && authStore.user?.id && Number(post.value.authorId) === Number(authStore.user.id))
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const canShare = computed(() => !!navigator.share)

const handleRestorePost = async () => {
  if (!await uiStore.showConfirm('이 게시글을 다시 활성화(ACTIVE) 처리하시겠습니까?', '게시글 복구')) return
  try {
    await axios.patch(`/api/admin/board/posts/${post.value.id}/status`, { status: 'ACTIVE' })
    await uiStore.showAlert('복구되었습니다.', '복구 완료')
    fetchPostDetail()
  } catch (e) { await uiStore.showAlert('복구 실패') }
}

const handleRestoreComment = async (commentId) => {
  if (!await uiStore.showConfirm('이 댓글을 다시 활성화(ACTIVE) 처리하시겠습니까?', '댓글 복구')) return
  try {
    await axios.patch(`/api/admin/board/comments/${commentId}/status`, { status: 'ACTIVE' })
    await uiStore.showAlert('댓글이 복구되었습니다.', '복구 완료')
    fetchComments(post.value.id)
  } catch (e) { await uiStore.showAlert('댓글 복구 실패') }
}

const isMyComment = (commentAuthorId) => {
  if (!authStore.user?.id || !commentAuthorId) return false;
  return Number(commentAuthorId) === Number(authStore.user.id);
}

const currentStatsDisplay = computed(() => {
  if (post.value?.noticeId) return `참여유저 ${post.value.interestCount || 0}`
  return `전체 ${post.value?.totalCount || 0} · 관심 ${post.value?.interestCount || 0}`
})

const totalCommentCount = computed(() => {
  const countRecursive = (list) => list.reduce((acc, cur) => acc + 1 + countRecursive(cur.children || []), 0)
  return countRecursive(comments.value)
})

const visibleComments = computed(() => comments.value.slice(0, visibleCount.value))
const hasMoreComments = computed(() => visibleCount.value < comments.value.length)

const getFlattenedReplies = (parentComment) => {
  const flattened = []
  const traverse = (children) => { if (!children) return; children.forEach(child => { flattened.push(child); if (child.children?.length > 0) traverse(child.children) }) }
  traverse(parentComment.children); return flattened.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
}

const getVisibleFlattenedReplies = (comment) => {
  const all = getFlattenedReplies(comment); const count = visibleRepliesMap[comment.id] || 3; return all.slice(0, count)
}

const hasMoreFlattenedReplies = (comment) => {
  const all = getFlattenedReplies(comment); const count = visibleRepliesMap[comment.id] || 3; return count < all.length
}

const loadMoreReplies = (commentId) => { visibleRepliesMap[commentId] = (visibleRepliesMap[commentId] || 3) + 3 }
const loadMore = () => { visibleCount.value += 3 }
const fetchPostDetail = async () => {
  const currentId = route.params.postId
  if (!currentId) return
  loading.value = true
  try {
    const res = await axios.get(`/api/posts/${currentId}`)
    post.value = res.data.data
    console.log("[DEBUG] 게시글 상세 데이터:", post.value);

    // [시니어 조치] 게시글 타입에 맞춰 사이드바 메뉴 자동 포커싱
    if (post.value.type) {
      activeMenu.value = post.value.type;

      // 서브 카테고리 매칭 로직
      if (post.value.type === 'NOTICE') {
        activeSubCategory.value = 'ACTIVE'; // 공고방 상세글은 보통 활성화된 방이므로
      } else if (post.value.category) {
        activeSubCategory.value = post.value.category; // 일반/후기/파트너는 카테고리값 사용
      } else {
        activeSubCategory.value = 'ALL';
      }
    }

    fetchComments(currentId)

    // [시니어 최적화] 데이터 로드 후 공지 바 위치로 자동 스크롤
    nextTick(() => {
      const noticeBar = document.querySelector('.system-notice-bar-soft');
      if (noticeBar) {
        const y = noticeBar.getBoundingClientRect().top + window.pageYOffset - 80;
        window.scrollTo({ top: y, behavior: 'smooth' });
      } else if (!isMobile.value) {
        window.scrollTo(0, 0);
      }
    });
  } catch (e) { 
    const msg = e.response?.data?.message || '게시글을 찾을 수 없거나 삭제된 게시글입니다.'
    await uiStore.showAlert(msg, '오류')
    router.push('/board') 
  } finally { loading.value = false }
}

const fetchComments = async (id) => {
  try {
    const isFromNoti = !!route.query.commentId;
    // 데이터는 넉넉히 가져오되
    const pageSize = isFromNoti ? 999 : 20;
    
    const res = await axios.get(`/api/posts/${id}/comments`, { params: { size: pageSize } })
    const rawData = res.data.data.content || res.data.data || []
    
    comments.value = rawData.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    
    // [시니어 조치] 무조건 기본값(접힘)으로 초기화하여 UI 통일성 유지
    visibleCount.value = 20; 
    comments.value.forEach(c => {
      visibleRepliesMap[c.id] = 3;
    });

    // [시니어 조치] 알림 이동 시 핀셋 스크롤 트리거
    if (isFromNoti) {
      nextTick(() => {
        setTimeout(() => {
          scrollToComment(route.query.commentId)
        }, 300)
      })
    }
  } catch (e) {
    console.error('댓글 로드 실패', e)
  }
}

const scrollToComments = () => {
  const el = document.getElementById('comment-section')
  if (el) {
    const y = el.getBoundingClientRect().top + window.pageYOffset - 80
    window.scrollTo({ top: y, behavior: 'smooth' })
  }
}

const toggleCommentMenu = (id) => {
  activeCommentMenuId.value = activeCommentMenuId.value === id ? null : id
}

const startEditComment = async (comment) => {
  if (isPostBlinded.value) {
    await uiStore.showAlert('관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.');
    return;
  }
  if (comment.status === 'BLINDED') {
    await uiStore.showAlert('관리자에 의해 차단된 댓글은 수정할 수 없습니다.');
    return;
  }
  editingCommentId.value = comment.id
  editingContent.value = comment.content
  activeCommentMenuId.value = null
}

const saveEditComment = async (id) => {
  if (!editingContent.value.trim()) return
  try {
    await axios.put(`/api/comments/${id}`, { content: editingContent.value })
    editingCommentId.value = null
    fetchComments(post.value.id)
  } catch (e) { 
    if (e.response?.status === 403 || e.response?.status === 500) {
      await uiStore.showAlert('관리자에 의해 차단된 댓글은 수정할 수 없습니다.');
    } else {
      await uiStore.showAlert('수정에 실패했습니다.');
    }
  }
}

const handleDeleteComment = async (id) => { 
  if (isPostBlinded.value) {
    await uiStore.showAlert('관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.');
    return;
  }
  const comment = comments.value.flatMap(c => [c, ...(c.children || [])]).find(c => c.id === id);
  if (comment && comment.status === 'BLINDED') {
    await uiStore.showAlert('관리자에 의해 차단된 댓글은 삭제할 수 없습니다.');
    return;
  }
  if (!await uiStore.showConfirm('댓글 삭제 시 획득한 경험치와 받은 신뢰 점수가 회수됩니다.\n정말 삭제하시겠습니까?', '댓글 삭제')) return; 
  try { 
    await axios.delete(`/api/comments/${id}`); 
    activeCommentMenuId.value = null;
    fetchComments(post.value.id) 
  } catch (e) {
    if (e.response?.status === 403 || e.response?.status === 500) {
      await uiStore.showAlert('관리자에 의해 차단된 댓글은 삭제할 수 없습니다.');
    }
  } 
}

const handleDeletePost = async () => { 
  if (post.value?.status === 'BLINDED') {
    await uiStore.showAlert('관리자에 의해 차단된 게시글은 삭제할 수 없습니다.');
    return;
  }
  if (!await uiStore.showConfirm('게시글 삭제 시 획득한 경험치(EXP)와 받은 좋아요에 따른 신뢰 점수(Karma)가 모두 회수됩니다.\n정말 삭제하시겠습니까?', '게시글 삭제')) return; 
  try { 
    await axios.delete(`/api/posts/${post.value.id}`); 
    
    // [시니어 조치] 삭제 후 이전 목록 맥락(쿼리 파라미터)을 그대로 들고 이동
    const noticeId = post.value?.noticeId;
    const path = noticeId ? `/board/${noticeId}` : '/board';
    
    router.push({ 
      path: path, 
      query: { ...route.query } 
    });
  } catch (e) {
    await uiStore.showAlert('게시글 삭제에 실패했습니다.')
  } 
}

const goToEdit = async () => {
  if (post.value?.status === 'BLINDED') {
    await uiStore.showAlert('관리자에 의해 차단된 게시글은 수정할 수 없습니다.');
    return;
  }
  // [시니어 조치] 라우터 정의(/board/post/edit/:postId)와 일치시키고 맥락 쿼리 보존
  router.push({
    path: `/board/post/edit/${post.value.id}`,
    query: { ...route.query }
  })
}
const toggleReplyInput = (id) => { if (replyingToId.value === id) { replyingToId.value = null; replyContent.value = '' } else { replyingToId.value = id; replyContent.value = '' } }
const toggleMoreMenu = () => { isMoreMenuOpen.value = !isMoreMenuOpen.value }
const copyUrl = async () => { 
  try { 
    await navigator.clipboard.writeText(window.location.href); 
    await uiStore.showAlert('URL이 클립보드에 복사되었습니다.', '알림'); 
    isMoreMenuOpen.value = false 
  } catch (e) {} 
}
const shareUrl = async () => { try { await navigator.share({ title: post.value.title, url: window.location.href }); isMoreMenuOpen.value = false } catch (e) {} }
const goToPrevPost = () => { 
  if (post.value?.prevId) {
    router.push({ path: `/board/post/${post.value.prevId}`, query: { ...route.query } })
  }
}
const goToNextPost = () => { 
  if (post.value?.nextId) {
    router.push({ path: `/board/post/${post.value.nextId}`, query: { ...route.query } })
  }
}

const openReportModal = async (type, id, status = 'ACTIVE') => { 
  // [시니어 조치] 탈퇴한 사용자인지 체크 (백엔드 authorActive 필드 활용)
  let isTargetActive = true;
  if (type === 'POST') {
    isTargetActive = post.value?.authorActive !== false;
  } else {
    const comment = comments.value.flatMap(c => [c, ...(c.children || [])]).find(c => c.id === id);
    isTargetActive = comment?.authorActive !== false;
  }

  if (!isTargetActive) {
    await uiStore.showAlert('탈퇴한 사용자는 신고할 수 없습니다.', '안내');
    activeCommentMenuId.value = null;
    isMoreMenuOpen.value = false;
    return;
  }

  // [시니어 조치] 게시글 자체가 이미 조치된 경우 모든 신고 차단
  const isParentPostManaged = post.value?.status === 'BLINDED' || post.value?.status === 'DELETED';
  
  if (status === 'BLINDED' || status === 'DELETED' || isParentPostManaged) {
    await uiStore.showAlert('이미 관리자에 의해 조치된 ' + (type === 'POST' ? '게시글' : '댓글') + '입니다.', '안내');
    activeCommentMenuId.value = null;
    isMoreMenuOpen.value = false;
    return;
  }
  reportTarget.value = { type, id }; 
  isMoreMenuOpen.value = false; 
  activeCommentMenuId.value = null 
}
const closeReportModal = () => { reportTarget.value = null; selectedReportReason.value = ''; customReportReason.value = '' }
const isReportValid = computed(() => { if (!selectedReportReason.value) return false; if (selectedReportReason.value === '기타' && !customReportReason.value.trim()) return false; return true })
const submitReport = async () => {
  const reason = selectedReportReason.value === '기타' ? customReportReason.value : selectedReportReason.value
  try {
    await axios.post('/api/reports', { targetType: reportTarget.value.type, targetId: reportTarget.value.id, reason: reason })
    await uiStore.showAlert('신고가 정상적으로 접수되었습니다.\n검토 후 조치하겠습니다.', '신고 완료'); 
    closeReportModal()
  } catch (e) { 
    await uiStore.showAlert(e.response?.data?.message || '신고 처리에 실패했습니다.', '오류') 
  }
}

// [시니어 조치] 같은 페이지 내에서 게시글 ID가 변경될 때 대응 (이전/다음글 이동)
watch(() => route.params.postId, (newId) => {
  if (newId) {
    fetchPostDetail();
  }
});

// [시니어 조치] URL 쿼리 파라미터 감시 (같은 페이지 내 이동 및 알림 클릭 대응)
watch(() => route.query, (newQuery) => {
  // 1. 특정 댓글/답글 포커싱 요청이 있는 경우
  if (newQuery.commentId) {
    console.log("[DEBUG] 알림 클릭 감지 (댓글):", newQuery.commentId);
    scrollToComment(newQuery.commentId);
  } 
  // 2. 게시글 자체(좋아요 등)에 대한 알림인 경우
  else if (newQuery.t && !newQuery.commentId) {
    console.log("[DEBUG] 알림 클릭 감지 (게시글 본문)");
    const noticeBar = document.querySelector('.system-notice-bar-soft');
    if (noticeBar) {
      const y = noticeBar.getBoundingClientRect().top + window.pageYOffset - 80;
      window.scrollTo({ top: y, behavior: 'smooth' });
    }
  }
}, { deep: true });

// [시니어 조치] 특정 댓글로 스크롤 및 하이라이트 효과 (핀셋 확장 로직)
const scrollToComment = (targetId) => {
  if (!targetId) return
  
  // 1. [핀셋 확장] 대상이 어디에 있는지 찾아서 필요한 만큼만 열기
  const surgicallyExpand = () => {
    // A. 답글인 경우 부모 찾기
    for (const c of comments.value) {
      if (c.children && c.children.some(r => String(r.id) === String(targetId))) {
        visibleRepliesMap[c.id] = 999; // 해당 부모의 답글만 다 열기
        
        // 부모 자체가 20번째 이후라면 부모가 보이게 visibleCount 조절
        const parentIdx = comments.value.indexOf(c);
        if (parentIdx >= visibleCount.value) {
          visibleCount.value = parentIdx + 1;
        }
        return;
      }
    }
    
    // B. 일반 댓글인 경우
    const targetIdx = comments.value.findIndex(c => String(c.id) === String(targetId));
    if (targetIdx !== -1 && targetIdx >= visibleCount.value) {
      visibleCount.value = targetIdx + 1; // 해당 댓글이 보일 만큼만 확장
    }
  }
  
  surgicallyExpand();

  // 2. 재시도 메커니즘 (DOM 렌더링 추적)
  let attempts = 0;
  const performScroll = () => {
    const el = document.getElementById(`comment-${targetId}`);
    if (el) {
      const rect = el.getBoundingClientRect();
      window.scrollTo({ top: rect.top + window.pageYOffset - 150, behavior: 'smooth' });
      highlightedCommentId.value = Number(targetId);
      el.classList.add('highlight-flash');
      setTimeout(() => { highlightedCommentId.value = null; el.classList.remove('highlight-flash'); }, 3500);
    } else if (attempts < 15) {
      attempts++;
      surgicallyExpand(); // 루프 안에서도 계속 확장 시도
      setTimeout(performScroll, 200);
    }
  };

  nextTick(performScroll);
}

const handleSliderScroll = () => { if (sliderRef.value) currentImgIdx.value = Math.round(sliderRef.value.scrollLeft / sliderRef.value.clientWidth) }
const scrollSlider = (direction) => {
  if (!sliderRef.value || !post.value?.imageUrls) return
  const total = post.value.imageUrls.length
  goToSlide((currentImgIdx.value + direction + total) % total)
}
const goToSlide = (idx) => { if (sliderRef.value) sliderRef.value.scrollTo({ left: sliderRef.value.clientWidth * idx, behavior: 'smooth' }) }
const handleKeyDown = (e) => { if (selectedFullImg.value) { if (e.key === 'Escape') selectedFullImg.value = null } else { if (e.key === 'ArrowLeft') scrollSlider(-1); if (e.key === 'ArrowRight') scrollSlider(1) } }
const handleResize = () => { isMobile.value = window.innerWidth <= 992 }
const handleToggleLike = async () => { 
  if (post.value && post.value.authorActive === false) {
    await uiStore.showAlert('탈퇴한 사용자의 게시글에는 좋아요를 누를 수 없습니다.', '안내')
    return
  }
  try { const res = await axios.post(`/api/posts/${post.value.id}/like`); const isLiked = res.data.data; post.value.liked = isLiked; post.value.likeCount += isLiked ? 1 : -1 } catch (e) {} 
}

const handleToggleCommentLike = async (comment) => {
  if (!authStore.isAuthenticated) { await uiStore.showAlert('로그인이 필요한 서비스입니다.', '안내'); return }
  if (comment.authorActive === false) {
    await uiStore.showAlert('탈퇴한 사용자의 댓글에는 좋아요를 누를 수 없습니다.', '안내')
    return
  }
  if (isMyComment(comment.authorId)) { await uiStore.showAlert('본인의 댓글에는 좋아요를 누를 수 없습니다.', '알림'); return }
  try {
    await axios.post(`/api/comments/${comment.id}/like`)
    comment.liked = !comment.liked
    comment.likeCount += comment.liked ? 1 : -1
  } catch (e) {
    if (e.response?.data?.message) await uiStore.showAlert(e.response.data.message, '오류')
  }
}

const handleCreateComment = async (parentId = null, rootParentId = null) => {
  if (isPostBlinded.value) {
    await uiStore.showAlert('관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.', '안내');
    return;
  }
  const content = parentId ? replyContent.value : newComment.value
  if (!content.trim()) return
  try {
    const res = await axios.post(`/api/posts/${post.value.id}/comments`, { content, parentId })
    const createdCommentId = res.data.data.id // 새로 생성된 댓글 ID
    
    if (parentId) { 
      replyContent.value = ''; 
      replyingToId.value = null; 
      if (rootParentId) visibleRepliesMap[rootParentId] = (visibleRepliesMap[rootParentId] || 3) + 1; 
      else visibleRepliesMap[parentId] = (visibleRepliesMap[parentId] || 3) + 1 
    }
    else { 
      newComment.value = ''; 
      // [시니어 조치] 새 댓글이 보일 수 있도록 visibleCount를 현재 댓글 수 + 1 로 조정
      visibleCount.value = comments.value.length + 1
    }
    
    await fetchComments(post.value.id)
    
    // [시니어 조치] 작성한 댓글 위치로 자동 스크롤
    nextTick(() => {
      scrollToComment(createdCommentId)
    })
  } catch (e) {
    const errorMsg = e.response?.data?.message || '댓글 등록에 실패했습니다.'
    await uiStore.showAlert(errorMsg, '오류')
  }
}
const handleGoBack = () => {
  const noticeId = post.value?.noticeId
  
  // [시니어 조치] URL에 담겨온 모든 맥락(menu, sub, 필터 등)을 그대로 목록으로 복사
  const query = { 
    ...route.query, 
    lastPostId: post.value?.id 
  }
  
  const path = noticeId ? `/board/${noticeId}` : '/board'
  router.push({ path, query })
}

// 브라우저 뒤로가기 포함, 페이지 이탈 시 현재 게시글 ID 저장 (BoardView에서 스크롤 복원에 사용)
onBeforeRouteLeave((to) => {
  const isBoardRoute = to.path === '/board' || (to.path.startsWith('/board/') && !to.path.startsWith('/board/post'))
  if (isBoardRoute && post.value?.id && !to.query.lastPostId) {
    sessionStorage.setItem('lastViewedPostId', post.value.id)
  }
})

const openImageModal = (url) => { selectedFullImg.value = getFullImageUrl(url) }
const getFullImageUrl = (url) => url ? (url.startsWith('http') ? url : (url.startsWith('/') ? url : `/${url}`)) : ''
const formatFullDate = (s) => { if (!s) return ''; const d = new Date(s); const pad = (n) => n < 10 ? '0' + n : n; return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}` }
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }
const handleScroll = () => { showTopBtn.value = window.pageYOffset > 400 }
const postTypeIcon = computed(() => { if (!post.value) return '💬'; const icons = { GENERAL: '🏛️', NOTICE: '💬', REVIEW: '✨', PARTNER: '🤝' }; return icons[post.value.type] || '💬' })
const vClickOutside = { mounted(el, binding) { el.clickOutsideEvent = (e) => { if (!(el === e.target || el.contains(e.target))) binding.value() }; document.addEventListener('click', el.clickOutsideEvent) }, unmounted(el) { document.removeEventListener('click', el.clickOutsideEvent) } }

onMounted(() => { fetchPostDetail(); window.addEventListener('scroll', handleScroll); window.addEventListener('keydown', handleKeyDown); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('scroll', handleScroll); window.removeEventListener('keydown', handleKeyDown); window.removeEventListener('resize', handleResize) })
watch(() => route.params.postId, fetchPostDetail)
</script>

<style scoped>
.board-view-container { max-width: 1200px; margin: 0 auto; padding-bottom: 80px; }
.board-layout { display: flex; gap: 30px; padding: 0 20px; margin-top: 30px; }
.board-main-content { flex: 1; min-width: 0; }
.system-notice-bar-soft { background: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 20px; margin-bottom: 20px; }
.system-msg { font-size: 0.85rem; color: var(--text-secondary); margin: 0; line-height: 1.5; }
.system-msg b { color: var(--link-color); }
.post-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 20px; padding: 40px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); margin-bottom: 30px; position: relative; }
.card-top-control-bar-compact { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-bottom: 25px; }
.nav-action-group { display: flex; align-items: center; gap: 8px; }
.btn-nav-mini { background: var(--card-bg); border: 1px solid var(--border-color); color: var(--text-secondary); height: 32px; padding: 0 10px; border-radius: 6px; font-size: 0.8rem; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.btn-nav-mini.text-btn { padding: 0 12px; }
.btn-nav-mini:hover:not(:disabled) { border-color: var(--link-color); color: var(--link-color); }
.btn-nav-mini:disabled { opacity: 0.3; cursor: not-allowed; }
.nav-icon-mini-thin { font-size: 1rem; font-weight: 300; }
.more-menu-container { position: relative; }
.btn-more-icon-mini { background: none; border: none; font-size: 1.2rem; color: var(--text-muted); cursor: pointer; padding: 0 6px; font-weight: bold; }
.more-dropdown-compact { position: absolute; top: 100%; right: 0; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 6px 0; box-shadow: 0 10px 30px rgba(0,0,0,0.15); z-index: 100; min-width: 140px; }
.dropdown-item-mini { width: 100%; padding: 10px 16px; text-align: left; background: none; border: none; font-size: 0.85rem; font-weight: 600; color: var(--text-primary); cursor: pointer; }
.dropdown-item-mini:hover { background: var(--hover-bg); color: var(--link-color); }
.dropdown-item-mini.text-danger { color: #ed4956; }
.dropdown-divider { height: 1px; background: var(--divider-color); margin: 4px 0; }
.post-header { margin-bottom: 30px; }
.post-title { font-size: 1.8rem; font-weight: 800; margin: 0 0 25px; line-height: 1.4; color: var(--text-primary); letter-spacing: -0.5px; padding-right: 40px; display: flex; align-items: flex-start; flex-wrap: wrap; gap: 12px; }
.post-category-label-detail { 
  font-size: 0.95rem; font-weight: 800; color: var(--text-primary); 
  background: var(--hover-bg); padding: 5px 15px; border-radius: 20px; 
  border: 1px solid var(--border-color); white-space: nowrap; margin-top: 2px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03); display: flex; align-items: center;
}
.post-meta-info { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid var(--divider-color); }
.author-block { display: flex; align-items: center; gap: 12px; }
.author-avatar { width: 36px; height: 36px; background: var(--hover-bg); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1rem; font-weight: 800; color: var(--text-secondary); border: 1px solid var(--border-color); }
.author-name { font-weight: 700; font-size: 1rem; color: var(--text-primary); display: flex; align-items: center; gap: 8px; }

/* [시니어 조치] 상세페이지 작성자 배지 스타일 */
.author-badge-tag {
  cursor: help;
}
.post-info-right { color: var(--text-muted); font-size: 0.8rem; font-weight: 500; }
.post-image-section { margin: 30px -40px; background: #000; position: relative; }
.slider-container { position: relative; overflow: hidden; }
.slider-wrapper { display: flex; overflow-x: auto; scroll-snap-type: x mandatory; scrollbar-width: none; }
.slider-wrapper::-webkit-scrollbar { display: none; }
.slider-item { flex: 0 0 100%; scroll-snap-align: start; display: flex; justify-content: center; }
.slider-item img { max-width: 100%; max-height: 650px; object-fit: contain; cursor: zoom-in; }
.slider-nav { position: absolute; top: 50%; transform: translateY(-50%); width: 44px; height: 44px; border-radius: 50%; background: rgba(255,255,255,0.15); border: none; color: white; cursor: pointer; z-index: 10; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); transition: background 0.2s; }
.slider-nav:hover { background: rgba(255,255,255,0.3); }
.slider-nav.prev { left: 20px; }
.slider-nav.next { right: 20px; }
.slider-pagination { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); display: flex; gap: 8px; z-index: 10; }
.p-dot { width: 7px; height: 7px; border-radius: 50%; background: rgba(255,255,255,0.4); cursor: pointer; transition: all 0.3s; }
.p-dot.active { width: 20px; border-radius: 4px; background: #fff; }
.post-content-area { font-size: 1.05rem; line-height: 1.85; color: var(--text-primary); white-space: pre-wrap; word-break: break-word; padding: 10px 0 20px; }
.post-tags-footer { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 30px; padding-bottom: 10px; }
.footer-tag { font-size: 0.85rem; font-weight: 700; color: var(--link-color); cursor: pointer; }
.post-footer-bar { display: flex; justify-content: space-between; align-items: center; padding-top: 25px; border-top: 1px solid var(--divider-color); }
.footer-left { display: flex; align-items: center; gap: 20px; }
.btn-interaction { display: flex; align-items: center; gap: 8px; background: none; border: none; padding: 0; color: var(--text-primary); cursor: pointer; height: 24px; }
.btn-interaction-no-click { display: flex; align-items: center; gap: 8px; color: var(--text-primary); cursor: default; height: 24px; }
.i-icon { font-size: 1.3rem; display: flex; align-items: center; justify-content: center; height: 100%; }
.white-eye-large { color: var(--text-primary); opacity: 0.9; display: flex; align-items: center; }
.i-count { font-size: 0.95rem; font-weight: 700; line-height: 1; display: flex; align-items: center; }
.btn-interaction.like.active .i-count { color: #ed4956; }
.comment-section { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 20px; padding: 40px; }
.section-title { font-size: 1.1rem; font-weight: 800; margin-bottom: 25px; }
.section-title span { color: var(--link-color); margin-left: 4px; }
.comment-input-box { background: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 15px; margin-bottom: 40px; }
.comment-input-box.main-input:focus-within { border-color: var(--link-color); box-shadow: 0 0 0 3px rgba(0,149,246,0.1); }
.comment-input-box.closed-notice { background-color: var(--hover-bg); border-style: dashed; padding: 25px; display: flex; align-items: center; justify-content: center; }
.closed-msg { margin: 0; font-size: 0.95rem; font-weight: 700; color: var(--text-secondary); text-align: center; }
.comment-textarea { width: 100%; height: 60px; border: none; background: transparent; resize: none; outline: none; font-size: 0.95rem; color: var(--text-primary); }
.input-footer { display: flex; justify-content: flex-end; margin-top: 10px; border-top: 1px solid var(--divider-color); padding-top: 10px; }
.btn-comment-submit { background: var(--link-color); color: white; border: none; padding: 6px 16px; border-radius: 6px; font-weight: 800; font-size: 0.85rem; cursor: pointer; }
.comment-list-container { display: flex; flex-direction: column; gap: 10px; }
.comment-group { border-bottom: 1px solid var(--divider-color); padding: 20px 0; }
.comment-group:last-child { border-bottom: none; }
.comment-item { display: flex; gap: 12px; transition: background-color 0.5s; padding: 8px; border-radius: 10px; }
.comment-item.main-item { padding-right: 27px; } 
.comment-item.highlight,
.reply-item-modern.highlight { 
  background-color: rgba(0, 149, 246, 0.15) !important; 
  animation: flash 2s ease-out; 
}

@keyframes flash {
  0% { background-color: rgba(0, 149, 246, 0.3); }
  70% { background-color: rgba(0, 149, 246, 0.1); }
  100% { background-color: transparent; }
}

.c-body { flex: 1; min-width: 0; }
.c-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.c-username { font-weight: 800; font-size: 0.95rem; color: var(--text-primary); display: flex; align-items: center; gap: 6px; }

/* [시니어 조치] 댓글 작성자 배지 스타일 */
.author-badge-mini {
  cursor: help;
  transform: scale(0.9);
  margin-left: -2px;
}
.c-more-container { position: relative; }
.btn-c-more { background: none; border: none; font-size: 1rem; color: var(--text-muted); cursor: pointer; padding: 0 4px; font-weight: bold; }
.c-dropdown-compact { position: absolute; top: 100%; right: 0; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 8px; padding: 4px 0; box-shadow: 0 5px 15px rgba(0,0,0,0.1); z-index: 100; min-width: 100px; }
.c-text { font-size: 1rem; line-height: 1.6; color: var(--text-primary); margin: 0 0 10px; white-space: pre-wrap; }
.c-text.is-blinded { 
  color: var(--text-muted); font-style: italic; font-size: 0.92rem; 
  background: rgba(237, 73, 86, 0.04); 
  padding: 10px 15px; border-radius: 10px; 
  border: 1px dashed rgba(237, 73, 86, 0.3);
  backdrop-filter: blur(2px);
  margin: 5px 0 12px;
  display: block;
}
.c-text.is-deleted { 
  color: var(--text-muted); font-style: italic; font-size: 0.92rem; 
  background: rgba(142, 68, 173, 0.04); 
  padding: 10px 15px; border-radius: 10px; 
  border: 1px dashed rgba(142, 68, 173, 0.3);
  backdrop-filter: blur(2px);
  margin: 5px 0 12px;
  display: block;
}

.dropdown-notice-mini { padding: 8px 12px; font-size: 0.75rem; color: var(--text-muted); font-weight: 700; text-align: center; font-style: italic; }
.mention-tag { display: inline-block; color: var(--link-color); font-weight: 800; font-size: 0.85rem; background: rgba(0, 149, 246, 0.1); padding: 1px 8px; border-radius: 4px; margin-right: 4px; transition: all 0.2s; cursor: pointer; border: 1px solid transparent; }
.mention-tag:hover { background: var(--link-color); color: white; transform: translateY(-1px); }
.c-edit-box { margin-bottom: 10px; background: var(--hover-bg); border-radius: 8px; padding: 10px; border: 1px solid var(--border-color); }
.c-edit-textarea { width: 100%; height: 60px; border: none; background: transparent; resize: none; outline: none; font-size: 0.95rem; color: var(--text-primary); }
.c-edit-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.btn-save-mini, .btn-cancel-mini { padding: 4px 10px; border-radius: 4px; font-size: 0.75rem; font-weight: 700; cursor: pointer; border: none; }
.btn-save-mini { background: var(--link-color); color: white; }
.btn-cancel-mini { background: var(--border-color); color: var(--text-secondary); }
.c-meta-footer { display: flex; align-items: center; gap: 12px; }
.c-date-small { font-size: 0.75rem; color: var(--text-muted); font-weight: 500; }
.btn-reply-action { background: none; border: none; font-size: 0.75rem; font-weight: 800; color: var(--text-secondary); cursor: pointer; padding: 0; transition: color 0.2s; }
.btn-reply-action:hover { color: var(--link-color); }

/* [시니어] 댓글 좋아요 버튼 스타일 */
.btn-c-like { 
  display: flex; align-items: center; gap: 4px; background: none; border: none; 
  cursor: pointer; padding: 0; margin-left: 8px; transition: transform 0.2s; 
}
.btn-c-like:hover { transform: scale(1.1); }
.btn-c-like.mini { margin-left: 10px; }
.btn-c-like .l-icon { font-size: 0.9rem; }
.btn-c-like .l-count { font-size: 0.75rem; font-weight: 800; color: var(--text-secondary); }
.btn-c-like.is-liked .l-count { color: #ed4956; }

.replies-area-modern { margin-top: 15px; margin-left: 20px; padding: 15px; background-color: var(--bg-primary); border-radius: 12px; border-left: 2px solid var(--divider-color); display: flex; flex-direction: column; gap: 15px; }
.reply-item-modern { display: flex; gap: 10px; padding: 12px; border-radius: 10px; background-color: var(--card-bg); border: 1px solid var(--border-color); box-shadow: 0 2px 6px rgba(0,0,0,0.02); transition: background-color 0.5s; }
.c-avatar.mini { width: 28px; height: 28px; font-size: 0.75rem; }
.reply-input-box-inline { margin-top: 15px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px; }
.reply-input-box-inline.inner { background: var(--hover-bg); border-color: var(--divider-color); }
.reply-textarea-inline { width: 100%; height: 50px; border: none; background: transparent; resize: none; outline: none; font-size: 0.9rem; color: var(--text-primary); }
.reply-footer-inline { display: flex; justify-content: flex-end; margin-top: 8px; }
.btn-reply-submit-inline { background: var(--link-color); color: white; border: none; padding: 4px 12px; border-radius: 4px; font-weight: 700; font-size: 0.8rem; cursor: pointer; }
.load-more-box, .load-more-replies { text-align: center; padding: 10px 0; }
.btn-load-more { background: var(--hover-bg); border: 1px solid var(--border-color); color: var(--text-secondary); padding: 10px 30px; border-radius: 30px; font-size: 0.85rem; font-weight: 800; cursor: pointer; transition: all 0.2s; }
.btn-load-more:hover { border-color: var(--link-color); color: var(--link-color); }
.btn-text-load-more { background: none; border: none; color: var(--text-secondary); font-size: 0.8rem; font-weight: 800; cursor: pointer; text-decoration: underline; }

/* 신고 모달 스타일 고도화 (컴팩트 & 테마 호환) */
.report-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); z-index: 3000; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); }
.report-modal-content { background: var(--card-bg); width: 90%; max-width: 380px; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); overflow: hidden; }
.report-modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--divider-color); }
.report-modal-header h3 { margin: 0; font-size: 1.05rem; font-weight: 800; color: var(--text-primary); }
.btn-close-modal { background: none; border: none; font-size: 1.2rem; color: var(--text-muted); cursor: pointer; }
.report-modal-body { padding: 16px 20px; }
.report-guide { font-size: 0.8rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 15px; }
.report-options { display: flex; flex-direction: column; gap: 8px; }
.report-option { display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 0.9rem; font-weight: 600; padding: 8px; border-radius: 8px; transition: background 0.2s; color: var(--text-primary); }
.report-option:hover { background: var(--hover-bg); }
.report-option input[type="radio"] { width: 16px; height: 16px; accent-color: var(--link-color); }
.report-option.other { flex-direction: row; align-items: center; flex-wrap: wrap; }
.report-custom-input { flex: 1; min-width: 150px; padding: 8px; border: 1px solid var(--border-color); border-radius: 6px; background: var(--hover-bg); color: var(--text-primary); font-size: 0.85rem; margin-top: 0; margin-left: 8px; }
.report-modal-footer { display: flex; gap: 10px; padding: 16px 20px; background: var(--hover-bg); }
.btn-report-cancel { flex: 1; padding: 10px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--card-bg); color: var(--text-primary); font-weight: 700; cursor: pointer; }
.btn-report-submit { flex: 2; padding: 10px; border-radius: 8px; border: none; background: var(--link-color); color: white; font-weight: 800; cursor: pointer; }
.btn-report-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.image-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.95); z-index: 2000; display: flex; align-items: center; justify-content: center; }
.modal-content { position: relative; max-width: 95%; max-height: 95%; }
.modal-img { max-width: 100%; max-height: 100vh; object-fit: contain; }
.btn-close-img { position: absolute; top: -40px; right: -10px; background: none; border: none; color: white; font-size: 2rem; cursor: pointer; }
.btn-scroll-top { position: fixed; bottom: 50px; left: calc(50% + 480px); width: 45px; height: 45px; border-radius: 50%; background: var(--card-bg); border: 1px solid var(--border-color); box-shadow: 0 4px 12px rgba(0,0,0,0.1); cursor: pointer; z-index: 100; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.btn-scroll-top:hover { transform: translateY(-5px); border-color: var(--link-color); }
.up-arrow { font-size: 1.2rem; color: var(--link-color); font-weight: bold; }

@media (max-width: 992px) {
  .board-layout { flex-direction: column; gap: 0; }
  .post-card { padding: 20px 14px; border-radius: 0; border-left: none; border-right: none; }
  .post-title { font-size: 1rem; padding-right: 0; margin-bottom: 12px; gap: 8px; }
  .post-category-label-detail { font-size: 0.75rem; padding: 2px 8px; border-radius: 5px; }
  .card-top-control-bar-compact { margin-bottom: 12px; }
  .btn-nav-mini { height: 28px; padding: 0 8px; font-size: 0.75rem; }
  .btn-more-icon-mini { font-size: 0.9rem; }
  .dropdown-item-mini { padding: 6px 10px; font-size: 0.75rem; }
  .post-image-section { margin: 15px -14px; }
  .comment-section { padding: 25px 14px; border-radius: 0; border-left: none; border-right: none; }
  .section-title { font-size: 0.95rem; margin-bottom: 15px; }
  .replies-area-modern { margin-left: 8px; padding: 10px; gap: 10px; }
  .btn-scroll-top { display: none; }
  .btn-reply-action { font-size: 0.65rem; }
  .btn-interaction .i-icon { font-size: 1.1rem; }
  .btn-interaction .i-count { font-size: 0.85rem; }
  .author-avatar { width: 30px; height: 30px; font-size: 0.85rem; }
  .author-name { font-size: 0.9rem; }
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.pop-enter-active, .pop-leave-active { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); transform-origin: top right; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: scale(0.9) translateY(-10px); }

/* [시니어] 관리자 전용 액션 툴바 스타일 */
.admin-top-action-bar {
  display: flex; justify-content: space-between; align-items: center;
  background: #2d3436; color: white; padding: 12px 20px; border-radius: 12px;
  margin-bottom: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}
.admin-status-info { display: flex; align-items: center; gap: 12px; }
.a-label { font-size: 0.85rem; font-weight: 800; color: #fab1a0; }
.a-status { font-size: 0.75rem; font-weight: 900; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; }
.a-status.ACTIVE { background: #00b894; }
.a-status.BLINDED { background: #d63031; }
.a-status.DELETED { background: #8e44ad; }

.admin-btns { display: flex; gap: 10px; }
.btn-admin-restore, .btn-admin-nav { 
  border: none; border-radius: 6px; padding: 6px 14px; font-size: 0.8rem; 
  font-weight: 800; cursor: pointer; transition: all 0.2s;
}
.btn-admin-restore { background: #0984e3; color: white; }
.btn-admin-restore:hover { background: #74b9ff; transform: translateY(-2px); }
.btn-admin-nav { background: #636e72; color: white; }
.btn-admin-nav:hover { background: #b2bec3; }

/* [시니어] 운영자 전용 댓글 관리 버튼 스타일 */
.admin-comment-controls { display: inline-flex; align-items: center; gap: 10px; margin-left: auto; padding-right: 10px; }
.btn-admin-c-restore { background: none; border: 1.5px solid #00b894; color: #00b894; padding: 2px 10px; border-radius: 6px; font-size: 0.72rem; font-weight: 800; cursor: pointer; transition: all 0.2s; }
.btn-admin-c-restore:hover { background: #00b894; color: white; transform: translateY(-1px); }
.btn-admin-c-nav { background: var(--hover-bg); border: 1px solid var(--border-color); color: var(--text-secondary); padding: 3px 10px; border-radius: 6px; font-size: 0.72rem; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.btn-admin-c-nav:hover { background: var(--link-color); color: white; border-color: var(--link-color); }

@media (max-width: 768px) {
  .admin-top-action-bar { flex-direction: column; gap: 12px; padding: 15px; text-align: center; }
  .admin-status-info { flex-direction: column; gap: 6px; }
  .admin-btns { width: 100%; }
  .btn-admin-restore, .btn-admin-nav { flex: 1; font-size: 0.75rem; }
  .admin-comment-controls { margin-left: 0; padding-right: 0; margin-top: 4px; width: 100%; justify-content: flex-end; }
}

/* [시니어] 작성자 전용 블라인드 안내 배너 스타일 */
.post-card.is-blinded-author { border: 2px solid #ed4956; }
.blinded-author-banner { 
  display: flex; gap: 15px; background: rgba(237, 73, 86, 0.05); 
  border: 1px solid rgba(237, 73, 86, 0.2); border-radius: 12px; 
  padding: 15px 20px; margin-bottom: 25px; align-items: flex-start;
}
.blinded-author-banner .b-icon { font-size: 1.5rem; }
.blinded-author-banner .b-text { flex: 1; }
.blinded-author-banner strong { font-size: 0.95rem; color: #ed4956; display: block; margin-bottom: 4px; }
.blinded-author-banner p { font-size: 0.85rem; color: var(--text-secondary); margin: 0; line-height: 1.5; }

@media (max-width: 768px) {
  .blinded-author-banner { padding: 12px 15px; gap: 10px; }
  .blinded-author-banner .b-icon { font-size: 1.2rem; }
  .blinded-author-banner strong { font-size: 0.85rem; }
  .blinded-author-banner p { font-size: 0.75rem; }
}

/* [시니어 조치] 특정 댓글 강조 애니메이션 */
@keyframes highlight-fade {
  0% { background-color: rgba(52, 152, 219, 0.2); box-shadow: 0 0 20px rgba(52, 152, 219, 0.3); }
  100% { background-color: transparent; box-shadow: none; }
}
.highlight-flash { 
  animation: highlight-fade 3s ease-out !important; 
  border-radius: 12px;
  position: relative;
  z-index: 10;
}
</style>
