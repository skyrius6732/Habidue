<template>
  <div class="admin-notice-container">
    <div class="admin-header">
      <div class="header-main">
        <h2>🏠 공고 데이터 관리</h2>
        <span class="count-info">{{ notices.length }}건 / {{ totalElements }}건</span>
      </div>
      
      <div class="search-filter-bar">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input 
            v-model="filters.keyword" 
            placeholder="제목 또는 내용 검색..." 
            class="admin-search-input"
            @keyup.enter="handleFilterChange" 
          />
        </div>
        <select v-model="filters.source" class="admin-filter-select" @change="handleFilterChange">
          <option value="ALL">모든 출처</option>
          <option value="LH">LH</option>
          <option value="SH">SH</option>
          <option value="HUG">HUG</option>
          <option value="PRIVATE">민간</option>
        </select>
        <select v-model="filters.status" class="admin-filter-select" @change="handleFilterChange">
          <option value="ALL">모든 상태</option>
          <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <button class="btn-new" @click="openCreateModal">➕ 등록</button>
        <button class="btn-reprocess" @click="handleReprocessTags" :disabled="isReprocessing" title="전체 공고 태그 및 상태 재설정">
          {{ isReprocessing ? '⏳' : '🔄 태그 재설정' }}
        </button>
      </div>
    </div>

    <!-- 공고 테이블 영역 (PC: 테이블 / Mobile: 카드형) -->
    <div class="table-outer-wrapper scrollable">
      <div class="table-wrapper">
        <table class="notice-table">
          <thead>
            <tr>
              <th class="col-check">
                <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
              </th>
              <th class="col-id">ID</th>
              <th class="col-source">출처</th>
              <th class="col-title">제목</th>
              <th class="col-date">공고일</th>
              <th class="col-date">마감일</th>
              <th class="col-status">상태</th>
              <th class="col-actions">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="notice in notices" :key="notice.id" class="notice-row" :class="{ selected: selectedIds.includes(notice.id) }">
              <td class="col-check check-cell">
                <input type="checkbox" :checked="selectedIds.includes(notice.id)" @change="toggleSelect(notice.id)" @click.stop />
              </td>
              <td class="col-id id-cell">{{ notice.id }}</td>
              <td class="col-source source-cell">
                <span class="source-badge" :class="notice.source === '민간' ? 'PRIVATE' : notice.source">
                  {{ notice.source === 'PRIVATE' || notice.source === 'private' || notice.source === '민간' ? '민간' : notice.source }}
                </span>
              </td>
              <td class="title-cell col-title">
                <div class="notice-title-wrapper" 
                     @touchstart.passive="handleTouchStart(notice)" 
                     @touchend="handleTouchEnd($event)"
                     @mousedown="handleTouchStart(notice)"
                     @mouseup="handleTouchEnd($event)">
                  <span class="notice-title" @click="openEditModal(notice)" :title="notice.title">{{ notice.title }}</span>
                  <a :href="notice.link" target="_blank" class="link-icon" @click.stop>🔗</a>
                </div>
              </td>
              <td class="col-date date-text announcement-date">
                <span class="mobile-label">공고일: </span>{{ formatDate(notice.announcementDate) }}
              </td>
              <td class="col-date date-text deadline-date">
                <span class="mobile-label">마감일: </span>{{ formatDate(notice.deadline) }}
              </td>
              <td class="col-status status-cell">
                <select :value="notice.status" @change="updateStatus(notice.id, $event.target.value)" class="status-select" @click.stop>
                  <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
                </select>
              </td>
              <td class="actions col-actions action-cell">
                <button @click.stop="openEditModal(notice)" class="btn-edit">수정</button>
                <button @click.stop="handleDelete(notice.id)" class="btn-delete">삭제</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 일괄 삭제 플로팅 바 -->
    <Transition name="slide-up">
      <div v-if="selectedIds.length > 0" class="floating-bulk-bar">
        <div class="bulk-info">
          <span class="selected-count"><strong>{{ selectedIds.length }}</strong>개 선택됨</span>
          <button class="btn-clear-selection" @click="selectedIds = []">선택 해제</button>
        </div>
        <button class="btn-bulk-delete" @click="handleBulkDelete">일괄 삭제</button>
      </div>
    </Transition>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="pagination">
      <button :disabled="currentPage === 0" @click="changePage(0)" class="btn-page first-last">처음</button>
      <button :disabled="currentPage === 0" @click="changePage(currentPage - 1)" class="btn-page">이전</button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)" class="btn-page">다음</button>
      <button :disabled="currentPage >= totalPages - 1" @click="changePage(totalPages - 1)" class="btn-page first-last">마지막</button>
    </div>

    <!-- 모바일 롱프레스 프리뷰 -->
    <Teleport to="body" v-if="previewNotice">
      <Transition name="fade" appear>
        <div class="preview-overlay" @click.self="previewNotice = null">
          <div class="preview-card">
            <div class="preview-header">
              <span class="source-badge" :class="previewNotice.source === '민간' ? 'PRIVATE' : previewNotice.source">
                {{ previewNotice.source === 'PRIVATE' || previewNotice.source === 'private' || previewNotice.source === '민간' ? '민간' : previewNotice.source }}
              </span>
            </div>
            <h4 class="preview-title">{{ previewNotice.title }}</h4>
            <div class="preview-content scrollable">
              <div v-html="previewNotice.content" class="content-html"></div>
            </div>
            <div class="preview-footer">
              <button class="btn-edit-full" @click="openEditModal(previewNotice); previewNotice = null">상세 수정하기</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 모달 -->
    <Teleport to="body" v-if="showModal">
      <Transition name="fade" appear>
        <div class="modal-overlay" @click.self="showModal = false">
          <div class="modal-content">
            <h3>{{ editingId ? '공고 수정' : '신규 공고 등록' }}</h3>
            <div class="form-grid">
              <div class="form-group full">
                <label>제목</label>
                <input v-model="form.title" class="form-input" />
              </div>
              <div class="form-group">
                <label>출처</label>
                <select v-model="form.source" class="form-select">
                  <option value="LH">LH</option>
                  <option value="SH">SH</option>
                  <option value="HUG">HUG</option>
                  <option value="PRIVATE">민간</option>
                </select>
              </div>
              <div class="form-group">
                <label>공고일</label>
                <input type="date" v-model="form.announcementDate" class="form-input" />
              </div>
              <div class="form-group">
                <label>마감일</label>
                <input type="date" v-model="form.deadline" class="form-input" />
              </div>
              <div class="form-group">
                <label>결과 발표일</label>
                <input type="date" v-model="form.resultDate" class="form-input" />
              </div>
              <div class="form-group full">
                <label>원문 링크</label>
                <input v-model="form.link" class="form-input" />
              </div>
              <div class="form-group full">
                <label>공고 내용 (HTML 또는 텍스트)</label>
                <textarea v-model="form.content" class="form-textarea"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button @click="handleSave" class="btn-save">{{ editingId ? '수정 완료' : '등록 완료' }}</button>
              <button @click="showModal = false" class="btn-cancel">취소</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from '@/plugins/axios'

const notices = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)

const filters = ref({
  keyword: '',
  source: 'ALL',
  status: 'ALL'
})

const statusOptions = [
  { value: 'RECRUITING', label: '접수중' },
  { value: 'CLOSED', label: '마감' },
  { value: 'RESULT', label: '결과발표' },
  { value: 'RESULT_COMPLETED', label: '발표완료' },
  { value: 'INFO', label: '안내' },
  { value: 'EXPIRED_INFO', label: '이전안내' }
]

const showModal = ref(false)
const editingId = ref(null)
const previewNotice = ref(null)
const isReprocessing = ref(false)
const selectedIds = ref([])
let touchTimer = null

const isAllSelected = computed(() => {
  return notices.value.length > 0 && notices.value.every(n => selectedIds.value.includes(n.id))
})

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = notices.value.map(n => n.id)
  }
}

const handleBulkDelete = async () => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`선택한 ${selectedIds.value.length}개의 공고를 정말 삭제하시겠습니까?\n연관된 모든 데이터가 함께 삭제됩니다.`)) return
  
  try {
    await axios.delete('/api/admin/notices/bulk', { data: selectedIds.value })
    alert('일괄 삭제되었습니다.')
    selectedIds.value = []
    fetchNotices()
  } catch (e) {
    alert('일괄 삭제 실패')
    console.error(e)
  }
}

const form = ref({
  title: '',
  source: 'LH',
  link: '',
  content: '',
  announcementDate: '',
  deadline: '',
  resultDate: ''
})

const handleReprocessTags = async () => {
  if (!confirm('모든 공고의 태그와 상태를 현재 규칙(메타데이터/날짜)에 맞춰 재설정하시겠습니까?\n데이터 양에 따라 시간이 걸릴 수 있습니다.')) return
  
  isReprocessing.value = true
  try {
    await axios.post('/api/admin/notices/reprocess-tags')
    alert('모든 공고의 태그 및 상태 재설정이 완료되었습니다.')
    fetchNotices()
  } catch (e) {
    alert('재설정 처리 중 오류가 발생했습니다.')
    console.error(e)
  } finally {
    isReprocessing.value = false
  }
}

const handleTouchStart = (notice) => {
  touchTimer = setTimeout(() => {
    previewNotice.value = notice
  }, 600)
}

const handleTouchEnd = (e) => {
  clearTimeout(touchTimer)
  if (previewNotice.value) {
    const touch = e.changedTouches ? e.changedTouches[0] : e
    const element = document.elementFromPoint(touch.clientX, touch.clientY)
    if (element && element.classList.contains('btn-edit-full')) {
      openEditModal(previewNotice.value)
    }
  }
}

const fetchNotices = async (resetPage = false) => {
  try {
    if (resetPage === true) {
      currentPage.value = 0
    }
    const params = {
      page: currentPage.value,
      size: 20,
      keyword: filters.value.keyword,
      sources: filters.value.source === 'ALL' ? null : [filters.value.source],
      statuses: filters.value.status === 'ALL' ? null : [filters.value.status]
    }
    const res = await axios.get('/api/admin/notices', { params })
    notices.value = res.data.data.content
    totalElements.value = res.data.data.totalElements
    totalPages.value = res.data.data.totalPages
  } catch (e) { console.error(e) }
}

const handleFilterChange = () => {
  fetchNotices(true)
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

const changePage = (page) => {
  currentPage.value = page
  fetchNotices()
}

const updateStatus = async (id, status) => {
  try {
    await axios.patch(`/api/admin/notices/${id}/status`, null, { params: { status } })
    fetchNotices()
  } catch (e) { alert('상태 변경 실패') }
}

const handleDelete = async (id) => {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await axios.delete(`/api/admin/notices/${id}`)
    fetchNotices()
  } catch (e) { alert('삭제 실패') }
}

const openCreateModal = () => {
  editingId.value = null
  form.value = { title: '', source: 'LH', link: '', content: '', announcementDate: '', deadline: '', resultDate: '' }
  showModal.value = true
}

const openEditModal = (notice) => {
  editingId.value = notice.id
  form.value = { 
    ...notice,
    announcementDate: notice.announcementDate ? notice.announcementDate.split('T')[0] : '',
    deadline: notice.deadline ? notice.deadline.split('T')[0] : '',
    resultDate: notice.resultDate ? notice.resultDate.split('T')[0] : ''
  }
  showModal.value = true
}

const handleSave = async () => {
  try {
    const payload = { ...form.value }
    const suffix = 'T23:59:59';
    if (payload.announcementDate && !payload.announcementDate.includes('T')) payload.announcementDate += suffix;
    if (payload.deadline && !payload.deadline.includes('T')) payload.deadline += suffix;
    if (payload.resultDate && !payload.resultDate.includes('T')) payload.resultDate += suffix;

    if (editingId.value) {
      await axios.put(`/api/admin/notices/${editingId.value}`, payload)
    } else {
      await axios.post('/api/admin/notices', payload)
    }
    alert('저장되었습니다.')
    showModal.value = false
    fetchNotices()
  } catch (e) { 
    console.error(e)
    alert('저장 실패: ' + (e.response?.data?.message || '알 수 없는 오류')) 
  }
}

onMounted(fetchNotices)
</script>

<style scoped>
.admin-notice-container { display: flex; flex-direction: column; gap: 20px; height: 100%; overflow: hidden; }

.admin-header { flex-shrink: 0; margin-bottom: 8px; }
.header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.header-main h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: var(--text-primary); }
.count-info { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; }

.search-filter-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; align-items: center; }
.search-input-wrapper { flex: 1; min-width: 200px; position: relative; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.4; color: var(--text-primary); pointer-events: none; }
.admin-search-input { width: 100%; height: 38px; padding: 0 12px 0 35px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.85rem; background: var(--header-bg); color: var(--text-primary); transition: border-color 0.2s; outline: none; }
.admin-search-input:focus { border-color: var(--link-color); }
.admin-filter-select { padding: 0 10px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.82rem; min-width: 110px; height: 38px; background: var(--header-bg); color: var(--text-primary); outline: none; cursor: pointer; }

.btn-reprocess {
  height: 38px;
  padding: 0 12px;
  background: var(--header-bg);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.btn-reprocess:hover { background: var(--hover-bg); border-color: var(--link-color); }
.btn-reprocess:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-new { 
  height: 38px; 
  padding: 0 15px; 
  background: var(--hover-bg); 
  border: 1px solid var(--border-color); 
  color: var(--text-primary); 
  border-radius: 8px; 
  font-weight: 700; 
  font-size: 0.82rem; 
  cursor: pointer; 
  white-space: nowrap; 
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 5px;
}
.btn-new:hover { background: var(--border-color); }

/* 테이블 영역 최적화 (PC) */
.table-outer-wrapper { flex: 1; min-height: 0; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; overflow-y: auto; position: relative; margin-bottom: 5px; }
.table-wrapper { width: 100%; }
.notice-table { width: 100%; border-collapse: collapse; table-layout: fixed; border: none; }
.notice-table thead { position: sticky; top: 0; z-index: 10; background: var(--hover-bg); }
.notice-table th { background: var(--hover-bg); padding: 10px 8px; text-align: center; font-size: 0.8rem; font-weight: 800; color: var(--text-secondary); border-bottom: 1px solid var(--border-color); }
.notice-table td { padding: 0 8px; height: 48px; border-bottom: 1px solid var(--border-color); font-size: 0.82rem; color: var(--text-primary); vertical-align: middle; box-sizing: border-box; text-align: center; }

.col-check { width: 40px; text-align: center; }
.col-id { width: 50px; }
.col-source { width: 70px; }
.col-title { width: auto; } 
.col-date { width: 100px; }
.col-status { width: 90px; }
.col-actions { width: 110px; }

.check-cell { padding: 0 !important; }
.check-cell input { width: 18px; height: 18px; cursor: pointer; accent-color: var(--link-color); }

.notice-row.selected { background-color: rgba(0, 149, 246, 0.05); }

/* 플로팅 바 스타일 (PC 전용 권장) */
.floating-bulk-bar {
  position: fixed;
  bottom: 30px;
  left: calc(50% + 100px); /* 사이드바 고려 중앙 정렬 */
  transform: translateX(-50%);
  background: var(--header-bg);
  border: 1px solid var(--border-color);
  border-radius: 50px;
  padding: 10px 25px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
  z-index: 1000;
  min-width: 300px;
  justify-content: space-between;
}
.selected-count { font-size: 0.9rem; color: var(--text-primary); }
.selected-count strong { color: var(--link-color); }
.btn-clear-selection { background: none; border: none; color: var(--text-secondary); font-size: 0.8rem; cursor: pointer; text-decoration: underline; }
.btn-bulk-delete { background: #ed4956; color: white; border: none; padding: 8px 20px; border-radius: 20px; font-weight: 800; font-size: 0.85rem; cursor: pointer; }

/* 애니메이션 */
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s ease; }
.slide-up-enter-from, .slide-up-leave-to { transform: translate(-50%, 100px); opacity: 0; }

.date-text { font-size: 0.75rem !important; letter-spacing: -0.5px; }
.mobile-label { display: none; } /* PC에선 숨김 */

/* 가로 구분선 1px 보정 */
@media (min-width: 769px) {
  .notice-table td:last-child { border-bottom: 1px solid transparent; }
  .notice-table td:last-child::after { content: ""; position: absolute; left: 0; right: 0; bottom: -2px; height: 1px; background-color: var(--border-color); }
}

.source-badge { display: inline-block; padding: 3px 4px; border-radius: 4px; font-size: 0.65rem; font-weight: 900; min-width: 45px; text-align: center; line-height: 1.2; vertical-align: middle; color: white; }
.source-badge.LH { background: #38a169; }
.source-badge.SH { background: #3182ce; }
.source-badge.PRIVATE { background: #c08457; }

.title-cell { padding: 0 12px !important; text-align: left !important; }
.notice-title-wrapper { display: flex; align-items: center; gap: 6px; height: 48px; width: 100%; user-select: none; }
.notice-title { font-weight: 700; cursor: pointer; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 48px; color: var(--text-primary); }
.notice-title:hover { color: var(--link-color); text-decoration: underline; }
.link-icon { text-decoration: none; opacity: 0.5; flex-shrink: 0; font-size: 0.85rem; display: flex; align-items: center; height: 48px; }

.status-select { width: 100%; padding: 2px 4px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--hover-bg); color: var(--text-primary); font-size: 0.72rem; font-weight: 700; outline: none; cursor: pointer; height: 26px; }

.actions { display: flex; gap: 4px; justify-content: center; align-items: center; height: 48px; position: relative; }
.btn-edit, .btn-delete { flex: 1; height: 26px; border-radius: 6px; font-size: 0.68rem; font-weight: 700; cursor: pointer; text-align: center; background: none; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.btn-edit { border: 1px solid var(--link-color); color: var(--link-color); }
.btn-edit:hover { background: var(--link-color); color: white; }
.btn-delete { border: 1px solid #ed4956; color: #ed4956; }
.btn-delete:hover { background: #ed4956; color: white; }

.pagination { flex-shrink: 0; height: 34px; display: flex; justify-content: center; align-items: center; gap: 8px; padding: 0; margin-top: 0; }
.btn-page { padding: 3px 8px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--header-bg); font-size: 0.7rem; cursor: pointer; color: var(--text-primary); transition: all 0.2s; }
.btn-page:hover:not(:disabled) { background: var(--hover-bg); border-color: var(--link-color); color: var(--link-color); }
.btn-page:disabled { opacity: 0.4; cursor: default; }
.first-last { background: var(--hover-bg); }
.page-info { font-size: 0.75rem; font-weight: 700; color: var(--text-primary); min-width: 50px; text-align: center; }

.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

@media (max-width: 1024px) {
  .col-date:first-of-type { display: none; } 
  .col-date { width: 90px; }
}

/* ★ 모바일 카드형 레이아웃 대전환 ★ */
@media (max-width: 768px) {
  .modal-overlay { 
    padding: 20px; /* 상하좌우 여백 추가 */
    align-items: center; /* 중앙 정렬로 변경 */
  }
  .modal-content {
    width: 100% !important;
    height: auto !important;
    max-height: 85vh !important; /* 화면의 85%만 차지 */
    border-radius: 16px !important; /* 부드러운 라운드 */
    box-shadow: 0 20px 50px rgba(0,0,0,0.3) !important;
    display: flex;
    flex-direction: column;
    border: 1px solid var(--border-color);
    overflow: hidden;
  }
  .modal-content h3 { 
    padding: 12px 18px; 
    font-size: 0.95rem; 
    font-weight: 800;
    text-align: center;
  }
  .form-grid { 
    padding: 12px 18px;
    gap: 10px;
  }
  .form-group { margin-bottom: 10px; }
  .form-group label { font-size: 0.7rem; margin-bottom: 2px; opacity: 0.8; }
  .form-input, .form-select { 
    height: 36px; /* 높이 축소 */
    padding: 0 10px; 
    font-size: 0.82rem; 
    border-radius: 8px;
  }
  .form-textarea { 
    min-height: 100px; 
    height: 120px; 
    font-size: 0.82rem;
    border-radius: 8px;
  } 
  .modal-footer { 
    padding: 12px 18px; 
    gap: 8px;
  }
  .btn-save, .btn-cancel { 
    height: 40px; /* 버튼 높이 축소 */
    font-size: 0.85rem; 
    border-radius: 8px;
  }
  
  .table-outer-wrapper { background: none; border: none; overflow-y: visible; }
  .notice-table, .notice-table thead, .notice-table tr, .notice-table td {
    display: block;
    width: 100% !important;
  }
  
  .notice-table thead { display: none; } /* 헤더 숨김 */

  /* 1행 2열 그리드 적용 */
  .notice-table tbody {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    padding-bottom: 20px;
  }
  
  .notice-row {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 10px;
    margin-bottom: 0; /* 그리드 갭으로 대체 */
    padding: 10px;
    box-sizing: border-box;
    box-shadow: 0 1px 4px rgba(0,0,0,0.04);
    display: flex;
    flex-direction: column;
    min-width: 0; /* 그리드 내 오버플로 방지 */
    position: relative;
  }

  .notice-table td {
    padding: 0;
    height: auto;
    border: none;
    text-align: left;
    display: flex;
    align-items: center;
    width: 100% !important;
  }

  /* 모바일에서 체크박스 숨김 (전체 영역 클릭으로 대체) */
  .check-cell { display: none !important; }

  /* ID 및 날짜 제거 */
  .id-cell, .announcement-date, .deadline-date { 
    display: none !important;
  }
  
  /* 상단: 기관(왼쪽) + 상태(오른쪽) */
  .source-cell { 
    display: flex; 
    width: auto !important; 
    margin-bottom: 6px;
    flex-shrink: 0;
  }

  .status-cell { 
    margin-left: auto; 
    width: auto !important;
    margin-bottom: 6px;
    flex-shrink: 0;
  }
  .status-select { 
    width: 65px; /* 폭이 좁으므로 더 축소 */
    height: 20px; 
    font-size: 0.65rem; 
    padding: 0 2px;
  }

  /* 제목: 2줄 제한 및 폰트 축소 */
  .title-cell {
    padding: 4px 0 !important;
    margin-bottom: 8px;
    border: none !important;
    flex: 1; /* 제목 영역이 가변적으로 확장 */
  }
  .notice-title-wrapper { height: auto; min-height: 32px; gap: 3px; align-items: flex-start; }
  .notice-title { 
    white-space: normal; 
    font-size: 0.8rem; 
    line-height: 1.25; 
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-weight: 700;
    word-break: break-all;
  }
  .link-icon { font-size: 0.65rem; height: auto; opacity: 0.6; margin-top: 2px; }

  /* 관리 버튼: 좁은 폭을 고려하여 나란히 배치 */
  .action-cell {
    margin-top: auto;
    display: flex;
    gap: 4px;
    border-top: 1px dashed var(--border-color) !important;
    padding-top: 8px !important;
  }
  .btn-edit, .btn-delete { height: 24px; font-size: 0.68rem; border-radius: 5px; padding: 0; }

  .admin-header { gap: 10px; margin-bottom: 5px; }
  .header-main { flex-direction: row; margin-bottom: 8px; }
  
  /* 필터 바 3단 구조 구성 (가독성 극대화) */
  .search-filter-bar { 
    display: grid;
    grid-template-columns: repeat(12, 1fr); 
    gap: 8px; 
    margin-bottom: 12px; 
  }

  /* 1행: 검색창 (전체) */
  .search-input-wrapper { 
    grid-column: span 12;
    display: flex;
    width: 100%;
  }
  .admin-search-input { width: 100%; padding-left: 30px; height: 38px; }
  .search-icon { left: 10px; }
  
  /* 2행: 출처(6) + 상태(6) */
  .admin-filter-select { 
    grid-column: span 6;
    width: 100%;
    height: 38px;
    font-size: 0.8rem;
    padding: 0 8px;
  }

  /* 3행: 등록(6) + 재설정(6) */
  .btn-new { 
    grid-column: span 6;
    height: 38px; 
    padding: 0; 
    font-size: 0.82rem; 
    justify-content: center;
    width: 100%;
  }

  .btn-reprocess {
    grid-column: span 6;
    height: 38px;
    padding: 0;
    font-size: 0.82rem;
    width: 100%;
  }
  
  .pagination { margin-top: 15px; }
}

/* MODAL STYLES */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(5px);
  z-index: 20000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  width: 90vw;
  max-width: 800px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0,0,0,0.3);
  overflow: hidden;
}

.modal-content h3 {
  flex-shrink: 0;
  padding: 20px 25px;
  margin: 0;
  font-size: 1.1rem;
  font-weight: 800;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}

.form-grid {
  flex: 1;
  overflow-y: auto;
  padding: 20px 25px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px 20px;
}

.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group.full { grid-column: 1 / -1; }
.form-group label { font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); }

.form-input, .form-select, .form-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--border-color);
  background: var(--header-bg);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 0.9rem;
  outline: none;
}
.form-input:focus, .form-select:focus, .form-textarea:focus { border-color: var(--link-color); }
.form-textarea { min-height: 200px; resize: vertical; }

.modal-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 15px 25px;
  border-top: 1px solid var(--border-color);
  background: var(--hover-bg);
}

.btn-save, .btn-cancel {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  border: none;
}
.btn-save { background-color: var(--link-color); color: white; }
.btn-save:hover { opacity: 0.9; }
.btn-cancel { background-color: var(--border-color); color: var(--text-secondary); }
.btn-cancel:hover { background-color: var(--hover-bg); }

/* Preview Card */
.preview-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(8px);
  z-index: 21000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}
.preview-card {
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0,0,0,0.5);
}
.preview-header { padding: 10px 15px; border-bottom: 1px solid var(--border-color); }
.preview-title { padding: 15px; margin: 0; font-size: 1.1rem; color: var(--text-primary); }
.preview-content { flex: 1; min-height: 0; padding: 0 15px; overflow-y: auto; }
.content-html { font-size: 0.9rem; line-height: 1.6; color: var(--text-primary); }
.preview-footer { padding: 10px; text-align: center; border-top: 1px solid var(--border-color); }
.btn-edit-full { width: 100%; background: var(--link-color); color: white; border: none; padding: 12px; border-radius: 8px; font-size: 0.9rem; font-weight: 700; cursor: pointer; }
</style>