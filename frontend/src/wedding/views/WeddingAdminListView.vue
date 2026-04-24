<template>
  <div class="wedding-admin-container">
    <div class="admin-header">
      <div class="header-main">
        <h2>💍 모바일 청첩장 관리</h2>
        <span class="count-info">{{ invitations.length }}건</span>
      </div>
      <button class="btn-create" @click="$router.push({ name: 'adminWeddingNew' })">+ 새 청첩장 만들기</button>
    </div>

    <div v-if="loading" class="loading-state">불러오는 중...</div>

    <div v-else-if="invitations.length === 0" class="empty-state">
      아직 등록된 청첩장이 없습니다.
    </div>

    <div v-else class="invitation-list">
      <div v-for="inv in invitations" :key="inv.id" class="invitation-card">
        <div class="inv-main">
          <div class="inv-names">{{ inv.groomName }} &amp; {{ inv.brideName }}</div>
          <div class="inv-date">{{ formatDate(inv.weddingDate) }} · {{ inv.venueName }}</div>
          <div class="inv-url">
            <a :href="`/wedding/${inv.slug}`" target="_blank" class="inv-link">
              /wedding/{{ inv.slug }}
            </a>
          </div>
        </div>
        <div class="inv-meta">
          <span class="status-badge" :class="inv.status.toLowerCase()">{{ statusLabel(inv.status) }}</span>
          <span class="view-count">👁 {{ inv.viewCount }}</span>
        </div>
        <div class="inv-actions">
          <button class="btn-action" @click="$router.push({ name: 'adminWeddingRsvp', params: { id: inv.id } })">
            RSVP 조회
          </button>
          <button class="btn-action" @click="$router.push({ name: 'adminWeddingEdit', params: { id: inv.id } })">
            수정
          </button>
          <button class="btn-action danger" @click="deleteInvitation(inv)">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'WeddingAdminListView',
  data() {
    return {
      invitations: [],
      loading: true,
    }
  },
  async created() {
    await this.fetchAll()
  },
  methods: {
    async fetchAll() {
      this.loading = true
      try {
        const res = await axios.get('/api/admin/wedding')
        this.invitations = res.data.data
      } finally {
        this.loading = false
      }
    },
    formatDate(dt) {
      if (!dt) return ''
      const d = new Date(dt)
      return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
    },
    statusLabel(status) {
      return { DRAFT: '초안', ACTIVE: '공개중', EXPIRED: '만료' }[status] || status
    },
    async deleteInvitation(inv) {
      if (!confirm(`"${inv.groomName} & ${inv.brideName}" 청첩장을 삭제하시겠습니까? 관련 데이터(RSVP, 방명록)가 모두 삭제됩니다.`)) return
      await axios.delete(`/api/admin/wedding/${inv.id}`)
      this.invitations = this.invitations.filter(i => i.id !== inv.id)
    },
  },
}
</script>

<style scoped>
.wedding-admin-container { padding: 0 8px; }

.admin-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 24px; }
.header-main { display: flex; align-items: center; gap: 12px; }
.admin-header h2 { margin: 0; font-size: 20px; }
.count-info { font-size: 13px; color: #888; }
.btn-create { background: #c8a4a4; color: #fff; border: none; padding: 10px 18px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-create:hover { background: #b08080; }

.loading-state, .empty-state { text-align: center; padding: 60px 0; color: #888; font-size: 15px; }

.invitation-list { display: flex; flex-direction: column; gap: 12px; }

.invitation-card {
  background: #fff;
  border: 1px solid #f0e0e0;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.inv-main { flex: 1; min-width: 200px; }
.inv-names { font-size: 17px; font-weight: 600; color: #4a3728; margin-bottom: 4px; }
.inv-date { font-size: 13px; color: #888; margin-bottom: 4px; }
.inv-link { font-size: 12px; color: #c8a4a4; text-decoration: none; }
.inv-link:hover { text-decoration: underline; }

.inv-meta { display: flex; align-items: center; gap: 10px; }
.status-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.status-badge.draft { background: #f5f5f5; color: #888; }
.status-badge.active { background: #e8f8e8; color: #4a8a4a; }
.status-badge.expired { background: #f8f0f0; color: #a06060; }
.view-count { font-size: 13px; color: #888; }

.inv-actions { display: flex; gap: 8px; }
.btn-action { padding: 8px 14px; border: 1px solid #e0d0d0; border-radius: 8px; background: #fff; font-size: 13px; cursor: pointer; color: #4a3728; }
.btn-action:hover { background: #f9f0f0; }
.btn-action.danger { color: #c05050; border-color: #f0c0c0; }
.btn-action.danger:hover { background: #fff0f0; }
</style>
