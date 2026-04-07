<template>
  <div class="whitehole-container">
    <div class="wh-ring"></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 17. WHITE_HOLE: 화이트홀 뱉어냄 */
.whitehole-container {
  position: absolute; left: -24px; top: 50%; transform: translateY(-50%);
  pointer-events: none; z-index: 4;
}
.wh-ring {
  width: 22px; height: 22px; border-radius: 50%;
  background: radial-gradient(circle, #ffffff 32%, rgba(210,235,255,0.88) 62%, rgba(170,210,255,0.3) 82%, transparent 100%);
  animation: wh-pulse 1.4s ease-in-out infinite alternate, wh-appear 7s ease-in-out infinite;
}
@keyframes wh-pulse {
  from { box-shadow: 0 0 8px 3px rgba(210,235,255,0.85), 0 0 18px rgba(150,200,255,0.55); }
  to   { box-shadow: 0 0 14px 5px rgba(230,245,255,0.95), 0 0 28px rgba(170,220,255,0.7); }
}
@keyframes wh-appear {
  0%, 10%  { opacity: 0; transform: scale(0); }
  18%, 38% { opacity: 1; transform: scale(1); }
  46%, 100% { opacity: 0; transform: scale(0); }
}
.has-whitehole .animated-nickname {
  animation: whitehole-eject 7s ease-in-out infinite !important;
  transform-origin: left center;
}
@keyframes whitehole-eject {
  0%, 14%  { opacity: 0; transform: scale(0.05) translateX(-40px); filter: blur(4px); }
  32%      { opacity: 0.5; transform: scale(0.5) translateX(-15px); filter: blur(1.5px); }
  43%, 74% { opacity: 1; transform: scale(1) translateX(0); filter: none; }
  88%      { opacity: 0.3; filter: blur(1px); }
  100%     { opacity: 0; transform: scale(0.05) translateX(-40px); filter: blur(4px); }
}
</style>
