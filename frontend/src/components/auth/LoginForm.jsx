import { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  InputAdornment,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import {
  Chat as ChatIcon,
  Visibility,
  VisibilityOff,
} from '@mui/icons-material';
import useUserStore from '../../store/userStore';

const LoginForm = () => {
  const [tabValue, setTabValue] = useState(0);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [openErrorDialog, setOpenErrorDialog] = useState(false);
  const [errorDialogMessage, setErrorDialogMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);
  const { login, register, loading, clearError } = useUserStore();

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
    setError('');
    clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!username.trim()) {
      setError('아이디를 입력해주세요.');
      return;
    }

    if (!password.trim()) {
      setError('비밀번호를 입력해주세요.');
      return;
    }

    if (username.length < 3) {
      setError('아이디는 3글자 이상이어야 합니다.');
      return;
    }

    if (password.length < 6) {
      setError('비밀번호는 6글자 이상이어야 합니다.');
      return;
    }

    let result;
    if (tabValue === 0) {
      result = await login(username, password);
    } else {
      if (!name.trim()) {
        setError('이름을 입력해주세요.');
        return;
      }
      if (name.length < 2) {
        setError('이름은 2글자 이상이어야 합니다.');
        return;
      }
      result = await register(username, password, name);
    }

    if (!result.success) {
      setIsSuccess(false);
      setErrorDialogMessage(result.error);
      setOpenErrorDialog(true);
    } else if (tabValue === 1) {
      // 회원가입 성공 시 로그인 탭으로 이동
      setIsSuccess(true);
      setTabValue(0);
      setUsername('');
      setPassword('');
      setName('');
      setError('');
      setErrorDialogMessage('회원가입이 완료되었습니다. 로그인해주세요.');
      setOpenErrorDialog(true);
    }
  };

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: (theme) => theme.palette.background.default,
        px: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 600, minWidth: 500 }}>
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <ChatIcon sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
            <Typography variant="h4" component="h1" gutterBottom>
              FlowChat
            </Typography>
            <Typography variant="body1" color="text.secondary">
              채팅 서비스
            </Typography>
          </Box>

          <Tabs
            value={tabValue}
            onChange={handleTabChange}
            centered
            sx={{ mb: 3 }}
          >
            <Tab label="로그인" />
            <Tab label="회원가입" />
          </Tabs>

          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="아이디"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              margin="normal"
              variant="outlined"
              placeholder="아이디를 입력하세요"
              disabled={loading}
            />

            <TextField
              fullWidth
              label="비밀번호"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              margin="normal"
              variant="outlined"
              placeholder="비밀번호를 입력하세요"
              disabled={loading}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            {tabValue === 1 && (
              <TextField
                fullWidth
                label="이름"
                value={name}
                onChange={(e) => setName(e.target.value)}
                margin="normal"
                variant="outlined"
                placeholder="표시될 이름을 입력하세요"
                disabled={loading}
                inputProps={{ maxLength: 20 }}
              />
            )}

            {error && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {error}
              </Alert>
            )}

            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2, py: 1.5 }}
              disabled={loading || !username.trim() || !password.trim()}
            >
              {loading ? (
                <CircularProgress size={24} color="inherit" />
              ) : tabValue === 0 ? (
                '로그인'
              ) : (
                '회원가입'
              )}
            </Button>
          </form>

          <Typography variant="body2" color="text.secondary" textAlign="center">
            {tabValue === 0
              ? '계정이 없으신가요? 회원가입 탭을 이용하세요'
              : '이미 계정이 있으신가요? 로그인 탭을 이용하세요'}
          </Typography>
        </CardContent>
      </Card>

      {/* 에러 다이얼로그 */}
      <Dialog
        open={openErrorDialog}
        onClose={() => setOpenErrorDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ color: isSuccess ? 'success.main' : 'error.main' }}>
          {isSuccess ? '회원가입 성공' : (tabValue === 0 ? '로그인 실패' : '회원가입 실패')}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {errorDialogMessage}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => {
              setOpenErrorDialog(false);
              setIsSuccess(false);
            }} 
            variant="contained"
            color="primary"
          >
            확인
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default LoginForm;