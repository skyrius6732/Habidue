<template>
  <div class="interests-container">
    <PageHeader 
      icon="❤️" 
      title="관심 공고" 
      stats-text="게시물" 
      :stats-value="userNotices.length"
      bio="내가 스크랩한 소중한 공고 목록을 한눈에 관리하세요."
    />

    <div v-if="loading && userNotices.length === 0" class="status-msg">공고를 불러오는 중...</div>
    
    <div v-else-if="!loading && userNotices.length === 0" class="no-data-container">
      <div class="empty-card">
        <div class="empty-icon">✨</div>
        <h3 class="empty-title">관심 공고가 비어있어요</h3>
        <p class="empty-subtext">아직 관심 등록한 공고가 없습니다.<br/>나에게 맞는 공고를 찾아 저장해 보세요.</p>
        <RouterLink to="/notices" class="explore-btn">공고 탐색하러 가기</RouterLink>
      </div>
    </div>

    <div v-else class="grid-container">
      <div 
        v-for="un in userNotices" 
        :key="un.id" 
        class="grid-item"
        @touchstart="showOverlay(un.id)"
        @touchend="hideOverlay"
        @touchcancel="hideOverlay"
      >
        <div class="grid-content" :class="[un.noticeSource, { 'is-expired': isExpired(un.noticeDeadline) }]" @click="openDetail(un)">
          <div class="card-top">
            <span class="agency-badge">{{ un.noticeSource === 'PRIVATE' ? '민간' : (un.noticeSource || '정보 없음') }}</span>
            <span v-if="un.memo" class="memo-indicator">📝</span>
          </div>
          <div class="card-center">
            <span class="d-day-label" :class="{ 'red-text': isExpired(un.noticeDeadline), 'no-date': !un.noticeDeadline }">
              {{ calculateDDay(un.noticeDeadline) }}
              <small v-if="!un.noticeDeadline" class="no-date-sub">본문 공고를 확인해 주세요.</small>
            </span>
          </div>
          <div class="card-bottom">
            <div class="region-tag">{{ extractRegion(un.noticeTitle) }}</div>
            <div class="notice-short-title">{{ un.noticeTitle || '제목 없음' }}</div>
          </div>
        </div>
        <button class="grid-heart-btn" @click.stop="removeFavorite(un.id)">❤️</button>
        <div class="overlay" :class="{ 'force-show': activeOverlayId === un.id }" @click="openDetail(un)">
          <div class="overlay-info">
            <p class="overlay-memo">{{ un.memo || '작성 메모가 없습니다.' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 상세 모달 -->
    <div v-if="selectedItem" class="modal-overlay" @click="closeModal">
      <div class="modal-card detail-modal" @click.stop>
        <div class="modal-header">
          <span class="modal-source-tag" :class="selectedItem.noticeSource">{{ selectedItem.noticeSource === 'PRIVATE' ? '민간' : selectedItem.noticeSource }} 공고</span>
          <div class="header-actions">
            <button @click="closeModal" class="modal-close-icon">&times;</button>
          </div>
        </div>
        <div class="modal-body">
          <h2 class="detail-title">{{ selectedItem.noticeTitle }}</h2>
          <div class="detail-grid">
            <div class="detail-info-item">
              <span class="info-label">공고 기간</span>
              <span v-if="selectedItem.noticeAnnouncementDate || selectedItem.noticeDeadline || selectedItem.noticeResultDate" class="info-value" :class="{ 'text-danger': isExpired(selectedItem.noticeDeadline) }">
                {{ formatDateDot(selectedItem.noticeAnnouncementDate) }} 
                <template v-if="selectedItem.noticeDeadline || selectedItem.noticeResultDate">
                  ~ {{ formatDateDot(selectedItem.noticeDeadline || selectedItem.noticeResultDate) }}
                </template>
                <span v-if="isExpired(selectedItem.noticeDeadline)" class="expired-text">(마감됨)</span>
              </span>
              <span v-else class="info-value text-muted">일정 정보 없음 (본문 공고 확인)</span>
            </div>
          </div>

          <!-- 참고 URL 섹션 -->
          <div class="reference-section">
            <label class="section-label">참고 URL</label>
            
            <div class="saved-urls-list">
              <div v-for="(url, idx) in savedUrls" :key="idx" class="url-badge-item">
                <a :href="url" target="_blank" class="url-link-text">{{ truncateUrl(url) }}</a>
                <button v-if="!isExpired(selectedItem.noticeDeadline)" @click="removeSavedUrl(idx)" class="url-delete-btn">&times;</button>
              </div>
            </div>

            <div v-if="!isExpired(selectedItem.noticeDeadline)" class="url-input-area">
              <div class="input-with-btn">
                <input 
                  v-model="newUrl" 
                  class="url-input" 
                  placeholder="이 공고와 관련된 URL을 남겨보세요."
                  @keyup.enter="addUrlToSaved"
                />
                <button @click="addUrlToSaved" class="add-url-btn">+</button>
              </div>
            </div>
          </div>

          <div class="memo-edit-section">
            <label class="section-label">나의 메모</label>
            <textarea v-model="selectedItem.memo" class="modal-memo" :readonly="isExpired(selectedItem.noticeDeadline)" placeholder="이 공고에 대한 메모를 남겨보세요."></textarea>
          </div>

          <a :href="isExpired(selectedItem.noticeDeadline) ? 'javascript:void(0)' : selectedItem.noticeLink" target="_blank" class="main-apply-btn" :class="{ 'disabled-btn': isExpired(selectedItem.noticeDeadline) }">
            {{ isExpired(selectedItem.noticeDeadline) ? '신청이 마감된 공고입니다' : '🔗 원문 공고보기' }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/plugins/axios'
import PageHeader from '@/components/PageHeader.vue'

const userNotices = ref([])
const loading = ref(false)
const selectedItem = ref(null)
const activeOverlayId = ref(null)

// URL 관리 상태
const savedUrls = ref([])
const newUrl = ref('')

const fetchInterests = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/user-notices', { params: { size: 100 } })
    if (response.data && response.data.data) { userNotices.value = response.data.data.content || [] }
  } catch (error) { console.error(error) } finally { loading.value = false }
}

const openDetail = (item) => { 
  selectedItem.value = JSON.parse(JSON.stringify(item));
  savedUrls.value = item.referenceUrls ? item.referenceUrls.split(',').filter(u => u.trim() !== '') : [];
  newUrl.value = '';
}

const addUrlToSaved = () => {
  if (!newUrl.value.trim()) return;
  let url = newUrl.value.trim();
  if (!url.startsWith('http')) url = 'https://' + url;
  savedUrls.value.push(url);
  newUrl.value = '';
}

const removeSavedUrl = (index) => {
  savedUrls.value.splice(index, 1);
}

const truncateUrl = (url) => {
  if (url.length > 40) return url.substring(0, 37) + '...';
  return url;
}

// 모달을 닫을 때 자동 저장하는 함수
const closeModal = async () => {
  if (selectedItem.value && !isExpired(selectedItem.value.noticeDeadline)) {
    try {
      // 현재 입력창에 남아있는 URL도 추가
      if (newUrl.value.trim()) {
        let url = newUrl.value.trim();
        if (!url.startsWith('http')) url = 'https://' + url;
        savedUrls.value.push(url);
      }
      
      const urlsString = savedUrls.value.join(',');
      await axios.put(`/api/user-notices/${selectedItem.value.id}`, {
        memo: selectedItem.value.memo,
        referenceUrls: urlsString
      });
      fetchInterests(); // 리스트 갱신
    } catch (error) {
      console.error('자동 저장 실패:', error);
    }
  }
  selectedItem.value = null;
}

const removeFavorite = async (id) => {
  try {
    await axios.delete(`/api/user-notices/${id}`);
    fetchInterests();
  } catch (error) { console.error(error); }
}

const isExpired = (d) => d && new Date(d).setHours(23,59,59,999) < new Date();
const calculateDDay = (d) => {
  if (!d) return '일정 정보 없음';
  const diff = Math.ceil((new Date(d).setHours(0,0,0,0) - new Date().setHours(0,0,0,0)) / 86400000);
  return diff === 0 ? 'D-Day' : diff < 0 ? `D+${Math.abs(diff)}` : `D-${diff}`;
}
const extractRegion = (t) => {
  const regions = ['서울', '경기', '인천', '부산', '남양주', '수원', '성남', '용인', '세종', '제주', '대구', '광주', '화성', '안양', '고양'];
  for (const r of regions) if (t?.includes(r)) return r;
  return '전국';
}

const showOverlay = (id) => { activeOverlayId.value = id; }
const hideOverlay = () => { activeOverlayId.value = null; }
const formatDateDot = (s) => s ? s.split('T')[0].replace(/-/g, '.') : '-';
const formatDateShort = (s) => s ? `${new Date(s).getMonth()+1}/${new Date(s).getDate()}` : '-';
onMounted(() => fetchInterests())
</script>

<style scoped>
.interests-container { width: 100%; max-width: 1200px; margin: 0 auto; padding-bottom: 50px; }
.grid-container { margin-top: 1.5rem; display: grid; grid-template-columns: repeat(3, 1fr); gap: 28px; padding: 0 20px; }

/* Empty State Styling */
.no-data-container { display: flex; justify-content: center; align-items: center; min-height: 400px; padding: 0 20px; }
.empty-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 20px; padding: 40px; text-align: center; max-width: 400px; width: 100%; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
.empty-icon { font-size: 3rem; margin-bottom: 1.5rem; }
.empty-title { font-size: 1.25rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.75rem; }
.empty-subtext { font-size: 0.95rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 2rem; }
.explore-btn { display: inline-block; background-color: var(--link-color); color: white; padding: 12px 32px; border-radius: 12px; font-weight: 700; text-decoration: none; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0, 149, 246, 0.2); }
.explore-btn:hover { transform: translateY(-2px); opacity: 0.9; box-shadow: 0 6px 16px rgba(0, 149, 246, 0.3); }
.grid-item { aspect-ratio: 1 / 1; border-radius: 8px; border: 1px solid var(--border-color); position: relative; cursor: pointer; overflow: hidden; background-color: var(--card-bg); user-select: none; }
.grid-heart-btn { position: absolute; top: 10px; right: 10px; background: white; border: 1px solid #efefef; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.1rem; cursor: pointer; z-index: 10; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
.grid-content { width: 100%; height: 100%; display: flex; flex-direction: column; justify-content: space-between; padding: 15px; box-sizing: border-box; }
.grid-content.LH:not(.is-expired) { background: linear-gradient(135deg, rgba(56, 161, 105, 0.1) 0%, rgba(56, 161, 105, 0.25) 100%); }
.grid-content.SH:not(.is-expired) { background: linear-gradient(135deg, rgba(49, 130, 206, 0.1) 0%, rgba(49, 130, 206, 0.25) 100%); }
.grid-content.PRIVATE:not(.is-expired), .grid-content.민간:not(.is-expired) { background: linear-gradient(135deg, rgba(192, 132, 87, 0.1) 0%, rgba(192, 132, 87, 0.25) 100%); }
.grid-content.HUG:not(.is-expired) { background: linear-gradient(135deg, rgba(76, 81, 191, 0.1) 0%, rgba(76, 81, 191, 0.25) 100%); }
.grid-content.is-expired { background: #f5f5f5 !important; filter: grayscale(1); opacity: 0.7; }
.dark-mode .grid-content.is-expired { background: #262626 !important; opacity: 0.5; }
.card-center { flex: 1; display: flex; align-items: center; justify-content: center; }
.d-day-label { font-size: 2.2rem; font-weight: 900; color: var(--text-primary); text-align: center; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.d-day-label.no-date { font-size: 1.1rem; font-weight: 800; color: var(--text-secondary); line-height: 1.2; }
.no-date-sub { font-size: 0.7rem; font-weight: 400; opacity: 0.8; }
.d-day-label.red-text { color: #ed4956 !important; font-size: 1.8rem; }
.agency-badge { font-size: 0.7rem; font-weight: 900; color: var(--text-secondary); background: var(--card-bg); padding: 2px 8px; border-radius: 4px; border: 1px solid var(--border-color); }
.region-tag { font-size: 0.75rem; font-weight: 700; color: var(--link-color); }
.notice-short-title { font-size: 0.85rem; font-weight: 600; color: var(--text-primary); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.3; }
.is-expired .region-tag, .is-expired .notice-short-title { color: var(--text-secondary) !important; }
.overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.7); opacity: 0; transition: opacity 0.1s; display: flex; align-items: center; justify-content: center; padding: 20px; z-index: 5; pointer-events: none; }
.grid-item:hover .overlay, .overlay.force-show { opacity: 1; }
.overlay-info { color: white; text-align: center; width: 100%; }
.overlay-memo { 
  font-size: 0.85rem; 
  line-height: 1.5; 
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.reference-section { margin-bottom: 25px; }
.section-label { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); margin-bottom: 10px; display: block; }
.saved-urls-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.url-badge-item { display: flex; align-items: center; justify-content: space-between; background: var(--hover-bg); padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); }
.url-link-text { color: var(--link-color); font-size: 0.85rem; text-decoration: none; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.url-link-text:hover { text-decoration: underline; }
.url-delete-btn { background: none; border: none; color: #ed4956; cursor: pointer; font-size: 1.2rem; padding: 0 5px; }

.url-input-area { margin-top: 10px; }
.input-with-btn { display: flex; gap: 8px; align-items: center; }
.url-input { flex: 1; border: 1px solid var(--border-color); border-radius: 4px; padding: 8px 12px; font-size: 0.9rem; background: var(--card-bg); color: var(--text-primary); outline: none; }
.add-url-btn { background: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 4px; width: 34px; height: 34px; cursor: pointer; color: var(--link-color); font-weight: bold; font-size: 1.2rem; }

.modal-memo { width: 100%; height: 100px; border: 1px solid var(--border-color); border-radius: 4px; padding: 10px; font-family: inherit; resize: none; outline: none; background: var(--card-bg); color: var(--text-primary); }
.main-apply-btn { display: block; width: 100%; padding: 12px; background-color: var(--link-color); color: white; text-align: center; border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 0.95rem; margin-top: 20px; transition: opacity 0.2s; }
.disabled-btn { background-color: #dbdbdb !important; color: #8e8e8e !important; cursor: not-allowed; pointer-events: none; }

@media (max-width: 768px) {
  .grid-container { grid-template-columns: repeat(2, 1fr); gap: 10px; padding: 0 10px; }
  .grid-heart-btn { width: 28px; height: 28px; font-size: 0.9rem; top: 5px; right: 5px; }
}

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: var(--modal-overlay); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.detail-modal { background: var(--card-bg); width: 95%; max-width: 550px; border-radius: 12px; overflow: hidden; max-height: 90vh; border: 1px solid var(--border-color); }
.modal-header { padding: 15px 20px; border-bottom: 1px solid var(--divider-color); display: flex; justify-content: space-between; align-items: center; }
.modal-source-tag { font-size: 0.8rem; font-weight: 800; padding: 4px 10px; border-radius: 20px; color: white; }
.modal-source-tag.LH { background-color: #38a169; }
.modal-source-tag.SH { background-color: #3182ce; }
.modal-source-tag.PRIVATE, .modal-source-tag.민간 { background-color: #c08457; }
.modal-source-tag.HUG { background-color: #4c51bf; }
.modal-body { padding: 30px; color: var(--text-primary); }
.detail-title { font-size: 1.4rem; font-weight: 800; margin-bottom: 10px; }
.detail-grid { background: var(--hover-bg); padding: 15px; border-radius: 8px; margin-bottom: 20px; }
.info-label { font-size: 0.75rem; color: var(--text-secondary); font-weight: 600; display: block; margin-bottom: 4px; }
.info-value { font-size: 0.95rem; font-weight: 700; }
.header-actions { display: flex; gap: 12px; }
.modal-close-icon { background: none; border: none; font-size: 1.8rem; color: var(--text-secondary); cursor: pointer; line-height: 1; padding: 0; transition: color 0.2s; }
.modal-close-icon:hover { color: var(--text-primary); }
.text-muted { color: var(--text-secondary); font-weight: 500; font-size: 0.9rem; }
</style>
