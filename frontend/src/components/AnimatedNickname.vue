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
    <div v-if="showParticles && particleOptions" class="nickname-particles-container">
      <vue-particles :id="`particles-${instanceId}`" :options="particleOptions" />
    </div>

    <!-- [시니어] 카르마 주의 아이콘 -->
    <span v-if="Number(karmaPoint) <= 800" class="karma-warning-icon-outer" title="활동 신뢰 점수 주의">⚠️</span>

    <!-- 화려한 닉네임 표시부 (클릭 시 토글) -->
    <span :class="['animated-nickname', currentTierClass]" :style="nicknameTextColor ? { color: nicknameTextColor } : {}" @click.stop="handleToggleClick">
      <span v-if="level >= 50" class="inner-shine-effect"></span>
      {{ nickname }}
    </span>

    <!-- 미니 프로필 툴팁 -->
    <div v-if="showTooltip" ref="tooltipRef" :class="['nickname-tooltip', currentTierClass, tooltipDirection]" @click.stop>
      <div v-if="level >= 30" class="glass-reflection-overlay"></div>
      
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

        <div class="karma-section-tooltip stagger-item" :class="{ 'warning': karmaPoint <= 800 }">
          <div class="karma-header-mini">
            <span class="k-label">활동 신뢰 점수</span>
            <span class="k-value">{{ (karmaPoint / 10).toFixed(1) }} P</span>
          </div>
          <p v-if="karmaPoint <= 800" class="k-desc">신뢰 점수 저하 주의</p>
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

        <div class="tooltip-actions stagger-item" v-if="userId && !isMe">
          <button class="msg-send-btn" @click.stop="openMessageModal">
            <span>💌 쪽지 보내기</span>
          </button>
        </div>
      </div>
    </div>
<!-- 쪽지 발송 모달 -->
<MessageSendModal 
  v-if="userId && showMessageModal"
  :show="showMessageModal" 
  :receiver-id="userId" 
  :receiver-nickname="nickname"
  @close="showMessageModal = false"
  @success="onMessageSuccess"
/>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useBadgeStore } from '@/stores/badge'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import MessageSendModal from '@/components/MessageSendModal.vue'
import gsap from 'gsap'

const props = defineProps({
  nickname: { type: String, required: true },
  userId: { type: [Number, String], default: null },
  level: { type: Number, default: 1 },
  exp: { type: Number, default: 0 },
  badges: { type: Array, default: () => [] },
  showEffects: { type: Boolean, default: true },
  showAvatar: { type: Boolean, default: false },
  equippedBadgeName: { type: String, default: null },
  karmaPoint: { type: Number, default: 1000 },
  tooltipDirection: { type: String, default: 'right' }
})

const badgeStore = useBadgeStore()
const authStore = useAuthStore()
const uiStore = useUiStore()
const showTooltip = ref(false)

// 다크/라이트 테마 감지
const isDark = ref(document.documentElement.classList.contains('dark-mode'))
let themeObserver = null
const instanceId = Math.random().toString(36).slice(2, 8)
const isPinned = ref(false) 
const tooltipRef = ref(null)
const showMessageModal = ref(false)

// [커스텀 디렉티브] 외부 클릭 감지
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
  return authStore.user && props.userId && String(authStore.user.id) === String(props.userId)
})

const openMessageModal = () => {
  if (!props.userId) {
    uiStore.showAlert('탈퇴한 사용자에게는 쪽지를 보낼 수 없습니다.', '안내');
    return;
  }
  isPinned.value = false
  showTooltip.value = false
  showMessageModal.value = true
}

const onMessageSuccess = () => {}

const showParticles = computed(() => props.showEffects && props.level >= 50)
const particleOptions = computed(() => {
  const lv = props.level
  if (lv >= 100) return { fullScreen: { enable: false }, particles: { number: { value: 15 }, color: { value: "#facc15" }, size: { value: { min: 1, max: 2.5 } }, move: { enable: true, speed: 1.2, direction: "top" } } }
  if (lv >= 90) return { fullScreen: { enable: false }, particles: { number: { value: 10 }, color: { value: "#22d3ee" }, shape: { type: "star" }, opacity: { value: 0.5 }, size: { value: 1 }, move: { enable: true, speed: 0.4 } } }
  if (lv >= 70) return { fullScreen: { enable: false }, particles: { number: { value: 10 }, color: { value: ["#ff416c", "#ffd700", "#48bb78", "#4facfe", "#9400d3"] }, opacity: { value: 0.4 }, size: { value: { min: 1.5, max: 2.5 } }, move: { enable: true, speed: 0.8, direction: "top", outModes: "out" } } }
  if (lv >= 50) return { fullScreen: { enable: false }, particles: { number: { value: 6 }, color: { value: "#10b981" }, size: { value: 1 }, move: { enable: true, speed: 0.6, direction: "top" } } }
  return null
})

const currentTierClass = computed(() => {
  if (!props.showEffects) return 'tier-none'
  return `tier-${badgeStore.getAccountTierNumber(props.level)}`
})

const nextLevelExp = computed(() => props.level * props.level * 50)
const expPercentage = computed(() => {
  const prevLevelExp = props.level > 1 ? (props.level - 1) * (props.level - 1) * 50 : 0
  const currentLevelMax = nextLevelExp.value - prevLevelExp
  const currentEarned = props.exp - prevLevelExp
  return Math.min(Math.max((currentEarned / currentLevelMax) * 100, 0), 100)
})
const mainBadge = computed(() => (props.badges?.length > 0) ? props.badges[0] : null)

// tier-1 / tier-none (이펙트 없는 기본 닉네임): 테마에 따라 텍스트 색 직접 적용
const nicknameTextColor = computed(() => {
  const tier = currentTierClass.value
  if (tier === 'tier-1' || tier === 'tier-none') {
    return isDark.value ? '#f5f5f5' : '#262626'
  }
  return null
})

onMounted(() => {
  if (!badgeStore.isLoaded) badgeStore.fetchRules()
  themeObserver = new MutationObserver(() => {
    isDark.value = document.documentElement.classList.contains('dark-mode')
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => { if (themeObserver) themeObserver.disconnect() })
</script>

<style scoped>
.animated-nickname-wrapper { position: relative; display: inline-flex; align-items: center; cursor: pointer; gap: 5px; }

/* 닉네임 크기 최적화 */
.animated-nickname {
  font-size: 0.76rem; font-weight: 850; display: inline-block; transition: all 0.3s ease; position: relative; z-index: 2; white-space: nowrap; letter-spacing: -0.03em;
  padding: 2px 8px; border-radius: 6px; border: 1.5px solid var(--tier-color);
  overflow: hidden;
  color: var(--text-primary);
  user-select: none; /* 클릭 시 텍스트 선택 방지 */
}

.nickname-particles-container { 
  position: absolute; top: 0; left: 0; right: 0; bottom: 0; 
  pointer-events: none; z-index: 0; overflow: hidden; 
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
/* tier-50: 기존 그라디언트 텍스트 유지 */
.animated-nickname.tier-50 {
  background: var(--tier-gradient); -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; background-color: var(--tier-bg);
}

/* tier-5/10/30: 금속 배경 + 검정 텍스트 */
.animated-nickname.tier-5 {
  background: linear-gradient(135deg, #6b3a1f 0%, #c68642 35%, #e8a96a 55%, #c68642 75%, #6b3a1f 100%);
  -webkit-background-clip: initial !important; background-clip: initial !important;
  -webkit-text-fill-color: #1a0800 !important;
  border-color: #a0622a;
}
.animated-nickname.tier-10 {
  background: linear-gradient(135deg, #5a5a5a 0%, #b8b8b8 35%, #efefef 55%, #b8b8b8 75%, #5a5a5a 100%);
  -webkit-background-clip: initial !important; background-clip: initial !important;
  -webkit-text-fill-color: #111 !important;
  border-color: #aaaaaa;
}
.animated-nickname.tier-30 {
  background: linear-gradient(135deg, #6b4900 0%, #c8920a 35%, #ffd700 55%, #c8920a 75%, #6b4900 100%);
  -webkit-background-clip: initial !important; background-clip: initial !important;
  -webkit-text-fill-color: #1a0e00 !important;
  border-color: #c8920a;
}

.animated-nickname.tier-50 { animation: emerald-aura-pulse 2s infinite alternate; }
@keyframes emerald-aura-pulse { from { box-shadow: 0 0 5px rgba(16, 185, 129, 0.3); } to { box-shadow: 0 0 15px rgba(16, 185, 129, 0.6); } }
.inner-shine-effect { position: absolute; top: 0; left: -100%; width: 50%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent); transform: skewX(-20deg); pointer-events: none; animation: inner-glint 3s infinite; z-index: 1; }
@keyframes inner-glint { 0% { left: -120%; } 30% { left: 120%; } 100% { left: 120%; } }

.animated-nickname.tier-70, .animated-nickname.tier-90, .animated-nickname.tier-100 { 
  background: var(--tier-gradient); background-size: 200% auto; -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; 
  position: relative; animation: shine-move 3s linear infinite; background-color: var(--tier-bg);
}
.animated-nickname.tier-70 { animation: shine-move 3s linear infinite, rainbow-aura-pulse 3s infinite alternate; }
.animated-nickname.tier-70::before { content: ""; position: absolute; inset: 0; border-radius: 6px; padding: 1px; background: var(--tier-gradient); -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0); -webkit-mask-composite: xor; mask-composite: exclude; pointer-events: none; z-index: 3; }

.animated-nickname.tier-90 { animation: shine-move 2.5s linear infinite, cold-burn 1.5s infinite alternate; }
.animated-nickname.tier-100 { font-size: 0.85rem; border: 2px solid var(--tier-color); animation: shine-move 2s linear infinite, legendary-burn 1.2s infinite alternate; }

@keyframes shine-move { to { background-position: 200% center; } }
@keyframes cold-burn { from { box-shadow: 0 0 4px rgba(34, 211, 238, 0.4); } to { box-shadow: 0 0 12px rgba(34, 211, 238, 0.8); } }
@keyframes legendary-burn { from { box-shadow: 0 0 8px rgba(250, 204, 21, 0.5); } to { box-shadow: 0 0 20px rgba(250, 204, 21, 0.9); } }
@keyframes rainbow-aura-pulse { from { box-shadow: 0 0 8px rgba(255, 65, 108, 0.3); } to { box-shadow: 0 0 18px rgba(255, 65, 108, 0.6); } }

/* ── 인라인 아바타 ── */
.an-avatar {
  width: 26px; height: 26px; border-radius: 50%; flex-shrink: 0;
  background: var(--tier-bg, rgba(0,0,0,0.03));
  border: 1.5px solid var(--tier-color, var(--border-color, #dbdbdb));
  display: inline-flex; align-items: center; justify-content: center;
  position: relative; overflow: visible; transition: border-color 0.3s;
}
.an-av-initial {
  font-size: 0.72rem; font-weight: 900;
  background: var(--tier-gradient, var(--text-primary));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text; line-height: 1;
}
.an-avatar.tier-1 .an-av-initial,
.an-avatar.tier-none .an-av-initial { -webkit-text-fill-color: var(--text-secondary, #8e8e8e); background: none; }

/* 아바타 파티클 */
.an-av-p {
  position: absolute; border-radius: 50%; pointer-events: none; opacity: 0; z-index: 10;
  width: 3px; height: 3px;
}
.an-av-p.p1 { left: 5%;  bottom: 10%; animation: an-float 2.2s 0s     infinite; }
.an-av-p.p2 { left: 35%; bottom: 0;   animation: an-float 2.2s 0.55s  infinite; }
.an-av-p.p3 { left: 65%; bottom: 0;   animation: an-float 2.2s 1.1s   infinite; }
.an-av-p.p4 { left: 90%; bottom: 10%; animation: an-float 2.2s 1.65s  infinite; }
@keyframes an-float {
  0%   { opacity: 0;   transform: translateY(0)     scale(0.7); }
  20%  { opacity: 0.9; }
  100% { opacity: 0;   transform: translateY(-22px) scale(0.2); }
}
/* tier별 파티클 색 */
.tier-50  .an-av-p { background: #10b981; }
.tier-70  .an-av-p.p1 { background: #ff416c; }
.tier-70  .an-av-p.p2 { background: #ffd700; }
.tier-70  .an-av-p.p3 { background: #4facfe; }
.tier-70  .an-av-p.p4 { background: #9400d3; }
.tier-90  .an-av-p { background: #e0f7ff; border-radius: 1px; }
.tier-90  .an-av-p.p1 { animation: an-snow 3s 0s    infinite; }
.tier-90  .an-av-p.p2 { animation: an-snow 3s 0.75s infinite; }
.tier-90  .an-av-p.p3 { animation: an-snow 3s 1.5s  infinite; }
.tier-90  .an-av-p.p4 { animation: an-snow 3s 2.25s infinite; }
@keyframes an-snow {
  0%   { opacity: 0;   transform: translateY(0) translateX(0) rotate(0deg) scale(0.8); }
  20%  { opacity: 0.8; }
  100% { opacity: 0;   transform: translateY(-20px) translateX(-2px) rotate(180deg) scale(0.2); }
}
.tier-100 .an-av-p { background: #facc15; width: 4px; height: 4px; box-shadow: 0 0 3px rgba(250,204,21,0.7); }
.tier-100 .an-av-p.p1 { animation-duration: 1.5s; }
.tier-100 .an-av-p.p2 { animation-duration: 1.5s; }
.tier-100 .an-av-p.p3 { animation-duration: 1.5s; }
.tier-100 .an-av-p.p4 { animation-duration: 1.5s; }

/* --- Tooltip System --- */
.nickname-tooltip {
  position: absolute; 
  width: 190px; background: var(--card-bg) !important; background-image: linear-gradient(to bottom, var(--card-bg), var(--tier-bg)) !important; 
  backdrop-filter: blur(20px); border: 1.5px solid var(--tier-color); border-radius: 16px; padding: 14px; box-shadow: 0 15px 40px rgba(0,0,0,0.3); 
  z-index: 2000000;
  cursor: default; overflow: hidden;
  transition: opacity 0.3s, transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  pointer-events: auto;
}

.nickname-tooltip.right { left: calc(100% + 15px); top: 50%; transform: translateY(-50%); }
.nickname-tooltip.top { bottom: calc(100% + 15px); left: 50%; transform: translateX(-50%); transform-origin: bottom center; }
.nickname-tooltip.top::after { content: ""; position: absolute; top: 100%; left: 50%; transform: translateX(-50%); border-width: 8px; border-style: solid; border-color: var(--tier-color) transparent transparent transparent; }

.tooltip-content-inner { position: relative; z-index: 10; }
.nickname-tooltip *:not(.user-name-styled) { -webkit-text-fill-color: initial !important; color: var(--tier-color) !important; background-clip: initial !important; -webkit-background-clip: initial !important; }

.user-name-styled { font-weight: 900; font-size: 0.85rem; display: inline-block; background: var(--tier-gradient); background-size: 200% auto; -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; }
.user-name-styled.tier-70, .user-name-styled.tier-90, .user-name-styled.tier-100 { animation: shine-move 3s linear infinite; }
/* 툴팁 닉네임도 금속 배경 방식 적용 */
.user-name-styled.tier-5  { background: linear-gradient(135deg, #6b3a1f, #c68642, #e8a96a, #c68642, #6b3a1f); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #1a0800 !important; padding: 1px 6px; border-radius: 4px; }
.user-name-styled.tier-10 { background: linear-gradient(135deg, #5a5a5a, #b8b8b8, #efefef, #b8b8b8, #5a5a5a); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #111 !important; padding: 1px 6px; border-radius: 4px; }
.user-name-styled.tier-30 { background: linear-gradient(135deg, #6b4900, #c8920a, #ffd700, #c8920a, #6b4900); -webkit-background-clip: initial !important; background-clip: initial !important; -webkit-text-fill-color: #1a0e00 !important; padding: 1px 6px; border-radius: 4px; }

.glass-reflection-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(135deg, transparent 45%, rgba(255, 255, 255, 0.15) 50%, transparent 55%); background-size: 300% 300%; z-index: 5; pointer-events: none; animation: sharpShine 6s infinite; }
@keyframes sharpShine { 0% { background-position: 250% 250%; } 20% { background-position: -150% -150%; } 100% { background-position: -150% -150%; } }

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

/* 쪽지 버튼 스타일 */
.tooltip-actions { margin-top: 12px; }
.msg-send-btn { 
  width: 100%; padding: 6px; border-radius: 8px; border: 1.2px solid var(--tier-color);
  background: transparent; color: var(--tier-color) !important; font-size: 0.7rem; font-weight: 850;
  cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center;
}
.msg-send-btn:hover { background: var(--tier-color); color: white !important; }

@media (max-width: 768px) {
  .nickname-tooltip { width: 170px; padding: 10px; border-radius: 12px; }
  .nickname-tooltip.top { bottom: calc(100% + 10px); transform: translateX(-50%) scale(0.85) !important; }
  .user-avatar { width: 28px; height: 32px; font-size: 0.9rem; }
  .user-name-styled { font-size: 0.8rem; }
}
</style>
