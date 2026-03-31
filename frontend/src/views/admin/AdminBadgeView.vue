<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import axios from '@/plugins/axios'
import AnimatedNickname from '@/components/AnimatedNickname.vue'

const rules = ref([])
const masters = ref([])
const loading = ref(true)
const isSyncing = ref(false)
const selectedType = ref('ALL')

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
      </div>

      <div class="search-filter-bar">
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

      <!-- 인라인 입력 패널 -->
      <Transition name="slide-fade">
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

    <div class="main-content-scrollable scrollable">
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
        <div class="tier-preview-grid">
          <div v-for="lv in previewLevels" :key="lv" class="preview-item">
            <span class="preview-label">Lv.{{ lv }}</span>
            <AnimatedNickname 
              :nickname="userProfile.nickname || userProfile.username" 
              :level="lv" 
              :exp="lv * lv * 50 - 1"
              :badges="activityData?.badges"
              :show-effects="true"
              tooltip-direction="top"
            />
          </div>
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
            <div v-if="testBadge.designTier >= 30" class="card-glass-reflection"></div>
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
.admin-section { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 25px; margin-bottom: 30px; }
.mt-40 { margin-top: 40px; }

.admin-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
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

  .badge-preview-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .badge-card-set { padding: 14px 12px; gap: 12px; }
  .badge-visual-set { width: 48px; height: 48px; font-size: 1.6rem; flex-shrink: 0; }
  .tier-preview-grid { grid-template-columns: repeat(2, 1fr) !important; gap: 10px; }
}

.slide-fade-enter-active { transition: all 0.3s ease-out; }
.slide-fade-leave-active { transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1); }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateY(-20px); opacity: 0; }

.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }
</style>
