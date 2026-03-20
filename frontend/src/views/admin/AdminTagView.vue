<template>
  <div class="admin-tag-container">
    <!-- 헤더 영역 (높이 고정) -->
    <div class="admin-header">
      <div class="header-main">
        <h2>🏷️ 태그 사전 관리</h2>
        <span class="count-info">{{ filteredTags.length }}건 / {{ tags.length }}건</span>
      </div>
      
      <div class="search-filter-bar">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input 
            v-model="searchQuery" 
            placeholder="태그 이름 검색..." 
            class="admin-search-input"
          />
        </div>
        <select v-model="selectedType" class="admin-filter-select">
          <option value="ALL">모든 타입</option>
          <option v-for="type in tagTypes" :key="type" :value="type">{{ getTypeLabel(type) }}</option>
        </select>
      </div>
    </div>

    <!-- 태그 그리드 (PC: 4열 / 모바일: 2열) -->
    <div class="tag-grid-outer scrollable">
      <div v-if="filteredTags.length === 0" class="empty-results">
        검색 결과와 일치하는 태그가 없습니다.
      </div>
      
      <div class="tag-grid-wrapper">
        <!-- 신규 태그 추가 카드 (정밀 보정됨) -->
        <div class="tag-card-item create-card" :class="{ 'is-expanded': isCreating }">
          <div class="tag-summary create-trigger" @click="isCreating = !isCreating">
            <span class="create-icon">{{ isCreating ? '➖' : '➕' }}</span>
            <span class="create-text">새 태그 추가</span>
          </div>
          <div v-if="isCreating" class="tag-edit-drawer create-drawer">
            <div class="edit-form-inner">
              <div class="edit-field">
                <label>태그 이름</label>
                <input v-model="newTagName" class="edit-input" placeholder="이름 입력..." />
              </div>
              <div class="edit-field">
                <label>타입 선택</label>
                <select v-model="newTagType" class="edit-select">
                  <option v-for="type in tagTypes" :key="type" :value="type">{{ getTypeLabel(type) }}</option>
                </select>
              </div>
              <button @click="handleCreate" class="btn-save-tag create-submit">생성하기</button>
            </div>
          </div>
        </div>

        <div 
          v-for="tag in paginatedTags" 
          :key="tag.id" 
          class="tag-card-item"
          :class="{ 'is-expanded': tag.isExpanded }"
        >
          <div class="tag-summary" @click="toggleExpand(tag)">
            <div class="summary-content">
              <span class="tag-type-badge" :class="tag.type">{{ getTypeLabel(tag.type) }}</span>
              <span class="tag-name-text">{{ tag.name }}</span>
            </div>
            <div class="summary-actions">
              <button @click.stop="handleDelete(tag)" class="btn-delete-icon" title="삭제">🗑️</button>
            </div>
          </div>

          <div v-if="tag.isExpanded" class="tag-edit-drawer">
            <div class="edit-form-inner">
              <div class="edit-field">
                <label>이름 수정</label>
                <input v-model="tag.editName" class="edit-input" @click.stop />
              </div>
              <div class="edit-field">
                <label>타입 수정</label>
                <select v-model="tag.editType" class="edit-select" @click.stop>
                  <option v-for="type in tagTypes" :key="type" :value="type">{{ getTypeLabel(type) }}</option>
                </select>
              </div>
              <div class="edit-btn-group">
                <button @click.stop="saveEdit(tag)" class="btn-save-tag">저장</button>
                <button @click.stop="tag.isExpanded = false" class="btn-cancel-tag">취소</button>
              </div>
            </div>
          </div>
        </div>
      </div>
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

const tags = ref([])
const searchQuery = ref('')
const selectedType = ref('ALL')

const isCreating = ref(false)
const newTagName = ref('')
const newTagType = ref('REGION')

const currentPage = ref(1)
const pageSize = 63 

const tagTypes = ['REGION', 'METRO', 'CITY_COUNTY', 'SUBWAY_LINE', 'STATION', 'TYPE', 'TARGET', 'SPECIAL', 'PROVIDER', 'SYSTEM']

const getTypeLabel = (type) => {
  const labels = { 'METRO': '광역', 'CITY_COUNTY': '시/군/구', 'SUBWAY_LINE': '노선', 'STATION': '역', 'TYPE': '유형', 'TARGET': '대상', 'SPECIAL': '특수', 'REGION': '지역', 'PROVIDER': '기관', 'SYSTEM': '상태' }
  return labels[type] || type
}

const fetchTags = async () => {
  try {
    const res = await axios.get('/api/admin/tags')
    // ID 역순(최신순)으로 정렬하여 저장
    tags.value = res.data.data
      .sort((a, b) => b.id - a.id)
      .map(t => ({
        ...t,
        isExpanded: false,
        editName: t.name,
        editType: t.type
      }))
  } catch (e) { console.error('태그 로드 실패') }
}

const filteredTags = computed(() => {
  return tags.value.filter(t => {
    const nameMatch = t.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    const typeMatch = selectedType.value === 'ALL' || t.type === selectedType.value
    return nameMatch && typeMatch
  })
})

const totalPages = computed(() => Math.ceil(filteredTags.value.length / pageSize))
const paginatedTags = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredTags.value.slice(start, start + pageSize)
})

watch([searchQuery, selectedType], () => { currentPage.value = 1 })

const toggleExpand = (targetTag) => {
  const newState = !targetTag.isExpanded
  tags.value.forEach(t => t.isExpanded = false)
  targetTag.isExpanded = newState
  if (newState) {
    targetTag.editName = targetTag.name
    targetTag.editType = targetTag.type
  }
}

const handleCreate = async () => {
  if (!newTagName.value.trim()) return alert('태그 이름을 입력하세요.')
  try {
    const res = await axios.post('/api/admin/tags', null, {
      params: { name: newTagName.value, type: newTagType.value }
    })
    
    // 생성 성공 시, 응답 데이터를 목록 맨 앞에 즉시 추가 (Optimistic UI)
    const createdTag = {
      ...res.data.data,
      isExpanded: false,
      editName: res.data.data.name,
      editType: res.data.data.type
    }
    
    tags.value.unshift(createdTag)
    
    alert('태그가 생성되었습니다.')
    newTagName.value = ''
    isCreating.value = false
    
    // 필요 시 전체 목록 동기화 (이미 추가했으므로 생략 가능하나 안전을 위해 호출할 수 있음)
    // await fetchTags() 
  } catch (e) {
    alert(e.response?.data?.message || '생성 실패: 이미 존재하거나 오류 발생')
  }
}

const saveEdit = async (targetTag) => {
  try {
    await axios.patch(`/api/admin/tags/${targetTag.id}`, null, {
      params: { name: targetTag.editName, type: targetTag.editType }
    })
    alert('수정되었습니다.')
    targetTag.isExpanded = false
    await fetchTags()
  } catch (e) { alert('수정 실패: 데이터 오류') }
}

const handleDelete = async (tag) => {
  try {
    // 1. 삭제 전 사용량 확인
    const usageRes = await axios.get(`/api/admin/tags/${tag.id}/usage`)
    const { noticeCount, userCount } = usageRes.data.data

    // 2. 맞춤형 경고 메시지 구성
    const confirmMsg = `태그 '${tag.name}'는 현재 ${noticeCount}개의 공고와 ${userCount}명의 유저가 사용 중입니다.\n정말 삭제하시겠습니까?`
    
    if (!confirm(confirmMsg)) return

    // 3. 실제 삭제 진행
    await axios.delete(`/api/admin/tags/${tag.id}`)
    
    // UI 즉시 반영 (낙관적 업데이트)
    tags.value = tags.value.filter(t => t.id !== tag.id)
    alert('태그가 삭제되었습니다.')
  } catch (e) {
    console.error('삭제 실패:', e)
    alert(e.response?.data?.message || '삭제 실패: 서버 오류가 발생했습니다.')
  }
}

onMounted(fetchTags)
</script>

<style scoped>
.admin-tag-container { max-width: 1400px; margin: 0 auto; height: 100%; width: 100%; display: flex; flex-direction: column; }

.admin-header { flex-shrink: 0; margin-bottom: 8px; }
.header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.header-main h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: var(--text-primary); }
.count-info { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; }

.search-filter-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.search-input-wrapper { flex: 1; min-width: 200px; position: relative; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.4; color: var(--text-primary); pointer-events: none; }
.admin-search-input { width: 100%; height: 38px; padding: 0 12px 0 35px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.85rem; background: var(--header-bg); color: var(--text-primary); transition: border-color 0.2s; outline: none; }
.admin-search-input:focus { border-color: var(--link-color); }
.admin-filter-select { padding: 0 10px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.82rem; min-width: 130px; height: 38px; background: var(--header-bg); color: var(--text-primary); outline: none; cursor: pointer; }

.tag-grid-outer { flex: 1; min-height: 0; overflow-y: auto; padding: 2px 0; margin-bottom: 5px; }
.tag-grid-wrapper { display: grid; grid-template-columns: repeat(4, 1fr); grid-auto-rows: min-content; gap: 6px; }

.tag-card-item { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; display: flex; flex-direction: column; height: fit-content; transition: all 0.2s; color: var(--text-primary); }

/* 신규 생성 카드 스타일 정밀 보정 */
.create-card { border: 1px dashed var(--link-color); background: var(--hover-bg); cursor: pointer; }
.create-trigger { justify-content: center; gap: 8px; color: var(--link-color); }
.create-icon { font-size: 0.8rem; }
.create-text { font-weight: 700; font-size: 0.7rem; }
.create-drawer { background: var(--card-bg) !important; }
.create-submit { margin-top: 5px; width: 100%; background: var(--link-color) !important; }

.tag-summary { padding: 8px 12px; display: flex; justify-content: space-between; align-items: center; cursor: pointer; min-height: 40px; box-sizing: border-box; }
.summary-content { display: flex; align-items: center; gap: 6px; flex: 1; min-width: 0; }
.tag-name-text { font-weight: 600; font-size: 0.7rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary); }
.tag-type-badge { font-size: 0.45rem; padding: 1px 4px; border-radius: 4px; color: white; background: #95a5a6; flex-shrink: 0; }
.tag-type-badge.REGION, .tag-type-badge.METRO { background: #3498db; }
.tag-type-badge.CITY_COUNTY { background: #5dade2; }
.tag-type-badge.SUBWAY_LINE { background: #8e44ad; }
.tag-type-badge.STATION { background: #e67e22; }
.tag-type-badge.TYPE { background: #2ecc71; }
.tag-type-badge.TARGET { background: #e91e63; }
.tag-type-badge.SPECIAL { background: #9b59b6; }
.tag-type-badge.PROVIDER { background: #34495e; }
.tag-type-badge.SYSTEM { background: #16a085; }
.tag-type-badge.IGNORE { background: #7f8c8d; }

.btn-delete-icon { background: none; border: none; cursor: pointer; font-size: 0.7rem; opacity: 0.3; color: var(--text-primary); }
.btn-delete-icon:hover { opacity: 1; color: #ed4956; }

.tag-edit-drawer { background: var(--hover-bg); border-top: 1px solid var(--border-color); border-bottom-left-radius: 10px; border-bottom-right-radius: 10px; }
.edit-form-inner { padding: 10px; display: flex; flex-direction: column; gap: 6px; }
.edit-field { display: flex; flex-direction: column; gap: 2px; }
.edit-field label { font-size: 0.6rem; font-weight: 700; color: var(--text-secondary); }
.edit-input, .edit-select { height: 32px; padding: 0 8px; border: 1px solid var(--border-color); border-radius: 6px; font-size: 0.75rem; background: var(--header-bg); color: var(--text-primary); outline: none; }
.edit-btn-group { display: flex; gap: 5px; margin-top: 4px; }
.btn-save-tag { flex: 1; background: var(--link-color); color: white; border: none; padding: 6px; border-radius: 6px; font-size: 0.7rem; cursor: pointer; font-weight: 700; }
.btn-cancel-tag { flex: 1; background: var(--tag-bg); color: var(--text-primary); border: none; padding: 6px; border-radius: 6px; font-size: 0.7rem; cursor: pointer; font-weight: 700; }

.pagination-bar { flex-shrink: 0; height: 34px; display: flex; justify-content: center; align-items: center; gap: 8px; }
.btn-page { padding: 3px 8px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--header-bg); font-size: 0.7rem; cursor: pointer; color: var(--text-primary); transition: all 0.2s; }
.btn-page:hover:not(:disabled) { background: var(--hover-bg); border-color: var(--link-color); color: var(--link-color); }
.btn-page:disabled { opacity: 0.4; cursor: default; }
.first-last { background: var(--hover-bg); }
.page-info { font-size: 0.75rem; font-weight: 700; color: var(--text-primary); min-width: 50px; text-align: center; }

.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

.empty-results { text-align: center; padding: 40px; color: #95a5a6; font-size: 0.8rem; }

@media (max-width: 1200px) { .tag-grid-wrapper { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) {
  .header-main { gap: 10px; flex-direction: row; justify-content: space-between; align-items: center; }
  .header-main h2 { font-size: 1rem; }
  .count-info { font-size: 0.75rem; }
  
  .search-filter-bar { flex-wrap: nowrap; gap: 6px; }
  .search-input-wrapper { flex: 1; min-width: 0; }
  .admin-filter-select { width: 100px; min-width: 0; flex-shrink: 0; }
  
  .tag-grid-wrapper { grid-template-columns: repeat(2, 1fr); }
  .admin-tag-container { height: auto; }
  .tag-grid-outer { overflow-y: visible; flex: none; min-height: auto; }
  .admin-search-input, .admin-filter-select { height: 34px; font-size: 0.8rem; }
  .pagination-bar { margin-top: 15px; height: auto; }
}
</style>
