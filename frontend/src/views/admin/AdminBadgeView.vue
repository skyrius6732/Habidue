<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import axios from '@/plugins/axios'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const rules = ref([])
const masters = ref([])
const loading = ref(true)
const isSyncing = ref(false)
const selectedType = ref('ALL')
const activeTab = ref('badges') // 'badges' 또는 'effects'
const isEffectAccordionOpen = ref(false) // [시니어 조치] 이펙트 아코디언 상태

// 사용자 데이터 (미리보기용)
const userProfile = ref(null)
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
const selectedUserIds = ref([])
const selectedEffectCode = ref('')
const effectSource = ref('EVENT')
const isGranting = ref(false)
const grantMessage = ref('')

// [시니어 조치] 특수 효과 리스트
const specialEffects = [
  { id: null, name: '효과 없음' },
  { id: 'PIONEER_WINGS', name: '🕊️ 화이트 윙 (White)' },
  { id: 'BRONZE_WINGS', name: '🥉 브론즈 윙 (Bronze)' },
  { id: 'SILVER_WINGS', name: '🥈 실버 윙 (Silver)' },
  { id: 'GOLD_WINGS', name: '👑 골드 윙 (Gold)' },
  { id: 'MAGIC_BUBBLES', name: '🫧 신비로운 버블 (Bubble)' },
  { id: 'STARRY_NIGHT', name: '✨ 반짝이는 별무리 (Starry)' },
  { id: 'THUNDER_BLUE', name: '⚡ 푸른 번개 (Thunder)' },
  { id: 'AURORA_FLAME', name: '🔥 오로라 화염 (Flame)' },
  { id: 'ICE_FROST', name: '❄️ 얼음 결정 (Frost)' },
  { id: 'SAKURA_BLOOM', name: '🌸 벚꽃 (Sakura)' },
  { id: 'ROSES_BLOOM', name: '🌹 장미 (Roses)' },
  { id: 'SHADOW_DEMON', name: '👹 섀도우 데몬 (Shadow)' },
  { id: 'NEON_SIGN', name: '🌟 네온 사인 (Neon)' },
  { id: 'PIXEL_GLITCH', name: '💥 픽셀 글리치 (Glitch)' },
  { id: 'VOID_RIFT', name: '🌀 보이드 리프트 (Void)' },
  { id: 'LOVE_HEART', name: '💕 두근두근 하트 (Heart)' },
  { id: 'RAINBOW_WAVE', name: '🌈 무지개 (Rainbow)' },
  { id: 'SHOOTING_STAR', name: '🌠 별똥별 (Shooting Star)' },
  { id: 'BOMB', name: '🔫 레이저 (Laser)' },
  { id: 'CROWN', name: '👑 왕관 (Crown)' },
  { id: 'SWORDS_CROSS', name: '⚔️ 칼 교차 (Swords)' },
  { id: 'FLOWER_CROWN', name: '🌸 화관 (Flower Crown)' },
  { id: 'STAR_TIARA', name: '🌟 별 티아라 (Star Tiara)' },
  { id: 'FLOWER_RAIN', name: '🌺 꽃비 (Flower Rain)' },
  { id: 'SCAN_LINE', name: '📺 스캔라인 (Scan Line)' },
  { id: 'CHROMATIC_ABERRATION', name: '🌈 색상 분리 (Chromatic)' },
  { id: 'ECHO_TRAIL', name: '👻 잔상 (Echo Trail)' },
  { id: 'CORRUPTED_TEXT', name: '🔲 손상된 텍스트 (Corrupted)' },
  { id: 'GLITCH_SHIFT', name: '⚡ 글리치 (Glitch Shift)' },
  { id: 'LIQUID_DISTORT', name: '💧 액체 (Liquid Distort)' },
  { id: 'HALO', name: '✨ 후광 (Halo)' },
  { id: 'LEAVES', name: '🍂 낙엽 (Leaves)' },
  { id: 'BUTTERFLIES', name: '🦋 나비 (Butterflies)' },
  { id: 'ORBS', name: '🔮 구체들 (Orbs)' },
  { id: 'SCALE_COLLAPSE', name: '⬇️ 축소 & 사라지기 (Collapse)' }
]

const updateEffect = async (effectId) => {
  if (!userProfile.value) return
  try {
    await axios.patch(`/api/users/${userProfile.value.id}/effect`, null, { params: { effectCode: effectId } })
    userProfile.value.equippedEffect = effectId
    // alert 제거: 즉시 반영되어 시각적 피드백 제공
  } catch (e) {
    console.error('이펙트 적용 실패', e)
  }
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

// [시니어 조치] 사용자 선택 토글
const toggleUserSelect = (userId) => {
  const idx = selectedUserIds.value.indexOf(userId)
  if (idx > -1) {
    selectedUserIds.value.splice(idx, 1)
  } else {
    selectedUserIds.value.push(userId)
  }
}

// [시니어 조치] 이펙트 지급 (단일 또는 일괄)
const grantEffectBatch = async () => {
  if (selectedUserIds.value.length === 0 || !selectedEffectCode.value) {
    alert('사용자와 이펙트를 선택해주세요.')
    return
  }

  if (!confirm(`${selectedUserIds.value.length}명에게 이펙트를 지급하시겠습니까?`)) return

  isGranting.value = true
  grantMessage.value = ''
  try {
    // 1명 선택: 단일 지급 엔드포인트 사용
    if (selectedUserIds.value.length === 1) {
      const userId = selectedUserIds.value[0]
      const res = await axios.post(`/api/users/admin/${userId}/grant-effect`, null, {
        params: {
          effectCode: selectedEffectCode.value,
          source: effectSource.value
        }
      })
      const data = res.data.data
      grantMessage.value = `✅ ${data.message}`
    } else {
      // 여러 명 선택: 일괄 지급 엔드포인트 사용
      const params = new URLSearchParams()
      selectedUserIds.value.forEach(id => params.append('userIds', id))
      params.append('effectCode', selectedEffectCode.value)
      params.append('source', effectSource.value)

      const res = await axios.post('/api/users/admin/batch/grant-effect', null, {
        params: params
      })
      const data = res.data.data
      grantMessage.value = `✅ ${data.message} (총 요청: ${data.totalRequested}명)`
    }

    selectedUserIds.value = []
    selectedEffectCode.value = ''
    searchedUsers.value = []
    searchUserInput.value = ''
  } catch (e) {
    grantMessage.value = `❌ 지급 실패: ${e.response?.data?.message || e.message}`
  } finally {
    isGranting.value = false
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
          :class="{ active: activeTab === 'badges' }"
          @click="activeTab = 'badges'"
        >
          🎖️ 배지 관리
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'effects' }"
          @click="activeTab = 'effects'"
        >
          ✨ 효과 관리
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
                  class="chip-btn-v3" :class="{ active: userProfile.equippedEffect === eff.id }"
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
              :equipped-effect="userProfile.equippedEffect"
              tooltip-direction="top"
              :is-ellipsis="(userProfile?.nickname || userProfile?.username || '').length >= 6"
            />          </div>
        </div>
      </section>

      <!-- 배지 카드 미리보기 (실제 규칙 기반) -->
      <section class="admin-section preview-section">
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

    <!-- [시니어 조치] 효과 관리 탭 -->
    <div v-if="activeTab === 'effects'" class="main-content-scrollable scrollable">
      <section class="admin-section effects-section">
        <div class="section-header">
          <h3>✨ 사용자 이펙트 지급</h3>
          <p class="section-desc">사용자에게 이펙트를 지급합니다 (이벤트, 베타테스터, 구매 등)</p>
        </div>

        <div class="effect-grant-container">
          <!-- 사용자 검색 -->
          <div class="grant-section">
            <div class="section-title">1️⃣ 사용자 선택</div>
            <div class="search-input-wrapper">
              <input
                v-model="searchUserInput"
                @input="searchUsers"
                placeholder="사용자 이름, 이메일, ID로 검색"
                class="search-input"
              />
            </div>

            <!-- 검색 결과 -->
            <div v-if="searchedUsers.length > 0" class="search-results">
              <div
                v-for="user in searchedUsers"
                :key="user.id"
                class="user-item"
                :class="{ selected: selectedUserIds.includes(user.id) }"
                @click="toggleUserSelect(user.id)"
              >
                <input
                  type="checkbox"
                  :checked="selectedUserIds.includes(user.id)"
                  readonly
                  class="user-checkbox"
                />
                <div class="user-info">
                  <div class="user-name">{{ user.nickname || user.username }}</div>
                  <div class="user-meta">{{ user.email }} (Lv.{{ user.level }})</div>
                </div>
              </div>
            </div>

            <div v-if="searchUserInput && searchedUsers.length === 0" class="empty-search">
              검색 결과가 없습니다.
            </div>

            <!-- 선택된 사용자 목록 -->
            <div v-if="selectedUserIds.length > 0" class="selected-users">
              <div class="selected-header">선택된 사용자 ({{ selectedUserIds.length }}명)</div>
              <div class="selected-chips">
                <div
                  v-for="userId in selectedUserIds"
                  :key="userId"
                  class="selected-chip"
                >
                  {{ searchedUsers.find(u => u.id === userId)?.nickname || `User ${userId}` }}
                  <button @click="toggleUserSelect(userId)" class="chip-close">✕</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 이펙트 선택 -->
          <div class="grant-section">
            <div class="section-title">2️⃣ 이펙트 선택</div>
            <div class="form-row">
              <div class="form-group">
                <label>지급할 이펙트</label>
                <select v-model="selectedEffectCode" class="form-select">
                  <option value="">-- 선택해주세요 --</option>
                  <option v-for="eff in specialEffects" :key="eff.id" :value="eff.id">
                    {{ eff.name }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>지급 사유</label>
                <select v-model="effectSource" class="form-select">
                  <option value="EVENT">이벤트</option>
                  <option value="BETA">베타테스터</option>
                  <option value="SHOP">상점 구매</option>
                  <option value="WELCOME">신규 가입 보너스</option>
                </select>
              </div>
            </div>
          </div>

          <!-- 지급 버튼 -->
          <div class="grant-section">
            <div class="section-title">3️⃣ 일괄 지급 실행</div>
            <button
              @click="grantEffectBatch"
              :disabled="selectedUserIds.length === 0 || !selectedEffectCode || isGranting"
              class="btn-grant"
            >
              {{ isGranting ? '지급 중...' : `${selectedUserIds.length}명에게 지급하기` }}
            </button>

            <!-- 지급 결과 메시지 -->
            <div v-if="grantMessage" class="grant-message" :class="{ success: grantMessage.includes('✅'), error: grantMessage.includes('❌') }">
              {{ grantMessage }}
            </div>
          </div>
        </div>
      </section>
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

/* --- [시니어 조치] 효과 관리 섹션 --- */
.effects-section { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 25px; }
.effect-grant-container { display: flex; flex-direction: column; gap: 30px; }
.grant-section { background: var(--hover-bg); padding: 20px; border-radius: 12px; border: 1px solid var(--border-color); }
.section-title { font-size: 0.95rem; font-weight: 900; color: var(--text-primary); margin-bottom: 15px; }

.search-input-wrapper { margin-bottom: 15px; }
.search-input { width: 100%; padding: 12px 15px; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); color: var(--text-primary); font-size: 0.9rem; outline: none; transition: border 0.2s; }
.search-input:focus { border-color: var(--link-color); }

.search-results { max-height: 300px; overflow-y: auto; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); margin-bottom: 15px; }
.user-item { padding: 12px 15px; display: flex; align-items: center; gap: 12px; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid var(--border-color); }
.user-item:hover { background: var(--hover-bg); }
.user-item.selected { background: rgba(56, 161, 105, 0.08); }
.user-checkbox { width: 18px; height: 18px; cursor: pointer; }
.user-info { flex: 1; min-width: 0; }
.user-name { font-weight: 800; font-size: 0.9rem; color: var(--text-primary); }
.user-meta { font-size: 0.8rem; color: var(--text-secondary); }

.empty-search { padding: 20px; text-align: center; color: var(--text-secondary); }

.selected-users { margin-top: 15px; padding: 12px; background: var(--bg-color); border-radius: 10px; border: 1px solid var(--border-color); }
.selected-header { font-size: 0.85rem; font-weight: 800; color: var(--text-secondary); margin-bottom: 10px; }
.selected-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.selected-chip { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; background: var(--link-color); color: white; border-radius: 6px; font-size: 0.85rem; font-weight: 700; }
.chip-close { background: none; border: none; color: white; font-size: 1rem; cursor: pointer; padding: 0; }

.form-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 0.8rem; font-weight: 900; color: var(--text-secondary); text-transform: uppercase; }
.form-select { width: 100%; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: 10px; background: var(--bg-color); color: var(--text-primary); font-size: 0.9rem; font-weight: 600; outline: none; transition: border 0.2s; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%234a5568' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 10px center; background-size: 14px; padding-right: 30px; }
.form-select:focus { border-color: var(--link-color); }

.btn-grant { width: 100%; padding: 12px 20px; background: var(--link-color); color: white; border: none; border-radius: 10px; font-weight: 900; font-size: 0.95rem; cursor: pointer; transition: all 0.2s; }
.btn-grant:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(56, 161, 105, 0.3); }
.btn-grant:disabled { opacity: 0.5; cursor: not-allowed; }

.grant-message { margin-top: 15px; padding: 12px 15px; border-radius: 10px; font-weight: 800; text-align: center; }
.grant-message.success { background: rgba(56, 161, 105, 0.1); color: #38a169; border: 1px solid rgba(56, 161, 105, 0.3); }
.grant-message.error { background: rgba(255, 77, 79, 0.1); color: #ff4d4f; border: 1px solid rgba(255, 77, 79, 0.3); }

@media (max-width: 768px) {
  .form-row { grid-template-columns: 1fr; }
  .admin-tabs { flex-wrap: wrap; }
  .effect-grant-container { gap: 20px; }

  .tab-header { padding: 12px 5px 15px; gap: 10px; }
  .tab-controls { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; }
  .tab-controls .btn-compact { padding: 8px 10px; font-size: 0.75rem; }
  .type-filter-chips { gap: 4px; }
  .chip-btn { padding: 5px 10px; font-size: 0.75rem; }
}
</style>
