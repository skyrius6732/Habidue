<template>
  <div class="plane-container">
    <svg class="trail" viewBox="0 0 100 100" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M 0,58 Q 50,24 100,52" fill="none"
        stroke="rgba(148,163,184,0.3)" stroke-width="1.5"
        stroke-dasharray="3,5" stroke-linecap="round"/>
    </svg>
    <div class="plane">
      <svg width="15" height="11" viewBox="0 0 15 11" xmlns="http://www.w3.org/2000/svg" style="transform: scaleX(-1)">
        <polygon points="0,5.5 15,0 10.5,5.5 15,11" fill="#e2e8f0" stroke="#94a3b8" stroke-width="0.4"/>
        <polygon points="0,5.5 15,0 6.5,5.5" fill="#f8fafc"/>
        <polygon points="10.5,5.5 15,11 6.5,9" fill="#cbd5e1"/>
      </svg>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.plane-container { position: absolute; inset: -12px -6px; pointer-events: none; z-index: 5; overflow: hidden; }
.trail { position: absolute; inset: 0; width: 100%; height: 100%; }
.plane {
  position: absolute;
  filter: drop-shadow(0 0 3px rgba(148,163,184,0.7));
  animation: plane-fly 3.6s ease-in-out infinite;
}
@keyframes plane-fly {
  0%   { left: -18px; top: calc(58% - 5px); opacity: 0; transform: rotate(-10deg); }
  7%   { opacity: 1; }
  50%  { left: 50%; top: calc(24% - 5px); transform: rotate(-14deg); }
  88%  { left: calc(100% + 8px); top: calc(52% - 5px); transform: rotate(-8deg); opacity: 1; }
  95%  { opacity: 0; }
  100% { left: calc(100% + 8px); opacity: 0; }
}
</style>
