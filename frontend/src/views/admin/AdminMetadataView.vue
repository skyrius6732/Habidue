<template>
  <div class="admin-metadata-container">
    <div class="admin-header">
      <div class="header-main">
        <h2>🗂️ 메타데이터 관리</h2>
        <span class="count-info">{{ filteredMetadata.length }}건 / {{ metadataList.length }}건</span>
      </div>
      
      <div class="search-filter-bar">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input 
            v-model="searchQuery" 
            placeholder="키워드 또는 대표명칭 검색..." 
            class="admin-search-input"
          />
        </div>
        <button class="btn-new" @click="isCreating = !isCreating">
          {{ isCreating ? '닫기' : '➕ 신규 추가' }}
        </button>
      </div>

      <!-- 타입별 칩 필터 -->
      <div class="type-filter-chips scrollable-x">
        <button 
          class="chip-btn" 
          :class="{ active: selectedType === 'ALL' }"
          @click="selectedType = 'ALL'"
        >
          전체 <span class="chip-count">{{ metadataList.length }}</span>
        </button>
        <button 
          v-for="type in tagTypes" 
          :key="type" 
          class="chip-btn" 
          :class="[type, { active: selectedType === type }]"
          @click="selectedType = type"
        >
          {{ getTypeLabel(type) }} 
          <span class="chip-count">{{ getTypeCount(type) }}</span>
        </button>
      </div>
    </div>

    <!-- 신규 생성 폼 -->
    <Transition name="slide">
      <div v-if="isCreating" class="create-form-panel">
        <div class="form-grid">
          <div class="form-group">
            <label>키워드 (매칭용)</label>
            <input v-model="newForm.keyword" placeholder="예: 강남역" class="form-input" />
          </div>
          <div class="form-group">
            <label>태그 타입</label>
            <select v-model="newForm.tagType" class="form-select">
              <option v-for="type in tagTypes" :key="type" :value="type">{{ getTypeLabel(type) }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>대표 명칭</label>
            <input v-model="newForm.representativeName" placeholder="예: 강남" class="form-input" />
          </div>
          <div class="form-group">
            <label>연동 상태 (선택)</label>
            <select v-model="newForm.targetStatus" class="form-select">
              <option :value="null">없음</option>
              <option v-for="status in noticeStatuses" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
          </div>
        </div>
        <div class="form-actions">
          <button @click="handleCreate" class="btn-primary">등록하기</button>
          <button @click="isCreating = false" class="btn-secondary">취소</button>
        </div>
      </div>
    </Transition>

    <div class="metadata-table-wrapper scrollable">
      <table class="metadata-table">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-type">타입</th>
            <th class="col-keyword">키워드 <span class="mobile-only-inline">/ 대표명칭</span></th>
            <th class="col-rep pc-only">대표명칭</th>
            <th class="col-status pc-only">연동상태</th>
            <th class="col-action">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in paginatedMetadata" :key="item.id">
            <td class="col-id">{{ item.id }}</td>
            <td class="col-type">
              <select v-if="editingId === item.id" v-model="editForm.tagType" class="table-select">
                <option v-for="type in tagTypes" :key="type" :value="type">{{ getTypeLabel(type) }}</option>
              </select>
              <span v-else class="type-badge" :class="item.tagType">{{ getTypeLabel(item.tagType) }}</span>
            </td>
            <td class="col-keyword">
              <!-- 수정 모드: PC에서는 키워드만, 모바일에서는 스택 -->
              <div v-if="editingId === item.id" class="edit-stack">
                <input 
                  v-model="editForm.keyword" 
                  class="table-input" 
                  placeholder="키워드" 
                  @keyup.enter="handleUpdate(item.id)"
                  @keyup.esc="editingId = null"
                />
                <input 
                  v-model="editForm.representativeName" 
                  class="table-input mobile-only-block" 
                  placeholder="대표명칭" 
                  @keyup.enter="handleUpdate(item.id)"
                  @keyup.esc="editingId = null"
                />
              </div>
              <div v-else class="text-stack">
                <span class="main-text">{{ item.keyword }}</span>
                <span class="sub-text mobile-only-inline"> / {{ item.representativeName || '-' }}</span>
              </div>
            </td>
            <td class="col-rep pc-only">
              <input 
                v-if="editingId === item.id" 
                v-model="editForm.representativeName" 
                class="table-input" 
                @keyup.enter="handleUpdate(item.id)"
                @keyup.esc="editingId = null"
              />
              <span v-else>{{ item.representativeName || '-' }}</span>
            </td>
            <td class="col-status pc-only">
              <select v-if="editingId === item.id" v-model="editForm.targetStatus" class="table-select">
                <option :value="null">없음</option>
                <option v-for="status in noticeStatuses" :key="status.value" :value="status.value">
                  {{ status.label }}
                </option>
              </select>
              <span v-else>{{ getStatusLabel(item.targetStatus) }}</span>
            </td>
            <td class="col-action">
              <div class="actions-wrapper">
                <template v-if="editingId === item.id">
                  <button @click="handleUpdate(item.id)" class="btn-save">저장</button>
                  <button @click="editingId = null" class="btn-cancel">취소</button>
                </template>
                <template v-else>
                  <button @click="startEdit(item)" class="btn-edit">수정</button>
                  <button @click="handleDelete(item.id)" class="btn-delete">삭제</button>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="pagination-bar">
      <button :disabled="currentPage === 1" @click="currentPage = 1" class="btn-page first-last">처음</button>
      <button :disabled="currentPage === 1" @click="currentPage--" class="btn-page">이전</button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button :disabled="currentPage === totalPages" @click="currentPage++" class="btn-page">다음</button>
      <button :disabled="currentPage === totalPages" @click="currentPage = totalPages" class="btn-page first-last">마지막</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import axios from '@/plugins/axios'

const metadataList = ref([])
const searchQuery = ref('')
const selectedType = ref('ALL')
const isCreating = ref(false)
const editingId = ref(null)

const currentPage = ref(1)
const pageSize = 20

const tagTypes = ['REGION', 'METRO', 'CITY_COUNTY', 'SUBWAY_LINE', 'STATION', 'TYPE', 'TARGET', 'SPECIAL', 'PROVIDER', 'SYSTEM', 'IGNORE']
const noticeStatuses = [
  { value: 'RECRUITING', label: '접수중' },
  { value: 'RECRUITING_NON_STOP', label: '원문확인' },
  { value: 'RESULT', label: '결과발표' },
  { value: 'INFO', label: '안내' },
  { value: 'CLOSED', label: '마감' },
  { value: 'UPCOMING', label: '예정' }
]

const newForm = ref({ keyword: '', tagType: 'STATION', representativeName: '', targetStatus: null })
const editForm = ref({ keyword: '', tagType: '', representativeName: '', targetStatus: null })

const getTypeLabel = (type) => {
  const labels = { 'METRO': '광역', 'CITY_COUNTY': '시/군/구', 'SUBWAY_LINE': '노선', 'STATION': '역', 'TYPE': '유형', 'TARGET': '대상', 'SPECIAL': '특수', 'REGION': '지역', 'PROVIDER': '기관', 'SYSTEM': '상태', 'IGNORE': '무시단어' }
  return labels[type] || type
}

const getTypeCount = (type) => metadataList.value.filter(m => m.tagType === type).length

const getStatusLabel = (status) => {
  if (!status) return '-'
  const found = noticeStatuses.find(s => s.value === status)
  return found ? found.label : status
}

const fetchMetadata = async () => {
  try {
    const res = await axios.get('/api/admin/metadata')
    metadataList.value = res.data.data
  } catch (e) { console.error('메타데이터 로드 실패') }
}

const filteredMetadata = computed(() => {
  return metadataList.value.filter(m => {
    const keywordMatch = m.keyword.toLowerCase().includes(searchQuery.value.toLowerCase())
    const repMatch = (m.representativeName || '').toLowerCase().includes(searchQuery.value.toLowerCase())
    const typeMatch = selectedType.value === 'ALL' || m.tagType === selectedType.value
    return (keywordMatch || repMatch) && typeMatch
  }).reverse()
})

const totalPages = computed(() => Math.ceil(filteredMetadata.value.length / pageSize))
const paginatedMetadata = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMetadata.value.slice(start, start + pageSize)
})

watch([searchQuery, selectedType], () => { currentPage.value = 1 })

const handleCreate = async () => {
  if (!newForm.value.keyword.trim()) return alert('키워드를 입력하세요.')
  try {
    await axios.post('/api/admin/metadata', newForm.value)
    alert('등록되었습니다.')
    newForm.value = { keyword: '', tagType: 'STATION', representativeName: '', targetStatus: null }
    isCreating.value = false
    await fetchMetadata()
  } catch (e) { alert('등록 실패') }
}

const startEdit = (item) => {
  editingId.value = item.id
  editForm.value = { ...item }
}

const handleUpdate = async (id) => {
  try {
    await axios.put(`/api/admin/metadata/${id}`, editForm.value)
    alert('수정되었습니다.')
    editingId.value = null
    await fetchMetadata()
  } catch (e) { alert('수정 실패') }
}

const handleDelete = async (id) => {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await axios.delete(`/api/admin/metadata/${id}`)
    await fetchMetadata()
  } catch (e) { alert('삭제 실패') }
}

onMounted(fetchMetadata)
</script>

<style scoped>
.admin-metadata-container { 
  max-width: 1400px; margin: 0 auto; height: 100%; width: 100%; display: flex; flex-direction: column;
}

.admin-header { flex-shrink: 0; margin-bottom: 8px; }
.header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.header-main h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: var(--text-primary); }
.count-info { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; }

.search-filter-bar { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
.search-input-wrapper { flex: 1; min-width: 200px; position: relative; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.4; color: var(--text-primary); pointer-events: none; }
.admin-search-input { width: 100%; height: 38px; padding: 0 12px 0 35px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.85rem; background: var(--header-bg); color: var(--text-primary); transition: border-color 0.2s; outline: none; }
.admin-search-input:focus { border-color: var(--link-color); }
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
}
.btn-new:hover { background: var(--border-color); }

.type-filter-chips { display: flex; gap: 8px; padding: 5px 0; margin-bottom: 5px; }
.scrollable-x { overflow-x: auto; scrollbar-width: none; -ms-overflow-style: none; }
.scrollable-x::-webkit-scrollbar { display: none; }

.chip-btn { padding: 6px 12px; border-radius: 20px; border: 1px solid var(--border-color); background: var(--card-bg); font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; white-space: nowrap; display: flex; align-items: center; gap: 6px; }
.chip-btn:hover { background: var(--hover-bg); border-color: var(--link-color); color: var(--link-color); }
.chip-btn.active { background: var(--link-color); color: white; border-color: var(--link-color); }
.chip-btn.active.REGION, .chip-btn.active.METRO { background: #3498db; border-color: #3498db; }
.chip-btn.active.CITY_COUNTY { background: #5dade2; border-color: #5dade2; }
.chip-btn.active.SUBWAY_LINE { background: #8e44ad; border-color: #8e44ad; }
.chip-btn.active.STATION { background: #e67e22; border-color: #e67e22; }
.chip-btn.active.TYPE { background: #2ecc71; border-color: #2ecc71; }
.chip-btn.active.TARGET { background: #e91e63; border-color: #e91e63; }
.chip-btn.active.SPECIAL { background: #9b59b6; border-color: #9b59b6; }
.chip-btn.active.PROVIDER { background: #34495e; border-color: #34495e; }
.chip-btn.active.SYSTEM { background: #16a085; border-color: #16a085; }
.chip-btn.active.IGNORE { background: #7f8c8d; border-color: #7f8c8d; }

.chip-count { font-size: 0.65rem; opacity: 0.7; font-weight: 600; }

.create-form-panel { background: var(--card-bg); border: 1px solid var(--link-color); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 20px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
.form-group { display: flex; flex-direction: column; gap: 5px; }
.form-group label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }
.form-input, .form-select { height: 38px; padding: 0 10px; border: 1px solid var(--border-color); border-radius: 6px; background: var(--hover-bg); color: var(--text-primary); outline: none; transition: border-color 0.2s; }
.form-input:focus, .form-select:focus { border-color: var(--link-color); }

.form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 5px; }
.btn-primary { padding: 10px 24px; background: var(--link-color); color: white; border: none; border-radius: 8px; font-weight: 800; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 6px rgba(0, 149, 246, 0.2); }
.btn-primary:hover { filter: brightness(1.1); transform: translateY(-1px); }
.btn-secondary { padding: 10px 24px; background: var(--hover-bg); color: var(--text-primary); border: 1px solid var(--border-color); border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.btn-secondary:hover { background: var(--border-color); }

/* 테이블 영역 (공고관리 스타일 동기화) */
.metadata-table-wrapper { flex: 1; min-height: 0; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; overflow-y: auto; margin-bottom: 5px; }
.metadata-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.metadata-table thead { position: sticky; top: 0; z-index: 10; background: var(--hover-bg); }
.metadata-table th { background: var(--hover-bg); padding: 10px 12px; text-align: center; font-size: 0.8rem; font-weight: 800; color: var(--text-secondary); border-bottom: 1px solid var(--border-color); }
.metadata-table td { padding: 0 12px; height: 48px; border-bottom: 1px solid var(--border-color); font-size: 0.82rem; color: var(--text-primary); vertical-align: middle; box-sizing: border-box; text-align: center; }

/* 컬럼 너비 (PC) */
.col-id { width: 50px; }
.col-type { width: 100px; }
.col-keyword { width: auto; text-align: left !important; } /* 키워드는 왼쪽 정렬 유지 */
.col-rep { width: 150px; text-align: left !important; }
.col-status { width: 110px; }
.col-action { width: 130px; }

/* PC에서는 키워드 셀 내부의 대표명칭 입력을 숨김 */
.mobile-only-block { display: none; }
.pc-only { display: table-cell; }
.mobile-only-inline { display: none; }

.table-input, .table-select { 
  width: 100%; 
  height: 28px; 
  padding: 0 8px; 
  border: 1px solid var(--link-color); 
  border-radius: 4px; 
  background: var(--header-bg); 
  color: var(--text-primary); 
  outline: none; 
  font-size: 0.75rem; 
  transition: all 0.2s;
}
.table-input:focus { box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2); }

.type-badge { font-size: 0.7rem; padding: 2px 6px; border-radius: 4px; color: white; background: #95a5a6; }
.type-badge.REGION, .type-badge.METRO { background: #3498db; }
.type-badge.CITY_COUNTY { background: #5dade2; }
.type-badge.SUBWAY_LINE { background: #8e44ad; }
.type-badge.STATION { background: #e67e22; }
.type-badge.TYPE { background: #2ecc71; }
.type-badge.TARGET { background: #e91e63; }
.type-badge.SPECIAL { background: #9b59b6; }
.type-badge.PROVIDER { background: #34495e; }
.type-badge.SYSTEM { background: #16a085; }
.type-badge.IGNORE { background: #7f8c8d; }

.actions-wrapper { display: flex; gap: 4px; justify-content: center; align-items: center; height: 100%; }
.btn-edit, .btn-save { border: 1px solid var(--link-color); color: var(--link-color); flex: 1; }
.btn-edit:hover, .btn-save { background: var(--link-color); color: white; }
.btn-delete, .btn-cancel { border: 1px solid #ed4956; color: #ed4956; flex: 1; }
.btn-delete:hover, .btn-cancel { background: #ed4956; color: white; }
.btn-edit, .btn-save, .btn-delete, .btn-cancel { height: 28px; border-radius: 6px; font-size: 0.7rem; font-weight: 700; cursor: pointer; background: none; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }

.pagination-bar { flex-shrink: 0; height: 34px; display: flex; justify-content: center; align-items: center; gap: 8px; }
.btn-page { padding: 3px 8px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--header-bg); font-size: 0.7rem; cursor: pointer; color: var(--text-primary); transition: all 0.2s; }
.btn-page:hover:not(:disabled) { background: var(--hover-bg); border-color: var(--link-color); color: var(--link-color); }
.btn-page:disabled { opacity: 0.4; cursor: default; }
.first-last { background: var(--hover-bg); }
.page-info { font-size: 0.75rem; font-weight: 700; color: var(--text-primary); min-width: 50px; text-align: center; }

.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

@media (max-width: 768px) {
  .header-main { gap: 10px; flex-direction: row; justify-content: space-between; align-items: center; }
  .header-main h2 { font-size: 1rem; }
  .count-info { font-size: 0.75rem; }
  .admin-search-input { height: 34px; font-size: 0.8rem; }
  .btn-new { height: 34px; font-size: 0.8rem; }

  /* 모바일 특화 테이블 레이아웃 */
  .pc-only { display: none; }
  .mobile-only-inline { display: inline; }
  .mobile-only-block { display: block; width: 100%; margin-top: 4px; }
  
  .col-id { width: 40px; }
  .col-type { width: 100px; }
  .col-action { width: 85px; }
  .metadata-table td { height: auto; min-height: 60px; padding: 8px; }
  
  .text-stack, .edit-stack { display: flex; flex-direction: column; gap: 2px; align-items: flex-start; text-align: left; }
  .main-text { font-weight: 800; color: var(--text-primary); font-size: 0.8rem; }
  .sub-text { font-weight: 600; color: var(--text-secondary); font-size: 0.75rem; }
  
  /* 모바일에서만 키워드/대표명칭 셀 왼쪽 정렬 */
  .col-keyword { text-align: left !important; }

  .actions-wrapper { flex-direction: column; gap: 4px; padding: 4px 0; }
  .btn-edit, .btn-delete, .btn-save, .btn-cancel { width: 100%; height: 26px; font-size: 0.65rem; }
  .pagination-bar { height: auto; margin-top: 15px; }
}
</style>
