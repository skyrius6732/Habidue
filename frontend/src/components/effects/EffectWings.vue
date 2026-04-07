<template>
  <template v-if="direction === 'left'">
    <!-- 날개 왼쪽 -->
    <div class="wings-container left">
      <div v-for="n in wingLayers" :key="`wing-l-${n}`" class="wing-layer" :style="{ '--layer': n, '--total': wingLayers }">
        <svg class="pioneer-wing-svg left" viewBox="0 0 100 100" :style="{ filter: `drop-shadow(0 0 ${3 + n}px ${wingColors.glow})` }">
          <path d="M90,50 Q70,10 20,40 Q40,50 90,50 Z" :fill="`url(#wing-grad-${instanceId}-${n})`" />
          <path d="M85,55 Q65,25 25,50 Q45,60 85,55 Z" :fill="`url(#wing-grad-${instanceId}-${n})`" opacity="0.6" />
          <defs>
            <linearGradient :id="`wing-grad-${instanceId}-${n}`" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" :style="{ 'stop-color': wingColors.s1 }" />
              <stop offset="50%" :style="{ 'stop-color': wingColors.s2 }" />
              <stop offset="100%" :style="{ 'stop-color': wingColors.s3 }" />
            </linearGradient>
          </defs>
        </svg>
      </div>
    </div>
  </template>

  <template v-else-if="direction === 'right'">
    <!-- 날개 오른쪽 -->
    <div class="wings-container right">
      <div v-for="n in wingLayers" :key="`wing-r-${n}`" class="wing-layer" :style="{ '--layer': n, '--total': wingLayers }">
        <svg class="pioneer-wing-svg right" viewBox="0 0 100 100" :style="{ filter: `drop-shadow(0 0 ${3 + n}px ${wingColors.glow})` }">
          <path d="M10,50 Q30,10 80,40 Q60,50 10,50 Z" :fill="`url(#wing-grad-rev-${instanceId}-${n})`" />
          <path d="M15,55 Q35,25 75,50 Q55,60 15,55 Z" :fill="`url(#wing-grad-rev-${instanceId}-${n})`" opacity="0.6" />
          <defs>
            <linearGradient :id="`wing-grad-rev-${instanceId}-${n}`" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" :style="{ 'stop-color': wingColors.s1 }" />
              <stop offset="50%" :style="{ 'stop-color': wingColors.s2 }" />
              <stop offset="100%" :style="{ 'stop-color': wingColors.s3 }" />
            </linearGradient>
          </defs>
        </svg>
      </div>
    </div>
  </template>
</template>

<script setup>
defineProps({
  wingLayers: { type: Number, required: true },
  wingColors: { type: Object, required: true },
  instanceId: { type: String, required: true },
  direction: { type: String, default: 'left', validator: (val) => ['left', 'right'].includes(val) }
})
</script>

<style scoped>
/* 1. Wings System */
.wings-container { display: flex; align-items: center; justify-content: center; position: relative; width: 40px; height: 32px; pointer-events: none; z-index: 1; }
.wing-layer {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * 12deg));
  opacity: calc(1 - (var(--layer) - 1) * 0.2);
  animation: wing-flap-multi 3s infinite ease-in-out;
  animation-delay: calc(var(--layer) * -0.4s);
}
.wings-container.right .wing-layer { transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * -12deg)); animation-name: wing-flap-multi-rev; }
.pioneer-wing-svg { width: 100%; height: 100%; filter: drop-shadow(0 0 5px rgba(255,255,255,0.4)); }

@keyframes wing-flap-multi {
  0%, 100% { transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * 12deg)) translateY(0); }
  50% { transform: scale(calc(1.1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * 12deg + 15deg)) translateY(-4px); }
}
@keyframes wing-flap-multi-rev {
  0%, 100% { transform: scale(calc(1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * -12deg)) translateY(0); }
  50% { transform: scale(calc(1.1 + (var(--layer) - 1) * 0.15)) rotate(calc((var(--layer) - 1) * -12deg - 15deg)) translateY(-4px); }
}
</style>
