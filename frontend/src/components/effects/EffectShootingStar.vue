<template>
  <div class="shooting-star-container">
    <div class="shooting-star"
      :style="{
        '--delay': '0s',
        '--y': '50%'
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 15. SHOOTING_STAR: 별똥별 가로지름 */
.shooting-star-container { position: absolute; inset: -2px; pointer-events: none; z-index: 6; overflow: hidden; }
.shooting-star {
  position: absolute; top: var(--y); left: -15%;
  width: 54px; height: 1.5px;
  background: linear-gradient(90deg, transparent 0%, rgba(255,255,220,0.25) 35%, rgba(255,255,255,0.85) 80%, transparent 100%);
  border-radius: 1px;
  animation: shoot-across 1.0s infinite var(--delay) ease-in;
  opacity: 0;
}
.shooting-star::after {
  content: ''; position: absolute; right: 0; top: -2px;
  width: 5px; height: 5px; background: white; border-radius: 50%;
  box-shadow: 0 0 5px 2px rgba(255,255,200,0.95), 0 0 12px rgba(200,225,255,0.7);
}
@keyframes shoot-across {
  0%   { opacity: 0; transform: translateX(-100%); }
  1.5%   { opacity: 3; }
  92%  { opacity: 1; }
  100% { opacity: 0; transform: translateX(250%); }
}
</style>
