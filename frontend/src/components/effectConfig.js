/**
 * 닉네임 이펙트 설정 모음
 * AnimatedNickname.vue에서 import해서 사용
 */

// 이펙트 목록 (선택 UI 드롭다운용)
export const SPECIAL_EFFECTS = [
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
  { id: 'ROSES_BLOOM', name: '🌹 장미 (Roses)', icon: '🌹' },
  { id: 'SHADOW_DEMON', name: '👹 섀도우 데몬 (Shadow)', icon: '👹' },
  { id: 'NEON_SIGN', name: '🌟 네온 사인 (Neon)', icon: '🌟' },
  { id: 'PIXEL_GLITCH', name: '💥 픽셀 글리치 (Glitch)', icon: '💥' },
  { id: 'VOID_RIFT', name: '🌀 보이드 리프트 (Void)', icon: '🌀' },
  { id: 'LOVE_HEART', name: '💕 두근두근 하트 (Heart)', icon: '💕' },
  { id: 'RAINBOW_WAVE', name: '🌈 무지개 (Rainbow)', icon: '🌈' },
  { id: 'SHOOTING_STAR', name: '💫 섬광 (Flash)', icon: '💫' },
  { id: 'BOMB', name: '🔫 레이저 (Laser)', icon: '🔫' },
  { id: 'CROWN', name: '👑 왕관 (Crown)', icon: '👑' },
  { id: 'SWORDS_CROSS', name: '⚔️ 칼 교차 (Swords)', icon: '⚔️' },
  { id: 'FLOWER_CROWN', name: '🌸 화관 (Flower Crown)', icon: '🌸' },
  { id: 'STAR_TIARA', name: '🌟 별 티아라 (Star Tiara)', icon: '🌟' },
  { id: 'FLOWER_RAIN', name: '🌺 꽃비 (Flower Rain)', icon: '🌺' },
  { id: 'SCAN_LINE', name: '📺 스캔라인 (Scan Line)', icon: '📺' },
  { id: 'CHROMATIC_ABERRATION', name: '🌈 색상 분리 (Chromatic)', icon: '🌈' },
  { id: 'ECHO_TRAIL', name: '👻 잔상 (Echo Trail)', icon: '👻' },
  { id: 'CORRUPTED_TEXT', name: '🔲 손상된 텍스트 (Corrupted)', icon: '🔲' },
  { id: 'GLITCH_SHIFT', name: '⚡ 글리치 (Glitch Shift)', icon: '⚡' },
  { id: 'LIQUID_DISTORT', name: '💧 액체 (Liquid Distort)', icon: '💧' },
  { id: 'HALO', name: '✨ 후광 (Halo)', icon: '✨' },
  { id: 'LEAVES', name: '🍂 낙엽 (Leaves)', icon: '🍂' },
  { id: 'BUTTERFLIES', name: '🦋 나비 (Butterflies)', icon: '🦋' },
  { id: 'ORBS', name: '🔮 구체들 (Orbs)', icon: '🔮' },
  { id: 'SCALE_COLLAPSE', name: '⬇️ 축소 & 사라지기 (Collapse)', icon: '⬇️' }
]

// 날개 이펙트별 색상 설정
export const WING_COLORS = {
  GOLD_WINGS: { s1: '#fff9c4', s2: '#facc15', s3: '#a16207', glow: 'rgba(250, 204, 21, 0.8)' },
  SILVER_WINGS: { s1: '#f8fafc', s2: '#94a3b8', s3: '#475569', glow: 'rgba(148, 163, 184, 0.7)' },
  BRONZE_WINGS: { s1: '#fed7aa', s2: '#ea580c', s3: '#7c2d12', glow: 'rgba(234, 88, 12, 0.6)' },
  PIONEER_WINGS: { s1: '#ffffff', s2: '#e2e8f0', s3: '#94a3b8', glow: 'rgba(255, 255, 255, 0.9)' }
}

export const DEFAULT_WING_COLOR = { s1: '#ffffff', s2: '#e2e8f0', s3: '#94a3b8', glow: 'rgba(255, 255, 255, 0.8)' }

// 날개 이펙트별 레이어 수
export const WING_LAYERS_MAP = {
  GOLD_WINGS: 4,
  SILVER_WINGS: 3,
  BRONZE_WINGS: 2,
  PIONEER_WINGS: 2
}

// 레벨별 이펙트 자동 해금 맵
export const LEVEL_UNLOCKS = {
  5: 'MAGIC_BUBBLES',
  10: 'BRONZE_WINGS',
  15: 'STARRY_NIGHT',
  20: 'SILVER_WINGS',
  25: 'BOMB',
  30: 'GOLD_WINGS',
  35: 'SHADOW_DEMON',
  40: 'VOID_RIFT',
  45: 'THUNDER_BLUE',
  50: 'NEON_SIGN',
  60: 'AURORA_FLAME',
  70: 'RAINBOW_WAVE'
}

// 파티클 시스템 레벨별 프리셋
export const PARTICLE_PRESETS = {
  level100: {
    fullScreen: { enable: false },
    particles: {
      number: { value: 15 },
      color: { value: '#facc15' },
      size: { value: { min: 1, max: 2.5 } },
      move: { enable: true, speed: 1.2, direction: 'top' }
    }
  },
  level90: {
    fullScreen: { enable: false },
    particles: {
      number: { value: 10 },
      color: { value: '#22d3ee' },
      shape: { type: 'star' },
      opacity: { value: 0.5 },
      size: { value: 1 },
      move: { enable: true, speed: 0.4 }
    }
  },
  level70: {
    fullScreen: { enable: false },
    particles: {
      number: { value: 10 },
      color: { value: ['#ff416c', '#ffd700', '#48bb78', '#4facfe', '#9400d3'] },
      opacity: { value: 0.4 },
      size: { value: { min: 1.5, max: 2.5 } },
      move: { enable: true, speed: 0.8, direction: 'top', outModes: 'out' }
    }
  },
  level50: {
    fullScreen: { enable: false },
    particles: {
      number: { value: 6 },
      color: { value: '#10b981' },
      size: { value: 1 },
      move: { enable: true, speed: 0.6, direction: 'top' }
    }
  }
}

export function getParticleOptionsForLevel(level) {
  if (level >= 100) return PARTICLE_PRESETS.level100
  if (level >= 90) return PARTICLE_PRESETS.level90
  if (level >= 70) return PARTICLE_PRESETS.level70
  if (level >= 50) return PARTICLE_PRESETS.level50
  return null
}

// 이펙트 플래그 helpers
export const EFFECT_CHECKS = {
  isWingsEffect: (effect) => effect?.endsWith('_WINGS'),
  isBubblesEffect: (effect) => effect === 'MAGIC_BUBBLES',
  isStarsEffect: (effect) => effect === 'STARRY_NIGHT',
  isThunderEffect: (effect) => effect === 'THUNDER_BLUE',
  isFlameEffect: (effect) => effect === 'AURORA_FLAME',
  isIceFrostEffect: (effect) => effect === 'ICE_FROST',
  isSakuraEffect: (effect) => effect === 'SAKURA_BLOOM',
  isShadowEffect: (effect) => effect === 'SHADOW_DEMON',
  isNeonEffect: (effect) => effect === 'NEON_SIGN',
  isGlitchEffect: (effect) => effect === 'PIXEL_GLITCH',
  isVoidEffect: (effect) => effect === 'VOID_RIFT',
  isHeartEffect: (effect) => effect === 'LOVE_HEART',
  isRainbowEffect: (effect) => effect === 'RAINBOW_WAVE',
  isShootingStarEffect: (effect) => effect === 'SHOOTING_STAR',
  isBombEffect: (effect) => effect === 'BOMB'
}
