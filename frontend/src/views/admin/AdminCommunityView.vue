<template>
  <div class="admin-community-container">
    <!-- 1. 헤더 영역 -->
    <div class="admin-header">
      <div class="header-main">
        <h2>💬 커뮤니티 관리</h2>
        <span class="count-info pc-only">{{ currentCountInfo }}</span>
      </div>
    </div>

    <!-- 2. 검색 및 필터 바 -->
    <div class="filter-bar">
      <form class="search-wrapper" @submit.prevent="handleFilterChange">
        <span class="search-icon">🔍</span>
        <input 
          v-model="currentFilter.keyword" 
          type="search"
          enterkeyhint="search"
          :placeholder="getPlaceholder"
          class="admin-search-input"
          @keydown.enter="handleFilterChange"
        />
        <button type="submit" class="mobile-search-btn">검색</button>
      </form>
      <div class="filter-group">
        <select v-if="currentTab === 'reports'" v-model="reportTargetFilter" class="admin-select" @change="handleFilterChange">
          <option value="ALL">모든 신고대상</option>
          <option value="POST">게시글 신고</option>
          <option value="COMMENT">댓글 신고</option>
          <option value="MESSAGE">쪽지 신고 (쌍방감지)</option>
        </select>

        <select v-model="currentFilter.status" class="admin-select" @change="handleFilterChange">
          <template v-if="currentTab === 'reports'">
            <option value="ALL">모든 처리상태</option>
            <option value="WAITING">처리 대기</option>
            <option value="BLIND_COMPLETE">블라인드 완료</option>
            <option value="DELETE_COMPLETE">영구 삭제 완료</option>
            <option value="REJECTED">반려됨</option>
          </template>
          <template v-else>
            <option value="ALL">모든 상태</option>
            <option value="ACTIVE">정상 (ACTIVE)</option>
            <option value="BLINDED">숨김 (BLINDED)</option>
            <option value="DELETED">관리자 삭제 (DELETED)</option>
            <option value="USER_DELETED">사용자 삭제 (USER_DELETED)</option>
            
          </template>
        </select>

        <button v-if="currentFilter.userId" class="btn-clear-user" @click="clearUserFilter">
          👤 유저 해제 (ID: {{ currentFilter.userId }})
        </button>

        <button v-if="currentTab === 'posts' && postsState.postId" class="btn-clear-user btn-id-filter" @click="clearIdFilter">
          📄 글번호 해제 (#{{ postsState.postId }})
        </button>

        <button v-if="currentTab === 'comments' && commentsState.commentId" class="btn-clear-user btn-id-filter" @click="clearIdFilter">
          💬 댓글번호 해제 (#{{ commentsState.commentId }})
        </button>
      </div>
    </div>

    <!-- 3. 상단 탭 및 액션 바 -->
    <div class="tab-header">
      <div class="tab-nav">
        <button class="tab-btn" :class="{ active: currentTab === 'posts' }" @click="currentTab = 'posts'">게시글 관리</button>
        <button class="tab-btn" :class="{ active: currentTab === 'comments' }" @click="currentTab = 'comments'">댓글 관리</button>
        <button class="tab-btn" :class="{ active: currentTab === 'reports' }" @click="currentTab = 'reports'">신고 관리</button>
      </div>
      
      <!-- 신고 관리 전용 액션 -->
      <div v-if="currentTab === 'reports'" class="bulk-actions">
        <div v-if="selectedIds.length === 0" class="bulk-actions-group">
          <button class="btn-select-all" @click="toggleSelectAll">체크 전체선택</button>
        </div>
        <div v-else class="bulk-actions-group">
          <button class="btn-bulk-wait" @click="handleBulkReport('WAITING')">{{ selectedIds.length }}건 복구</button>
          <button class="btn-bulk-reject" @click="handleBulkReport('REJECTED')">{{ selectedIds.length }}건 반려</button>
          <button class="btn-bulk-blind" @click="handleBulkReport('BLIND_COMPLETE')">{{ selectedIds.length }}건 블라인드</button>
          <button class="btn-bulk-delete" @click="handleBulkReport('DELETE_COMPLETE')">{{ selectedIds.length }}건 영구 삭제</button>
          <button class="btn-select-all" @click="toggleSelectAll">전체 해제</button>
        </div>
      </div>
    </div>

    <!-- 4. 리스트 영역 -->
    <div class="content-body scrollable">
      <!-- 게시글 리스트 -->
      <div v-if="currentTab === 'posts'" class="list-wrapper">
        <div v-for="post in postsState.list" :key="post.id" class="content-card" 
        :class="{ 'is-blinded': post.status === 'BLINDED', 'is-deleted': post.status === 'DELETED', 'is-user-deleted': post.status === 'USER_DELETED' }">
          <div class="card-header">
            <span class="id-tag">#{{ post.id }}</span>
            <span class="type-badge" :class="post.type">{{ getPostTypeLabel(post.type) }}</span>
            <span class="status-badge" :class="post.status">{{ getStatusLabel(post.status) }}</span>
            <span class="date-text">{{ formatDate(post.createdAt) }}</span>
          </div>
          <div class="card-main">
            <h3 class="post-title" @click="viewPostDetail(post.id)">{{ post.title }}</h3>
            <p class="post-author">작성자: <span class="author-link" @click="setUserFilter(post.authorId)">{{ post.authorName }}</span></p>
            <div class="post-stats"><span>👀 {{ post.viewCount }}</span><span>💬 {{ post.commentCount }}</span><span>👍 {{ post.likeCount }}</span></div>
          </div>
          <div class="card-actions">
            <button @click="openEditModal(post)" class="btn-action edit">수정</button>
            <template v-if="isItemActive(post)">
              <button @click="toggleStatus(post)" class="btn-action blind">블라인드</button>
              <button @click="handlePermanentDelete(post)" class="btn-action delete">영구 삭제</button>
            </template>
            <template v-else><button @click="toggleStatus(post)" class="btn-action restore">복구</button></template>
          </div>
        </div>
      </div>

      <!-- 댓글 리스트 -->
      <div v-else-if="currentTab === 'comments'" class="list-wrapper">
        <div v-for="comment in commentsState.list" :key="comment.id" class="content-card comment-card" :class="{ 'is-blinded': comment.status === 'BLINDED', 
        'is-deleted': comment.status === 'DELETED', 'is-user-deleted': comment.status === 'USER_DELETED' }">
          <div class="card-header">
            <span class="id-tag">#{{ comment.id }}</span>
            <span class="status-badge" :class="comment.status">{{ getStatusLabel(comment.status) }}</span>
            <span class="date-text">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <div class="card-main">
            <p class="comment-text" @click="viewCommentDetail(comment)">{{ comment.content }}</p>
            <p class="post-author">작성자: <span class="author-link" @click="setUserFilter(comment.authorId)">{{ comment.authorName }}</span></p>
          </div>
          <div class="card-actions">
            <button @click="openEditModal(comment)" class="btn-action edit">수정</button>
            <template v-if="isItemActive(comment)">
              <button @click="toggleStatus(comment)" class="btn-action blind">블라인드</button>
              <button @click="handlePermanentDelete(comment)" class="btn-action delete">영구 삭제</button>
            </template>
            <template v-else><button @click="toggleStatus(comment)" class="btn-action restore">복구</button></template>
          </div>
        </div>
      </div>

      <!-- 신고 관리 리스트 -->
      <div v-else-if="currentTab === 'reports'" class="list-wrapper" :class="{ 'is-message-view': reportsState.targetType === 'MESSAGE' }">
        <!-- 1. 일반 신고 리스트 (모든 대상 포함) -->
        <template v-if="reportsState.targetType !== 'MESSAGE'">
          <div v-for="report in reportsState.list" :key="report.targetType + report.targetId" class="content-card report-card" :class="{ 'is-processed': report.reportStatus !== 'WAITING' }">
            <div class="card-header">
              <input type="checkbox" :value="report.targetType + ':' + report.targetId" v-model="selectedIds" class="report-checkbox" @click.stop />
              <span class="id-tag">#{{ report.targetId }}</span>
              <span class="type-badge" :class="report.targetType">{{ getReportTargetLabel(report.targetType) }}</span>
              <span class="report-status-badge" :class="report.reportStatus">{{ getReportStatusLabel(report.reportStatus) }}</span>
              <span class="report-count-badge clickable" @click="openReporterModal(report)">신고자 {{ report.reportCount }}명 🔍</span>
              <span class="date-text">{{ formatDate(report.latestReportDate) }}</span>
            </div>
            <div class="card-main">
              <div class="report-content-preview" @click="viewReportTarget(report)">
                <p class="target-title">{{ report.targetTitle }}</p>
                <p class="latest-reason">사유: {{ report.latestReason }}</p>
                <p class="target-author">대상: {{ report.authorName }}</p>
              </div>
            </div>
            <div class="card-actions" v-if="report.reportStatus === 'WAITING'">
              <button @click="handleReportAction(report, 'REJECTED')" class="btn-action edit">반려</button>
              <button @click="handleReportAction(report, 'BLIND_COMPLETE')" class="btn-action blind">블라인드</button>
              <button @click="handleReportAction(report, 'DELETE_COMPLETE')" class="btn-action delete">영구 삭제</button>
            </div>
            <div class="card-actions" v-else>
              <button @click="handleReportAction(report, 'WAITING')" class="btn-action restore">복구</button>
            </div>
          </div>
        </template>

        <!-- 2. 쪽지 전용 스플릿 카드 UI -->
        <template v-else>
          <div v-for="group in groupedMessagesList" :key="group.conversationId" class="split-card">
            <div class="split-card-header">
              <div class="user-info left">👤 {{ group.user1Name }}</div>
              <div class="vs-icon">⚔️</div>
              <div class="user-info right">👤 {{ group.user2Name }}</div>
            </div>
            <div class="split-card-body">
              <div class="report-side left" :class="{ 'no-report': !group.user1Report }">
                <template v-if="group.user1Report">
                  <div class="side-top">
                    <input type="checkbox" :value="'MESSAGE:' + group.user1Report.targetId" v-model="selectedIds" class="report-checkbox" />
                    <span class="report-status-badge mini" :class="group.user1Report.reportStatus">{{ getReportStatusLabel(group.user1Report.reportStatus) }}</span>
                  </div>
                  <div class="side-content" @click="viewReportTarget(group.user1Report)">
                    <p class="msg-preview">"{{ group.user1Report.targetTitle }}"</p>
                    <p class="reason-text">사유: {{ group.user1Report.latestReason }}</p>
                    <small class="date-small">{{ formatDate(group.user1Report.latestReportDate) }}</small>
                  </div>
                  <div class="side-actions">
                    <template v-if="group.user1Report.reportStatus === 'WAITING'">
                      <button @click="handleReportAction(group.user1Report, 'REJECTED')" class="btn-side-action edit">반려</button>
                      <button @click="handleReportAction(group.user1Report, 'BLIND_COMPLETE')" class="btn-side-action blind">블라인드</button>
                      <button @click="handleReportAction(group.user1Report, 'DELETE_COMPLETE')" class="btn-side-action delete">영구 삭제</button>
                    </template>
                    <button v-else @click="handleReportAction(group.user1Report, 'WAITING')" class="btn-side-action restore">복구하기</button>
                  </div>
                </template>
                <div v-else class="empty-side">접수된 신고 없음</div>
              </div>
              <div class="report-side right" :class="{ 'no-report': !group.user2Report }">
                <template v-if="group.user2Report">
                  <div class="side-top">
                    <input type="checkbox" :value="'MESSAGE:' + group.user2Report.targetId" v-model="selectedIds" class="report-checkbox" />
                    <span class="report-status-badge mini" :class="group.user2Report.reportStatus">{{ getReportStatusLabel(group.user2Report.reportStatus) }}</span>
                  </div>
                  <div class="side-content" @click="viewReportTarget(group.user2Report)">
                    <p class="msg-preview">"{{ group.user2Report.targetTitle }}"</p>
                    <p class="reason-text">사유: {{ group.user2Report.latestReason }}</p>
                    <small class="date-small">{{ formatDate(group.user2Report.latestReportDate) }}</small>
                  </div>
                  <div class="side-actions">
                    <template v-if="group.user2Report.reportStatus === 'WAITING'">
                      <button @click="handleReportAction(group.user2Report, 'REJECTED')" class="btn-side-action edit">반려</button>
                      <button @click="handleReportAction(group.user2Report, 'BLIND_COMPLETE')" class="btn-side-action blind">블라인드</button>
                      <button @click="handleReportAction(group.user2Report, 'DELETE_COMPLETE')" class="btn-side-action delete">영구 삭제</button>
                    </template>
                    <button v-else @click="handleReportAction(group.user2Report, 'WAITING')" class="btn-side-action restore">복구하기</button>
                  </div>
                </template>
                <div v-else class="empty-side">접수된 신고 없음</div>
              </div>
            </div>
          </div>
        </template>
      </div>
      <div v-if="currentList.length === 0" class="empty-state">검색 결과가 없습니다.</div>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination-bar" v-if="totalPages > 1">
      <button :disabled="currentPage === 0" @click="changePage(0)" class="btn-page">&laquo;</button>
      <div class="page-numbers">
        <button v-for="p in visiblePages" :key="p" class="btn-page-num" :class="{ active: currentPage + 1 === p }" @click="changePage(p - 1)">{{ p }}</button>
      </div>
      <button :disabled="currentPage === totalPages - 1" @click="changePage(totalPages - 1)" class="btn-page">&raquo;</button>
    </div>

    <!-- 모달 섹션 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="isEditModalOpen" class="modal-overlay" @click="isEditModalOpen = false">
          <div class="modal-content" @click.stop>
            <div class="modal-head"><h3>🛠️ {{ currentTab === 'posts' ? '게시글' : '댓글' }} 수정</h3><span class="modal-id-badge">ID: {{ editForm.id }}</span></div>
            <div class="modal-form-body">
              <!-- [시니어 조치] 정밀 4단계 계층형 수정 UI -->
              <template v-if="currentTab === 'posts'">
                <div class="form-row-multi">
                  <!-- 1단계: 게시판 -->
                  <div class="form-group quarter">
                    <label>1. 게시판</label>
                    <select v-model="editForm.type" class="form-select" :disabled="editForm.type === 'NOTICE'">
                      <option v-if="editForm.type === 'NOTICE'" value="NOTICE">📢 공고소통방</option>
                      <template v-else>
                        <option value="GENERAL">🏛️ 통합광장</option>
                        <option value="REVIEW">✨ 당첨후기</option>
                        <option value="PARTNER">🤝 파트너스</option>
                      </template>
                    </select>
                  </div>
                  
                  <!-- 2단계: 소메뉴 분류 -->
                  <div v-if="ADMIN_4STEP_MAP[editForm.type]" class="form-group quarter">
                    <label>2. 소메뉴</label>
                    <select v-model="categoryGroup" class="form-select">
                      <option v-for="group in ADMIN_4STEP_MAP[editForm.type].groups" :key="group.value" :value="group.value">
                        {{ group.label }}
                      </option>
                    </select>
                  </div>

                  <!-- 3단계: 상세 대상 (지역/기관 등) -->
                  <div v-if="ADMIN_4STEP_MAP[editForm.type]" class="form-group quarter">
                    <label>3. 상세 대상</label>
                    <select v-model="editForm.subCategory" class="form-select">
                      <option v-for="target in ADMIN_4STEP_MAP[editForm.type].groups.find(g => g.value === categoryGroup)?.targets" :key="target.value" :value="target.value">
                        {{ target.label }}
                      </option>
                    </select>
                  </div>

                  <!-- 4단계: 소통 주제 -->
                  <div v-if="ADMIN_4STEP_MAP[editForm.type]" class="form-group quarter">
                    <label>4. 소통 주제</label>
                    <select v-model="editForm.category" class="form-select">
                      <option v-for="topic in ADMIN_4STEP_MAP[editForm.type].groups.find(g => g.value === categoryGroup)?.topics" :key="topic.value" :value="topic.value">
                        {{ topic.label }}
                      </option>
                    </select>
                  </div>
                </div>

                <div class="form-group">
                  <label>게시글 제목</label>
                  <input v-model="editForm.title" class="form-input" />
                </div>
              </template>
              
              <div class="form-group">
                <label>{{ currentTab === 'posts' ? '본문 내용' : '댓글 내용' }}</label>
                <textarea v-model="editForm.content" class="form-textarea" rows="10"></textarea>
              </div>
            </div>
            <div class="modal-actions-footer"><button @click="handleUpdate" class="btn-save">저장</button><button @click="isEditModalOpen = false" class="btn-cancel">닫기</button></div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="isReporterModalOpen" class="modal-overlay" @click="isReporterModalOpen = false">
          <div class="modal-content reporter-modal" @click.stop>
            <div class="modal-head"><h3>👥 신고자 상세 명단</h3></div>
            <div class="modal-body scrollable">
              <div class="reporter-table-wrapper">
                <table class="reporter-table">
                  <thead><tr><th>신고자 (현재 카르마)</th><th>신고 사유</th><th>신고 일시</th><th>관리</th></tr></thead>
                  <tbody>
                    <tr v-for="rep in selectedReporters" :key="rep.reporterId">
                      <td data-label="신고자"><div class="rep-name-flex"><span class="rep-name">{{ rep.reporterName }}</span><span class="rep-karma" :class="getKarmaClass(rep.reporterKarma)">({{ (rep.reporterKarma / 10).toFixed(1) }}P)</span></div></td>
                      <td data-label="사유" class="rep-reason">{{ rep.reason }}</td>
                      <td data-label="일시" class="rep-date">{{ formatDate(rep.reportedAt) }}</td>
                      <td class="rep-action"><button class="btn-penalize" @click="handleReporterKarmaChange(rep)">허위신고 제재</button></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div class="modal-actions-footer"><button @click="isReporterModalOpen = false" class="btn-cancel">닫기</button></div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <AdminMessageLogModal :show="isLogModalOpen" :targetId="selectedLogTargetId" :reporterId="selectedLogReporterId" @close="isLogModalOpen = false" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/plugins/axios'
import AdminMessageLogModal from '@/components/AdminMessageLogModal.vue'

const route = useRoute()
const currentTab = ref('posts')
const selectedIds = ref([]) 

const postsState = ref({ list: [], keyword: '', status: 'ALL', userId: null, postId: null, page: 0, totalPages: 0, totalElements: 0 })
const commentsState = ref({ list: [], keyword: '', status: 'ALL', userId: null, commentId: null, page: 0, totalPages: 0, totalElements: 0 })
const reportsState = ref({ list: [], keyword: '', status: 'ALL', targetType: 'ALL', page: 0, totalPages: 0, totalElements: 0 })
const groupedMessagesList = ref([])

onMounted(() => {
  // [시니어 조치] URL 파라미터(postId, commentId)가 넘어온 경우 해당 탭 및 필터 자동 설정
  if (route.query.postId) {
    currentTab.value = 'posts';
    postsState.value.postId = route.query.postId;
  } else if (route.query.commentId) {
    currentTab.value = 'comments';
    commentsState.value.commentId = route.query.commentId;
  }
  fetchData();
});

const reportTargetFilter = computed({ get: () => reportsState.value.targetType, set: (val) => { reportsState.value.targetType = val } })
const currentFilter = computed(() => { if (currentTab.value === 'posts') return postsState.value; if (currentTab.value === 'comments') return commentsState.value; return reportsState.value })
const currentList = computed(() => {
  if (currentTab.value === 'reports' && reportsState.value.targetType === 'MESSAGE') {
    return groupedMessagesList.value;
  }
  return currentFilter.value.list;
})
const currentPage = computed(() => currentFilter.value.page)
const totalPages = computed(() => currentFilter.value.totalPages)
const filteredCount = computed(() => currentFilter.value.totalElements)
const totalCount = ref(0)
const isEditModalOpen = ref(false)
const categoryGroup = ref('ALL')
const editForm = ref({ id: null, title: '', content: '', type: '', category: '', subCategory: '' })
const isReporterModalOpen = ref(false)
const selectedReporters = ref([])
const selectedTargetId = ref(null)
const currentCountInfo = computed(() => `${filteredCount.value}건 / ${totalCount.value}건`)

const getPlaceholder = computed(() => { if (currentTab.value === 'posts') return '제목, 내용, 닉네임 또는 #ID...'; if (currentTab.value === 'comments') return '내용, 닉네임 또는 #ID...'; return '신고 사유 검색...' })
const isItemActive = (item) => item && item.status && item.status.toString().toUpperCase() === 'ACTIVE'

const fetchData = async () => {
  try {
    let endpoint = ''; let params = { page: currentFilter.value.page, size: 20 }
    if (currentTab.value === 'posts') { 
      endpoint = '/api/admin/board/posts'; 
      params.userId = postsState.value.userId; 
      params.postId = postsState.value.postId; // [시니어 조치] ID 필터 추가
      params.keyword = postsState.value.keyword; 
      params.status = postsState.value.status === 'ALL' ? null : postsState.value.status 
    }
    else if (currentTab.value === 'comments') { 
      endpoint = '/api/admin/board/comments'; 
      params.userId = commentsState.value.userId; 
      params.commentId = commentsState.value.commentId; // [시니어 조치] ID 필터 추가
      params.keyword = commentsState.value.keyword; 
      params.status = commentsState.value.status === 'ALL' ? null : commentsState.value.status 
    }
    else { 
      if (reportsState.value.targetType === 'MESSAGE') {
        endpoint = '/api/admin/board/reports/grouped-messages';
        params.status = reportsState.value.status === 'ALL' ? null : reportsState.value.status;
        params.keyword = reportsState.value.keyword;
        const res = await axios.get(endpoint, { params });
        groupedMessagesList.value = res.data.data;
        
        // [시니어 조치] 쪽지 신고는 그룹화된 리스트를 사용하므로 관련 상태를 수동 업데이트
        let totalReportsCount = 0;
        groupedMessagesList.value.forEach(group => {
          if (group.user1Report) totalReportsCount++;
          if (group.user2Report) totalReportsCount++;
        });
        
        reportsState.value.list = []; // 일반 리스트는 비움
        reportsState.value.totalElements = totalReportsCount;
        reportsState.value.totalPages = 1; // 현재 쪽지 그룹화는 페이징 미지원(전체반환)하므로 1로 설정
        reportsState.value.page = 0;
        
        if (!reportsState.value.keyword && reportsState.value.status === 'ALL') totalCount.value = totalReportsCount;
        selectedIds.value = [];
        return;
      }
      endpoint = '/api/admin/board/reports'; 
      params.targetType = reportsState.value.targetType === 'ALL' ? null : reportsState.value.targetType; 
      params.status = reportsState.value.status === 'ALL' ? null : reportsState.value.status;
      params.keyword = reportsState.value.keyword; 
    }
    const res = await axios.get(endpoint, { params }); const pageData = res.data.data
    currentFilter.value.list = pageData.content; currentFilter.value.totalPages = pageData.totalPages; currentFilter.value.totalElements = pageData.totalElements
    if (!currentFilter.value.keyword && currentFilter.value.status === 'ALL') totalCount.value = pageData.totalElements
    selectedIds.value = [] 
  } catch (e) { console.error('데이터 로드 실패') }
}

const handleReportAction = async (report, newStatus) => {
  const msg = newStatus === 'BLIND_COMPLETE' ? '블라인드 처리하시겠습니까?' : (newStatus === 'REJECTED' ? '신고를 반려하시겠습니까?' : (newStatus === 'WAITING' ? '처리 대기 상태로 복구하시겠습니까?' : '영구 삭제하시겠습니까?'))
  if (!confirm(msg)) return
  try {
    await axios.patch('/api/admin/board/reports/handle', { targetId: report.targetId, targetType: report.targetType, status: newStatus })
    fetchData()
  } catch (e) { alert('처리 실패') }
}

const isAllSelected = computed(() => {
  if (currentTab.value === 'reports' && reportsState.value.targetType === 'MESSAGE') {
    let totalInGroup = 0;
    groupedMessagesList.value.forEach(group => {
      if (group.user1Report) totalInGroup++;
      if (group.user2Report) totalInGroup++;
    });
    return totalInGroup > 0 && selectedIds.value.length === totalInGroup;
  }
  return currentFilter.value.list.length > 0 && selectedIds.value.length === currentFilter.value.list.length;
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = [];
  } else {
    if (currentTab.value === 'reports' && reportsState.value.targetType === 'MESSAGE') {
      const ids = [];
      groupedMessagesList.value.forEach(group => {
        if (group.user1Report) ids.push(`MESSAGE:${group.user1Report.targetId}`);
        if (group.user2Report) ids.push(`MESSAGE:${group.user2Report.targetId}`);
      });
      selectedIds.value = ids;
    } else {
      selectedIds.value = currentFilter.value.list.map(r => `${r.targetType}:${r.targetId}`);
    }
  }
}

const handleBulkReport = async (newStatus) => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`선택한 ${selectedIds.value.length}건을 일괄 처리하시겠습니까?`)) return
  const targetIdsByPost = selectedIds.value.filter(id => id.startsWith('POST')).map(id => Number(id.split(':')[1]))
  const targetIdsByComment = selectedIds.value.filter(id => id.startsWith('COMMENT')).map(id => Number(id.split(':')[1]))
  const targetIdsByMessage = selectedIds.value.filter(id => id.startsWith('MESSAGE')).map(id => Number(id.split(':')[1]))
  try {
    if (targetIdsByPost.length > 0) await axios.post('/api/admin/board/reports/bulk-handle', { targetIds: targetIdsByPost, targetType: 'POST', status: newStatus })
    if (targetIdsByComment.length > 0) await axios.post('/api/admin/board/reports/bulk-handle', { targetIds: targetIdsByComment, targetType: 'COMMENT', status: newStatus })
    if (targetIdsByMessage.length > 0) await axios.post('/api/admin/board/reports/bulk-handle', { targetIds: targetIdsByMessage, targetType: 'MESSAGE', status: newStatus })
    alert('처리되었습니다.'); fetchData()
  } catch (e) { alert('일괄 처리 실패') }
}

const isLogModalOpen = ref(false)
const selectedLogTargetId = ref(null)
const selectedLogReporterId = ref(null)

const openMessageLogModal = (report) => {
  if (!report || report.targetType !== 'MESSAGE') return
  selectedLogTargetId.value = report.targetId
  selectedLogReporterId.value = report.reporters && report.reporters.length > 0 ? report.reporters[0].reporterId : null
  isLogModalOpen.value = true
}

const viewPostDetail = (id) => { window.open(`/board/post/${id}`, '_blank') }
const viewCommentDetail = (comment) => { window.open(`/board/post/${comment.postId}#comment-${comment.id}`, '_blank') }
const viewReportTarget = (report) => {
  if (report.targetType === 'POST') viewPostDetail(report.targetId)
  else if (report.targetType === 'COMMENT') viewCommentDetail({ postId: report.parentPostId, id: report.targetId })
  else openMessageLogModal(report)
}

const getReportTargetLabel = (type) => { const labels = { POST: '게시글', COMMENT: '댓글', MESSAGE: '쪽지' }; return labels[type] || type }
const getReportStatusLabel = (status) => { const labels = { WAITING: '대기중', BLIND_COMPLETE: '블라인드 완료', DELETE_COMPLETE: '영구 삭제 완료', REJECTED: '반려됨' }; return labels[status] || status }

const getPostTypeLabel = (type) => {
  const labels = { GENERAL: '통합광장', NOTICE: '공고소통방', REVIEW: '당첨후기', PARTNER: '파트너스' }
  return labels[type] || type
}

const getStatusLabel = (status) => {
  const labels = { ACTIVE: '정상', BLINDED: '숨김', DELETED: '관리자 삭제', USER_DELETED:'사용자 삭제' }
  return labels[status] || status
}

const changePage = (page) => { currentFilter.value.page = page; fetchData() }
const handleFilterChange = () => {
  // #숫자 or 순수 숫자 입력 시 ID 필터로 처리
  const kw = currentFilter.value.keyword?.trim() || ''
  const idMatch = kw.match(/^#?(\d+)$/)
  if (idMatch && currentTab.value === 'posts') {
    postsState.value.postId = idMatch[1]
    postsState.value.keyword = ''
  } else if (idMatch && currentTab.value === 'comments') {
    commentsState.value.commentId = idMatch[1]
    commentsState.value.keyword = ''
  } else {
    // 일반 검색 시 ID 필터 초기화
    if (currentTab.value === 'posts') postsState.value.postId = null
    else if (currentTab.value === 'comments') commentsState.value.commentId = null
  }
  currentFilter.value.page = 0
  fetchData()
}
const toggleStatus = async (item) => {
  const newStatus = item.status === 'ACTIVE' ? 'BLINDED' : 'ACTIVE'; const type = currentTab.value === 'posts' ? 'posts' : 'comments'
  if (!confirm(`${newStatus === 'BLINDED' ? '블라인드 처리' : '정상 상태로 복구'} 하시겠습니까?`)) return
  try { await axios.patch(`/api/admin/board/${type}/${item.id}/status`, { status: newStatus }); item.status = newStatus } catch (e) { alert('상태 변경 실패') }
}

const handlePermanentDelete = async (item) => {
  if (!confirm('이 게시물을 영구 삭제하시겠습니까?')) return
  const type = currentTab.value === 'posts' ? 'posts' : 'comments'
  try { await axios.patch(`/api/admin/board/${type}/${item.id}/status`, { status: 'DELETED' }); alert('영구 삭제 처리되었습니다.'); fetchData() } catch (e) { alert('삭제 실패') }
}

// [시니어 조치] 정밀 4단계 계층형 맵 (사용자 요청 100% 반영)
const ADMIN_4STEP_MAP = {
  GENERAL: {
    name: '🏛️ 통합광장',
    groups: [
      { label: '🌐 전체', value: 'ALL', targets: [{ label: '전체', value: 'ALL' }], topics: [
        { label: '☕ 자유수다', value: 'FREE' }
      ]},
      { label: '📍 지역별 소통', value: 'REGION', 
        targets: [
          { label: '서울', value: 'SEOUL' }, { label: '경기/인천', value: 'GYEONGGI_INCHEON' }, 
          { label: '충청/강원/세종', value: 'CHUNG_GANG_SEJONG' }, { label: '경상/부산/대구', value: 'GYEONG_BU_DAE' }, 
          { label: '전라/광주/제주', value: 'JEON_GWANG_JEJU' }
        ], 
        topics: [
          { label: '📢 동네소식', value: 'LOCAL_NEWS' }, { label: '📍 장소추천', value: 'PLACE' }, { label: '🤝 동네번개', value: 'MEETUP' }
        ]
      },
      { label: '🏢 기관별 소통', value: 'INSTITUTION', 
        targets: [
          { label: 'LH 소통', value: 'LH' }, { label: 'SH 소통', value: 'SH' }, { label: '민간임대 소통', value: 'PRIVATE' }
        ], 
        topics: [
          { label: '📊 서류현황', value: 'STATUS' }, { label: '💡 기관별팁', value: 'INST_TIPS' }, { label: '📢 기관문의', value: 'INQUIRY' }
        ]
      }
    ]
  },
  REVIEW: {
    name: '✨ 당첨후기',
    groups: [
      { label: '🌐 전체', value: 'ALL', targets: [{ label: '전체', value: 'ALL' }], topics: [{ label: '☕ 자유수다', value: 'FREE' }]},
      { label: '🏆 당첨자 인터뷰', value: 'INTERVIEW', targets: [{ label: '전체', value: 'ALL' }], 
        topics: [{ label: '🏆 내집마련기', value: 'SUCCESS_STORY' }, { label: '🤫 당첨비결', value: 'SECRET' }]},
      { label: '📝 서류/청약 팁', value: 'TIPS', targets: [{ label: '전체', value: 'ALL' }], 
        topics: [{ label: '📝 서류팁', value: 'DOCUMENT' }, { label: '💰 자금계획', value: 'LOAN' }, { label: '💡 일반팁', value: 'GENERAL_TIPS' }]},
      { label: '🏠 입주/사전점검', value: 'MOVE_IN', targets: [{ label: '전체', value: 'ALL' }], 
        topics: [{ label: '🏠 사전점검', value: 'PRE_CHECK' }, { label: '🚚 입주후기', value: 'POST_CHECK' }]}
    ]
  },
  PARTNER: {
    name: '🤝 파트너스',
    groups: [
      { label: '🌐 전체', value: 'ALL', targets: [{ label: '전체', value: 'ALL' }], topics: [{ label: '☕ 자유수다', value: 'FREE' }]},
      { label: '🚚 이사', value: 'MOVING', targets: [{ label: '전체', value: 'ALL' }], 
        topics: [{ label: '🚚 이용후기', value: 'PARTNER_REVIEW' }, { label: '🚚 견적공유', value: 'ESTIMATE' }, { label: '🚚 체크리스트', value: 'CHECKLIST' }]},
      { label: '🧼 청소', value: 'CLEANING', targets: [{ label: '전체', value: 'ALL' }], 
        topics: [{ label: '🧼 이용후기', value: 'PARTNER_REVIEW' }, { label: '🧼 견적공유', value: 'ESTIMATE' }, { label: '🧼 체크리스트', value: 'CHECKLIST' }]},
      { label: '🛋️ 인테리어', value: 'INTERIOR', targets: [{ label: '전체', value: 'ALL' }], 
        topics: [{ label: '🛋️ 이용후기', value: 'PARTNER_REVIEW' }, { label: '🛋️ 견적공유', value: 'ESTIMATE' }, { label: '🛋️ 체크리스트', value: 'CHECKLIST' }]}
    ]
  }
};

// 계층형 선택 연동 로직
watch(() => editForm.value.type, (newType) => {
  if (isEditModalOpen.value && ADMIN_4STEP_MAP[newType]) {
    categoryGroup.value = ADMIN_4STEP_MAP[newType].groups[0].value;
  }
});

watch(categoryGroup, (newGroup) => {
  if (isEditModalOpen.value && editForm.value.type && ADMIN_4STEP_MAP[editForm.value.type]) {
    const group = ADMIN_4STEP_MAP[editForm.value.type].groups.find(g => g.value === newGroup);
    if (group) {
      // 2단계 선택 시 3, 4단계 기본값 세팅
      editForm.value.subCategory = group.targets[0].value;
      editForm.value.category = group.topics[0].value;
    }
  }
});

const openEditModal = (item) => { 
  if (currentTab.value === 'posts') {
    editForm.value = { 
      id: item.id, title: item.title, content: item.content,
      type: item.type, category: item.category || '', subCategory: item.subCategory || ''
    };
    // 현재 데이터로부터 categoryGroup 유추 (UI 동기화)
    const board = ADMIN_4STEP_MAP[item.type];
    if (board) {
      for (const group of board.groups) {
        if (group.topics.some(t => t.value === item.category)) {
          categoryGroup.value = group.value;
          break;
        }
      }
    }
  } else { 
    editForm.value = { id: item.id, title: '', content: item.content, type: '', category: '', subCategory: '' }; 
  } 
  isEditModalOpen.value = true 
}

const handleUpdate = async () => {
  try {
    const type = currentTab.value === 'posts' ? 'posts' : 'comments'; 
    const payload = currentTab.value === 'posts' ? { 
      title: editForm.value.title, content: editForm.value.content,
      type: editForm.value.type, category: editForm.value.category, subCategory: editForm.value.subCategory
    } : { content: editForm.value.content }
    
    await axios.patch(`/api/admin/board/${type}/${editForm.value.id}`, payload); 
    alert('수정되었습니다 (3단계 매핑 및 점수 재계산 완료).'); 
    fetchData(); isEditModalOpen.value = false
  } catch (e) { alert('수정 실패') }
}

const setUserFilter = (id) => { currentFilter.value.userId = id; handleFilterChange() }
const clearUserFilter = () => { currentFilter.value.userId = null; handleFilterChange() }
const clearIdFilter = () => {
  if (currentTab.value === 'posts') postsState.value.postId = null
  else if (currentTab.value === 'comments') commentsState.value.commentId = null
  fetchData()
}
const formatDate = (dateStr) => { if (!dateStr) return '-'; const date = new Date(dateStr); return date.toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }
const visiblePages = computed(() => { const range = []; const start = Math.max(1, currentFilter.value.page - 2); const end = Math.min(totalPages.value, start + 4); for (let i = Math.max(1, start); i <= end; i++) range.push(i); return range })

watch(currentTab, () => { fetchData() })
const openReporterModal = (report) => { selectedReporters.value = report.reporters || []; selectedTargetId.value = report.targetId; isReporterModalOpen.value = true }
const handleReporterKarmaChange = async (reporter) => {
  const points = prompt(`감점할 점수를 입력하세요 (예: -10):`, "-10"); if (!points || isNaN(points)) return
  try {
    await axios.patch(`/api/admin/users/${reporter.reporterId}/karma`, null, { params: { points: parseFloat(points), reason: 'FALSE_REPORT', reasonText: `대상 ID(#${selectedTargetId.value})에 대한 악의적 신고` } })
    alert('페널티 부여 완료'); reporter.reporterKarma += Math.round(parseFloat(points) * 10)
  } catch (e) { alert('페널티 부여 실패') }
}
const getKarmaClass = (point) => { if (point >= 800) return 'safe'; if (point >= 500) return 'warning'; return 'danger' }
onMounted(() => { if (route.query.userId) postsState.value.userId = route.query.userId; fetchData() })
</script>

<style scoped>
.admin-community-container { display: flex; flex-direction: column; height: 100%; gap: 0; }
.admin-header { margin-top: 0; margin-bottom: 5px; }
.header-main { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.header-main h2 { font-size: 1.1rem; font-weight: 800; margin: 0; color: var(--text-primary); }
.count-info { font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); }

.filter-bar { display: flex; justify-content: space-between; align-items: center; gap: 15px; flex-wrap: wrap; margin-bottom: 12px; }
.search-wrapper { flex: 1; min-width: 250px; position: relative; display: flex; gap: 8px; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.5; z-index: 1; }
.admin-search-input { flex: 1; padding: 10px 10px 10px 35px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--header-bg); color: var(--text-primary); outline: none; min-width: 0; }
.mobile-search-btn {
  padding: 0 15px;
  background: var(--link-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.mobile-search-btn:active { transform: scale(0.95); opacity: 0.9; }
@media (min-width: 768px) {
  .mobile-search-btn { display: none; } /* PC에서는 깔끔하게 숨김 (엔터로 충분) */
}
.filter-group { display: flex; gap: 10px; align-items: center; }
.admin-select { padding: 8px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--header-bg); color: var(--text-primary); cursor: pointer; }
.btn-clear-user { padding: 6px 12px; background: #34495e; color: white; border-radius: 6px; font-size: 0.75rem; border: none; cursor: pointer; }

.tab-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid var(--border-color); margin-bottom: 15px; flex-wrap: wrap; gap: 10px; }
.tab-nav { display: flex; gap: 5px; }
.tab-btn { padding: 12px 20px; font-weight: 700; font-size: 0.95rem; cursor: pointer; background: none; border: none; color: var(--text-secondary); border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; white-space: nowrap; }
.tab-btn.active { color: var(--link-color); border-bottom-color: var(--link-color); }

.bulk-actions { display: flex; gap: 12px; align-items: center; padding-bottom: 5px; }
.bulk-actions-group { display: flex; gap: 8px; align-items: center; }
.btn-select-all { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: 8px; font-size: 0.8rem; font-weight: 700; cursor: pointer; color: var(--text-primary); }
.btn-bulk-blind { background: #ed4956; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
.btn-bulk-delete { background: #8e44ad; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
.btn-bulk-wait { background: #3498db; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
.btn-bulk-reject { background: #95a5a6; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }

.btn-detect-mutual { background: #34495e; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 800; cursor: pointer; font-size: 0.82rem; transition: all 0.2s; border: 2px solid transparent; }
.btn-detect-mutual:hover { background: #2c3e50; transform: translateY(-1px); }
.btn-detect-mutual.active { background: #2c3e50; border-color: var(--link-color); box-shadow: 0 0 10px rgba(0, 149, 246, 0.3); }

.message-guide-card { grid-column: 1 / -1; background: linear-gradient(135deg, rgba(52, 152, 219, 0.1) 0%, rgba(46, 204, 113, 0.1) 100%); border: 2px dashed var(--link-color); padding: 25px; text-align: center; cursor: pointer; transition: all 0.3s; }
.message-guide-card:hover { transform: scale(1.01); background: linear-gradient(135deg, rgba(52, 152, 219, 0.15) 0%, rgba(46, 204, 113, 0.15) 100%); }
.guide-content h3 { margin: 0 0 8px; font-size: 1.1rem; color: var(--link-color); }
.guide-content p { margin: 0 0 15px; font-size: 0.9rem; color: var(--text-secondary); }
.btn-go-message { background: var(--link-color); color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 800; cursor: pointer; }

.content-body { flex: 1; overflow-y: auto; }
.list-wrapper { display: grid; grid-template-columns: 1fr; gap: 15px; }
@media (min-width: 1024px) { .list-wrapper { grid-template-columns: 1fr 1fr; } }

.content-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 15px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s; }
.card-header { display: flex; align-items: center; gap: 8px; }
.report-checkbox { width: 18px; height: 18px; cursor: pointer; }
.id-tag { font-family: monospace; font-weight: 800; color: var(--link-color); font-size: 0.8rem; background: rgba(0,149,246,0.05); padding: 2px 6px; border-radius: 4px; }
.type-badge { font-size: 0.65rem; font-weight: 800; padding: 3px 8px; border-radius: 4px; background: #eee; color: #666; }
.type-badge.GENERAL { background: #f0f2f5; color: #475569; border: 1px solid #cbd5e1; }
.type-badge.NOTICE { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
.type-badge.REVIEW { background: #fffbeb; color: #d97706; border: 1px solid #fde68a; }
.type-badge.PARTNER { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }

.status-badge, .report-status-badge { font-size: 0.65rem; font-weight: 800; padding: 3px 8px; border-radius: 4px; }
.status-badge.ACTIVE, .report-status-badge.WAITING { background: #e8f5e9; color: #2e7d32; }
.status-badge.BLINDED, .report-status-badge.BLIND_COMPLETE { background: #fff3e0; color: #e65100; }
.report-status-badge.REJECTED { background: #ffebee; color: #c62828; }
.report-status-badge.DELETE_COMPLETE { background: rgba(142, 68, 173, 0.1); color: #8e44ad; border: 1px solid #8e44ad; }
.status-badge.DELETED { background: #8e44ad; color: #fff; font-weight: 900; }
.status-badge.USER_DELETED { background: #6c757d; color: #fff; font-weight: 900; }

.report-count-badge { font-size: 0.65rem; font-weight: 850; padding: 3px 8px; border-radius: 4px; background: #f0f2f5; color: var(--link-color); }
.report-count-badge.clickable { cursor: pointer; border: 1px solid var(--link-color); transition: all 0.2s; }
.report-count-badge.clickable:hover { background: var(--link-color); color: white; }

.date-text { font-size: 0.75rem; color: var(--text-secondary); margin-left: auto; }
.card-main { display: flex; flex-direction: column; gap: 5px; cursor: pointer; }
.post-title, .target-title { font-size: 1rem; font-weight: 700; margin: 0; color: var(--text-primary); }
.latest-reason { font-size: 0.85rem; color: #e67e22; font-weight: 600; margin: 2px 0; }
.post-author { font-size: 0.8rem; color: var(--text-secondary); margin: 0; }
.author-link { color: var(--link-color); font-weight: 700; text-decoration: underline; cursor: pointer; }

.card-actions { display: flex; gap: 8px; margin-top: 5px; }
.btn-action { flex: 1; padding: 10px; border-radius: 8px; font-size: 0.85rem; font-weight: 700; cursor: pointer; border: none; transition: all 0.2s; }
.btn-action.edit { background: var(--hover-bg); color: var(--text-primary); border: 1px solid var(--border-color); }
.btn-action.blind { background: #ed4956; color: white; }
.btn-action.restore { background: #3498db; color: white; }
.btn-action.delete { background: #8e44ad; color: white; }

.pagination-bar { display: flex; justify-content: center; align-items: center; gap: 15px; padding: 20px 0; }
.page-numbers { display: flex; gap: 5px; }
.btn-page { padding: 8px 12px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; cursor: pointer; color: var(--text-secondary); }
.btn-page-num { width: 36px; height: 36px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; cursor: pointer; font-weight: 700; color: var(--text-secondary); }
.btn-page-num.active { background: var(--link-color); color: white; border-color: var(--link-color); }

.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 99999; backdrop-filter: blur(5px); }
.modal-content { background: var(--card-bg); width: 95%; max-width: 650px; border-radius: 20px; display: flex; flex-direction: column; box-shadow: 0 30px 60px rgba(0,0,0,0.3); border: 1px solid var(--border-color); max-height: 85vh; }
.modal-head { padding: 25px 30px 15px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--divider-color); }
.modal-head h3 { margin: 0; font-size: 1.2rem; font-weight: 800; color: var(--text-primary); }
.modal-form-body { flex: 1; min-height: 0; overflow-y: auto; padding: 20px 30px; }

/* [시니어 조치] 수정 모달 폼 고도화 스타일 */
.form-row { display: flex; gap: 15px; margin-bottom: 5px; }
.form-row-multi { display: flex; gap: 10px; margin-bottom: 5px; flex-wrap: wrap; }
.form-group.half { flex: 1; }
.form-group.quarter { flex: 1; min-width: 130px; }
@media (max-width: 768px) { .form-group.quarter { min-width: 45%; } }
@media (max-width: 480px) { .form-group.quarter { min-width: 100%; } }
.form-select {
  width: 100%; padding: 12px; border: 1.5px solid var(--border-color); 
  border-radius: 10px; background: var(--header-bg); color: var(--text-primary); 
  font-size: 0.9rem; font-weight: 700; outline: none; cursor: pointer;
}
.form-select:focus { border-color: var(--link-color); background: var(--card-bg); }

.form-group { margin-bottom: 20px; display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 0.85rem; font-weight: 800; color: var(--text-secondary); }
.form-input, .form-textarea { 
  width: 100% !important; 
  box-sizing: border-box !important; 
  padding: 12px 15px; 
  border: 1.5px solid var(--border-color); 
  border-radius: 10px; 
  background: var(--header-bg); 
  color: var(--text-primary); 
  font-size: 0.95rem; 
  outline: none;
  transition: all 0.2s;
}
.form-input:focus, .form-textarea:focus { border-color: var(--link-color); background: var(--card-bg); }
.form-textarea { resize: vertical; min-height: 200px; line-height: 1.6; }

.modal-actions-footer { padding: 15px 30px 25px; display: flex; gap: 12px; justify-content: center; }
.btn-save { flex: 2; padding: 14px; background: var(--link-color); color: white; border: none; border-radius: 10px; font-size: 1rem; font-weight: 800; cursor: pointer; }
.btn-cancel { flex: 1; padding: 14px; background: var(--hover-bg); color: var(--text-primary); border: 1px solid var(--border-color); border-radius: 10px; font-size: 1rem; font-weight: 700; cursor: pointer; }

/* 쪽지 전용 스플릿 카드 스타일 */
.list-wrapper.is-message-view { grid-template-columns: 1fr !important; }
.split-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; overflow: hidden; margin-bottom: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
.split-card-header { display: flex; align-items: center; padding: 12px 20px; background: var(--header-bg); border-bottom: 1px solid var(--border-color); }
.user-info { flex: 1; font-weight: 900; font-size: 0.95rem; color: var(--text-primary); text-align: center; }
.vs-icon { background: #ed4956; color: white; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.8rem; margin: 0 15px; }
.split-card-body { display: flex; min-height: 120px; }
.report-side { flex: 1; padding: 15px 20px; display: flex; flex-direction: column; gap: 10px; transition: 0.2s; }
.report-side.left { border-right: 1px solid var(--divider-color); }
.report-side.no-report { background: var(--header-bg); opacity: 0.5; justify-content: center; align-items: center; color: var(--text-muted); font-style: italic; font-size: 0.85rem; }
.side-top { display: flex; justify-content: space-between; align-items: center; }
.report-status-badge.mini { font-size: 0.65rem; padding: 2px 6px; }
.side-content { cursor: pointer; flex: 1; }
.msg-preview { font-weight: 800; font-size: 0.9rem; color: var(--text-primary); margin: 0 0 5px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.reason-text { font-size: 0.8rem; color: #e67e22; font-weight: 600; margin: 0; }
.date-small { font-size: 0.7rem; color: var(--text-muted); }

/* 사이드 개별 액션 버튼 스타일 */
.side-actions { display: flex; gap: 5px; margin-top: 10px; border-top: 1px dashed var(--divider-color); padding-top: 10px; }
.btn-side-action { flex: 1; padding: 6px 2px; border-radius: 6px; font-size: 0.72rem; font-weight: 800; cursor: pointer; border: none; transition: 0.2s; white-space: nowrap; }
.btn-side-action.edit { background: #95a5a6; color: white; }
.btn-side-action.blind { background: #ed4956; color: white; }
.btn-side-action.delete { background: #8e44ad; color: white; }
/* [시니어 조치] 신고자 모달 및 테이블 스타일 복구 */
.reporter-modal { max-width: 800px !important; }
.reporter-table-wrapper { width: 100%; border: 1px solid var(--border-color); border-radius: 10px; background: var(--card-bg); overflow: hidden; margin-top: 15px; }
.reporter-table { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 0.78rem; text-align: left; }
.reporter-table th { background: var(--hover-bg); padding: 12px 10px; border-bottom: 2px solid var(--border-color); color: var(--text-secondary); font-weight: 850; }
.reporter-table td { padding: 12px 10px; border-bottom: 1px solid var(--divider-color); vertical-align: middle; }

.rep-name-flex { display: flex; align-items: center; gap: 6px; }
.rep-name { font-weight: 700; color: var(--text-primary); }
.rep-karma { font-size: 0.7rem; font-weight: 800; opacity: 0.8; }
.rep-karma.safe { color: #2ecc71; }
.rep-karma.warning { color: #f1c40f; }
.rep-karma.danger { color: #e74c3c; }
.rep-reason { color: #e67e22; font-weight: 600; line-height: 1.4; min-width: 150px; font-size: 0.75rem; }
.rep-date { font-size: 0.7rem; color: var(--text-muted); white-space: nowrap; }
.btn-penalize { background: none; border: 1.5px solid #ed4956; color: #ed4956; padding: 3px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 800; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-penalize:hover { background: #ed4956; color: white; }

/* 기존 모바일 대응 스타일 하단에 추가 */
@media (max-width: 768px) {
  /* [시니어 조치] 모달 깨짐 방지 및 가독성 최적화 */
  .modal-overlay { padding: 15px; }
  .modal-content {
    width: 95% !important;
    max-width: 100%;
    border-radius: 16px;
    max-height: 90vh;
  }
  .modal-head { padding: 15px 20px; }
  .modal-head h3 { font-size: 1rem; }
  .modal-form-body { padding: 15px 20px; }
  .form-input, .form-textarea { font-size: 0.9rem; padding: 10px; }
  .modal-actions-footer { padding: 15px 20px; gap: 8px; }
  .btn-save, .btn-cancel { padding: 12px; font-size: 0.9rem; }

  .reporter-table, .reporter-table thead, .reporter-table tbody, .reporter-table th, .reporter-table td, .reporter-table tr { display: block; }
  .reporter-table thead { display: none; }
  .reporter-table tr { border: 1px solid var(--border-color); border-radius: 6px; margin-bottom: 6px; padding: 8px 10px; background: var(--header-bg); }
  .reporter-table td { border: none; padding: 3px 0 3px 65px; position: relative; display: flex; align-items: center; }
  .reporter-table td:before { content: attr(data-label); position: absolute; left: 0; width: 60px; font-weight: 850; color: var(--text-secondary); font-size: 0.6rem; opacity: 0.7; }
}
</style>