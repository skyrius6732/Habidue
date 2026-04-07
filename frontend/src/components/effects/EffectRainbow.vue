<template>
  <svg class="rainbow-arch-wrap" viewBox="0 0 200 170" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
    <defs>
      <linearGradient id="rainbowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" style="stop-color:#ff0000;stop-opacity:1" />
        <stop offset="16.67%" style="stop-color:#ff7700;stop-opacity:1" />
        <stop offset="33.33%" style="stop-color:#ffee00;stop-opacity:1" />
        <stop offset="50%" style="stop-color:#00dd44;stop-opacity:1" />
        <stop offset="66.67%" style="stop-color:#0088ff;stop-opacity:1" />
        <stop offset="83.33%" style="stop-color:#6600dd;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#cc00ff;stop-opacity:1" />
      </linearGradient>
    </defs>
    <path class="rainbow-arch" d="M 20,70 Q 100,15 180,70" stroke="url(#rainbowGrad)" stroke-width="15" fill="none" stroke-linecap="round" />
  </svg>
</template>

<script setup>
</script>

<style scoped>
/* 14. RAINBOW_WAVE: 무지개 아치형 */
.rainbow-arch-wrap {
  position: absolute; bottom: -10px; left: 0; right: 0;
  pointer-events: none; z-index: 3; width: 100%; height: 80px;
}
.rainbow-arch {
  stroke-dasharray: 240;
  stroke-dashoffset: 240;
  filter: drop-shadow(0 0 8px rgba(255, 100, 150, 0.5)) drop-shadow(0 0 15px rgba(120, 80, 255, 0.3));
  animation: rainbow-draw-erase 4s ease-in-out infinite;
}
@keyframes rainbow-draw-erase {
  0%   { stroke-dashoffset: 240; opacity: 1; }
  25%  { stroke-dashoffset: 0; opacity: 1; }
  50%  { stroke-dashoffset: 0; opacity: 1; }
  75%  { stroke-dashoffset: -240; opacity: 1; }
  100% { stroke-dashoffset: -240; opacity: 1; }
}
</style>
