<template>
  <div class="music-container">
    <div v-for="n in 7" :key="`note-${n}`" class="music-note"
      :style="{
        '--left': `${(n - 1) * 14}%`,
        '--delay': `${((n * 0.43) % 3.5).toFixed(2)}s`,
        '--dur': `${(2.4 + (n % 4) * 0.4).toFixed(1)}s`,
        '--size': `${9 + (n % 3) * 3}px`,
        '--sway': `${-12 + (n % 5) * 6}px`
      }"
    >{{ ['♪','♫','♩','♬','♪','♫','♩'][n - 1] }}</div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.music-container { position: absolute; inset: -14px -6px; pointer-events: none; z-index: 5; overflow: hidden; }
.music-note {
  position: absolute; bottom: -2px; left: var(--left);
  font-size: var(--size); line-height: 1;
  color: #f59e0b;
  filter: drop-shadow(0 0 4px rgba(245, 158, 11, 0.9));
  animation: note-rise var(--dur) infinite var(--delay) ease-out;
  opacity: 0;
}
@keyframes note-rise {
  0%   { opacity: 0; transform: translateY(0) translateX(0) rotate(0deg) scale(0.7); }
  12%  { opacity: 0.9; }
  50%  { transform: translateY(-22px) translateX(var(--sway)) rotate(15deg) scale(1); opacity: 0.85; }
  80%  { opacity: 0.3; }
  100% { opacity: 0; transform: translateY(-52px) translateX(calc(var(--sway) * 1.5)) rotate(30deg) scale(0.8); }
}
</style>
