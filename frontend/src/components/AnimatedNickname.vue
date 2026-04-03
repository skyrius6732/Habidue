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
    <div :class="['nickname-with-effects', { 'has-wings': isWingsEffect, 'has-bubbles': isBubblesEffect, 'has-stars': isStarsEffect, 'has-thunder': isThunderEffect, 'has-flame': isFlameEffect, 'has-ice': isIceFrostEffect, 'has-sakura': isSakuraEffect, 'has-shadow': isShadowEffect, 'has-neon': isNeonEffect, 'has-glitch': isGlitchEffect, 'has-void': isVoidEffect, 'has-heart': isHeartEffect, 'has-rainbow': isRainbowEffect, 'has-shooting': isShootingStarEffect, 'has-blackhole': isBlackholeEffect, 'has-whitehole': isWhiteholeEffect, 'has-bomb': isBombEffect }]">
      
      <!-- 1. 날개 이펙트 (멀티 티어 시스템) -->
      <template v-if="isWingsEffect">
        <div class="wings-container left">
          <div v-for="n in wingLayers" :key="`wing-l-${n}`" class="wing-layer" :style="{ '--layer': n, '--total': wingLayers }">
            <svg class="pioneer-wing-svg left" viewBox="0 0 100 100" :style="{ filter: `drop-shadow(0 0 ${3 + n}px ${wingColors.glow})` }">
              <path d="M90,50 Q70,10 20,40 Q40,50 90,50 Z" :fill="`url(#wing-grad-${instanceId}-${n})`" />
              <path d="M85,55 Q65,25 25,50 Q45,60 85,55 Z" :fill="`url(#wing-grad-${instanceId}-${n})`" opacity="0.6" />
              <defs>
                <linearGradient :id="`wing-grad-${instanceId}-${n}`" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" :style="{ 'stop-color': wingColors.s1 }" />
                  <stop offset="50%" :style="{ 'stop-color': wingColors.s2 }" />
                  <stop offset="100%" :style="{ 'stop-color': wingColors.s3 }" />
                </linearGradient>
              </defs>
            </svg>
          </div>
        </div>
      </template>

      <!-- 2. 신비로운 버블 이펙트 (MAGIC_BUBBLES) -->
      <div v-if="isBubblesEffect" class="bubbles-container">
        <div v-for="n in 8" :key="`bubble-${n}`" class="bubble" 
          :style="{ 
            '--left': `${Math.random() * 100}%`, 
            '--size': `${4 + Math.random() * 6}px`,
            '--delay': `${Math.random() * 4}s`,
            '--dur': `${2 + Math.random() * 2}s`
          }"
        ></div>
      </div>

      <!-- 3. 반짝이는 별무리 이펙트 (STARRY_NIGHT) -->
      <div v-if="isStarsEffect" class="stars-container">
        <svg v-for="n in 6" :key="`star-${n}`" class="twinkle-star" viewBox="0 0 24 24"
          :style="{
            '--top': `${5 + Math.random() * 70}%`,
            '--left': `${3 + Math.random() * 94}%`,
            '--delay': `${Math.random() * 3}s`,
            '--size': `${8 + Math.random() * 8}px`
          }"
        >
          <path fill="#facc15" d="M12,2L15.09,8.26L22,9.27L17,14.14L18.18,21.02L12,17.77L5.82,21.02L7,14.14L2,9.27L8.91,8.26L12,2Z" />
        </svg>
      </div>

      <!-- 5. 번개 이펙트 (THUNDER_BLUE) -->
      <div v-if="isThunderEffect" class="thunder-container">
        <svg v-for="n in 6" :key="`bolt-${n}`" class="lightning-bolt" viewBox="0 0 16 32"
          :style="{
            '--left': `${10 + (n - 1) * 14}%`,
            '--top': `${-5 + (n % 3) * 28}%`,
            '--delay': `${((n * 0.47) % 2.2).toFixed(2)}s`,
            '--dur': `${(1.2 + (n % 3) * 0.6).toFixed(2)}s`,
            '--size': `${13 + (n % 3) * 5}px`
          }"
        >
          <polygon points="10,0 5,13 9,13 6,32 14,11 8,11 12,0"
            :fill="n % 2 === 0 ? '#93c5fd' : '#e0f2fe'"
            stroke="#bfdbfe" stroke-width="0.4"/>
        </svg>
      </div>


      <!-- 7. ICE_FROST: 눈결정 흩날림 -->
      <div v-if="isIceFrostEffect" class="frost-container">
        <svg v-for="n in 7" :key="`flake-${n}`" class="snowflake" viewBox="0 0 24 24"
          :style="{
            '--left': `${(n - 1) * 15 + 2}%`,
            '--delay': `${((n * 0.62) % 3).toFixed(2)}s`,
            '--dur': `${(2.4 + (n % 3) * 0.7).toFixed(1)}s`,
            '--size': `${8 + (n % 3) * 4}px`,
            '--drift': `${-8 + (n % 5) * 4}px`
          }"
        >
          <line x1="12" y1="2" x2="12" y2="22" stroke="#bae6fd" stroke-width="1.8" stroke-linecap="round"/>
          <line x1="2" y1="7" x2="22" y2="17" stroke="#bae6fd" stroke-width="1.8" stroke-linecap="round"/>
          <line x1="22" y1="7" x2="2" y2="17" stroke="#bae6fd" stroke-width="1.8" stroke-linecap="round"/>
          <line x1="2" y1="12" x2="22" y2="12" stroke="#bae6fd" stroke-width="1.8" stroke-linecap="round"/>
          <line x1="7" y1="3.5" x2="17" y2="20.5" stroke="#7dd3fc" stroke-width="1" stroke-linecap="round" opacity="0.6"/>
          <line x1="17" y1="3.5" x2="7" y2="20.5" stroke="#7dd3fc" stroke-width="1" stroke-linecap="round" opacity="0.6"/>
        </svg>
      </div>

      <!-- 8. SAKURA_BLOOM: 벚꽃잎 살랑살랑 -->
      <div v-if="isSakuraEffect" class="sakura-container">
        <svg v-for="n in 9" :key="`petal-${n}`" class="sakura-petal" viewBox="0 0 20 30"
          :style="{
            '--left': `${(n - 1) * 11 - 3}%`,
            '--delay': `${((n * 0.55) % 2.8).toFixed(2)}s`,
            '--dur': `${(2.4 + (n % 4) * 0.5).toFixed(1)}s`,
            '--size': `${6 + (n % 3) * 2}px`,
            '--sway': `${-22 + (n % 5) * 11}px`,
            '--rot-s': `${-40 + (n % 4) * 22}deg`
          }"
        >
          <template v-if="n % 3 === 0">
            <path d="M10,1 C13,1 16,5 15,10 C14,16 12,22 10,28 C8,22 6,16 5,10 C4,5 7,1 10,1Z"
              :fill="`hsl(${340 + (n % 4) * 7}, 86%, ${74 + (n % 3) * 5}%)`" opacity="0.9"/>
            <path d="M10,4 C11.5,4 12.5,8 12,13 C11.5,17 11,21 10,25 C9,21 8.5,17 8,13 C7.5,8 8.5,4 10,4Z"
              fill="rgba(255,255,255,0.35)"/>
          </template>
          <template v-else-if="n % 3 === 1">
            <path d="M10,3 C16,3 19,8 17,14 C15,19 12,24 10,27 C8,24 5,19 3,14 C1,8 4,3 10,3Z"
              :fill="`hsl(${338 + (n % 4) * 7}, 82%, ${76 + (n % 3) * 5}%)`" opacity="0.87"/>
            <path d="M10,6 C13,6 15,10 14,14 C13,18 11.5,22 10,25 C8.5,22 7,18 6,14 C5,10 7,6 10,6Z"
              fill="rgba(255,255,255,0.3)"/>
          </template>
          <template v-else>
            <path d="M8,1 C13,1 17,6 16,12 C15,18 12,24 10,28 C8,24 6,18 5.5,12 C5,7 7,2 8,1Z"
              :fill="`hsl(${342 + (n % 4) * 7}, 84%, ${75 + (n % 3) * 5}%)`" opacity="0.88"/>
            <path d="M9,5 C11,5 12.5,9 12,13 C11.5,17 11,21 10,25 C9,21 8,17 7.5,13 C7,9 7.5,5 9,5Z"
              fill="rgba(255,255,255,0.28)"/>
          </template>
        </svg>
      </div>

      <!-- 9. SHADOW_DEMON: 어두운 연기 blob -->
      <div v-if="isShadowEffect" class="shadow-container">
        <div v-for="n in 6" :key="`smoke-${n}`" class="smoke-blob"
          :style="{
            '--left': `${5 + (n - 1) * 16}%`,
            '--delay': `${((n * 0.5) % 2.5).toFixed(2)}s`,
            '--dur': `${(1.8 + (n % 3) * 0.6).toFixed(1)}s`,
            '--size': `${14 + (n % 4) * 6}px`,
            '--hue': `${260 + (n % 4) * 12}`
          }"
        ></div>
      </div>

      <!-- 14. RAINBOW_WAVE: 무지개 아치형 -->
      <svg v-if="isRainbowEffect" class="rainbow-arch-wrap" viewBox="0 0 200 170" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
        <defs>
          <linearGradient id="rainbowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" style="stop-color:#ff0000;stop-opacity:1" />
            <stop offset="16.67%" style="stop-color:#ff7700;stop-opacity:1" />
            <stop offset="33.33%" style="stop-color:#ffee00;stop-opacity:1" />
            <stop offset="50%" style="stop-color:#00dd44;stop-opacity:1" />
            <stop offset="66.67%" style="stop-color:#0088ff;stop-opacity:1" />
            <stop offset="83.33%" style="stop-color:#6600dd;stop-opacity:1" />
            <stop offset="100%" style="stop-color:#cc00ff;stop-opacity:1" />
          </linearGradient>
        </defs>
        <path class="rainbow-arch" d="M 20,70 Q 100,15 180,70" stroke="url(#rainbowGrad)" stroke-width="15" fill="none" stroke-linecap="round" />
      </svg>

      <!-- 15. SHOOTING_STAR: 별똥별 닉네임 앞 가로질러 -->
      <div v-if="isShootingStarEffect" class="shooting-star-container">
        <div class="shooting-star"
          :style="{
            '--delay': '0s',
            '--y': '50%'
          }"
        ></div>
      </div>

      <!-- 16. BLACK_HOLE: 블랙홀 빨려들어감 -->
      <div v-if="isBlackholeEffect" class="blackhole-container">
        <div class="bh-ring"></div>
      </div>

      <!-- 17. WHITE_HOLE: 화이트홀 뱉어냄 -->
      <div v-if="isWhiteholeEffect" class="whitehole-container">
        <div class="wh-ring"></div>
      </div>


      <!-- 18. BOMB: 폭탄 심줄 테두리 타들어감 -->
      <svg v-if="isBombEffect" class="bomb-fuse-container" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
        <rect class="bomb-border-rect" x="4" y="4" width="92" height="92" />
      </svg>

      <!-- 13. LOVE_HEART: 하트 아래서 위로 -->
      <div v-if="isHeartEffect" class="heart-container">
        <svg v-for="n in 7" :key="`heart-${n}`" class="floating-heart" viewBox="0 0 24 22"
          :style="{
            '--left': `${(n - 1) * 15 + 2}%`,
            '--delay': `${((n * 0.58) % 3).toFixed(2)}s`,
            '--dur': `${(2.2 + (n % 3) * 0.7).toFixed(1)}s`,
            '--size': `${7 + (n % 4) * 3}px`,
            '--sway': `${-10 + (n % 5) * 5}px`
          }"
        >
          <path d="M12,20 C12,20 2,13 2,7 C2,4.24 4.24,2 7,2 C8.9,2 10.6,3 12,4.7 C13.4,3 15.1,2 17,2 C19.76,2 22,4.24 22,7 C22,13 12,20 12,20Z"
            :fill="`hsl(${345 + (n % 3) * 12}, 92%, ${62 + (n % 3) * 8}%)`"/>
          <path d="M7,4 C6,4 5,5 5,6 C5,7.5 6,8.5 7,9.5" stroke="rgba(255,255,255,0.45)" stroke-width="1" fill="none" stroke-linecap="round"/>
        </svg>
      </div>

      <!-- 12. VOID_RIFT: 차원 균열 보라 파티클 orbit -->
      <div v-if="isVoidEffect" class="void-container">
        <div v-for="n in 8" :key="`void-p-${n}`" class="void-particle"
          :style="{
            '--angle': `${(n - 1) * 45}deg`,
            '--delay': `${((n * 0.38) % 2.5).toFixed(2)}s`,
            '--dur': `${(1.6 + (n % 3) * 0.5).toFixed(1)}s`,
            '--dist': `${18 + (n % 3) * 7}px`,
            '--size': `${3 + (n % 3) * 2}px`
          }"
        ></div>
      </div>

      <span :class="['animated-nickname', currentTierClass]" :data-text="nickname" :style="nicknameTextColor ? { color: nicknameTextColor } : {}" @click.stop="handleToggleClick">
        <span v-if="(level >= 50)" class="inner-shine-effect"></span>
        {{ nickname }}
      </span>

      <!-- 4. 날개 오른쪽 (멀티 티어) -->
      <template v-if="isWingsEffect">
        <div class="wings-container right">
          <div v-for="n in wingLayers" :key="`wing-r-${n}`" class="wing-layer" :style="{ '--layer': n, '--total': wingLayers }">
            <svg class="pioneer-wing-svg right" viewBox="0 0 100 100" :style="{ filter: `drop-shadow(0 0 ${3 + n}px ${wingColors.glow})` }">
              <path d="M10,50 Q30,10 80,40 Q60,50 10,50 Z" :fill="`url(#wing-grad-rev-${instanceId}-${n})`" />
              <path d="M15,55 Q35,25 75,50 Q55,60 15,55 Z" :fill="`url(#wing-grad-rev-${instanceId}-${n})`" opacity="0.6" />
              <defs>
                <linearGradient :id="`wing-grad-rev-${instanceId}-${n}`" x1="100%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" :style="{ 'stop-color': wingColors.s1 }" />
                  <stop offset="50%" :style="{ 'stop-color': wingColors.s2 }" />
                  <stop offset="100%" :style="{ 'stop-color': wingColors.s3 }" />
                </linearGradient>
              </defs>
            </svg>
          </div>
        </div>
      </template>

    </div>

    <!-- 미니 프로필 툴팁 -->
    <div v-if="showTooltip" ref="tooltipRef" :class="['nickname-tooltip', currentTierClass, tooltipDirection]" @click.stop>
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

        <div class="tooltip-actions stagger-item" v-if="userId && !isMe">
          <button class="msg-send-btn" @click.stop="openMessageModal">
            <span>💌 쪽지 보내기</span>
          </button>
        </div>

        <!-- [시니어 조치] 본인일 경우 이펙트 설정 노출 -->
        <div class="tooltip-effect-settings stagger-item" v-if="(isMe && (level >= 50 || isAdmin))">
          <div class="effect-header-mini">
            <span class="effect-label-mini">MY EFFECT</span>
            <span v-if="!isAdmin" class="effect-lock-guide">Lv.50 이상 해제</span>
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
                  locked: !isAdmin && eff.id && level < 50
                }"
                @click.stop="isAdmin || !eff.id || level >= 50 ? updateEffect(eff.id, $event) : null"
                :disabled="isUpdatingEffect"
                :title="!isAdmin && eff.id && level < 50 ? '레벨 50 달성 시 해제됩니다' : eff.name"
              >
                <span class="eff-icon">{{ eff.icon }}</span>
                <span v-if="!isAdmin && eff.id && level < 50" class="lock-overlay">🔒</span>
                <span v-if="(displayEquippedEffect === eff.id) || (!displayEquippedEffect && !eff.id)" class="active-check">✓</span>
              </button>
            </div>
          </div>
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
import axios from '@/plugins/axios'
import { useBadgeStore } from '@/stores/badge'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import MessageSendModal from '@/components/MessageSendModal.vue'
import gsap from 'gsap'

// [시니어 조치] 특수 효과 리스트
const specialEffects = [
  { id: null, name: '효과 없음', icon: '🚫' },
  { id: 'PIONEER_WINGS', name: '🕊️ 화이트 윙 (White)', icon: '🕊️' },
  { id: 'BRONZE_WINGS', name: '🥉 브론즈 윙 (Bronze)', icon: '🥉' },
  { id: 'SILVER_WINGS', name: '🥈 실버 윙 (Silver)', icon: '🥈' },
  { id: 'GOLD_WINGS', name: '👑 골드 윙 (Gold)', icon: '👑' },
  { id: 'MAGIC_BUBBLES', name: '🫧 신비로운 버블 (Bubble)', icon: '🫧' },
  { id: 'STARRY_NIGHT', name: '✨ 반짝이는 별무리 (Starry)', icon: '✨' },
  { id: 'THUNDER_BLUE', name: '⚡ 푸른 번개 (Thunder)', icon: '⚡' },
  { id: 'AURORA_FLAME', name: '🔥 오로라 화염 (Flame)', icon: '🔥' },
  { id: 'ICE_FROST', name: '❄️ 얼음 결정 (Frost)', icon: '❄️' },
  { id: 'SAKURA_BLOOM', name: '🌸 벚꽃 (Sakura)', icon: '🌸' },
  { id: 'SHADOW_DEMON', name: '👹 섀도우 데몬 (Shadow)', icon: '👹' },
  { id: 'NEON_SIGN', name: '🌟 네온 사인 (Neon)', icon: '🌟' },
  { id: 'PIXEL_GLITCH', name: '💥 픽셀 글리치 (Glitch)', icon: '💥' },
  { id: 'VOID_RIFT', name: '🌀 보이드 리프트 (Void)', icon: '🌀' },
  { id: 'LOVE_HEART', name: '💕 두근두근 하트 (Heart)', icon: '💕' },
  { id: 'RAINBOW_WAVE', name: '🌈 무지개 (Rainbow)', icon: '🌈' },
  { id: 'SHOOTING_STAR', name: '🌠 별똥별 (Shooting Star)', icon: '🌠' },
  { id: 'BLACK_HOLE', name: '🕳️ 블랙홀 (Black Hole)', icon: '🕳️' },
  { id: 'WHITE_HOLE', name: '🤍 화이트홀 (White Hole)', icon: '🤍' }
]

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
  if (effect === 'GOLD_WINGS') return { s1: '#fff9c4', s2: '#facc15', s3: '#a16207', glow: 'rgba(250, 204, 21, 0.8)' }
  if (effect === 'SILVER_WINGS') return { s1: '#f8fafc', s2: '#94a3b8', s3: '#475569', glow: 'rgba(148, 163, 184, 0.7)' }
  if (effect === 'BRONZE_WINGS') return { s1: '#fed7aa', s2: '#ea580c', s3: '#7c2d12', glow: 'rgba(234, 88, 12, 0.6)' }
  if (effect === 'PIONEER_WINGS') return { s1: '#ffffff', s2: '#e2e8f0', s3: '#94a3b8', glow: 'rgba(255, 255, 255, 0.9)' }
  return { s1: '#ffffff', s2: '#e2e8f0', s3: '#94a3b8', glow: 'rgba(255, 255, 255, 0.8)' }
})

// [시니어 조치] 날개 레이어 개수 계산 (장착된 이펙트 코드 기준 고정)
const wingLayers = computed(() => {
  const effect = actualEquippedEffect.value
  if (effect === 'GOLD_WINGS') return 4
  if (effect === 'SILVER_WINGS') return 3
  if (effect === 'BRONZE_WINGS' || effect === 'PIONEER_WINGS') return 2
  return 1
})

const props = defineProps({
  nickname: { type: String, required: true },
  userId: { type: [Number, String], default: null },
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
  authorEquippedEffect: { type: String, default: null } // [시니어 대응] 백엔드 DTO 필드명 호환용
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

const isAdmin = computed(() => authStore.user?.role === 'ROLE_ADMIN')

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
const isShadowEffect = computed(() => displayEquippedEffect.value === 'SHADOW_DEMON')
const isNeonEffect = computed(() => displayEquippedEffect.value === 'NEON_SIGN')
const isGlitchEffect = computed(() => displayEquippedEffect.value === 'PIXEL_GLITCH')
const isVoidEffect = computed(() => displayEquippedEffect.value === 'VOID_RIFT')
const isHeartEffect = computed(() => displayEquippedEffect.value === 'LOVE_HEART')
const isRainbowEffect = computed(() => displayEquippedEffect.value === 'RAINBOW_WAVE')
const isShootingStarEffect = computed(() => displayEquippedEffect.value === 'SHOOTING_STAR')
const isBlackholeEffect = computed(() => displayEquippedEffect.value === 'BLACK_HOLE')
const isWhiteholeEffect = computed(() => displayEquippedEffect.value === 'WHITE_HOLE')
const isBombEffect = computed(() => displayEquippedEffect.value === 'BOMB')

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
  if (!props.userId) {
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
      await axios.patch(`/api/users/${authStore.user.id}/effect`, null, { params: { effectCode: effectId } })
    } catch (e) {
      uiStore.showAlert('효과 저장 실패', '안내')
    } finally {
      isUpdatingEffect.value = false
    }
  }, 500)
}

const onMessageSuccess = () => {}

const showParticles = computed(() => displayShowEffects.value && (props.level >= 50))
const particleOptions = computed(() => {
  const lv = props.level
  if (lv >= 100) return { fullScreen: { enable: false }, particles: { number: { value: 15 }, color: { value: "#facc15" }, size: { value: { min: 1, max: 2.5 } }, move: { enable: true, speed: 1.2, direction: "top" } } }
  if (lv >= 90) return { fullScreen: { enable: false }, particles: { number: { value: 10 }, color: { value: "#22d3ee" }, shape: { type: "star" }, opacity: { value: 0.5 }, size: { value: 1 }, move: { enable: true, speed: 0.4 } } }
  if (lv >= 70) return { fullScreen: { enable: false }, particles: { number: { value: 10 }, color: { value: ["#ff416c", "#ffd700", "#48bb78", "#4facfe", "#9400d3"] }, opacity: { value: 0.4 }, size: { value: { min: 1.5, max: 2.5 } }, move: { enable: true, speed: 0.8, direction: "top", outModes: "out" } } }
  if (lv >= 50) return { fullScreen: { enable: false }, particles: { number: { value: 6 }, color: { value: "#10b981" }, size: { value: 1 }, move: { enable: true, speed: 0.6, direction: "top" } } }
  return null
})

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

.animated-nickname {
  font-size: 0.76rem; font-weight: 850; display: inline-block; transition: all 0.3s ease; position: relative; z-index: 2; white-space: nowrap; letter-spacing: -0.03em;
  padding: 2px 8px; border-radius: 6px; border: 1.5px solid var(--tier-color);
  overflow: hidden; color: var(--text-primary); user-select: none;
}

.nickname-particles-container { position: absolute; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; z-index: 0; overflow: hidden; }

.nickname-with-effects { display: flex; align-items: center; position: relative; gap: 4px; padding: 0 4px; }

/* 1. Wings System */
.wings-container { display: flex; align-items: center; justify-content: center; position: relative; width: 40px; height: 32px; pointer-events: none; z-index: 1; }
.wing-layer {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * 12deg));
  opacity: calc(1 - (var(--layer) - 1) * 0.2);
  animation: wing-flap-multi 3s infinite ease-in-out;
  animation-delay: calc(var(--layer) * -0.4s);
}
.wings-container.right .wing-layer { transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * -12deg)); animation-name: wing-flap-multi-rev; }
.pioneer-wing-svg { width: 100%; height: 100%; filter: drop-shadow(0 0 5px rgba(255,255,255,0.4)); }

@keyframes wing-flap-multi {
  0%, 100% { transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * 12deg)) translateY(0); }
  50% { transform: scale(calc(1.1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * 12deg + 15deg)) translateY(-4px); }
}
@keyframes wing-flap-multi-rev {
  0%, 100% { transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * -12deg)) translateY(0); }
  50% { transform: scale(calc(1.1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * -12deg - 15deg)) translateY(-4px); }
}

/* 2. Magic Bubbles (귀여운 버블) */
.bubbles-container { position: absolute; inset: -10px -5px; pointer-events: none; z-index: 1; overflow: hidden; }
.bubble {
  position: absolute; bottom: 0; left: var(--left); width: var(--size); height: var(--size);
  background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 50%;
  box-shadow: inset -2px -2px 4px rgba(0,0,0,0.05), 0 0 10px rgba(255,255,255,0.3);
  animation: bubble-float var(--dur) infinite ease-in var(--delay); opacity: 0;
}
@keyframes bubble-float {
  0% { transform: translateY(0) scale(0.5); opacity: 0; }
  20% { opacity: 0.8; }
  80% { opacity: 0.8; transform: translateY(-30px) translateX(10px); }
  100% { transform: translateY(-40px) scale(1.2); opacity: 0; }
}

/* 3. Starry Night (반짝이는 별) */
.stars-container { position: absolute; inset: -4px; pointer-events: none; z-index: 10; }
.twinkle-star {
  position: absolute; top: var(--top); left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 5px #facc15);
  animation: star-twinkle 2s infinite ease-in-out var(--delay); opacity: 0;
}
@keyframes star-twinkle {
  0%, 100% { transform: scale(0) rotate(0deg); opacity: 0; }
  50% { transform: scale(1) rotate(180deg); opacity: 1; }
}

/* 5. Thunder Blue (번개) */
.thunder-container { position: absolute; inset: -12px -8px; pointer-events: none; z-index: 5; overflow: visible; }
.lightning-bolt {
  position: absolute; top: var(--top); left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 3px #60a5fa) drop-shadow(0 0 7px rgba(147, 197, 253, 0.9));
  animation: bolt-flash var(--dur) infinite var(--delay) ease-out;
  opacity: 0;
}
@keyframes bolt-flash {
  0%, 100% { opacity: 0; transform: scaleY(0.85) translateY(2px); }
  8% { opacity: 1; transform: scaleY(1.1) translateY(-1px); }
  18% { opacity: 0.6; transform: scaleY(0.95); }
  22% { opacity: 1; }
  30% { opacity: 0; }
}
.has-thunder .animated-nickname { box-shadow: 0 0 6px rgba(96, 165, 250, 0.5) !important; animation: thunder-nick-pulse 0.6s infinite alternate !important; }
@keyframes thunder-nick-pulse { from { box-shadow: 0 0 4px rgba(96, 165, 250, 0.3); } to { box-shadow: 0 0 14px rgba(147, 197, 253, 0.7); } }

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

/* 7. ICE_FROST — 눈결정 흩날림 */
.frost-container { position: absolute; inset: -15px -5px; pointer-events: none; z-index: 5; overflow: hidden; }
.snowflake {
  position: absolute; top: -10px; left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 4px rgba(125, 211, 252, 0.95));
  animation: snowfall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes snowfall {
  0%   { opacity: 0; transform: translateY(-4px) translateX(0) rotate(0deg); }
  15%  { opacity: 0.95; }
  85%  { opacity: 0.8; transform: translateY(36px) translateX(var(--drift)) rotate(240deg); }
  100% { opacity: 0; transform: translateY(42px) translateX(var(--drift)) rotate(360deg); }
}
.has-ice .animated-nickname { border-color: #7dd3fc !important; animation: ice-glow 1.6s ease-in-out infinite alternate !important; }
@keyframes ice-glow {
  from { box-shadow: 0 0 4px rgba(125,211,252,0.4), 0 0 8px rgba(186,230,253,0.2); }
  to   { box-shadow: 0 0 10px rgba(186,230,253,0.75), 0 0 20px rgba(125,211,252,0.35), 0 0 32px rgba(56,189,248,0.18); }
}

/* 8. SAKURA_BLOOM — 벚꽃잎 살랑살랑 */
.sakura-container { position: absolute; inset: -18px -8px; pointer-events: none; z-index: 5; overflow: hidden; }
.sakura-petal {
  position: absolute; top: -14px; left: var(--left); width: var(--size); height: calc(var(--size) * 1.7);
  filter: drop-shadow(0 0 2px rgba(251, 164, 175, 0.65));
  animation: petal-fall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
  transform-origin: center 60%;
}
@keyframes petal-fall {
  0%   { opacity: 0; transform: translateY(-5px) translateX(0) rotate(var(--rot-s)); }
  10%  { opacity: 0.92; }
  30%  { transform: translateY(12px) translateX(var(--sway)) rotate(calc(var(--rot-s) + 42deg)); }
  58%  { transform: translateY(26px) translateX(calc(var(--sway) * -0.75)) rotate(calc(var(--rot-s) + 88deg)); }
  82%  { opacity: 0.5; transform: translateY(40px) translateX(calc(var(--sway) * 0.5)) rotate(calc(var(--rot-s) + 125deg)); }
  100% { opacity: 0; transform: translateY(50px) translateX(calc(var(--sway) * -0.2)) rotate(calc(var(--rot-s) + 158deg)); }
}
.has-sakura .animated-nickname { border-color: #fb7185 !important; animation: sakura-glow 2s ease-in-out infinite alternate !important; }
@keyframes sakura-glow {
  from { box-shadow: 0 0 5px rgba(251,113,133,0.35); }
  to   { box-shadow: 0 0 12px rgba(253,164,175,0.65), 0 0 22px rgba(251,113,133,0.28); }
}

/* 9. SHADOW_DEMON — 보라 연기 blob */
.shadow-container { position: absolute; inset: -10px -8px; pointer-events: none; z-index: 1; overflow: visible; }
.smoke-blob {
  position: absolute; bottom: 0; left: var(--left);
  width: var(--size); height: var(--size);
  border-radius: 50%;
  background: radial-gradient(circle,
    hsla(var(--hue), 65%, 25%, 0.88) 0%,
    hsla(var(--hue), 75%, 12%, 0.5) 50%,
    transparent 78%
  );
  filter: blur(4px);
  animation: smoke-rise var(--dur) infinite var(--delay) ease-out;
  opacity: 0;
}
@keyframes smoke-rise {
  0%   { opacity: 0; transform: translateY(0) scale(0.45); }
  20%  { opacity: 0.88; transform: translateY(-10px) scale(0.85) translateX(4px); }
  60%  { opacity: 0.55; transform: translateY(-26px) scale(1.4) translateX(-5px); }
  100% { opacity: 0; transform: translateY(-40px) scale(1.7) translateX(2px); }
}
.has-shadow .animated-nickname { border-color: #7c3aed !important; animation: shadow-pulse 1.4s ease-in-out infinite alternate !important; }
@keyframes shadow-pulse {
  from { box-shadow: 0 0 6px rgba(124,58,237,0.45); }
  to   { box-shadow: 0 0 14px rgba(124,58,237,0.75), 0 0 28px rgba(76,29,149,0.4), 0 0 0 2px rgba(109,40,217,0.3); }
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

/* 13. LOVE_HEART — 하트 아래서 위로 */
.heart-container { position: absolute; inset: -10px -5px; pointer-events: none; z-index: 5; overflow: hidden; }
.floating-heart {
  position: absolute; bottom: 0; left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 3px rgba(244, 63, 94, 0.8));
  animation: heart-float var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes heart-float {
  0%   { opacity: 0; transform: translateY(0) translateX(0) scale(0.55); }
  12%  { opacity: 0.92; transform: translateY(-6px) translateX(calc(var(--sway) * 0.3)) scale(0.8); }
  45%  { opacity: 0.85; transform: translateY(-20px) translateX(var(--sway)) scale(1.05); }
  78%  { opacity: 0.4; transform: translateY(-34px) translateX(calc(var(--sway) * 0.4)) scale(0.9); }
  100% { opacity: 0; transform: translateY(-44px) translateX(0) scale(0.6); }
}
.has-heart .animated-nickname { border-color: #f43f5e !important; animation: heart-glow 1.8s ease-in-out infinite alternate !important; }
@keyframes heart-glow {
  from { box-shadow: 0 0 5px rgba(244,63,94,0.38); }
  to   { box-shadow: 0 0 12px rgba(251,113,133,0.68), 0 0 22px rgba(244,63,94,0.3); }
}

/* 14. RAINBOW_WAVE — 무지개 아치형 */
.rainbow-arch-wrap {
  position: absolute; bottom: -10px; left: 0; right: 0;
  pointer-events: none; z-index: 3; width: 100%; height: 80px;
}
.rainbow-arch {
  stroke-dasharray: 240;
  stroke-dashoffset: 240;
  filter: drop-shadow(0 0 8px rgba(255, 100, 150, 0.5)) drop-shadow(0 0 15px rgba(120, 80, 255, 0.3));
  animation: rainbow-draw-erase 4s ease-in-out infinite;
}
@keyframes rainbow-draw-erase {
  0%   { stroke-dashoffset: 240; opacity: 1; }
  25%  { stroke-dashoffset: 0; opacity: 1; }
  50%  { stroke-dashoffset: 0; opacity: 1; }
  75%  { stroke-dashoffset: -240; opacity: 1; }
  100% { stroke-dashoffset: -240; opacity: 1; }
}

/* 15. SHOOTING_STAR — 별똥별 가로지름 */
.shooting-star-container { position: absolute; inset: -2px; pointer-events: none; z-index: 6; overflow: hidden; }
.shooting-star {
  position: absolute; top: var(--y); left: -15%;
  width: 54px; height: 1.5px;
  background: linear-gradient(90deg, transparent 0%, rgba(255,255,220,0.25) 35%, rgba(255,255,255,0.85) 80%, transparent 100%);
  border-radius: 1px;
  animation: shoot-across 1.0s infinite var(--delay) ease-in;
  opacity: 0;
}
.shooting-star::after {
  content: ''; position: absolute; right: 0; top: -2px;
  width: 5px; height: 5px; background: white; border-radius: 50%;
  box-shadow: 0 0 5px 2px rgba(255,255,200,0.95), 0 0 12px rgba(200,225,255,0.7);
}
@keyframes shoot-across {
  0%   { opacity: 0; transform: translateX(-100%); }
  1.5%   { opacity: 3; }
  92%  { opacity: 1; }
  100% { opacity: 0; transform: translateX(250%); }
}

/* 16. BLACK_HOLE — 블랙홀 빨려들어감 */
.blackhole-container {
  position: absolute; right: -24px; top: 50%; transform: translateY(-50%);
  pointer-events: none; z-index: 4;
}
.bh-ring {
  width: 22px; height: 22px; border-radius: 50%;
  background: radial-gradient(circle, #000 32%, rgba(80,0,130,0.88) 62%, rgba(50,0,80,0.35) 82%, transparent 100%);
  animation: bh-pulse 1.4s ease-in-out infinite alternate, bh-appear 7s ease-in-out infinite;
}
@keyframes bh-pulse {
  from { box-shadow: 0 0 8px 2px rgba(120,0,200,0.75), 0 0 16px rgba(60,0,100,0.45); }
  to   { box-shadow: 0 0 14px 4px rgba(160,0,255,0.9), 0 0 26px rgba(80,0,150,0.6); }
}
@keyframes bh-appear {
  0%, 22%  { opacity: 0; transform: scale(0); }
  28%, 68% { opacity: 1; transform: scale(1); }
  76%, 100% { opacity: 0; transform: scale(0); }
}
.has-blackhole .animated-nickname {
  animation: blackhole-pull 7s ease-in-out infinite !important;
  transform-origin: right center;
}
@keyframes blackhole-pull {
  0%, 26%  { opacity: 1; transform: scale(1) translateX(0); filter: none; }
  48%      { opacity: 0.5; transform: scale(0.45) translateX(22px); filter: blur(1.5px); }
  58%, 78% { opacity: 0; transform: scale(0.05) translateX(40px); filter: blur(4px); }
  79%      { opacity: 0; transform: scale(1) translateX(0); filter: none; }
  88%, 100% { opacity: 1; transform: scale(1) translateX(0); filter: none; }
}

/* 17. WHITE_HOLE — 화이트홀 뱉어냄 */
.whitehole-container {
  position: absolute; left: -24px; top: 50%; transform: translateY(-50%);
  pointer-events: none; z-index: 4;
}
.wh-ring {
  width: 22px; height: 22px; border-radius: 50%;
  background: radial-gradient(circle, #ffffff 32%, rgba(210,235,255,0.88) 62%, rgba(170,210,255,0.3) 82%, transparent 100%);
  animation: wh-pulse 1.4s ease-in-out infinite alternate, wh-appear 7s ease-in-out infinite;
}
@keyframes wh-pulse {
  from { box-shadow: 0 0 8px 3px rgba(210,235,255,0.85), 0 0 18px rgba(150,200,255,0.55); }
  to   { box-shadow: 0 0 14px 5px rgba(230,245,255,0.95), 0 0 28px rgba(170,220,255,0.7); }
}
@keyframes wh-appear {
  0%, 10%  { opacity: 0; transform: scale(0); }
  18%, 38% { opacity: 1; transform: scale(1); }
  46%, 100% { opacity: 0; transform: scale(0); }
}
.has-whitehole .animated-nickname {
  animation: whitehole-eject 7s ease-in-out infinite !important;
  transform-origin: left center;
}
@keyframes whitehole-eject {
  0%, 14%  { opacity: 0; transform: scale(0.05) translateX(-40px); filter: blur(4px); }
  32%      { opacity: 0.5; transform: scale(0.5) translateX(-15px); filter: blur(1.5px); }
  43%, 74% { opacity: 1; transform: scale(1) translateX(0); filter: none; }
  88%      { opacity: 0.3; filter: blur(1px); }
  100%     { opacity: 0; transform: scale(0.05) translateX(-40px); filter: blur(4px); }
}

/* 18. BOMB — 폭탄 심줄 테두리 따라 타들어감 */
.bomb-fuse-container {
  position: absolute; inset: 0px 0px 0px 0px; 
  pointer-events: none; z-index: 7; overflow: visible;
   width: 100%; height: 100%;
}
.bomb-border-rect {
  fill: none;
  stroke: rgb(255, 255, 255);
  stroke-width: 5;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 400;
  stroke-dashoffset: 400;
  filter: drop-shadow(0 0 2px rgb(255, 135, 135)) 
  drop-shadow(0 0 20px rgba(255, 173, 173, 0.9)) 
  drop-shadow(0 0 20px rgba(255, 201, 201, 0.6));
  animation: bomb-burn-border 4s infinite ease-in;
}
@keyframes bomb-burn-border {
  0% { stroke-dashoffset: 400; opacity: 1; }
  90% { stroke-dashoffset: 0; opacity: 1; }
  100% { stroke-dashoffset: 0; opacity: 0; }
}

.has-bomb .animated-nickname {
  animation: bomb-nick-glow 3s infinite ease-in !important;
}
@keyframes bomb-nick-glow {
  0% { box-shadow: 0 0 5px rgba(255, 0, 0, 0.2); }
  50% { box-shadow: 0 0 12px rgba(255, 100, 100, 0.5); }
  100% { box-shadow: 0 0 5px rgba(255, 0, 0, 0); }
}

/* 12. VOID_RIFT — 차원 균열 파티클 orbit */
.void-container { position: absolute; inset: -22px; pointer-events: none; z-index: 5; }
.void-particle {
  position: absolute; top: 50%; left: 50%;
  width: var(--size); height: var(--size);
  margin: calc(var(--size) / -2) 0 0 calc(var(--size) / -2);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192,132,252,0.95), rgba(109,40,217,0.65));
  box-shadow: 0 0 5px rgba(167,139,250,0.9);
  animation: void-orbit var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes void-orbit {
  0%   { opacity: 0; transform: rotate(var(--angle)) translateX(var(--dist)) scale(0.4); }
  25%  { opacity: 1; }
  70%  { opacity: 0.7; transform: rotate(calc(var(--angle) + 130deg)) translateX(var(--dist)) scale(1.3); }
  100% { opacity: 0; transform: rotate(calc(var(--angle) + 210deg)) translateX(calc(var(--dist) * 0.25)) scale(0.2); }
}
.has-void .animated-nickname { border-color: #8b5cf6 !important; animation: void-distort 4.5s ease-in-out infinite !important; }
@keyframes void-distort {
  0%, 78%, 100% { transform: skewX(0) skewY(0); box-shadow: 0 0 7px rgba(109,40,217,0.5); }
  80% { transform: skewX(-3deg) skewY(1deg); box-shadow: 0 0 12px rgba(167,139,250,0.75); }
  82% { transform: skewX(2.5deg); box-shadow: 0 0 18px rgba(139,92,246,0.85); }
  84% { transform: skewX(0); box-shadow: 0 0 22px rgba(109,40,217,0.55), 0 0 40px rgba(76,29,149,0.3); }
  93% { transform: skewX(-1deg) scaleX(0.98); }
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

.nickname-tooltip { position: absolute; width: 190px; background: var(--card-bg) !important; background-image: linear-gradient(to bottom, var(--card-bg), var(--tier-bg)) !important; backdrop-filter: blur(20px); border: 1.5px solid var(--tier-color); border-radius: 16px; padding: 14px; box-shadow: 0 15px 40px rgba(0,0,0,0.3); z-index: 2000000; cursor: default; overflow: hidden; transition: opacity 0.3s, transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); pointer-events: auto; }
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
  .nickname-tooltip { width: 170px; padding: 10px; border-radius: 12px; }
  .nickname-tooltip.top { bottom: calc(100% + 10px); transform: translateX(-50%) scale(0.85) !important; }
  .user-avatar { width: 28px; height: 32px; font-size: 0.9rem; }
  .user-name-styled { font-size: 0.8rem; }
}
</style>
