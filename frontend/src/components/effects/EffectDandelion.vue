<template>
  <div class="dandelion-container">
    <svg v-for="n in 11" :key="`seed-${n}`" class="dandelion-seed" viewBox="0 0 12 22"
      :style="{
        '--left': `${(n - 1) * 9}%`,
        '--delay': `${((n * 0.33) % 3.2).toFixed(2)}s`,
        '--dur': `${(3 + (n % 5) * 0.4).toFixed(1)}s`,
        '--sway': `${-18 + (n % 5) * 9}px`,
        '--size': `${8 + (n % 3) * 3}px`
      }"
    >
      <!-- 씨앗 몸체 -->
      <ellipse cx="6" cy="20" rx="1.5" ry="1.8" fill="rgba(200,170,120,0.9)"/>
      <!-- 줄기 -->
      <line x1="6" y1="18.2" x2="6" y2="11" stroke="rgba(210,210,200,0.8)" stroke-width="0.5"/>
      <!-- 솜털 라인 (방사형) -->
      <line x1="6" y1="11" x2="3" y2="7.5" stroke="rgba(240,240,240,0.9)" stroke-width="0.45"/>
      <line x1="6" y1="11" x2="9" y2="7.5" stroke="rgba(240,240,240,0.9)" stroke-width="0.45"/>
      <line x1="6" y1="11" x2="6" y2="6.5" stroke="rgba(240,240,240,0.9)" stroke-width="0.45"/>
      <line x1="6" y1="11" x2="1.5" y2="9.5" stroke="rgba(240,240,240,0.9)" stroke-width="0.4"/>
      <line x1="6" y1="11" x2="10.5" y2="9.5" stroke="rgba(240,240,240,0.9)" stroke-width="0.4"/>
      <line x1="6" y1="11" x2="2" y2="13" stroke="rgba(240,240,240,0.85)" stroke-width="0.4"/>
      <line x1="6" y1="11" x2="10" y2="13" stroke="rgba(240,240,240,0.85)" stroke-width="0.4"/>
      <!-- 끝 동그라미 -->
      <circle cx="3" cy="7.5" r="1.1" fill="rgba(255,255,255,0.95)"/>
      <circle cx="9" cy="7.5" r="1.1" fill="rgba(255,255,255,0.95)"/>
      <circle cx="6" cy="6.5" r="1.1" fill="rgba(255,255,255,0.95)"/>
      <circle cx="1.5" cy="9.5" r="0.9" fill="rgba(255,255,255,0.9)"/>
      <circle cx="10.5" cy="9.5" r="0.9" fill="rgba(255,255,255,0.9)"/>
      <circle cx="2" cy="13" r="0.9" fill="rgba(255,255,255,0.85)"/>
      <circle cx="10" cy="13" r="0.9" fill="rgba(255,255,255,0.85)"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dandelion-container { position: absolute; inset: -16px -8px; pointer-events: none; z-index: 5; overflow: hidden; }
.dandelion-seed {
  position: absolute; bottom: -4px; left: var(--left);
  width: var(--size); height: calc(var(--size) * 2.2);
  filter: drop-shadow(0 0 2px rgba(255, 255, 255, 0.7));
  animation: dandelion-float var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes dandelion-float {
  0%   { opacity: 0; transform: translateY(0) translateX(0) rotate(-5deg); }
  10%  { opacity: 0.8; }
  45%  { transform: translateY(-28px) translateX(var(--sway)) rotate(15deg); opacity: 0.75; }
  80%  { opacity: 0.25; }
  100% { opacity: 0; transform: translateY(-62px) translateX(calc(var(--sway) * 1.6)) rotate(35deg); }
}
</style>
