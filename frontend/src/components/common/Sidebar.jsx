import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Box,
  Typography,
} from '@mui/material';
import {
  Analytics as AnalyticsIcon,
  List as ListIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';

const drawerWidth = 240;

const menuItems = [
  { text: '채팅방 목록', icon: <ListIcon />, path: '/rooms' },
  { text: '분석', icon: <AnalyticsIcon />, path: '/analytics' },
  { text: '설정', icon: <SettingsIcon />, path: '/settings' },
];

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleNavigation = (path) => {
    navigate(path);
  };

  const drawerContent = (
    <Box>
      <Box
        sx={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        <Typography variant="h6" color="primary">
          Menu
        </Typography>
      </Box>
      
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path}
              onClick={() => handleNavigation(item.path)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      
      <Divider />
    </Box>
  );

  return (
    <Drawer
      variant="permanent"
      sx={{
        display: { xs: 'none', sm: 'block' }, // 작은 화면에서는 숨김
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
          marginTop: '64px',
          height: 'calc(100% - 64px)',
        },
      }}
    >
      {drawerContent}
    </Drawer>
  );
};

export default Sidebar;