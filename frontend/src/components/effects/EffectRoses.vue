<template>
  <div class="roses-container">
    <svg v-for="n in 9" :key="`rose-${n}`" class="rose-petal" viewBox="0 0 24 24"
      :style="{
        '--left': `${(n - 1) * 11 - 3}%`,
        '--delay': `${((n * 0.55) % 2.8).toFixed(2)}s`,
        '--dur': `${(2.6 + (n % 4) * 0.5).toFixed(1)}s`,
        '--size': `${7 + (n % 3) * 2}px`,
        '--sway': `${-20 + (n % 5) * 10}px`,
        '--rot-s': `${-30 + (n % 4) * 20}deg`,
        '--rose-color': `${n % 3 === 0 ? 'hsl(0, 0%, 95%)' : (n % 3 === 1 ? 'hsl(350, 90%, 48%)' : 'hsl(330, 100%, 62%)')}`
      }"
    >
      <!-- 장미 꽃잎 (외부) -->
      <circle cx="12" cy="12" r="8" :fill="getRoseColor(n)" opacity="0.95"/>
      <!-- 장미 꽃잎 (중간) -->
      <circle cx="12" cy="12" r="6" :fill="getRoseColor(n, true)" opacity="0.9"/>
      <!-- 장미 꽃잎 (내부) -->
      <circle cx="12" cy="12" r="4" :fill="getRoseColor(n, false, true)" opacity="0.85"/>
      <!-- 장미 중심 -->
      <circle cx="12" cy="12" r="2" fill="#2d5a3d" opacity="0.7"/>
      <!-- 꽃받침 -->
      <path d="M8,18 Q10,20 12,22 Q14,20 16,18 Q14,19 12,19 Q10,19 8,18 Z" fill="#2d5a3d" opacity="0.6"/>
    </svg>
  </div>
</template>

<script setup>
const getRoseColor = (n, isDarker = false, isCenter = false) => {
  const colorType = n % 3
  if (colorType === 0) {
    // 백장미
    if (isDarker) return 'hsl(0, 0%, 98%)'
    if (isCenter) return 'hsl(30, 40%, 90%)'
    return 'hsl(0, 0%, 95%)'
  } else if (colorType === 1) {
    // 빨간장미
    if (isDarker) return 'hsl(350, 95%, 42%)'
    if (isCenter) return 'hsl(0, 100%, 35%)'
    return 'hsl(350, 90%, 48%)'
  } else {
    // 분홍장미
    if (isDarker) return 'hsl(330, 100%, 55%)'
    if (isCenter) return 'hsl(330, 80%, 45%)'
    return 'hsl(330, 100%, 62%)'
  }
}
</script>

<style scoped>
/* Roses - 장미 */
.roses-container { position: absolute; inset: -18px -8px; pointer-events: none; z-index: 5; overflow: hidden; }
.rose-petal {
  position: absolute; top: -14px; left: var(--left); width: var(--size); height: calc(var(--size) * 1.5);
  filter: drop-shadow(0 0 2px rgba(200, 100, 120, 0.5));
  animation: rose-fall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
  transform-origin: center 60%;
}

@keyframes rose-fall {
  0%   { opacity: 0; transform: translateY(-5px) translateX(0) rotate(var(--rot-s)); }
  10%  { opacity: 0.9; }
  30%  { transform: translateY(12px) translateX(var(--sway)) rotate(calc(var(--rot-s) + 40deg)); }
  58%  { transform: translateY(26px) translateX(calc(var(--sway) * -0.75)) rotate(calc(var(--rot-s) + 90deg)); }
  82%  { opacity: 0.5; transform: translateY(40px) translateX(calc(var(--sway) * 0.5)) rotate(calc(var(--rot-s) + 140deg)); }
  100% { opacity: 0; transform: translateY(50px) translateX(calc(var(--sway) * -0.2)) rotate(calc(var(--rot-s) + 180deg)); }
}
</style>
