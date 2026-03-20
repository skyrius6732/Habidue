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
      <div class="search-wrapper">
        <span class="search-icon">🔍</span>
        <input 
          v-model="currentFilter.keyword" 
          :placeholder="getPlaceholder"
          class="admin-search-input"
          @keyup.enter="handleFilterChange"
        />
      </div>
      <div class="filter-group">
        <select v-if="currentTab === 'reports'" v-model="reportTargetFilter" class="admin-select" @change="handleFilterChange">
          <option value="ALL">모든 신고대상</option>
          <option value="POST">게시글 신고</option>
          <option value="COMMENT">댓글 신고</option>
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
            <option value="DELETED">삭제 (DELETED)</option>
          </template>
        </select>

        <button v-if="currentFilter.userId" class="btn-clear-user" @click="clearUserFilter">
          👤 해제 (ID: {{ currentFilter.userId }})
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
      
      <!-- 신고 관리 전용 액션 (순서 조정됨) -->
      <div v-if="currentTab === 'reports'" class="bulk-actions">
        <!-- Default state -->
        <button v-if="selectedIds.length === 0" class="btn-select-all" @click="toggleSelectAll">
          체크 전체선택
        </button>
        <!-- Action state: 순서 변경 (대기/반려/블라인드/전체해제) -->
        <div v-else class="bulk-actions-group">
          <button class="btn-bulk-wait" @click="handleBulkReport('WAITING')">{{ selectedIds.length }}건 대기</button>
          <button class="btn-bulk-reject" @click="handleBulkReport('REJECTED')">{{ selectedIds.length }}건 반려</button>
          <button class="btn-bulk-blind" @click="handleBulkReport('BLIND_COMPLETE')">{{ selectedIds.length }}건 블라인드</button>
          <button class="btn-bulk-delete" @click="handleBulkReport('DELETE_COMPLETE')">{{ selectedIds.length }}건 영구 삭제</button>
          <button class="btn-select-all" @click="toggleSelectAll">전체 해제</button>
        </div>
      </div>
    </div>

    <!-- 4. 리스트 영역 -->
    <div class="content-body scrollable">
      <div v-if="currentTab === 'posts'" class="list-wrapper">
        <div v-for="post in postsState.list" :key="post.id" class="content-card" :class="{ 'is-blinded': post.status === 'BLINDED', 'is-deleted': post.status === 'DELETED' }">
          <div class="card-header">
            <span class="id-tag">#{{ post.id }}</span>
            <span class="type-badge" :class="post.type">{{ post.type }}</span>
            <span class="status-badge" :class="post.status">{{ post.status }}</span>
            <span class="date-text">{{ formatDate(post.createdAt) }}</span>
          </div>
          <div class="card-main">
            <h3 class="post-title" @click="viewPostDetail(post.id)">{{ post.title }}</h3>
            <p class="post-author">작성자: <span class="author-link" @click="setUserFilter(post.authorId)">{{ post.authorName }}</span></p>
            <div class="post-stats">
              <span>👀 {{ post.viewCount }}</span>
              <span>💬 {{ post.commentCount }}</span>
              <span>👍 {{ post.likeCount }}</span>
            </div>
          </div>
          <div class="card-actions">
            <button @click="openEditModal(post)" class="btn-action edit">수정</button>
            
            <!-- [시니어] 상태별 액션 단일화: ACTIVE가 아니면 무조건 '복구' 버튼 하나만 노출 -->
            <template v-if="isItemActive(post)">
              <button @click="toggleStatus(post)" class="btn-action blind">블라인드</button>
              <button @click="handlePermanentDelete(post)" class="btn-action delete">영구 삭제</button>
            </template>
            <template v-else>
              <button @click="toggleStatus(post)" class="btn-action restore">복구</button>
            </template>
          </div>
        </div>
      </div>

      <div v-else-if="currentTab === 'comments'" class="list-wrapper">
        <div v-for="comment in commentsState.list" :key="comment.id" class="content-card comment-card" :class="{ 'is-blinded': comment.status === 'BLINDED', 'is-deleted': comment.status === 'DELETED' }">
          <div class="card-header">
            <span class="id-tag">#{{ comment.id }}</span>
            <span class="status-badge" :class="comment.status">{{ comment.status }}</span>
            <span class="date-text">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <div class="card-main">
            <p class="comment-text" @click="viewCommentDetail(comment)">{{ comment.content }}</p>
            <p class="post-author">작성자: <span class="author-link" @click="setUserFilter(comment.authorId)">{{ comment.authorName }}</span></p>
          </div>
          <div class="card-actions">
            <button @click="openEditModal(comment)" class="btn-action edit">수정</button>

            <!-- [시니어] 상태별 액션 단일화 -->
            <template v-if="isItemActive(comment)">
              <button @click="toggleStatus(comment)" class="btn-action blind">블라인드</button>
              <button @click="handlePermanentDelete(comment)" class="btn-action delete">영구 삭제</button>
            </template>
            <template v-else>
              <button @click="toggleStatus(comment)" class="btn-action restore">복구</button>
            </template>
          </div>
        </div>
      </div>

      <div v-else-if="currentTab === 'reports'" class="list-wrapper">
        <div v-for="report in reportsState.list" :key="report.targetType + report.targetId" 
             class="content-card report-card" 
             :class="{ 'is-processed': report.reportStatus !== 'WAITING' }">
          <div class="card-header">
            <input type="checkbox" :value="report.targetType + ':' + report.targetId" v-model="selectedIds" class="report-checkbox" @click.stop />
            <span class="id-tag">#{{ report.targetId }}</span>
            <span class="type-badge" :class="report.targetType">{{ report.targetType === 'POST' ? '게시글' : '댓글' }}</span>
            <span class="report-status-badge" :class="report.reportStatus">{{ getReportStatusLabel(report.reportStatus) }}</span>
            <span class="report-count-badge clickable" @click="openReporterModal(report)">신고자 {{ report.reportCount }}명 🔍</span>
            <span class="date-text">{{ formatDate(report.latestReportDate) }}</span>
          </div>
          <div class="card-main">
            <div class="report-content-preview" @click="viewReportTarget(report)">
              <p class="target-title">[{{ report.targetStatus }}] {{ report.targetTitle }}</p>
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
      </div>

      <div v-if="currentList.length === 0" class="empty-state">검색 결과가 없습니다.</div>
    </div>

    <!-- 5. 페이지네이션 -->
    <div class="pagination-bar" v-if="totalPages > 1">
      <button :disabled="currentPage === 0" @click="changePage(0)" class="btn-page">&laquo;</button>
      <div class="page-numbers">
        <button v-for="p in visiblePages" :key="p" 
                class="btn-page-num" :class="{ active: currentPage + 1 === p }"
                @click="changePage(p - 1)">
          {{ p }}
        </button>
      </div>
      <button :disabled="currentPage === totalPages - 1" @click="changePage(totalPages - 1)" class="btn-page">&raquo;</button>
    </div>

    <!-- 수정 모달 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="isEditModalOpen" class="modal-overlay" @click="isEditModalOpen = false">
          <div class="modal-content" @click.stop>
            <div class="modal-head">
              <h3>🛠️ {{ currentTab === 'posts' ? '게시글' : '댓글' }} 수정</h3>
              <span class="modal-id-badge">ID: {{ editForm.id }}</span>
            </div>
            <div class="modal-form-body">
              <div v-if="currentTab === 'posts'" class="form-group">
                <label>게시글 제목</label>
                <input v-model="editForm.title" class="form-input" />
              </div>
              <div class="form-group">
                <label>내용</label>
                <textarea v-model="editForm.content" class="form-textarea" rows="10"></textarea>
              </div>
            </div>
            <div class="modal-actions-footer">
              <button @click="handleUpdate" class="btn-save">저장</button>
              <button @click="isEditModalOpen = false" class="btn-cancel">닫기</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- [시니어 조치] 신고자 상세 모달 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="isReporterModalOpen" class="modal-overlay" @click="isReporterModalOpen = false">
          <div class="modal-content reporter-modal" @click.stop>
            <div class="modal-head">
              <h3>👥 신고자 상세 명단</h3>
            </div>
            <div class="modal-body scrollable">
              <div class="reporter-table-wrapper">
                <table class="reporter-table">
                  <thead>
                    <tr>
                      <th>신고자 (현재 카르마)</th>
                      <th>신고 사유</th>
                      <th>신고 일시</th>
                      <th>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="rep in selectedReporters" :key="rep.reporterId">
                      <td data-label="신고자">
                        <div class="rep-name-flex">
                          <span class="rep-name">{{ rep.reporterName }}</span>
                          <span class="rep-karma" :class="getKarmaClass(rep.reporterKarma)">({{ (rep.reporterKarma / 10).toFixed(1) }}P)</span>
                        </div>
                      </td>
                      <td data-label="사유" class="rep-reason">{{ rep.reason }}</td>
                      <td data-label="일시" class="rep-date">{{ formatDate(rep.reportedAt) }}</td>
                      <td class="rep-action">
                        <button class="btn-penalize" @click="handleReporterKarmaChange(rep)">허위신고 제재</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div class="modal-actions-footer">
              <button @click="isReporterModalOpen = false" class="btn-cancel">닫기</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/plugins/axios'

const route = useRoute()
const currentTab = ref('posts')
const selectedIds = ref([]) 

const postsState = ref({ list: [], keyword: '', status: 'ALL', userId: null, page: 0, totalPages: 0, totalElements: 0 })
const commentsState = ref({ list: [], keyword: '', status: 'ALL', userId: null, page: 0, totalPages: 0, totalElements: 0 })
const reportsState = ref({ list: [], keyword: '', status: 'ALL', targetType: 'ALL', page: 0, totalPages: 0, totalElements: 0 })

const reportTargetFilter = computed({ get: () => reportsState.value.targetType, set: (val) => { reportsState.value.targetType = val } })
const currentFilter = computed(() => { if (currentTab.value === 'posts') return postsState.value; if (currentTab.value === 'comments') return commentsState.value; return reportsState.value })
const currentList = computed(() => currentFilter.value.list)
const currentPage = computed(() => currentFilter.value.page)
const totalPages = computed(() => currentFilter.value.totalPages)
const filteredCount = computed(() => currentFilter.value.totalElements)
const totalCount = ref(0)
const currentCountInfo = computed(() => `${filteredCount.value}건 / ${totalCount.value}건`)

const getPlaceholder = computed(() => { if (currentTab.value === 'posts') return '제목, 내용 또는 닉네임...'; if (currentTab.value === 'comments') return '내용 또는 닉네임...'; return '신고 사유 검색...' })
const isItemActive = (item) => item && item.status && item.status.toString().toUpperCase() === 'ACTIVE'

const fetchData = async () => {
  try {
    let endpoint = ''; let params = { page: currentFilter.value.page, size: 20 }
    if (currentTab.value === 'posts') { endpoint = '/api/admin/board/posts'; params.userId = postsState.value.userId; params.keyword = postsState.value.keyword; params.status = postsState.value.status === 'ALL' ? null : postsState.value.status }
    else if (currentTab.value === 'comments') { endpoint = '/api/admin/board/comments'; params.userId = commentsState.value.userId; params.keyword = commentsState.value.keyword; params.status = commentsState.value.status === 'ALL' ? null : commentsState.value.status }
    else { 
      endpoint = '/api/admin/board/reports'; 
      params.targetType = reportsState.value.targetType === 'ALL' ? null : reportsState.value.targetType; 
      params.status = reportsState.value.status === 'ALL' ? null : reportsState.value.status;
      params.keyword = reportsState.value.keyword; // [시니어 조치] 신고 사유 검색 키워드 추가
    }
    const res = await axios.get(endpoint, { params }); const pageData = res.data.data
    currentFilter.value.list = pageData.content; currentFilter.value.totalPages = pageData.totalPages; currentFilter.value.totalElements = pageData.totalElements
    if (!currentFilter.value.keyword && currentFilter.value.status === 'ALL') totalCount.value = pageData.totalElements
    selectedIds.value = [] 
  } catch (e) { console.error('데이터 로드 실패') }
}

const handleReportAction = async (report, newStatus) => {
  const msg = newStatus === 'BLIND_COMPLETE' ? '블라인드 처리하시겠습니까?' : (newStatus === 'REJECTED' ? '신고를 반려하시겠습니까?' : '처리 대기 상태로 되돌리시겠습니까?')
  if (!confirm(msg)) return
  try {
    await axios.patch('/api/admin/board/reports/handle', { targetId: report.targetId, targetType: report.targetType, status: newStatus })
    fetchData()
  } catch (e) { alert('처리 실패') }
}

const isAllSelected = computed(() => reportsState.value.list.length > 0 && selectedIds.value.length === reportsState.value.list.length)
const toggleSelectAll = () => { if (isAllSelected.value) selectedIds.value = []; else selectedIds.value = reportsState.value.list.map(r => `${r.targetType}:${r.targetId}`) }

const handleBulkReport = async (newStatus) => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`선택한 ${selectedIds.value.length}건을 일괄 처리하시겠습니까?`)) return
  const targetIdsByPost = selectedIds.value.filter(id => id.startsWith('POST')).map(id => Number(id.split(':')[1]))
  const targetIdsByComment = selectedIds.value.filter(id => id.startsWith('COMMENT')).map(id => Number(id.split(':')[1]))
  try {
    if (targetIdsByPost.length > 0) await axios.post('/api/admin/board/reports/bulk-handle', { targetIds: targetIdsByPost, targetType: 'POST', status: newStatus })
    if (targetIdsByComment.length > 0) await axios.post('/api/admin/board/reports/bulk-handle', { targetIds: targetIdsByComment, targetType: 'COMMENT', status: newStatus })
    alert('처리되었습니다.'); fetchData()
  } catch (e) { alert('일괄 처리 실패') }
}

const viewPostDetail = (id) => { window.open(`/board/post/${id}`, '_blank') }
const viewCommentDetail = (comment) => { window.open(`/board/post/${comment.postId}#comment-${comment.id}`, '_blank') }
const viewReportTarget = (report) => {
  if (report.targetType === 'POST') viewPostDetail(report.targetId)
  else window.open(`/board/post/${report.parentPostId}#comment-${report.targetId}`, '_blank')
}

const getReportStatusLabel = (status) => { 
  const labels = { 
    WAITING: '대기중', 
    BLIND_COMPLETE: '블라인드 완료', 
    DELETE_COMPLETE: '영구 삭제 완료',
    REJECTED: '반려됨' 
  }; 
  return labels[status] || status 
}
const changePage = (page) => { currentFilter.value.page = page; fetchData() }
const handleFilterChange = () => { currentFilter.value.page = 0; fetchData() }
const toggleStatus = async (item) => {
  const newStatus = item.status === 'ACTIVE' ? 'BLINDED' : 'ACTIVE'; const type = currentTab.value === 'posts' ? 'posts' : 'comments'
  if (!confirm(`${newStatus === 'BLINDED' ? '블라인드 처리' : '정상 상태로 복구'} 하시겠습니까?`)) return
  try { await axios.patch(`/api/admin/board/${type}/${item.id}/status`, { status: newStatus }); item.status = newStatus } catch (e) { alert('상태 변경 실패') }
}

const handlePermanentDelete = async (item) => {
  if (!confirm('이 게시물을 영구 삭제하시겠습니까? 작성자 본인에게도 더 이상 노출되지 않습니다.')) return
  const type = currentTab.value === 'posts' ? 'posts' : 'comments'
  try { 
    await axios.patch(`/api/admin/board/${type}/${item.id}/status`, { status: 'DELETED' })
    alert('영구 삭제 처리되었습니다.')
    fetchData()
  } catch (e) { alert('삭제 실패') }
}

const isEditModalOpen = ref(false); const editForm = ref({ id: null, title: '', content: '' })
const openEditModal = (item) => { if (currentTab.value === 'posts') editForm.value = { id: item.id, title: item.title, content: item.content }; else editForm.value = { id: item.id, title: '', content: item.content }; isEditModalOpen.value = true }
const handleUpdate = async () => {
  try {
    const type = currentTab.value === 'posts' ? 'posts' : 'comments'; const payload = currentTab.value === 'posts' ? { title: editForm.value.title, content: editForm.value.content } : { content: editForm.value.content }
    await axios.patch(`/api/admin/board/${type}/${editForm.value.id}`, payload); alert('수정되었습니다.'); fetchData(); isEditModalOpen.value = false
  } catch (e) { alert('수정 실패') }
}

const setUserFilter = (id) => { currentFilter.value.userId = id; handleFilterChange() }
const clearUserFilter = () => { currentFilter.value.userId = null; handleFilterChange() }
const formatDate = (dateStr) => { if (!dateStr) return '-'; const date = new Date(dateStr); return date.toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }
const visiblePages = computed(() => { const range = []; const start = Math.max(1, currentFilter.value.page - 2); const end = Math.min(totalPages.value, start + 4); for (let i = Math.max(1, start); i <= end; i++) range.push(i); return range })

watch(currentTab, () => { fetchData() })
const isReporterModalOpen = ref(false)
const selectedReporters = ref([])
const selectedTargetId = ref(null)

const openReporterModal = (report) => {
  selectedReporters.value = report.reporters || []
  selectedTargetId.value = report.targetId
  isReporterModalOpen.value = true
}

const handleReporterKarmaChange = async (reporter) => {
  const points = prompt(`신고자 '${reporter.reporterName}'님에게 허위 신고 페널티를 부여하시겠습니까?\n감점할 점수를 입력하세요 (예: -10):`, "-10")
  if (!points || isNaN(points)) return

  try {
    // [시니어 조치] FALSE_REPORT 사유로 카르마 차감
    await axios.patch(`/api/admin/users/${reporter.reporterId}/karma`, null, { 
      params: { 
        points: parseFloat(points), 
        reason: 'FALSE_REPORT',
        reasonText: `대상 ID(#${selectedTargetId.value})에 대한 악의적/허위 신고`
      } 
    })
    alert('페널티가 부여되었습니다.')
    // 점수 로컬 업데이트 (UI 반영, 10배 스케일링 적용)
    reporter.reporterKarma += Math.round(parseFloat(points) * 10)
  } catch (e) { alert('페널티 부여 실패') }
}

const getKarmaClass = (point) => {
  if (point >= 800) return 'safe'
  if (point >= 500) return 'warning'
  return 'danger'
}

onMounted(() => { if (route.query.userId) postsState.value.userId = route.query.userId; fetchData() })
</script>

<style scoped>
.admin-community-container { display: flex; flex-direction: column; height: 100%; gap: 0; }
.admin-header { margin-top: 0; margin-bottom: 5px; }
.header-main { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.header-main h2 { font-size: 1.1rem; font-weight: 800; margin: 0; color: var(--text-primary); }
.count-info { font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); }

.filter-bar { display: flex; justify-content: space-between; align-items: center; gap: 15px; flex-wrap: wrap; margin-bottom: 12px; }
.search-wrapper { flex: 1; min-width: 250px; position: relative; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.5; }
.admin-search-input { width: 100%; padding: 10px 10px 10px 35px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--header-bg); color: var(--text-primary); outline: none; }
.filter-group { display: flex; gap: 10px; align-items: center; }
.admin-select { padding: 8px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--header-bg); color: var(--text-primary); cursor: pointer; }
.btn-clear-user { padding: 6px 12px; background: #34495e; color: white; border-radius: 6px; font-size: 0.75rem; border: none; cursor: pointer; }

.tab-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid var(--border-color); margin-bottom: 15px; flex-wrap: wrap; gap: 10px; }
.tab-nav { display: flex; gap: 5px; }
.tab-btn { padding: 12px 20px; font-weight: 700; font-size: 0.95rem; cursor: pointer; background: none; border: none; color: var(--text-secondary); border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; white-space: nowrap; }
.tab-btn.active { color: var(--link-color); border-bottom-color: var(--link-color); }

/* PC 버튼 순서 및 간격 조정 */
.bulk-actions { display: flex; gap: 12px; align-items: center; padding-bottom: 5px; }
.bulk-actions-group { display: flex; gap: 8px; align-items: center; } /* PC 간격 8px로 조정 */
.btn-select-all { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: 8px; font-size: 0.8rem; font-weight: 700; cursor: pointer; color: var(--text-primary); }
.btn-bulk-blind { background: #ed4956; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
.btn-bulk-delete { background: #8e44ad; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
.btn-bulk-wait { background: #f39c12; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
.btn-bulk-reject { background: #95a5a6; color: white; border: none; padding: 8px 15px; border-radius: 8px; font-weight: 700; cursor: pointer; font-size: 0.82rem; }

.content-body { flex: 1; overflow-y: auto; }
.list-wrapper { display: grid; grid-template-columns: 1fr; gap: 15px; }
@media (min-width: 1024px) { .list-wrapper { grid-template-columns: 1fr 1fr; } }

.content-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 15px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s; }
.card-header { display: flex; align-items: center; gap: 8px; }
.report-checkbox { width: 18px; height: 18px; cursor: pointer; }
.id-tag { font-family: monospace; font-weight: 800; color: var(--link-color); font-size: 0.8rem; background: rgba(0,149,246,0.05); padding: 2px 6px; border-radius: 4px; }
.type-badge { font-size: 0.65rem; font-weight: 800; padding: 3px 8px; border-radius: 4px; background: #eee; color: #666; }
.status-badge, .report-status-badge { font-size: 0.65rem; font-weight: 800; padding: 3px 8px; border-radius: 4px; }
.status-badge.ACTIVE, .report-status-badge.BLIND_COMPLETE { background: #e8f5e9; color: #2e7d32; }
.status-badge.BLINDED { background: #fff3e0; color: #e65100; }
.report-status-badge.REJECTED { background: #ffebee; color: #c62828; }
.report-status-badge.DELETE_COMPLETE { background: rgba(142, 68, 173, 0.1); color: #8e44ad; border: 1px solid #8e44ad; }
.status-badge.DELETED { background: #8e44ad; color: #fff; font-weight: 900; }

/* [시니어] 삭제된 카드 시각적 감쇄 효과 */
.content-card.is-deleted { 
  background-color: var(--hover-bg) !important; 
  opacity: 0.6; 
  border: 1.5px dashed #8e44ad !important;
  background-image: repeating-linear-gradient(45deg, transparent, transparent 10px, rgba(142, 68, 173, 0.03) 10px, rgba(142, 68, 173, 0.03) 20px);
}
.content-card.is-deleted .card-main { filter: grayscale(0.8); }
.report-status-badge.WAITING { background: #fff3e0; color: #e65100; }
.report-count-badge { font-size: 0.65rem; font-weight: 850; padding: 3px 8px; border-radius: 4px; background: #f0f2f5; color: var(--link-color); }
.report-count-badge.clickable { cursor: pointer; border: 1px solid var(--link-color); transition: all 0.2s; }
.report-count-badge.clickable:hover { background: var(--link-color); color: white; }

/* [시니어] 신고자 모달 및 테이블 스타일 (중앙 정렬 및 컴팩트화) */
.reporter-modal { 
  max-width: 750px !important; 
  width: 95%; 
  max-height: 85vh; 
  display: flex; 
  flex-direction: column; 
  padding: 20px !important;
}
.reporter-table-wrapper { 
  width: 100%; 
  overflow-y: auto; 
  margin-top: 10px; 
  flex: 1; 
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
.reporter-table { width: 100%; border-collapse: collapse; font-size: 0.78rem; text-align: left; }
.reporter-table th { 
  background: var(--hover-bg); padding: 10px; 
  border-bottom: 2px solid var(--border-color); 
  color: var(--text-secondary); font-weight: 850; 
  position: sticky; top: 0; z-index: 10; 
}
.reporter-table td { padding: 8px 10px; border-bottom: 1px solid var(--divider-color); vertical-align: middle; }
.rep-name-flex { display: flex; align-items: center; gap: 6px; }
.rep-name { font-weight: 700; color: var(--text-primary); }
.rep-karma { font-size: 0.7rem; font-weight: 800; opacity: 0.8; }
.rep-karma.safe { color: #2ecc71; }
.rep-karma.warning { color: #f1c40f; }
.rep-karma.danger { color: #e74c3c; }
.rep-reason { color: #e67e22; font-weight: 600; line-height: 1.3; min-width: 120px; font-size: 0.75rem; }
.rep-date { font-size: 0.7rem; color: var(--text-muted); white-space: nowrap; }
.btn-penalize { 
  background: none; border: 1.5px solid #ed4956; color: #ed4956; 
  padding: 3px 8px; border-radius: 5px; font-size: 0.7rem; 
  font-weight: 800; cursor: pointer; transition: all 0.2s; white-space: nowrap; 
}
.btn-penalize:hover { background: #ed4956; color: white; }

@media (max-width: 768px) {
  .reporter-modal { padding: 15px !important; width: 98%; }
  .reporter-table th, .reporter-table td { padding: 8px 6px; font-size: 0.7rem; }
  .rep-name-flex { flex-direction: column; align-items: flex-start; gap: 0; }
  .btn-penalize { padding: 2px 5px; font-size: 0.65rem; }
}

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
.btn-action.delete { background: #8e44ad; color: white; } /* 보라색 통일 */
.btn-action.delete:hover { background: #7d3c98; }

/* [시니어 조치] 다크모드 전용 버튼 색상 고도화 */
[data-theme='dark'] .btn-action.edit { 
  background: #ffffff; 
  color: #000000; 
  border-color: #ffffff; 
  font-weight: 800;
}
[data-theme='dark'] .btn-action.edit:hover { 
  background: #e0e0e0; 
}

.pagination-bar { display: flex; justify-content: center; align-items: center; gap: 15px; padding: 20px 0; }
.page-numbers { display: flex; gap: 5px; }
.btn-page { padding: 8px 12px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; cursor: pointer; color: var(--text-secondary); }
.btn-page-num { width: 36px; height: 36px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; cursor: pointer; font-weight: 700; color: var(--text-secondary); }
.btn-page-num.active { background: var(--link-color); color: white; border-color: var(--link-color); }

.modal-overlay { 
  position: fixed; top: 0; left: 0; 
  width: 100vw; height: 100vh; 
  background: rgba(0,0,0,0.7); 
  display: flex; align-items: center; justify-content: center; 
  z-index: 99999; 
  backdrop-filter: blur(5px); 
}
.modal-content { 
  background: var(--card-bg); width: 95%; max-width: 650px; 
  border-radius: 20px; display: flex; flex-direction: column; 
  box-shadow: 0 30px 60px rgba(0,0,0,0.3); border: 1px solid var(--border-color);
  max-height: 85vh; /* 전체 화면의 85%로 제한 */
}

.modal-head { padding: 25px 30px 15px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--divider-color); }
.modal-head h3 { margin: 0; font-size: 1.2rem; font-weight: 800; color: var(--text-primary); }
.modal-form-body, .modal-body { flex: 1; min-height: 0; overflow-y: auto; padding: 20px 30px; }
.modal-actions-footer { padding: 15px 30px 25px; display: flex; gap: 12px; justify-content: center; }

/* [시니어 조치] 모달 내부 폼 스타일 복구 */
.modal-id-badge { font-size: 0.75rem; background: var(--hover-bg); padding: 4px 10px; border-radius: 6px; color: var(--text-muted); font-weight: 600; }
.form-group { display: flex; flex-direction: column; gap: 8px; margin-bottom: 15px; }
.form-group label { font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); }
.form-input { padding: 12px 15px; border-radius: 10px; border: 1px solid var(--border-color); background: var(--hover-bg); color: var(--text-primary); font-size: 1rem; font-weight: 600; outline: none; transition: all 0.2s; }
.form-input:focus { border-color: var(--link-color); background: var(--card-bg); }
.form-textarea { padding: 15px; border-radius: 10px; border: 1px solid var(--border-color); background: var(--hover-bg); color: var(--text-primary); font-size: 1rem; line-height: 1.6; outline: none; resize: vertical; min-height: 200px; transition: all 0.2s; }
.form-textarea:focus { border-color: var(--link-color); background: var(--card-bg); }

.btn-save { flex: 2; padding: 14px; background: var(--link-color); color: white; border: none; border-radius: 10px; font-size: 1rem; font-weight: 800; cursor: pointer; transition: opacity 0.2s; }
.btn-save:hover { opacity: 0.9; }
.btn-cancel { flex: 1; padding: 14px; background: var(--hover-bg); color: var(--text-primary); border: 1px solid var(--border-color); border-radius: 10px; font-size: 1rem; font-weight: 700; cursor: pointer; transition: background 0.2s; }
.btn-cancel:hover { background: var(--border-color); }

.reporter-modal .btn-cancel { 
  flex: 0 0 120px; padding: 12px; 
  background: var(--hover-bg); color: var(--text-primary); 
  border: 1px solid var(--border-color); border-radius: 10px; 
  font-size: 0.9rem; font-weight: 700; cursor: pointer;
}

/* [시니어] 신고자 모달 특화 스타일 (스크롤 트리거) */
.reporter-modal { max-width: 800px !important; }
.reporter-table-wrapper { width: 100%; border: 1px solid var(--border-color); border-radius: 10px; background: var(--card-bg); overflow: hidden; }
.reporter-table { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 0.78rem; text-align: left; }
.reporter-table th { background: var(--hover-bg); padding: 12px 10px; border-bottom: 2px solid var(--border-color); color: var(--text-secondary); font-weight: 850; position: sticky; top: 0; z-index: 20; }
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

@media (max-width: 768px) {
  .modal-content { max-height: 92vh; width: 98%; }
  .modal-head { padding: 15px 15px 10px; }
  .modal-head h3 { font-size: 1rem; }
  .modal-body { padding: 10px 12px; }
  .modal-actions-footer { padding: 10px 15px 15px; }
  .reporter-modal .btn-cancel { flex: 1; padding: 10px; font-size: 0.85rem; }
  
  /* 모바일 초컴팩트 카드 전환 */
  .reporter-table, .reporter-table thead, .reporter-table tbody, .reporter-table th, .reporter-table td, .reporter-table tr { display: block; }
  .reporter-table thead { display: none; }
  .reporter-table tr {
    border: 1px solid var(--border-color);
    border-radius: 6px;
    margin-bottom: 6px; 
    padding: 8px 10px;
    background: var(--header-bg);
  }
  .reporter-table td {
    border: none;
    padding: 3px 0 3px 65px;
    position: relative;
    min-height: 24px;
    display: flex;
    align-items: center;
    font-size: 0.72rem;
  }
  .reporter-table td:before {
    content: attr(data-label);
    position: absolute;
    left: 0;
    width: 60px;
    font-weight: 850;
    color: var(--text-secondary);
    font-size: 0.6rem;
    opacity: 0.7;
  }
  .reporter-table td.rep-action { padding-left: 0; justify-content: flex-end; border-top: 1px dashed var(--divider-color); margin-top: 6px; padding-top: 8px; }
  .reporter-table td.rep-action:before { display: none; }
  
  .rep-name-flex { flex-direction: row; gap: 5px; }
  .rep-name { font-size: 0.75rem; }
  .rep-reason { min-width: 0; white-space: normal; text-align: left; font-size: 0.72rem; }
}

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
.btn-action.delete { background: #8e44ad; color: white; } /* 보라색 통일 */
.btn-action.delete:hover { background: #7d3c98; }

/* [시니어 조치] 다크모드 전용 버튼 색상 고도화 */
[data-theme='dark'] .btn-action.edit { 
  background: #ffffff; 
  color: #000000; 
  border-color: #ffffff; 
  font-weight: 800;
}
[data-theme='dark'] .btn-action.edit:hover { 
  background: #e0e0e0; 
}
.pagination-bar { display: flex; justify-content: center; align-items: center; gap: 15px; padding: 20px 0; }
.page-numbers { display: flex; gap: 5px; }
.btn-page { padding: 8px 12px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; cursor: pointer; color: var(--text-secondary); }
.btn-page-num { width: 36px; height: 36px; border: 1px solid var(--border-color); background: var(--card-bg); border-radius: 8px; cursor: pointer; font-weight: 700; color: var(--text-secondary); }
.btn-page-num.active { background: var(--link-color); color: white; border-color: var(--link-color); }
.empty-state { text-align: center; padding: 50px; color: var(--text-secondary); font-size: 0.9rem; }

/* --- 모바일 대응 (메인 리스트) --- */
@media (max-width: 992px) {
  .header-main { flex-direction: column; align-items: stretch; gap: 10px; }
  .filter-bar { flex-direction: column; align-items: stretch; gap: 10px; }
  .search-wrapper { min-width: 100%; }
  .filter-group { 
    width: 100%; 
    display: flex; 
    flex-wrap: wrap;
    justify-content: flex-end; 
    gap: 8px; 
  }
  .admin-select { width: auto; min-width: 140px; font-size: 0.85rem; padding: 8px; }
  .btn-clear-user { grid-column: span 2; }
  .tab-header { flex-direction: column; align-items: flex-start; border-bottom: none; gap: 0; }
  .tab-nav { width: 100%; border-bottom: 2px solid var(--border-color); justify-content: space-between; display: flex; }
  .tab-btn { flex: 1; padding: 12px 5px; font-size: 0.82rem; text-align: center; }
  .bulk-actions { width: 100%; flex-direction: column; align-items: stretch; margin-top: 12px; gap: 10px; padding: 0 5px; }
  .btn-select-all { width: 100%; padding: 10px; font-size: 0.85rem; }
  .bulk-actions-group { width: 100%; display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .list-wrapper { grid-template-columns: 1fr; gap: 10px; }
  .content-card { padding: 12px; }
  .post-title, .target-title { font-size: 0.92rem; }
  .card-header { flex-wrap: wrap; gap: 5px; }
  .date-text { width: 100%; margin-top: 4px; font-size: 0.7rem; border-top: 1px solid var(--hover-bg); padding-top: 4px; }
  .btn-action { padding: 8px; font-size: 0.8rem; }
  .pc-only { display: none; }
}
</style>
