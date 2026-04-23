<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import axios from '@/plugins/axios'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const rules = ref([])
const masters = ref([])
const loading = ref(true)
const isSyncing = ref(false)
const selectedType = ref('ALL')
const activeTab = ref('effects') // 'badges' 또는 'effects'
const isEffectAccordionOpen = ref(false) // [시니어 조치] 이펙트 아코디언 상태

// 사용자 데이터 (미리보기용)
const userProfile = ref(null)
const previewEffect = ref(null) // 아코디언 프리뷰 전용 (백엔드 미반영)
const activityData = ref(null)

// 폼 노출 상태
const showMasterModal = ref(false) // 배지 추가 패널
const showEditModal = ref(false)   // 규칙 추가/수정 패널
const isNewRule = ref(false)
const editingId = ref(null)

// 폼 데이터
const masterForm = ref({ code: '', name: '', description: '', type: 'COMMUNITY', badgeTip: '' })
const ruleForm = ref({ badgeType: '', level: 1, requiredValue: 0, rankEmoji: '', rankTitle: '', categoryName: '' })

// [시니어 조치] 이펙트 관리 상태
const searchUserInput = ref('')
const searchedUsers = ref([])
const selectedEffectCodes = ref([])   // 복수 이펙트 선택
const effectSource = ref('EVENT')
const isGranting = ref(false)
const isRevoking = ref(false)
const isLoadingAllUsers = ref(false)
const grantMessage = ref('')

// 선택된 사용자: publicId → userObject 맵으로 관리 (검색 변경 후에도 유지)
const selectedUsersData = ref({})
const selectedUserIds = computed(() => Object.keys(selectedUsersData.value))
const selectedUsersList = computed(() => Object.values(selectedUsersData.value))

const addUserToSelection = (user) => {
  selectedUsersData.value = { ...selectedUsersData.value, [user.publicId]: user }
}
const removeUserFromSelection = (publicId) => {
  const copy = { ...selectedUsersData.value }
  delete copy[publicId]
  selectedUsersData.value = copy
}
const toggleUserSelect = (user) => {
  if (selectedUsersData.value[user.publicId]) {
    removeUserFromSelection(user.publicId)
  } else {
    addUserToSelection(user)
  }
}

// 검색결과 전체선택 체크박스 상태
const isAllSearchedSelected = computed(() =>
  searchedUsers.value.length > 0 &&
  searchedUsers.value.every(u => !!selectedUsersData.value[u.publicId])
)
const toggleSelectAllSearched = () => {
  if (isAllSearchedSelected.value) {
    searchedUsers.value.forEach(u => removeUserFromSelection(u.publicId))
  } else {
    searchedUsers.value.forEach(u => addUserToSelection(u))
  }
}

// 전체선택 (검색 없을때 → 모든 활성 유저)
const selectAllUsers = async () => {
  if (searchUserInput.value.trim()) {
    toggleSelectAllSearched()
    return
  }
  isLoadingAllUsers.value = true
  try {
    const res = await axios.get('/api/users/admin/active-list')
    const users = res.data.data || []
    users.forEach(u => addUserToSelection(u))
  } catch (e) {
    console.error('전체 사용자 로드 실패', e)
  } finally {
    isLoadingAllUsers.value = false
  }
}

// 레벨 이펙트 해금 맵 (effectConfig.js와 동일하게 유지)
const LEVEL_UNLOCKS = {
  5: 'MAGIC_BUBBLES', 10: 'BRONZE_WINGS', 15: 'STARRY_NIGHT', 20: 'SILVER_WINGS',
  25: 'BOMB', 30: 'GOLD_WINGS', 35: 'SHADOW_DEMON', 40: 'VOID_RIFT',
  45: 'THUNDER_BLUE', 50: 'NEON_SIGN', 60: 'AURORA_FLAME', 70: 'RAINBOW_WAVE'
}
const getLevelUnlock = (code) => {
  const entry = Object.entries(LEVEL_UNLOCKS).find(([, v]) => v === code)
  return entry ? parseInt(entry[0]) : null
}

// 이펙트 코드 → 괄호 안 짧은 이름
const effectLabel = (code) => {
  const found = specialEffects.find(e => e.id === code)
  if (!found) return code
  const match = found.name.match(/\(([^)]+)\)/)
  return match ? match[1] : found.name
}

// 이펙트 복수 토글
const toggleEffectSelect = (code) => {
  const idx = selectedEffectCodes.value.indexOf(code)
  if (idx > -1) {
    selectedEffectCodes.value.splice(idx, 1)
  } else {
    selectedEffectCodes.value.push(code)
  }
}

const selectableEffectCodes = computed(() => specialEffects.filter(e => e.id).map(e => e.id))
const isAllEffectsSelected = computed(() =>
  selectableEffectCodes.value.length > 0 &&
  selectableEffectCodes.value.every(code => selectedEffectCodes.value.includes(code))
)
const toggleSelectAllEffects = () => {
  if (isAllEffectsSelected.value) {
    selectedEffectCodes.value = []
  } else {
    selectedEffectCodes.value = [...selectableEffectCodes.value]
  }
}

const canExecute = computed(() =>
  selectedUserIds.value.length > 0 && selectedEffectCodes.value.length > 0
)

const resetSelection = () => {
  selectedUsersData.value = {}
  selectedEffectCodes.value = []
  searchedUsers.value = []
  searchUserInput.value = ''
}

// 특수 효과 리스트 — 마이페이지와 동일한 순서 (레벨순 → 이벤트/상점순)
const specialEffects = [
  { id: null, name: '효과 없음' },
  // 레벨 해금 순서
  { id: 'PIONEER_WINGS', name: '🕊️ 화이트 윙 (White)' },
  { id: 'MAGIC_BUBBLES', name: '🫧 방울방울 (Bubble)' },
  { id: 'BRONZE_WINGS', name: '🥉 브론즈 윙 (Bronze)' },
  { id: 'STARRY_NIGHT', name: '✨ 별무리 (Starry)' },
  { id: 'SILVER_WINGS', name: '🥈 실버 윙 (Silver)' },
  { id: 'BOMB', name: '🔫 레이저 (Laser)' },
  { id: 'GOLD_WINGS', name: '🎖️ 골드 윙 (Gold)' },
  { id: 'SHADOW_DEMON', name: '🧪 포이즌 (Poison)' },
  { id: 'VOID_RIFT', name: '🌀 보이드 (Void)' },
  { id: 'THUNDER_BLUE', name: '⚡ 번개 (Thunder)' },
  { id: 'NEON_SIGN', name: '💡 네온 (Neon)' },
  { id: 'AURORA_FLAME', name: '🔥 화염 (Flame)' },
  { id: 'RAINBOW_WAVE', name: '🌈 무지개 (Rainbow)' },
  // 이벤트 / 상점 이펙트
  { id: 'ICE_FROST', name: '❄️ 얼음 (Frost)' },
  { id: 'SAKURA_BLOOM', name: '🌸 벚꽃 (Sakura)' },
  { id: 'ROSES_BLOOM', name: '🌹 장미 (Roses)' },
  { id: 'PIXEL_GLITCH', name: '💥 픽셀 글리치 (Glitch)' },
  { id: 'LOVE_HEART', name: '💕 하트 (Heart)' },
  { id: 'SHOOTING_STAR', name: '🌠 별똥별 (Shooting)' },
  { id: 'CROWN', name: '👑 왕관 (Crown)' },
  { id: 'SWORDS_CROSS', name: '⚔️ 칼 교차 (Swords)' },
  { id: 'FLOWER_CROWN', name: '🌸 화관 (Flower Crown)' },
  { id: 'STAR_TIARA', name: '🌟 별 티아라 (Tiara)' },
  { id: 'FLOWER_RAIN', name: '🌺 꽃비 (Flower Rain)' },
  { id: 'SCAN_LINE', name: '📺 스캔라인 (Scan)' },
  { id: 'CHROMATIC_ABERRATION', name: '🌈 색분리 (Chromatic)' },
  { id: 'ECHO_TRAIL', name: '👻 잔상 (Echo)' },
  { id: 'CORRUPTED_TEXT', name: '🔲 손상 텍스트 (Corrupted)' },
  { id: 'GLITCH_SHIFT', name: '⚡ 글리치 (Glitch Shift)' },
  { id: 'LIQUID_DISTORT', name: '💧 액체 (Liquid)' },
  { id: 'HALO', name: '✨ 후광 (Halo)' },
  { id: 'LEAVES', name: '🍂 낙엽 (Leaves)' },
  { id: 'BUTTERFLIES', name: '🦋 나비 (Butterflies)' },
  { id: 'ORBS', name: '🔮 구체 (Orbs)' },
  { id: 'SCALE_COLLAPSE', name: '⬇️ 축소 (Collapse)' }
]

const updateEffect = (effectId) => {
  // 로컬 프리뷰만 변경 — 실제 장착(백엔드) 반영 없음
  previewEffect.value = previewEffect.value === effectId ? null : effectId
}

const badgeTypes = computed(() => {
  const tabs = [{ id: 'ALL', name: '전체보기' }]
  masters.value.forEach(m => {
    const typeCode = m.code.replace('_BASE', '')
    tabs.push({ id: typeCode, name: m.name })
  })
  return tabs
})

const fetchInitialData = async () => {
  loading.value = true
  try {
    const [rulesRes, mastersRes, userRes, activityRes] = await Promise.all([
      axios.get('/api/admin/badges/rules'),
      axios.get('/api/admin/badges/master'),
      axios.get('/api/users/me'),
      axios.get('/api/users/me/activity')
    ])
    rules.value = rulesRes.data.data
    masters.value = mastersRes.data.data
    userProfile.value = userRes.data.data
    previewEffect.value = userRes.data.data?.equippedEffect ?? null
    activityData.value = activityRes.data.data
  } catch (e) {
    console.error('데이터 로드 실패', e)
  } finally {
    loading.value = false
  }
}

const filteredRules = computed(() => {
  if (selectedType.value === 'ALL') return rules.value
  return rules.value.filter(r => r.badgeType === selectedType.value)
})

// 디자인 참조용 고정 티어
const previewLevels = [1, 5, 10, 30, 50, 70, 90, 100]

// 선택된 타입에 따른 배지 미리보기 데이터 생성 (실제 규칙 기반)
const tierPreviewBadges = computed(() => {
  if (selectedType.value === 'ALL') return []
  
  const type = selectedType.value
  const typeRules = rules.value.filter(r => r.badgeType === type).sort((a, b) => a.level - b.level)
  
  // 현재 타입의 마스터 정보 찾기
  const master = masters.value.find(m => m.code === `${type}_BASE`) || 
                 masters.value.find(m => m.code.startsWith(type)) || 
                 { description: '상세 설명이 없습니다.', badgeTip: '획득 팁이 없습니다.' }

  return typeRules.map(rule => {
    // 레벨에 따른 디자인 티어 매핑 (1, 5, 10, 30, 50, 70, 90, 100)
    const tiers = [100, 90, 70, 50, 30, 10, 5, 1]
    const designTier = tiers.find(t => rule.level >= t) || 1

    return {
      level: rule.level,
      designTier: designTier,
      emoji: rule.rankEmoji,
      displayName: `${rule.rankEmoji} ${rule.rankTitle} ${rule.categoryName}`,
      description: master.description,
      badgeTip: master.badgeTip
    }
  })
})

const getTypeLabel = (typeId) => {
  const found = badgeTypes.value.find(t => t.id === typeId)
  return found ? found.name : typeId
}

// --- 규칙 관리 로직 ---
const startEditRule = (rule) => {
  showMasterModal.value = false
  editingId.value = rule.id
  ruleForm.value = { ...rule }
  isNewRule.value = false
  showEditModal.value = true
  nextTick(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
}

const handleRuleToggle = () => {
  if (showEditModal.value) {
    showEditModal.value = false
    return
  }
  if (selectedType.value === 'ALL') { alert('배지 타입을 먼저 선택해주세요.'); return }
  showMasterModal.value = false
  isNewRule.value = true
  editingId.value = null
  ruleForm.value = { badgeType: selectedType.value, level: 1, requiredValue: 0, rankEmoji: '🌱', rankTitle: '', categoryName: '' }
  showEditModal.value = true
  nextTick(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
}

const saveRule = async () => {
  try {
    if (isNewRule.value) {
      await axios.post(`/api/admin/badges/rules`, ruleForm.value, { 
        params: { type: ruleForm.value.badgeType, level: ruleForm.value.level } 
      })
    } else {
      await axios.put(`/api/admin/badges/rules/${editingId.value}`, ruleForm.value)
    }
    showEditModal.value = false
    fetchInitialData()
    alert('저장되었습니다.')
  } catch (e) { alert('저장 실패') }
}

const deleteRule = async (id) => {
  if (!confirm('이 규칙을 영구 삭제하시겠습니까?')) return
  try {
    await axios.delete(`/api/admin/badges/rules/${id}`)
    fetchInitialData()
  } catch (e) { alert('삭제 실패') }
}

const saveMaster = async () => {
  try {
    const code = masterForm.value.code.toUpperCase().endsWith('_BASE') ? masterForm.value.code : masterForm.value.code + '_BASE'
    await axios.post('/api/admin/badges/master', null, { params: { ...masterForm.value, code: code } })
    showMasterModal.value = false
    fetchInitialData()
    alert('새 배지가 생성되었습니다.')
  } catch (e) { alert('생성 실패') }
}

const handleSyncAll = async () => {
  if (!confirm('전체 유저의 배지 정보를 재동기화하시겠습니까?')) return
  isSyncing.value = true
  try {
    const res = await axios.post('/api/admin/badges/sync-all')
    alert(res.data.data)
  } catch (e) { alert('동기화 실패') } finally { isSyncing.value = false }
}

const handleDeleteMaster = async () => {
  if (selectedType.value === 'ALL') return
  const master = masters.value.find(m => m.code.startsWith(selectedType.value))
  if (!master) return
  if (!confirm(`[위험] "${master.name}" 배지와 모든 규칙을 삭제하시겠습니까?`)) return
  try {
    await axios.delete(`/api/admin/badges/master/${master.code}`)
    selectedType.value = 'ALL'
    fetchInitialData()
    alert('삭제되었습니다.')
  } catch (e) { alert('삭제 실패') }
}

const handleWheelScroll = (e) => {
  const container = e.currentTarget
  if (e.deltaY !== 0) {
    e.preventDefault()
    container.scrollLeft += e.deltaY
  }
}

// [시니어 조치] 사용자 검색
const searchUsers = async () => {
  if (!searchUserInput.value.trim()) {
    searchedUsers.value = []
    return
  }
  try {
    const res = await axios.get('/api/users', {
      params: { search: searchUserInput.value, limit: 10 }
    })
    searchedUsers.value = res.data.data || []
  } catch (e) {
    console.error('사용자 검색 실패', e)
    searchedUsers.value = []
  }
}

// 이펙트 지급 (배치 엔드포인트 통일)
const grantEffectBatch = async () => {
  if (!canExecute.value) { alert('사용자와 이펙트를 선택해주세요.'); return }
  if (!confirm(`${selectedUserIds.value.length}명에게 ${selectedEffectCodes.value.length}개 이펙트를 지급하시겠습니까?`)) return

  isGranting.value = true
  grantMessage.value = ''
  try {
    const params = new URLSearchParams()
    selectedUserIds.value.forEach(id => params.append('publicIds', id))
    selectedEffectCodes.value.forEach(code => params.append('effectCodes', code))
    params.append('source', effectSource.value)

    const res = await axios.post('/api/users/admin/batch/grant-effect', null, { params })
    const data = res.data.data
    grantMessage.value = `✅ 지급 완료 — 신규 지급: ${data.successCount}건 / 이미 보유: ${data.alreadyOwned}건 (${data.totalRequested}명 × ${data.effectCount}개 요청)`
    resetSelection()
  } catch (e) {
    grantMessage.value = `❌ 지급 실패: ${e.response?.data?.message || e.message}`
  } finally {
    isGranting.value = false
  }
}

// 이펙트 회수 (배치 엔드포인트 통일)
const revokeEffectBatch = async () => {
  if (!canExecute.value) { alert('사용자와 이펙트를 선택해주세요.'); return }
  if (!confirm(`${selectedUserIds.value.length}명에게서 ${selectedEffectCodes.value.length}개 이펙트를 회수하시겠습니까?`)) return

  isRevoking.value = true
  grantMessage.value = ''
  try {
    const params = new URLSearchParams()
    selectedUserIds.value.forEach(id => params.append('publicIds', id))
    selectedEffectCodes.value.forEach(code => params.append('effectCodes', code))

    const res = await axios.delete('/api/users/admin/batch/revoke-effect', { params })
    const data = res.data.data
    grantMessage.value = `✅ 회수 완료 — 회수됨: ${data.successCount}건 / 미보유: ${data.notOwned}건 (${data.totalRequested}명 × ${data.effectCount}개 요청)`
    resetSelection()
  } catch (e) {
    grantMessage.value = `❌ 회수 실패: ${e.response?.data?.message || e.message}`
  } finally {
    isRevoking.value = false
  }
}

onMounted(fetchInitialData)
</script>

<template>
  <div class="admin-badge-container">
    <div class="admin-header">
      <div class="header-main">
        <div class="title-area">
          <h2>🎖️ 배지 마스터리 관리</h2>
        </div>
        <div class="header-btn-group">
        </div>
      </div>

      <!-- [시니어 조치] 탭 네비게이션 -->
      <div class="admin-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'effects' }"
          @click="activeTab = 'effects'"
        >
          ✨ 효과 관리
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'badges' }"
          @click="activeTab = 'badges'"
        >
          🎖️ 배지 관리
        </button>
      </div>

      <div class="search-filter-bar">
      </div>

      <!-- [시니어 조치] 인라인 입력 패널 (탭 내부로 이동) -->
      <Transition name="slide-fade" v-if="activeTab === 'badges'">
        <div v-if="showMasterModal || showEditModal" class="admin-form-panel">
          <!-- 1. 배지 추가 폼 -->
          <div v-if="showMasterModal" class="panel-content">
            <div class="form-grid">
              <div class="form-group-v3">
                <label>식별 코드 (영문)</label>
                <input v-model="masterForm.code" placeholder="예: PICK_KING" />
              </div>
              <div class="form-group-v3">
                <label>배지 이름</label>
                <input v-model="masterForm.name" placeholder="예: 추천 왕" />
              </div>
              <div class="form-group-v3 full">
                <label>설명</label>
                <textarea v-model="masterForm.description" rows="1" placeholder="배지에 대한 설명을 입력하세요."></textarea>
              </div>
              <div class="form-group-v3 full">
                <label>획득 팁</label>
                <input v-model="masterForm.badgeTip" placeholder="예: 더 많은 글을 작성하여 레벨을 올려보세요!" />
              </div>
              <div class="form-group-v3">
                <label>활동 타입 연동</label>
                <select v-model="masterForm.type">
                  <option value="KNOWLEDGE">정보형 (좋아요)</option>
                  <option value="SINCERITY">참여형 (댓글/출석)</option>
                  <option value="REVIEW">후기형 (후기글)</option>
                  <option value="COMMUNITY">활동형 (일반글)</option>
                  <option value="EVENT">이벤트형 (스페셜)</option>
                </select>
              </div>
            </div>
            <div class="panel-footer justify-end">
              <button class="btn-primary" @click="saveMaster">배지 생성하기</button>
              <button class="btn-secondary" @click="showMasterModal = false">취소</button>
            </div>
          </div>

          <!-- 2. 규칙 추가/수정 폼 -->
          <div v-if="showEditModal" class="panel-content">
            <div class="form-grid">
              <div class="form-item">
                <label>대상 레벨</label>
                <input type="number" v-model="ruleForm.level" :disabled="!isNewRule" placeholder="예: 10" />
              </div>
              <div class="form-item">
                <label>필요 수치</label>
                <input type="number" v-model="ruleForm.requiredValue" placeholder="예: 30" />
              </div>
              <div class="form-item emoji-row">
                <div class="inner-field">
                  <label>이모지</label>
                  <input v-model="ruleForm.rankEmoji" placeholder="🥉" />
                </div>
                <div class="inner-field">
                  <label>등급 칭호</label>
                  <input v-model="ruleForm.rankTitle" placeholder="활동가" />
                </div>
              </div>
              <div class="form-item full">
                <label>분야 명칭</label>
                <input v-model="ruleForm.categoryName" placeholder="지식인" />
              </div>
            </div>
            <div class="panel-footer">
              <div class="inline-preview pc-only">
                미리보기: <b>{{ ruleForm.rankEmoji }} {{ ruleForm.rankTitle }} {{ ruleForm.categoryName }}</b>
              </div>
              <div class="footer-btn-group">
                <button class="btn-primary" @click="saveRule">{{ isNewRule ? '규칙 추가하기' : '규칙 수정완료' }}</button>
                <button class="btn-secondary" @click="showEditModal = false">취소</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- [시니어 조치] 배지 관리 탭 -->
    <div v-if="activeTab === 'badges'" class="main-content-scrollable scrollable">
      <!-- 배지 관리 탭 헤더 (버튼 + 필터) -->
      <div class="tab-header">
        <div class="tab-controls">
          <button class="btn-compact btn-sync" @click="handleSyncAll" :disabled="isSyncing">
            {{ isSyncing ? '동기화 중...' : '🔄 전체 동기화' }}
          </button>
          <button class="btn-compact btn-master-add" @click="showMasterModal = !showMasterModal; showEditModal = false">
            {{ showMasterModal ? '닫기' : '➕ 배지 추가' }}
          </button>
          <button v-if="selectedType !== 'ALL'" class="btn-compact btn-danger" @click="handleDeleteMaster">🗑️ 배지 삭제</button>
          <button v-if="selectedType !== 'ALL'" class="btn-compact btn-rule-add" @click="handleRuleToggle">
            {{ showEditModal ? '닫기' : '➕ 규칙 추가' }}
          </button>
        </div>

        <div class="type-filter-chips scrollable-x" @wheel="handleWheelScroll">
          <button
            v-for="tab in badgeTypes" :key="tab.id"
            class="chip-btn" :class="{ active: selectedType === tab.id }"
            @click="selectedType = tab.id"
          >
            {{ tab.name }}
          </button>
        </div>
      </div>

      <!-- PC: 테이블 리스트 -->
      <div class="table-wrapper pc-only">
        <div v-if="loading" class="loading-state">데이터를 불러오는 중...</div>
        <table v-else class="admin-table">
          <thead>
            <tr>
              <th class="col-type">타입</th>
              <th class="col-lv">레벨</th>
              <th class="col-emoji">이모지</th>
              <th class="col-title">등급 칭호</th>
              <th class="col-cat">분야 명칭</th>
              <th class="col-val">필요수치</th>
              <th class="col-preview">프리뷰</th>
              <th class="col-action">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in filteredRules" :key="rule.id">
              <td class="col-type"><span class="type-badge-v3">{{ getTypeLabel(rule.badgeType) }}</span></td>
              <td class="col-lv"><b>Lv.{{ rule.level }}</b></td>
              <td class="col-emoji">{{ rule.rankEmoji }}</td>
              <td class="cell-text">{{ rule.rankTitle }}</td>
              <td class="cell-text">{{ rule.categoryName }}</td>
              <td class="col-val"><code>{{ rule.requiredValue }}</code></td>
              <td class="col-preview">
                <span class="preview-chip">{{ rule.rankEmoji }} {{ rule.rankTitle }} {{ rule.categoryName }}</span>
              </td>
              <td class="col-action">
                <div class="actions-wrapper">
                  <button class="btn-edit" @click="startEditRule(rule)">수정</button>
                  <button class="btn-delete" @click="deleteRule(rule.id)">삭제</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredRules.length === 0">
              <td colspan="8" class="empty-td">표시할 데이터가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 모바일: 카드 리스트 -->
      <div class="mobile-rule-cards mobile-only">
        <div v-if="loading" class="loading-state">데이터 로드 중...</div>
        <div v-else v-for="rule in filteredRules" :key="rule.id" class="rule-mobile-card">
          <div class="card-header-flex">
            <div class="card-emoji-box">{{ rule.rankEmoji }}</div>
            <div class="card-meta-right">
              <span class="card-lv">Lv.<b>{{ rule.level }}</b></span>
              <span class="card-val">수치: <code>{{ rule.requiredValue }}</code></span>
            </div>
          </div>
          <div class="card-info">
            <div class="card-title-row">
              <span class="card-rank">{{ rule.rankTitle }}</span>
              <span class="card-cat">{{ rule.categoryName }}</span>
            </div>
          </div>
          <div class="card-actions">
            <button class="btn-edit-m" @click="startEditRule(rule)">수정</button>
            <button class="btn-delete-m" @click="deleteRule(rule.id)">삭제</button>
          </div>
        </div>
        <div v-if="filteredRules.length === 0 && !loading" class="empty-state-m">표시할 데이터가 없습니다.</div>
      </div>

      <!-- 배지 카드 미리보기 (실제 규칙 기반) -->
      <section class="admin-section preview-section" style="margin-top: 40px;">
        <div class="section-header">
          <h3>💳 카드 미리보기</h3>
          <p class="section-desc">{{ selectedType === 'ALL' ? '배지 타입을 선택하면 미리보기가 표시됩니다.' : getTypeLabel(selectedType) + ' 기준 디자인 프리뷰' }}</p>
        </div>
        <div class="badge-preview-grid">
          <div v-for="testBadge in tierPreviewBadges" :key="testBadge.level" 
            class="badge-card-set" :class="`tier-${testBadge.designTier}`">
            <div v-if="(testBadge.designTier >= 30)" class="card-glass-reflection"></div>
            <div class="badge-visual-set" :class="`tier-${testBadge.designTier}`">
              <span class="badge-emoji-set">{{ testBadge.emoji }}</span>
            </div>
            <div class="badge-info-set">
              <div class="badge-header-flex">
                <div class="badge-level-tag-set" :class="`tier-${testBadge.designTier}`">Lv.{{ testBadge.level }}</div>
                <span class="preview-tag">PREVIEW</span>
              </div>
              <h4 class="badge-name-set" :class="`tier-${testBadge.designTier}`">{{ testBadge.displayName.replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]\s*/g, '') }}</h4>
              <p class="badge-desc-set">{{ testBadge.description }}</p>
              <div class="badge-tip-set" :class="`tier-${testBadge.designTier}`">
                <span class="tip-icon-v3">💡</span>
                <span class="tip-text-standard">{{ testBadge.badgeTip }}</span>
              </div>
              <div class="badge-footer-set" :class="`tier-${testBadge.designTier}`">
                <span class="badge-metric-set">수치: <b>999</b></span>
                <span class="badge-date-set">2026.03.17</span>
              </div>
            </div>
          </div>
          <div v-if="tierPreviewBadges.length === 0" class="empty-preview-state">
            <span class="empty-icon">📂</span>
            <p>표시할 배지 규칙이 없습니다.<br>상단에서 <b>규칙 추가</b>를 통해 카드를 등록해 보세요!</p>
          </div>
        </div>
      </section>
    </div>

    <!-- 효과 관리 탭 -->
    <div v-if="activeTab === 'effects'" class="main-content-scrollable scrollable">
      <div class="effect-layout">

        <!-- 등급별 효과 미리보기 -->
        <section v-if="userProfile" class="admin-section preview-section mt-40">
          <div class="section-header">
            <h3>✨ 등급별 효과 미리보기</h3>
            <p class="section-desc">현재 계정(@{{ userProfile.username }}) 기준 출력</p>
          </div>

          <!-- 특수 효과 샌드박스 (아코디언 형식) -->
          <div class="effect-sandbox-accordion">
            <div class="accordion-header" @click="isEffectAccordionOpen = !isEffectAccordionOpen">
              <span class="a-icon">🪄</span>
              <div class="a-text-group">
                <span class="a-title">본인에게 효과 즉시 적용 테스트</span>
                <span class="a-subtitle">다양한 효과를 선택해 실시간으로 확인해보세요.</span>
              </div>
              <span class="a-arrow">{{ isEffectAccordionOpen ? '▼' : '▲' }}</span>
            </div>

            <Transition name="fade-slide">
              <div v-if="isEffectAccordionOpen" class="accordion-body">
                <div class="effect-chips">
                  <button
                    v-for="eff in specialEffects" :key="eff.id"
                    class="chip-btn-v3" :class="{ active: previewEffect === eff.id }"
                    @click="updateEffect(eff.id)"
                  >
                    {{ eff.name }}
                  </button>
                </div>
              </div>
            </Transition>
          </div>

          <div class="tier-preview-grid">
            <div v-for="lv in previewLevels" :key="lv" class="preview-item">
              <span class="preview-label">Lv.{{ lv }}</span>
              <AnimatedNickname
                :nickname="userProfile.nickname || userProfile.username"
                :level="lv"
                :exp="lv * lv * 50 - 1"
                :badges="activityData?.badges"
                :show-effects="true"
                :equipped-effect="previewEffect"
                tooltip-direction="top"
                :is-ellipsis="(userProfile?.nickname || userProfile?.username || '').length >= 6"
              />
            </div>
          </div>
        </section>

        <!-- 1️⃣ 사용자 검색 & 선택 -->
        <section class="effect-panel">
          <div class="effect-panel-header">
            <span class="effect-panel-title">1️⃣ 사용자 선택</span>
            <button class="btn-select-all" @click="selectAllUsers" :disabled="isLoadingAllUsers">
              {{ isLoadingAllUsers ? '로딩 중...' : (searchUserInput ? `검색된 ${searchedUsers.length}명 전체선택` : '모든 사용자 전체선택') }}
            </button>
          </div>

          <input
            v-model="searchUserInput"
            @input="searchUsers"
            placeholder="이름, 이메일, ID 검색 (탈퇴 유저 제외)"
            class="search-input"
          />

          <!-- 검색 결과 -->
          <div v-if="searchedUsers.length > 0" class="search-results">
            <div class="select-all-row" @click="toggleSelectAllSearched">
              <input type="checkbox" :checked="isAllSearchedSelected" readonly class="user-checkbox" />
              <span class="select-all-label">검색 결과 전체 선택 ({{ searchedUsers.length }}명)</span>
            </div>
            <div
              v-for="user in searchedUsers"
              :key="user.publicId"
              class="user-item"
              :class="{ selected: !!selectedUsersData[user.publicId] }"
              @click="toggleUserSelect(user)"
            >
              <input type="checkbox" :checked="!!selectedUsersData[user.publicId]" readonly class="user-checkbox" />
              <div class="user-info">
                <div class="user-name">{{ user.nickname || user.username }}</div>
                <div class="user-meta">{{ user.email }} (Lv.{{ user.level }})</div>
                <div v-if="user.ownedEffectCodes?.length > 0" class="user-effects">
                  <span
                    v-for="code in user.ownedEffectCodes" :key="code"
                    class="effect-chip-small"
                    :class="{ matched: selectedEffectCodes.includes(code) }"
                  >{{ effectLabel(code) }}</span>
                </div>
                <div v-else class="user-no-effect">이펙트 없음</div>
              </div>
            </div>
          </div>
          <div v-else-if="searchUserInput" class="empty-search">검색 결과가 없습니다.</div>

          <!-- 선택된 사용자 카드 패널 -->
          <div v-if="selectedUsersList.length > 0" class="selected-panel">
            <div class="selected-panel-header">
              선택된 사용자 <b>{{ selectedUsersList.length }}명</b>
              <button class="btn-clear-all" @click="selectedUsersData = {}">전체 해제</button>
            </div>

            <!-- 50명 이하: 카드 뷰 -->
            <div v-if="selectedUsersList.length <= 50" class="selected-cards">
              <div v-for="user in selectedUsersList" :key="user.publicId" class="user-card">
                <div class="card-top">
                  <span class="card-name">{{ user.nickname || user.username }}</span>
                  <button class="card-remove" @click="removeUserFromSelection(user.publicId)">✕</button>
                </div>
                <div class="card-effects">
                  <template v-if="user.ownedEffectCodes?.length > 0">
                    <span
                      v-for="code in user.ownedEffectCodes" :key="code"
                      class="effect-chip-small"
                      :class="{ matched: selectedEffectCodes.includes(code) }"
                    >{{ effectLabel(code) }}</span>
                  </template>
                  <span v-else class="user-no-effect">없음</span>
                </div>
              </div>
            </div>

            <!-- 50명 초과: 요약만 -->
            <div v-else class="selected-summary">
              {{ selectedUsersList.length }}명 선택됨 — 이펙트 칩은 50명 이하일 때만 표시됩니다.
            </div>
          </div>
        </section>

        <!-- 2️⃣ 이펙트 복수 선택 -->
        <section class="effect-panel">
          <div class="effect-panel-header">
            <span class="effect-panel-title">2️⃣ 이펙트 선택</span>
            <div style="display: flex; align-items: center; gap: 8px;">
              <span class="selected-count-badge" v-if="selectedEffectCodes.length > 0">{{ selectedEffectCodes.length }}개 선택</span>
              <button class="btn-select-all" @click="toggleSelectAllEffects">
                {{ isAllEffectsSelected ? '전체 해제' : '전체 선택' }}
              </button>
            </div>
          </div>
          <div class="effects-toggle-grid">
            <button
              v-for="eff in specialEffects.filter(e => e.id)"
              :key="eff.id"
              class="effect-toggle-btn"
              :class="{ active: selectedEffectCodes.includes(eff.id), 'is-level': getLevelUnlock(eff.id) !== null }"
              @click="toggleEffectSelect(eff.id)"
            >
              <span class="effect-btn-name">{{ eff.name }}</span>
              <span v-if="getLevelUnlock(eff.id) !== null" class="level-badge">Lv.{{ getLevelUnlock(eff.id) }}</span>
            </button>
          </div>
        </section>

        <!-- 3️⃣ 지급 사유 & 실행 -->
        <section class="effect-panel">
          <div class="effect-panel-title">3️⃣ 실행</div>
          <div class="form-group" style="margin-bottom: 15px;">
            <label>지급 사유 <span class="label-hint">(회수 시 무시됨)</span></label>
            <select v-model="effectSource" class="form-select">
              <option value="EVENT">이벤트</option>
              <option value="BETA">베타테스터</option>
              <option value="SHOP">상점 구매</option>
              <option value="WELCOME">신규 가입 보너스</option>
            </select>
          </div>
          <div class="action-btn-row">
            <button @click="grantEffectBatch" :disabled="!canExecute || isGranting || isRevoking" class="btn-grant">
              {{ isGranting ? '지급 중...' : `${selectedUserIds.length}명 × ${selectedEffectCodes.length}개 지급` }}
            </button>
            <button @click="revokeEffectBatch" :disabled="!canExecute || isGranting || isRevoking" class="btn-revoke">
              {{ isRevoking ? '회수 중...' : `${selectedUserIds.length}명 × ${selectedEffectCodes.length}개 회수` }}
            </button>
          </div>
          <div v-if="grantMessage" class="grant-message" :class="{ success: grantMessage.includes('✅'), error: grantMessage.includes('❌') }">
            {{ grantMessage }}
          </div>
        </section>

      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-badge-container { height: 100%; width: 100%; max-width: 1400px; margin: 0 auto; display: flex; flex-direction: column; background: var(--bg-color); }

/* --- 헤더 및 필터 --- */
.admin-header { flex-shrink: 0; padding: 0 0 10px; }
.header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; padding: 0 5px; }
.header-main h2 { margin: 0; font-size: 1.15rem; font-weight: 850; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.header-btn-group { display: flex; gap: 6px; align-items: center; }
.btn-compact { height: 32px; padding: 0 12px; border-radius: 8px; font-weight: 800; font-size: 0.75rem; cursor: pointer; transition: all 0.2s; border: 1px solid transparent; white-space: nowrap; }
.btn-sync { background: var(--hover-bg); border-color: var(--border-color); color: var(--link-color); }
.btn-master-add { background: var(--link-color); color: white; }
.btn-danger { background: #fff5f5; border-color: #feb2b2; color: #e53e3e; }
.btn-danger:hover { background: #feb2b2; color: white; }
.btn-rule-add { background: #f0fff4; border-color: #9ae6b4; color: #38a169; }
.btn-rule-add:hover { background: #38a169; color: white; }
.btn-compact:disabled { opacity: 0.5; cursor: not-allowed; }

.search-filter-bar { margin-bottom: 15px; padding: 0 5px; }
.type-filter-chips { display: flex; gap: 8px; padding: 5px 0; overflow-x: auto; scrollbar-width: none; -ms-overflow-style: none; -webkit-overflow-scrolling: touch; }
.type-filter-chips::-webkit-scrollbar { display: none; }
.chip-btn { padding: 6px 14px; border-radius: 20px; border: 1px solid var(--border-color); background: var(--card-bg); font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; white-space: nowrap; flex-shrink: 0; }
.chip-btn.active { background: var(--link-color); color: white; border-color: var(--link-color); box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2); }

/* --- 인라인 폼 패널 --- */
.admin-form-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; margin: 0 5px 20px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); }
.panel-content { display: flex; flex-direction: column; gap: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }
.form-item, .form-group-v3 { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.form-item.full, .form-group-v3.full { grid-column: 1 / -1; }
.form-item.emoji-row { grid-column: 1 / -1; display: grid; grid-template-columns: 80px 1fr; gap: 12px; }
.inner-field { display: flex; flex-direction: column; gap: 6px; }

.form-item label, .form-group-v3 label { font-size: 0.72rem; font-weight: 850; color: var(--text-secondary); text-transform: uppercase; }
.form-item input, .form-item select, .form-group-v3 input, .form-group-v3 select, .form-group-v3 textarea { 
  width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); color: var(--text-primary); font-size: 0.9rem; font-weight: 600; outline: none; transition: all 0.2s; appearance: none;
}
.form-item select, .form-group-v3 select { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%234a5568' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 10px center; background-size: 14px; padding-right: 30px; }

.panel-footer { display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding-top: 15px; border-top: 1px solid var(--hover-bg); }
.panel-footer.justify-end { justify-content: flex-end; }
.footer-btn-group { display: flex; gap: 12px; }

.btn-primary { background: var(--link-color); color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 800; font-size: 0.85rem; cursor: pointer; }
.btn-secondary { background: var(--hover-bg); color: var(--text-secondary); border: 1px solid var(--border-color); padding: 9px 18px; border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; }

.inline-preview { margin-right: auto; font-size: 0.82rem; color: var(--text-secondary); }
.inline-preview b { color: #38a169; }

/* --- 메인 컨텐츠 --- */
.main-content-scrollable { flex: 1; min-height: 0; overflow-y: auto; padding: 10px 5px 40px; }
.table-wrapper { border: 1px solid var(--divider-color); border-radius: 12px; overflow: hidden; background: var(--card-bg); }
.admin-section { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 25px; margin-bottom: 35px; }
.mt-40 { margin-top: 50px; }

.section-header { margin-bottom: 25px; } /* 헤더와 아래 내용 사이 간격 확대 */
.section-header h3 { font-size: 1.15rem; font-weight: 850; margin: 0 0 6px; color: var(--text-primary); }
.section-desc { font-size: 0.85rem; color: var(--text-secondary); margin: 0; }
.admin-table th { background: var(--hover-bg); padding: 12px 15px; text-align: center; font-size: 0.8rem; font-weight: 800; color: var(--text-secondary); border-bottom: 1px solid var(--border-color); white-space: nowrap; }
.admin-table td { padding: 12px 15px; border-bottom: 1px solid var(--border-color); font-size: 0.85rem; color: var(--text-primary); vertical-align: middle; text-align: center; }

.col-lv { width: 70px; } .col-emoji { width: 60px; } .col-val { width: 90px; } .col-type { width: 140px; } .col-title { width: 100px; } .col-cat { width: 100px; } .col-preview { width: 220px; } .col-action { width: 110px; }

.type-badge-v3 { font-size: 0.7rem; font-weight: 800; padding: 3px 8px; border-radius: 6px; background: var(--bg-color); border: 1px solid var(--border-color); }
.preview-chip { background: rgba(56, 161, 105, 0.08); color: #38a169; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.82rem; border: 1px solid rgba(56, 161, 105, 0.2); }
code { background: var(--hover-bg); padding: 2px 6px; border-radius: 4px; font-family: monospace; font-weight: 700; color: var(--link-color); }

.actions-wrapper { display: flex; gap: 6px; justify-content: center; }
.btn-edit { height: 30px; border: 1px solid var(--link-color); color: var(--link-color); background: none; border-radius: 6px; font-size: 0.75rem; font-weight: 700; cursor: pointer; flex: 1; }
.btn-delete { height: 30px; border: 1px solid #ff4d4f; color: #ff4d4f; background: none; border-radius: 6px; font-size: 0.75rem; font-weight: 700; cursor: pointer; flex: 1; }

/* --- 미리보기 스타일 --- */
.tier-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }
.preview-item { background: var(--bg-color); padding: 15px; border-radius: 12px; border: 1px solid var(--border-color); display: flex; flex-direction: column; align-items: center; gap: 10px; }
.preview-label { font-size: 0.7rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; }

/* --- 특수 효과 샌드박스 아코디언 --- */
.effect-sandbox-accordion { 
  margin-bottom: 25px; 
  background: var(--hover-bg); 
  border-radius: 12px; 
  border: 1px solid var(--border-color); 
  overflow: hidden; 
}
.accordion-header { 
  padding: 12px 20px; 
  display: flex; 
  align-items: center; 
  gap: 15px; 
  cursor: pointer; 
  transition: background 0.2s; 
  user-select: none;
}
.accordion-header:hover { background: var(--border-color); }
.a-icon { font-size: 1.2rem; }
.a-text-group { flex: 1; display: flex; flex-direction: column; }
.a-title { font-size: 0.85rem; font-weight: 850; color: var(--text-primary); }
.a-subtitle { font-size: 0.72rem; color: var(--text-secondary); margin-top: 1px; }
.a-arrow { font-size: 0.7rem; color: var(--text-muted); opacity: 0.6; }

.accordion-body { padding: 12px 15px; border-top: 1px solid var(--border-color); background: var(--card-bg); }
.effect-chips { 
  display: grid; 
  grid-template-columns: repeat(2, 1fr); 
  gap: 6px; 
}
.chip-btn-v3 { 
  padding: 6px 10px; 
  border-radius: 6px; 
  border: 1px solid var(--border-color); 
  background: var(--card-bg); 
  font-size: 0.68rem; /* 글씨 작게 */
  font-weight: 700; 
  color: var(--text-primary); 
  cursor: pointer; 
  transition: all 0.2s;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 긴 글자 말줄임 */
  width: 100%; /* 열 맞춤을 위해 꽉 채움 */
}
.chip-btn-v3.active { background: #facc15; color: #000; border-color: #facc15; box-shadow: 0 4px 10px rgba(250, 204, 21, 0.3); }
.chip-btn-v3:hover:not(.active) { background: var(--divider-color); }

/* 애니메이션 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(-10px); }

.badge-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.empty-preview-state { 
  grid-column: 1 / -1; padding: 60px 20px; background: var(--hover-bg); border: 1px dashed var(--border-color); border-radius: 20px; text-align: center; color: var(--text-muted); 
}
.empty-icon { font-size: 2.5rem; display: block; margin-bottom: 12px; opacity: 0.5; }

/* --- 배지 카드 프리뷰 스타일 --- */
.badge-card-set { position: relative; background: var(--card-bg); border: 1.5px solid var(--t-color); border-radius: 20px; padding: 24px; display: flex; gap: 20px; transition: 0.3s; overflow: hidden; background-color: var(--t-bg) !important; }
.card-glass-reflection { position: absolute; inset: 0; background: linear-gradient(135deg, transparent 45%, rgba(255,255,255,0.1) 50%, transparent 55%); background-size: 250% 250%; animation: cardShine 6s infinite; pointer-events: none; z-index: 5; }
@keyframes cardShine { 0% { background-position: 250% 250%; } 15% { background-position: -150% -150%; } 100% { background-position: -150% -150%; } }

.badge-visual-set { width: 66px; height: 66px; border-radius: 18px; border: 1.5px solid var(--t-color); display: flex; align-items: center; justify-content: center; font-size: 2.2rem; flex-shrink: 0; }
.badge-info-set { flex: 1; min-width: 0; position: relative; z-index: 10; text-align: left; }
.badge-header-flex { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }
.badge-level-tag-set { font-size: 0.7rem; font-weight: 900; padding: 2px 10px; border-radius: 8px; border: 1px solid var(--t-color); color: var(--t-color); background: rgba(255,255,255,0.5); }
.preview-tag { font-size: 0.65rem; font-weight: 800; color: var(--text-muted); opacity: 0.6; }

.badge-name-set { font-size: 1.15rem; font-weight: 950; margin: 8px 0 4px; }
.badge-desc-set { font-size: 0.82rem; color: var(--text-secondary); line-height: 1.4; margin-bottom: 12px; }
.badge-tip-set { border: 1px dashed var(--t-color); padding: 8px 12px; border-radius: 10px; margin-bottom: 12px; font-size: 0.75rem; display: flex; gap: 8px; }

/* --- 티어 색상 변수 --- */
.tier-1 { --t-color: #94a3b8; --t-bg: rgba(148,163,184,0.05); }
.tier-5 { --t-color: #b08d57; --t-bg: rgba(176,141,87,0.08); }
:global(.dark-mode) .tier-5 { --t-color: #d4943a; --t-bg: rgba(212,148,58,0.12); }
.tier-10 { --t-color: #475569; --t-bg: rgba(71,85,105,0.08); }
.tier-30 { --t-color: #facc15; --t-bg: rgba(250,204,21,0.08); }
.tier-50 { --t-color: #10b981; --t-bg: rgba(16,185,129,0.08); }
.tier-70 { --t-color: #ff416c; --t-bg: rgba(225,29,72,0.06); }
.tier-90 { --t-color: #22d3ee; --t-bg: rgba(34,211,238,0.06); }
.tier-100 { --t-color: #facc15; --t-bg: rgba(0,0,0,0.95); }
/* tier-100 검은 배경 → 텍스트 흰색 */
.badge-level-tag-set.tier-100 { color: #fff !important; border-color: rgba(255,255,255,0.4) !important; background: rgba(255,255,255,0.1) !important; }
.badge-card-set.tier-100 .badge-desc-set { color: rgba(255,255,255,0.75); }
.badge-tip-set.tier-100 .tip-text-standard { color: rgba(255,255,255,0.85); }
.badge-footer-set.tier-100 .badge-metric-set { color: rgba(255,255,255,0.65); }
.badge-footer-set.tier-100 .badge-metric-set b { color: #fff; }
.badge-footer-set.tier-100 .badge-date-set { color: rgba(255,255,255,0.5); }
.badge-name-set.tier-100 { color: #fff; }

/* --- 모바일 대응 --- */
.mobile-only { display: none; }
@media (max-width: 992px) {
  .pc-only { display: none; } .mobile-only { display: block; }
  .header-main { flex-direction: column; align-items: stretch; gap: 12px; }
  .header-btn-group { width: 100%; display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .btn-compact { width: 100%; justify-content: center; }
  
  .admin-form-panel { padding: 15px; }
  .form-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .form-item.emoji-row { grid-template-columns: 65px 1fr; gap: 10px; }
  .panel-footer { justify-content: flex-end; gap: 8px; }
  .btn-primary, .btn-secondary { padding: 8px 15px; font-size: 0.82rem; }

  .mobile-rule-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; padding: 0 2px; }
  .rule-mobile-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 8px 6px; display: flex; flex-direction: column; gap: 6px; min-width: 0; aspect-ratio: 1 / 1.15; justify-content: space-between; }
  .card-header-flex { width: 100%; display: flex; align-items: center; gap: 6px; border-bottom: 1px solid var(--hover-bg); padding-bottom: 4px; }
  .card-emoji-box { font-size: 1.1rem; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; background: var(--hover-bg); border-radius: 6px; flex-shrink: 0; }
  .card-meta-right { display: flex; flex-direction: column; gap: 1px; min-width: 0; text-align: left; }
  .card-lv { font-size: 0.62rem; color: var(--link-color); font-weight: 900; }
  .card-val { font-size: 0.58rem; color: var(--text-muted); }
  .card-rank { font-size: 0.72rem; font-weight: 900; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .card-cat { font-size: 0.6rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  
  .card-actions { width: 100%; display: flex; gap: 3px; }
  .btn-edit-m { flex: 1; height: 24px; border-radius: 5px; font-weight: 800; font-size: 0.62rem; background: var(--hover-bg); color: var(--link-color); border: 1px solid var(--border-color); cursor: pointer; }
  .btn-delete-m { flex: 1; height: 24px; border-radius: 5px; font-weight: 800; font-size: 0.62rem; background: #fff5f5; color: #e53e3e; border: 1px solid #feb2b2; cursor: pointer; }

  .badge-preview-grid { 
    grid-template-columns: 1fr; /* 모바일에서 1열로 변경하여 카드 짤림 및 늘어짐 방지 */
    gap: 15px; 
  }
  .badge-card-set { 
    padding: 16px; 
    gap: 15px; 
    min-height: auto; 
    border-radius: 16px; 
    flex-direction: row; /* 가로 레이어 유지 */
    align-items: center;
  }
  .badge-visual-set { width: 54px; height: 54px; font-size: 1.8rem; border-radius: 12px; }
  .badge-name-set { font-size: 1rem; margin: 4px 0; }
  .badge-desc-set { font-size: 0.75rem; margin-bottom: 8px; line-height: 1.4; }
  .badge-tip-set { padding: 6px 10px; border-radius: 8px; margin-bottom: 8px; font-size: 0.7rem; }
  .badge-footer-set { font-size: 0.65rem; }

  .tier-preview-grid {
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 10px;
    display: grid !important;
  }
  .preview-item { padding: 10px; border-radius: 10px; min-width: 0; }
  .preview-label { font-size: 0.65rem; }
}

.slide-fade-enter-active { transition: all 0.3s ease-out; }
.slide-fade-leave-active { transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1); }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateY(-20px); opacity: 0; }

.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

/* --- [시니어 조치] 탭 네비게이션 --- */
.admin-tabs { display: flex; gap: 8px; padding: 0 5px 12px; border-bottom: 1px solid var(--border-color); }
.tab-btn { background: none; border: none; padding: 8px 16px; border-bottom: 2px solid transparent; font-weight: 800; font-size: 0.95rem; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; }
.tab-btn.active { color: var(--link-color); border-bottom-color: var(--link-color); }
.tab-btn:hover:not(.active) { color: var(--text-primary); }

/* --- [시니어 조치] 탭 내부 헤더 --- */
.tab-header { padding: 15px 5px 20px; display: flex; flex-direction: column; gap: 12px; background: var(--card-bg); border-bottom: 1px solid var(--border-color); margin: 0 -5px 20px; padding-left: 5px; padding-right: 5px; }
.tab-controls { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; justify-content: flex-end; }
.tab-controls .btn-compact { min-width: fit-content; }
.type-filter-chips { display: flex; gap: 6px; overflow-x: auto; padding: 5px 0; scrollbar-width: thin; }
.type-filter-chips::-webkit-scrollbar { height: 4px; }
.type-filter-chips::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }
.chip-btn { background: var(--hover-bg); border: 1px solid var(--border-color); color: var(--text-secondary); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.chip-btn:hover { border-color: var(--link-color); color: var(--link-color); }
.chip-btn.active { background: var(--link-color); color: white; border-color: var(--link-color); }

/* --- 효과 관리 탭 레이아웃 --- */
.effect-layout { display: flex; flex-direction: column; gap: 20px; padding-bottom: 40px; }
.effect-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; }
.effect-panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.effect-panel-title { font-size: 0.95rem; font-weight: 900; color: var(--text-primary); display: block; margin-bottom: 14px; }
.selected-count-badge { font-size: 0.78rem; font-weight: 800; padding: 3px 10px; border-radius: 20px; background: var(--link-color); color: white; }
.btn-select-all { height: 30px; padding: 0 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--hover-bg); color: var(--text-secondary); font-size: 0.75rem; font-weight: 800; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-select-all:hover:not(:disabled) { border-color: var(--link-color); color: var(--link-color); }
.btn-select-all:disabled { opacity: 0.5; cursor: not-allowed; }
.search-input { width: 100%; padding: 11px 14px; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); color: var(--text-primary); font-size: 0.9rem; outline: none; transition: border 0.2s; box-sizing: border-box; margin-bottom: 12px; }
.search-input:focus { border-color: var(--link-color); }
.search-results { max-height: 280px; overflow-y: auto; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); margin-bottom: 16px; }
.select-all-row { padding: 10px 14px; display: flex; align-items: center; gap: 10px; cursor: pointer; background: var(--hover-bg); border-bottom: 2px solid var(--border-color); transition: background 0.2s; }
.select-all-row:hover { background: var(--divider-color); }
.select-all-label { font-size: 0.82rem; font-weight: 900; color: var(--text-primary); }
.user-item { padding: 10px 14px; display: flex; align-items: flex-start; gap: 10px; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid var(--border-color); }
.user-item:last-child { border-bottom: none; }
.user-item:hover { background: var(--hover-bg); }
.user-item.selected { background: rgba(56, 161, 105, 0.07); }
.user-checkbox { width: 16px; height: 16px; cursor: pointer; margin-top: 3px; flex-shrink: 0; }
.user-info { flex: 1; min-width: 0; }
.user-name { font-weight: 800; font-size: 0.88rem; color: var(--text-primary); }
.user-meta { font-size: 0.76rem; color: var(--text-secondary); margin-bottom: 4px; }
.user-effects { display: flex; flex-wrap: wrap; gap: 3px; }
.effect-chip-small { font-size: 0.65rem; font-weight: 700; padding: 2px 6px; border-radius: 4px; background: rgba(52, 152, 219, 0.1); color: var(--link-color); border: 1px solid rgba(52, 152, 219, 0.25); white-space: nowrap; transition: all 0.2s; }
.effect-chip-small.matched { background: rgba(229, 62, 62, 0.12); color: #e53e3e; border-color: rgba(229, 62, 62, 0.4); font-weight: 900; }
.user-no-effect { font-size: 0.65rem; color: var(--text-muted); opacity: 0.6; }
.empty-search { padding: 20px; text-align: center; color: var(--text-secondary); font-size: 0.85rem; }
.selected-panel { margin-top: 4px; background: var(--hover-bg); border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; }
.selected-panel-header { padding: 10px 14px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-color); font-size: 0.82rem; color: var(--text-secondary); }
.selected-panel-header b { color: var(--text-primary); }
.btn-clear-all { background: none; border: 1px solid var(--border-color); color: var(--text-secondary); font-size: 0.72rem; font-weight: 700; padding: 3px 8px; border-radius: 6px; cursor: pointer; }
.btn-clear-all:hover { border-color: #e53e3e; color: #e53e3e; }
.selected-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px; padding: 10px; }
.user-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 10px; }
.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }
.card-name { font-size: 0.82rem; font-weight: 800; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-remove { background: none; border: none; color: var(--text-muted); font-size: 0.8rem; cursor: pointer; padding: 0; flex-shrink: 0; }
.card-remove:hover { color: #e53e3e; }
.card-effects { display: flex; flex-wrap: wrap; gap: 3px; }
.selected-summary { padding: 14px; font-size: 0.82rem; color: var(--text-secondary); text-align: center; }
.effects-toggle-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 6px; }
.effect-toggle-btn { padding: 8px 10px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-color); color: var(--text-secondary); font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: all 0.18s; text-align: left; display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.effect-toggle-btn:hover { border-color: var(--link-color); color: var(--link-color); }
.effect-toggle-btn.active { background: rgba(52, 152, 219, 0.12); border-color: var(--link-color); color: var(--link-color); font-weight: 900; }
.effect-toggle-btn.is-level { border-style: dashed; }
.effect-btn-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.level-badge { flex-shrink: 0; font-size: 0.6rem; font-weight: 900; padding: 1px 5px; border-radius: 4px; background: rgba(251, 191, 36, 0.15); color: #d97706; border: 1px solid rgba(251, 191, 36, 0.4); }
.action-btn-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 10px; }
.btn-grant { width: 100%; padding: 12px 16px; background: var(--link-color); color: white; border: none; border-radius: 10px; font-weight: 900; font-size: 0.88rem; cursor: pointer; transition: all 0.2s; }
.btn-grant:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3); }
.btn-grant:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-revoke { width: 100%; padding: 12px 16px; background: #e53e3e; color: white; border: none; border-radius: 10px; font-weight: 900; font-size: 0.88rem; cursor: pointer; transition: all 0.2s; }
.btn-revoke:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(229, 62, 62, 0.3); }
.btn-revoke:disabled { opacity: 0.5; cursor: not-allowed; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 0.8rem; font-weight: 900; color: var(--text-secondary); text-transform: uppercase; }
.label-hint { font-size: 0.72rem; font-weight: 600; color: var(--text-muted); text-transform: none; opacity: 0.75; }
.form-select { width: 100%; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); color: var(--text-primary); font-size: 0.9rem; font-weight: 600; outline: none; transition: border 0.2s; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%234a5568' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 10px center; background-size: 14px; padding-right: 30px; }
.form-select:focus { border-color: var(--link-color); }
.grant-message { margin-top: 12px; padding: 11px 14px; border-radius: 10px; font-weight: 800; text-align: center; font-size: 0.88rem; }
.grant-message.success { background: rgba(56, 161, 105, 0.1); color: #38a169; border: 1px solid rgba(56, 161, 105, 0.3); }
.grant-message.error { background: rgba(255, 77, 79, 0.1); color: #ff4d4f; border: 1px solid rgba(255, 77, 79, 0.3); }

@media (max-width: 768px) {
  .admin-tabs { flex-wrap: wrap; }
  .action-btn-row { grid-template-columns: 1fr; }
  .effects-toggle-grid { grid-template-columns: repeat(2, 1fr); }
  .effect-toggle-btn { font-size: 0.62rem; padding: 6px 6px; gap: 3px; }
  .effect-toggle-btn .effect-btn-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; }
  .effect-toggle-btn .level-badge { flex-shrink: 0; font-size: 0.55rem; padding: 1px 3px; }
  .selected-cards { grid-template-columns: repeat(2, 1fr); }
  .effect-chips { max-height: 200px; overflow-y: auto; scrollbar-width: thin; }
  .effect-chips::-webkit-scrollbar { width: 4px; }
  .effect-chips::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

  .tab-header { padding: 12px 5px 15px; gap: 10px; }
  .tab-controls { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; }
  .tab-controls .btn-compact { padding: 8px 10px; font-size: 0.75rem; }
  .type-filter-chips { gap: 4px; }
  .chip-btn { padding: 5px 10px; font-size: 0.75rem; }
}
</style>
