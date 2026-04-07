<template>
  <div class="bubbles-container">
    <div v-for="n in 8" :key="`bubble-${n}`" class="bubble"
      :style="{
        '--left': `${Math.random() * 100}%`,
        '--size': `${4 + Math.random() * 6}px`,
        '--delay': `${Math.random() * 4}s`,
        '--dur': `${2 + Math.random() * 2}s`
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 2. Magic Bubbles (귀여운 버블) */
.bubbles-container { position: absolute; inset: -10px -5px; pointer-events: none; z-index: 1; overflow: hidden; }
.bubble {
  position: absolute; bottom: 0; left: var(--left); width: var(--size); height: var(--size);
  background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 50%;
  box-shadow: inset -2px -2px 4px rgba(0,0,0,0.05), 0 0 10px rgba(255,255,255,0.3);
  animation: bubble-float var(--dur) infinite ease-in var(--delay); opacity: 0;
}
@keyframes bubble-float {
  0% { transform: translateY(0) scale(0.5); opacity: 0; }
  20% { opacity: 0.8; }
  80% { opacity: 0.8; transform: translateY(-30px) translateX(10px); }
  100% { transform: translateY(-40px) scale(1.2); opacity: 0; }
}
</style>
