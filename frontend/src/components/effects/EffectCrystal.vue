<template>
  <div class="crystal-container">
    <div v-for="n in 6" :key="`gem-${n}`" class="gem"
      :style="{
        '--angle': `${(n - 1) * 60}deg`,
        '--delay': `${((n * 0.55) % 3).toFixed(2)}s`,
        '--dur': `${(3 + (n % 3) * 0.5).toFixed(1)}s`,
        '--color': ['#7dd3fc','#a78bfa','#67e8f9','#818cf8','#e879f9','#38bdf8'][n - 1]
      }"
    >◆</div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.crystal-container { position: absolute; inset: -18px -10px; pointer-events: none; z-index: 5; }
.gem {
  position: absolute; top: 50%; left: 50%;
  font-size: 8px; line-height: 1;
  color: var(--color);
  filter: drop-shadow(0 0 5px var(--color));
  transform-origin: 0 0;
  animation: gem-orbit var(--dur) linear infinite var(--delay);
  opacity: 0.85;
}
@keyframes gem-orbit {
  0%   { transform: rotate(var(--angle)) translateX(22px) rotate(calc(-1 * var(--angle))) scale(0.8); }
  25%  { transform: rotate(calc(var(--angle) + 90deg)) translateX(24px) rotate(calc(-1 * (var(--angle) + 90deg))) scale(1.1); }
  50%  { transform: rotate(calc(var(--angle) + 180deg)) translateX(22px) rotate(calc(-1 * (var(--angle) + 180deg))) scale(0.9); }
  75%  { transform: rotate(calc(var(--angle) + 270deg)) translateX(25px) rotate(calc(-1 * (var(--angle) + 270deg))) scale(1.05); }
  100% { transform: rotate(calc(var(--angle) + 360deg)) translateX(22px) rotate(calc(-1 * (var(--angle) + 360deg))) scale(0.8); }
}
</style>
