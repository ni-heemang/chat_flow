import { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardHeader,
  FormGroup,
  FormControlLabel,
  Switch,
  RadioGroup,
  Radio,
  Divider,
  Alert,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  IconButton,
} from '@mui/material';
import {
  Brightness4 as DarkModeIcon,
  Brightness7 as LightModeIcon,
  BrightnessAuto as AutoModeIcon,
  Notifications as NotificationsIcon,
  VolumeUp as SoundIcon,
  Computer as DesktopIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import useThemeStore from '../store/themeStore';
import useNotificationStore from '../store/notificationStore';

const SettingsPage = () => {
  const {
    mode: themeMode,
    setMode: setThemeMode,
    isDarkMode,
  } = useThemeStore();

  const {
    soundEnabled,
    desktopEnabled,
    notifications,
    updateSettings,
    requestDesktopPermission,
    clearAllNotifications,
  } = useNotificationStore();

  const [permissionDialog, setPermissionDialog] = useState(false);
  const [clearDialog, setClearDialog] = useState(false);

  const handleThemeChange = (event) => {
    const newMode = event.target.value;
    setThemeMode(newMode); // themeStore에서 자동으로 이벤트 발생시킴
  };

  const handleSoundToggle = (event) => {
    updateSettings({ soundEnabled: event.target.checked });
  };

  const handleDesktopToggle = async (event) => {
    if (event.target.checked) {
      if ('Notification' in window && Notification.permission !== 'granted') {
        setPermissionDialog(true);
        return;
      }
    }
    updateSettings({ desktopEnabled: event.target.checked });
  };

  const handleRequestPermission = async () => {
    const granted = await requestDesktopPermission();
    if (granted) {
      updateSettings({ desktopEnabled: true });
    }
    setPermissionDialog(false);
  };

  const handleClearNotifications = () => {
    clearAllNotifications();
    setClearDialog(false);
  };

  const getThemeIcon = (mode) => {
    switch (mode) {
      case 'light': return <LightModeIcon />;
      case 'dark': return <DarkModeIcon />;
      case 'system': return <AutoModeIcon />;
      default: return <AutoModeIcon />;
    }
  };

  const getNotificationPermissionStatus = () => {
    if (!('Notification' in window)) {
      return { status: 'unsupported', text: '지원되지 않음', color: 'default' };
    }
    
    switch (Notification.permission) {
      case 'granted':
        return { status: 'granted', text: '허용됨', color: 'success' };
      case 'denied':
        return { status: 'denied', text: '거부됨', color: 'error' };
      default:
        return { status: 'default', text: '미설정', color: 'warning' };
    }
  };

  const permissionStatus = getNotificationPermissionStatus();

  return (
    <Box sx={{ 
      maxWidth: 800,
      mx: 'auto'
    }}>
      <Typography variant="h4" gutterBottom>
        설정
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        앱의 테마, 알림 및 기타 설정을 변경할 수 있습니다.
      </Typography>

      {/* 테마 설정 */}
      <Card sx={{ mb: 3 }}>
        <CardHeader
          title="테마 설정"
          avatar={getThemeIcon(themeMode)}
        />
        <CardContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            앱의 외관을 변경할 수 있습니다. 현재 테마: {isDarkMode() ? '다크' : '라이트'} 모드
          </Typography>
          
          <RadioGroup
            value={themeMode}
            onChange={handleThemeChange}
          >
            <FormControlLabel
              value="light"
              control={<Radio />}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <LightModeIcon />
                  라이트 모드
                </Box>
              }
            />
            <FormControlLabel
              value="dark"
              control={<Radio />}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <DarkModeIcon />
                  다크 모드
                </Box>
              }
            />
            <FormControlLabel
              value="system"
              control={<Radio />}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <AutoModeIcon />
                  시스템 설정 따라가기
                </Box>
              }
            />
          </RadioGroup>
        </CardContent>
      </Card>

      {/* 알림 설정 */}
      <Card sx={{ mb: 3 }}>
        <CardHeader
          title="알림 설정"
          avatar={<NotificationsIcon />}
        />
        <CardContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            새 메시지와 분석 완료 알림을 설정할 수 있습니다.
          </Typography>

          <FormGroup>
            <FormControlLabel
              control={
                <Switch
                  checked={soundEnabled}
                  onChange={handleSoundToggle}
                />
              }
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <SoundIcon />
                  알림 소리
                </Box>
              }
            />
            
            <FormControlLabel
              control={
                <Switch
                  checked={desktopEnabled && permissionStatus.status === 'granted'}
                  onChange={handleDesktopToggle}
                  disabled={permissionStatus.status === 'unsupported'}
                />
              }
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <DesktopIcon />
                  데스크톱 알림
                  <Chip
                    size="small"
                    label={permissionStatus.text}
                    color={permissionStatus.color}
                  />
                </Box>
              }
            />
          </FormGroup>

          {permissionStatus.status === 'denied' && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              데스크톱 알림이 차단되어 있습니다. 브라우저 설정에서 알림을 허용해주세요.
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* 알림 히스토리 */}
      {notifications.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardHeader
            title="알림 히스토리"
            action={
              <Button
                startIcon={<DeleteIcon />}
                onClick={() => setClearDialog(true)}
                color="error"
                size="small"
              >
                모두 삭제
              </Button>
            }
          />
          <CardContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              최근 {notifications.length}개의 알림
            </Typography>
            
            <List dense>
              {notifications.slice(0, 5).map((notification) => (
                <ListItem key={notification.id}>
                  <ListItemIcon>
                    <NotificationsIcon 
                      color={notification.read ? 'disabled' : 'primary'}
                      fontSize="small"
                    />
                  </ListItemIcon>
                  <ListItemText
                    primary={notification.title}
                    secondary={`${notification.message} • ${new Date(notification.timestamp).toLocaleString()}`}
                    primaryTypographyProps={{
                      style: { opacity: notification.read ? 0.6 : 1 }
                    }}
                  />
                </ListItem>
              ))}
            </List>
            
            {notifications.length > 5 && (
              <Typography variant="caption" color="text.secondary">
                ...및 {notifications.length - 5}개 더
              </Typography>
            )}
          </CardContent>
        </Card>
      )}

      {/* 데스크톱 알림 권한 요청 다이얼로그 */}
      <Dialog open={permissionDialog} onClose={() => setPermissionDialog(false)}>
        <DialogTitle>데스크톱 알림 권한</DialogTitle>
        <DialogContent>
          <DialogContentText>
            새 메시지와 중요한 알림을 데스크톱에서 받으시겠습니까?
            브라우저에서 알림 권한을 요청합니다.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPermissionDialog(false)}>
            취소
          </Button>
          <Button onClick={handleRequestPermission} variant="contained">
            허용
          </Button>
        </DialogActions>
      </Dialog>

      {/* 알림 삭제 확인 다이얼로그 */}
      <Dialog open={clearDialog} onClose={() => setClearDialog(false)}>
        <DialogTitle>알림 삭제</DialogTitle>
        <DialogContent>
          <DialogContentText>
            모든 알림 히스토리를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClearDialog(false)}>
            취소
          </Button>
          <Button onClick={handleClearNotifications} color="error">
            삭제
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SettingsPage;