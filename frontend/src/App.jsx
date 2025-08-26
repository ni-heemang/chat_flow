import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import { useMemo, useEffect, useState } from 'react';
import createAppTheme from './theme';
import useThemeStore from './store/themeStore';
import AuthGuard from './components/auth/AuthGuard';
import Layout from './components/common/Layout';
import ChatPage from './pages/ChatPage';
import RoomListPage from './pages/RoomListPage';
import AnalyticsPage from './pages/AnalyticsPage';
import StatsPage from './pages/StatsPage';
import SettingsPage from './pages/SettingsPage';
import ErrorBoundary from './components/common/ErrorBoundary';
import useTokenRefresh from './hooks/useTokenRefresh';

function App() {
  const { getEffectiveMode, mode } = useThemeStore();
  const [themeUpdateTrigger, setThemeUpdateTrigger] = useState(0);
  
  // 토큰 자동 갱신 훅 사용
  useTokenRefresh();
  
  const theme = useMemo(() => {
    return createAppTheme(getEffectiveMode());
  }, [getEffectiveMode, mode, themeUpdateTrigger]);

  useEffect(() => {
    // 테마 변경 이벤트 리스너
    const handleThemeChange = () => {
      setThemeUpdateTrigger(prev => prev + 1);
    };

    // 시스템 테마 변경 감지
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleSystemThemeChange = () => {
      setThemeUpdateTrigger(prev => prev + 1);
    };
    
    // 이벤트 리스너 등록
    window.addEventListener('theme-change', handleThemeChange);
    mediaQuery.addEventListener('change', handleSystemThemeChange);
    
    return () => {
      window.removeEventListener('theme-change', handleThemeChange);
      mediaQuery.removeEventListener('change', handleSystemThemeChange);
    };
  }, []);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ErrorBoundary>
        <Router>
          <AuthGuard>
            {(isAuthenticated) =>
              isAuthenticated ? (
                <Layout>
                  <Routes>
                    <Route path="/" element={<Navigate to="/rooms" replace />} />
                    <Route path="/rooms" element={<RoomListPage />} />
                    <Route path="/chat" element={<ChatPage />} />
                    <Route path="/chat/:roomId" element={<ChatPage />} />
                    <Route path="/analytics" element={<AnalyticsPage />} />
                    <Route path="/stats" element={<StatsPage />} />
                    <Route path="/settings" element={<SettingsPage />} />
                  </Routes>
                </Layout>
              ) : null
            }
          </AuthGuard>
        </Router>
      </ErrorBoundary>
    </ThemeProvider>
  );
}

export default App;
