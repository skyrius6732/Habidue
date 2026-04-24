import { defineStore } from 'pinia'
import axios from '@/plugins/axios'

export const useWeddingStore = defineStore('wedding', {
  state: () => ({
    invitation: null,
    guestbook: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchBySlug(slug) {
      this.loading = true
      this.error = null
      try {
        const res = await axios.get(`/api/wedding/${slug}`)
        this.invitation = res.data.data
        return this.invitation
      } catch (e) {
        this.error = e.response?.data?.message || '청첩장을 불러올 수 없습니다.'
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchGuestbook(slug) {
      const res = await axios.get(`/api/wedding/${slug}/guestbook`)
      this.guestbook = res.data.data
    },

    async submitRsvp(slug, payload) {
      await axios.post(`/api/wedding/${slug}/rsvp`, payload)
    },

    async writeGuestbook(slug, payload) {
      const res = await axios.post(`/api/wedding/${slug}/guestbook`, payload)
      this.guestbook.unshift(res.data.data)
    },

    async deleteGuestbook(guestbookId, password) {
      await axios.delete(`/api/wedding/guestbook/${guestbookId}`, { params: { password } })
      this.guestbook = this.guestbook.filter(g => g.id !== guestbookId)
    },
  }
})
