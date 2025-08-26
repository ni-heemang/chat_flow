import { useEffect, useRef } from 'react';
import useUserStore from '../store/userStore';

// 토큰 자동 갱신 훅
const useTokenRefresh = () => {
  const { isAuthenticated, validateAndRefreshToken } = useUserStore();
  const refreshTimeoutRef = useRef(null);

  useEffect(() => {
    if (!isAuthenticated) {
      // 인증되지 않은 경우 타이머 정리
      if (refreshTimeoutRef.current) {
        clearTimeout(refreshTimeoutRef.current);
        refreshTimeoutRef.current = null;
      }
      return;
    }

    const scheduleTokenRefresh = () => {
      // 5분마다 토큰 유효성 검사 및 갱신
      refreshTimeoutRef.current = setTimeout(async () => {
        try {
          await validateAndRefreshToken();
        } catch (error) {
          console.error('Scheduled token refresh failed:', error);
        } finally {
          // 다음 검사 스케줄링
          scheduleTokenRefresh();
        }
      }, 5 * 60 * 1000); // 5분
    };

    // 초기 스케줄링
    scheduleTokenRefresh();

    // 페이지 가시성 변경 시 즉시 토큰 검사
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        validateAndRefreshToken().catch(console.error);
      }
    };

    // 페이지 포커스 시 토큰 검사
    const handleFocus = () => {
      validateAndRefreshToken().catch(console.error);
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('focus', handleFocus);

    return () => {
      if (refreshTimeoutRef.current) {
        clearTimeout(refreshTimeoutRef.current);
      }
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('focus', handleFocus);
    };
  }, [isAuthenticated, validateAndRefreshToken]);

  return null;
};

export default useTokenRefresh;