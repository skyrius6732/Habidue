<template>
  <div class="admin-simulation-container">
    <!-- 1행: 사용자 설정 (상단 고정) -->
    <div class="header-full-row">
      <div class="header-main-v36">
        <div class="title-bar-v36">
          <h2>🔬 Reward Lab <span class="beta-tag">v3.6.3</span></h2>
          <div class="global-actions">
            <button @click="loadAllData" class="btn-refresh-all">🔄 새로고침</button>
            <button @click="resetSimulation" class="btn-reset-all">🧹 전체 초기화</button>
          </div>
        </div>
        <div class="user-selector-v36">
          <div v-for="type in ['subject', 'actor']" :key="type" 
               class="u-card-v36" :class="{ 'is-acting': activePerformer === type }"
               @click="activePerformer = type">
            <div class="u-head">
              <label class="u-radio"><input type="radio" v-model="activePerformer" :value="type" @click.stop /> 행동 주체</label>
              <span :class="['u-badge', type]">{{ type.toUpperCase() }}</span>
            </div>
            <div v-if="!(type === 'subject' ? subjectUser : actorUser)" class="u-input-box" @click.stop>
              <input v-model="userInputs[type]" placeholder="유저 ID" class="mini-input" @keyup.enter="fetchUser(type)" />
              <button @click="fetchUser(type)" class="mini-btn">로드</button>
            </div>
            <div v-else class="u-meta-v36">
              <span class="u-name">👤 {{ (type === 'subject' ? subjectUser : actorUser).nickname }}</span>
              <button @click.stop="clearUser(type)" class="u-clear">×</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 시뮬레이션 본문 -->
    <div v-if="subjectUser && actorUser" class="simulation-view-grid">
      <!-- 2행: 트래킹 및 조작 (높이 고정) -->
      <div class="middle-section">
        <div class="tracking-container">
          <div class="tracking-header">
            <h3>📌 타겟 트래킹 
              <span class="post-like-icon" v-if="lastPostId" @click="runLikeSim('POST')">
                {{ postDetail?.liked ? '❤️' : '🤍' }}
              </span>
            </h3>
            <span class="current-post-id" v-if="lastPostId">Post #{{ lastPostId }}</span>
          </div>
          
          <div class="tracking-columns-fixed">
            <!-- 💬 댓글 목록 -->
            <div class="track-col-v36">
              <div class="track-title-v36">💬 댓글 (ACTIVE)</div>
              <div class="track-list-scroll-v36">
                <div v-for="c in filteredComments" :key="c.id" 
                     class="track-item-v36" :class="{ 'selected': expandedCommentId == c.id }"
                     @click="selectComment(c)">
                  <div class="t-item-head">
                    <span class="t-id">#{{ c.id }}</span>
                    <span class="t-like-icon" @click.stop="targetAndLike('COMMENT', c.id)">{{ c.liked ? '❤️' : '🤍' }}</span>
                    <span class="t-author">{{ c.authorName }}</span>
                  </div>
                  <p class="t-text">{{ c.content }}</p>
                </div>
                <div v-if="filteredComments.length === 0" class="track-empty">댓글 없음</div>
              </div>
            </div>
            
            <!-- ↪️ 답글 목록 -->
            <div class="track-col-v36">
              <div class="track-title-v36">↪️ 답글 (ACTIVE)</div>
              <div class="track-list-scroll-v36">
                <div v-for="r in filteredReplies" :key="r.id" 
                     class="track-item-v36" :class="{ 'selected': lastCommentId == r.id && expandedCommentId != r.id }"
                     @click="lastCommentId = r.id">
                  <div class="t-item-head">
                    <span class="t-id">#{{ r.id }}</span>
                    <span class="t-like-icon" @click.stop="targetAndLike('COMMENT', r.id)">{{ r.liked ? '❤️' : '🤍' }}</span>
                    <span class="t-author">{{ r.authorName }}</span>
                  </div>
                  <p class="t-text">{{ r.content }}</p>
                </div>
                <div v-if="filteredReplies.length === 0" class="track-empty">답글 없음</div>
              </div>
            </div>
          </div>
        </div>

        <div class="action-container-v36">
          <div class="active-performer-tag-v36">📢 <b>{{ activePerformerName }}</b> 액션 수행 중</div>
          <div class="action-grid-v36">
            <div class="action-group-v36">
              <h4>✍️ 게시글</h4>
              <button @click="runPostSim('GENERAL', 'FREE', false)" :disabled="activePerformer !== 'subject'" class="btn-v36-fixed">일반글 작성</button>
              <button @click="runPostSim('NOTICE', 'ALL', false)" :disabled="activePerformer !== 'subject'" class="btn-v36-fixed blue">지식글 작성</button>
              <button @click="runPostSim('REVIEW', 'SUCCESS_STORY', true)" :disabled="activePerformer !== 'subject'" class="btn-v36-fixed orange">리뷰+사진</button>
            </div>
            <div class="action-group-v36">
              <h4>💬 소통</h4>
              <button @click="runCommentSim(false)" :disabled="!lastPostId" class="btn-v36-fixed">댓글 달기</button>
              <button @click="runCommentSim(true)" :disabled="!expandedCommentId" class="btn-v36-fixed">답글 달기</button>
              <button @click="runLikeSim('POST')" :disabled="!lastPostId" class="btn-v36-fixed red">글 좋아요 ❤️</button>
              <button @click="runLikeSim('COMMENT')" :disabled="!lastCommentId" class="btn-v36-fixed red">댓글 좋아요 ❤️</button>
            </div>
            <div class="action-group-v36 danger">
              <h4>🗑️ 삭제</h4>
              <button @click="runDeleteSim('post')" :disabled="!lastPostId" class="btn-v36-fixed outline-red">글 삭제</button>
              <button @click="runDeleteSim('comment')" :disabled="!lastCommentId" class="btn-v36-fixed outline-red">댓글 삭제</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 3행: 지표 리포트 (Dual Layout) -->
      <div class="dashboard-section-v36">
        <div v-for="type in ['subject', 'actor']" :key="type" class="dashboard-card-v36">
          <div class="db-head">
            <h3>{{ type === 'subject' ? '🎯 Subject' : '🎭 Actor' }}: {{ (type === 'subject' ? subjectUser : actorUser).nickname }}</h3>
            <span class="db-last-act">{{ type === 'subject' ? lastActionSubject : lastActionActor }}</span>
          </div>
          <div class="db-table-scroll-v36">
            <table class="db-table-v36">
              <thead><tr><th>지표</th><th>시작</th><th>전</th><th>후</th><th>증감</th><th>누적</th></tr></thead>
              <tbody>
                <tr v-for="m in metrics" :key="m.key" :class="{ 'row-changed': getDiff(type, m.key, 'initial') !== 0, 'row-divider': m.isGroupEnd }">
                  <td class="td-label">{{ m.label }}</td>
                  <td class="td-init">{{ formatVal(m.key, stats[`${type}Initial`]) }}</td>
                  <td class="td-prev">{{ formatVal(m.key, stats[`${type}Before`]) }}</td>
                  <td class="td-after">{{ formatVal(m.key, stats[`${type}After`]) }}</td>
                  <td :class="['td-diff', getDiffClass(type, m.key, 'last')]">{{ formatDiff(type, m.key, 'last') }}</td>
                  <td :class="['td-total', getDiffClass(type, m.key, 'initial')]">{{ formatDiff(type, m.key, 'initial') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import axios from '@/plugins/axios'

const userInputs = reactive({ subject: '', actor: '' })
const subjectUser = ref(null)
const actorUser = ref(null)
const activePerformer = ref('subject')

const lastPostId = ref('')
const lastCommentId = ref('')
const expandedCommentId = ref('')
const commentList = ref([])
const currentReplies = ref([])
const postDetail = ref(null)
const lastActionSubject = ref('대기중')
const lastActionActor = ref('대기중')

const stats = reactive({
  subjectInitial: null, subjectBefore: null, subjectAfter: null,
  actorInitial: null, actorBefore: null, actorAfter: null
})

const filteredComments = computed(() => commentList.value.filter(c => c.status === 'ACTIVE'))
const filteredReplies = computed(() => currentReplies.value.filter(r => r.status === 'ACTIVE'))
const activePerformerName = computed(() => activePerformer.value === 'subject' ? subjectUser.value?.nickname : actorUser.value?.nickname)
const performerId = computed(() => activePerformer.value === 'subject' ? subjectUser.value?.userId : actorUser.value?.userId)

const metrics = [
  { key: 'karma', label: '⚖️ 신뢰 점수' },
  { key: 'level', label: '⭐ 레벨' },
  { key: 'totalExp', label: '🎖️ 종합 경험치' },
  { key: 'knowledgeExp', label: '💡 지식인 EXP' },
  { key: 'reviewExp', label: '✍️ 리뷰왕 EXP' },
  { key: 'sincerityExp', label: '🔥 열정파 EXP', isGroupEnd: true },
  { key: 'totalPostCount', label: '📝 총 게시글' },
  { key: 'totalCommentCount', label: '💬 총 댓글' },
  { key: 'postLikeReceivedCount', label: '❤️ 받은 글좋아요' },
  { key: 'commentLikeReceivedCount', label: '💬 받은 댓글좋아요' },
  { key: 'totalViewReceivedCount', label: '👁️ 수신 조회수' }
]

const fetchUser = async (type) => {
  const id = userInputs[type]
  if (!id) return
  try {
    const res = await axios.get(`/api/admin/simulation/user/${id}`)
    if (type === 'subject') {
      subjectUser.value = res.data; stats.subjectInitial = JSON.parse(JSON.stringify(res.data)); stats.subjectBefore = res.data; stats.subjectAfter = res.data
    } else {
      actorUser.value = res.data; stats.actorInitial = JSON.parse(JSON.stringify(res.data)); stats.actorBefore = res.data; stats.actorAfter = res.data
    }
  } catch (e) { alert('로드 실패') }
}

const loadPostInfo = async () => {
  if (!lastPostId.value || !performerId.value) return
  try {
    const res = await axios.get(`/api/admin/simulation/posts/${lastPostId.value}`, { params: { performerId: performerId.value } })
    postDetail.value = res.data
  } catch (e) {}
}

const loadComments = async () => {
  if (!lastPostId.value || !performerId.value) return
  try {
    const res = await axios.get(`/api/admin/simulation/posts/${lastPostId.value}/comments`, { params: { performerId: performerId.value } })
    commentList.value = res.data
    if (expandedCommentId.value) {
      const parent = commentList.value.find(c => c.id == expandedCommentId.value)
      currentReplies.value = parent ? (parent.children || []) : []
    }
  } catch (e) {}
}

const selectComment = (comment) => {
  expandedCommentId.value = comment.id
  lastCommentId.value = comment.id
  currentReplies.value = comment.children || []
}

const targetAndLike = async (type, id) => {
  lastCommentId.value = id
  await runLikeSim(type)
}

const loadAllData = () => { loadPostInfo(); loadComments() }

const updateAllStats = (data) => {
  stats.subjectBefore = JSON.parse(JSON.stringify(stats.subjectAfter))
  stats.actorBefore = JSON.parse(JSON.stringify(stats.actorAfter))
  stats.subjectAfter = data.subjectAfter; stats.actorAfter = data.actorAfter
  lastActionSubject.value = data.actionName
  lastActionActor.value = activePerformer.value === 'actor' ? data.actionName : '영향 받음'
  if (data.targetId) {
    if (data.actionName.includes('게시글')) lastPostId.value = data.targetId
    if (data.actionName.includes('댓글')) lastCommentId.value = data.targetId
  }
  loadAllData()
}

const resetSimulation = () => {
  subjectUser.value = null; actorUser.value = null; lastPostId.value = ''; lastCommentId.value = ''; expandedCommentId.value = ''; commentList.value = []; currentReplies.value = []
  userInputs.subject = ''; userInputs.actor = ''
  Object.keys(stats).forEach(k => stats[k] = null)
}

const getParams = () => ({
  subjectId: subjectUser.value.userId, actorId: actorUser.value.userId, performerId: performerId.value
})

const runPostSim = async (type, category, hasPhoto) => {
  try {
    const res = await axios.post('/api/admin/simulation/post', null, { params: { ...getParams(), type, category, hasPhoto } })
    updateAllStats(res.data)
  } catch (e) { alert('실패') }
}

const runCommentSim = async (isReply) => {
  const pId = isReply ? expandedCommentId.value : null
  try {
    const res = await axios.post('/api/admin/simulation/comment', null, { params: { ...getParams(), postId: lastPostId.value, parentId: pId } })
    updateAllStats(res.data)
  } catch (e) { alert('실패') }
}

const runLikeSim = async (targetType) => {
  const tId = targetType === 'POST' ? lastPostId.value : lastCommentId.value
  try {
    const res = await axios.post('/api/admin/simulation/like', null, { params: { ...getParams(), targetType, targetId: tId } })
    updateAllStats(res.data)
  } catch (e) { alert('실패') }
}

const runDeleteSim = async (type) => {
  const tId = type === 'post' ? lastPostId.value : lastCommentId.value
  try {
    const res = await axios.post('/api/admin/simulation/delete-target', null, { params: { ...getParams(), type, targetId: tId } })
    updateAllStats(res.data)
    if (type === 'post') { 
      lastPostId.value = ''; expandedCommentId.value = ''; commentList.value = []; currentReplies.value = [] 
    } else {
      if (tId == expandedCommentId.value) { expandedCommentId.value = ''; currentReplies.value = [] }
      lastCommentId.value = ''; loadComments()
    }
  } catch (e) { alert('실패') }
}

const clearUser = (type) => { if (type === 'subject') subjectUser.value = null; else actorUser.value = null }
const formatVal = (key, obj) => { if (!obj) return '-'; return key === 'karma' ? obj[key].toFixed(1) + 'P' : obj[key].toLocaleString() }
const getDiff = (user, key, mode) => {
  const base = mode === 'initial' ? stats[`${user}Initial`] : stats[`${user}Before`]
  const current = stats[`${user}After`]
  return (base && current) ? current[key] - base[key] : 0
}
const formatDiff = (user, key, mode) => {
  const diff = getDiff(user, key, mode); if (diff === 0) return '-'
  const prefix = diff > 0 ? '+' : ''; return key === 'karma' ? prefix + diff.toFixed(1) + 'P' : prefix + diff.toLocaleString()
}
const getDiffClass = (user, key, mode) => { const d = getDiff(user, key, mode); return d > 0 ? 'text-plus' : (d < 0 ? 'text-minus' : '') }

watch(activePerformer, () => loadAllData())
watch(lastPostId, (newId) => { if (newId) loadAllData(); else { expandedCommentId.value = ''; commentList.value = []; currentReplies.value = [] } })
</script>

<style scoped>
.admin-simulation-container { display: flex; flex-direction: column; height: 100vh; padding: 15px; background: var(--bg-color); overflow: hidden; gap: 15px; }

/* 1행: 헤더 */
.header-full-row { width: 100%; flex-shrink: 0; }
.header-main-v36 { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 15px 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
.title-bar-v36 { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.global-actions { display: flex; gap: 10px; }
.btn-refresh-all { padding: 5px 12px; border-radius: 6px; border: 1px solid var(--link-color); color: var(--link-color); background: transparent; font-weight: 800; cursor: pointer; font-size: 0.75rem; }
.btn-reset-all { padding: 5px 12px; border-radius: 6px; border: 1px solid #ed4956; color: #ed4956; background: transparent; font-weight: 800; cursor: pointer; font-size: 0.75rem; }

.user-selector-v36 { display: flex; gap: 20px; }
.u-card-v36 { flex: 1; padding: 10px; border: 2px solid var(--border-color); border-radius: 12px; background: var(--header-bg); cursor: pointer; transition: 0.2s; }
.u-card-v36.is-acting { border-color: var(--link-color); background: rgba(0,149,246,0.05); }
.u-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
.u-radio { font-size: 0.75rem; font-weight: 800; color: var(--text-primary); cursor: pointer; }
.u-badge { padding: 2px 8px; border-radius: 4px; font-size: 0.6rem; font-weight: 900; color: white; }
.u-badge.subject { background: #3498db; }
.u-badge.actor { background: #9b59b6; }
.u-input-box { display: flex; gap: 5px; }
.mini-input { flex: 1; height: 28px; padding: 0 8px; border-radius: 4px; border: 1px solid var(--border-color); background: var(--card-bg); font-size: 0.8rem; color: var(--text-primary); }
.mini-btn { height: 28px; padding: 0 10px; background: var(--link-color); color: white; border: none; border-radius: 4px; font-weight: 800; cursor: pointer; font-size: 0.75rem; }
.u-meta-v36 { display: flex; justify-content: space-between; align-items: center; height: 28px; }
.u-name { font-weight: 800; color: var(--link-color); font-size: 0.85rem; }
.u-clear { background: none; border: none; color: #ed4956; font-size: 1.2rem; cursor: pointer; font-weight: 900; }

/* 메인 그리드 */
.simulation-view-grid { display: flex; flex-direction: column; gap: 15px; flex: 1; overflow: hidden; }

/* 2행: 트래킹 및 조작 */
.middle-section { display: grid; grid-template-columns: 500px 1fr; gap: 15px; height: 380px; flex-shrink: 0; }

.tracking-container { background: #2c3e50; color: white; padding: 15px; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; }
.tracking-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 10px; margin-bottom: 10px; }
.post-like-icon { cursor: pointer; font-size: 1.1rem; margin-left: 10px; }
.current-post-id { font-size: 0.7rem; opacity: 0.6; font-family: monospace; }

.tracking-columns-fixed { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; flex: 1; min-height: 0; }
.track-col-v36 { background: rgba(0,0,0,0.2); border-radius: 10px; padding: 10px; display: flex; flex-direction: column; min-height: 0; }
.track-title-v36 { font-size: 0.75rem; font-weight: 900; color: #3498db; margin-bottom: 8px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 5px; flex-shrink: 0; }
.track-list-scroll-v36 { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; padding-right: 5px; }

.track-item-v36 { padding: 8px; background: rgba(255,255,255,0.05); border-radius: 6px; cursor: pointer; transition: 0.2s; border: 1px solid transparent; flex-shrink: 0; }
.track-item-v36:hover { background: rgba(255,255,255,0.1); }
.track-item-v36.selected { border-color: #3498db; background: rgba(52,152,219,0.2); }
.t-item-head { display: flex; align-items: center; gap: 5px; margin-bottom: 3px; }
.t-id { font-size: 0.6rem; color: #3498db; font-weight: 900; }
.t-like-icon { font-size: 0.75rem; cursor: pointer; }
.t-author { font-size: 0.65rem; opacity: 0.7; font-weight: 800; }
.t-text { font-size: 0.75rem; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #ecf0f1; }
.track-empty { text-align: center; margin-top: 50px; font-size: 0.7rem; opacity: 0.3; }

.action-container-v36 { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 20px; display: flex; flex-direction: column; overflow: hidden; }
.active-performer-tag-v36 { background: var(--hover-bg); padding: 8px; border-radius: 8px; font-size: 0.8rem; text-align: center; margin-bottom: 15px; border: 1px dashed var(--link-color); color: var(--text-primary); }
.action-grid-v36 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; flex: 1; }
.action-group-v36 h4 { font-size: 0.75rem; font-weight: 800; color: var(--text-secondary); border-bottom: 1px solid var(--divider-color); padding-bottom: 5px; margin-bottom: 10px; }
.btn-v36-fixed { width: 100%; padding: 10px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--header-bg); font-weight: 700; font-size: 0.75rem; cursor: pointer; transition: 0.2s; text-align: left; margin-bottom: 6px; color: var(--text-primary); }
.btn-v36-fixed:hover:not(:disabled) { border-color: var(--link-color); transform: translateX(3px); }
.btn-v36-fixed.blue { color: #3498db !important; }
.btn-v36-fixed.orange { color: #e67e22 !important; }
.btn-v36-fixed.red { color: #ed4956 !important; }
.btn-v36-fixed.outline-red { border-color: #ed4956; color: #ed4956 !important; }

/* 3행: 결과 리포트 */
.dashboard-section-v36 { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; flex: 1; min-height: 0; }
.dashboard-card-v36 { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; padding: 15px; display: flex; flex-direction: column; overflow: hidden; }
.db-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; flex-shrink: 0; }
.db-head h3 { font-size: 0.9rem; margin: 0; font-weight: 800; color: var(--text-primary); }
.db-last-act { background: #34495e; color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.65rem; }
.db-table-scroll-v36 { flex: 1; overflow-y: auto; }

.db-table-v36 { width: 100%; border-collapse: collapse; font-size: 0.75rem; }
.db-table-v36 th { position: sticky; top: 0; background: var(--card-bg); text-align: left; padding: 10px 6px; border-bottom: 2px solid var(--border-color); color: var(--text-secondary); font-weight: 800; z-index: 1; }
.db-table-v36 td { padding: 8px 6px; border-bottom: 1px solid var(--divider-color); color: var(--text-primary); }
.td-label { font-weight: 700; }
.td-init { opacity: 0.4; font-style: italic; }
.td-prev { opacity: 0.6; }
.td-after { font-weight: 800; }
.td-diff { font-weight: 900; }
.td-total { font-weight: 900; background: rgba(0,149,246,0.02); font-size: 0.85rem; }

.row-changed { background: rgba(0,149,246,0.03); }
.row-divider { border-bottom: 2px solid var(--border-color) !important; }

.text-plus { color: #3498db !important; font-weight: 900; }
.text-minus { color: #ed4956 !important; font-weight: 900; }

/* Scrollbar */
::-webkit-scrollbar { width: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 10px; }
</style>
