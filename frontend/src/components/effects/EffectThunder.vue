<template>
  <div class="thunder-container">
    <svg v-for="n in 6" :key="`bolt-${n}`" class="lightning-bolt" viewBox="0 0 16 32"
      :style="{
        '--left': `${10 + (n - 1) * 14}%`,
        '--top': `${-5 + (n % 3) * 28}%`,
        '--delay': `${((n * 0.47) % 2.2).toFixed(2)}s`,
        '--dur': `${(1.2 + (n % 3) * 0.6).toFixed(2)}s`,
        '--size': `${13 + (n % 3) * 5}px`
      }"
    >
      <polygon points="10,0 5,13 9,13 6,32 14,11 8,11 12,0"
        :fill="n % 2 === 0 ? '#93c5fd' : '#e0f2fe'"
        stroke="#bfdbfe" stroke-width="0.4"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 5. Thunder Blue (번개) */
.thunder-container { position: absolute; inset: -12px -8px; pointer-events: none; z-index: 5; overflow: visible; }
.lightning-bolt {
  position: absolute; top: var(--top); left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 3px #60a5fa) drop-shadow(0 0 7px rgba(147, 197, 253, 0.9));
  animation: bolt-flash var(--dur) infinite var(--delay) ease-out;
  opacity: 0;
}
@keyframes bolt-flash {
  0%, 100% { opacity: 0; transform: scaleY(0.85) translateY(2px); }
  8% { opacity: 1; transform: scaleY(1.1) translateY(-1px); }
  18% { opacity: 0.6; transform: scaleY(0.95); }
  22% { opacity: 1; }
  30% { opacity: 0; }
}
.has-thunder .animated-nickname { box-shadow: 0 0 6px rgba(96, 165, 250, 0.5) !important; animation: thunder-nick-pulse 0.6s infinite alternate !important; }
@keyframes thunder-nick-pulse { from { box-shadow: 0 0 4px rgba(96, 165, 250, 0.3); } to { box-shadow: 0 0 14px rgba(147, 197, 253, 0.7); } }
</style>
