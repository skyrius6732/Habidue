import { defineStore } from 'pinia';
import axios from '@/plugins/axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    user: JSON.parse(localStorage.getItem('user')) || null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    isAdmin: (state) => state.user?.role === 'ADMIN' || localStorage.getItem('userRole') === 'ADMIN',
  },
  actions: {
    setTokens(accessToken, refreshToken) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
    },
    async fetchUserProfile() {
      try {
        const res = await axios.get('/api/users/me');
        this.user = res.data.data;
        localStorage.setItem('user', JSON.stringify(this.user));
        localStorage.setItem('userRole', this.user.role);
        return this.user;
      } catch (error) {
        console.error('Failed to fetch user profile:', error);
        this.clearTokens();
        throw error;
      }
    },
    clearTokens() {
      this.accessToken = null;
      this.refreshToken = null;
      this.user = null;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('userRole');
    },
  },
});
