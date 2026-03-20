<script setup>
defineProps({
  icon: String,      // 이모지 (🏠, 📅 등)
  title: String,     // 메뉴 명 (공고 리스트 등)
  statsText: String, // 통계 라벨 (전체 공고 등)
  statsValue: [String, Number], // 통계 수치
  bio: String        // 설명글
})
</script>

<template>
  <div class="header-wrapper">
    <div class="profile-header">
      <div class="profile-pic-large">{{ icon }}</div>
      <div class="profile-info">
        <h2 class="username">{{ title }}</h2>
        <div class="stats">
          <span>{{ statsText }} <strong>{{ statsValue }}</strong></span>
        </div>
        <div class="bio-container">
          <p class="bio">{{ bio }}</p>
        </div>
      </div>
    </div>
    <div class="tab-divider"></div>
  </div>
</template>

<style scoped>
.header-wrapper { width: 100%; }

/* --- PC 표준 규격 --- */
.profile-header {
  display: flex;
  gap: 80px;
  align-items: center;
  margin-bottom: 44px;
  padding: 30px 20px;
}

.profile-pic-large {
  width: 150px !important;
  height: 150px !important;
  min-width: 150px;
  background-color: var(--profile-icon-bg);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 4rem;
  color: var(--text-primary);
}

.profile-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 15px; /* PC 간격 */
}

.username {
  font-size: 1.75rem;
  font-weight: 300;
  margin: 0;
  color: var(--text-primary);
}

.stats {
  margin: 0;
  font-size: 1rem;
  color: var(--text-primary);
}

.bio {
  font-weight: 600;
  font-size: 0.95rem;
  margin: 0;
  line-height: 1.5;
  color: var(--text-primary);
}

.tab-divider {
  border-top: 1px solid var(--divider-color);
  margin-bottom: 0;
  width: 100%;
}

/* --- 모바일 표준 규격 (관심 공고 기준) --- */
@media (max-width: 768px) {
  .profile-header {
    gap: 20px;
    flex-direction: column;
    text-align: center;
    margin-bottom: 20px;
    min-height: 180px;
    justify-content: flex-start;
    padding-top: 10px;
  }

  .profile-pic-large {
    width: 70px !important;
    height: 70px !important;
    min-width: 70px;
    font-size: 2rem;
    margin-top: 5px;
  }

  .profile-info {
    gap: 5px; /* 모바일은 관심 공고의 좁은 간격(5px)을 기준으로 통일 */
    min-height: 85px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .username {
    font-size: 1.3rem;
    height: 24px;
    line-height: 1.3;
  }

  .stats {
    font-size: 0.85rem;
    height: 18px;
  }

  .bio {
    font-size: 0.85rem;
    height: 38px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .tab-divider {
    margin-bottom: 0;
  }
}
</style>
