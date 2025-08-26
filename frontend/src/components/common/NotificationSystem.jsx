import { useState, useEffect } from 'react';
import {
  Snackbar,
  Alert,
  Badge,
  IconButton,
  Drawer,
  Box,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  Button,
  Divider,
  Chip,
  Paper,
} from '@mui/material';
import {
  Notifications as NotificationIcon,
  NotificationsActive as NotificationActiveIcon,
  Close as CloseIcon,
  Message as MessageIcon,
  Analytics as AnalysisIcon,
  Error as ErrorIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  CheckCircle as SuccessIcon,
  Clear as ClearIcon,
  DoneAll as DoneAllIcon,
} from '@mui/icons-material';
import useNotificationStore from '../../store/notificationStore';

const getNotificationIcon = (type) => {
  switch (type) {
    case 'success': return <SuccessIcon color="success" />;
    case 'error': return <ErrorIcon color="error" />;
    case 'warning': return <WarningIcon color="warning" />;
    case 'info': return <InfoIcon color="info" />;
    case 'message': return <MessageIcon color="primary" />;
    case 'analysis': return <AnalysisIcon color="secondary" />;
    default: return <InfoIcon />;
  }
};

const getNotificationColor = (type) => {
  switch (type) {
    case 'success': return 'success';
    case 'error': return 'error'; 
    case 'warning': return 'warning';
    case 'message': return 'primary';
    case 'analysis': return 'secondary';
    default: return 'info';
  }
};

const NotificationToast = () => {
  const { notifications, removeNotification } = useNotificationStore();
  const [currentNotification, setCurrentNotification] = useState(null);

  useEffect(() => {
    // 읽지 않은 최신 알림을 토스트로 표시
    const unreadNotification = notifications.find(n => !n.read && !n.toastShown);
    
    if (unreadNotification && !currentNotification) {
      setCurrentNotification(unreadNotification);
      
      // 자동 닫기 타이머
      const duration = unreadNotification.duration || 4000;
      setTimeout(() => {
        setCurrentNotification(null);
        removeNotification(unreadNotification.id);
      }, duration);

      // 토스트 표시됨을 마킹 (중복 표시 방지)
      unreadNotification.toastShown = true;
    }
  }, [notifications, currentNotification, removeNotification]);

  const handleClose = () => {
    if (currentNotification) {
      setCurrentNotification(null);
      removeNotification(currentNotification.id);
    }
  };

  return (
    <Snackbar
      open={!!currentNotification}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
    >
      {currentNotification && (
        <Alert
          onClose={handleClose}
          severity={getNotificationColor(currentNotification.type)}
          variant="filled"
          sx={{ minWidth: 300 }}
        >
          <Typography variant="subtitle2" gutterBottom>
            {currentNotification.title}
          </Typography>
          <Typography variant="body2">
            {currentNotification.message}
          </Typography>
        </Alert>
      )}
    </Snackbar>
  );
};

const NotificationCenter = () => {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const {
    notifications,
    removeNotification,
    markAsRead,
    markAllAsRead,
    clearAllNotifications,
  } = useNotificationStore();

  const unreadCount = notifications.filter(n => !n.read).length;

  const handleNotificationClick = (notification) => {
    if (!notification.read) {
      markAsRead(notification.id);
    }
  };

  const formatTime = (timestamp) => {
    const now = new Date();
    const diff = now - new Date(timestamp);
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return '방금 전';
    if (minutes < 60) return `${minutes}분 전`;
    if (hours < 24) return `${hours}시간 전`;
    if (days < 7) return `${days}일 전`;
    return new Date(timestamp).toLocaleDateString();
  };

  return (
    <>
      <IconButton
        color="inherit"
        onClick={() => setDrawerOpen(true)}
        sx={{ ml: 1 }}
      >
        <Badge badgeContent={unreadCount} color="error">
          {unreadCount > 0 ? <NotificationActiveIcon /> : <NotificationIcon />}
        </Badge>
      </IconButton>

      <Drawer
        anchor="right"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        sx={{
          '& .MuiDrawer-paper': {
            width: 400,
            maxWidth: '90vw',
          },
        }}
      >
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="h6">
              알림센터
            </Typography>
            <IconButton onClick={() => setDrawerOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Box>
          
          {notifications.length > 0 && (
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                size="small"
                startIcon={<DoneAllIcon />}
                onClick={markAllAsRead}
                disabled={unreadCount === 0}
              >
                모두 읽음
              </Button>
              <Button
                size="small"
                startIcon={<ClearIcon />}
                onClick={clearAllNotifications}
                color="error"
              >
                모두 삭제
              </Button>
            </Box>
          )}
        </Box>

        <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
          {notifications.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <NotificationIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
              <Typography variant="body2" color="text.secondary">
                알림이 없습니다
              </Typography>
            </Box>
          ) : (
            <List>
              {notifications.map((notification, index) => (
                <div key={notification.id}>
                  <ListItem
                    button
                    onClick={() => handleNotificationClick(notification)}
                    sx={{
                      bgcolor: notification.read ? 'transparent' : 'action.hover',
                      '&:hover': { bgcolor: 'action.selected' },
                    }}
                  >
                    <ListItemIcon>
                      {getNotificationIcon(notification.type)}
                    </ListItemIcon>
                    
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="subtitle2">
                            {notification.title}
                          </Typography>
                          {!notification.read && (
                            <Chip size="small" label="New" color="primary" />
                          )}
                        </Box>
                      }
                      secondary={
                        <>
                          <Typography variant="body2" sx={{ mt: 0.5 }}>
                            {notification.message}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {formatTime(notification.timestamp)}
                          </Typography>
                        </>
                      }
                    />
                    
                    <ListItemSecondaryAction>
                      <IconButton
                        edge="end"
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeNotification(notification.id);
                        }}
                      >
                        <CloseIcon fontSize="small" />
                      </IconButton>
                    </ListItemSecondaryAction>
                  </ListItem>
                  {index < notifications.length - 1 && <Divider />}
                </div>
              ))}
            </List>
          )}
        </Box>
      </Drawer>
    </>
  );
};

const NotificationSystem = () => {
  return (
    <>
      <NotificationToast />
      <NotificationCenter />
    </>
  );
};

export default NotificationSystem;