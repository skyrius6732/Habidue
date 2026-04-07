<template>
  <div class="halo-container">
    <div class="halo-glow"></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Halo - 후광 */
.halo-container {
  position: absolute; inset: -3px; pointer-events: none; z-index: 0;
  display: flex; align-items: center; justify-content: center;
}
.halo-glow {
  position: absolute;
  width: 100%; height: 100%;
  border-radius: 6px;
  background: radial-gradient(ellipse at center, rgba(255, 215, 0, 0.4) 0%, rgba(255, 215, 0, 0.15) 60%, transparent 100%);
  box-shadow:
    0 0 15px rgba(255, 215, 0, 0.6),
    0 0 25px rgba(255, 215, 0, 0.4),
    0 0 40px rgba(255, 200, 0, 0.2),
    inset 0 0 12px rgba(255, 255, 255, 0.15);
  filter: blur(2px);
  animation: halo-pulse 2.5s ease-in-out infinite;
}

@keyframes halo-pulse {
  0%, 100% {
    opacity: 0.3;
    box-shadow:
      0 0 15px rgba(255, 215, 0, 0.4),
      0 0 25px rgba(255, 215, 0, 0.25),
      0 0 40px rgba(255, 200, 0, 0.15),
      inset 0 0 10px rgba(255, 255, 255, 0.1);
  }
  50% {
    opacity: 1;
    box-shadow:
      0 0 25px rgba(255, 215, 0, 0.9),
      0 0 45px rgba(255, 215, 0, 0.65),
      0 0 70px rgba(255, 200, 0, 0.4),
      inset 0 0 20px rgba(255, 255, 255, 0.3);
  }
}
</style>
