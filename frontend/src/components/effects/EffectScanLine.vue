<template>
  <div class="scanline-effect">
    <div class="scanline-bar"></div>
  </div>
</template>

<script setup>
</script>

<style>
.scanline-effect {
  position: absolute;
  inset: -10px;
  pointer-events: none;
  z-index: 6;
  overflow: hidden;
}

.scanline-bar {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(180deg, transparent, rgba(0, 200, 255, 0.5), transparent);
  animation: scanline-move 3s linear infinite;
  box-shadow: 0 0 8px rgba(0, 200, 255, 0.6);
}

@keyframes scanline-move {
  0% {
    top: -3px;
  }
  100% {
    top: 100%;
  }
}

.has-scanline .animated-nickname {
  filter: brightness(1.1) contrast(1.2);
}
</style>
