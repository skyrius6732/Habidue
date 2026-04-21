import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    modal: {
      isOpen: false,
      title: '',
      message: '',
      type: 'alert', // 'alert' | 'confirm' | 'prompt'
      confirmText: '확인',
      cancelText: '취소',
      resolvePromise: null, // Promise를 통해 사용자의 선택을 기다림
      inputValue: '', // prompt 입력값
      inputPlaceholder: '',
    }
  }),

  actions: {
    /**
     * [시니어 조치] 단순 알림 모달 (Alert)
     */
    showAlert(message, title = '알림', confirmText = '확인') {
      this.modal = {
        isOpen: true,
        title,
        message,
        type: 'alert',
        confirmText,
        cancelText: '',
        resolvePromise: null
      }
      return new Promise((resolve) => {
        this.modal.resolvePromise = resolve
      })
    },

    /**
     * [시니어 조치] 확인/취소 선택 모달 (Confirm)
     */
    showConfirm(message, title = '확인', confirmText = '확인', cancelText = '취소') {
      this.modal = {
        isOpen: true,
        title,
        message,
        type: 'confirm',
        confirmText,
        cancelText,
        resolvePromise: null,
        inputValue: '',
        inputPlaceholder: ''
      }
      return new Promise((resolve) => {
        this.modal.resolvePromise = resolve
      })
    },

    /**
     * [시니어 조치] 텍스트 입력 모달 (Prompt)
     */
    showPrompt(message, title = '입력', placeholder = '', confirmText = '확인', cancelText = '취소') {
      this.modal = {
        isOpen: true,
        title,
        message,
        type: 'prompt',
        confirmText,
        cancelText,
        inputValue: '',
        inputPlaceholder: placeholder,
        resolvePromise: null
      }
      return new Promise((resolve) => {
        this.modal.resolvePromise = resolve
      })
    },

    /**
     * 사용자가 '확인'을 눌렀을 때
     */
    confirm() {
      if (this.modal.resolvePromise) {
        // prompt 모달일 때는 입력값 반환, 아니면 true 반환
        const result = this.modal.type === 'prompt' ? this.modal.inputValue : true
        this.modal.resolvePromise(result)
      }
      this.closeModal()
    },

    /**
     * 사용자가 '취소'를 눌렀을 때
     */
    cancel() {
      if (this.modal.resolvePromise) {
        // prompt 모달일 때는 null 반환, 아니면 false 반환
        const result = this.modal.type === 'prompt' ? null : false
        this.modal.resolvePromise(result)
      }
      this.closeModal()
    },

    closeModal() {
      this.modal.isOpen = false
      // 애니메이션 종료 후 초기화 위해 약간의 지연 후 데이터 삭제
      setTimeout(() => {
        if (!this.modal.isOpen) {
          this.modal.title = ''
          this.modal.message = ''
          this.modal.resolvePromise = null
        }
      }, 300)
    }
  }
})
