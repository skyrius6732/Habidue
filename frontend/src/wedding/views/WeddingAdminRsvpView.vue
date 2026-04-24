<template>
  <div class="rsvp-admin-container">
    <div class="admin-header">
      <button class="btn-back" @click="$router.push({ name: 'adminWedding' })">← 목록으로</button>
      <h2>📋 RSVP 응답 조회</h2>
    </div>

    <div v-if="summary" class="summary-cards">
      <div class="summary-card">
        <div class="s-label">총 응답</div>
        <div class="s-value">{{ summary.totalResponses }}명</div>
      </div>
      <div class="summary-card green">
        <div class="s-label">참석</div>
        <div class="s-value">{{ summary.attendingCount }}명</div>
      </div>
      <div class="summary-card red">
        <div class="s-label">불참</div>
        <div class="s-value">{{ summary.notAttendingCount }}명</div>
      </div>
      <div class="summary-card blue">
        <div class="s-label">총 하객</div>
        <div class="s-value">{{ summary.totalGuests }}명</div>
      </div>
    </div>

    <div class="filter-bar">
      <select v-model="filterSide" class="filter-select">
        <option value="ALL">전체</option>
        <option value="GROOM">신랑측</option>
        <option value="BRIDE">신부측</option>
        <option value="UNKNOWN">미지정</option>
      </select>
      <select v-model="filterAttendance" class="filter-select">
        <option value="ALL">참석/불참 전체</option>
        <option value="YES">참석만</option>
        <option value="NO">불참만</option>
      </select>
    </div>

    <div class="rsvp-table-wrap">
      <table class="rsvp-table">
        <thead>
          <tr>
            <th>이름</th>
            <th>연락처</th>
            <th>참석</th>
            <th>인원</th>
            <th>식사</th>
            <th>측</th>
            <th>메시지</th>
            <th>응답일</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredItems" :key="item.id">
            <td class="td-name">{{ item.name }}</td>
            <td>{{ item.phone || '-' }}</td>
            <td>
              <span class="attendance-badge" :class="item.attendance ? 'yes' : 'no'">
                {{ item.attendance ? '참석' : '불참' }}
              </span>
            </td>
            <td>{{ item.attendance ? item.guestCount : '-' }}</td>
            <td>{{ item.mealOption === null ? '-' : item.mealOption ? '예정' : '안 함' }}</td>
            <td>{{ sideLabel(item.side) }}</td>
            <td class="td-msg">{{ item.message || '-' }}</td>
            <td>{{ formatDate(item.createdAt) }}</td>
          </tr>
          <tr v-if="filteredItems.length === 0">
            <td colspan="8" class="empty-row">응답이 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'WeddingAdminRsvpView',
  data() {
    return {
      summary: null,
      filterSide: 'ALL',
      filterAttendance: 'ALL',
    }
  },
  computed: {
    filteredItems() {
      if (!this.summary?.items) return []
      return this.summary.items.filter(item => {
        if (this.filterSide !== 'ALL' && item.side !== this.filterSide) return false
        if (this.filterAttendance === 'YES' && !item.attendance) return false
        if (this.filterAttendance === 'NO' && item.attendance) return false
        return true
      })
    },
  },
  async created() {
    const res = await axios.get(`/api/admin/wedding/${this.$route.params.id}/rsvp`)
    this.summary = res.data.data
  },
  methods: {
    formatDate(dt) {
      if (!dt) return ''
      const d = new Date(dt)
      return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    },
    sideLabel(side) {
      return { GROOM: '신랑', BRIDE: '신부', UNKNOWN: '-' }[side] || side
    },
  },
}
</script>

<style scoped>
.rsvp-admin-container { padding: 0 8px; }

.admin-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.admin-header h2 { margin: 0; font-size: 20px; }
.btn-back { background: none; border: 1px solid #e0d0d0; padding: 8px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; color: #7a6a60; }

.summary-cards { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.summary-card { flex: 1; min-width: 100px; background: #fff; border: 1px solid #f0e0e0; border-radius: 12px; padding: 16px; text-align: center; }
.summary-card.green { border-color: #c8e8c8; }
.summary-card.red { border-color: #f0c8c8; }
.summary-card.blue { border-color: #c8d8f0; }
.s-label { font-size: 12px; color: #888; margin-bottom: 6px; }
.s-value { font-size: 24px; font-weight: 700; color: #4a3728; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.filter-select { padding: 8px 12px; border: 1px solid #e0d0d0; border-radius: 8px; font-size: 13px; background: #fff; }

.rsvp-table-wrap { overflow-x: auto; }
.rsvp-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.rsvp-table th { background: #f9f0f0; color: #7a6a60; padding: 10px 12px; text-align: left; border-bottom: 2px solid #f0e0e0; white-space: nowrap; }
.rsvp-table td { padding: 10px 12px; border-bottom: 1px solid #f5f0ec; color: #4a3728; }
.rsvp-table tr:hover td { background: #fffaf7; }
.td-name { font-weight: 600; }
.td-msg { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #888; }
.attendance-badge { padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; }
.attendance-badge.yes { background: #e8f8e8; color: #4a8a4a; }
.attendance-badge.no { background: #f8e8e8; color: #a06060; }
.empty-row { text-align: center; color: #aaa; padding: 40px; }
</style>
