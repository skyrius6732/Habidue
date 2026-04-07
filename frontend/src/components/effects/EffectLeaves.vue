<template>
  <div class="leaves-container">
    <svg v-for="n in 8" :key="`leaf-${n}`" class="falling-leaf" viewBox="0 0 24 24"
      :style="{
        '--left': `${(n - 1) * 12}%`,
        '--delay': `${((n * 0.45) % 3.2).toFixed(2)}s`,
        '--dur': `${(2.8 + (n % 3) * 0.6).toFixed(1)}s`,
        '--size': `${7 + (n % 3) * 3}px`,
        '--sway': `${-18 + (n % 4) * 10}px`,
        '--hue': `${n % 3 === 0 ? 15 : (n % 3 === 1 ? 45 : 0)}`
      }"
    >
      <!-- 낙엽 모양 (메이플 잎) - 주황색, 노란색, 빨간색 혼합 -->
      <path :fill="`hsl(${n % 3 === 0 ? 15 : (n % 3 === 1 ? 45 : 0)}, 85%, 55%)`" d="M12,2 L14,8 L20,6 L17,12 L22,15 L16,15 L18,21 L12,18 L6,21 L8,15 L2,15 L7,12 L4,6 L10,8 Z" opacity="0.9" stroke="rgba(0,0,0,0.1)" stroke-width="0.3"/>
      <!-- 잎맥 -->
      <path fill="none" :stroke="`hsl(${n % 3 === 0 ? 15 : (n % 3 === 1 ? 45 : 0)}, 75%, 35%)`" stroke-width="0.4" d="M12,2 L12,18 M12,10 L6,8 M12,10 L18,8 M12,10 L8,15 M12,10 L16,15" opacity="0.4"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Leaves - 낙엽 */
.leaves-container { position: absolute; inset: -12px -8px; pointer-events: none; z-index: 5; overflow: hidden; }
.falling-leaf {
  position: absolute; top: -10px; left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 2px rgba(210, 105, 30, 0.6));
  animation: leaf-fall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
  transform-origin: center;
}
@keyframes leaf-fall {
  0%   { opacity: 0; transform: translateY(-5px) translateX(0) rotate(0deg); }
  10%  { opacity: 0.9; }
  50%  { transform: translateY(20px) translateX(var(--sway)) rotate(180deg); opacity: 0.85; }
  85%  { opacity: 0.4; transform: translateY(45px) translateX(calc(var(--sway) * 0.5)) rotate(360deg); }
  100% { opacity: 0; transform: translateY(55px) translateX(0) rotate(450deg); }
}
</style>
