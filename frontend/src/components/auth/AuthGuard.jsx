import { useEffect, useState } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import useUserStore from '../../store/userStore';
import LoginForm from './LoginForm';

const AuthGuard = ({ children }) => {
  const { isAuthenticated, loading, initializeAuth, validateAndRefreshToken } = useUserStore();
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        // 먼저 저장된 토큰으로 초기화
        await initializeAuth();
        
        // 토큰 유효성 검사 및 필요시 갱신
        if (localStorage.getItem('accessToken') && localStorage.getItem('refreshToken')) {
          await validateAndRefreshToken();
        }
      } catch (error) {
        console.error('Auth check failed:', error);
      } finally {
        setAuthChecked(true);
      }
    };

    checkAuth();
  }, [initializeAuth, validateAndRefreshToken]);

  // 초기 인증 확인이 완료되지 않았거나 로딩 중인 경우
  if (!authChecked || loading) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
          gap: 2,
        }}
      >
        <CircularProgress />
        <Typography variant="body2" color="text.secondary">
          인증 상태를 확인하는 중...
        </Typography>
      </Box>
    );
  }

  if (!isAuthenticated) {
    return <LoginForm />;
  }

  return typeof children === 'function' ? children(isAuthenticated) : children;
};

export default AuthGuard;