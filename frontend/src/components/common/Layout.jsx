import {
  Box,
  CssBaseline,
  useTheme,
} from '@mui/material';
import Header from './Header';
import Sidebar from './Sidebar';

const Layout = ({ children }) => {
  const theme = useTheme();

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      
      <Header />
      
      <Sidebar />
      
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          ml: { xs: 0, sm: '240px' }, // 반응형: 작은 화면에서는 마진 없음
          mt: '64px',
          minHeight: 'calc(100vh - 64px)',
          backgroundColor: theme.palette.background.default,
          p: 3,
          overflow: 'auto', // 스크롤 가능하도록
        }}
      >
        <Box sx={{
          maxWidth: '1200px', // 최대 너비 제한
          mx: 'auto', // 중앙 정렬
          width: '100%'
        }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
};

export default Layout;