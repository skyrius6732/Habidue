<template>
  <div class="sakura-container">
    <svg v-for="n in 9" :key="`petal-${n}`" class="sakura-petal" viewBox="0 0 20 30"
      :style="{
        '--left': `${(n - 1) * 11 - 3}%`,
        '--delay': `${((n * 0.55) % 2.8).toFixed(2)}s`,
        '--dur': `${(2.4 + (n % 4) * 0.5).toFixed(1)}s`,
        '--size': `${6 + (n % 3) * 2}px`,
        '--sway': `${-22 + (n % 5) * 11}px`,
        '--rot-s': `${-40 + (n % 4) * 22}deg`
      }"
    >
      <template v-if="n % 3 === 0">
        <path d="M10,1 C13,1 16,5 15,10 C14,16 12,22 10,28 C8,22 6,16 5,10 C4,5 7,1 10,1Z"
          :fill="`hsl(${340 + (n % 4) * 7}, 86%, ${74 + (n % 3) * 5}%)`" opacity="0.9"/>
        <path d="M10,4 C11.5,4 12.5,8 12,13 C11.5,17 11,21 10,25 C9,21 8.5,17 8,13 C7.5,8 8.5,4 10,4Z"
          fill="rgba(255,255,255,0.35)"/>
      </template>
      <template v-else-if="n % 3 === 1">
        <path d="M10,3 C16,3 19,8 17,14 C15,19 12,24 10,27 C8,24 5,19 3,14 C1,8 4,3 10,3Z"
          :fill="`hsl(${338 + (n % 4) * 7}, 82%, ${76 + (n % 3) * 5}%)`" opacity="0.87"/>
        <path d="M10,6 C13,6 15,10 14,14 C13,18 11.5,22 10,25 C8.5,22 7,18 6,14 C5,10 7,6 10,6Z"
          fill="rgba(255,255,255,0.3)"/>
      </template>
      <template v-else>
        <path d="M8,1 C13,1 17,6 16,12 C15,18 12,24 10,28 C8,24 6,18 5.5,12 C5,7 7,2 8,1Z"
          :fill="`hsl(${342 + (n % 4) * 7}, 84%, ${75 + (n % 3) * 5}%)`" opacity="0.88"/>
        <path d="M9,5 C11,5 12.5,9 12,13 C11.5,17 11,21 10,25 C9,21 8,17 7.5,13 C7,9 7.5,5 9,5Z"
          fill="rgba(255,255,255,0.28)"/>
      </template>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 8. SAKURA_BLOOM: 벚꽃잎 살랑살랑 */
.sakura-container { position: absolute; inset: -18px -8px; pointer-events: none; z-index: 5; overflow: hidden; }
.sakura-petal {
  position: absolute; top: -14px; left: var(--left); width: var(--size); height: calc(var(--size) * 1.7);
  filter: drop-shadow(0 0 2px rgba(251, 164, 175, 0.65));
  animation: petal-fall var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
  transform-origin: center 60%;
}
@keyframes petal-fall {
  0%   { opacity: 0; transform: translateY(-5px) translateX(0) rotate(var(--rot-s)); }
  10%  { opacity: 0.92; }
  30%  { transform: translateY(12px) translateX(var(--sway)) rotate(calc(var(--rot-s) + 42deg)); }
  58%  { transform: translateY(26px) translateX(calc(var(--sway) * -0.75)) rotate(calc(var(--rot-s) + 88deg)); }
  82%  { opacity: 0.5; transform: translateY(40px) translateX(calc(var(--sway) * 0.5)) rotate(calc(var(--rot-s) + 125deg)); }
  100% { opacity: 0; transform: translateY(50px) translateX(calc(var(--sway) * -0.2)) rotate(calc(var(--rot-s) + 158deg)); }
}
.has-sakura .animated-nickname { border-color: #fb7185 !important; animation: sakura-glow 2s ease-in-out infinite alternate !important; }
@keyframes sakura-glow {
  from { box-shadow: 0 0 5px rgba(251,113,133,0.35); }
  to   { box-shadow: 0 0 12px rgba(253,164,175,0.65), 0 0 22px rgba(251,113,133,0.28); }
}
</style>
