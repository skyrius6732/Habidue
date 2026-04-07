<template>
  <div class="star-tiara-container">
    <!-- 중앙 큰 별 -->
    <div class="star star-center">⭐</div>

    <!-- 양쪽 별들 -->
    <div v-for="i in 4" :key="`star-left-${i}`" class="star star-side-left"
      :style="{ '--star-idx': i - 1, '--delay': `${i * 0.15}s` }">
      ⭐
    </div>
    <div v-for="i in 4" :key="`star-right-${i}`" class="star star-side-right"
      :style="{ '--star-idx': i - 1, '--delay': `${i * 0.15}s` }">
      ⭐
    </div>

    <!-- 반짝임 광선들 -->
    <div v-for="i in 8" :key="`sparkle-${i}`" class="sparkle-ray"
      :style="{ '--ray-angle': `${(i - 1) * 45}deg`, '--ray-delay': `${i * 0.1}s` }">
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Star Tiara */
.star-tiara-container {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  height: 20px;
  pointer-events: none;
  z-index: 8;
}

.star {
  position: absolute;
  font-size: 10px;
  line-height: 1;
  filter: drop-shadow(0 0 2px rgba(255, 215, 0, 0.8));
  animation: star-twinkle 1.5s ease-in-out infinite;
}

.star-center {
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  font-size: 14px;
  animation: star-pulse 2s ease-in-out infinite;
}

.star-side-left {
  top: calc(2.5px + var(--star-idx) * 3px);
  left: calc(50% - 10px - var(--star-idx) * 4px);
  font-size: calc(9px - var(--star-idx) * 0.75px);
  animation: star-side-float 2s ease-in-out infinite var(--delay);
  opacity: 0.8;
}

.star-side-right {
  top: calc(2.5px + var(--star-idx) * 3px);
  right: calc(50% - 10px - var(--star-idx) * 4px);
  font-size: calc(9px - var(--star-idx) * 0.75px);
  animation: star-side-float 2s ease-in-out infinite var(--delay);
  opacity: 0.8;
}

.sparkle-ray {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 15px;
  height: 1px;
  margin-top: -0.5px;
  margin-left: -7.5px;
  background: linear-gradient(90deg, transparent, #ffd700, transparent);
  transform: rotate(var(--ray-angle));
  animation: ray-flash 1s ease-in-out infinite var(--ray-delay);
  opacity: 0;
}

@keyframes star-pulse {
  0%, 100% {
    transform: translateX(-50%) scale(1);
    opacity: 1;
  }
  50% {
    transform: translateX(-50%) scale(1.15);
    opacity: 0.9;
  }
}

@keyframes star-twinkle {
  0%, 100% {
    opacity: 0.8;
  }
  50% {
    opacity: 1;
  }
}

@keyframes star-side-float {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.7;
  }
  50% {
    transform: translateY(-4px) scale(1.1);
    opacity: 1;
  }
}

@keyframes ray-flash {
  0% {
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}
</style>
