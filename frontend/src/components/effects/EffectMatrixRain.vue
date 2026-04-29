<template>
  <div class="matrix-container">
    <div v-for="n in 9" :key="`col-${n}`" class="matrix-col"
      :style="{
        '--left': `${(n - 1) * 11}%`,
        '--delay': `${((n * 0.29) % 2.2).toFixed(2)}s`,
        '--dur': `${(1.4 + (n % 4) * 0.25).toFixed(1)}s`
      }"
    >
      <span
        v-for="c in 5" :key="`c-${n}-${c}`"
        class="matrix-char"
        :class="{ 'matrix-head': c === 1 }"
      >{{ chars[(n * 3 + c * 7) % chars.length] }}</span>
    </div>
  </div>
</template>

<script setup>
const chars = ['0','1','ア','ウ','ス','カ','ナ','マ','タ','キ','ク','テ','ソ','0','1','ラ','リ']
</script>

<style scoped>
.matrix-container { position: absolute; inset: -18px -4px; pointer-events: none; z-index: 5; overflow: hidden; }
.matrix-col {
  position: absolute; left: var(--left); top: -24px;
  display: flex; flex-direction: column; gap: 1px;
  animation: matrix-drop var(--dur) infinite var(--delay) linear;
  opacity: 0;
}
.matrix-char {
  font-family: monospace; font-size: 7px; font-weight: 900;
  color: #00c136; text-shadow: 0 0 4px #00ff41; line-height: 1.1; display: block;
}
.matrix-char.matrix-head {
  color: #ffffff;
  text-shadow: 0 0 8px #00ff41, 0 0 16px rgba(0,255,65,0.8);
}
@keyframes matrix-drop {
  0%   { opacity: 0; transform: translateY(-100%); }
  5%   { opacity: 1; }
  80%  { opacity: 0.85; }
  100% { opacity: 0; transform: translateY(88px); }
}
</style>
