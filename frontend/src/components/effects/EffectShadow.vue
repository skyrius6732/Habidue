<template>
  <div class="shadow-container">
    <div v-for="n in 6" :key="`smoke-${n}`" class="smoke-blob"
      :style="{
        '--left': `${5 + (n - 1) * 16}%`,
        '--delay': `${((n * 0.5) % 2.5).toFixed(2)}s`,
        '--dur': `${(1.8 + (n % 3) * 0.6).toFixed(1)}s`,
        '--size': `${14 + (n % 4) * 6}px`,
        '--hue': `${260 + (n % 4) * 12}`
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 9. SHADOW_DEMON: 보라 연기 blob */
.shadow-container { position: absolute; inset: -10px -8px; pointer-events: none; z-index: 1; overflow: visible; }
.smoke-blob {
  position: absolute; bottom: 0; left: var(--left);
  width: var(--size); height: var(--size);
  border-radius: 50%;
  background: radial-gradient(circle,
    hsla(var(--hue), 65%, 25%, 0.88) 0%,
    hsla(var(--hue), 75%, 12%, 0.5) 50%,
    transparent 78%
  );
  filter: blur(4px);
  animation: smoke-rise var(--dur) infinite var(--delay) ease-out;
  opacity: 0;
}
@keyframes smoke-rise {
  0%   { opacity: 0; transform: translateY(0) scale(0.45); }
  20%  { opacity: 0.88; transform: translateY(-10px) scale(0.85) translateX(4px); }
  60%  { opacity: 0.55; transform: translateY(-26px) scale(1.4) translateX(-5px); }
  100% { opacity: 0; transform: translateY(-40px) scale(1.7) translateX(2px); }
}
.has-shadow .animated-nickname { border-color: #7c3aed !important; animation: shadow-pulse 1.4s ease-in-out infinite alternate !important; }
@keyframes shadow-pulse {
  from { box-shadow: 0 0 6px rgba(124,58,237,0.45); }
  to   { box-shadow: 0 0 14px rgba(124,58,237,0.75), 0 0 28px rgba(76,29,149,0.4), 0 0 0 2px rgba(109,40,217,0.3); }
}
</style>
