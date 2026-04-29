<template>
  <div class="rune-container">
    <div v-for="(rune, i) in runes" :key="`rune-${i}`" class="rune-symbol"
      :style="{
        '--angle': `${i * 60}deg`,
        '--delay': `${(i * 0.45).toFixed(1)}s`,
        '--dur': `${(2.8 + (i % 3) * 0.6).toFixed(1)}s`,
        '--dist': `${32 + (i % 2) * 9}px`,
        '--hue': `${[260, 180, 300, 220, 140, 70][i]}`
      }"
    >{{ rune }}</div>
  </div>
</template>

<script setup>
const runes = ['ᚱ', 'ᚹ', 'ᛟ', 'ᚷ', 'ᛏ', 'ᚢ']
</script>

<style scoped>
.rune-container { position: absolute; inset: -44px; pointer-events: none; z-index: 4; }
.rune-symbol {
  position: absolute; top: 50%; left: 50%;
  font-size: 11px; font-weight: 900; line-height: 1;
  color: hsl(var(--hue), 80%, 68%);
  text-shadow: 0 0 6px hsl(var(--hue), 90%, 72%), 0 0 14px hsl(var(--hue), 70%, 52%);
  margin: -7px 0 0 -5px;
  animation: rune-orbit var(--dur) ease-in-out infinite var(--delay);
  opacity: 0;
}
@keyframes rune-orbit {
  0%   { opacity: 0; transform: rotate(var(--angle)) translateX(var(--dist)) rotate(calc(-1 * var(--angle))) scale(0.4); }
  18%  { opacity: 1; transform: rotate(calc(var(--angle) + 25deg)) translateX(var(--dist)) rotate(calc(-1 * (var(--angle) + 25deg))) scale(1); }
  82%  { opacity: 0.85; transform: rotate(calc(var(--angle) + 335deg)) translateX(var(--dist)) rotate(calc(-1 * (var(--angle) + 335deg))) scale(1); }
  100% { opacity: 0; transform: rotate(calc(var(--angle) + 360deg)) translateX(var(--dist)) rotate(calc(-1 * (var(--angle) + 360deg))) scale(0.4); }
}
</style>
