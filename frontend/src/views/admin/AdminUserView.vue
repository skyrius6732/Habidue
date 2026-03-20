<template>
  <div class="admin-user-container">
    <div class="admin-header">
      <div class="header-main">
        <h2>👥 사용자 관리 및 권한</h2>
        <span class="count-info">{{ filteredUsers.length }}건 / {{ users.length }}건</span>
      </div>
      
      <div class="search-filter-bar-v3">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input 
            v-model="searchQuery" 
            placeholder="닉네임 또는 이메일 검색..." 
            class="admin-search-input"
          />
        </div>
        
        <select v-model="onlineFilter" class="admin-filter-select">
          <option value="ALL">접속상태 (전체)</option>
          <option value="ONLINE">접속 중</option>
          <option value="OFFLINE">오프라인</option>
        </select>
        
        <select v-model="statusFilter" class="admin-filter-select">
          <option value="ALL">모든 계정 상태</option>
          <option value="ACTIVE">ACTIVE (정상)</option>
          <option value="RESTRICTED">RESTRICTED (활동 제한)</option>
          <option value="BLOCKED">BLOCKED (차단)</option>
        </select>
        
        <select v-model="roleFilter" class="admin-filter-select">
          <option value="ALL">모든 권한</option>
          <option value="ADMIN">ADMIN (관리자)</option>
          <option value="USER">USER (일반)</option>
        </select>

        <button @click="fetchUsers" class="btn-refresh-status-v3">🔄 접속상태갱신</button>
      </div>
    </div>

    <!-- 사용자 그리드 (PC: 5열 / 모바일: 2열) -->
    <div class="user-grid-outer scrollable">
      <div v-if="filteredUsers.length === 0" class="empty-results">
        검색 결과와 일치하는 사용자가 없습니다.
      </div>
      
      <div class="user-grid-wrapper">
        <div 
          v-for="user in paginatedUsers" 
          :key="user.id" 
          class="user-card-item"
          :class="{ 
            'is-expanded': expandedId === user.id, 
            'is-me': isMe(user), 
            'is-blocked': user.status === 'BLOCKED', 
            'is-online': user.isOnline,
            'is-admin': user.role === 'ADMIN'
          }"
        >
          <div class="user-summary" @click="toggleExpand(user.id)">
            <div class="summary-content">
              <span class="user-role-dot" :class="getStatusDotClass(user)"></span>
              <span class="user-name-text" :class="{ 'admin-text': user.role === 'ADMIN' }">
                {{ user.username }}
              </span>
              <span v-if="user.isOnline" class="online-status-badge">접속중</span>
              <span v-if="user.status === 'BLOCKED'" class="blocked-tag">🚫</span>
            </div>
            <div class="summary-actions">
              <span class="role-badge" :class="user.role">{{ user.role }}</span>
              <span v-if="isRestricted(user)" class="restricted-tag-mini">제한됨</span>
            </div>
          </div>

          <div v-if="expandedId === user.id" class="user-edit-drawer">
            <div class="edit-form-inner">
              <div class="edit-field">
                <label>이메일 / 가입일</label>
                <div class="readonly-text">{{ user.email }} ({{ formatDate(user.createdAt) }})</div>
              </div>

              <!-- [시니어] 카르마 및 제재 관리 -->
              <div class="penalty-admin-section">
                <div class="admin-row">
                  <div class="admin-cell">
                    <label>활동 신뢰 점수(Karma)</label>
                    <div class="karma-status-flex">
                      <span class="karma-value-large" :class="getKarmaClass(user.karmaPoint)">{{ (user.karmaPoint / 10).toFixed(1) }}</span>
                      <button @click="handleKarmaChange(user)" class="btn-adjust-karma">조정</button>
                    </div>
                  </div>
                  <div class="admin-cell">
                    <label>활동 제한 상태</label>
                    <div v-if="isRestricted(user)" class="restriction-info-flex">
                      <span class="restricted-text">~{{ formatDate(user.restrictedUntil) }}</span>
                      <button @click="handleLiftRestriction(user)" class="btn-lift-restriction">해제</button>
                    </div>
                    <div v-else class="restricted-text normal">정상</div>
                  </div>
                </div>
              </div>

              <div v-if="user.status === 'BLOCKED'" class="edit-field">
                <label>차단 사유</label>
                <div class="readonly-text reason">{{ user.blockedReason || '사유 없음' }}</div>
              </div>
              <div class="edit-field">
                <label>권한 변경</label>
                <select 
                  :value="user.role" 
                  @change="e => handleRoleChange(user, e.target.value)"
                  class="edit-select"
                  :disabled="isMe(user)"
                >
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
              <div v-if="!isMe(user)" class="edit-field">
                <label>활동 내역</label>
                <button @click="viewUserActivity(user.id)" class="btn-activity">
                  작성 글/댓글 보기
                </button>
              </div>
              <div v-if="!isMe(user)" class="edit-field">
                <label>계정 상태 관리</label>
                <button 
                  v-if="user.status === 'ACTIVE'" 
                  @click="handleStatusChange(user, 'BLOCKED')"
                  class="btn-block"
                >
                  계정 차단하기
                </button>
                <button 
                  v-else 
                  @click="handleStatusChange(user, 'ACTIVE')"
                  class="btn-unblock"
                >
                  차단 해제하기
                </button>
              </div>
              <!-- [시니어] 카르마 히스토리 리스트 -->
              <div class="karma-history-section">
                <label>최근 활동 신뢰 점수 변동 이력</label>
                <div v-if="userKarmaHistory[user.id] && userKarmaHistory[user.id].length > 0" class="history-list-mini">
                  <div v-for="item in userKarmaHistory[user.id].slice(0, 5)" :key="item.id" class="history-item-mini">
                    <span class="h-date">{{ formatDate(item.createdAt).split(' ')[0] }}</span>
                    <span class="h-reason" :title="item.comment">{{ item.reasonDescription }}</span>
                    <span class="h-change" :class="item.pointChange > 0 ? 'plus' : 'minus'">
                      {{ item.pointChange > 0 ? '+' : '' }}{{ (item.pointChange / 10).toFixed(1) }}
                    </span>
                  </div>
                </div>
                <div v-else class="empty-history">변동 이력이 없습니다.</div>
              </div>

              <div v-if="isMe(user)" class="me-warning-box">
                <span class="warning-icon">🛡️</span>
                <p class="warning-text">본인 계정은 시스템 보호 정책에 따라 관리 도구 사용이 제한됩니다.</p>
              </div>
              <button @click.stop="expandedId = null" class="btn-close-drawer">닫기</button>
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
import { useRouter } from 'vue-router'
import axios from '@/plugins/axios'
import { useAuthStore } from '@/stores/auth'

const users = ref([])
const router = useRouter()
const authStore = useAuthStore()
const searchQuery = ref('')
const roleFilter = ref('ALL')
const statusFilter = ref('ALL')
const onlineFilter = ref('ALL')
const expandedId = ref(null)

const currentPage = ref(1)
const pageSize = 60 

const karmaReasons = ref([])
const userKarmaHistory = ref({}) // userId별 히스토리 저장

const fetchUsers = async () => {
  try {
    const res = await axios.get('/api/admin/users')
    users.value = res.data.data
  } catch (e) { console.error('사용자 로드 실패') }
}

const fetchKarmaReasons = async () => {
  try {
    const res = await axios.get('/api/admin/users/karma-reasons')
    karmaReasons.value = res.data.data
  } catch (e) { console.error('사유 목록 로드 실패') }
}

const fetchUserKarmaHistory = async (userId) => {
  try {
    const res = await axios.get(`/api/admin/users/${userId}/karma-history`)
    userKarmaHistory.value[userId] = res.data.data
  } catch (e) { console.error('히스토리 로드 실패') }
}

const filteredUsers = computed(() => {
  return users.value.filter(u => {
    const matchesSearch = u.username.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
                         u.email.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchesRole = roleFilter.value === 'ALL' || u.role === roleFilter.value
    const matchesStatus = statusFilter.value === 'ALL' || u.status === statusFilter.value
    const matchesOnline = onlineFilter.value === 'ALL' || 
                         (onlineFilter.value === 'ONLINE' ? u.isOnline : !u.isOnline)
    return matchesSearch && matchesRole && matchesStatus && matchesOnline
  })
})

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / pageSize))
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredUsers.value.slice(start, start + pageSize)
})

watch([searchQuery, roleFilter, statusFilter, onlineFilter], () => { currentPage.value = 1 })

const toggleExpand = (id) => { 
  if (expandedId.value !== id) {
    fetchUserKarmaHistory(id)
  }
  expandedId.value = expandedId.value === id ? null : id 
}

const viewUserActivity = (userId) => {
  router.push({ name: 'adminCommunity', query: { userId } })
}

const handleRoleChange = async (user, newRole) => {
  if (!confirm(`'${user.username}'님의 권한을 ${newRole}(으)로 변경하시겠습니까?`)) return
  try {
    await axios.patch(`/api/admin/users/${user.id}/role`, null, { params: { role: newRole } })
    alert('변경되었습니다.')
    fetchUsers()
  } catch (e) { alert('변경 실패') }
}

const handleStatusChange = async (user, newStatus) => {
  let reason = null
  if (newStatus === 'BLOCKED') {
    reason = prompt(`'${user.username}'님을 차단하시겠습니까? 사유를 입력하세요:`)
    if (reason === null) return 
  } else {
    if (!confirm(`'${user.username}'님의 차단을 해제하시겠습니까?`)) return
  }
  try {
    await axios.patch(`/api/admin/users/${user.id}/status`, null, { params: { status: newStatus, reason: reason } })
    alert(newStatus === 'BLOCKED' ? '차단되었습니다.' : '해제되었습니다.')
    fetchUsers()
  } catch (e) { alert('상태 변경 실패') }
}

const handleKarmaChange = async (user) => {
  const currentP = (user.karmaPoint / 10).toFixed(1)
  const points = prompt(`'${user.username}'님의 점수 조정 (현재: ${currentP}P)\n양수(가점), 음수(감점) 입력 (예: 5 입력 시 +5.0P):`)
  if (!points || isNaN(points)) return

  const reasonListStr = karmaReasons.value.map((r, i) => `${i+1}. ${r.label}`).join('\n')
  const reasonIdx = prompt(`조정 사유를 선택하세요 (번호 입력):\n${reasonListStr}`)
  if (!reasonIdx || isNaN(reasonIdx)) return
  
  const selectedReason = karmaReasons.value[parseInt(reasonIdx) - 1]
  if (!selectedReason) return

  const comment = prompt('상세 사유(메모)를 입력해주세요:')
  if (comment === null) return

  try {
    await axios.patch(`/api/admin/users/${user.id}/karma`, null, { 
      params: { 
        points: parseFloat(points), 
        reason: selectedReason.value,
        reasonText: comment
      } 
    })
    alert('조정되었습니다.')
    fetchUsers()
    fetchUserKarmaHistory(user.id)
  } catch (e) { alert('카르마 조정 실패') }
}

const handleLiftRestriction = async (user) => {
  if (!confirm(`'${user.username}'님의 활동 제한을 즉시 해제하시겠습니까?`)) return
  try {
    await axios.delete(`/api/admin/users/${user.id}/restriction`)
    alert('해제되었습니다.')
    fetchUsers()
  } catch (e) { alert('제한 해제 실패') }
}

const isRestricted = (user) => {
  if (!user.restrictedUntil) return false
  return new Date(user.restrictedUntil) > new Date()
}

const getKarmaClass = (point) => {
  if (point >= 800) return 'safe'
  if (point >= 500) return 'warning'
  return 'danger'
}

const isMe = (user) => user.email === authStore.user?.email
const getStatusDotClass = (user) => {
  if (user.role === 'ADMIN') return user.isOnline ? 'online' : 'admin-offline'
  return user.isOnline ? 'online' : 'offline'
}
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(() => {
  fetchUsers()
  fetchKarmaReasons()
})
</script>

<style scoped>
.admin-user-container { max-width: 1400px; margin: 0 auto; height: 100%; width: 100%; display: flex; flex-direction: column; }

.admin-header { flex-shrink: 0; margin-bottom: 8px; }
.header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.header-main h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: var(--text-primary); }
.count-info { font-size: 0.85rem; color: var(--text-secondary); font-weight: 600; }

.search-filter-bar-v3 { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; align-items: center; }
.search-input-wrapper { flex: 1; min-width: 200px; position: relative; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.4; color: var(--text-primary); pointer-events: none; }
.admin-search-input { width: 100%; height: 38px; padding: 0 12px 0 35px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.85rem; background: var(--header-bg); color: var(--text-primary); transition: border-color 0.2s; outline: none; }
.admin-search-input:focus { border-color: var(--link-color); }

.admin-filter-select { padding: 0 10px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.82rem; min-width: 130px; height: 38px; background: var(--header-bg); color: var(--text-primary); outline: none; cursor: pointer; }
.btn-refresh-status-v3 { height: 38px; padding: 0 15px; background: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.82rem; font-weight: 700; cursor: pointer; color: var(--text-primary); white-space: nowrap; transition: all 0.2s; }
.btn-refresh-status-v3:hover { background: var(--border-color); }

.user-grid-outer { flex: 1; min-height: 0; overflow-y: auto; padding: 2px 0; margin-bottom: 5px; }
.user-grid-wrapper { display: grid; grid-template-columns: repeat(4, 1fr); grid-auto-rows: min-content; gap: 8px; }

.user-card-item { 
  background: var(--card-bg); 
  border: 1px solid var(--border-color); 
  border-radius: 10px; 
  display: flex; 
  flex-direction: column; 
  height: fit-content; 
  transition: all 0.2s; 
  color: var(--text-primary);
  width: 100%; /* 너비 고정 */
  box-sizing: border-box;
}
.user-card-item.is-me { border-color: var(--link-color); }
.user-summary { padding: 8px 12px; display: flex; justify-content: space-between; align-items: center; cursor: pointer; min-height: 40px; box-sizing: border-box; }
.summary-content { display: flex; align-items: center; gap: 6px; flex: 1; min-width: 0; }
.user-role-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; background-color: var(--text-secondary); }
.user-role-dot.online { background-color: #2ecc71; box-shadow: 0 0 5px #2ecc71; }
.user-role-dot.admin-offline { background-color: #ed4956; }
.user-name-text { font-weight: 600; font-size: 0.7rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary); }
.user-name-text.admin-text { color: #ed4956; font-weight: 800; }
.online-status-badge { font-size: 0.55rem; color: #2ecc71; font-weight: 800; background: rgba(46, 204, 113, 0.1); padding: 1px 4px; border-radius: 4px; margin-left: 2px; white-space: nowrap; }
.summary-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.role-badge { font-size: 0.45rem; font-weight: 800; color: var(--text-secondary); background: var(--tag-bg); padding: 1px 4px; border-radius: 4px; }
.role-badge.ADMIN { background-color: #ed4956; color: white; }

.user-edit-drawer { background: var(--hover-bg); border-top: 1px solid var(--border-color); border-bottom-left-radius: 10px; border-bottom-right-radius: 10px; }
.edit-form-inner { padding: 10px; display: flex; flex-direction: column; gap: 6px; }
.edit-field { display: flex; flex-direction: column; gap: 2px; }
.edit-field label { font-size: 0.6rem; font-weight: 700; color: var(--text-secondary); }
.readonly-text { font-size: 0.7rem; color: var(--text-primary); font-weight: 600; padding: 2px 0; word-break: break-all; }
.edit-select { height: 32px; padding: 0 8px; border: 1px solid var(--border-color); border-radius: 6px; font-size: 0.75rem; background: var(--header-bg); color: var(--text-primary); outline: none; }
.btn-activity { background: none; border: 1px solid var(--link-color); color: var(--link-color); padding: 6px; border-radius: 6px; font-size: 0.7rem; cursor: pointer; font-weight: 700; margin-top: 2px; transition: all 0.2s; }
.btn-activity:hover { background: var(--link-color); color: white; }
.btn-block { background: none; border: 1px solid #ed4956; color: #ed4956; padding: 6px; border-radius: 6px; font-size: 0.7rem; cursor: pointer; font-weight: 700; margin-top: 2px; }
.btn-block:hover { background: #ed4956; color: white; }
.btn-unblock { background: var(--link-color); color: white; border: none; padding: 6px; border-radius: 6px; font-size: 0.7rem; cursor: pointer; font-weight: 700; margin-top: 2px; }

/* [시니어] 카르마 뱃지 및 제한 태그 스타일 */
.karma-badge-mini { font-size: 0.55rem; font-weight: 850; padding: 1px 5px; border-radius: 4px; margin-left: auto; }
.karma-badge-mini.safe { background: rgba(46, 204, 113, 0.1); color: #2ecc71; }
.karma-badge-mini.warning { background: rgba(241, 196, 15, 0.1); color: #f1c40f; }
.karma-badge-mini.danger { background: rgba(231, 76, 60, 0.1); color: #e74c3c; }
.restricted-tag-mini { font-size: 0.5rem; font-weight: 850; color: white; background: #f39c12; padding: 1px 4px; border-radius: 4px; box-shadow: 0 1px 3px rgba(243, 156, 18, 0.3); }

/* [시니어] 패널티 관리 섹션 스타일 (2열 가로 배치) */
.penalty-admin-section { 
  background: var(--card-bg); 
  border: 1px solid var(--border-color); 
  border-radius: 10px; 
  margin: 8px 0; 
  padding: 12px;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.02);
}
.admin-row { 
  display: grid; 
  grid-template-columns: 1fr 1fr; 
  gap: 15px; 
}
.admin-cell { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  text-align: center;
  gap: 8px; /* 레이블과 콘텐츠 사이 간격 통일 */
  justify-content: flex-start;
}
.admin-cell label { 
  font-size: 0.55rem !important; 
  opacity: 0.7; 
  letter-spacing: -0.02em;
  height: 12px; /* 레이블 높이 고정 */
}
.karma-status-flex, .restriction-info-flex { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  gap: 6px; 
  width: 100%;
  min-height: 45px; /* 콘텐츠 영역 최소 높이 통일로 버튼 라인 맞춤 */
  justify-content: flex-end; /* 버튼이 항상 바닥에 오도록 설정 */
}
.karma-value-large { font-size: 1.1rem; font-weight: 900; line-height: 1; }
.karma-value-large.safe { color: #2ecc71; }
.karma-value-large.warning { color: #f1c40f; }
.karma-value-large.danger { color: #e74c3c; }

.btn-adjust-karma, .btn-lift-restriction { 
  width: 100%; 
  background: var(--hover-bg); 
  border: 1px solid var(--border-color); 
  padding: 4px 0; 
  border-radius: 5px; 
  font-size: 0.65rem; 
  font-weight: 800; 
  cursor: pointer; 
  color: var(--text-primary); 
  transition: all 0.2s;
}
.btn-lift-restriction { 
  border-color: #f39c12; 
  color: #f39c12;
  margin-top: 5px; /* 미세하게 위치를 아래로 보정 (최종 5px) */
}
.btn-lift-restriction:hover { background: #f39c12; color: white; }

.restricted-text { font-size: 0.65rem; font-weight: 800; color: #f39c12; line-height: 1.2; }
.restricted-text.normal { color: var(--text-secondary); opacity: 0.5; font-weight: 600; }

/* [시니어] 카르마 히스토리 섹션 스타일 */
.karma-history-section { 
  margin-top: 8px; 
  border-top: 1px dashed var(--border-color); 
  padding-top: 10px; 
  min-height: 120px; /* 이력이 없어도 공간을 확보하여 너비 고정 보조 */
  display: flex;
  flex-direction: column;
}
.karma-history-section label { font-size: 0.6rem; font-weight: 800; color: var(--text-secondary); margin-bottom: 6px; display: block; }
.history-list-mini { display: flex; flex-direction: column; gap: 4px; }
.history-item-mini { display: flex; align-items: center; justify-content: space-between; font-size: 0.65rem; background: var(--card-bg); padding: 4px 8px; border-radius: 4px; }
.h-date { color: var(--text-secondary); flex: 0 0 65px; }
.h-reason { color: var(--text-primary); font-weight: 600; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding: 0 8px; }
.h-change { font-weight: 800; flex: 0 0 30px; text-align: right; }
.h-change.plus { color: #2ecc71; }
.h-change.minus { color: #e74c3c; }
.empty-history { font-size: 0.65rem; color: var(--text-secondary); text-align: center; padding: 10px 0; opacity: 0.5; }

/* 본인 계정 안내 박스 스타일 */
.me-warning-box {
  margin-top: 8px;
  padding: 10px;
  background-color: rgba(52, 152, 219, 0.08);
  border: 1px solid rgba(52, 152, 219, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.warning-icon { font-size: 1rem; }
.warning-text { 
  margin: 0; 
  font-size: 0.68rem; 
  font-weight: 600; 
  color: var(--link-color); 
  line-height: 1.4;
  word-break: keep-all;
}

.btn-close-drawer { background: var(--tag-bg); color: var(--text-primary); border: none; padding: 6px; border-radius: 6px; font-size: 0.7rem; margin-top: 4px; cursor: pointer; font-weight: 600; }

.pagination-bar { flex-shrink: 0; height: 34px; display: flex; justify-content: center; align-items: center; gap: 8px; }
.btn-page { padding: 3px 8px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--header-bg); font-size: 0.7rem; cursor: pointer; color: var(--text-primary); transition: all 0.2s; }
.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

@media (max-width: 1200px) { .user-grid-wrapper { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 900px) { .user-grid-wrapper { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) {
  .header-main { gap: 10px; flex-direction: row; justify-content: space-between; align-items: center; }
  .header-main h2 { font-size: 1rem; }
  .count-info { font-size: 0.75rem; }
  .search-filter-bar-v3 { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .search-input-wrapper { grid-column: span 2; }
  .admin-search-input, .admin-filter-select, .btn-refresh-status-v3 { height: 34px; font-size: 0.8rem; min-width: 0; width: 100%; }
  .user-grid-wrapper { grid-template-columns: 1fr; } /* 모바일 1행 1유저 */
  .pagination-bar { margin-top: 15px; height: auto; }
}
</style>
