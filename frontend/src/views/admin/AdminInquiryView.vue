<template>
  <div class="admin-inquiry-container">
    <!-- 1. 헤더 섹션 -->
    <div class="admin-header">
      <div class="header-main">
        <h2 class="title">📫 고객센터 문의 관리</h2>
        <!-- 기존 count-badge 제거 -->
      </div>
      
      <!-- 2. 컨트롤 바 (검색 7 : 필터 3 + 건수 표시) -->
      <div class="admin-controls-bar">
        <div class="controls-left-group">
          <div class="controls-search">
            <div class="search-box">
              <span class="search-icon-fixed">🔍</span>
              <input 
                v-model="searchKeyword" 
                type="text" 
                class="compact-input" 
                placeholder="작성자, 제목, 내용 검색 후 엔터..."
                @keyup.enter="fetchData"
              >
            </div>
          </div>
          
          <div class="controls-filter">
            <select v-model="selectedStatus" class="compact-select" @change="fetchData">
              <option v-for="status in statusList" :key="status.id" :value="status.id">
                {{ status.name }}
              </option>
            </select>
          </div>
        </div>

        <!-- [시니어 조치] 우측 끝에 n건/n건 표시 -->
        <div class="controls-right-stats">
          <span class="stats-text">{{ totalCount }}건 / {{ grandTotalCount }}건</span>
        </div>
      </div>
    </div>

    <!-- 3. 리스트 영역 -->
    <div class="inquiry-table-wrapper">
      <table class="compact-admin-table">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-type">문의 유형</th>
            <th class="col-title">제목 및 문의 내용</th>
            <th class="col-author">작성자</th>
            <th class="col-date">작성일</th>
            <th class="col-status">상태</th>
            <th class="col-action">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="empty-row">데이터를 불러오는 중...</td></tr>
          <tr v-else-if="inquiries.length === 0"><td colspan="7" class="empty-row">데이터가 없습니다.</td></tr>
          <tr v-for="item in inquiries" :key="item.id" :class="{ 'answered-row': item.status === 'ANSWERED', 'deleted-row': item.deletedByAuthor }">
            <td class="text-center font-id col-id">{{ item.id }}</td>
            <td class="col-type">
              <span class="category-chip">{{ item.categoryDescription }}</span>
              <div v-if="item.deletedByAuthor" class="delete-flag">🗑️ 사용자 삭제</div>
            </td>
            <td class="col-title">
              <div class="inquiry-brief">
                <div class="title-line">
                  <span v-if="item.imageUrl" class="img-bullet">🖼️</span>
                  <span class="main-title">{{ item.title }}</span>
                </div>
                <p class="sub-content">{{ item.content }}</p>
              </div>
            </td>
            <td class="col-author"><strong class="author-name">{{ item.authorNickname }}</strong></td>
            <td class="date-cell col-date">{{ formatDateShort(item.createdAt) }}</td>
            <td class="text-center col-status">
              <span class="status-dot-badge" :class="item.status.toLowerCase()">{{ item.statusDescription }}</span>
            </td>
            <td class="text-center col-action">
              <button class="compact-action-btn" @click="openModal(item)">
                {{ item.status === 'ANSWERED' ? '상세' : '답변' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 4. 답변 모달 -->
    <Transition name="modal-fade">
      <div v-if="isModalOpen" class="admin-modal-overlay" @click.self="closeModal">
        <div class="admin-modal-window">
          <div class="modal-header">
            <div class="modal-title-group">
              <span class="modal-icon">📫</span>
              <h3>문의 상세 및 답변</h3>
            </div>
            <button class="modal-close-btn" @click="closeModal">&times;</button>
          </div>
          
          <div class="modal-scroll-body">
            <div class="detail-info-grid">
              <div class="info-block">
                <label>작성자</label>
                <div class="val">{{ selectedInquiry.authorNickname }}</div>
              </div>
              <div class="info-block">
                <label>유형</label>
                <div class="val">{{ selectedInquiry.categoryDescription }}</div>
              </div>
              <div class="info-block">
                <label>작성일</label>
                <div class="val">{{ formatDate(selectedInquiry.createdAt) }}</div>
              </div>
            </div>

            <div class="detail-section">
              <h4 class="section-title">📝 문의 내용</h4>
              <div class="content-view-card">
                <h5 class="view-title">{{ selectedInquiry.title }}</h5>
                <div class="view-text">{{ selectedInquiry.content }}</div>
                
                <div v-if="selectedInquiry.imageUrl" class="view-attachment">
                  <p class="attach-label">🖼️ 첨부 이미지</p>
                  <img :src="selectedInquiry.imageUrl" class="attach-preview" @click="openImage(selectedInquiry.imageUrl)">
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h4 class="section-title">👑 운영자 답변</h4>
              <textarea 
                v-model="answerText" 
                rows="6" 
                class="modal-textarea"
                placeholder="답변 내용을 입력하세요. 등록 시 사용자에게 알림이 전송됩니다."
                :disabled="selectedInquiry.status === 'ANSWERED' && !isEditing"
              ></textarea>
              <div v-if="selectedInquiry.answeredAt" class="answer-meta">
                마지막 답변: {{ formatDate(selectedInquiry.answeredAt) }}
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="closeModal">닫기</button>
            <template v-if="selectedInquiry.status === 'ANSWERED' && !isEditing">
              <button class="btn-edit" @click="isEditing = true">수정</button>
            </template>
            <template v-else>
              <button 
                class="btn-submit" 
                :disabled="!answerText.trim() || isSaving" 
                @click="submitAnswer"
              >
                {{ isSaving ? '저장 중...' : '답변 등록' }}
              </button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from '@/plugins/axios'

const inquiries = ref([])
const totalCount = ref(0) // 검색 결과 건수
const grandTotalCount = ref(0) // 전체 원본 건수
const loading = ref(false)
const selectedStatus = ref('ALL')
const searchKeyword = ref('')
const isModalOpen = ref(false)
const selectedInquiry = ref(null)
const answerText = ref('')
const isSaving = ref(false)
const isEditing = ref(false)

const statusList = [
  { id: 'ALL', name: '전체 상태' },
  { id: 'RECEIVED', name: '접수됨' },
  { id: 'REVIEWING', name: '검토중' },
  { id: 'ANSWERED', name: '답변완료' }
]

// [시니어 조치] 데이터 로드 (검색/필터 결과용)
const fetchData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (selectedStatus.value !== 'ALL') params.append('status', selectedStatus.value)
    if (searchKeyword.value.trim()) params.append('keyword', searchKeyword.value.trim())
    
    const res = await axios.get(`/api/admin/inquiries?${params.toString()}`)
    inquiries.value = res.data.data.content
    totalCount.value = res.data.data.totalElements
  } catch (e) {
    console.error('문의 로드 실패:', e)
  } finally {
    loading.value = false
  }
}

// [시니어 조치] 전체 누적 건수 로드 (필터 없이)
const fetchGrandTotal = async () => {
  try {
    const res = await axios.get('/api/admin/inquiries')
    grandTotalCount.value = res.data.data.totalElements
  } catch (e) {}
}

const openModal = (item) => {
  selectedInquiry.value = item
  answerText.value = item.answer || ''
  isEditing.value = false
  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
  selectedInquiry.value = null
  answerText.value = ''
}

const submitAnswer = async () => {
  if (!answerText.value.trim()) return
  isSaving.value = true
  try {
    await axios.post(`/api/admin/inquiries/${selectedInquiry.value.id}/answer`, {
      answer: answerText.value
    })
    alert('답변이 등록되었습니다.')
    closeModal()
    fetchData()
  } catch (e) {
    alert('저장 실패')
  } finally {
    isSaving.value = false
  }
}

const openImage = (url) => { window.open(url, '_blank') }
const formatDate = (s) => s ? s.replace('T', ' ').substring(0, 16) : '-'
const formatDateShort = (s) => s ? s.split('T')[0].substring(2).replace(/-/g, '.') : '-'

onMounted(() => {
  fetchData()
  fetchGrandTotal()
})
</script>

<style scoped>
.admin-inquiry-container { padding: 0; text-align: left; }

/* 1. 헤더 영역 */
.admin-header { margin-bottom: 12px; }
.header-main { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.title { font-size: 1.1rem; font-weight: 800; margin: 0; color: var(--text-primary); }

/* 2. 컨트롤 바 개편 (7:3 그룹화 + 우측 건수) */
.admin-controls-bar { 
  display: flex; justify-content: space-between; align-items: center; 
  background: var(--card-bg); padding: 8px 12px; border-radius: 8px; border: 1px solid var(--border-color);
}
.controls-left-group { display: flex; gap: 12px; flex: 1; align-items: center; }
.controls-search { flex: 7; max-width: 500px; }
.controls-filter { flex: 3; max-width: 150px; }

.search-box { 
  display: flex; align-items: center; background: var(--bg-color); 
  border: 1px solid var(--border-color); border-radius: 6px; overflow: hidden; width: 100%;
}
.search-icon-fixed { padding: 0 8px; color: var(--text-secondary); font-size: 0.75rem; }
.compact-input { 
  flex: 1; border: none; background: none; padding: 6px 5px; 
  font-size: 0.8rem; color: var(--text-primary); outline: none; 
}

.compact-select { 
  background: var(--bg-color); border: 1px solid var(--border-color); 
  padding: 5px 10px; border-radius: 6px; color: var(--text-primary); 
  font-size: 0.8rem; font-weight: 600; outline: none; width: 100%;
}

/* 우측 건수 표시 (요청 사양) */
.controls-right-stats { padding-left: 15px; }
.stats-text { font-size: 0.75rem; color: var(--text-secondary); font-weight: 700; white-space: nowrap; }

/* 3. 테이블 영역 */
.inquiry-table-wrapper { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 8px; overflow: hidden; }
.compact-admin-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.compact-admin-table th { background: var(--hover-bg); padding: 8px 12px; text-align: left; font-size: 0.7rem; color: var(--text-secondary); border-bottom: 1px solid var(--border-color); font-weight: 800; }
.compact-admin-table td { padding: 8px 12px; border-bottom: 1px solid var(--border-color); font-size: 0.82rem; vertical-align: middle; }

.col-id { width: 45px; text-align: center; }
.col-type { width: 160px; } 
.col-title { width: auto; }
.col-author { width: 100px; }
.col-date { width: 100px; }
.col-status { width: 85px; text-align: center; }
.col-action { width: 75px; text-align: center; }

.font-id { font-family: monospace; color: var(--text-secondary); }
.category-chip { font-size: 0.62rem; font-weight: 800; color: var(--link-color); background: rgba(0, 149, 246, 0.08); padding: 2px 6px; border-radius: 4px; }
.delete-flag { font-size: 0.58rem; color: #ed4956; font-weight: bold; margin-top: 2px; }

.inquiry-brief { display: flex; flex-direction: column; gap: 1px; }
.title-line { display: flex; align-items: center; gap: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.main-title { font-weight: 700; color: var(--text-primary); }
.sub-content { font-size: 0.75rem; color: var(--text-secondary); margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.status-dot-badge { font-size: 0.65rem; font-weight: 800; padding: 2px 8px; border-radius: 10px; }
.status-dot-badge.received { background: #eee; color: #666; }
.status-dot-badge.answered { background: rgba(0, 153, 117, 0.1); color: #009975; }

.compact-action-btn { 
  background: var(--hover-bg); border: 1px solid var(--border-color); padding: 2px 8px; border-radius: 4px; 
  font-size: 0.7rem; font-weight: 700; cursor: pointer; color: var(--text-primary); 
}
.compact-action-btn:hover { border-color: var(--link-color); color: var(--link-color); }

.answered-row { background-color: rgba(0, 153, 117, 0.01); }
.deleted-row { opacity: 0.6; }

/* 4. 모달 스타일 */
.admin-modal-overlay { 
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; 
  background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 9999; backdrop-filter: blur(4px);
}
.admin-modal-window { 
  background: var(--card-bg); width: 85%; max-width: 520px; border-radius: 12px; 
  display: flex; flex-direction: column; max-height: 80vh; border: 1px solid var(--border-color); box-shadow: 0 20px 50px rgba(0,0,0,0.4);
}
.modal-header { padding: 12px 18px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.modal-header h3 { margin: 0; font-size: 1rem; font-weight: 800; color: var(--text-primary); }
.modal-close-btn { background: none; border: none; font-size: 1.4rem; cursor: pointer; color: var(--text-secondary); line-height: 1; }

.modal-scroll-body { padding: 15px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 15px; }
.detail-info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; background: var(--bg-color); padding: 10px; border-radius: 8px; border: 1px solid var(--border-color); }
.info-block label { display: block; font-size: 0.55rem; font-weight: 800; color: var(--text-secondary); margin-bottom: 2px; }
.info-block .val { font-size: 0.78rem; font-weight: 700; color: var(--text-primary); }

.section-title { font-size: 0.8rem; font-weight: 800; color: var(--text-primary); margin: 0 0 6px 0; }
.content-view-card { background: var(--bg-color); padding: 10px; border-radius: 8px; border: 1px solid var(--border-color); }
.view-title { margin: 0 0 6px 0; font-size: 0.9rem; font-weight: 800; }
.view-text { font-size: 0.85rem; line-height: 1.5; white-space: pre-wrap; color: var(--text-primary); }

.attach-preview { max-width: 100%; max-height: 180px; border-radius: 6px; border: 1px solid var(--border-color); cursor: zoom-in; margin-top: 8px; }

.modal-textarea { 
  width: 100%; background: var(--bg-color); border: 1px solid var(--border-color); 
  border-radius: 8px; padding: 10px; font-size: 0.88rem; line-height: 1.4; resize: none; color: var(--text-primary); outline: none;
}
.modal-footer { padding: 12px 18px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; gap: 8px; background: var(--hover-bg); border-bottom-left-radius: 12px; border-bottom-right-radius: 12px; }
.btn-cancel { background: none; border: 1px solid var(--border-color); padding: 6px 12px; border-radius: 6px; font-size: 0.78rem; font-weight: 700; cursor: pointer; color: var(--text-primary); }
.btn-submit { background: var(--link-color); color: white; border: none; padding: 6px 15px; border-radius: 6px; font-size: 0.78rem; font-weight: 700; cursor: pointer; }
.btn-edit { background: #ff9500; color: white; border: none; padding: 6px 15px; border-radius: 6px; font-size: 0.78rem; font-weight: 700; cursor: pointer; }

/* 모바일 대응 */
@media (max-width: 768px) {
  .admin-controls-bar { flex-direction: column; align-items: flex-start; gap: 8px; padding: 8px; }
  .controls-left-group { width: 100%; flex-direction: column; align-items: flex-start; gap: 8px; }
  .controls-search, .controls-filter { width: 100%; flex: none; max-width: none; }
  .controls-filter { justify-content: flex-start; }
  
  .controls-right-stats { width: 100%; padding: 0; display: flex; justify-content: flex-end; }
  .stats-text { font-size: 0.7rem; }

  .compact-admin-table td { padding: 6px 8px; font-size: 0.72rem; }
  .col-id, .col-author, .col-date { display: none; }
  .col-type { width: 80px; } 
  .col-status { width: 60px; }
  .col-action { width: 50px; }
  
  .admin-modal-window { width: 90%; max-height: 70vh; }
}
</style>
