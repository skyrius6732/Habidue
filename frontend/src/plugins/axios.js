import axios from 'axios'
import router from '@/router'

const clearAuthStorage = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  localStorage.removeItem('userRole')
}

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
      // [시니어 조치] 비표준 문자(한글 등)가 포함된 경우 헤더 주입 시 브라우저 에러 발생 방지
      // 이 경우 헤더를 주입하지 않고 내보내어 서버에서 401을 받고 재발급 로직을 타게 함
      const isAsciiOnly = /^[\x00-\x7F]*$/.test(token)
      if (isAsciiOnly) {
        config.headers['Authorization'] = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// [시니어 조치] 토큰 재발급 중 발생하는 요청들을 대기시키기 위한 변수들
let isRefreshing = false
let refreshSubscribers = []

const subscribeTokenRefresh = (cb) => {
  refreshSubscribers.push(cb)
}

const onRefreshed = (token) => {
  refreshSubscribers.map((cb) => cb(token))
  refreshSubscribers = []
}

// 응답 인터셉터: 401 에러(만료) 발생 시 자동 갱신 로직 및 차단 유저 처리
instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (!originalRequest) return Promise.reject(error)

    const errorMsg = error.response?.data?.message || ''

    // 1. 차단된 유저 처리 (기존 로직 유지)
    if (errorMsg.startsWith('USER_BLOCKED:')) {
      const displayMsg = errorMsg.replace('USER_BLOCKED:', '')
      clearAuthStorage()
      router.push({ name: 'blocked', query: { reason: displayMsg } })
      return Promise.reject(error)
    }

    // 2. 401 에러(토큰 만료) 처리
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        clearAuthStorage()
        if (router.currentRoute.value.name !== 'home') router.push({ name: 'home' })
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // 이미 재발급 중이라면 큐에 담고 대기
        return new Promise((resolve) => {
          subscribeTokenRefresh((token) => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`
            resolve(instance(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post('/api/auth/reissue', null, {
          headers: { 'Authorization-Refresh': refreshToken }
        })

        const newAccessToken = res.headers['authorization']?.replace('Bearer ', '')
        if (newAccessToken) {
          localStorage.setItem('accessToken', newAccessToken)
          
          // Pinia 스토어 동기화
          try {
            const { useAuthStore } = await import('@/stores/auth')
            useAuthStore().syncTokenFromStorage()
          } catch (e) {}

          isRefreshing = false
          onRefreshed(newAccessToken) // 대기 중인 요청들 해소

          originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`
          return instance(originalRequest)
        }
      } catch (reissueError) {
        isRefreshing = false
        clearAuthStorage()
        if (router.currentRoute.value.name !== 'home') router.push({ name: 'home' })
        return Promise.reject(reissueError)
      }
    }
    
    return Promise.reject(error)
  }
)

export default instance
