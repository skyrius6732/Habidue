<template>
  <div class="redirect-page">
    <h2>로그인 처리 중...</h2>
    <p v-if="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useUiStore } from '@/stores/ui';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const uiStore = useUiStore();
const error = ref(null);

onMounted(async () => {
  const accessToken = route.query.accessToken;
  const refreshToken = route.query.refreshToken;
  const withdrawalWarning = route.query.withdrawalWarning;

  if (accessToken && refreshToken) {
    try {
      authStore.setTokens(accessToken, refreshToken);
      // 로그인 성공 직후 사용자 정보를 가져와 Role 등을 로컬에 저장
      await authStore.fetchUserProfile();

      // [시니어 조치] 7일 이내 탈퇴한 사용자 로그인 시 안내 메시지 표시
      if (withdrawalWarning) {
        await uiStore.showAlert(withdrawalWarning, '⚠️ 탈퇴 계정 안내');
      }

      router.push('/');
    } catch (e) {
      error.value = '사용자 정보를 불러오는 중 오류가 발생했습니다.';
      setTimeout(() => router.push('/login-error'), 2000);
    }
  } else {
    error.value = '로그인 토큰을 찾을 수 없습니다.';
    router.push('/login-error');
  }
});
</script>

<style scoped>
.redirect-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  text-align: center;
}
</style>
