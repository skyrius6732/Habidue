<template>
  <svg class="bomb-fuse-container" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
    <rect class="bomb-border-rect" x="4" y="4" width="92" height="92" />
  </svg>
</template>

<script setup>
</script>

<style scoped>
/* 18. BOMB: 폭탄 심줄 테두리 따라 타들어감 */
.bomb-fuse-container {
  position: absolute; inset: 0px 0px 0px 0px;
  pointer-events: none; z-index: 7; overflow: visible;
   width: 100%; height: 100%;
}
.bomb-border-rect {
  fill: none;
  stroke: rgb(255, 255, 255);
  stroke-width: 5;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 400;
  stroke-dashoffset: 400;
  filter: drop-shadow(0 0 2px rgb(255, 135, 135))
  drop-shadow(0 0 20px rgba(255, 173, 173, 0.9))
  drop-shadow(0 0 20px rgba(255, 201, 201, 0.6));
  animation: bomb-burn-border 4s infinite ease-in;
}
@keyframes bomb-burn-border {
  0% { stroke-dashoffset: 400; opacity: 1; }
  90% { stroke-dashoffset: 0; opacity: 1; }
  100% { stroke-dashoffset: 0; opacity: 0; }
}

.has-bomb .animated-nickname {
  animation: bomb-nick-glow 3s infinite ease-in !important;
}
@keyframes bomb-nick-glow {
  0% { box-shadow: 0 0 5px rgba(255, 0, 0, 0.2); }
  50% { box-shadow: 0 0 12px rgba(255, 100, 100, 0.5); }
  100% { box-shadow: 0 0 5px rgba(255, 0, 0, 0); }
}
</style>
