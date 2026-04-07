<template>
  <div class="swords-container">
    <!-- 왼쪽 칼 -->
    <svg class="sword left-sword" viewBox="0 0 40 120">
      <!-- 칼날 -->
      <path d="M15,10 L20,0 L25,10 L22,100 L18,100 Z" fill="#c0c0c0" stroke="#808080" stroke-width="0.5" opacity="0.95"/>
      <!-- 칼날 highlight -->
      <path d="M19,15 L20,5 L21,15 L20.5,80 Z" fill="#ffffff" opacity="0.5"/>
      <!-- 호스 (칼의 고리 부분) -->
      <circle cx="20" cy="105" r="4" fill="#d4af37" stroke="#b8860b" stroke-width="0.5"/>
      <!-- 손잡이 -->
      <rect x="16" y="107" width="8" height="10" rx="2" fill="#8b4513" stroke="#654321" stroke-width="0.5"/>
      <!-- 손잡이 감싸개 -->
      <line x1="17" y1="110" x2="23" y2="110" stroke="#d4af37" stroke-width="1"/>
      <line x1="17" y1="113" x2="23" y2="113" stroke="#d4af37" stroke-width="1"/>
    </svg>

    <!-- 오른쪽 칼 -->
    <svg class="sword right-sword" viewBox="0 0 40 120">
      <!-- 칼날 -->
      <path d="M15,10 L20,0 L25,10 L22,100 L18,100 Z" fill="#c0c0c0" stroke="#808080" stroke-width="0.5" opacity="0.95"/>
      <!-- 칼날 highlight -->
      <path d="M19,15 L20,5 L21,15 L20.5,80 Z" fill="#ffffff" opacity="0.5"/>
      <!-- 호스 -->
      <circle cx="20" cy="105" r="4" fill="#d4af37" stroke="#b8860b" stroke-width="0.5"/>
      <!-- 손잡이 -->
      <rect x="16" y="107" width="8" height="10" rx="2" fill="#8b4513" stroke="#654321" stroke-width="0.5"/>
      <!-- 손잡이 감싸개 -->
      <line x1="17" y1="110" x2="23" y2="110" stroke="#d4af37" stroke-width="1"/>
      <line x1="17" y1="113" x2="23" y2="113" stroke="#d4af37" stroke-width="1"/>
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* Swords Cross */
.swords-container {
  position: absolute;
  inset: -40px;
  pointer-events: none;
  z-index: 1;
}

.sword {
  position: absolute;
  width: 26px;
  height: 75px;
  filter: drop-shadow(0 2px 3px rgba(192, 192, 192, 0.5));
  opacity: 0.9;
}

.left-sword {
  top: 50%;
  left: 50%;
  transform: translateX(-50%) translateY(-50%) rotate(-45deg);
  animation: sword-glow 1.8s infinite;
}

.right-sword {
  top: 50%;
  right: 50%;
  transform: translateX(50%) translateY(-50%) rotate(45deg);
  animation: sword-glow 1.8s infinite 0.3s;
}

@keyframes sword-glow {
  0%, 100% {
    filter: drop-shadow(0 2px 3px rgba(192, 192, 192, 0.5));
  }
  50% {
    filter: drop-shadow(0 0 6px rgba(212, 175, 55, 0.8)) drop-shadow(0 2px 3px rgba(192, 192, 192, 0.7));
  }
}
</style>
