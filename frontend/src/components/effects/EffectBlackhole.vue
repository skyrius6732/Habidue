<template>
  <div class="blackhole-container">
    <div class="bh-ring"></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 16. BLACK_HOLE: 블랙홀 빨려들어감 */
.blackhole-container {
  position: absolute; right: -24px; top: 50%; transform: translateY(-50%);
  pointer-events: none; z-index: 4;
}
.bh-ring {
  width: 22px; height: 22px; border-radius: 50%;
  background: radial-gradient(circle, #000 32%, rgba(80,0,130,0.88) 62%, rgba(50,0,80,0.35) 82%, transparent 100%);
  animation: bh-pulse 1.4s ease-in-out infinite alternate, bh-appear 7s ease-in-out infinite;
}
@keyframes bh-pulse {
  from { box-shadow: 0 0 8px 2px rgba(120,0,200,0.75), 0 0 16px rgba(60,0,100,0.45); }
  to   { box-shadow: 0 0 14px 4px rgba(160,0,255,0.9), 0 0 26px rgba(80,0,150,0.6); }
}
@keyframes bh-appear {
  0%, 22%  { opacity: 0; transform: scale(0); }
  28%, 68% { opacity: 1; transform: scale(1); }
  76%, 100% { opacity: 0; transform: scale(0); }
}
.has-blackhole .animated-nickname {
  animation: blackhole-pull 7s ease-in-out infinite !important;
  transform-origin: right center;
}
@keyframes blackhole-pull {
  0%, 26%  { opacity: 1; transform: scale(1) translateX(0); filter: none; }
  48%      { opacity: 0.5; transform: scale(0.45) translateX(22px); filter: blur(1.5px); }
  58%, 78% { opacity: 0; transform: scale(0.05) translateX(40px); filter: blur(4px); }
  79%      { opacity: 0; transform: scale(1) translateX(0); filter: none; }
  88%, 100% { opacity: 1; transform: scale(1) translateX(0); filter: none; }
}
</style>
