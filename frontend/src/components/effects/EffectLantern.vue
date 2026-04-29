<template>
  <div class="lantern-container">
    <div v-for="n in 4" :key="`lan-${n}`" class="lantern"
      :style="{
        '--left': `${8 + (n-1)*22}%`,
        '--delay': `${((n*0.65)%2.5).toFixed(2)}s`,
        '--dur': `${(2.4+(n%3)*0.45).toFixed(1)}s`,
        '--sway': `${-6+(n%4)*4}px`
      }"
    >
      <svg width="10" height="15" viewBox="0 0 10 15" xmlns="http://www.w3.org/2000/svg">
        <line x1="5" y1="0" x2="5" y2="2" stroke="#991b1b" stroke-width="0.8"/>
        <rect x="1.5" y="1.5" width="7" height="2" rx="0.8" fill="#991b1b"/>
        <rect x="1" y="3" width="8" height="9" rx="2" fill="#dc2626" stroke="#991b1b" stroke-width="0.4"/>
        <rect x="2" y="3.5" width="6" height="8" rx="1.5" fill="#fca5a5" opacity="0.45"/>
        <line x1="1" y1="7" x2="9" y2="7" stroke="#991b1b" stroke-width="0.4" opacity="0.5"/>
        <rect x="1.5" y="11.5" width="7" height="1.8" rx="0.8" fill="#991b1b"/>
        <line x1="5" y1="13.3" x2="5" y2="15" stroke="#fca5a5" stroke-width="0.8"/>
      </svg>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.lantern-container { position: absolute; inset: -14px -6px; pointer-events: none; z-index: 5; overflow: hidden; }
.lantern {
  position: absolute; bottom: 2px; left: var(--left);
  filter: drop-shadow(0 0 4px rgba(239,68,68,0.75)) drop-shadow(0 0 8px rgba(251,191,36,0.35));
  animation: lantern-rise var(--dur) infinite var(--delay) ease-in-out;
  opacity: 0;
}
@keyframes lantern-rise {
  0%   { opacity: 0; transform: translateY(0) translateX(0) rotate(0deg); }
  12%  { opacity: 0.9; }
  50%  { transform: translateY(-22px) translateX(var(--sway)) rotate(4deg); opacity: 0.85; }
  80%  { opacity: 0.4; }
  100% { opacity: 0; transform: translateY(-50px) translateX(calc(var(--sway)*1.5)) rotate(-3deg); }
}
</style>
