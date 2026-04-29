<template>
  <div class="feather-container">
    <div v-for="n in 7" :key="`fth-${n}`" class="feather"
      :style="{
        '--left': `${(n - 1) * 14}%`,
        '--delay': `${((n * 0.48) % 3.2).toFixed(2)}s`,
        '--dur': `${(2.8 + (n % 4) * 0.5).toFixed(1)}s`,
        '--sway': `${-10 + (n % 5) * 5}px`,
        '--size': `${10 + (n % 3) * 2}px`
      }"
    >❧</div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.feather-container { position: absolute; inset: -14px -6px; pointer-events: none; z-index: 5; overflow: hidden; }
.feather {
  position: absolute; top: -4px; left: var(--left);
  font-size: var(--size); line-height: 1;
  color: #e2e8f0;
  filter: drop-shadow(0 0 3px rgba(255, 255, 255, 0.7));
  animation: feather-drift var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes feather-drift {
  0%   { opacity: 0; transform: translateY(-4px) translateX(0) rotate(-10deg); }
  12%  { opacity: 0.85; }
  40%  { transform: translateY(16px) translateX(var(--sway)) rotate(8deg); opacity: 0.8; }
  70%  { opacity: 0.5; transform: translateY(30px) translateX(calc(var(--sway) * 0.5)) rotate(-5deg); }
  100% { opacity: 0; transform: translateY(48px) translateX(calc(var(--sway) * 1.4)) rotate(15deg); }
}
</style>
