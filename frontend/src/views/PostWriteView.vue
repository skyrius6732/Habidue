<template>
  <div class="board-view-container post-write-page">
    <PageHeader 
      :icon="editMode ? '✏️' : '📝'" 
      :title="editMode ? '소통글 수정' : '소통글 작성'" 
      stats-text="" 
      :stats-value="boardTypeLabel"
      :bio="noticeInfo && !editMode ? noticeInfo.title : '이웃과 함께 나누는 따뜻한 정보 공간입니다.'"
    />

    <div class="board-layout">
      <!-- 사이드바 (맥락 보존) -->
      <CommunitySidebar 
        :activeMenu="post.type"
        @update:activeMenu="val => { if (!editMode) post.type = val }"
        :activeSubCategory="post.subCategory || 'ALL'"
        @update:activeSubCategory="val => { if (!editMode) post.subCategory = val }"
        :disabled="editMode"
      />

      <main class="write-form-main">
        <header class="write-action-bar">
          <div class="header-left">
            <span class="location-guide">
              <span class="dest-badge">{{ boardTypeLabel.split(' (')[0] }}</span> 
              <span class="arrow-sep">></span> 
              <span v-if="post.subCategory" class="dest-badge sub">
                {{ getSubLabel(post.subCategory) }}
              </span>
            </span>
          </div>
          <div class="header-right-actions">
            <button class="btn-list-back slim-badge" @click="handleListBack">이전</button>
            <button class="btn-submit slim-badge" :disabled="!isFormValid || loading" @click="submitPost">
              {{ loading ? '...' : (editMode ? '수정' : '등록') }}
            </button>
          </div>
        </header>

        <div class="write-form-card">
          <!-- 작성 팁 가이드 -->
          <div class="internal-guide-banner">
            <div class="g-header">
              <span class="g-icon">💡</span>
              <h4 class="g-title">작성 전 확인해 주세요</h4>
            </div>
            <ul class="g-list-horizontal">
              <li>정확한 정보를 위해 <b>공고문</b>을 참고해 주세요.</li>
              <li>사진을 첨부하면 <b>이웃들의 관심</b>을 더 많이 받을 수 있어요.</li>
              <li>서로 배려하는 <b>고운 언어</b>를 사용해 주세요.</li>
            </ul>
          </div>

          <!-- 1. 주제 선택 영역 -->
          <div class="category-section">
            <div class="section-label">
              소통글 주제 선택 
              <span v-if="editMode" class="lock-text">(수정 중 변경 불가 🔒)</span>
            </div>

            <div class="category-chips" :class="[noticeInfo?.source, { 'edit-locked-mode': editMode }]">
              <button 
                v-for="cat in currentCategories" 
                :key="cat.value" 
                class="chip" 
                :class="{ 
                  active: post.category === cat.value,
                  dimmed: editMode && post.category !== cat.value 
                }"
                :disabled="editMode"
                @click="selectCategory(cat)"
              >
                {{ cat.label }}
              </button>
            </div>
          </div>

          <!-- 2. 스마트 가이드 카드 -->
          <div class="smart-guide-card" :class="post.category" v-if="currentGuidance">
            <div class="guide-header">
              <span class="guide-icon">✨</span>
              <h4 class="guide-title">{{ currentGuidance.title }} 작성 가이드</h4>
              <button v-if="currentGuidance.template && !editMode" class="btn-apply-template" @click="applyTemplate">
                📄 양식 적용
              </button>
            </div>
            <p class="guide-desc">{{ currentGuidance.desc }}</p>
          </div>

          <!-- 3. 제목 및 본문 입력 -->
          <div class="input-group">
            <input 
              v-model="post.title" 
              type="text" 
              placeholder="제목을 입력하세요" 
              class="input-title"
              maxlength="100"
            />
          </div>

          <div class="editor-container">
            <textarea 
              v-model="post.content" 
              placeholder="함께 정보를 나누고 따뜻한 응원의 메시지를 남겨주세요." 
              class="comment-textarea"
            ></textarea>
          </div>

          <!-- 4. 사진 첨부 -->
          <div class="image-upload-section">
            <div class="section-label">사진 첨부 (최대 5장)</div>
            <div class="image-grid">
              <div v-for="(img, idx) in imagePreviews" :key="idx" class="image-preview-item">
                <img :src="img" alt="preview" />
                <button class="btn-remove-img" @click="removeImage(idx)">✕</button>
              </div>
              <label v-if="imagePreviews.length < 5" class="image-upload-btn">
                <input type="file" @change="handleImageUpload" multiple accept="image/*" hidden />
                <span class="upload-icon">+</span>
              </label>
            </div>
          </div>

          <!-- 5. 연관 키워드 태그 선택 (Pro Version) -->
          <div class="tag-selection-section">
            <div class="section-label">연관 키워드 태그 <small>(최대 3개 선택 가능)</small></div>
            <div v-if="userTags && userTags.length > 0" class="tag-chips-wrapper">
              <button 
                v-for="tag in userTags" 
                :key="tag.tagId" 
                class="pro-tag-chip"
                :class="{ active: selectedTagIds.includes(tag.tagId) }"
                @click="toggleTag(tag.tagId)"
              >
                <span class="hash">#</span>{{ tag.name }}
              </button>
            </div>
            <div v-else class="no-tags-msg-pro">
              <span class="info-icon">ℹ️</span>
              등록된 관심 키워드가 없습니다. 마이페이지에서 키워드를 설정해 보세요!
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/plugins/axios'
import PageHeader from '@/components/PageHeader.vue'
import CommunitySidebar from '@/components/CommunitySidebar.vue'

const route = useRoute()
const router = useRouter()

const editMode = ref(false)
const loading = ref(false)
const userTags = ref([])
const noticeInfo = ref(null)
const images = ref([])
const imagePreviews = ref([])
const selectedTagIds = ref([]) 

const guidanceMap = {
  // [공고 소통방 & 통합광장 지식인 주제]
  INQUIRY: { 
    title: '📢 공고/기관 문의', 
    desc: '공고문의 특정 항목이나 기관 답변에 대해 궁금한 점을 질문해 보세요.', 
    template: '■ 문의 대상(기관/단지): \n■ 현재 본인의 상황(순위/가점 등): \n■ 구체적인 궁금증: \n\n※ 개인정보가 포함되지 않도록 유의해 주세요!' 
  },
  PREPARE: { 
    title: '📝 서류 준비 팁', 
    desc: '해당 공고에 필요한 서류나 발급 시 주의사항을 공유해 주세요.', 
    template: '■ 필수 서류 리스트: \n■ 서류 발급 기관/방법: \n■ 준비 시 가장 헷갈렸던 점: \n■ 예비 신청자를 위한 한 줄 팁: ' 
  },
  PHOTO: { 
    title: '🏠 현장 사진 공유', 
    desc: '단지 외관이나 주변 환경 사진을 공유해 주세요. (사진 보너스 +10 EXP)', 
    template: '■ 단지명 및 동 위치: \n■ 촬영 시점: \n■ 현장에서 느낀 분위기(조경/편의시설 등): \n■ 주변 입지 특징(역과의 거리/상권 등): ' 
  },
  COMPETE: { 
    title: '📊 경쟁률 논의', 
    desc: '예상 경쟁률이나 인기 타입을 분석해 보세요.', 
    template: '■ 희망 타입: \n■ 내가 생각하는 예상 경쟁률: \n■ 그렇게 생각하는 이유(입지/공급세대수 등): \n■ 지원자들끼리 나누고 싶은 의견: ' 
  },
  INST_TIPS: { 
    title: '💡 기관별 노하우', 
    desc: 'LH, SH 등 기관별 서류 준비나 심사 대응 노하우를 공유해 주세요.', 
    template: '■ 대상 기관: \n■ 핵심 노하우 주제: \n■ 상세 내용 및 절차: \n■ 주의해야 할 변수(심사 탈락 사유 등): ' 
  },
  STATUS: { 
    title: '📊 서류/심사 현황', 
    desc: '서류 제출 여부나 심사 진행 상태를 공유해 보세요.', 
    template: '■ 신청 공고명: \n■ 현재 진행 단계(접수/심사/발표대기): \n■ 접수 시 특이사항: \n■ 다른 분들의 진행 현황 확인: ' 
  },

  // [당첨후기 & 파트너스 리뷰왕 주제]
  SUCCESS_STORY: { 
    title: '🏆 내 집 마련 성공기', 
    desc: '첫 신청부터 당첨까지의 생생한 여정을 들려주세요.', 
    template: '■ 당첨 단지/타입: \n■ 나의 청약 히스토리(몇 번째 도전인가요?): \n■ 당첨을 위해 가장 공들였던 부분: \n■ 당첨 확인 순간의 기분과 소감: \n■ 이웃들에게 전하는 응원의 한마디: ' 
  },
  SECRET: { 
    title: '🤫 나만의 당첨 비결', 
    desc: '당첨 확률을 높인 나만의 전략을 공유해 주세요.', 
    template: '■ 지원 전략(타입 선택 기준 등): \n■ 가점/점수 관리 비법: \n■ 서류 준비 시 꼼꼼했던 나만의 체크리스트: \n■ 예비 번호에서 당첨까지의 팁(해당 시): ' 
  },
  MOVE_IN: { 
    title: '🏠 사전점검 후기', 
    desc: '단지 점검 시 발견한 하자나 체크리스트를 공유해 주세요.', 
    template: '■ 단지명/호수: \n■ 점검 시 반드시 챙겨야 할 준비물: \n■ 주요 발견 하자(벽지/타일/수전 등): \n■ 시공 상태 만족도(5점 만점): \n■ 점검 대행 업체 이용 여부 및 추천: ' 
  },
  PARTNER_REVIEW: { 
    title: '✨ 입주/업체 이용후기', 
    desc: '이사, 청소, 인테리어 등 실제 이용한 서비스 만족도를 리뷰해 주세요.', 
    template: '■ 이용 서비스(이사/청소/인테리어 등): \n■ 업체명(초성 권장): \n■ 견적 비용 및 시공 기간: \n■ 만족도(별점): ★★★★★ \n■ 장점: \n■ 단점 및 아쉬운 점: ' 
  },
  ESTIMATE: { 
    title: '📝 견적 공유', 
    desc: '받은 견적이 적정한지 이웃들에게 정보를 공유해 보세요.', 
    template: '■ 시공 종류: \n■ 견적 금액: \n■ 포함된 상세 서비스 범위: \n■ 상담 과정에서 느낀 업체의 전문성: \n■ 이 가격이면 추천하시나요?: ' 
  },
  CHECKLIST: { 
    title: '✅ 시공 체크리스트', 
    desc: '시공 전후 반드시 챙겨야 할 주의사항을 공유해 주세요.', 
    template: '■ 시공 전 확인사항: \n■ 작업 진행 중 반드시 지켜봐야 할 포인트: \n■ 시공 완료 후 최종 검수 리스트: \n■ 하자 발생 시 대처 방법: ' 
  },
  DOCUMENT: { 
    title: '📝 서류 준비 팁', 
    desc: '까다로운 서류 준비 과정을 상세히 알려주세요.', 
    template: '■ 대상 공고/기관: \n■ 준비한 서류 리스트: \n■ 발급 시 가장 어려웠던 서류와 해결법: \n■ 서류 제출 시 주의사항(유효기간 등): \n■ 신청자들을 위한 한 줄 조언: ' 
  },
  LOAN: { 
    title: '💰 자금 계획 노하우', 
    desc: '대출 상담 후기나 이자 지원 사업 활용법을 공유해 주세요.', 
    template: '■ 이용한 대출 상품명: \n■ 대출 금리 및 한도 정보: \n■ 상담 과정 및 필요 서류: \n■ 자금 마련 시 가장 중요하게 생각한 점: \n■ 이자 부담을 줄이는 나만의 팁: ' 
  },
  TIPS: { 
    title: '💡 일반 청약 팁', 
    desc: '청약 전반에 걸친 유용한 팁을 자유롭게 공유해 주세요.', 
    template: '■ 청약 주제: \n■ 핵심 내용 요약: \n■ 상세 설명 및 절차: \n■ 참고하면 좋은 사이트/앱: \n■ 이 팁이 필요한 분들께: ' 
  },

  // [열정파 주제]
  LOCAL_NEWS: { title: '📢 동네 소식', desc: '우리 동네의 최신 소식이나 공사 현황 등을 가볍게 공유해 주세요.', template: '■ 소식 위치: \n■ 내용: \n■ 동네 이웃들의 반응: ' },
  PLACE: { title: '📍 장소 추천', desc: '맛집, 산책로 등 이웃들에게 추천하고 싶은 장소를 공유해 주세요.', template: '■ 장소 이름: \n■ 추천 이유: \n■ 방문 꿀팁(시간대/메뉴 등): ' },
  MEETUP: { title: '🤝 동네 번개', desc: '함께 운동하거나 취미를 나눌 이웃을 찾아보세요.', template: '■ 모임 목적: \n■ 일시 및 장소: \n■ 참여 방법: ' },
  FREE: { title: '☕ 자유 수다', desc: '일상적인 이야기나 가벼운 인사를 나누는 따뜻한 소통 공간입니다.', template: '' }
}

const post = ref({ title: '', content: '', category: '', subCategory: null, type: 'GENERAL', noticeId: null, regionTag: '' })

const getCategoriesBySub = (type, sub) => {
  if (!sub) return []
  const subIcons = { MOVING: '🚚', CLEANING: '🧼', INTERIOR: '🛋️' }
  if (type === 'NOTICE') return [{ label: '📢 공고문의', value: 'INQUIRY', sub: 'ALL' }, { label: '📝 서류준비', value: 'PREPARE', sub: 'ALL' }, { label: '🏠 현장사진', value: 'PHOTO', sub: 'ALL' }, { label: '📊 경쟁률', value: 'COMPETE', sub: 'ALL' }]
  if (sub === 'ALL') return [{ label: '☕ 자유수다', value: 'FREE', sub: 'ALL' }]
  if (type === 'GENERAL') {
    const instSubs = ['LH', 'SH', 'PRIVATE_RENTAL', 'INSTITUTE']; if (instSubs.includes(sub)) return [{ label: '📊 서류현황', value: 'STATUS', sub }, { label: '💡 기관별팁', value: 'INST_TIPS', sub }, { label: '📢 기관문의', value: 'INQUIRY', sub }]
    return [{ label: '📢 동네소식', value: 'LOCAL_NEWS', sub }, { label: '📍 장소추천', value: 'PLACE', sub }, { label: '🤝 동네번개', value: 'MEETUP', sub }]
  }
  if (type === 'REVIEW') {
    if (sub === 'INTERVIEW') return [{ label: '🏆 내집마련기', value: 'SUCCESS_STORY', sub }, { label: '🤫 당첨비결', value: 'SECRET', sub }]
    if (sub === 'TIPS') return [{ label: '📝 서류팁', value: 'DOCUMENT', sub }, { label: '💰 자금계획', value: 'LOAN', sub }, { label: '💡 일반팁', value: 'TIPS', sub }]
    if (sub === 'MOVE_IN') return [{ label: '🏠 사전점검', value: 'MOVE_IN', sub }, { label: '🚚 입주후기', value: 'PARTNER_REVIEW', sub }]
  }
  if (type === 'PARTNER') {
    const icon = subIcons[sub] || ''; return [{ label: `${icon} 이용후기`, value: 'PARTNER_REVIEW', sub }, { label: `${icon} 견적공유`, value: 'ESTIMATE', sub }, { label: `${icon} 체크리스트`, value: 'CHECKLIST', sub }]
  }
  return []
}

const getSubLabel = (sub) => {
  if (!sub) return '';
  if (sub === 'ALL') return '전체';
  const subLabels = { 
    SEOUL: '서울', METRO: '경기/인천', CHUNGCHEONG_GANGWON: '충청/강원/세종', 
    GYEONGSANG: '경상/부산/대구', JEOLLA_JEJU: '전라/광주/제주', 
    LH: 'LH 소통', SH: 'SH 소통', PRIVATE_RENTAL: '민간임대 소통', INSTITUTE: '기관소통', 
    INTERVIEW: '당첨자 인터뷰', TIPS: '서류/청약 팁', MOVE_IN: '입주/사전점검', 
    MOVING: '이사', CLEANING: '청소', INTERIOR: '인테리어',
    MY: '관심'
  }
  return subLabels[sub] || sub
}

const currentCategories = computed(() => getCategoriesBySub(post.value.type, post.value.subCategory || route.query.sub))

const boardTypeLabel = computed(() => {
  const typeLabels = { GENERAL: '통합광장', NOTICE: '공고소통방', REVIEW: '당첨후기', PARTNER: '파트너스' }
  return typeLabels[post.value.type] || '소통글'
})
const currentGuidance = computed(() => guidanceMap[post.value.category] || (post.value.category === 'FREE' ? guidanceMap.FREE : null))

onMounted(async () => {
  window.scrollTo(0, 0)
  const paramId = route.params.postId
  if (paramId && /^\d+$/.test(paramId)) {
    editMode.value = true
    await fetchPostForEdit(paramId)
  } else {
    post.value.type = route.query.type || 'GENERAL'
    post.value.subCategory = route.query.sub || 'ALL'
    post.value.noticeId = route.query.noticeId || null
    const categories = getCategoriesBySub(post.value.type, post.value.subCategory)
    if (categories.length > 0) post.value.category = categories[0].value
    updateRegionTag(post.value.subCategory)
  }
  await Promise.all([fetchUserTags(), fetchNoticeInfo()])
})

const updateRegionTag = (sub) => {
  if (post.value.type === 'GENERAL' && sub && sub !== 'ALL') {
    const subCategoryMap = { SEOUL: '#서울', METRO: '#경기 #인천', CHUNGCHEONG_GANGWON: '#충청 #강원 #세종', GYEONGSANG: '#경상 #부산 #대구', JEOLLA_JEJU: '#전라 #광주 #제주', LH: '#LH', SH: '#SH', PRIVATE_RENTAL: '#민간임대' }
    post.value.regionTag = subCategoryMap[sub] || ''
  }
}

const fetchPostForEdit = async (id) => {
  try {
    const res = await axios.get(`/api/posts/${id}`)
    const data = res.data.data
    post.value.type = data.type
    post.value.subCategory = data.subCategory
    post.value.category = data.category
    post.value.title = data.title
    post.value.content = data.content
    post.value.noticeId = data.noticeId
    post.value.regionTag = data.regionTag
    
    if (data.imageUrls) imagePreviews.value = [...data.imageUrls]
    // [핵심 해결] 상세 조회 응답의 태그에서 id(진짜 태그 ID)를 추출
    if (data.tags) {
      selectedTagIds.value = data.tags.map(t => t.tagId || t.id)
    }
  } catch (e) { alert('데이터 로드 실패') }
}

const handleSubChange = (sub) => { if (!editMode.value) { post.value.subCategory = sub; updateRegionTag(sub); const categories = getCategoriesBySub(post.value.type, sub); if (categories.length > 0) post.value.category = categories[0].value } }
const selectCategory = (cat) => { if (!editMode.value) { post.value.category = cat.value; if (cat.sub) { post.value.subCategory = cat.sub; updateRegionTag(cat.sub) } } }
const applyTemplate = () => { if (!editMode.value && currentGuidance.value?.template) { if (post.value.content.trim() && !confirm('기존 내용이 지워지고 양식이 적용됩니다. 계속할까요?')) return; post.value.content = currentGuidance.value.template } }

// [핵심 해결] tagId를 사용하여 토글 (진짜 태그 테이블 ID)
const toggleTag = (tagId) => {
  const idx = selectedTagIds.value.indexOf(tagId)
  if (idx > -1) {
    selectedTagIds.value.splice(idx, 1)
  } else {
    if (selectedTagIds.value.length >= 3) {
      alert('태그는 최대 3개까지 선택 가능합니다.')
      return
    }
    selectedTagIds.value.push(tagId)
  }
}

const submitPost = async () => {
  if (!isFormValid.value) return
  loading.value = true
  try {
    let imageUrls = [...imagePreviews.value.filter(url => url.startsWith('http'))]
    if (images.value.length > 0) {
      const formData = new FormData()
      images.value.forEach(file => { formData.append('files', file) })
      const uploadRes = await axios.post('/api/images/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      imageUrls = [...imageUrls, ...uploadRes.data.data]
    }
    
    const payload = { 
      title: post.value.title, 
      content: post.value.content, 
      category: post.value.category,
      subCategory: post.value.subCategory || 'ALL', 
      type: post.value.type,
      noticeId: post.value.noticeId, 
      imageUrls: imageUrls, 
      tagIds: [...selectedTagIds.value], // 진짜 태그 ID 리스트 전송
      regionTag: post.value.regionTag
    }

    if (editMode.value && route.params.postId) {
      await axios.put(`/api/posts/${route.params.postId}`, payload)
      alert('수정되었습니다.')
    } else {
      await axios.post('/api/posts', payload)
      alert('등록되었습니다.')
    }
    handleListBack()
  } catch (e) { 
    const errorMsg = e.response?.data?.message || '저장에 실패했습니다.'
    alert(errorMsg) 
  } finally { loading.value = false }
}

const handleListBack = () => router.back()
const handleImageUpload = (e) => {
  const files = Array.from(e.target.files); if (imagePreviews.value.length + files.length > 5) { alert('사진은 최대 5장까지 가능합니다.'); return }
  files.forEach(file => { images.value.push(file); const reader = new FileReader(); reader.onload = (ev) => { imagePreviews.value.push(ev.target.result) }; reader.readAsDataURL(file) })
}
const removeImage = (idx) => { 
  const preview = imagePreviews.value[idx]; if (typeof preview === 'string' && preview.startsWith('http')) { imagePreviews.value.splice(idx, 1) } 
  else { const localIdx = imagePreviews.value.filter((p, i) => i < idx && !(typeof p === 'string' && p.startsWith('http'))).length; images.value.splice(localIdx, 1); imagePreviews.value.splice(idx, 1) }
}
const fetchUserTags = async () => { try { const res = await axios.get('/api/user-tags'); userTags.value = res.data.data } catch (e) {} }
const fetchNoticeInfo = async () => { if (post.value.noticeId) { try { const res = await axios.get(`/api/notices/${post.value.noticeId}`); noticeInfo.value = res.data.data } catch (e) {} } }
const isFormValid = computed(() => post.value.title?.trim() && post.value.content?.trim() && post.value.category)
const isMobile = ref(window.innerWidth <= 992)
const handleResize = () => { isMobile.value = window.innerWidth <= 992 }
onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => window.removeEventListener('resize', handleResize))
</script>

<style scoped>
.post-write-page { max-width: 1200px; margin: 0 auto; padding-bottom: 80px; }
.board-layout { display: flex; gap: 30px; padding: 0 20px; margin-top: 24px; }
.write-form-main { flex: 1; max-width: 850px; min-width: 0; }

.write-action-bar { display: flex; justify-content: space-between; align-items: center; padding: 15px 25px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; margin-bottom: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
.location-guide { font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); display: flex; align-items: center; gap: 4px; }
.dest-badge { background: var(--hover-bg); padding: 3px 10px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); font-size: 0.75rem; white-space: nowrap; }
.dest-badge.sub { background: rgba(0, 149, 246, 0.05); border-color: var(--link-color); color: var(--link-color); min-width: 40px; text-align: center; }
.arrow-sep { opacity: 0.4; font-size: 0.7rem; }
.header-right-actions { display: flex; gap: 8px; align-items: center; margin-left: auto; }
.btn-list-back, .btn-submit { padding: 6px 16px; border-radius: 6px; font-size: 0.85rem; font-weight: 700; cursor: pointer; transition: all 0.2s; height: 34px; display: flex; align-items: center; justify-content: center; }
.btn-list-back { background: none; border: 1px solid var(--border-color); color: var(--text-secondary); }
.btn-submit { background: var(--link-color); color: white; border: none; font-weight: 800; box-shadow: 0 4px 12px rgba(0, 149, 246, 0.15); }
.btn-submit:disabled { opacity: 0.4; cursor: not-allowed; }

.write-form-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 24px; padding: 40px; display: flex; flex-direction: column; gap: 30px; box-shadow: 0 4px 30px rgba(0,0,0,0.03); }

.internal-guide-banner { background: var(--hover-bg); border-radius: 16px; padding: 20px 25px; border: 1px solid var(--border-color); margin-bottom: 5px; }
.internal-guide-banner .g-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.internal-guide-banner .g-title { font-size: 0.9rem; font-weight: 800; margin: 0; color: var(--text-primary); }
.g-list-horizontal { list-style: none; padding: 0; margin: 0; display: flex; gap: 20px; flex-wrap: wrap; }
.g-list-horizontal li { font-size: 0.82rem; color: var(--text-secondary); position: relative; padding-left: 12px; }
.g-list-horizontal li::before { content: "•"; position: absolute; left: 0; color: var(--link-color); font-weight: bold; }

.category-section { display: flex; flex-direction: column; gap: 15px; }
.section-label { font-size: 0.8rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; display: flex; align-items: center; }
.lock-text { color: #ed4956; font-size: 0.75rem; text-transform: none; margin-left: 4px; font-weight: 800; }

.sub-board-selector { display: flex; gap: 8px; flex-wrap: wrap; border-bottom: 1px solid var(--divider-color); padding-bottom: 8px; }
.sub-tab { white-space: nowrap; padding: 6px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--hover-bg); color: var(--text-secondary); font-size: 0.8rem; font-weight: 700; cursor: pointer; }
.sub-tab.active { background: var(--text-primary); color: var(--card-bg); border-color: var(--text-primary); }

.category-chips { display: flex; gap: 10px; flex-wrap: wrap; padding-bottom: 5px; }
.chip { white-space: nowrap; padding: 10px 22px; border-radius: 25px; background: var(--hover-bg); border: 1px solid var(--border-color); color: var(--text-secondary); font-size: 0.85rem; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.chip.active { background: var(--link-color); color: white; border-color: var(--link-color); box-shadow: 0 4px 12px rgba(0, 149, 246, 0.2); }

.edit-locked-mode .chip { cursor: not-allowed; }
.edit-locked-mode .chip.dimmed { opacity: 0.45; filter: grayscale(0.5); background: var(--hover-bg); border-color: var(--border-color); }
.edit-locked-mode .chip.active { opacity: 0.9; box-shadow: none; }

.smart-guide-card { background: rgba(0, 149, 246, 0.03); border-radius: 16px; padding: 20px; border-left: 4px solid var(--link-color); }
.smart-guide-card .guide-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.smart-guide-card .guide-title { font-size: 0.9rem; font-weight: 800; margin: 0; }
.btn-apply-template { margin-left: auto; background: var(--card-bg); border: 1px solid var(--border-color); padding: 6px 14px; border-radius: 8px; font-size: 0.75rem; font-weight: 800; color: var(--link-color); cursor: pointer; }
.guide-desc { font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; margin: 0; }

.input-title { width: 100%; border: none; border-bottom: 2px solid var(--divider-color); padding: 15px 0; font-size: 1.6rem; font-weight: 800; background: none; outline: none; color: var(--text-primary); transition: border-color 0.2s; }
.input-title::placeholder { color: var(--text-secondary); opacity: 0.6; }
.input-title:focus { border-color: var(--link-color); }
.comment-textarea { width: 100%; min-height: 350px; border: none; resize: none; font-size: 1.1rem; line-height: 1.8; background: none; outline: none; color: var(--text-primary); }
.comment-textarea::placeholder { color: var(--text-secondary); opacity: 0.6; }

.image-upload-section { margin-top: 10px; padding-top: 25px; border-top: 1px solid var(--divider-color); }
.image-grid { display: flex; gap: 15px; flex-wrap: wrap; margin-top: 15px; }
.image-preview-item { width: 110px; height: 110px; border-radius: 14px; overflow: hidden; position: relative; border: 1px solid var(--border-color); }
.image-preview-item img { width: 100%; height: 100%; object-fit: cover; }
.btn-remove-img { position: absolute; top: 6px; right: 6px; background: rgba(0,0,0,0.6); color: white; border: none; border-radius: 50%; width: 22px; height: 22px; font-size: 11px; cursor: pointer; }
.image-upload-btn { width: 110px; height: 110px; border-radius: 14px; border: 2px dashed var(--border-color); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; color: var(--text-secondary); }
.image-upload-btn:hover { border-color: var(--link-color); background: var(--hover-bg); color: var(--link-color); }
.upload-icon { font-size: 2.5rem; }

.tag-selection-section { margin-top: 10px; padding-top: 25px; border-top: 1px solid var(--divider-color); }
.tag-chips-wrapper { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 15px; }
.pro-tag-chip { 
  padding: 8px 20px; border-radius: 25px; background: var(--hover-bg); border: 1px solid var(--border-color); 
  color: var(--text-primary); font-size: 0.85rem; font-weight: 700; cursor: pointer; 
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); display: flex; align-items: center;
}
.pro-tag-chip .hash { color: var(--link-color); margin-right: 4px; font-weight: 900; }
.pro-tag-chip.active { 
  background: var(--link-color); color: white; border-color: var(--link-color); 
  box-shadow: 0 6px 15px rgba(0, 149, 246, 0.3); transform: translateY(-2px); 
}
.pro-tag-chip.active .hash { color: white; opacity: 0.8; }
.pro-tag-chip:hover:not(.active) { border-color: var(--link-color); background: var(--hover-bg); transform: translateY(-1px); }
.no-tags-msg-pro { 
  font-size: 0.85rem; color: var(--text-secondary); margin-top: 15px; padding: 16px; 
  background: var(--hover-bg); border-radius: 12px; text-align: center; width: 100%; border: 1px dashed var(--border-color);
  display: flex; align-items: center; justify-content: center; gap: 10px;
}

@media (max-width: 992px) { 
  .board-layout { flex-direction: column; gap: 0; padding: 0; }
  .write-form-main { width: 100%; max-width: none; flex: none; }
  .write-form-card { padding: 25px 15px; border-radius: 0; border-left: none; border-right: none; }
  .slim-badge { padding: 3px 12px !important; border-radius: 6px !important; font-size: 0.75rem !important; height: 26px !important; font-weight: 800 !important; }
  .input-title { font-size: 1.3rem; }
  .write-action-bar { padding: 10px 15px; border-radius: 0; margin-bottom: 10px; border-left: none; border-right: none; }
  .location-guide { font-size: 0.75rem; }
}

@media (max-width: 768px) { .g-list-horizontal { flex-direction: column; gap: 8px; } }
</style>
