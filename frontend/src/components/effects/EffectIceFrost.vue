<template>
  <div class="frost-container">
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
</template>

<script setup>
</script>

<style scoped>
/* 7. ICE_FROST: 눈결정 흩날림 */
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
</style>
