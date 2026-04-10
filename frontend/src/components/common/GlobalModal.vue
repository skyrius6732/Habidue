<script setup>
import { useUiStore } from '@/stores/ui'
import { watch, onMounted, onUnmounted } from 'vue'

const uiStore = useUiStore()

// [시니어 조치] 모달이 열려 있을 때 배경 스크롤 방지
watch(() => uiStore.modal.isOpen, (val) => {
  if (val) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

// ESC 키로 닫기 지원
const handleEsc = (e) => {
  if (e.key === 'Escape' && uiStore.modal.isOpen) {
    uiStore.cancel()
  }
}

onMounted(() => window.addEventListener('keydown', handleEsc))
onUnmounted(() => window.removeEventListener('keydown', handleEsc))
</script>

<template>
  <Transition name="modal-fade">
    <div v-if="uiStore.modal.isOpen" class="global-modal-overlay" @click.self="uiStore.cancel">
      <div class="global-modal-card">
        <div class="modal-content">
          <h3 v-if="uiStore.modal.title" class="modal-title">{{ uiStore.modal.title }}</h3>
          <p class="modal-message" v-html="uiStore.modal.message.replace(/\n/g, '<br>')"></p>
        </div>
        
        <div class="modal-actions" :class="{ 'has-cancel': uiStore.modal.type === 'confirm' }">
          <button 
            v-if="uiStore.modal.type === 'confirm'" 
            class="modal-btn cancel-btn" 
            @click="uiStore.cancel"
          >
            {{ uiStore.modal.cancelText }}
          </button>
          <button 
            class="modal-btn confirm-btn" 
            :class="{ 'alert-only': uiStore.modal.type === 'alert' }"
            @click="uiStore.confirm"
          >
            {{ uiStore.modal.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.global-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20000;
  padding: 20px;
}

.global-modal-card {
  background: var(--card-bg);
  width: 100%;
  max-width: 320px;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid var(--border-color);
  animation: modal-pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modal-pop {
  from { transform: scale(0.8) translateY(20px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}

.modal-content {
  padding: 30px 25px 20px;
  text-align: center;
}

.modal-title {
  font-size: 1rem;
  font-weight: 800;
  margin: 0 0 12px;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.modal-message {
  font-size: 0.85rem;
  line-height: 1.5;
  color: var(--text-secondary);
  margin: 0;
  font-weight: 500;
}

.modal-actions {
  display: flex;
  border-top: 1px solid var(--border-color);
}

.modal-actions.has-cancel {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.modal-btn {
  border: none;
  background: none;
  padding: 18px 15px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
  color: var(--text-primary);
}

.modal-btn:active {
  background: var(--hover-bg);
}

.confirm-btn {
  color: var(--link-color);
  border-left: 1px solid var(--border-color);
}

.confirm-btn.alert-only {
  width: 100%;
  border-left: none;
}

.cancel-btn {
  color: var(--text-secondary);
  font-weight: 500;
}

/* 다크모드 대응 강조색상 보정 */
.dark-mode .confirm-btn {
  color: #38bdf8;
}

.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}

@media (max-width: 480px) {
  .global-modal-card {
    max-width: 280px;
  }
}
</style>
