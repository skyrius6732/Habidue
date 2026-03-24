<template>
  <div class="admin-about-container">
    <!-- 헤더 영역 -->
    <div class="admin-header">
      <div class="header-main">
        <h2>📢 공지 및 패치 관리</h2>
        
        <!-- 탭 메뉴를 우측 끝으로 이동 -->
        <div class="admin-tabs">
          <button 
            :class="{ active: activeTab === 'NOTICE' }" 
            @click="activeTab = 'NOTICE'"
          >공지사항</button>
          <button 
            :class="{ active: activeTab === 'PATCH' }" 
            @click="activeTab = 'PATCH'"
          >패치노트</button>
        </div>
      </div>
    </div>

    <!-- 메인 컨텐츠 영역 -->
    <div class="admin-content-layout">
      <!-- 왼쪽: 목록 패널 -->
      <div class="list-panel card">
        <div class="panel-header">
          <!-- 제목 옆에 건수 표시 통합 -->
          <h3>{{ activeTab === 'NOTICE' ? '공지 목록' : '패치 목록' }} ({{ currentList.length }}건)</h3>
          <button class="btn-new" @click="createNew">➕ 신규 등록</button>
        </div>
        <div class="list-wrapper scrollable">
          <div 
            v-for="item in currentList" 
            :key="item.id" 
            class="list-item"
            :class="{ active: selectedItem?.id === item.id }"
            @click="selectItem(item)"
          >
            <div class="item-info">
              <span v-if="activeTab === 'PATCH'" class="version-tag">v{{ item.version }}</span>
              <span v-else class="notice-type-tag" :class="item.type">{{ item.tag }}</span>
              <span class="item-title">{{ item.title }}</span>
            </div>
            <span class="item-date">{{ activeTab === 'PATCH' ? formatDate(item.date) : item.date }}</span>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 상세/수정 패널 -->
      <div class="form-panel card">
        <div v-if="selectedItem || isCreating" class="form-wrapper">
          <div class="form-header">
            <h3>{{ isCreating ? '신규 등록' : '상세 정보 및 수정' }}</h3>
            <button v-if="!isCreating" class="btn-delete" @click="handleDelete">삭제하기</button>
          </div>
          
          <div class="form-body scrollable">
            <div class="form-grid">
              <div class="form-group full-width">
                <label>제목</label>
                <input v-model="form.title" placeholder="제목을 입력하세요" class="form-input" />
              </div>
              
              <!-- 공지사항 전용 필드 -->
              <template v-if="activeTab === 'NOTICE'">
                <div class="form-group">
                  <label>게시 날짜</label>
                  <input type="date" v-model="form.date" class="form-input" />
                </div>
                <div class="form-group">
                  <label>공지 태그</label>
                  <select v-model="form.tag" class="form-select" @change="updateNoticeType">
                    <option v-for="tag in noticeTags" :key="tag.value" :value="tag.value">
                      {{ tag.label }}
                    </option>
                  </select>
                </div>
                <!-- [시니어 조치] 실시간 알림 전송 옵션 추가 -->
                <div class="form-group full-width checkbox-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="form.sendNotification" />
                    <span class="checkbox-text">저장 시 모든 유저에게 실시간 알림 발송 📢</span>
                  </label>
                  <p class="form-tip">※ 중요한 공지일 경우에만 체크해 주세요. 잦은 알림은 유저 피로도를 높일 수 있습니다.</p>
                </div>
              </template>

              <!-- 패치노트 전용 필드 -->
              <template v-if="activeTab === 'PATCH'">
                <div class="form-group">
                  <label>버전</label>
                  <input v-model="form.version" placeholder="예: 1.0.0" class="form-input" />
                </div>
                <div class="form-group">
                  <label>패치 날짜</label>
                  <input type="date" v-model="form.patchDate" class="form-input" />
                </div>
              </template>

              <div class="form-group full-width">
                <label>내용 (패치노트는 줄바꿈으로 구분)</label>
                <textarea 
                  v-model="form.content" 
                  placeholder="내용을 입력하세요" 
                  class="form-textarea large"
                ></textarea>
              </div>
            </div>
          </div>

          <div class="form-footer">
            <button class="btn-secondary" @click="cancelEdit">취소</button>
            <button class="btn-primary" @click="handleSave">{{ isCreating ? '등록하기' : '수정완료' }}</button>
          </div>
        </div>
        <div v-else class="empty-form">
          <div class="empty-icon">📝</div>
          <p>목록에서 항목을 선택하거나<br/>새로운 내용을 등록해 주세요.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import axios from '@/plugins/axios'

const activeTab = ref('NOTICE')
const notices = ref([])
const patches = ref([])
const selectedItem = ref(null)
const isCreating = ref(false)

const noticeTags = [
  { label: '📌 중요', value: '중요', type: 'notice' },
  { label: '📢 안내', value: '안내', type: 'info' },
  { label: '📝 공지', value: '공지', type: 'info' },
  { label: '🎁 이벤트', value: '이벤트', type: 'info' },
  { label: '⚙️ 점검', value: '점검', type: 'notice' },
  { label: '🚨 긴급', value: '긴급', type: 'notice' },
  { label: '🆕 업데이트', value: '업데이트', type: 'info' }
]

const form = ref({ 
  title: '', 
  content: '', 
  version: '', 
  patchDate: '', 
  date: '', 
  tag: '안내', 
  type: 'info',
  sendNotification: false 
})

const currentList = computed(() => activeTab.value === 'NOTICE' ? notices.value : patches.value)

const fetchItems = async () => {
  try {
    const [noticeRes, patchRes] = await Promise.all([
      axios.get('/api/announce-patch/notices'),
      axios.get('/api/announce-patch/patches')
    ])
    notices.value = noticeRes.data.data
    patches.value = patchRes.data.data
  } catch (e) { console.error('데이터 로드 실패') }
}

watch(activeTab, () => { selectedItem.value = null; isCreating.value = false })

const selectItem = (item) => {
  isCreating.value = false
  selectedItem.value = item
  if (activeTab.value === 'NOTICE') {
    form.value = { 
      ...item,
      tag: item.rawTag || item.tag,
      date: item.date ? item.date.replace(/\./g, '-') : '',
      sendNotification: false
    }
  } else {
    form.value = { 
      ...item, 
      patchDate: item.date ? item.date.split('T')[0] : '',
      content: item.details ? item.details.join('\n') : '',
      sendNotification: false
    }
  }
}

const createNew = () => {
  selectedItem.value = null
  isCreating.value = true
  const today = new Date().toISOString().split('T')[0]
  if (activeTab.value === 'NOTICE') {
    form.value = { title: '', content: '', date: today, tag: '안내', type: 'info', sendNotification: false }
  } else {
    form.value = { title: '', content: '', version: '', patchDate: today, sendNotification: false }
  }
}

const cancelEdit = () => { selectedItem.value = null; isCreating.value = false }

const updateNoticeType = () => {
  const selectedTag = noticeTags.find(t => t.value === form.value.tag)
  if (selectedTag) form.value.type = selectedTag.type
}

const handleSave = async () => {
  if (!form.value.title.trim()) return alert('제목을 입력하세요.')
  const isNotice = activeTab.value === 'NOTICE'
  const url = isNotice ? '/api/announce-patch/notices' : '/api/announce-patch/patches'
  const payload = { ...form.value }
  if (isNotice) {
    if (payload.date) payload.date = payload.date.replace(/-/g, '.')
  } else {
    payload.details = form.value.content ? form.value.content.split('\n').filter(line => line.trim()) : []
    payload.date = form.value.patchDate
  }

  try {
    let savedItemId = selectedItem.value?.id
    if (isCreating.value) {
      const res = await axios.post(url, payload)
      alert('등록되었습니다.')
      savedItemId = res.data.data.id
    } else {
      await axios.put(`${url}/${selectedItem.value.id}`, payload)
      alert('수정되었습니다.')
    }
    await fetchItems()
    const newList = isNotice ? notices.value : patches.value
    const found = newList.find(item => item.id === savedItemId)
    if (found) selectItem(found)
    else cancelEdit()
  } catch (e) { alert('저장 실패') }
}

const handleDelete = async () => {
  if (!confirm('정말 삭제하시겠습니까?')) return
  const url = activeTab.value === 'NOTICE' ? '/api/announce-patch/notices' : '/api/announce-patch/patches'
  try {
    await axios.delete(`${url}/${selectedItem.value.id}`); alert('삭제되었습니다.'); await fetchItems(); cancelEdit()
  } catch (e) { alert('삭제 실패') }
}

const formatDate = (s) => s ? s.split('T')[0].replace(/-/g, '.') : '-'
onMounted(fetchItems)
</script>

<style scoped>
.admin-about-container { height: 100%; display: flex; flex-direction: column; gap: 15px; }

.admin-header { flex-shrink: 0; }
.header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.header-main h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: var(--text-primary); }

.admin-tabs { display: flex; background: var(--hover-bg); padding: 4px; border-radius: 10px; gap: 4px; }
.admin-tabs button { border: none; background: none; padding: 6px 15px; border-radius: 8px; font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; }
.admin-tabs button.active { background: var(--card-bg); color: var(--link-color); box-shadow: 0 2px 8px rgba(0,0,0,0.05); }

.admin-content-layout { flex: 1; display: flex; gap: 15px; min-height: 0; }
.card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; display: flex; flex-direction: column; }

/* 목록 패널 */
.list-panel { width: 350px; flex-shrink: 0; }
.panel-header { padding: 15px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.panel-header h3 { margin: 0; font-size: 0.95rem; font-weight: 800; }
.btn-new { 
  height: 32px; 
  padding: 0 12px; 
  background: var(--hover-bg); 
  border: 1px solid var(--border-color); 
  color: var(--text-primary); 
  border-radius: 8px; 
  font-weight: 700; 
  font-size: 0.78rem; 
  cursor: pointer; 
  transition: all 0.2s;
}
.btn-new:hover { background: var(--border-color); }

.list-wrapper { flex: 1; overflow-y: auto; padding: 10px; }
.list-item { padding: 12px; border-radius: 10px; cursor: pointer; transition: all 0.2s; margin-bottom: 5px; border: 1px solid transparent; }
.list-item:hover { background: var(--hover-bg); }
.list-item.active { background: rgba(0, 149, 246, 0.05); border-color: var(--link-color); }

.item-info { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; overflow: hidden; }
.version-tag { font-size: 0.65rem; font-weight: 800; color: white; background: var(--link-color); padding: 1px 5px; border-radius: 4px; flex-shrink: 0; }
.notice-type-tag { font-size: 0.6rem; font-weight: 800; padding: 1px 5px; border-radius: 4px; color: white; flex-shrink: 0; }
.notice-type-tag.notice { background-color: #ed4956; }
.notice-type-tag.info { background-color: var(--link-color); }
.item-title { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-date { font-size: 0.75rem; color: var(--text-secondary); }

/* 폼 패널 */
.form-panel { flex: 1; position: relative; }
.form-wrapper { padding: 20px; display: flex; flex-direction: column; height: 100%; box-sizing: border-box; }
.form-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-shrink: 0; }
.form-header h3 { margin: 0; font-size: 0.95rem; font-weight: 800; color: var(--text-primary); }
.btn-delete { padding: 4px 10px; border: 1px solid #ed4956; background: none; color: #ed4956; border-radius: 6px; font-size: 0.75rem; font-weight: 700; cursor: pointer; }

.form-body { flex: 1; overflow-y: auto; padding-right: 5px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group.full-width { grid-column: span 2; }
.form-group label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }

.form-input, .form-select, .form-textarea { width: 100%; height: 38px; box-sizing: border-box; padding: 0 12px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--hover-bg); color: var(--text-primary); font-size: 0.85rem; outline: none; transition: border-color 0.2s; }
.form-input:focus, .form-select:focus, .form-textarea:focus { border-color: var(--link-color); background: var(--card-bg); }
.form-textarea { height: auto; min-height: 200px; padding: 12px; resize: vertical; }
.form-textarea.large { min-height: 350px; }

/* [시니어 조치] 체크박스 그룹 스타일 */
.checkbox-group {
  margin-top: 5px;
  background: var(--hover-bg);
  padding: 12px 15px;
  border-radius: 10px;
  border: 1px dashed var(--border-color);
}
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--link-color);
}
.checkbox-text {
  font-size: 0.85rem;
  font-weight: 800;
  color: var(--text-primary);
}
.form-tip {
  margin: 8px 0 0;
  font-size: 0.72rem;
  color: var(--text-muted);
  font-weight: 500;
  line-height: 1.4;
}

.form-footer { margin-top: 15px; padding-top: 15px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; gap: 10px; flex-shrink: 0; }
.btn-primary { background: var(--link-color); color: white; border: none; height: 38px; padding: 0 25px; border-radius: 8px; font-weight: 800; font-size: 0.85rem; cursor: pointer; }
.btn-secondary { background: var(--tag-bg); color: var(--text-primary); border: none; height: 38px; padding: 0 20px; border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; }

.empty-form { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; color: var(--text-secondary); }
.empty-icon { font-size: 3rem; margin-bottom: 15px; opacity: 0.3; }

.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

@media (max-width: 992px) {
  .admin-header h2 { font-size: 1.1rem; }
  .header-main { align-items: center; margin-bottom: 15px; }
  .admin-tabs { width: fit-content; }
  .admin-about-container { height: auto; }
  .admin-content-layout { flex-direction: column; }
  .list-panel { width: 100%; height: 300px; }
  .form-panel { height: auto; min-height: 400px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-input, .form-select, .form-textarea, .btn-primary, .btn-secondary { height: 34px; font-size: 0.8rem; }
}
</style>
