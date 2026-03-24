import axios from 'axios'
import router from '@/router'

// [시니어 조치] 하드코딩된 주소를 제거하고 Vite Proxy(/api)를 활용하도록 설정
const instance = axios.create({
  baseURL: '', // Vite 프록시 사용을 위해 비워둠 (상대 경로 사용)
  timeout: 5000, // 타임아웃을 5초로 단축하여 뺑뺑이 조기 차단
  headers: { 'Content-Type': 'application/json' }
})

// 요청 인터셉터: 모든 요청에 Access Token 주입
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 응답 인터셉터: 401 에러(만료) 발생 시 자동 갱신 로직 및 차단 유저 처리
instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    
    // 요청 설정이 없거나 이미 타임아웃된 경우 즉시 종료
    if (!originalRequest) return Promise.reject(error)

    const errorMsg = error.response?.data?.message || ''

    // 1. 차단된 유저인 경우 (즉시 로그아웃 및 차단 페이지 이동)
    if (errorMsg.startsWith('USER_BLOCKED:')) {
      const displayMsg = errorMsg.replace('USER_BLOCKED:', '')
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userRole')
      router.push({ name: 'blocked', query: { reason: displayMsg } })
      return Promise.reject(error)
    }

    // 2. 401 에러(토큰 만료) 처리 - 무한 루프 방지를 위해 _retry 플래그 체크
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')

      if (refreshToken) {
        try {
          // [중요] 재발급 시에는 인터셉터가 걸리지 않은 기본 axios 인스턴스 사용
          // 주소는 프록시(/api)를 거치도록 설정
          const res = await axios.post('/api/auth/reissue', null, {
            headers: { 'Authorization-Refresh': refreshToken }
          })

          const newAccessToken = res.headers['authorization']
          if (newAccessToken) {
            const tokenValue = newAccessToken.replace('Bearer ', '')
            localStorage.setItem('accessToken', tokenValue)
            
            // [시니어 조치] Pinia 스토어 상태도 즉시 동기화 (SSE 등에서 활용)
            try {
              const { useAuthStore } = await import('@/stores/auth')
              const authStore = useAuthStore()
              authStore.syncTokenFromStorage()
            } catch (e) {}

            originalRequest.headers['Authorization'] = `Bearer ${tokenValue}`
            
            // 원래 요청 재시도
            return instance(originalRequest)
          }
        } catch (reissueError) {
          // 재발급 실패 시 (리프레시 토큰 만료 등) 깨끗하게 비우고 로그인으로 이동
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('userRole')
          
          // 현재 페이지가 홈이 아닐 때만 리다이렉트 (무한 루프 방지)
          if (router.currentRoute.value.name !== 'home') {
            router.push({ name: 'home' })
          }
          return Promise.reject(reissueError)
        }
      } else {
        // 리프레시 토큰조차 없으면 로그아웃
        localStorage.removeItem('accessToken')
        if (router.currentRoute.value.name !== 'home') {
          router.push({ name: 'home' })
        }
      }
    }
    
    return Promise.reject(error)
  }
)

export default instance
