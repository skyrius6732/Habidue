<template>
  <div class="flower-crown-container">
    <!-- 꽃 머리띠 -->
    <svg class="flower-crown" viewBox="0 0 100 30">
      <!-- 녹색 줄기 (머리띠) -->
      <ellipse cx="50" cy="18" rx="48" ry="6" fill="#4a7c59" opacity="0.7"/>
      <path d="M4,18 Q50,12 96,18" stroke="#2d5a3d" stroke-width="1.5" fill="none" opacity="0.6"/>

      <!-- 왼쪽 꽃들 -->
      <!-- 꽃1 (빨간색) -->
      <circle cx="15" cy="10" r="5" fill="#ff1493" opacity="0.9"/>
      <circle cx="12" cy="9" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="18" cy="9" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="15" cy="6" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="15" cy="12" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="15" cy="10" r="2" fill="#fff700" opacity="0.9"/>

      <!-- 꽃2 (분홍색) -->
      <circle cx="30" cy="8" r="4.5" fill="#ff69b4" opacity="0.85"/>
      <circle cx="27" cy="7" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="33" cy="7" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="30" cy="5" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="30" cy="10" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="30" cy="8" r="1.5" fill="#fff700" opacity="0.85"/>

      <!-- 꽃3 (주황색) -->
      <circle cx="45" cy="6" r="5" fill="#ffa500" opacity="0.9"/>
      <circle cx="42" cy="5" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="48" cy="5" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="45" cy="2" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="45" cy="8" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="45" cy="6" r="2" fill="#fff700" opacity="0.9"/>

      <!-- 중앙 꽃 (노란색 큼) -->
      <circle cx="50" cy="9" r="5.5" fill="#ffd700" opacity="0.95"/>
      <circle cx="46" cy="7.5" r="3.5" fill="#ffed4e" opacity="0.85"/>
      <circle cx="54" cy="7.5" r="3.5" fill="#ffed4e" opacity="0.85"/>
      <circle cx="50" cy="4" r="3.5" fill="#ffed4e" opacity="0.85"/>
      <circle cx="50" cy="11" r="3.5" fill="#ffed4e" opacity="0.85"/>
      <circle cx="50" cy="9" r="2.5" fill="#fff700" opacity="0.95"/>

      <!-- 오른쪽 꽃들 -->
      <!-- 꽃4 (주황색) -->
      <circle cx="55" cy="6" r="5" fill="#ffa500" opacity="0.9"/>
      <circle cx="52" cy="5" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="58" cy="5" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="55" cy="2" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="55" cy="8" r="3" fill="#ffb84d" opacity="0.8"/>
      <circle cx="55" cy="6" r="2" fill="#fff700" opacity="0.9"/>

      <!-- 꽃5 (분홍색) -->
      <circle cx="70" cy="8" r="4.5" fill="#ff69b4" opacity="0.85"/>
      <circle cx="67" cy="7" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="73" cy="7" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="70" cy="5" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="70" cy="10" r="2.5" fill="#ffc0cb" opacity="0.8"/>
      <circle cx="70" cy="8" r="1.5" fill="#fff700" opacity="0.85"/>

      <!-- 꽃6 (빨간색) -->
      <circle cx="85" cy="10" r="5" fill="#ff1493" opacity="0.9"/>
      <circle cx="82" cy="9" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="88" cy="9" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="85" cy="6" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="85" cy="12" r="3" fill="#ff69b4" opacity="0.8"/>
      <circle cx="85" cy="10" r="2" fill="#fff700" opacity="0.9"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Flower Crown */
.flower-crown-container {
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  width: 45px;
  height: 15px;
  pointer-events: none;
  z-index: 8;
}

.flower-crown {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 2px 4px rgba(255, 215, 0, 0.4));
  animation: crown-shimmer 2.5s ease-in-out infinite;
}

@keyframes crown-shimmer {
  0%, 100% {
    filter: drop-shadow(0 2px 4px rgba(255, 215, 0, 0.4));
  }
  50% {
    filter: drop-shadow(0 2px 8px rgba(255, 215, 0, 0.7)) drop-shadow(0 0 4px rgba(255, 192, 203, 0.5));
  }
}
</style>
