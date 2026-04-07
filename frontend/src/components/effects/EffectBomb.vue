<template>
  <svg class="laser-container" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
    <!-- 상단 레이저 -->
    <line class="laser-line laser-top" x1="4" y1="4" x2="96" y2="4" />
    <!-- 우측 레이저 -->
    <line class="laser-line laser-right" x1="96" y1="4" x2="96" y2="96" />
    <!-- 하단 레이저 -->
    <line class="laser-line laser-bottom" x1="96" y1="96" x2="4" y2="96" />
    <!-- 좌측 레이저 -->
    <line class="laser-line laser-left" x1="4" y1="96" x2="4" y2="4" />
  </svg>
</template>

<script setup>
</script>

<style scoped>
/* LASER: 레이저 테두리 */
.laser-container {
  position: absolute; inset: 0px 0px 0px 0px;
  pointer-events: none; z-index: 7; overflow: visible;
  width: 100%; height: 100%;
}

.laser-line {
  stroke: rgb(255, 255, 255);
  stroke-linecap: round;
  stroke-dasharray: 400;
  stroke-dashoffset: 400;
  filter: drop-shadow(0 0 3px rgb(0, 255, 150))
  drop-shadow(0 0 15px rgba(0, 255, 150, 0.8))
  drop-shadow(0 0 25px rgba(100, 255, 200, 0.5));
  animation: laser-scan 3s infinite ease-in;
}

/* 좌우는 얇게, 상하는 두껍게 */
.laser-line.laser-top,
.laser-line.laser-bottom {
  stroke-width: 7;
}

.laser-line.laser-left,
.laser-line.laser-right {
  stroke-width: 4;
}

@keyframes laser-scan {
  0% { stroke-dashoffset: 400; opacity: 1; }
  85% { stroke-dashoffset: 0; opacity: 1; }
  100% { stroke-dashoffset: 0; opacity: 0; }
}

.has-bomb .animated-nickname {
  animation: laser-nick-glow 3s infinite ease-in !important;
}
@keyframes laser-nick-glow {
  0% { box-shadow: 0 0 5px rgba(0, 255, 150, 0.2); }
  50% { box-shadow: 0 0 12px rgba(0, 255, 150, 0.6); }
  100% { box-shadow: 0 0 5px rgba(0, 255, 150, 0); }
}
</style>
