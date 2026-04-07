<template>
  <div class="butterflies-container">
    <svg v-for="n in 10" :key="`butterfly-${n}`" class="butterfly" viewBox="0 0 24 20"
      :style="{
        '--angle': `${(n - 1) * 36}deg`,
        '--delay': `${(Math.random() * 3).toFixed(2)}s`,
        '--dur': `${(3.2 + Math.random() * 1.5).toFixed(1)}s`,
        '--size': `${8 + Math.random() * 5}px`,
        '--hue': `${[0, 30, 60, 120, 280, 320, 45, 15, 200, 350][n - 1]}`,
        '--sway-y1': `${-10 + Math.random() * 6}px`,
        '--sway-y2': `${-15 + Math.random() * 8}px`,
        '--sway-y3': `${-2 + Math.random() * 4}px`,
        '--sway-y4': `${10 + Math.random() * 8}px`,
        '--dist-x': `${45 + Math.random() * 15}px`
      }"
    >
      <!-- 왼쪽 날개 -->
      <path :fill="`hsl(var(--hue), 85%, 60%)`" d="M8,10 Q4,4 2,8 Q2,12 6,14 Q8,12 8,10 Z" opacity="0.9"/>
      <!-- 오른쪽 날개 -->
      <path :fill="`hsl(var(--hue), 85%, 60%)`" d="M16,10 Q20,4 22,8 Q22,12 18,14 Q16,12 16,10 Z" opacity="0.9"/>
      <!-- 몸 -->
      <ellipse cx="12" cy="10" rx="2" ry="4" fill="#333" opacity="0.8"/>
      <!-- 패턴 -->
      <circle cx="5" cy="8" r="1.5" fill="rgba(255,255,255,0.5)"/>
      <circle cx="19" cy="8" r="1.5" fill="rgba(255,255,255,0.5)"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Butterflies - 나비 */
.butterflies-container { position: absolute; inset: -50px; pointer-events: none; z-index: 4; }
.butterfly {
  position: absolute; top: 50%; left: 50%;
  width: var(--size); height: var(--size);
  margin-left: calc(var(--size) / -2);
  margin-top: calc(var(--size) / -2);
  filter: drop-shadow(0 0 2px rgba(100, 100, 100, 0.4));
  animation: butterfly-fly var(--dur) ease-in-out infinite var(--delay);
  opacity: 0;
}

@keyframes butterfly-fly {
  0% {
    opacity: 0;
    transform: rotate(var(--angle)) translateX(var(--dist-x)) translateY(var(--sway-y1)) rotate(calc(-1 * var(--angle))) scaleX(1);
  }
  10% {
    opacity: 1;
  }
  25% {
    transform: rotate(calc(var(--angle) + 15deg)) translateX(calc(var(--dist-x) * 0.78)) translateY(var(--sway-y2)) rotate(calc(-1 * (var(--angle) + 15deg))) scaleX(-1);
  }
  50% {
    transform: rotate(var(--angle)) translateX(calc(var(--dist-x) * 0.56)) translateY(var(--sway-y3)) rotate(calc(-1 * var(--angle))) scaleX(1);
  }
  75% {
    transform: rotate(calc(var(--angle) - 15deg)) translateX(calc(var(--dist-x) * 0.78)) translateY(var(--sway-y4)) rotate(calc(-1 * (var(--angle) - 15deg))) scaleX(-1);
  }
  90% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: rotate(var(--angle)) translateX(var(--dist-x)) translateY(var(--sway-y1)) rotate(calc(-1 * var(--angle))) scaleX(1);
  }
}
</style>
