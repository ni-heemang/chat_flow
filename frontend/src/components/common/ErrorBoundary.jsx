import React from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Alert,
  AlertTitle,
  Stack,
} from '@mui/material';
import {
  ErrorOutline as ErrorIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error: error,
      errorInfo: errorInfo,
    });
    
    // 에러 로깅 (개발환경에서만)
    if (import.meta.env.DEV) {
      console.error('Error caught by boundary:', error, errorInfo);
    }
  }

  handleReload = () => {
    window.location.reload();
  };

  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '400px',
            p: 3,
          }}
        >
          <Paper elevation={3} sx={{ maxWidth: 600, p: 4, textAlign: 'center' }}>
            <ErrorIcon sx={{ fontSize: 64, color: 'error.main', mb: 2 }} />
            
            <Typography variant="h5" gutterBottom color="error">
              앱에서 오류가 발생했습니다
            </Typography>
            
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              예상치 못한 오류가 발생했습니다. 페이지를 새로고침하거나 관리자에게 문의해주세요.
            </Typography>

            {import.meta.env.DEV && this.state.error && (
              <Alert severity="error" sx={{ mb: 3, textAlign: 'left' }}>
                <AlertTitle>개발 모드 - 에러 상세 정보</AlertTitle>
                <Typography variant="body2" component="pre" sx={{ fontSize: '0.75rem', overflow: 'auto' }}>
                  {this.state.error.toString()}
                  {this.state.errorInfo.componentStack}
                </Typography>
              </Alert>
            )}

            <Stack direction="row" spacing={2} justifyContent="center">
              <Button
                variant="outlined"
                onClick={this.handleReset}
                startIcon={<RefreshIcon />}
              >
                다시 시도
              </Button>
              
              <Button
                variant="contained"
                onClick={this.handleReload}
                startIcon={<RefreshIcon />}
              >
                페이지 새로고침
              </Button>
            </Stack>
          </Paper>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;