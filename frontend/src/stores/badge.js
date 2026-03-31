import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { ref } from 'vue'

export const useBadgeStore = defineStore('badge', () => {
  const allRules = ref([])
  const userBadges = ref([])
  const isLoaded = ref(false)
  let fetchPromise = null

  const fetchRules = async () => {
    if (isLoaded.value) return
    if (fetchPromise) return fetchPromise

    fetchPromise = (async () => {
      try {
        const res = await axios.get('/api/users/me/activity')
        allRules.value = res.data.data.badgeRules || []
        userBadges.value = res.data.data.badges || []
        isLoaded.value = true
      } catch (e) {
        console.error('배지 규칙 로드 실패:', e)
        throw e
      } finally {
        fetchPromise = null
      }
    })()

    return fetchPromise
  }

  /**
   * 계정 레벨(userLevel)을 넣으면 해당 단계의 숫자(1, 5, 10, 30...)를 반환합니다.
   * 예: 레벨 35인 유저 -> 30 반환 -> 클래스 'tier-30' 적용
   */
  // CSS tier 클래스 숫자와 일치하는 고정 임계값 (tier-1, tier-5, ... tier-100)
  const TIER_THRESHOLDS = [
    { min: 100, tier: 100 }, { min: 90, tier: 90 }, { min: 70, tier: 70 },
    { min: 50, tier: 50 },   { min: 30, tier: 30 }, { min: 10, tier: 10 },
    { min: 5,  tier: 5 },    { min: 1,  tier: 1 },
  ]

  const getAccountTierNumber = (userLevel) => {
    const lv = userLevel || 1
    // ACCOUNT 타입 규칙이 있으면 우선 사용
    if (allRules.value.length > 0) {
      const accountRules = allRules.value
        .filter(r => r.badgeType === 'ACCOUNT')
        .sort((a, b) => b.level - a.level)
      if (accountRules.length > 0) {
        const match = accountRules.find(r => lv >= r.level)
        return match ? match.level : 1
      }
    }
    // 폴백: CSS tier 클래스와 일치하는 임계값 기준
    return (TIER_THRESHOLDS.find(t => lv >= t.min) || TIER_THRESHOLDS[7]).tier
  }

  /**
   * 장착한 배지(equippedBadgeId)의 이모지와 계급명을 반환합니다.
   * 장착 배지가 없거나 규칙이 로드되지 않은 경우 계정 레벨 기반 폴백을 반환합니다.
   */
  const getEquippedBadgeInfo = (equippedBadgeId, userLevel) => {
    if (equippedBadgeId && userBadges.value.length > 0) {
      const badge = userBadges.value.find(b => b.id === equippedBadgeId)
      if (badge) {
        const rule = allRules.value.find(r => r.badgeType === badge.type && r.level === badge.level)
        if (rule) return { emoji: rule.rankEmoji, rankTitle: rule.rankTitle }
      }
    }
    // 폴백: 계정 레벨 기준으로 KNOWLEDGE 타입 규칙에서 추출 (모든 타입이 동일한 rankEmoji/rankTitle 사용)
    const rules = allRules.value
      .filter(r => r.badgeType === 'KNOWLEDGE')
      .sort((a, b) => b.level - a.level)
    const match = rules.find(r => (userLevel || 1) >= r.level)
    return match
      ? { emoji: match.rankEmoji, rankTitle: match.rankTitle }
      : { emoji: '🌱', rankTitle: '새싹' }
  }

  const refresh = async () => {
    isLoaded.value = false
    fetchPromise = null
    return fetchRules()
  }

  return { allRules, userBadges, isLoaded, fetchRules, refresh, getAccountTierNumber, getEquippedBadgeInfo }
})
