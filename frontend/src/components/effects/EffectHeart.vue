<template>
  <div class="heart-container">
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
</template>

<script setup>
</script>

<style scoped>
/* 13. LOVE_HEART: 하트 아래서 위로 */
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
</style>
