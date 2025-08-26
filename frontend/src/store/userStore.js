import { create } from 'zustand';
import { authService } from '../services/authService';

// 토큰 만료 시간을 확인하는 유틸리티 함수
const isTokenExpired = (token) => {
  if (!token) return true;
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const currentTime = Date.now() / 1000;
    return payload.exp < currentTime;
  } catch (error) {
    return true; // 토큰 파싱 실패 시 만료로 간주
  }
};

const useUserStore = create((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  loading: false,
  error: null,

  login: async (username, password) => {
    set({ loading: true, error: null });
    try {
      const response = await authService.login(username, password);
      const { accessToken, refreshToken, user } = response;

      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));

      set({
        user,
        accessToken,
        refreshToken,
        isAuthenticated: true,
        loading: false,
        error: null,
      });

      return { success: true };
    } catch (error) {
      set({
        loading: false,
        error: error.message,
      });
      return { success: false, error: error.message };
    }
  },

  register: async (username, password, name) => {
    set({ loading: true, error: null });
    try {
      const response = await authService.register(username, password, name);
      const { accessToken, refreshToken, user } = response;

      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));

      set({
        user,
        accessToken,
        refreshToken,
        isAuthenticated: true,
        loading: false,
        error: null,
      });

      return { success: true };
    } catch (error) {
      set({
        loading: false,
        error: error.message,
      });
      return { success: false, error: error.message };
    }
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      error: null,
    });
  },

  initializeAuth: async () => {
    set({ loading: true });
    try {
      const storedAccessToken = localStorage.getItem('accessToken');
      const storedRefreshToken = localStorage.getItem('refreshToken');
      const storedUser = localStorage.getItem('user');

      if (storedAccessToken && storedRefreshToken && storedUser) {
        try {
          // 저장된 사용자 정보 파싱
          const parsedUser = JSON.parse(storedUser);
          
          // 먼저 저장된 토큰으로 인증 상태 설정
          set({
            user: parsedUser,
            accessToken: storedAccessToken,
            refreshToken: storedRefreshToken,
            isAuthenticated: true,
            loading: false,
          });
          
          // 백그라운드에서 프로필 유효성 검증 (토큰 만료 시 자동 갱신)
          try {
            const profile = await authService.getUserProfile();
            // 최신 프로필 정보로 업데이트
            set({ user: profile });
            localStorage.setItem('user', JSON.stringify(profile));
          } catch (profileError) {
            // 프로필 조회 실패 시 토큰이 만료되었을 가능성이 높음
            // API interceptor가 자동으로 토큰 갱신을 처리함
            console.log('Profile validation failed, token may have been refreshed automatically');
          }
          
        } catch (initError) {
          console.error('Failed to initialize with stored tokens:', initError);
          get().logout();
          set({ loading: false });
        }
      } else {
        set({ loading: false });
      }
    } catch (error) {
      console.error('Failed to initialize auth:', error);
      get().logout();
      set({ loading: false });
    }
  },

  updateTokens: (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
      set({ accessToken, refreshToken });
    } else {
      set({ accessToken });
    }
  },

  // 토큰 유효성 검사 및 자동 갱신
  validateAndRefreshToken: async () => {
    const { refreshToken, accessToken } = get();
    
    if (!refreshToken) {
      get().logout();
      return false;
    }

    try {
      // 현재 액세스 토큰으로 프로필 조회 시도
      await authService.getUserProfile();
      return true; // 토큰이 유효함
    } catch (error) {
      // 401 에러인 경우 토큰 갱신 시도
      if (error.response?.status === 401) {
        try {
          const refreshResponse = await authService.refreshToken(refreshToken);
          const { accessToken: newAccessToken, refreshToken: newRefreshToken } = refreshResponse;
          
          get().updateTokens(newAccessToken, newRefreshToken || refreshToken);
          return true; // 토큰 갱신 성공
        } catch (refreshError) {
          console.error('Token refresh failed:', refreshError);
          get().logout();
          return false;
        }
      }
      return false;
    }
  },

  updateUser: async (userData) => {
    try {
      const updatedUser = await authService.updateProfile(userData);
      localStorage.setItem('user', JSON.stringify(updatedUser));
      set({ user: updatedUser });
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  },

  clearError: () => {
    set({ error: null });
  },
}));

export default useUserStore;