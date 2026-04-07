<template>
  <div class="crown-container">
    <svg class="crown-svg" viewBox="0 0 100 60" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="crownGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style="stop-color:#ffd700;stop-opacity:1" />
          <stop offset="50%" style="stop-color:#ffed4e;stop-opacity:1" />
          <stop offset="100%" style="stop-color:#ffc700;stop-opacity:1" />
        </linearGradient>
      </defs>
      <!-- 왕관 베이스 -->
      <path d="M 10,45 L 20,20 L 35,35 L 50,10 L 65,35 L 80,20 L 90,45 Q 50,55 10,45 Z" fill="url(#crownGrad)" stroke="#e6b800" stroke-width="1.5"/>
      <!-- 보석 -->
      <circle cx="20" cy="20" r="4" fill="#ff1493" filter="drop-shadow(0 0 4px #ff1493)"/>
      <circle cx="50" cy="10" r="5" fill="#ff69b4" filter="drop-shadow(0 0 6px #ff69b4)"/>
      <circle cx="80" cy="20" r="4" fill="#ff1493" filter="drop-shadow(0 0 4px #ff1493)"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Crown - 왕관 */
.crown-container {
  position: absolute; top: -18px; left: 50%; transform: translateX(-50%);
  pointer-events: none; z-index: 8; width: 33px; height: 20px;
}
.crown-svg {
  width: 100%; height: 100%;
  filter: drop-shadow(0 0 2px rgba(255, 215, 0, 0.8)) drop-shadow(0 0 4px rgba(255, 105, 180, 0.6));
  animation: crown-shine 2s ease-in-out infinite;
}
@keyframes crown-shine {
  0%, 100% { opacity: 1; transform: scaleY(1); }
  50% { opacity: 0.85; transform: scaleY(1.05); }
}
</style>
