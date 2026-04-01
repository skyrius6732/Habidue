<template>
  <div class="calendar-container">
    <div ref="calendarTopAnchor" class="scroll-anchor"></div>
    <PageHeader 
      icon="📅" 
      title="캘린더" 
      stats-text="공고 일정" 
      :stats-value="totalCount"
      bio="기관별, 상태별 일정을 달력에서 자유롭게 필터링하세요."
    />

    <div class="calendar-content-inner">
      <!-- 통합 서클 필터 바 -->
      <div class="unified-filter-bar">
        <div class="filter-scroll" @click.stop>
          <div v-for="f in allFilterOptions" :key="f.value" 
               class="filter-item" :class="{ active: isFilterActive(f) }" 
               @click="toggleUnifiedFilter(f)">
            <div class="filter-circle" :style="{ backgroundColor: f.color, borderColor: isFilterActive(f) ? f.color : 'var(--border-color)' }">
              {{ f.label }}
            </div>
            <span class="filter-label">{{ f.name }}</span>
          </div>
        </div>
      </div>

      <!-- 출석 체크 버튼 -->
      <div class="calendar-attendance-row">
        <button 
          class="attendance-compact-btn" 
          :class="{ 'is-checked': isCheckedToday }"
          @click="checkIn"
        >
          {{ isCheckedToday ? '✅ 출석완료' : '📌 출석체크' }}
        </button>
      </div>

      <div class="calendar-wrapper" ref="calendarWrapper">
        <VCalendar 
          expanded 
          trim-weeks
          locale="ko"
          :masks="{ title: 'YYYY년 MM월' }"
          :attributes="calendarAttributes" 
          class="custom-calendar"
        >
          <template #day-content="{ day, attributes }">
            <div 
              class="day-cell" 
              :class="{ 
                'in-range': isWithinSelectedRange(day.date),
                'range-start': isRangeStart(day.date),
                'range-end': isRangeEnd(day.date),
                'focus-mode': selectedRange !== null,
                'has-notices': attributes.length > 0,
                'is-selected': isDateSelected(day.date)
              }"
              :style="getRangeStyle(day.date)"
              @click="handleDayClick(day)"
            >
              <div class="day-header">
                <span class="day-label" :class="{ 
                  'is-today': day.isToday,
                  'not-current-month': !day.inMonth 
                }">{{ day.day }}</span>
                <span v-if="shouldShowAttendanceMark(day.id)" 
                      class="attendance-circle-mark" 
                      :class="{ 'is-completed': isAttendanceCompleted(day.id) }"
                      :title="isAttendanceCompleted(day.id) ? '출석 완료!' : '출석 전'">
                </span>
              </div>
              
              <!-- 배지 컨테이너 -->
              <div class="badges-container day-scroll">
                <div v-for="attr in attributes" :key="attr.key" class="badge-wrapper">
                  <div 
                    v-if="attr.customData && (!selectedRange || selectedRange.noticeId === attr.customData.noticeId)"
                    class="source-badge" 
                    :class="[
                      attr.customData.sourceClass, 
                      attr.customData.type,
                      attr.customData.statusClass,
                      { 'is-active': selectedRange?.noticeId === attr.customData.noticeId }
                    ]"
                    @click.stop="toggleSelectRange(attr.customData)"
                    @mouseenter="!isMobile && showPreview($event, attr.customData)"
                    @mouseleave="!isMobile && hidePreview()"
                    @touchstart="handleTouchStart(attr.customData)"
                    @touchend="hidePreview()"
                    @contextmenu.prevent
                  >
                    {{ attr.customData.sourceLabel }} {{ attr.customData.type === 'start' ? '등록' : '마감' }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </VCalendar>

        <!-- 퀵 프리뷰 오버레이 -->
        <Transition name="fade">
          <div 
            ref="previewRef"
            v-show="hoveredNotice" 
            class="notice-quick-preview" 
            :style="{ left: previewPosition.x + 'px', top: previewPosition.y + 'px' }"
          >
            <div v-if="hoveredNotice">
              <div class="preview-header">
                <span class="source-tag" :class="hoveredNotice.sourceClass">{{ hoveredNotice.sourceLabel }}</span>
                <span class="status-badge" :class="getStatus(hoveredNotice).class">{{ getStatus(hoveredNotice).text }}</span>
              </div>
              <h4 class="preview-title">{{ hoveredNotice.title }}</h4>
              <div class="preview-footer">
                <span class="region-info">📍 {{ hoveredNotice.region || '지역 정보 없음' }}</span>
                <span class="date-range">{{ formatDateDot(hoveredNotice.start) }} ~ {{ formatDateDot(hoveredNotice.end) }}</span>
              </div>
            </div>
          </div>
        </Transition>
      </div>

      <!-- 하단 상세 리스트 -->
      <div v-if="selectedDate || isMobile" class="selected-notices-section" ref="detailSection">
        <div class="selected-header">
          <h3 class="date-title">{{ selectedDate ? formatDate(selectedDate) : '날짜를 선택하세요' }}</h3>
          <span v-if="selectedDateNotices.length > 0" class="count-badge">{{ selectedDateNotices.length }}개의 공고</span>
        </div>

        <div v-if="selectedDateNotices.length > 0" class="notices-grid">
          <div 
            v-for="notice in selectedDateNotices" 
            :key="notice.id" 
            class="list-item-card"
            :class="{ 'highlight-card': selectedRange?.noticeId === notice.id }"
            @click="handleNoticeClickInList(notice)"
          >
            <div class="card-row-top">
              <span class="source-tag" :class="notice.source === '민간' ? 'PRIVATE' : notice.source">{{ getSourceLabel(notice.source) }}</span>
              <span class="status-badge" :class="getStatus(notice).class">{{ getStatus(notice).text }}</span>
              <span class="region-info">📍 {{ notice.region }}</span>
              <span class="date-info">{{ formatDateDot(notice.announcementDate || notice.createdAt) }} ~ {{ formatDateDot(notice.deadline) }}</span>
            </div>
            <div class="card-row-body">
              <h4 class="notice-title" :title="notice.title">{{ notice.title }}</h4>
            </div>
            <div class="card-row-tags" v-if="notice.tags && notice.tags.length > 0">
              <span v-for="tag in notice.tags.slice(0, 5)" :key="tag.name" class="mini-tag" :class="{ matched: userKeywords.includes(tag.name) }">#{{ tag.name }}</span>
            </div>
            <div class="card-row-action">
              <a :href="notice.link" 
                 target="_blank" 
                 class="apply-link-btn" 
                 :style="{ backgroundColor: colorMap[notice.source === '민간' || notice.source === 'PRIVATE' ? 'PRIVATE' : notice.source] || 'var(--link-color)' }"
                 @click.stop>
                🔗 원문 공고보기
              </a>
            </div>
          </div>
        </div>
        <div v-else class="no-data-msg">
          <p v-if="selectedDate">해당 날짜에 예정된 공고 일정이 없습니다.</p>
          <p v-else>달력에서 날짜를 클릭하여 상세 일정을 확인하세요.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import axios from '@/plugins/axios'
import PageHeader from '@/components/PageHeader.vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
const notices = ref([])
const userKeywords = ref([])
const selectedDate = ref(null)
const selectedDateNotices = ref([])
const detailSection = ref(null)
const calendarTopAnchor = ref(null)
const selectedRange = ref(null)
const isMobile = ref(false)
const loading = ref(false)
const totalCount = ref(0)
const isCheckedToday = ref(false)
const attendanceDates = ref([])
const calendarWrapper = ref(null) // 캘린더 래퍼 참조
const previewRef = ref(null) // 프리뷰 참조
const hoveredNotice = ref(null)
const previewPosition = ref({ x: 0, y: 0 })
let longPressTimer = null
let debounceTimer = null

const getLocalTodayStr = () => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const todayStr = getLocalTodayStr();

const isAttendanceCompleted = (dateId) => attendanceDates.value.includes(dateId);
const shouldShowAttendanceMark = (dateId) => {
  if (isAttendanceCompleted(dateId)) return true;
  return dateId <= todayStr; 
};

const activeSources = ref(['ALL'])
const activeStatuses = ref(['RECRUITING'])
const isInterestOnly = ref(false)

const checkIn = async () => {
  if (isCheckedToday.value) return;
  try {
    await axios.post('/api/attendance');
    isCheckedToday.value = true;
    const today = getLocalTodayStr();
    if (!attendanceDates.value.includes(today)) attendanceDates.value.push(today);
    await uiStore.showAlert('오늘의 출석이 완료되었습니다! ✅', '출석 체크');
  } catch (error) {
    console.error('출석 체크 실패:', error);
    await uiStore.showAlert(error.response?.data?.message || '출석 체크 중 오류가 발생했습니다.', '오류');
  }
}

const fetchAttendance = async (year, month) => {
  try {
    const res = await axios.get('/api/attendance', { params: { year, month } });
    attendanceDates.value = res.data.data;
  } catch (error) { console.error('출석 데이터 로드 실패:', error); }
}

const checkTodayStatus = async () => {
  try {
    const res = await axios.get('/api/attendance/today');
    isCheckedToday.value = res.data.data;
  } catch (error) { console.error('출석 상태 확인 실패:', error); }
}

const showPreview = (event, data) => {
  if (isMobile.value) return; // 모바일에서는 터치 이벤트를 사용하므로 호버 무시

  // 이벤트 발생 즉시 대상의 좌표를 캡처
  const targetElement = event.currentTarget;
  const targetRect = targetElement.getBoundingClientRect();
  
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(async () => {
    hoveredNotice.value = data;
    
    await nextTick();

    if (calendarWrapper.value && previewRef.value) {
      const calendarRect = calendarWrapper.value.getBoundingClientRect();
      const previewRect = previewRef.value.getBoundingClientRect();

      const x = targetRect.left - calendarRect.left + (targetRect.width / 2);
      const pHeight = previewRect.height || 180;
      let y = targetRect.top - calendarRect.top - pHeight - 10;

      const pWidth = previewRect.width || 300;
      const halfWidth = pWidth / 2;
      let finalX = x;
      if (x - halfWidth < 10) {
        finalX = halfWidth + 10;
      } else if (x + halfWidth > calendarRect.width - 10) {
        finalX = calendarRect.width - halfWidth - 10;
      }

      if (y < 10) {
        y = targetRect.bottom - calendarRect.top + 10;
      }

      previewPosition.value = { x: finalX, y };
    }
  }, 50);
}

const hidePreview = () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  hoveredNotice.value = null;
  if (longPressTimer) clearTimeout(longPressTimer);
}

const handleTouchStart = (data) => {
  if (!isMobile.value) return;
  longPressTimer = setTimeout(() => {
    hoveredNotice.value = data;
    
    // 모바일에서는 화면 중앙 상단에 위치하도록 설정
    if (calendarWrapper.value) {
      const calendarRect = calendarWrapper.value.getBoundingClientRect();
      previewPosition.value = {
        x: calendarRect.width / 2, // 가로 중앙
        y: 20 // 상단에서 20px 여백
      };
    }
    
    if (window.navigator.vibrate) window.navigator.vibrate(10);
  }, 500);
}

const allFilterOptions = [
  { name: '전체', value: 'ALL', label: 'ALL', type: 'GLOBAL', color: '#8e8e8e' },
  { name: '관심 공고', value: 'MY_INTEREST', label: '관심', type: 'INTEREST', color: '#f6ad55' },
  { name: 'LH 공사', value: 'LH', label: 'LH', type: 'SOURCE', color: '#38a169' },
  { name: 'SH 공사', value: 'SH', label: 'SH', type: 'SOURCE', color: '#3182ce' },
  { name: '민간임대', value: 'PRIVATE', label: '민간', type: 'SOURCE', color: '#c08457' },
  { name: '접수중', value: 'RECRUITING', label: '접수', type: 'STATUS', color: '#38a169' },
  { name: '마감', value: 'CLOSED', label: '마감', type: 'STATUS', color: '#ed4956' },
  { name: '결과발표', value: 'RESULT', label: '결과', type: 'STATUS', color: '#6b46c1' },
  { name: '안내', value: 'INFO', label: '안내', type: 'STATUS', color: '#3182ce' }
]

const isFilterActive = (f) => {
  if (f.value === 'ALL') return activeSources.value.includes('ALL') && activeStatuses.value.includes('ALL') && !isInterestOnly.value;
  if (f.value === 'MY_INTEREST') return isInterestOnly.value;
  if (f.type === 'SOURCE') return activeSources.value.includes(f.value);
  if (f.type === 'STATUS') return activeStatuses.value.includes(f.value);
  return false;
}

const toggleUnifiedFilter = (f) => {
  if (f.value === 'MY_INTEREST') {
    isInterestOnly.value = !isInterestOnly.value;
  } else if (f.value === 'ALL') {
    activeSources.value = ['ALL'];
    activeStatuses.value = ['ALL'];
    isInterestOnly.value = false;
  } else {
    const targetArr = f.type === 'SOURCE' ? activeSources : activeStatuses;
    const allIdx = targetArr.value.indexOf('ALL');
    if (allIdx !== -1) targetArr.value.splice(allIdx, 1);
    const idx = targetArr.value.indexOf(f.value);
    if (idx === -1) targetArr.value.push(f.value);
    else {
      targetArr.value.splice(idx, 1);
      if (activeSources.value.length === 0 && activeStatuses.value.length === 0) {
        activeSources.value = ['ALL']; activeStatuses.value = ['ALL'];
      } else if (targetArr.value.length === 0) targetArr.value.push('ALL');
    }
  }
  fetchNotices();
}

const fetchNotices = async () => {
  loading.value = true
  try {
    let response;
    if (isInterestOnly.value) {
      // 1. 내 관심 공고 전체 조회
      response = await axios.get('/api/user-notices', { params: { size: 1000 } });
      
      const allInterests = response.data.data.content.map(un => ({
        id: un.noticeId || un.id,
        title: un.noticeTitle,
        source: un.noticeSource,
        announcementDate: un.noticeAnnouncementDate,
        deadline: un.noticeDeadline,
        link: un.noticeLink,
        status: un.noticeStatus, // 백엔드에서 내려주는 실제 상태값 사용
        region: un.region || '', 
        tags: un.noticeTags || [], // 백엔드에서 내려주는 태그 정보 사용
        userNoticeId: un.id,
        memo: un.memo,
        userDeadline: un.userDeadline
      }));

      // 2. 현재 선택된 기관 및 상태 필터를 관심 공고 리스트에 적용 (중복 필터링)
      notices.value = allInterests.filter(n => {
        const sourceMatch = activeSources.value.includes('ALL') || activeSources.value.includes(n.source);
        // getStatus를 통해 판별된 실제 상태값으로 필터링
        const statusMatch = activeStatuses.value.includes('ALL') || activeStatuses.value.includes(getStatus(n).statusValue);
        return sourceMatch && statusMatch;
      });
      
      totalCount.value = notices.value.length; // 필터링된 결과의 개수로 업데이트
    } else {
      // 일반 공고 필터링 조회 (서버 사이드 필터링)
      const sourcesParam = activeSources.value.includes('ALL') ? null : activeSources.value.join(',');
      const statusesParam = activeStatuses.value.includes('ALL') ? null : activeStatuses.value.join(',');
      response = await axios.get('/api/notices', { 
        params: { size: 1000, sources: sourcesParam, statuses: statusesParam },
        timeout: 15000 // 대량 데이터 조회를 위해 타임아웃 연장 (15초)
      });
      notices.value = response.data.data.content;
      totalCount.value = response.data.data.totalElements;
    }
    
    // 선택된 날짜가 있다면 하단 상세 리스트 갱신
    if (selectedDate.value) {
        const dateStr = selectedDate.value.toISOString().split('T')[0];
        selectedDateNotices.value = notices.value.filter(n => {
            const start = (n.announcementDate || n.createdAt)?.split('T')[0];
            const end = n.deadline?.split('T')[0];
            return start === dateStr || end === dateStr;
        });
    }
  } catch (error) { console.error('공고 로드 실패:', error) } finally { loading.value = false }
}

const getSourceLabel = (source) => (source === 'PRIVATE' || source === 'private' || source === '민간') ? '민간' : source;
const colorMap = { 'LH': '#38a169', 'SH': '#3182ce', 'PRIVATE': '#c08457', 'HUG': '#4c51bf' };

const calendarAttributes = computed(() => {
  const attrs = [];
  notices.value.forEach(notice => {
    const sClass = notice.source;
    const statusInfo = getStatus(notice);
    const commonData = { 
      noticeId: notice.id, title: notice.title, region: notice.region,
      source: notice.source, sourceLabel: getSourceLabel(notice.source), sourceClass: sClass,
      status: statusInfo.statusValue, statusClass: statusInfo.class === 'closed' ? 'is-closed' : 'is-open',
      start: notice.announcementDate || notice.createdAt, end: notice.deadline, color: colorMap[sClass] || '#8e8e8e' 
    };
    if (notice.announcementDate || notice.createdAt) {
      attrs.push({ key: `start-${notice.id}`, dates: new Date(notice.announcementDate || notice.createdAt), customData: { ...commonData, type: 'start' } });
    }
    if (notice.deadline) {
      attrs.push({ key: `end-${notice.id}`, dates: new Date(notice.deadline), customData: { ...commonData, type: 'end' } });
    }
  });
  return attrs;
})

const toggleSelectRange = (data) => {
  if (selectedRange.value?.noticeId === data.noticeId) selectedRange.value = null;
  else selectedRange.value = data;
}

const isWithinSelectedRange = (date) => {
  if (!selectedRange.value || !selectedRange.value.start || !selectedRange.value.end) return false;
  const d = new Date(date).setHours(0,0,0,0);
  const s = new Date(selectedRange.value.start).setHours(0,0,0,0);
  const e = new Date(selectedRange.value.end).setHours(0,0,0,0);
  return d >= s && d <= e;
}

const isRangeStart = (date) => selectedRange.value && selectedRange.value.start && new Date(date).setHours(0,0,0,0) === new Date(selectedRange.value.start).setHours(0,0,0,0);
const isRangeEnd = (date) => selectedRange.value && selectedRange.value.end && new Date(date).setHours(0,0,0,0) === new Date(selectedRange.value.end).setHours(0,0,0,0);

const isDateSelected = (date) => {
  if (!selectedDate.value) return false;
  return new Date(date).setHours(0,0,0,0) === new Date(selectedDate.value).setHours(0,0,0,0);
}

const getRangeStyle = (date) => {
  if (!isWithinSelectedRange(date)) return {};
  const color = selectedRange.value.color;
  return {
    backgroundColor: `${color}15`, borderTop: `2px solid ${color}30`, borderBottom: `2px solid ${color}30`,
    borderLeft: isRangeStart(date) ? `4px solid ${color}` : 'none',
    borderRight: isRangeEnd(date) ? `4px solid ${color}` : 'none'
  };
}

const handleDayClick = (day) => {
  selectedRange.value = null; 
  selectedDate.value = day.date;
  const dateStr = day.id; 
  selectedDateNotices.value = notices.value.filter(n => {
    const start = (n.announcementDate || n.createdAt)?.split('T')[0];
    const end = n.deadline?.split('T')[0];
    return start === dateStr || end === dateStr;
  });
  setTimeout(() => { detailSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }, 100);
}

const handleNoticeClickInList = (notice) => {
  const sClass = notice.source;
  selectedRange.value = {
    noticeId: notice.id, sourceLabel: getSourceLabel(notice.source), sourceClass: sClass,
    start: notice.announcementDate || notice.createdAt, end: notice.deadline, color: colorMap[sClass] || '#8e8e8e'
  };
  calendarTopAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

const getStatus = (notice) => {
  const systemTag = (notice.tags || notice.noticeTags)?.find(t => ['접수중', '마감', '결과발표', '발표완료', '안내', '이전안내'].includes(t.name));
  const tagName = systemTag ? systemTag.name : '';

  if (tagName === '발표완료' || tagName === '이전안내' || notice.status === 'CLOSED') {
    return { text: '마감', class: 'closed', statusValue: 'CLOSED' };
  }

  switch (notice.status) {
    case 'RECRUITING': return { text: '접수중', class: 'open', statusValue: 'RECRUITING' };
    case 'RESULT': return { text: '결과발표', class: 'result', statusValue: 'RESULT' };
    case 'INFO': return { text: '안내', class: 'info', statusValue: 'INFO' };
    default: return { text: '안내', class: 'info', statusValue: 'INFO' };
  }
}

const formatDate = (date) => date ? new Date(date).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' }) : '';
const formatDateDot = (s) => s ? s.split('T')[0].replace(/-/g, '.') : '-';
const updateWidth = () => { isMobile.value = window.innerWidth <= 768; }

onMounted(async () => {
  fetchNotices();
  updateWidth();
  window.addEventListener('resize', updateWidth);
  const today = new Date();
  fetchAttendance(today.getFullYear(), today.getMonth() + 1);
  checkTodayStatus();
  try {
    const kwRes = await axios.get('/api/user-tags');
    userKeywords.value = kwRes.data.data.map(k => k.name);
  } catch (e) { console.error('키워드 로드 실패:', e); }
})

onUnmounted(() => { window.removeEventListener('resize', updateWidth); })
</script>

<style scoped>
.calendar-container { width: 100%; min-height: 100vh; position: relative; }
.scroll-anchor { height: 0; position: absolute; top: 0; visibility: hidden; }
.calendar-content-inner { padding: 0 20px; margin-bottom: 50px; }

.unified-filter-bar { margin-top: 1.5rem; margin-bottom: 1.5rem; }
.filter-scroll { display: flex; gap: 1.2rem; overflow-x: auto; padding: 10px 5px; justify-content: center; scrollbar-width: none; }
.filter-item { display: flex; flex-direction: column; align-items: center; gap: 8px; cursor: pointer; color: var(--text-primary); opacity: 0.4; transition: all 0.2s; }
.filter-item.active { opacity: 1; }
.filter-circle { width: 52px; height: 52px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: 800; font-size: 0.65rem; border: 2px solid transparent; box-shadow: 0 0 0 2px var(--border-color); }
.filter-item.active .filter-circle { box-shadow: 0 0 0 2px #0095f6; transform: scale(1.05); }
.filter-label { font-size: 0.7rem; font-weight: 600; color: var(--text-primary); }

.calendar-attendance-row { display: flex; justify-content: flex-end; align-items: center; margin-bottom: 15px; padding: 0 5px; }
.attendance-compact-btn { display: flex; align-items: center; padding: 6px 16px; background-color: transparent; color: #ed4956; border: 1.5px solid #ed4956; border-radius: 20px; font-weight: 800; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.attendance-compact-btn:hover { background-color: rgba(237, 73, 86, 0.05); transform: translateY(-1px); }
.attendance-compact-btn.is-checked { color: #38a169; border-color: #38a169; }

.calendar-wrapper { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; margin-bottom: 2rem; box-shadow: 0 4px 12px rgba(0,0,0,0.05); position: relative; }

/* 퀵 프리뷰 오버레이 스타일 */
.notice-quick-preview {
  position: absolute;
  width: 90%;
  max-width: 300px;
  background: var(--card-bg);
  border: 1px solid var(--link-color);
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  z-index: 1000;
  pointer-events: none;
  transform: translateX(-50%); /* 가로만 중앙 정렬 */
}

.preview-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.preview-title { font-size: 0.9rem; font-weight: 800; margin: 0; color: var(--text-primary); line-height: 1.4; }
.preview-footer { display: flex; justify-content: space-between; align-items: center; font-size: 0.7rem; color: var(--text-secondary); font-weight: 600; margin-top: 10px; }

.fade-enter-active, .fade-leave-active { transition: all 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translate(-50%, 10px); }

:deep(.custom-calendar) { border: none !important; width: 100% !important; background-color: var(--card-bg) !important; }
:deep(.vc-container) { background-color: var(--card-bg) !important; border: none !important; color: var(--text-primary) !important; }
:deep(.vc-header) { padding: 25px 15px !important; }
:deep(.vc-title) { font-size: 1.3rem !important; font-weight: 800 !important; color: var(--text-primary) !important; background: none !important; }
:deep(.vc-weekday) { padding: 15px 0 !important; color: var(--text-secondary) !important; font-weight: 700; }
:deep(.vc-day) { min-height: 140px !important; height: 140px !important; border-right: 1px solid var(--divider-color); border-bottom: 1px solid var(--divider-color); overflow: hidden; }
:deep(.vc-day:nth-child(7n)) { border-right: none; }

.day-cell { height: 100%; width: 100%; padding: 8px; display: flex; flex-direction: column; cursor: pointer; transition: background-color 0.2s; position: relative; }
.day-cell:hover { background-color: var(--hover-bg); }

.day-cell.is-selected { background-color: rgba(0, 149, 246, 0.08); }
.day-cell.is-selected .day-label:not(.is-today) { color: var(--link-color); font-weight: 800; }
.day-cell.is-selected::after { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; border: 2px solid var(--link-color); border-radius: 4px; pointer-events: none; z-index: 5; }

.focus-mode:not(.in-range) { opacity: 0.4; }
.day-header { display: flex; align-items: center; justify-content: center; width: 100%; position: relative; min-height: 24px; margin-bottom: 8px; }
.day-label { font-size: 0.9rem; font-weight: 600; color: var(--text-primary); display: flex; align-items: center; justify-content: center; }
.day-label.is-today { background-color: #ed4956; color: white; width: 24px; height: 24px; border-radius: 50%; font-size: 0.8rem; box-shadow: 0 2px 4px rgba(237,73,86,0.3); }
.attendance-circle-mark { position: absolute; right: 5px; top: 50%; transform: translateY(-50%); width: 10px; height: 10px; background-color: #ed4956; border-radius: 50%; display: block; box-shadow: 0 0 5px rgba(237, 73, 86, 0.5); z-index: 10; transition: all 0.2s ease-in-out; }
.attendance-circle-mark.is-completed { background-color: #38a169; box-shadow: 0 0 5px rgba(56, 161, 105, 0.5); }

@media (max-width: 768px) {
  .day-header { justify-content: flex-start; }
  .attendance-circle-mark { width: 6px; height: 6px; right: 2px; }
}

.badges-container { width: 100%; display: flex; flex-direction: row; flex-wrap: wrap; gap: 4px; overflow-y: auto; max-height: 95px; padding: 2px; scrollbar-width: none; }
.day-scroll::-webkit-scrollbar { display: none; }
.badge-wrapper { flex: 0 0 calc(33.33% - 4px); min-width: 0; }
.source-badge { font-size: 0.52rem; font-weight: 800; padding: 2px 1px; border-radius: 3px; color: white; text-align: center; white-space: nowrap; width: 100%; display: block; line-height: 1.2; transition: all 0.2s; }
.source-badge.is-active { transform: scale(1.05); box-shadow: 0 0 0 2px var(--link-color); z-index: 10; }
.source-badge.is-closed { opacity: 0.5; filter: grayscale(0.4); }
.source-badge.LH { background-color: #38a169; }
.source-badge.SH { background-color: #3182ce; }
.source-badge.PRIVATE { background-color: #c08457; }
.source-badge.HUG { background-color: #4c51bf; }

.selected-notices-section { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 30px; margin-top: 10px; }
.selected-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 1px solid var(--divider-color); padding-bottom: 15px; }
.date-title { font-size: 1.2rem; font-weight: 800; margin: 0; color: var(--text-primary); }
.count-badge { font-size: 0.8rem; font-weight: 700; color: var(--link-color); background: rgba(0,149,246,0.1); padding: 4px 12px; border-radius: 20px; }
.notices-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.list-item-card { display: flex; flex-direction: column; gap: 12px; padding: 20px; background: var(--hover-bg); border-radius: 12px; border: 1px solid transparent; cursor: pointer; transition: all 0.2s; }
.list-item-card:hover { border-color: var(--border-color); transform: translateY(-3px); }
.highlight-card { border-color: var(--link-color) !important; background-color: rgba(0,149,246,0.02); }
.card-row-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.source-tag { padding: 2px 8px; border-radius: 4px; font-size: 0.65rem; font-weight: 900; color: white; }
.source-tag.LH { background-color: #38a169; }
.source-tag.SH { background-color: #3182ce; }
.source-tag.PRIVATE { background-color: #c08457; }
.source-tag.HUG { background-color: #4c51bf; }
.status-badge { font-size: 0.65rem; font-weight: 700; padding: 1px 6px; border-radius: 4px; }
.status-badge.open { background-color: rgba(56, 161, 105, 0.1); color: #38a169; }
.status-badge.result { background-color: rgba(107, 70, 193, 0.1); color: #6b46c1; }
.status-badge.info { background-color: rgba(49, 130, 206, 0.1); color: #3182ce; }
.status-badge.closed { background-color: rgba(237, 73, 86, 0.1); color: #ed4956; }
.region-info { font-size: 0.75rem; font-weight: 700; color: var(--text-primary); }
.date-info { font-size: 0.75rem; color: var(--text-secondary); margin-left: auto; }
.notice-title { font-size: 1rem; font-weight: 700; margin: 0; color: var(--text-primary); line-height: 1.4; min-height: 2.8em; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: ellipsis; }
.card-row-tags { display: flex; flex-wrap: wrap; gap: 6px; min-height: 1.5rem; }
.mini-tag { font-size: 0.75rem; color: var(--text-secondary); font-weight: 600; }
.mini-tag.matched { color: var(--link-color); font-weight: 700; }
.apply-link-btn { display: block; width: 100%; padding: 10px; background-color: var(--link-color); color: white; border-radius: 8px; text-decoration: none; font-size: 0.85rem; font-weight: 700; text-align: center; transition: opacity 0.2s; cursor: pointer; }
.no-data-msg { text-align: center; padding: 40px 0; color: var(--text-secondary); grid-column: span 2; }

@media (max-width: 768px) {
  .filter-scroll { justify-content: flex-start; padding: 10px 15px; }
  :deep(.vc-day) { min-height: 100px !important; height: 100px !important; }
  .day-cell { padding: 4px; }
  .selected-notices-section { padding: 20px; border-radius: 0; margin-top: 0; }
  .notices-grid { grid-template-columns: 1fr; }
  .no-data-msg { grid-column: span 1; }
  .date-info { margin-left: 0; width: 100%; margin-top: 2px; }
  .badge-wrapper { flex: 0 0 100%; } 
  .source-badge { font-size: 0.48rem; padding: 1px 2px; letter-spacing: -0.5px; }
  .mini-tag { background-color: var(--hover-bg); padding: 1px 6px; border-radius: 4px; font-size: 0.7rem; color: var(--text-secondary); font-weight: 500; display: inline-block; line-height: 1.4; }
}
</style>
