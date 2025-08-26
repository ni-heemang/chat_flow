import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  Chat as ChatIcon,
  Group as GroupIcon,
  TrendingUp as TrendingUpIcon,
  AccessTime as AccessTimeIcon,
} from '@mui/icons-material';
import useChatStore from '../store/chatStore';

const StatsPage = () => {
  const { getStats } = useChatStore();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchStats = async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await getStats();
        if (result.success) {
          setStats(result.data);
        } else {
          setError(result.error);
        }
      } catch {
        setError('통계 데이터를 불러오는 중 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, [getStats]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
        <Typography variant="h4" gutterBottom>
          채팅방 통계
        </Typography>

        <Grid container spacing={3}>
          {/* 전체 채팅방 수 */}
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <ChatIcon color="primary" sx={{ fontSize: 40, mr: 2 }} />
                  <Box>
                    <Typography variant="h4" color="primary">
                      {stats?.totalRooms || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      전체 채팅방
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* 활성 채팅방 수 */}
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <TrendingUpIcon color="success" sx={{ fontSize: 40, mr: 2 }} />
                  <Box>
                    <Typography variant="h4" color="success.main">
                      {stats?.activeRooms || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      활성 채팅방
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* 전체 사용자 수 */}
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <GroupIcon color="info" sx={{ fontSize: 40, mr: 2 }} />
                  <Box>
                    <Typography variant="h4" color="info.main">
                      {stats?.totalUsers || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      총 사용자
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* 온라인 사용자 수 */}
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <AccessTimeIcon color="warning" sx={{ fontSize: 40, mr: 2 }} />
                  <Box>
                    <Typography variant="h4" color="warning.main">
                      {stats?.onlineUsers || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      온라인 사용자
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* 상세 통계 */}
          <Grid item xs={12}>
            <Paper elevation={2} sx={{ p: 3 }}>
              <Typography variant="h5" gutterBottom>
                상세 통계
              </Typography>
              
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle1" gutterBottom>
                    채팅방 현황
                  </Typography>
                  <Box sx={{ pl: 2 }}>
                    <Typography variant="body2">
                      • 공개 채팅방: {stats?.publicRooms || 0}개
                    </Typography>
                    <Typography variant="body2">
                      • 비공개 채팅방: {stats?.privateRooms || 0}개
                    </Typography>
                    <Typography variant="body2">
                      • 평균 참여자 수: {stats?.averageParticipants || 0}명
                    </Typography>
                  </Box>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle1" gutterBottom>
                    사용 현황
                  </Typography>
                  <Box sx={{ pl: 2 }}>
                    <Typography variant="body2">
                      • 오늘 생성된 채팅방: {stats?.todayCreatedRooms || 0}개
                    </Typography>
                    <Typography variant="body2">
                      • 오늘 전송된 메시지: {stats?.todayMessages || 0}개
                    </Typography>
                    <Typography variant="body2">
                      • 최고 동시 접속자: {stats?.peakConcurrentUsers || 0}명
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </Paper>
          </Grid>
        </Grid>
      </Box>
  );
};

export default StatsPage;