<template>
  <div 
    class="animated-nickname-wrapper" 
    v-click-outside="closeTooltip"
    @mouseenter="handleMouseEnter" 
    @mouseleave="handleMouseLeave"
  >
    <!-- 아바타 (show-avatar=true 일 때) -->
    <div v-if="showAvatar" class="an-avatar" :class="currentTierClass">
      <template v-if="showParticles">
        <span class="an-av-p p1"></span>
        <span class="an-av-p p2"></span>
        <span class="an-av-p p3"></span>
        <span class="an-av-p p4"></span>
      </template>
      <span class="an-av-initial">{{ nickname.charAt(0).toUpperCase() }}</span>
    </div>

    <!-- 등급별 미세 파티클 (50레벨 이상) -->
    <div v-if="(showParticles && particleOptions)" class="nickname-particles-container">
      <vue-particles :id="`particles-${instanceId}`" :options="particleOptions" />
    </div>

    <!-- [시니어] 카르마 주의 아이콘 -->
    <span v-if="(Number(karmaPoint) <= 800)" class="karma-warning-icon-outer" title="활동 신뢰 점수 주의">⚠️</span>

    <!-- 화려한 닉네임 표시부 (클릭 시 토글) -->
    <div :class="['nickname-with-effects', { 'has-wings': isWingsEffect, 'has-bubbles': isBubblesEffect, 'has-stars': isStarsEffect, 'has-thunder': isThunderEffect, 'has-flame': isFlameEffect, 'has-ice': isIceFrostEffect, 'has-sakura': isSakuraEffect, 'has-roses': isRosesEffect, 'has-shadow': isShadowEffect, 'has-neon': isNeonEffect, 'has-glitch': isGlitchEffect, 'has-void': isVoidEffect, 'has-heart': isHeartEffect, 'has-rainbow': isRainbowEffect, 'has-shooting': isShootingStarEffect, 'has-bomb': isBombEffect, 'has-crown': isCrownEffect, 'has-swords': isSwordsCrossEffect, 'has-flower': isFlowerCrownEffect, 'has-star-tiara': isStarTiaraEffect, 'has-flower-rain': isFlowerRainEffect, 'has-scanline': isScanLineEffect, 'has-chromatic': isChromaticAberrationEffect, 'has-echo': isEchoTrailEffect, 'has-corrupted': isCorruptedTextEffect, 'has-glitch-shift': isGlitchShiftEffect, 'has-liquid': isLiquidDistortEffect, 'has-halo': isHaloEffect, 'has-leaves': isLeavesEffect, 'has-butterflies': isButterfliesEffect, 'has-orbs': isOrbsEffect, 'has-scale-collapse': isScaleCollapseEffect }]">
      
      <!-- Effect Components Left -->
      <EffectWings v-if="isWingsEffect" direction="left" :wing-layers="wingLayers" :wing-colors="wingColors" :instance-id="instanceId" />
      <EffectBubbles v-if="isBubblesEffect" />
      <EffectStars v-if="isStarsEffect" />
      <EffectThunder v-if="isThunderEffect" />
      <EffectIceFrost v-if="isIceFrostEffect" />
      <EffectSakura v-if="isSakuraEffect" />
      <EffectRoses v-if="isRosesEffect" />
      <EffectShadow v-if="isShadowEffect" />
      <EffectRainbow v-if="isRainbowEffect" />
      <EffectShootingStar v-if="isShootingStarEffect" />
      <EffectBomb v-if="isBombEffect" />
      <EffectHeart v-if="isHeartEffect" />
      <EffectVoid v-if="isVoidEffect" />
      <EffectCrown v-if="isCrownEffect" />
      <EffectSwordsCross v-if="isSwordsCrossEffect" />
      <EffectFlowerCrown v-if="isFlowerCrownEffect" />
      <EffectStarTiara v-if="isStarTiaraEffect" />
      <EffectFlowerRain v-if="isFlowerRainEffect" />
      <EffectScanLine v-if="isScanLineEffect" />
      <EffectChromaticAberration v-if="isChromaticAberrationEffect" />
      <EffectEchoTrail v-if="isEchoTrailEffect" />
      <EffectCorruptedText v-if="isCorruptedTextEffect" />
      <EffectGlitchShift v-if="isGlitchShiftEffect" />
      <EffectLiquidDistort v-if="isLiquidDistortEffect" />
      <EffectHalo v-if="isHaloEffect" />
      <EffectLeaves v-if="isLeavesEffect" />
      <EffectButterflies v-if="isButterfliesEffect" />
      <EffectOrbs v-if="isOrbsEffect" />
      <EffectScaleCollapse v-if="isScaleCollapseEffect" />

      <span ref="nicknameRef" :class="['animated-nickname', currentTierClass, { 'is-ellipsis': isEllipsis }]" :data-text="nickname" :style="nicknameTextColor ? { color: nicknameTextColor } : {}" @click.stop="handleToggleClick">
        {{ nickname }}
      </span>

      <!-- Wings Right -->
      <EffectWings v-if="isWingsEffect" direction="right" :wing-layers="wingLayers" :wing-colors="wingColors" :instance-id="instanceId" />

    </div>

    <!-- 미니 프로필 툴팁 -->
    <div v-if="showTooltip && !effectOnly" ref="tooltipRef" :class="['nickname-tooltip', currentTierClass, computedDirection]" @click.stop>
      <div v-if="(level >= 30)" class="glass-reflection-overlay"></div>
      
      <div class="tooltip-content-inner">
        <div class="tooltip-header stagger-item">
          <div class="user-avatar" :class="currentTierClass">{{ nickname.charAt(0).toUpperCase() }}</div>
          <div class="user-info-basic">
            <span class="user-name-styled" :class="currentTierClass">{{ nickname }}</span>
            <span class="user-level" :class="currentTierClass">Lv.{{ level }}</span>
          </div>
        </div>
        
        <div v-if="equippedBadgeName || mainBadge" :class="['main-title-box', currentTierClass, 'stagger-item']">
          <span class="title-label">대표 칭호</span>
          <span class="title-value">{{ equippedBadgeName || mainBadge.displayName }}</span>
        </div>

        <div class="karma-section-tooltip stagger-item" :class="{ 'warning': (karmaPoint <= 800) }">
          <div class="karma-header-mini">
            <span class="k-label">활동 신뢰 점수</span>
            <span class="k-value">{{ (karmaPoint / 10).toFixed(1) }} P</span>
          </div>
          <p v-if="(karmaPoint <= 800)" class="k-desc">신뢰 점수 저하 주의</p>
        </div>

        <div class="exp-section stagger-item">
          <div class="exp-labels">
            <span class="exp-label-text">EXP</span>
            <span class="exp-values">{{ exp }} / {{ nextLevelExp }}</span>
          </div>
          <div class="exp-bar-bg">
            <div class="exp-bar-fill" :class="currentTierClass" :style="{ width: expPercentage + '%' }"></div>
          </div>
        </div>

        <div class="tooltip-actions stagger-item" v-if="userPublicId && !isMe">
          <button class="msg-send-btn" @click.stop="openMessageModal">
            <span>💌 쪽지 보내기</span>
          </button>
        </div>

        <!-- [시니어 조치] 본인일 경우 이펙트 설정 노출 -->
        <div class="tooltip-effect-settings stagger-item" v-if="isMe">
          <div class="effect-header-mini">
            <span class="effect-label-mini">닉네임 장식</span>
          </div>
          
          <div 
            class="effect-scroll-viewport"
            ref="scrollContainer"
            @mousedown="handleDragStart"
            @mousemove="handleDragMove"
            @mouseleave="handleDragEnd"
            @mouseup="handleDragEnd"
          >
            <div class="effect-toggle-group">
              <button
                v-for="eff in specialEffects" :key="eff.id"
                class="effect-toggle-btn"
                :class="{
                  active: (displayEquippedEffect === eff.id) || (!displayEquippedEffect && !eff.id),
                  locked: !isAdmin && eff.id && !effectCodes.includes(eff.id)
                }"
                @click.stop="isAdmin || !eff.id || effectCodes.includes(eff.id) ? updateEffect(eff.id, $event) : null"
                :disabled="isUpdatingEffect"
                :title="eff.name"
              >
                <span class="eff-icon">{{ eff.icon }}</span>
                <span v-if="!isAdmin && eff.id && !effectCodes.includes(eff.id)" class="lock-overlay">🔒</span>
                <span v-if="(displayEquippedEffect === eff.id) || (!displayEquippedEffect && !eff.id)" class="active-check">✓</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 쪽지 발송 모달 -->
    <MessageSendModal 
      v-if="userPublicId && showMessageModal"
      :show="showMessageModal" 
      :receiver-public-id="userPublicId"
      :receiver-nickname="nickname"
      :is-withdrawn="authorActive === false"
      @close="showMessageModal = false"
      @success="onMessageSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import axios from '@/plugins/axios'
import { useBadgeStore } from '@/stores/badge'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import MessageSendModal from '@/components/MessageSendModal.vue'
import EffectWings from '@/components/effects/EffectWings.vue'
import EffectBubbles from '@/components/effects/EffectBubbles.vue'
import EffectStars from '@/components/effects/EffectStars.vue'
import EffectThunder from '@/components/effects/EffectThunder.vue'
import EffectIceFrost from '@/components/effects/EffectIceFrost.vue'
import EffectSakura from '@/components/effects/EffectSakura.vue'
import EffectRoses from '@/components/effects/EffectRoses.vue'
import EffectShadow from '@/components/effects/EffectShadow.vue'
import EffectRainbow from '@/components/effects/EffectRainbow.vue'
import EffectShootingStar from '@/components/effects/EffectShootingStar.vue'
import EffectBomb from '@/components/effects/EffectBomb.vue'
import EffectHeart from '@/components/effects/EffectHeart.vue'
import EffectVoid from '@/components/effects/EffectVoid.vue'
import EffectCrown from '@/components/effects/EffectCrown.vue'
import EffectSwordsCross from '@/components/effects/EffectSwordsCross.vue'
import EffectFlowerCrown from '@/components/effects/EffectFlowerCrown.vue'
import EffectStarTiara from '@/components/effects/EffectStarTiara.vue'
import EffectFlowerRain from '@/components/effects/EffectFlowerRain.vue'
import EffectScanLine from '@/components/effects/EffectScanLine.vue'
import EffectChromaticAberration from '@/components/effects/EffectChromaticAberration.vue'
import EffectEchoTrail from '@/components/effects/EffectEchoTrail.vue'
import EffectCorruptedText from '@/components/effects/EffectCorruptedText.vue'
import EffectGlitchShift from '@/components/effects/EffectGlitchShift.vue'
import EffectLiquidDistort from '@/components/effects/EffectLiquidDistort.vue'
import EffectHalo from '@/components/effects/EffectHalo.vue'
import EffectLeaves from '@/components/effects/EffectLeaves.vue'
import EffectButterflies from '@/components/effects/EffectButterflies.vue'
import EffectOrbs from '@/components/effects/EffectOrbs.vue'
import EffectScaleCollapse from '@/components/effects/EffectScaleCollapse.vue'
import gsap from 'gsap'
import { SPECIAL_EFFECTS, WING_COLORS, DEFAULT_WING_COLOR, WING_LAYERS_MAP, LEVEL_UNLOCKS, PARTICLE_PRESETS, getParticleOptionsForLevel } from '@/components/effectConfig'

// [시니어 조치] 특수 효과 리스트
const specialEffects = SPECIAL_EFFECTS

// [시니어 조치] 실제 장착 중인 효과 코드 (UI 선택 상태 표시용)
const actualEquippedEffect = computed(() => {
  if (isMe.value && authStore.user) {
    return authStore.user.equippedEffect
  }
  return props.authorEquippedEffect || props.equippedEffect
})

// [시니어 조치] 실시간 반응성을 위해 스토어와 props를 통합 참조 (화면 렌더링용)
const displayEquippedEffect = computed(() => {
  // 1. 표시 여부 확인
  let isVisible = true
  
  // props가 명시적으로 전달된 경우 (true/false) 이를 최우선함
  if (props.showEquippedEffect !== null && props.showEquippedEffect !== undefined) {
    isVisible = props.showEquippedEffect
  } 
  // props가 없는데 본인일 경우 스토어 설정 참조
  else if (isMe.value && authStore.user && authStore.user.showEquippedEffect !== undefined) {
    isVisible = authStore.user.showEquippedEffect
  }
  
  // 효과가 꺼져 있으면 null 반환하여 렌더링 중단
  if (!isVisible) return null

  return actualEquippedEffect.value
})

// 이펙트별 색상 정의
const wingColors = computed(() => {
  const effect = actualEquippedEffect.value
  return WING_COLORS[effect] || DEFAULT_WING_COLOR
})

// [시니어 조치] 날개 레이어 개수 계산 (장착된 이펙트 코드 기준 고정)
const wingLayers = computed(() => {
  const effect = actualEquippedEffect.value
  return WING_LAYERS_MAP[effect] || 1
})

const props = defineProps({
  nickname: { type: String, required: true },
  userPublicId: { type: String, default: null },
  level: { type: Number, default: 1 },
  exp: { type: Number, default: 0 },
  badges: { type: Array, default: () => [] },
  showEffects: { type: Boolean, default: true }, // 기본 효과 표시 여부 (fallback)
  showLevelEffects: { type: Boolean, default: null }, // [표준] 백엔드 showLevelEffects 필드와 직접 매핑
  showAvatar: { type: Boolean, default: false },
  equippedBadgeName: { type: String, default: null },
  karmaPoint: { type: Number, default: 1000 },
  tooltipDirection: { type: String, default: 'right' },
  showEquippedEffect: { type: Boolean, default: null }, // [시니어 조치] 이펙트 효과 표시 설정 직접 매핑
  effectOnly: { type: Boolean, default: false }, // [시니어 조치] 닉네임 없이 이펙트만 렌더링하는 모드
  equippedTier: { type: Number, default: null }, // [시니어 조치] 장착 중인 티어 스타일
  equippedEffect: { type: String, default: null }, // 장착 중인 특수 효과 코드
  authorEquippedEffect: { type: String, default: null }, // [시니어 대응] 백엔드 DTO 필드명 호환용
  authorActive: { type: Boolean, default: true }, // [시니어 조치] 작성자 활성 상태 (탈퇴 여부) 추가
  ownedEffectCodes: { type: Array, default: () => [] }, // 사용자가 소유한 이펙트 목록 (user_effects 테이블)
  isEllipsis: { type: Boolean, default: false } // [시니어 조치] 모바일에서 선택적으로 말줄임 적용
})

const badgeStore = useBadgeStore()
const authStore = useAuthStore()
const uiStore = useUiStore()
const showTooltip = ref(false)
const nicknameRef = ref(null)

// 다크/라이트 테마 감지
const isDark = ref(document.documentElement.classList.contains('dark-mode'))
let themeObserver = null
const isMobileView = ref(window.innerWidth <= 768) // 모바일 여부 실시간 감지

const handleResize = () => {
  isMobileView.value = window.innerWidth <= 768
}

// [시니어 조치] 툴팁의 최종 표시 방향 결정
const computedDirection = computed(() => {
  if (isMobileView.value) return 'top' // 모바일은 무조건 상단 (날개 이펙트로 인한 가로 짤림 방지)
  // PC: 부모가 준 방향이 있으면 따르고, 없으면 기본 'right'
  return props.tooltipDirection || 'right'
})

const instanceId = Math.random().toString(36).slice(2, 8)
const isPinned = ref(false) 
const tooltipRef = ref(null)
const showMessageModal = ref(false)

const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event);
      }
    };
    document.addEventListener("click", el.clickOutsideEvent);
  },
  unmounted(el) {
    document.removeEventListener("click", el.clickOutsideEvent);
  },
};

const handleMouseEnter = () => {
  if (isPinned.value) return
  showTooltip.value = true
  animateIn()
}

const handleMouseLeave = () => {
  if (isPinned.value) return
  animateOut()
}

const handleToggleClick = () => {
  if (isPinned.value) {
    isPinned.value = false
    animateOut()
  } else {
    isPinned.value = true
    showTooltip.value = true
    animateIn()
  }
}

const closeTooltip = () => {
  if (showTooltip.value) {
    isPinned.value = false
    animateOut()
  }
}

const animateIn = () => {
  if (!badgeStore.isLoaded) badgeStore.fetchRules()
  nextTick(() => {
    if (!tooltipRef.value) return
    const el = tooltipRef.value
    const items = el.querySelectorAll(".stagger-item")
    gsap.fromTo(el, { opacity: 0, scale: 0.9, x: -10 }, { opacity: 1, scale: 1, x: 0, duration: 0.3, ease: "back.out(1.5)" })
    gsap.fromTo(items, { opacity: 0, y: 10 }, { opacity: 1, y: 0, duration: 0.4, stagger: 0.06, ease: "power2.out", delay: 0.1 })
  })
}

const animateOut = () => {
  if (tooltipRef.value) {
    gsap.to(tooltipRef.value, { 
      opacity: 0, scale: 0.9, duration: 0.2, 
      onComplete: () => { showTooltip.value = false } 
    })
  } else {
    showTooltip.value = false
  }
}

const isMe = computed(() => {
  return authStore.user && props.userPublicId && String(authStore.user.publicId) === String(props.userPublicId)
})

const isAdmin = computed(() => authStore.user?.role === 'ROLE_ADMIN')

// [시니어 조치] 본인일 경우 authStore.user의 ownedEffectCodes 사용, 아니면 props 사용
const effectCodes = computed(() => {
  let codes = []
  
  // 1. 보유한 이펙트 코드 목록 (DB 기반)
  if (isMe.value && authStore.user?.ownedEffectCodes) {
    codes = [...authStore.user.ownedEffectCodes]
  } else {
    codes = [...(props.ownedEffectCodes || [])]
  }

  // 2. [안전장치] 레벨 기반 자동 해금 보완
  // ownedEffectCodes가 로드되지 않은 경우나 누락된 경우를 위해 레벨로 한 번 더 체크
  const userLv = isMe.value && authStore.user ? authStore.user.level : props.level

  Object.entries(LEVEL_UNLOCKS).forEach(([lv, id]) => {
    if (userLv >= parseInt(lv) && !codes.includes(id)) {
      codes.push(id)
    }
  })

  return codes
})

// [시니어 조치] 닉네임 효과 표시 여부 실시간 동기화 (필드명: showLevelEffects)
const displayShowEffects = computed(() => {
  // 1. 백엔드에서 명시적으로 효과 표시 여부를 내려준 경우 (댓글/게시글 리스트 등) 이를 최우선함
  if (props.showLevelEffects !== null && props.showLevelEffects !== undefined) {
    // 단, '나'일 경우에는 마이페이지 토글 즉시 반영을 위해 스토어 상태를 우선함
    if (isMe.value && authStore.user && authStore.user.showLevelEffects !== undefined) {
      return authStore.user.showLevelEffects
    }
    return props.showLevelEffects
  }
  
  // 2. 백엔드 데이터가 없는 경우 (fallback)
  if (isMe.value && authStore.user && authStore.user.showLevelEffects !== undefined) {
    return authStore.user.showLevelEffects
  }
  
  return props.showEffects
})

const isWingsEffect = computed(() => displayEquippedEffect.value?.endsWith('_WINGS'))
const isBubblesEffect = computed(() => displayEquippedEffect.value === 'MAGIC_BUBBLES')
const isStarsEffect = computed(() => displayEquippedEffect.value === 'STARRY_NIGHT')
const isThunderEffect = computed(() => displayEquippedEffect.value === 'THUNDER_BLUE')
const isFlameEffect = computed(() => displayEquippedEffect.value === 'AURORA_FLAME')
const isIceFrostEffect = computed(() => displayEquippedEffect.value === 'ICE_FROST')
const isSakuraEffect = computed(() => displayEquippedEffect.value === 'SAKURA_BLOOM')
const isRosesEffect = computed(() => displayEquippedEffect.value === 'ROSES_BLOOM')
const isShadowEffect = computed(() => displayEquippedEffect.value === 'SHADOW_DEMON')
const isNeonEffect = computed(() => displayEquippedEffect.value === 'NEON_SIGN')
const isGlitchEffect = computed(() => displayEquippedEffect.value === 'PIXEL_GLITCH')
const isVoidEffect = computed(() => displayEquippedEffect.value === 'VOID_RIFT')
const isHeartEffect = computed(() => displayEquippedEffect.value === 'LOVE_HEART')
const isRainbowEffect = computed(() => displayEquippedEffect.value === 'RAINBOW_WAVE')
const isShootingStarEffect = computed(() => displayEquippedEffect.value === 'SHOOTING_STAR')
const isBombEffect = computed(() => displayEquippedEffect.value === 'BOMB')
const isCrownEffect = computed(() => displayEquippedEffect.value === 'CROWN')
const isSwordsCrossEffect = computed(() => displayEquippedEffect.value === 'SWORDS_CROSS')
const isFlowerCrownEffect = computed(() => displayEquippedEffect.value === 'FLOWER_CROWN')
const isStarTiaraEffect = computed(() => displayEquippedEffect.value === 'STAR_TIARA')
const isFlowerRainEffect = computed(() => displayEquippedEffect.value === 'FLOWER_RAIN')
const isScanLineEffect = computed(() => displayEquippedEffect.value === 'SCAN_LINE')
const isChromaticAberrationEffect = computed(() => displayEquippedEffect.value === 'CHROMATIC_ABERRATION')
const isEchoTrailEffect = computed(() => displayEquippedEffect.value === 'ECHO_TRAIL')
const isCorruptedTextEffect = computed(() => displayEquippedEffect.value === 'CORRUPTED_TEXT')
const isGlitchShiftEffect = computed(() => displayEquippedEffect.value === 'GLITCH_SHIFT')
const isLiquidDistortEffect = computed(() => displayEquippedEffect.value === 'LIQUID_DISTORT')
const isHaloEffect = computed(() => displayEquippedEffect.value === 'HALO')
const isLeavesEffect = computed(() => displayEquippedEffect.value === 'LEAVES')
const isButterfliesEffect = computed(() => displayEquippedEffect.value === 'BUTTERFLIES')
const isOrbsEffect = computed(() => displayEquippedEffect.value === 'ORBS')
const isScaleCollapseEffect = computed(() => displayEquippedEffect.value === 'SCALE_COLLAPSE')

// [드래그 스크롤 로직]
const scrollContainer = ref(null)
const isDragging = ref(false)
const startX = ref(0)
const scrollLeftState = ref(0)

const handleDragStart = (e) => {
  if (isUpdatingEffect.value) return 
  isDragging.value = true
  startX.value = e.pageX - scrollContainer.value.offsetLeft
  scrollLeftState.value = scrollContainer.value.scrollLeft
  scrollContainer.value.style.cursor = 'grabbing'
}

const handleDragMove = (e) => {
  if (!isDragging.value) return
  e.preventDefault()
  const x = e.pageX - scrollContainer.value.offsetLeft
  const walk = (x - startX.value) * 2
  scrollContainer.value.scrollLeft = scrollLeftState.value - walk
}

const handleDragEnd = () => {
  isDragging.value = false
  if (scrollContainer.value) scrollContainer.value.style.cursor = 'grab'
}

const openMessageModal = () => {
  if (!props.userPublicId) {
    uiStore.showAlert('탈퇴한 사용자에게는 쪽지를 보낼 수 없습니다.', '안내');
    return;
  }
  isPinned.value = false
  showTooltip.value = false
  showMessageModal.value = true
}

const isUpdatingEffect = ref(false)
let debounceTimer = null

const updateEffect = (effectId, event) => {
  if (!isMe.value) return
  
  // UI 및 전역 스토어 즉시 업데이트
  if (authStore.user) {
    authStore.user.equippedEffect = effectId
    // [시니어 조치] 로컬 스토리지 동기화 (새로고침 유지용)
    localStorage.setItem('user', JSON.stringify(authStore.user))
  }

  // 클릭 애니메이션
  if (event && event.currentTarget) {
    gsap.fromTo(event.currentTarget, 
      { scale: 0.85 }, 
      { scale: 1, duration: 0.4, ease: "elastic.out(1.2, 0.5)" }
    )
  }

  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    isUpdatingEffect.value = true
    try {
      await axios.patch(`/api/users/${authStore.user.publicId}/effect`, null, { params: { effectCode: effectId } })
    } catch (e) {
      uiStore.showAlert('효과 저장 실패', '안내')
    } finally {
      isUpdatingEffect.value = false
    }
  }, 500)
}

const onMessageSuccess = () => {}

const showParticles = computed(() => displayShowEffects.value && (props.level >= 50))
const particleOptions = computed(() => getParticleOptionsForLevel(props.level))

const currentTierClass = computed(() => {
  if (!displayShowEffects.value) return 'tier-none'
  
  // 1. 장착된 티어 스타일 확인
  let tierValue = props.equippedTier
  if (tierValue === null || tierValue === undefined) {
    if (isMe.value && authStore.user && authStore.user.equippedTier !== undefined) {
      tierValue = authStore.user.equippedTier
    }
  }

  // 2. 장착된 티어가 없으면 현재 레벨 기준 자동 티어
  if (tierValue === null || tierValue === undefined) {
    tierValue = badgeStore.getAccountTierNumber(props.level)
  }

  return `tier-${tierValue}`
})

const nextLevelExp = computed(() => props.level * props.level * 50)
const expPercentage = computed(() => {
  const prevLevelExp = props.level > 1 ? (props.level - 1) * (props.level - 1) * 50 : 0
  const currentLevelMax = nextLevelExp.value - prevLevelExp
  const currentEarned = props.exp - prevLevelExp
  return Math.min(Math.max((currentEarned / currentLevelMax) * 100, 0), 100)
})
const mainBadge = computed(() => (props.badges?.length > 0) ? props.badges[0] : null)

const nicknameTextColor = computed(() => {
  const tier = currentTierClass.value
  if (tier === 'tier-1' || tier === 'tier-none') {
    return isDark.value ? '#f5f5f5' : '#262626'
  }
  return null
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (!badgeStore.isLoaded) badgeStore.fetchRules()
  themeObserver = new MutationObserver(() => {
    isDark.value = document.documentElement.classList.contains('dark-mode')
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => { 
  window.removeEventListener('resize', handleResize)
  if (themeObserver) themeObserver.disconnect() 
})
</script>

<style scoped>
.animated-nickname-wrapper { position: relative; display: inline-flex; align-items: center; cursor: pointer; gap: 5px; }

.animated-nickname {
  font-size: 0.76rem; font-weight: 850; display: inline-block; transition: all 0.3s ease; position: relative; z-index: 2; white-space: nowrap; letter-spacing: -0.03em;
  padding: 2px 8px; border-radius: 6px; border: 1.5px solid var(--tier-color);
  overflow: hidden; color: var(--text-primary); user-select: none;
}

.nickname-particles-container { position: absolute; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; z-index: 0; overflow: hidden; }

.nickname-with-effects { display: flex; align-items: center; position: relative; gap: 4px; padding: 0 4px; }



/* 6. Aurora Flame — 태양 코로나 불꽃 테두리 (세로 불꽃 없음, box-shadow 전용) */
.has-flame .animated-nickname {
  border-color: #ff6600 !important;
  animation: fire-corona 5s ease-in-out infinite alternate !important;
}
@keyframes fire-corona {
  0% {
    box-shadow:
      0 0 0 2px  rgba(255, 200, 60,  0.9),
      0 0 4px 4px  rgba(255, 110,  0, 0.75),
      0 0 9px 7px  rgba(240,  50,  0, 0.52),
      0 0 16px 10px rgba(200,  20,  0, 0.32),
      0 0 26px 14px rgba(160,   5,  0, 0.16);
  }
  33% {
    box-shadow:
      0 0 0 2px  rgba(255, 240, 100, 0.95),
      0 0 6px 5px  rgba(255, 150, 20, 0.8),
      0 0 13px 9px  rgba(255,  70,  0, 0.52),
      0 0 20px 12px rgba(210,  30,  0, 0.3),
      0 0 30px 16px rgba(170,  10,  0, 0.14);
  }
  66% {
    box-shadow:
      0 0 0 2px  rgba(255,  80, 10, 0.88),
      0 0 5px 4px  rgba(255, 100,  0, 0.72),
      0 0 11px 8px  rgba(240,  40,  0, 0.5),
      0 0 18px 11px rgba(195,  15,  0, 0.28),
      0 0 28px 15px rgba(155,   5,  0, 0.13);
  }
  100% {
    box-shadow:
      0 0 0 3px  rgba(255, 220, 50,  1),
      0 0 8px 6px  rgba(255, 160,  0, 0.82),
      0 0 16px 11px rgba(255,  80,  0, 0.56),
      0 0 24px 14px rgba(220,  40,  0, 0.32),
      0 0 36px 18px rgba(180,  10,  0, 0.16);
  }
}


/* 10. NEON_SIGN — 형광 네온 깜박임 (CSS only) */
.has-neon .animated-nickname { border-color: #00f5ff !important; animation: neon-flicker 3.5s steps(1) infinite !important; }
@keyframes neon-flicker {
  0%, 18%, 22%, 27%, 55%, 59%, 100% {
    box-shadow: 0 0 4px #00f5ff, 0 0 10px #00f5ff, 0 0 22px rgba(0,245,255,0.6), 0 0 40px rgba(0,245,255,0.3);
    border-color: #00f5ff !important;
  }
  19%, 25%, 56%, 58% {
    box-shadow: none;
    border-color: rgba(0,245,255,0.2) !important;
  }
}

/* 11. PIXEL_GLITCH — 디지털 글리치 */
.has-glitch .animated-nickname { animation: glitch-base 2s ease-in-out infinite !important; position: relative; }
/* inner-shine이 닉네임 밖으로 나가지 않도록 클리핑 유지 */
.has-glitch .animated-nickname .inner-shine-effect { clip-path: inset(0); }
.has-glitch .animated-nickname::before,
.has-glitch .animated-nickname::after {
  content: attr(data-text);
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  padding: 2px 8px; border-radius: 6px;
  pointer-events: none; white-space: nowrap;
  font-size: inherit; font-weight: inherit; letter-spacing: inherit;
}
.has-glitch .animated-nickname::before {
  animation: glitch-slice-r 2s ease-in-out infinite;
  color: #ff00ea !important; -webkit-text-fill-color: #ff00ea !important;
  clip-path: polygon(0 15%, 100% 15%, 100% 38%, 0 38%);
  z-index: 3;
}
.has-glitch .animated-nickname::after {
  animation: glitch-slice-b 2s ease-in-out infinite;
  color: #00ffff !important; -webkit-text-fill-color: #00ffff !important;
  clip-path: polygon(0 58%, 100% 58%, 100% 76%, 0 76%);
  z-index: 3;
}
@keyframes glitch-base {
  0%, 78%, 100% { transform: translateX(0) skewX(0); filter: none; }
  80% { transform: translateX(-3px) skewX(-3deg); filter: brightness(1.4); }
  82% { transform: translateX(3px) skewX(1deg); }
  84% { transform: translateX(-1px); filter: none; }
  86% { transform: translateX(0) skewX(0); }
  91% { transform: translateX(-1px) skewX(-1deg); }
}
@keyframes glitch-slice-r {
  0%, 78%, 100% { opacity: 0; transform: translateX(0); }
  80%, 84% { opacity: 0.85; transform: translateX(-5px); }
  82% { transform: translateX(-3px); }
  86% { opacity: 0; }
}
@keyframes glitch-slice-b {
  0%, 78%, 100% { opacity: 0; transform: translateX(0); }
  80%, 84% { opacity: 0.85; transform: translateX(5px); }
  82% { transform: translateX(3px); }
  86% { opacity: 0; }
}


/* --- Tier CSS Variables --- */
.tier-1 { --tier-color: var(--text-secondary); --tier-bg: rgba(0,0,0,0.03); --tier-gradient: var(--text-primary); }
.tier-5 { --tier-color: #a0522d; --tier-bg: rgba(160, 82, 45, 0.1); --tier-gradient: linear-gradient(135deg, #804a00, #a0522d, #804a00); }
.tier-10 { --tier-color: #94a3b8; --tier-bg: rgba(148, 163, 184, 0.12); --tier-gradient: linear-gradient(135deg, #94a3b8, #f8fafc, #94a3b8); }
.tier-30 { --tier-color: #facc15; --tier-bg: rgba(250, 204, 21, 0.08); --tier-gradient: linear-gradient(135deg, #a16207, #fde047, #a16207); }
.tier-50 { --tier-color: #10b981; --tier-bg: rgba(16, 185, 129, 0.08); --tier-gradient: linear-gradient(135deg, #065f46, #34d399, #065f46); }
.tier-70 { --tier-color: #ff416c; --tier-bg: rgba(225, 29, 72, 0.06); --tier-gradient: linear-gradient(90deg, #ff416c, #ffd700, #48bb78, #4facfe, #9400d3); }
.tier-90 { --tier-color: #22d3ee; --tier-bg: rgba(34, 211, 238, 0.06); --tier-gradient: linear-gradient(135deg, #0891b2, #ffffff, #0891b2); }
.tier-100 { --tier-color: #facc15; --tier-bg: rgba(0, 0, 0, 0.95); --tier-gradient: linear-gradient(to right, #facc15, #ffffff, #facc15); }

@media (prefers-color-scheme: dark) { .tier-10 { --tier-color: #cbd5e1; --tier-bg: rgba(203, 213, 225, 0.12); --tier-gradient: linear-gradient(135deg, #94a3b8, #f8fafc, #94a3b8); } }

.animated-nickname.tier-1 { color: var(--text-primary); border: none; padding: 0; overflow: visible; }
.animated-nickname.tier-50 { background: var(--tier-gradient); -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; background-color: var(--tier-bg); }
.animated-nickname.tier-5 { background: linear-gradient(135deg, #6b3a1f 0%, #c68642 35%, #e8a96a 55%, #c68642 75%, #6b3a1f 100%); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #1a0800 !important; border-color: #a0622a; }
.animated-nickname.tier-10 { background: linear-gradient(135deg, #5a5a5a 0%, #b8b8b8 35%, #efefef 55%, #b8b8b8 75%, #5a5a5a 100%); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #111 !important; border-color: #aaaaaa; }
.animated-nickname.tier-30 { background: linear-gradient(135deg, #6b4900 0%, #c8920a 35%, #ffd700 55%, #c8920a 75%, #6b4900 100%); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #1a0e00 !important; border-color: #c8920a; }

.animated-nickname.tier-50 { animation: emerald-aura-pulse 2s infinite alternate; }
@keyframes emerald-aura-pulse { from { box-shadow: 0 0 5px rgba(16, 185, 129, 0.3); } to { box-shadow: 0 0 15px rgba(16, 185, 129, 0.6); } }
.inner-shine-effect { position: absolute; top: 0; left: -100%; width: 50%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent); transform: skewX(-20deg); pointer-events: none; animation: inner-glint 3s infinite; z-index: 1; }
@keyframes inner-glint { 0% { left: -120%; } 30% { left: 120%; } 100% { left: 120%; } }

.animated-nickname.tier-70, .animated-nickname.tier-90, .animated-nickname.tier-100 { background: var(--tier-gradient); background-size: 200% auto; -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; position: relative; animation: shine-move 3s linear infinite; background-color: var(--tier-bg); }
.animated-nickname.tier-70 { animation: shine-move 3s linear infinite, rainbow-aura-pulse 3s infinite alternate; }
.animated-nickname.tier-70::before { content: ""; position: absolute; inset: 0; border-radius: 6px; padding: 1px; background: var(--tier-gradient); -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0); -webkit-mask-composite: xor; mask-composite: exclude; pointer-events: none; z-index: 3; }
.animated-nickname.tier-90 { animation: shine-move 2.5s linear infinite, cold-burn 1.5s infinite alternate; }
.animated-nickname.tier-100 { font-size: 0.85rem; border: 2px solid var(--tier-color); animation: shine-move 2s linear infinite, legendary-burn 1.2s infinite alternate; }

@keyframes shine-move { to { background-position: 200% center; } }
@keyframes cold-burn { from { box-shadow: 0 0 4px rgba(34, 211, 238, 0.4); } to { box-shadow: 0 0 12px rgba(34, 211, 238, 0.8); } }
@keyframes legendary-burn { from { box-shadow: 0 0 8px rgba(250, 204, 21, 0.5); } to { box-shadow: 0 0 20px rgba(250, 204, 21, 0.9); } }
@keyframes rainbow-aura-pulse { from { box-shadow: 0 0 8px rgba(255, 65, 108, 0.3); } to { box-shadow: 0 0 18px rgba(255, 65, 108, 0.6); } }

.an-avatar { width: 26px; height: 26px; border-radius: 50%; flex-shrink: 0; background: var(--tier-bg, rgba(0,0,0,0.03)); border: 1.5px solid var(--tier-color, var(--border-color, #dbdbdb)); display: inline-flex; align-items: center; justify-content: center; position: relative; overflow: visible; transition: border-color 0.3s; }
.an-av-initial { font-size: 0.72rem; font-weight: 900; background: var(--tier-gradient, var(--text-primary)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; line-height: 1; }
.an-avatar.tier-1 .an-av-initial, .an-avatar.tier-none .an-av-initial { -webkit-text-fill-color: var(--text-secondary, #8e8e8e); background: none; }

.an-av-p { position: absolute; border-radius: 50%; pointer-events: none; opacity: 0; z-index: 10; width: 3px; height: 3px; }
.an-av-p.p1 { left: 5%; bottom: 10%; animation: an-float 2.2s 0s infinite; }
.an-av-p.p2 { left: 35%; bottom: 0; animation: an-float 2.2s 0.55s infinite; }
.an-av-p.p3 { left: 65%; bottom: 0; animation: an-float 2.2s 1.1s infinite; }
.an-av-p.p4 { left: 90%; bottom: 10%; animation: an-float 2.2s 1.65s infinite; }
@keyframes an-float { 0% { opacity: 0; transform: translateY(0) scale(0.7); } 20% { opacity: 0.9; } 100% { opacity: 0; transform: translateY(-22px) scale(0.2); } }
.tier-50 .an-av-p { background: #10b981; }
.tier-70 .an-av-p.p1 { background: #ff416c; }
.tier-70 .an-av-p.p2 { background: #ffd700; }
.tier-70 .an-av-p.p3 { background: #4facfe; }
.tier-70 .an-av-p.p4 { background: #9400d3; }
.tier-90 .an-av-p { background: #e0f7ff; border-radius: 1px; }
.tier-90 .an-av-p.p1 { animation: an-snow 3s 0s infinite; }
.tier-90 .an-av-p.p2 { animation: an-snow 3s 0.75s infinite; }
.tier-90 .an-av-p.p3 { animation: an-snow 3s 1.5s infinite; }
.tier-90 .an-av-p.p4 { animation: an-snow 3s 2.25s infinite; }
@keyframes an-snow { 0% { opacity: 0; transform: translateY(0) translateX(0) rotate(0deg) scale(0.8); } 20% { opacity: 0.8; } 100% { opacity: 0; transform: translateY(-20px) translateX(-2px) rotate(180deg) scale(0.2); } }
.tier-100 .an-av-p { background: #facc15; width: 4px; height: 4px; box-shadow: 0 0 3px rgba(250,204,21,0.7); }
.tier-100 .an-av-p.p1 { animation-duration: 1.5s; }
.tier-100 .an-av-p.p2 { animation-duration: 1.5s; }
.tier-100 .an-av-p.p3 { animation-duration: 1.5s; }
.tier-100 .an-av-p.p4 { animation-duration: 1.5s; }

.nickname-tooltip { position: absolute; width: 220px; max-width: 90vw; background: var(--card-bg) !important; background-image: linear-gradient(to bottom, var(--card-bg), var(--tier-bg)) !important; backdrop-filter: blur(20px); border: 1.5px solid var(--tier-color); border-radius: 16px; padding: 14px; box-shadow: 0 15px 40px rgba(0,0,0,0.3); z-index: 9999999; cursor: default; overflow: hidden; transition: opacity 0.3s, transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); pointer-events: auto; }
.nickname-tooltip.right { left: calc(100% + 15px); top: 50%; transform: translateY(-50%); }
.nickname-tooltip.top { bottom: calc(100% + 15px); left: 50%; transform: translateX(-50%); transform-origin: bottom center; }
.nickname-tooltip.top::after { content: ""; position: absolute; top: 100%; left: 50%; transform: translateX(-50%); border-width: 8px; border-style: solid; border-color: var(--tier-color) transparent transparent transparent; }

.tooltip-content-inner { position: relative; z-index: 10; }
.nickname-tooltip *:not(.user-name-styled) { -webkit-text-fill-color: initial !important; color: var(--tier-color) !important; background-clip: initial !important; -webkit-background-clip: initial !important; }

.user-name-styled { font-weight: 900; font-size: 0.85rem; display: inline-block; background: var(--tier-gradient); background-size: 200% auto; -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; }
.user-name-styled.tier-70, .user-name-styled.tier-90, .user-name-styled.tier-100 { animation: shine-move 3s linear infinite; }
.user-name-styled.tier-5  { background: linear-gradient(135deg, #6b3a1f, #c68642, #e8a96a, #c68642, #6b3a1f); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #1a0800 !important; padding: 1px 6px; border-radius: 4px; }
.user-name-styled.tier-10 { background: linear-gradient(135deg, #5a5a5a, #b8b8b8, #efefef, #b8b8b8, #5a5a5a); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #111 !important; padding: 1px 6px; border-radius: 4px; }
.user-name-styled.tier-30 { background: linear-gradient(135deg, #6b4900, #c8920a, #ffd700, #c8920a, #6b4900); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #1a0e00 !important; padding: 1px 6px; border-radius: 4px; }

.glass-reflection-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(135deg, transparent 45%, rgba(255, 255, 255, 0.15) 50%, transparent 55%); background-size: 300% 300%; z-index: 5; pointer-events: none; animation: sharpShine 6s infinite; }
.nickname-tooltip.tier-50 { border-color: var(--tier-color); box-shadow: 0 0 20px rgba(16, 185, 129, 0.2); animation: tooltip-emerald-glow 3s infinite alternate; }
@keyframes tooltip-emerald-glow { from { box-shadow: 0 0 10px rgba(16, 185, 129, 0.2); } to { box-shadow: 0 0 20px rgba(16, 185, 129, 0.4); } }
.nickname-tooltip.tier-70 { border: 1px solid transparent; background-image: linear-gradient(var(--tier-bg), var(--tier-bg)), var(--tier-gradient); background-origin: border-box; background-clip: content-box, border-box; }
.nickname-tooltip.tier-100 { background: #000 !important; border: 2px solid var(--tier-color); box-shadow: 0 0 25px rgba(250, 204, 21, 0.4); }

.user-level { font-size: 0.7rem; font-weight: 800; color: var(--tier-color) !important; }
.main-title-box { display: flex; flex-direction: column; gap: 2px; background: rgba(255, 255, 255, 0.03); padding: 6px 10px; border-radius: 8px; margin-bottom: 8px; border: 1.2px solid var(--tier-color); }
.karma-warning-icon-outer { margin-right: 4px; font-size: 0.8rem; filter: drop-shadow(0 0 2px rgba(0,0,0,0.3)); display: inline-block; vertical-align: middle; position: relative; z-index: 5; }
.karma-section-tooltip { background: rgba(0,0,0,0.05); padding: 6px 8px; border-radius: 6px; margin-bottom: 10px; border-left: 2.5px solid var(--tier-color); }
.karma-header-mini { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.k-label { font-size: 0.55rem !important; font-weight: 800; opacity: 0.7; }
.k-value { font-size: 0.7rem !important; font-weight: 900; }
.k-desc { font-size: 0.55rem !important; color: #e67e22 !important; margin: 2px 0 0 0; line-height: 1.2; font-weight: 600; }
.title-label { font-size: 0.55rem !important; color: var(--tier-color) !important; opacity: 0.7; font-weight: 850; text-transform: uppercase; display: block; margin-bottom: 1px; }
.title-value { font-size: 0.75rem !important; font-weight: 950; color: var(--tier-color) !important; letter-spacing: -0.02em; }
.exp-bar-fill { height: 100%; border-radius: 3px; background: var(--tier-color); transition: width 0.5s ease; }
.exp-bar-fill.tier-70, .exp-bar-fill.tier-90, .exp-bar-fill.tier-100 { background: var(--tier-gradient); background-size: 200% auto; animation: shine-move 2s linear infinite; }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; background: #444; color: white !important; display: flex; align-items: center; justify-content: center; font-size: 1rem; font-weight: bold; flex-shrink: 0; }
.user-avatar.tier-5, .user-avatar.tier-10, .user-avatar.tier-30, .user-avatar.tier-50, .user-avatar.tier-90 { background: var(--tier-color); }
.user-avatar.tier-70 { background: var(--tier-gradient); background-size: 200% auto; animation: shine-move 3s linear infinite; }
.user-avatar.tier-100 { background: linear-gradient(to bottom, #facc15, #f59e0b); }
.tooltip-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.user-info-basic { display: flex; flex-direction: column; gap: 1px; }
.exp-section { display: flex; flex-direction: column; gap: 6px; }
.exp-labels { display: flex; justify-content: space-between; font-size: 0.6rem; color: var(--tier-color) !important; opacity: 0.9; font-weight: 800; }
.exp-bar-bg { width: 100%; height: 5px; background: rgba(0, 0, 0, 0.05); border-radius: 3px; overflow: hidden; }
.tooltip-actions { margin-top: 12px; }
.msg-send-btn { width: 100%; padding: 6px; border-radius: 8px; border: 1.2px solid var(--tier-color); background: transparent; color: var(--tier-color) !important; font-size: 0.7rem; font-weight: 850; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.msg-send-btn:hover { background: var(--tier-color); color: white !important; }
.beta-badge { font-size: 0.6rem; background: linear-gradient(135deg, #3b82f6, #2563eb); color: white !important; padding: 1px 4px; border-radius: 4px; margin-left: 4px; font-weight: 800; vertical-align: middle; box-shadow: 0 0 5px rgba(37, 99, 235, 0.4); text-transform: uppercase; }
/* [시니어 조치] 툴팁 내 이펙트 설정 스타일 */
.tooltip-effect-settings {
  margin-top: 15px; padding-top: 12px; border-top: 1px dashed var(--tier-color); display: flex; flex-direction: column; gap: 8px;
}
.effect-header-mini { display: flex; justify-content: space-between; align-items: center; }
.effect-label-mini { font-size: 0.55rem !important; font-weight: 850; opacity: 0.6; color: var(--tier-color) !important; letter-spacing: 0.05em; }
.effect-lock-guide { font-size: 0.5rem !important; color: #ff4d4d !important; font-weight: 800; }

.effect-scroll-viewport {
  overflow-x: auto; scrollbar-width: none; -ms-overflow-style: none;
  cursor: grab; padding: 2px 0; margin: 0 -4px;
}
.effect-scroll-viewport::-webkit-scrollbar { display: none; }

.effect-toggle-group { display: flex; gap: 6px; padding: 0 4px; width: max-content; }
.effect-toggle-btn { 
  flex-shrink: 0; width: 34px; height: 34px; border-radius: 10px; border: 1.5px solid var(--tier-color); background: var(--card-bg); cursor: pointer; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); display: flex; align-items: center; justify-content: center; font-size: 1.1rem; position: relative; overflow: visible;
}
.effect-toggle-btn.active { 
  border-width: 2px; border-color: var(--tier-color); 
  box-shadow: 0 0 10px var(--tier-color), inset 0 0 5px rgba(255,255,255,0.2);
  transform: scale(1.05);
  z-index: 2;
}
.effect-toggle-btn.locked { opacity: 0.3; filter: grayscale(1); cursor: not-allowed; border-style: dashed; }
.effect-toggle-btn:not(.active):not(.locked):hover { background: var(--hover-bg); transform: translateY(-2px); border-color: var(--tier-color); }

.active-check {
  position: absolute; right: -4px; bottom: -4px; width: 12px; height: 12px; background: #22c55e; color: white; border-radius: 50%; font-size: 8px; display: flex; align-items: center; justify-content: center; font-weight: 950; border: 1.5px solid var(--card-bg); z-index: 10; box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.effect-toggle-btn:disabled { cursor: wait; opacity: 0.7; }

.lock-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 0.6rem; background: rgba(0,0,0,0.2);
}

@media (max-width: 768px) {
  .animated-nickname.is-ellipsis { 
    max-width: 85px; /* 6글자 내외에서 말줄임 발생하도록 제한 */
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    display: inline-block;
    vertical-align: middle;
  }
  .nickname-tooltip { width: 200px; padding: 12px; border-radius: 12px; }
  .nickname-tooltip.top { 
    bottom: calc(100% + 10px); 
    left: 20px; /* 왼쪽 아바타/여백 고려하여 시작점 고정 */
    transform: translateX(0) scale(0.9) !important; 
    transform-origin: bottom left;
  }
  .nickname-tooltip.top::after {
    left: 30px; /* 꼬리표(화살표)도 툴팁의 왼쪽 부분에 맞춤 */
    transform: translateX(0);
  }
  .nickname-tooltip.right { transform: translateY(-50%) scale(0.9) !important; margin-left: -5px; }
  .user-avatar { width: 28px; height: 32px; font-size: 0.9rem; }
  .user-name-styled { font-size: 0.8rem; }
}

/* 색상 분리 효과 */
.has-chromatic .animated-nickname { animation: chromatic-shift 2.5s ease-in-out infinite; text-shadow: -2px 0 #ff0000, 2px 0 #00ff00, 0 -2px #0000ff; }
@keyframes chromatic-shift { 0%, 100% { text-shadow: -2px 0 #ff0000, 2px 0 #00ff00, 0 -2px #0000ff; } 50% { text-shadow: -3px 0 #ff0000, 3px 0 #00ff00, 0 -3px #0000ff; } }

/* 잔상 효과 */
.has-echo .animated-nickname { animation: echo-wave 2s ease-in-out infinite; position: relative; }
.has-echo .animated-nickname::before { content: attr(data-text); position: absolute; left: 0; top: 0; opacity: 0; animation: echo-ghost 2s ease-in-out infinite; }
.has-echo .animated-nickname::after { content: attr(data-text); position: absolute; left: 0; top: 0; opacity: 0; animation: echo-ghost 2s ease-in-out infinite 0.3s; }
@keyframes echo-wave { 0%, 100% { transform: translateX(0); } 50% { transform: translateX(3px); } }
@keyframes echo-ghost { 0% { opacity: 0; transform: translateX(-8px); } 20% { opacity: 0.6; } 50% { opacity: 0.3; transform: translateX(0); } 100% { opacity: 0; transform: translateX(8px); } }

/* 손상된 텍스트 효과 */
.has-corrupted .animated-nickname { animation: text-corrupt 3s ease-in-out infinite; }
.has-corrupted .animated-nickname::before { content: attr(data-text); position: absolute; left: 0; top: 0; opacity: 0; color: #ff0000; animation: corrupt-glitch 3s ease-in-out infinite; clip-path: polygon(0 0, 100% 0, 100% 45%, 0 45%); }
@keyframes text-corrupt { 0%, 100% { opacity: 1; transform: skewX(0); } 20% { opacity: 0.8; transform: skewX(-2deg); } 40% { opacity: 1; } 55% { opacity: 0.3; transform: skewX(3deg); } 65% { opacity: 1; transform: skewX(0); } }
@keyframes corrupt-glitch { 0%, 100% { opacity: 0; } 20% { opacity: 0.7; } 40% { opacity: 0; } 55% { opacity: 0.8; } 65% { opacity: 0; } }

/* 글리치 이동 효과 */
.has-glitch-shift .animated-nickname { animation: glitch-shake 0.4s ease-in-out infinite; }
.has-glitch-shift .animated-nickname::before { content: attr(data-text); position: absolute; left: 0; top: 0; opacity: 0; color: #ff00ff; animation: glitch-offset-left 0.4s ease-in-out infinite; clip-path: polygon(0 0, 100% 0, 100% 30%, 0 30%); }
.has-glitch-shift .animated-nickname::after { content: attr(data-text); position: absolute; left: 0; top: 0; opacity: 0; color: #00ffff; animation: glitch-offset-right 0.4s ease-in-out infinite; clip-path: polygon(0 70%, 100% 70%, 100% 100%, 0 100%); }
@keyframes glitch-shake { 0% { transform: translateX(-2px); } 25% { transform: translateX(2px); } 50% { transform: translateX(-3px); } 75% { transform: translateX(3px); } 100% { transform: translateX(0); } }
@keyframes glitch-offset-left { 0%, 25%, 50%, 75%, 100% { opacity: 0; } 10% { opacity: 0.8; transform: translateX(-3px); } 30% { opacity: 0; } 60% { opacity: 0.8; transform: translateX(-4px); } }
@keyframes glitch-offset-right { 0%, 25%, 50%, 75%, 100% { opacity: 0; } 20% { opacity: 0.8; transform: translateX(3px); } 40% { opacity: 0; } 70% { opacity: 0.8; transform: translateX(4px); } }

/* 액체 흐름 효과 */
.has-liquid .animated-nickname { animation: liquid-wave 3s ease-in-out infinite; filter: blur(0.3px); }
@keyframes liquid-wave { 0%, 100% { transform: skewY(0); letter-spacing: 0; } 15% { transform: skewY(-1deg); letter-spacing: 1px; } 30% { transform: skewY(1deg); letter-spacing: -1px; } 45% { transform: skewY(-0.5deg); letter-spacing: 0.5px; } 60% { transform: skewY(0.5deg); letter-spacing: -0.5px; } 75% { transform: skewY(-1deg); letter-spacing: 1px; } 90% { transform: skewY(0); letter-spacing: 0; } }

/* 축소 사라지기 효과 */
.has-scale-collapse .animated-nickname { animation: scale-collapse-effect 2.5s ease-in-out infinite; transform-origin: center center; }
@keyframes scale-collapse-effect { 0% { opacity: 1; transform: scale(1); } 35% { opacity: 1; transform: scale(1); } 50% { opacity: 0; transform: scale(0.1); } 75% { opacity: 0; transform: scale(0.1); } 100% { opacity: 1; transform: scale(1); } }
</style>
