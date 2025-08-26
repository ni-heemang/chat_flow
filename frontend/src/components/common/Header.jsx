import { useState } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Avatar,
  Box,
  useTheme,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Button,
} from '@mui/material';
import {
  Chat as ChatIcon,
  Person as PersonIcon,
  Logout as LogoutIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import useUserStore from '../../store/userStore';

const Header = () => {
  const theme = useTheme();
  const { user, logout } = useUserStore();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleUserMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleUserMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logout();
    handleUserMenuClose();
  };

  return (
    <AppBar position="fixed" sx={{ zIndex: theme.zIndex.drawer + 1 }}>
      <Toolbar>
        <ChatIcon sx={{ mr: 1 }} />
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          FlowChat
        </Typography>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {user ? (
            <>
              <IconButton
                color="inherit"
                onClick={handleUserMenuOpen}
                sx={{ p: 0.5 }}
              >
                <Avatar sx={{ width: 32, height: 32 }}>
                  {(user.name || user.username)[0].toUpperCase()}
                </Avatar>
              </IconButton>
              
              <Button
                color="inherit"
                onClick={handleLogout}
                startIcon={<LogoutIcon />}
                sx={{ ml: 1 }}
              >
                로그아웃
              </Button>
              
              <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleUserMenuClose}
                anchorOrigin={{
                  vertical: 'bottom',
                  horizontal: 'right',
                }}
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
              >
                <MenuItem disabled>
                  <ListItemText 
                    primary={user.name || user.username} 
                    secondary={user.username}
                  />
                </MenuItem>
                <MenuItem onClick={handleUserMenuClose}>
                  <ListItemIcon>
                    <SettingsIcon fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="설정" />
                </MenuItem>
              </Menu>
            </>
          ) : (
            <IconButton color="inherit">
              <PersonIcon />
            </IconButton>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Header;