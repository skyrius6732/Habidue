import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { ref } from 'vue'

export const useBadgeStore = defineStore('badge', () => {
  const allRules = ref([])
  const isLoaded = ref(false)

  const fetchRules = async () => {
    if (isLoaded.value) return
    try {
      const res = await axios.get('/api/users/me/activity')
      allRules.value = res.data.data.badgeRules || []
      isLoaded.value = true
    } catch (e) {
      console.error('배지 규칙 로드 실패:', e)
    }
  }

  /**
   * 계정 레벨(userLevel)을 넣으면 해당 단계의 숫자(1, 5, 10, 30...)를 반환합니다.
   * 예: 레벨 35인 유저 -> 30 반환 -> 클래스 'tier-30' 적용
   */
  const getAccountTierNumber = (userLevel) => {
    if (allRules.value.length === 0) return 1

    const accountRules = allRules.value
      .filter(r => r.badgeType === 'ACCOUNT')
      .sort((a, b) => b.level - a.level) // 레벨 숫자 역순 정렬

    // 내 레벨보다 작거나 같은 기준 레벨 중 가장 큰 것 찾기
    const match = accountRules.find(r => userLevel >= r.level)
    return match ? match.level : 1
  }

  return { allRules, isLoaded, fetchRules, getAccountTierNumber }
})
