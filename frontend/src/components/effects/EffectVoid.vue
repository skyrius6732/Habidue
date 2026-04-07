<template>
  <div class="void-container">
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
</template>

<script setup>
</script>

<style scoped>
/* 12. VOID_RIFT: 차원 균열 보라 파티클 orbit */
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
</style>
