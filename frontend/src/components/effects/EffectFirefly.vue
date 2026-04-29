<template>
  <div class="firefly-container">
    <div v-for="n in 7" :key="`firefly-${n}`" class="firefly"
      :style="{
        '--angle': `${(n - 1) * 51.4}deg`,
        '--delay': `${((n * 0.71) % 3.2).toFixed(2)}s`,
        '--dur': `${(3.2 + (n % 3) * 0.7).toFixed(1)}s`,
        '--dist': `${26 + (n % 3) * 9}px`,
        '--size': `${4 + (n % 2) * 3}px`,
        '--blink': `${0.8 + (n % 4) * 0.35}s`
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.firefly-container { position: absolute; inset: -38px; pointer-events: none; z-index: 3; }
.firefly {
  position: absolute; top: 50%; left: 50%;
  width: var(--size); height: var(--size);
  margin: calc(var(--size) / -2) 0 0 calc(var(--size) / -2);
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #fffde7, #ffd600 60%, #ffb300);
  box-shadow: 0 0 5px 2px rgba(255, 214, 0, 0.85), 0 0 10px rgba(255, 240, 100, 0.5);
  animation:
    firefly-orbit var(--dur) ease-in-out infinite var(--delay),
    firefly-blink var(--blink) ease-in-out infinite var(--delay);
  opacity: 0;
}
@keyframes firefly-orbit {
  0%   { opacity: 0; transform: rotate(var(--angle)) translateX(var(--dist)); }
  15%  { opacity: 0.9; }
  50%  { transform: rotate(calc(var(--angle) + 180deg)) translateX(calc(var(--dist) * 0.65)); }
  85%  { opacity: 0.8; }
  100% { opacity: 0; transform: rotate(calc(var(--angle) + 360deg)) translateX(var(--dist)); }
}
@keyframes firefly-blink {
  0%, 100% { filter: brightness(1); box-shadow: 0 0 5px 2px rgba(255,214,0,0.85); }
  50% { filter: brightness(2.8) blur(0.5px); box-shadow: 0 0 10px 5px rgba(255,240,0,0.95); }
}
</style>
