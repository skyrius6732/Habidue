<template>
  <div class="about-container">
    <PageHeader 
      icon="ℹ️" 
      title="HabiDue 소개" 
      stats-text="현재 버전" 
      :stats-value="latestVersion"
      bio="더 쉽고 편리한 주거 복지 정보의 시작, HabiDue입니다."
    />

    <div class="about-content-inner">
      <!-- 탭 메뉴 -->
      <div class="about-tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.id" 
          class="tab-btn" 
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
        </button>
      </div>

      <div class="tab-content-wrapper">
        <!-- 1. 서비스 소개 탭 -->
        <div v-if="activeTab === 'intro'" class="tab-content fade-in">
          <section class="intro-section">
            <h1 class="logo-font pc-only-title">HabiDue</h1>
            <p class="subtitle">공공 주택 공고, 이제 HabiDue에서 한눈에 확인하세요.</p>
          </section>

          <section class="features-section">
            <div class="feature-card">
              <div class="card-icon">🏠</div>
              <h3>통합 공고 리스트</h3>
              <p>LH, SH 등 여러 기관의 흩어져 있는 공고를 한곳에 모아 피드 형태로 제공합니다.</p>
            </div>
            <div class="feature-card">
              <div class="card-icon">📅</div>
              <h3>스마트 캘린더</h3>
              <p>마감 임박 공고를 달력 형식으로 시각화하여 중요한 신청 기회를 놓치지 않게 도와줍니다.</p>
            </div>
            <div class="feature-card">
              <div class="card-icon">❤️</div>
              <h3>관심 공고 관리</h3>
              <p>마음에 드는 공고는 하트를 눌러 스크랩하고, 나만의 메모를 남겨 효율적으로 관리하세요.</p>
            </div>
            <div class="feature-card">
              <div class="card-icon">🔔</div>
              <h3>키워드 맞춤 알림</h3>
              <p>나의 주거 희망 지역이나 유형을 알림 키워드로 등록하면 매칭되는 공고를 우선적으로 보여줍니다.</p>
            </div>
          </section>

          <section class="mission-section">
            <p>"주거 정보의 불균형을 해소하고, 누구나 평등하게 주거 복지 정보를 누리는 세상을 꿈꿉니다."</p>
          </section>
        </div>

        <!-- 2. 공지사항 탭 -->
        <div v-if="activeTab === 'news'" class="tab-content fade-in">
          <div v-if="announcements.length === 0" class="empty-msg">공지사항이 없습니다.</div>
          <div class="announcement-list">
            <div v-for="news in announcements" :key="news.id" class="news-item">
              <div class="news-header">
                <span class="news-tag" :class="news.type">{{ news.tag }}</span>
                <span class="news-date">{{ news.date }}</span>
              </div>
              <h4 class="news-title">{{ news.title }}</h4>
              <p class="news-content">{{ news.content }}</p>
            </div>
          </div>
        </div>

        <!-- 3. 패치 히스토리 탭 -->
        <div v-if="activeTab === 'patch'" class="tab-content fade-in">
          <div v-if="patchNotes.length === 0" class="empty-msg">패치 내역이 없습니다.</div>
          <div class="timeline">
            <div v-for="patch in patchNotes" :key="patch.id" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="patch-header">
                  <span class="patch-version">{{ patch.version }}</span>
                  <span class="patch-date">{{ formatDateDot(patch.date) }}</span>
                </div>
                <h4 class="patch-title">{{ patch.title }}</h4>
                <ul class="patch-details">
                  <li v-for="(detail, sIdx) in patch.details" :key="sIdx">
                    <span class="detail-bullet">-</span> {{ detail }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. 고객센터 탭 -->
        <div v-if="activeTab === 'support'" class="tab-content fade-in">
          <div v-if="!authStore.isAuthenticated" class="empty-msg">
            <p>고객센터 이용을 위해 로그인이 필요합니다.</p>
            <button class="login-prompt-btn" @click="router.push('/')">로그인하러 가기</button>
          </div>
          
          <div v-else class="support-wrapper">
            <!-- 1) 문의하기 폼 -->
            <section class="support-form-section">
              <h3 class="section-title">📫 새로운 문의하기</h3>
              <div class="inquiry-form-card">
                <div class="form-row">
                  <label>문의 유형</label>
                  <select v-model="inquiryForm.category">
                    <option v-for="cat in inquiryCategories" :key="cat.id" :value="cat.id">
                      {{ cat.name }}
                    </option>
                  </select>
                </div>
                <div class="form-row">
                  <label>제목</label>
                  <input v-model="inquiryForm.title" type="text" placeholder="문의 제목을 입력해주세요.">
                </div>
                <div class="form-row">
                  <label>내용</label>
                  <textarea v-model="inquiryForm.content" rows="6" placeholder="문의하실 내용을 상세히 적어주세요."></textarea>
                </div>
                <!-- 이미지 첨부 -->
                <div class="form-row">
                  <label>이미지 첨부 (선택)</label>
                  <div class="file-upload-wrapper">
                    <input type="file" accept="image/*" @change="handleImageUpload" ref="fileInput" class="hidden-input" id="inquiry-image">
                    <label for="inquiry-image" class="file-label">
                      <span v-if="!imagePreview">📸 이미지 선택하기</span>
                      <img v-else :src="imagePreview" class="preview-img">
                    </label>
                    <button v-if="imagePreview" class="remove-img-btn" @click.stop="removeImage">×</button>
                  </div>
                </div>
                <div class="form-footer">
                  <p class="form-tip">* 문의하신 내용은 비공개로 접수되며, 답변 완료 시 알림이 발송됩니다.</p>
                  <button class="submit-btn" :disabled="isSubmitting || !isFormValid" @click="submitInquiry">
                    {{ isSubmitting ? '접수 중...' : '문의 접수하기' }}
                  </button>
                </div>
              </div>
            </section>

            <!-- 2) 내 문의 내역 -->
            <section class="my-inquiries-section">
              <h3 class="section-title">📋 내 문의 내역</h3>
              <div v-if="inquiryList.length === 0" class="empty-inquiry">
                등록된 문의 내역이 없습니다.
              </div>
              <div v-else class="inquiry-list">
                <!-- [시니어 조치] 포커싱을 위해 각 아이템에 ID 부여 -->
                <div 
                  v-for="item in inquiryList" 
                  :key="item.id" 
                  :id="'inquiry-' + item.id"
                  class="inquiry-item-card"
                >
                  <div class="inquiry-header" @click="toggleInquiry(item.id)">
                    <div class="inquiry-main">
                      <div class="header-top-meta">
                        <span class="category-tag">{{ item.categoryDescription }}</span>
                        <span class="status-badge-inline" :class="item.status.toLowerCase()">{{ item.statusDescription }}</span>
                      </div>
                      <h4 class="inquiry-title">{{ item.title }}</h4>
                      <div class="header-bottom-meta">
                        <span class="inquiry-date-sub">{{ formatDateDot(item.createdAt) }}</span>
                      </div>
                    </div>
                    <div class="inquiry-meta-status">
                      <span class="toggle-icon-large">{{ expandedInquiry === item.id ? '▲' : '▼' }}</span>
                    </div>
                  </div>
                  
                  <div v-if="expandedInquiry === item.id" class="inquiry-body fade-in">
                    <div class="question-box">
                      <p class="content-text">{{ item.content }}</p>
                      <div v-if="item.imageUrl" class="attached-image-wrapper">
                        <img :src="item.imageUrl" class="attached-img" @click.stop="openImage(item.imageUrl)">
                      </div>
                    </div>
                    
                    <div v-if="item.answer" class="answer-box">
                      <div class="answer-header">
                        <div class="admin-profile-mini">
                          <span class="admin-icon-shield">🛡️</span>
                          <span class="admin-label-text">운영자 답변</span>
                        </div>
                        <span class="answer-date-text">{{ formatDateDot(item.answeredAt) }}</span>
                      </div>
                      <div class="answer-content-text">{{ item.answer }}</div>
                    </div>
                    <!-- [시니어 조치] 워딩 및 이모지 변경: 실시간 대기 압박 제거 -->
                    <div v-else class="pending-status-box">
                      <div class="pending-icon-static">📩</div>
                      <p class="pending-text">문의가 정상적으로 접수되었습니다.<br>운영자가 확인 후 정성껏 답변 드릴게요!</p>
                    </div>
                    
                    <div class="inquiry-actions-footer">
                      <button class="delete-inquiry-btn" @click.stop="deleteInquiry(item.id)">
                        <span class="del-icon">🗑️</span> 이 문의 삭제하기
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import axios from '@/plugins/axios'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const activeTab = ref(route.query.tab || 'intro')
const tabs = [
  { id: 'intro', name: '서비스 소개' },
  { id: 'news', name: '공지사항' },
  { id: 'patch', name: '패치 히스토리' },
  { id: 'support', name: '고객센터' }
]

// [시니어 조치] URL 쿼리 파라미터 감시하여 탭 전환 및 특정 문의 포커싱
watch(() => route.query.tab, (newTab) => {
  if (newTab) activeTab.value = newTab
})


// 고객센터 관련 데이터
const inquiryCategories = [
  { id: 'BUG_REPORT', name: '🐛 서비스 버그/오류' },
  { id: 'FEATURE_SUGGESTION', name: '💡 기능 제안' },
  { id: 'DATA_CORRECTION', name: '🏠 주거 정보 수정' },
  { id: 'ACCOUNT_INQUIRY', name: '🔑 계정/인증 문의' },
  { id: 'PARTNERSHIP', name: '🤝 제휴/광고 문의' },
  { id: 'OTHER', name: '❓ 기타 문의' }
]

const inquiryForm = reactive({
  category: 'BUG_REPORT',
  title: '',
  content: ''
})

const selectedFile = ref(null)
const imagePreview = ref(null)
const fileInput = ref(null)

const isSubmitting = ref(false)
const inquiryList = ref([])
const expandedInquiry = ref(null)

const isFormValid = computed(() => {
  return inquiryForm.title.trim().length >= 2 && inquiryForm.content.trim().length >= 5
})

const handleImageUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    await uiStore.showAlert('이미지 크기는 5MB를 초과할 수 없습니다.', '용량 초과')
    return
  }
  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (event) => { imagePreview.value = event.target.result }
  reader.readAsDataURL(file)
}

const removeImage = () => {
  selectedFile.value = null
  imagePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const openImage = (url) => { window.open(url, '_blank') }

// 특정 문의로 스크롤 및 확장
const focusInquiry = (id) => {
  const targetId = Number(id)
  expandedInquiry.value = targetId

  // 데이터 로드 후 DOM 렌더링 시간을 고려하여 지연 스크롤
  setTimeout(() => {
    const el = document.getElementById(`inquiry-${targetId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('highlight-focus')
      setTimeout(() => el.classList.remove('highlight-focus'), 2000)
    }
  }, 600)
}

watch(() => route.query.inquiryId, (newId) => {
  if (newId) {
    activeTab.value = 'support'
    focusInquiry(newId)
  }
}, { immediate: true })

const announcements = ref([])
const patchNotes = ref([])

const latestVersion = computed(() => {
  return patchNotes.value.length > 0 ? patchNotes.value[0].version : 'v1.0.0'
})

const formatDateDot = (s) => s ? s.split('T')[0].replace(/-/g, '.') : '-';

const toggleInquiry = (id) => {
  expandedInquiry.value = expandedInquiry.value === id ? null : id
}

const fetchInquiries = async () => {
  if (!authStore.isAuthenticated) return
  try {
    const res = await axios.get('/api/inquiries/my')
    inquiryList.value = res.data.data.content
    
    // 진입 시 특정 문의 ID가 있다면 포커싱 수행
    if (route.query.inquiryId) {
      focusInquiry(route.query.inquiryId)
    }
  } catch (e) {
    console.error('내 문의 내역 로드 실패:', e)
  }
}

const submitInquiry = async () => {
  if (!isFormValid.value) return
  isSubmitting.value = true
  
  const formData = new FormData()
  formData.append('inquiry', new Blob([JSON.stringify(inquiryForm)], { type: 'application/json' }))
  if (selectedFile.value) {
    formData.append('image', selectedFile.value)
  }

  try {
    await axios.post('/api/inquiries', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    await uiStore.showAlert('문의가 정상적으로 접수되었습니다. 답변 완료 시 알림을 보내드릴게요!', '접수 완료')
    inquiryForm.title = ''
    inquiryForm.content = ''
    removeImage()
    fetchInquiries()
  } catch (e) {
    await uiStore.showAlert('문의 접수 중 오류가 발생했습니다.', '오류')
  } finally {
    isSubmitting.value = false
  }
}

const deleteInquiry = async (id) => {
  if (!await uiStore.showConfirm('정말로 이 문의 내역을 삭제하시겠습니까?', '삭제 확인')) return
  try {
    await axios.delete(`/api/inquiries/${id}`)
    fetchInquiries()
  } catch (e) {
    await uiStore.showAlert('삭제 실패', '오류')
  }
}

const fetchData = async () => {
  try {
    const [newsRes, patchRes] = await Promise.all([
      axios.get('/api/announce-patch/notices'),
      axios.get('/api/announce-patch/patches')
    ])
    announcements.value = newsRes.data.data
    patchNotes.value = patchRes.data.data
  } catch (e) {
    console.error('About 데이터 로드 실패:', e)
  }
}

onMounted(() => {
  fetchData()
  fetchInquiries()
})
</script>

<style scoped>
.about-container { width: 100%; min-height: 100vh; background-color: var(--bg-color); }
.about-content-inner { max-width: 935px; margin: 0 auto; padding: 40px 20px; }

/* 탭 UI */
.about-tabs { display: flex; justify-content: center; gap: 10px; margin-bottom: 50px; border-bottom: 1px solid var(--border-color); padding-bottom: 1px; }
.tab-btn { background: none; border: none; padding: 15px 30px; font-size: 1rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; position: relative; transition: all 0.2s; }
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--link-color); }
.tab-btn.active::after { content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 2px; background-color: var(--link-color); }

.tab-content-wrapper { min-height: 400px; }
.fade-in { animation: fadeIn 0.3s ease-in-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* 1. 소개 섹션 */
.intro-section { text-align: center; margin-bottom: 60px; }
.logo-font { font-family: 'Grand Hotel', cursive; font-size: 4rem; color: var(--text-primary); margin-bottom: 10px; }
.subtitle { font-size: 1.2rem; color: var(--text-secondary); font-weight: 500; }

.features-section { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.feature-card { background: var(--card-bg); padding: 30px 20px; border-radius: 16px; border: 1px solid var(--border-color); text-align: center; transition: all 0.3s ease; }
.feature-card:hover { transform: translateY(-5px); border-color: var(--link-color); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }
.card-icon { font-size: 2.5rem; margin-bottom: 20px; }
.feature-card h3 { font-size: 1.15rem; font-weight: 700; margin-bottom: 12px; color: var(--text-primary); }
.feature-card p { font-size: 0.9rem; color: var(--text-secondary); line-height: 1.6; word-break: keep-all; }

/* 2. 공지사항 */
.announcement-list { display: flex; flex-direction: column; gap: 15px; }
.news-item { background: var(--card-bg); padding: 24px; border-radius: 12px; border: 1px solid var(--border-color); transition: border-color 0.2s; }
.news-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.news-tag { font-size: 0.72rem; font-weight: 800; padding: 4px 10px; border-radius: 6px; display: flex; align-items: center; gap: 4px; }
.news-tag.notice { background-color: rgba(237, 73, 86, 0.1); color: #ed4956; border: 1px solid rgba(237, 73, 86, 0.2); }
.news-tag.info { background-color: rgba(0, 149, 246, 0.1); color: var(--link-color); border: 1px solid rgba(0, 149, 246, 0.2); }
.news-date { font-size: 0.8rem; color: var(--text-secondary); font-weight: 500; }
.news-title { font-size: 1.1rem; font-weight: 700; color: var(--text-primary); margin: 0 0 10px 0; }
.news-content { font-size: 0.92rem; color: var(--text-secondary); line-height: 1.6; margin: 0; }

/* 3. 패치 히스토리 */
.timeline { position: relative; padding-left: 30px; margin-top: 20px; }
.timeline::before { content: ''; position: absolute; left: 7px; top: 10px; bottom: 10px; width: 2px; background-color: var(--border-color); }
.timeline-item { position: relative; margin-bottom: 40px; }
.timeline-item:last-child { margin-bottom: 0; }
.timeline-dot { position: absolute; left: -30px; top: 8px; width: 16px; height: 16px; border-radius: 50%; background-color: var(--card-bg); border: 3px solid var(--link-color); z-index: 1; }
.patch-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 15px; }
.patch-version { font-size: 1.2rem; font-weight: 800; color: var(--link-color); }
.patch-date { font-size: 0.85rem; color: var(--text-secondary); font-weight: 500; }
.patch-title { font-size: 1.05rem; font-weight: 800; color: var(--text-primary); margin: 0 0 12px 0; line-height: 1.4; }
.patch-details { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.patch-details li { font-size: 0.92rem; color: var(--text-primary); line-height: 1.5; display: flex; gap: 8px; }
.detail-bullet { color: var(--link-color); font-weight: bold; }

/* 4. 고객센터 스타일 */
.support-wrapper { display: flex; flex-direction: column; gap: 40px; }
.section-title { font-size: 1.3rem; font-weight: 800; margin-bottom: 20px; color: var(--text-primary); }
.inquiry-form-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 30px; }
.form-row { margin-bottom: 20px; display: flex; flex-direction: column; gap: 8px; }
.form-row label { font-size: 0.9rem; font-weight: 700; color: var(--text-secondary); }
.form-row input, .form-row select, .form-row textarea { 
  background: var(--bg-color); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px; 
  font-size: 0.95rem; color: var(--text-primary); transition: border-color 0.2s;
}
.form-row input:focus, .form-row select:focus, .form-row textarea:focus { outline: none; border-color: var(--link-color); }

.file-upload-wrapper { position: relative; display: flex; align-items: flex-start; gap: 15px; }
.hidden-input { display: none; }
.file-label { 
  width: 120px; height: 120px; border: 2px dashed var(--border-color); border-radius: 12px; 
  display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden;
}
.file-label span { font-size: 0.8rem; color: var(--text-secondary); text-align: center; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }
.remove-img-btn { position: absolute; top: -10px; left: 110px; background: #ed4956; color: white; border: none; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; }

.attached-image-wrapper { margin-top: 15px; }
.attached-img { max-width: 300px; max-height: 200px; border-radius: 8px; border: 1px solid var(--border-color); cursor: zoom-in; }

.form-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; flex-wrap: wrap; gap: 15px; }
.form-tip { font-size: 0.8rem; color: var(--text-secondary); }
.submit-btn { background-color: var(--link-color); color: white; border: none; padding: 12px 25px; border-radius: 10px; font-weight: 700; cursor: pointer; }
.submit-btn:disabled { opacity: 0.5; }

/* 내 문의 리스트 */
.inquiry-list { display: flex; flex-direction: column; gap: 15px; }
.inquiry-item-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; transition: all 0.3s ease; }
.inquiry-item-card:hover { border-color: var(--link-color); }
.inquiry-header { padding: 18px 20px; display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.inquiry-main { display: flex; flex-direction: column; gap: 6px; flex: 1; text-align: left; }
.header-top-meta { display: flex; align-items: center; gap: 8px; }
.category-tag { font-size: 0.65rem; font-weight: 800; background: var(--hover-bg); color: var(--text-secondary); padding: 2px 8px; border-radius: 4px; text-transform: uppercase; }
.status-badge-inline { font-size: 0.65rem; font-weight: 800; padding: 2px 8px; border-radius: 4px; }
.status-badge-inline.received { background: #eee; color: #666; }
.status-badge-inline.answered { background: rgba(0, 153, 117, 0.1); color: #009975; }

.inquiry-title { font-size: 1.05rem; font-weight: 700; color: var(--text-primary); margin: 2px 0; }
.inquiry-date-sub { font-size: 0.75rem; color: var(--text-secondary); font-weight: 500; font-family: monospace; }

.inquiry-meta-status { display: flex; align-items: center; padding-left: 15px; }
.toggle-icon-large { font-size: 0.9rem; color: var(--text-secondary); opacity: 0.5; }

.inquiry-body { padding: 25px; border-top: 1px solid var(--border-color); background-color: var(--bg-color); text-align: left; }
.content-text { font-size: 0.95rem; line-height: 1.7; color: var(--text-primary); white-space: pre-wrap; margin: 0; }

/* 운영자 답변 영역 개편 */
.answer-box { margin-top: 30px; padding: 20px; background: var(--card-bg); border-radius: 12px; border: 1px solid var(--border-color); position: relative; }
.answer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 1px dashed var(--border-color); padding-bottom: 12px; }
.admin-profile-mini { display: flex; align-items: center; gap: 8px; }
.admin-icon-shield { font-size: 1.1rem; }
.admin-label-text { font-size: 0.9rem; font-weight: 800; color: var(--link-color); }
.answer-date-text { font-size: 0.75rem; color: var(--text-secondary); font-weight: 500; font-family: monospace; }
.answer-content-text { font-size: 0.95rem; color: var(--text-primary); line-height: 1.7; white-space: pre-wrap; }

/* 운영자 확인 중 디자인 개선 */
.pending-status-box { 
  margin-top: 30px; padding: 35px 20px; 
  background: linear-gradient(145deg, var(--card-bg), var(--hover-bg)); 
  border-radius: 12px; border: 1px dashed var(--border-color);
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  text-align: center;
}
.pending-icon-static { font-size: 2rem; margin-bottom: 5px; filter: drop-shadow(0 4px 8px rgba(0,0,0,0.1)); }
.pending-text { font-size: 0.92rem; color: var(--text-secondary); font-weight: 600; margin: 0; line-height: 1.6; white-space: pre-line; }

/* 하단 삭제 액션 */
.inquiry-actions-footer { margin-top: 30px; display: flex; justify-content: flex-end; padding-top: 15px; border-top: 1px solid var(--border-color); }
.delete-inquiry-btn { background: none; border: 1px solid transparent; color: var(--text-secondary); font-size: 0.8rem; font-weight: 600; cursor: pointer; padding: 6px 12px; border-radius: 6px; display: flex; align-items: center; gap: 6px; transition: all 0.2s; }
.delete-inquiry-btn:hover { color: #ed4956; background-color: rgba(237, 73, 86, 0.05); border-color: rgba(237, 73, 86, 0.1); }
.del-icon { font-size: 0.9rem; }

/* 하이라이트 효과 */
.highlight-focus { animation: highlight-pulse 2s ease-out; border-color: var(--link-color) !important; }
@keyframes highlight-pulse {
  0% { box-shadow: 0 0 0 0 rgba(0, 149, 246, 0.4); background-color: var(--hover-bg); }
  100% { box-shadow: 0 0 0 20px rgba(0, 149, 246, 0); background-color: transparent; }
}

.login-prompt-btn { margin-top: 20px; background: var(--link-color); color: white; border: none; padding: 12px 30px; border-radius: 10px; font-weight: 700; cursor: pointer; }
.empty-msg { text-align: center; padding: 100px 0; color: var(--text-secondary); font-size: 0.95rem; }
.mission-section { text-align: center; padding: 60px 40px; background-color: var(--hover-bg); border-radius: 20px; font-style: italic; font-size: 1.1rem; color: var(--text-primary); line-height: 1.8; margin-top: 60px; }
.empty-inquiry { text-align: center; padding: 60px 0; color: var(--text-secondary); border: 1px dashed var(--border-color); border-radius: 12px; }

@media (max-width: 768px) {
  .pc-only-title { display: none; }
  .about-content-inner { margin: 0 auto; padding: 20px 15px; }
  .features-section { grid-template-columns: 1fr; }
  .form-footer { flex-direction: column; align-items: stretch; }
  .submit-btn { width: 100%; }
}
</style>
