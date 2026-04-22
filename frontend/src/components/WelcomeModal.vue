<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="modal-backdrop" @click.self="close">
        <div class="modal-card">
          <button class="btn-close" @click="close">✕</button>

          <div class="gradient-bar"></div>

          <div class="modal-body">
            <p class="headline-emoji">🏠</p>
            <h2 class="headline">"우리, 이제 집부터<br>시작해야지"</h2>
            <p class="sub">
              임대주택 공고, 경쟁률, 실제 후기까지<br>
              <strong>하비듀</strong>에서 한 번에 확인하세요.
            </p>

            <ul class="feature-list">
              <li>💬 놓치기 쉬운 공고 알림</li>
              <li>📊 경쟁률 정보</li>
              <li>👥 실사용자 커뮤니티</li>
            </ul>

            <div class="beta-badge">🎉 지금은 베타 테스트 중!</div>
            <p class="beta-sub">참여하면 닉네임 이펙트도 드려요 ✨</p>

            <button class="btn-cta" @click="close">지금 한 번 둘러볼까요? →</button>

            <label class="no-show-label">
              <input type="checkbox" v-model="dontShowAgain" />
              <span>하루동안 다시 보지 않기</span>
            </label>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const STORAGE_KEY = 'habidue_welcome_hide_until'
const ONE_DAY_MS = 24 * 60 * 60 * 1000

const visible = ref(false)
const dontShowAgain = ref(false)

onMounted(() => {
  const hideUntil = localStorage.getItem(STORAGE_KEY)
  if (!hideUntil || Date.now() > Number(hideUntil)) {
    visible.value = true
  }
})

const close = () => {
  if (dontShowAgain.value) {
    localStorage.setItem(STORAGE_KEY, String(Date.now() + ONE_DAY_MS))
  }
  visible.value = false
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.modal-card {
  position: relative;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  width: 100%;
  max-width: 360px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);
}

.gradient-bar {
  height: 5px;
  background: linear-gradient(90deg, #f9ce34, #ee2a7b, #6228d7);
}

.btn-close {
  position: absolute;
  top: 14px;
  right: 16px;
  background: none;
  border: none;
  font-size: 1rem;
  color: var(--text-secondary);
  cursor: pointer;
  line-height: 1;
  padding: 4px;
  z-index: 1;
}
.btn-close:hover { color: var(--text-primary); }

.modal-body {
  padding: 28px 28px 24px;
  text-align: center;
}

.headline-emoji {
  font-size: 2.2rem;
  margin: 0 0 8px;
}

.headline {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.45;
  margin: 0 0 14px;
}

.sub {
  font-size: 0.88rem;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 18px;
}
.sub strong { color: var(--text-primary); }

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  text-align: left;
}

.feature-list li {
  font-size: 0.88rem;
  color: var(--text-secondary);
  padding: 9px 14px;
  background: var(--bg-primary);
  border-radius: 10px;
  border: 1px solid var(--border-color);
}

.beta-badge {
  display: inline-block;
  background: linear-gradient(135deg, #ee2a7b22, #6228d722);
  border: 1px solid #ee2a7b55;
  color: #ee2a7b;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 5px 14px;
  border-radius: 20px;
  margin-bottom: 6px;
}

.beta-sub {
  font-size: 0.82rem;
  color: var(--text-secondary);
  margin: 0 0 20px;
}

.btn-cta {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ee2a7b, #6228d7);
  color: white;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
  margin-bottom: 16px;
}
.btn-cta:hover { opacity: 0.9; }
.btn-cta:active { transform: scale(0.98); }

.no-show-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 0.78rem;
  color: var(--text-secondary);
  cursor: pointer;
}
.no-show-label input { cursor: pointer; accent-color: #ee2a7b; }

/* 트랜지션 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
.modal-fade-enter-active .modal-card,
.modal-fade-leave-active .modal-card {
  transition: transform 0.25s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-enter-from .modal-card {
  transform: scale(0.94) translateY(10px);
}
.modal-fade-leave-to .modal-card {
  transform: scale(0.94) translateY(10px);
}
</style>
