<template>
  <div class="stars-container">
    <svg v-for="n in 6" :key="`star-${n}`" class="twinkle-star" viewBox="0 0 24 24"
      :style="{
        '--top': `${5 + Math.random() * 70}%`,
        '--left': `${3 + Math.random() * 94}%`,
        '--delay': `${Math.random() * 3}s`,
        '--size': `${8 + Math.random() * 8}px`
      }"
    >
      <path fill="#facc15" d="M12,2L15.09,8.26L22,9.27L17,14.14L18.18,21.02L12,17.77L5.82,21.02L7,14.14L2,9.27L8.91,8.26L12,2Z" />
    </svg>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* 3. Starry Night (반짝이는 별) */
.stars-container { position: absolute; inset: -4px; pointer-events: none; z-index: 10; }
.twinkle-star {
  position: absolute; top: var(--top); left: var(--left); width: var(--size); height: var(--size);
  filter: drop-shadow(0 0 5px #facc15);
  animation: star-twinkle 2s infinite ease-in-out var(--delay); opacity: 0;
}
@keyframes star-twinkle {
  0%, 100% { transform: scale(0) rotate(0deg); opacity: 0; }
  50% { transform: scale(1) rotate(180deg); opacity: 1; }
}
</style>
