<template>
  <div class="snowfall-container">
    <div v-for="n in 10" :key="`snow-${n}`" class="snowflake"
      :style="{
        '--left': `${(n - 1) * 10}%`,
        '--delay': `${((n * 0.37) % 3.5).toFixed(2)}s`,
        '--dur': `${(2.5 + (n % 4) * 0.5).toFixed(1)}s`,
        '--size': `${7 + (n % 3) * 3}px`,
        '--sway': `${-14 + (n % 5) * 7}px`,
        '--op': `${0.55 + (n % 3) * 0.15}`
      }"
    >{{ ['❄','❅','❆','❄','❅','❆','❄','❅','❆','❄'][n - 1] }}</div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.snowfall-container { position: absolute; inset: -14px -8px; pointer-events: none; z-index: 5; overflow: hidden; }
.snowflake {
  position: absolute; top: -12px; left: var(--left);
  font-size: var(--size); color: #e0f7ff; line-height: 1;
  filter: drop-shadow(0 0 4px rgba(160, 220, 255, 0.9));
  animation: snow-fall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes snow-fall {
  0%   { opacity: 0; transform: translateY(-5px) translateX(0) rotate(0deg); }
  10%  { opacity: var(--op); }
  50%  { transform: translateY(24px) translateX(var(--sway)) rotate(140deg); opacity: var(--op); }
  85%  { opacity: calc(var(--op) * 0.4); }
  100% { opacity: 0; transform: translateY(56px) translateX(calc(var(--sway) * 0.3)) rotate(280deg); }
}
</style>
