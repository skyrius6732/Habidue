<template>
  <div class="flower-rain-container">
    <div v-for="n in 10" :key="`flower-${n}`" class="flower"
      :style="{
        '--left': `${(n - 1) * 10}%`,
        '--delay': `${((n * 0.6) % 3).toFixed(2)}s`,
        '--dur': `${(3 + (n % 3) * 0.5).toFixed(1)}s`,
        '--sway': `${-15 + (n % 4) * 10}px`,
        '--flower-color': `${['#ff69b4', '#ff1493', '#ffa500', '#ffb6c1', '#ff6347', '#ff4500', '#da70d6', '#ff00ff', '#ff69b4', '#ffc0cb'][n - 1]}`
      }"
    >🌸</div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Flower Rain - 꽃잎 내림 */
.flower-rain-container { position: absolute; inset: -20px; pointer-events: none; z-index: 5; overflow: hidden; }
.flower {
  position: absolute; top: -14px; left: var(--left); font-size: 10px; line-height: 1;
  filter: drop-shadow(0 0 3px rgba(255, 105, 180, 0.6));
  animation: flower-fall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes flower-fall {
  0% {
    opacity: 0;
    transform: translateY(-10px) translateX(0) rotate(0deg);
  }
  10% {
    opacity: 0.9;
  }
  50% {
    opacity: 0.8;
    transform: translateY(45px) translateX(var(--sway)) rotate(180deg);
  }
  90% {
    opacity: 0.5;
  }
  100% {
    opacity: 0;
    transform: translateY(80px) translateX(calc(var(--sway) * 0.5)) rotate(360deg);
  }
}
</style>
