<template>
  <div class="spider-container">
    <!-- 왼쪽 상단 -->
    <svg class="web-corner web-tl" viewBox="0 0 44 44" fill="none">
      <line x1="0" y1="0" x2="36" y2="36" stroke="rgba(210,210,230,0.5)" stroke-width="0.7"/>
      <line x1="0" y1="0" x2="44" y2="18" stroke="rgba(210,210,230,0.5)" stroke-width="0.7"/>
      <line x1="0" y1="0" x2="18" y2="44" stroke="rgba(210,210,230,0.5)" stroke-width="0.7"/>
      <path d="M 11,0 Q 5.5,5.5 0,11" stroke="rgba(210,210,230,0.4)" stroke-width="0.55" fill="none"/>
      <path d="M 22,0 Q 11,11 0,22" stroke="rgba(210,210,230,0.35)" stroke-width="0.55" fill="none"/>
      <path d="M 33,0 Q 16.5,16.5 0,33" stroke="rgba(210,210,230,0.25)" stroke-width="0.5" fill="none"/>
    </svg>
    <!-- 오른쪽 상단 -->
    <svg class="web-corner web-tr" viewBox="0 0 44 44" fill="none">
      <line x1="44" y1="0" x2="8" y2="36" stroke="rgba(210,210,230,0.5)" stroke-width="0.7"/>
      <line x1="44" y1="0" x2="0" y2="18" stroke="rgba(210,210,230,0.5)" stroke-width="0.7"/>
      <line x1="44" y1="0" x2="26" y2="44" stroke="rgba(210,210,230,0.5)" stroke-width="0.7"/>
      <path d="M 33,0 Q 38.5,5.5 44,11" stroke="rgba(210,210,230,0.4)" stroke-width="0.55" fill="none"/>
      <path d="M 22,0 Q 33,11 44,22" stroke="rgba(210,210,230,0.35)" stroke-width="0.55" fill="none"/>
      <path d="M 11,0 Q 27.5,16.5 44,33" stroke="rgba(210,210,230,0.25)" stroke-width="0.5" fill="none"/>
    </svg>
    <!-- 거미 -->
    <div class="spider-body">
      <div class="spider-legs spider-legs-l"></div>
      <div class="spider-dot"></div>
      <div class="spider-legs spider-legs-r"></div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.spider-container { position: absolute; inset: -10px; pointer-events: none; z-index: 6; }
.web-corner { position: absolute; width: 44px; height: 44px; }
.web-tl { top: -10px; left: -10px; animation: web-pulse 3.5s ease-in-out infinite; }
.web-tr { top: -10px; right: -10px; animation: web-pulse 3.5s ease-in-out infinite 0.6s; }
@keyframes web-pulse {
  0%, 100% { opacity: 0.35; }
  50% { opacity: 0.85; }
}

.spider-body {
  position: absolute; top: -6px; left: 50%;
  transform: translateX(-50%);
  display: flex; align-items: center; gap: 1px;
  animation: spider-swing 4s ease-in-out infinite;
}
.spider-dot {
  width: 6px; height: 7px; border-radius: 50% 50% 55% 55%;
  background: #222; border: 0.8px solid rgba(200,200,220,0.7);
  box-shadow: 0 0 3px rgba(0,0,0,0.9);
  position: relative;
}
.spider-dot::before {
  content: ''; position: absolute; top: -3px; left: 50%; transform: translateX(-50%);
  width: 4px; height: 4px; border-radius: 50%;
  background: #2a2a2a; border: 0.8px solid rgba(200,200,220,0.5);
}
.spider-legs-l, .spider-legs-r {
  width: 6px; height: 5px;
  background: transparent;
  border-top: 0.8px solid rgba(180,180,200,0.7);
  border-bottom: 0.8px solid rgba(180,180,200,0.7);
  animation: legs-wiggle 0.4s ease-in-out infinite alternate;
}
.spider-legs-r { animation-delay: 0.2s; }
@keyframes spider-swing {
  0%, 100% { transform: translateX(-50%) rotate(-8deg) translateY(0); }
  25%  { transform: translateX(-60%) rotate(5deg) translateY(3px); }
  75%  { transform: translateX(-40%) rotate(-5deg) translateY(5px); }
}
@keyframes legs-wiggle {
  from { transform: scaleX(1); }
  to   { transform: scaleX(0.6) rotate(5deg); }
}
</style>
