<template>
  <div class="meteor-container">
    <div v-for="n in 5" :key="`meteor-${n}`" class="meteor"
      :style="{
        '--delay': `${((n - 1) * 0.5).toFixed(2)}s`,
        '--dur': `${(1.3 + (n % 3) * 0.25).toFixed(1)}s`,
        '--left': `${-8 + (n - 1) * 22}%`,
        '--top': `-12%`,
        '--len': `${44 + (n % 3) * 10}px`
      }"
    ></div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.meteor-container {
  position: absolute; inset: -18px -28px;
  pointer-events: none; z-index: 6; overflow: hidden;
}
.meteor {
  position: absolute;
  top: var(--top); left: var(--left);
  width: var(--len); height: 1.5px;
  background: linear-gradient(90deg, transparent 0%, rgba(255,190,70,0.15) 25%, rgba(255,140,0,0.75) 70%, #ffffff 100%);
  border-radius: 1px;
  transform: rotate(32deg);
  transform-origin: left center;
  opacity: 0;
  animation: meteor-fly var(--dur) infinite var(--delay) ease-in;
}
.meteor::after {
  content: '';
  position: absolute; right: -1px; top: -2px;
  width: 5px; height: 5px;
  background: radial-gradient(circle at 35% 35%, #fff 0%, rgba(255,160,30,0.9) 55%, transparent 100%);
  border-radius: 50%;
  box-shadow: 0 0 5px 2px rgba(255,150,0,0.95), 0 0 10px rgba(255,100,0,0.5);
}
@keyframes meteor-fly {
  0%   { opacity: 0; transform: rotate(32deg) translateX(-5%); }
  4%   { opacity: 1; }
  88%  { opacity: 0.85; }
  100% { opacity: 0; transform: rotate(32deg) translateX(340%); }
}
</style>
