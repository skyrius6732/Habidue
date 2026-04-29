<template>
  <div class="coin-container">
    <div v-for="n in 8" :key="`coin-${n}`" class="coin"
      :style="{
        '--left': `${(n - 1) * 12 + 1}%`,
        '--delay': `${((n * 0.41) % 2.6).toFixed(2)}s`,
        '--dur': `${(1.7 + (n % 4) * 0.35).toFixed(1)}s`,
        '--size': `${6 + (n % 3) * 2}px`
      }"
    >
      <svg width="100%" height="100%" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <radialGradient id="coinGrad" cx="35%" cy="30%" r="65%">
            <stop offset="0%" stop-color="#ffffff"/>
            <stop offset="25%" stop-color="#fef08a"/>
            <stop offset="60%" stop-color="#facc15"/>
            <stop offset="100%" stop-color="#a16207"/>
          </radialGradient>
        </defs>
        <circle cx="10" cy="10" r="9.5" fill="#92400e"/>
        <circle cx="10" cy="10" r="8.8" fill="url(#coinGrad)"/>
        <circle cx="10" cy="10" r="6.5" fill="none" stroke="rgba(255,255,255,0.35)" stroke-width="0.8"/>
      </svg>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.coin-container { position: absolute; inset: -14px -6px; pointer-events: none; z-index: 5; overflow: hidden; }
.coin {
  position: absolute; top: -4px; left: var(--left);
  width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 3px rgba(250, 204, 21, 0.95)) drop-shadow(0 0 6px rgba(250, 204, 21, 0.6));
  animation: coin-fall var(--dur) infinite var(--delay) ease-in;
  opacity: 0;
}
@keyframes coin-fall {
  0%   { opacity: 0; transform: translateY(-4px) rotateY(0deg); }
  10%  { opacity: 1; }
  50%  { transform: translateY(18px) rotateY(180deg); opacity: 1; }
  85%  { opacity: 0.5; }
  100% { opacity: 0; transform: translateY(44px) rotateY(360deg); }
}
</style>
