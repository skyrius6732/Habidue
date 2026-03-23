<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import axios from '@/plugins/axios'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'

const props = defineProps({
  show: Boolean,
  targetId: Number,
  reporterId: Number
})

const emit = defineEmits(['close'])

const conversation = ref([])
const isMutualReport = ref(false)
const isLoading = ref(false)
const activeTab = ref('log')

const isAiAnalyzing = ref(false)
const aiResult = ref(null)

// [시니어 조치] 시스템 메시지 미리보기 템플릿 생성 로직
const previewMessages = computed(() => {
  if (!aiResult.value) return null
  const violation = aiResult.value.violationPoint || '운영원칙 위반'
  const suspectName = conversation.value.find(m => m.id === props.targetId)?.sender?.nickname || '회원'

  return {
    blind: {
      suspect: `⚠️ [경고] ${suspectName}님, 회원님께서 사용하신 표현은 ${violation} 정황이 확인되어 제재가 적용되었으며, 본 대화방에서의 메시지 발송이 제한됩니다.`,
      reporter: `📢 [안내] 신고하신 내용에 대해 관리자 검토가 완료되었습니다. 해당 대화방은 ${violation}로 인해 발송 제한 조치가 취해졌습니다.`
    },
    delete: {
      suspect: `🚫 [주의] ${suspectName}님, 회원님께서 사용하신 표현은 ${violation} 심각한 운영원칙 위반이 확인되어 본 대화방에서의 메시지 발송이 영구적으로 제한됩니다.`,
      reporter: `📢 [안내] 신고하신 대화방에 대해 ${violation} 심각한 위반이 확인되어 영구 제한 및 상대방 차단 조치가 완료되었습니다.`
    }
  }
})

watch(() => [props.show, props.targetId], async ([newShow, newTargetId]) => {
  if (newShow && newTargetId) {
    activeTab.value = 'log'
    aiResult.value = null
    await fetchLog()
  }
}, { immediate: true })

const fetchLog = async () => {
  isLoading.value = true
  try {
    const res = await axios.get(`/api/admin/messages/log/${props.targetId}`)
    conversation.value = res.data.data.messages || []
    isMutualReport.value = res.data.data.isMutualReport || false
    if (res.data.data.aiResult) aiResult.value = res.data.data.aiResult
  } catch (e) { conversation.value = [] } finally { isLoading.value = false }
}

const runAiAnalysis = async () => {
  isAiAnalyzing.value = true
  activeTab.value = 'ai'
  aiResult.value = null
  try {
    const res = await axios.post(`/api/admin/messages/${props.targetId}/ai-analyze`, {}, { timeout: 60000 })
    if (res.data && res.data.data) aiResult.value = res.data.data
    else aiResult.value = { error: '데이터 형식이 올바르지 않습니다.' }
  } catch (e) {
    let errorMsg = 'AI 분석 서비스와 통신 중 오류가 발생했습니다.'
    if (e.code === 'ECONNABORTED') errorMsg = '분석 시간이 초과되었습니다.'
    aiResult.value = { error: errorMsg }
  } finally { isAiAnalyzing.value = false }
}

const formatDate = (dateStr) => dateStr ? format(new Date(dateStr), 'yyyy.MM.dd HH:mm', { locale: ko }) : '-'
const isImage = (type) => type && type.startsWith('image/')
const openImage = (url) => window.open(url, '_blank')
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="log-modal-overlay" @click.self="$emit('close')">
      <div class="log-modal-content">
        <div class="log-header">
          <div class="header-top">
            <div class="header-title">
              <h3>🔍 신고 대화 분석</h3>
              <span class="target-id-badge">ID #{{ targetId }}</span>
              <span v-if="isMutualReport" class="mutual-report-badge">🚨 쌍방 신고 감지</span>
            </div>
            <button class="close-btn" @click="$emit('close')">&times;</button>
          </div>
          <div class="tab-menu">
            <button :class="{ active: activeTab === 'log' }" @click="activeTab = 'log'">대화 로그</button>
            <button :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">AI 분석 및 조치 가이드</button>
          </div>
        </div>

        <div class="log-body thin-scrollbar">
          <!-- 탭 1: 대화 로그 -->
          <template v-if="activeTab === 'log'">
            <div v-if="isLoading" class="loading">로그를 불러오는 중...</div>
            <div v-else v-for="msg in conversation" :key="msg.id" class="log-row" :class="{ 'is-reported': msg.id === targetId }">
              <div class="log-meta">
                <div class="author-box">
                  <span class="log-author" :class="{ 'reporter': msg.sender.id === reporterId }">{{ msg.sender.nickname }}</span>
                  <span class="author-id">(ID: {{ msg.sender.id }})</span>
                  <small v-if="msg.sender.id === reporterId" class="badge reporter">신고자</small>
                  <small v-else class="badge suspect">피신고자</small>
                </div>
                <span class="log-date">{{ formatDate(msg.createdAt) }}</span>
              </div>
              <div class="log-bubble">
                <p class="log-text">{{ msg.content }}</p>
                <div v-if="msg.attachments && msg.attachments.length > 0" class="log-files">
                  <div v-for="f in msg.attachments" :key="f.id" class="log-file-item">
                    <img v-if="isImage(f.fileType)" :src="f.fileUrl" @click="openImage(f.fileUrl)" class="log-img-thumb" />
                    <div v-else class="log-file-box"><span class="log-f-name">📎 {{ f.originalFileName }}</span></div>
                  </div>
                </div>
                <div v-if="msg.id === targetId" class="reported-badge">🚨 신고 타겟</div>
              </div>
            </div>
          </template>

          <!-- 탭 2: AI 분석 및 가이드 -->
          <template v-else>
            <div class="ai-analysis-container">
              <div v-if="!aiResult && !isAiAnalyzing" class="ai-intro">
                <div class="ai-icon">🤖</div>
                <h4>대화 맥락 정밀 분석</h4>
                <p>Gemini AI가 제재 수위 추천 및 시스템 메시지 사유를 자동 생성합니다.</p>
                <button class="btn-run-ai" @click="runAiAnalysis">AI 분석 시작하기</button>
              </div>

              <div v-else-if="isAiAnalyzing" class="ai-loading"><div class="ai-spinner"></div><p>AI가 분석 중입니다...</p></div>

              <div v-else-if="aiResult" class="ai-result-view">
                <div v-if="aiResult.error" class="ai-error-box"><span class="error-icon">⚠️</span><div class="error-msg"><strong>분석 실패</strong><p>{{ aiResult.error }}</p></div><button class="btn-retry" @click="runAiAnalysis">재시도</button></div>

                <template v-else>
                  <!-- 위험도 게이지 및 점수 -->
                  <div class="result-card" :class="aiResult.recommendedAction">
                    <div class="risk-header">
                      <span class="risk-label">🔥 대화 위험도 ({{ Math.round((aiResult.score || 0) * 100) }}%)</span>
                      <span class="recommend-badge" :class="aiResult.recommendedAction">
                        추천: {{ aiResult.recommendedAction === 'DELETE' ? '2단계(영구 삭제)' : (aiResult.recommendedAction === 'BLIND' ? '1단계(블라인드)' : '반려 권장') }}
                      </span>
                    </div>
                    <div class="risk-score-bar"><div class="fill" :style="{ width: (aiResult.score || 0) * 100 + '%' }"></div></div>
                  </div>

                  <div class="analysis-section">
                    <h5>💡 AI 맥락 분석 요약</h5>
                    <p class="analysis-text">{{ aiResult.summary }}</p>
                  </div>

                  <!-- [요청사항] 4가지 시스템 메시지 미리보기 -->
                  <div class="analysis-section" v-if="previewMessages">
                    <h5>📩 제재 시 발송될 시스템 메시지 미리보기</h5>
                    <div class="template-grid">
                      <div class="template-group blind">
                        <div class="group-title">1단계 (블라인드 / 주황색)</div>
                        <div class="msg-box suspect"><strong>피신고자용:</strong> <p>{{ previewMessages.blind.suspect }}</p></div>
                        <div class="msg-box reporter"><strong>신고자용:</strong> <p>{{ previewMessages.blind.reporter }}</p></div>
                      </div>
                      <div class="template-group delete">
                        <div class="group-title">2단계 (영구 삭제 / 빨간색)</div>
                        <div class="msg-box suspect"><strong>피신고자용:</strong> <p>{{ previewMessages.delete.suspect }}</p></div>
                        <div class="msg-box reporter"><strong>신고자용:</strong> <p>{{ previewMessages.delete.reporter }}</p></div>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </template>
        </div>

        <div class="log-footer">
          <div class="footer-info">
            <p class="info-text">* AI 결과는 참고용이며 최종 결정은 관리자의 몫입니다.</p>
            <p v-if="isMutualReport" class="mutual-warning-text">⚠️ 쌍방 신고 감지됨</p>
          </div>
          <div class="footer-btns"><button class="btn-close-modal" @click="$emit('close')">닫기</button></div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.log-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.85); display: flex; align-items: center; justify-content: center; z-index: 100000; backdrop-filter: blur(8px); }
.log-modal-content { background: #111; color: #eee; width: 95%; max-width: 850px; height: 90vh; border-radius: 24px; display: flex; flex-direction: column; border: 1px solid #333; box-shadow: 0 40px 100px rgba(0,0,0,0.6); overflow: hidden; }

.log-header { padding: 20px 30px; border-bottom: 1px solid #222; }
.header-top { display: flex; justify-content: space-between; margin-bottom: 15px; }
.header-title { display: flex; align-items: center; gap: 12px; }
.header-title h3 { margin: 0; font-size: 1.2rem; font-weight: 900; }
.target-id-badge { background: #333; padding: 2px 8px; border-radius: 6px; font-size: 0.75rem; color: #aaa; }
.mutual-report-badge { background: #ef4444; color: white; font-size: 0.7rem; font-weight: 950; padding: 3px 10px; border-radius: 20px; animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }

.tab-menu { display: flex; gap: 10px; }
.tab-menu button { background: #222; border: 1px solid #333; color: #888; padding: 10px 20px; border-radius: 12px; font-size: 0.85rem; font-weight: 700; cursor: pointer; }
.tab-menu button.active { background: #a855f7; color: white; border-color: #a855f7; }

.close-btn { background: none; border: none; color: #555; font-size: 2.2rem; cursor: pointer; padding: 0; }

.log-body { flex: 1; overflow-y: auto; padding: 30px; background: #000; }
.thin-scrollbar::-webkit-scrollbar { width: 5px; }
.thin-scrollbar::-webkit-scrollbar-thumb { background: #222; border-radius: 10px; }

.log-row { display: flex; flex-direction: column; gap: 10px; margin-bottom: 25px; }
.log-row.is-reported .log-bubble { border: 2px solid #ed4956; background: rgba(237, 73, 86, 0.05); }
.log-meta { display: flex; justify-content: space-between; align-items: center; }
.author-box { display: flex; align-items: center; gap: 8px; }
.log-author { font-size: 0.95rem; font-weight: 900; color: #fff; }
.author-id { font-size: 0.75rem; color: #666; }
.badge { font-size: 0.6rem; padding: 2px 6px; border-radius: 4px; font-weight: 800; }
.badge.reporter { background: rgba(59, 130, 246, 0.2); color: #3b82f6; border: 1px solid #3b82f6; }
.badge.suspect { background: rgba(239, 68, 68, 0.2); color: #ef4444; border: 1px solid #ef4444; }
.log-date { font-size: 0.75rem; color: #444; }
.log-bubble { background: #1a1a1a; padding: 18px; border-radius: 18px; position: relative; border: 1px solid #222; }
.log-text { margin: 0; font-size: 1rem; line-height: 1.6; color: #ddd; }
.log-img-thumb { 
  max-width: 100%; 
  max-height: 400px; 
  border-radius: 12px; 
  margin-top: 10px; 
  cursor: pointer; 
  object-fit: contain; 
  border: 1px solid #333;
  transition: transform 0.2s;
}
.log-img-thumb:hover { transform: scale(1.02); }
.reported-badge { position: absolute; top: -12px; right: 15px; background: #ed4956; color: white; font-size: 0.7rem; font-weight: 950; padding: 3px 10px; border-radius: 20px; }

.ai-analysis-container { min-height: 400px; display: flex; flex-direction: column; }
.ai-intro { text-align: center; margin: auto; }
.ai-icon { font-size: 4rem; margin-bottom: 20px; }
.btn-run-ai { background: linear-gradient(135deg, #a855f7, #6366f1); color: white; border: none; padding: 15px 40px; border-radius: 15px; font-weight: 800; font-size: 1.1rem; cursor: pointer; }

.ai-result-view { display: flex; flex-direction: column; gap: 30px; }
.result-card { background: #1a1a1a; padding: 25px; border-radius: 20px; border-left: 6px solid #444; }
.result-card.BLIND { border-left-color: #f59e0b; background: rgba(245, 158, 11, 0.05); }
.result-card.DELETE { border-left-color: #ef4444; background: rgba(239, 68, 68, 0.05); }

.risk-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.risk-label { font-weight: 800; font-size: 1rem; }
.recommend-badge { padding: 4px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 900; }
.recommend-badge.BLIND { background: #f59e0b; color: white; }
.recommend-badge.DELETE { background: #ef4444; color: white; }
.recommend-badge.REJECT { background: #10b981; color: white; }

.risk-score-bar { height: 10px; background: #333; border-radius: 5px; overflow: hidden; }
.risk-score-bar .fill { height: 100%; background: linear-gradient(to right, #10b981, #f59e0b, #ef4444); transition: width 1s ease-out; }

.analysis-section h5 { font-size: 0.95rem; color: #a855f7; margin: 0 0 12px; font-weight: 850; border-left: 3px solid #a855f7; padding-left: 10px; }
.analysis-text { font-size: 1rem; line-height: 1.7; color: #ccc; background: #151515; padding: 15px; border-radius: 12px; }

/* 템플릿 그리드 레이아웃 */
.template-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.template-group { background: #151515; padding: 20px; border-radius: 16px; border: 1px solid #222; display: flex; flex-direction: column; gap: 15px; }
.group-title { font-size: 0.85rem; font-weight: 900; padding-bottom: 10px; border-bottom: 1px solid #333; text-align: center; }
.template-group.blind .group-title { color: #f59e0b; }
.template-group.delete .group-title { color: #ef4444; }

.msg-box { font-size: 0.8rem; line-height: 1.5; }
.msg-box strong { display: block; margin-bottom: 5px; opacity: 0.6; font-size: 0.7rem; }
.msg-box p { margin: 0; background: #1a1a1a; padding: 12px; border-radius: 10px; color: #aaa; border: 1px solid #222; }
.template-group.blind .msg-box.suspect p { border-left: 3px solid #f59e0b; }
.template-group.delete .msg-box.suspect p { border-left: 3px solid #ef4444; }

.log-footer { padding: 20px 30px; border-top: 1px solid #222; display: flex; justify-content: space-between; align-items: center; }
.footer-info { font-size: 0.75rem; color: #666; }
.mutual-warning-text { color: #ef4444; font-weight: 800; margin-top: 4px; }
.btn-close-modal { background: #222; color: #fff; border: 1px solid #333; padding: 10px 25px; border-radius: 10px; font-weight: 800; cursor: pointer; }

@keyframes ai-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.ai-spinner { 
  width: 50px; height: 50px; 
  border: 4px solid rgba(168, 85, 247, 0.1); 
  border-top-color: #a855f7; 
  border-radius: 50%; 
  animation: ai-spin 1s linear infinite; 
  margin: 40px auto 20px; 
}
.ai-loading { text-align: center; margin: auto; }
</style>