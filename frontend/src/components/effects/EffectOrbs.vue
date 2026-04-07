<template>
  <div class="orbs-container">
    <div v-for="n in 5" :key="`orb-${n}`" class="orb-particle"
      :style="{
        '--angle': `${(n - 1) * 72}deg`,
        '--delay': `${((n * 0.3) % 2).toFixed(2)}s`,
        '--dur': `${(2.2 + (n % 2) * 0.4).toFixed(1)}s`,
        '--dist': `${25 + (n % 2) * 5}px`,
        '--size': `${5 + (n % 2) * 2}px`,
        '--hue': `${(n - 1) * 72}`
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Orbs - 구체들 */
.orbs-container { position: absolute; inset: -30px; pointer-events: none; z-index: 3; }
.orb-particle {
  position: absolute; top: 50%; left: 50%;
  width: var(--size); height: var(--size);
  margin: calc(var(--size) / -2) 0 0 calc(var(--size) / -2);
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.9), hsl(var(--hue), 100%, 60%));
  box-shadow: 0 0 8px hsl(var(--hue), 100%, 50%), inset -1px -1px 2px rgba(0, 0, 0, 0.3);
  animation: orb-orbit var(--dur) linear infinite var(--delay);
  opacity: 0;
}
@keyframes orb-orbit {
  0% {
    opacity: 0;
    transform: rotate(var(--angle)) translateX(var(--dist)) scale(0.5);
  }
  20% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: rotate(calc(var(--angle) + 360deg)) translateX(var(--dist)) scale(0.5);
  }
}
</style>
