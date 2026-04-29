<template>
  <div class="confetti-container">
    <div v-for="n in 16" :key="`cf-${n}`" class="piece"
      :style="{
        '--left': `${(n - 1) * 6 + 1}%`,
        '--delay': `${((n * 0.29) % 2.4).toFixed(2)}s`,
        '--dur': `${(1.5 + (n % 5) * 0.3).toFixed(1)}s`,
        '--color': ['#f43f5e','#fb923c','#facc15','#4ade80','#38bdf8','#818cf8','#e879f9','#f9a8d4'][((n-1) % 8)],
        '--rot': `${(n * 37) % 360}deg`,
        '--w': `${4 + (n % 3) * 2}px`,
        '--h': `${6 + (n % 4) * 2}px`
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.confetti-container { position: absolute; inset: -14px -6px; pointer-events: none; z-index: 5; overflow: hidden; }
.piece {
  position: absolute; top: -4px; left: var(--left);
  width: var(--w); height: var(--h);
  background: var(--color);
  border-radius: 1px;
  opacity: 0;
  animation: confetti-fall var(--dur) infinite var(--delay) ease-in;
}
@keyframes confetti-fall {
  0%   { opacity: 0; transform: translateY(-4px) rotate(var(--rot)) scale(0.8); }
  10%  { opacity: 1; }
  50%  { transform: translateY(20px) rotate(calc(var(--rot) + 180deg)) scale(1); opacity: 0.9; }
  85%  { opacity: 0.5; }
  100% { opacity: 0; transform: translateY(46px) rotate(calc(var(--rot) + 360deg)) scale(0.7); }
}
</style>
