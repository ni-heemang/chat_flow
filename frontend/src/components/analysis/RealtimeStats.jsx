import { Box, Typography, Paper, Grid, Card, CardContent, Chip } from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  People as PeopleIcon,
  Message as MessageIcon,
  Schedule as ScheduleIcon,
  Analytics as AnalyticsIcon,
} from '@mui/icons-material';

const RealtimeStats = ({ analysisStats, keywordData, participationData, hourlyActivityData, lastUpdated }) => {
  // 통계 계산
  const totalMessages = participationData?.userParticipation?.reduce((sum, user) => sum + user.messageCount, 0) || 0;
  const totalKeywords = keywordData?.totalKeywords || 0;
  const totalUsers = participationData?.totalUsers || 0;
  const peakHour = hourlyActivityData?.hourlyActivity?.reduce((max, current) => 
    current.messageCount > max.messageCount ? current : max
  , { hour: 0, messageCount: 0 });

  const statsCards = [
    {
      title: '총 메시지',
      value: totalMessages,
      icon: <MessageIcon color="primary" sx={{ fontSize: 30 }} />,
      color: 'primary.main',
      suffix: '개',
    },
    {
      title: '활성 사용자',
      value: totalUsers,
      icon: <PeopleIcon color="success" sx={{ fontSize: 30 }} />,
      color: 'success.main',
      suffix: '명',
    },
    {
      title: '키워드 수',
      value: totalKeywords,
      icon: <AnalyticsIcon color="info" sx={{ fontSize: 30 }} />,
      color: 'info.main',
      suffix: '개',
    },
    {
      title: '최고 활동 시간',
      value: peakHour?.hour !== undefined ? `${peakHour.hour}시` : '-',
      icon: <ScheduleIcon color="warning" sx={{ fontSize: 30 }} />,
      color: 'warning.main',
      suffix: '',
      subtitle: peakHour?.messageCount ? `${peakHour.messageCount}개 메시지` : '',
    },
  ];

  return (
    <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h6" display="flex" alignItems="center">
          <TrendingUpIcon sx={{ mr: 1 }} />
          실시간 통계
        </Typography>
        <Box display="flex" alignItems="center" gap={1}>
          {analysisStats?.hasRealtimeData && (
            <Chip 
              label="실시간" 
              color="success" 
              size="small" 
              variant="outlined" 
            />
          )}
          {lastUpdated && (
            <Typography variant="caption" color="text.secondary">
              마지막 업데이트: {new Date(lastUpdated).toLocaleString()}
            </Typography>
          )}
        </Box>
      </Box>

      <Grid container spacing={2}>
        {statsCards.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card 
              elevation={1} 
              sx={{ 
                height: '100%',
                borderLeft: 4,
                borderLeftColor: stat.color,
                transition: 'all 0.3s ease',
                '&:hover': {
                  elevation: 4,
                  transform: 'translateY(-2px)',
                }
              }}
            >
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      {stat.title}
                    </Typography>
                    <Typography variant="h4" component="div" sx={{ color: stat.color, fontWeight: 'bold' }}>
                      {stat.value}
                      {stat.suffix && (
                        <Typography component="span" variant="h6" color="text.secondary">
                          {stat.suffix}
                        </Typography>
                      )}
                    </Typography>
                    {stat.subtitle && (
                      <Typography variant="caption" color="text.secondary">
                        {stat.subtitle}
                      </Typography>
                    )}
                  </Box>
                  <Box>
                    {stat.icon}
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {analysisStats && (
        <Box mt={3} p={2} sx={{ backgroundColor: 'grey.50', borderRadius: 1 }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            분석 통계 요약
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <Typography variant="body2">
                총 분석: <strong>{analysisStats.totalAnalysisCount || 0}회</strong>
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Typography variant="body2">
                키워드 분석: <strong>{analysisStats.keywordAnalysisCount || 0}회</strong>
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Typography variant="body2">
                시간 패턴: <strong>{analysisStats.timePatternCount || 0}회</strong>
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Typography variant="body2">
                참여도 분석: <strong>{analysisStats.participationCount || 0}회</strong>
              </Typography>
            </Grid>
          </Grid>
          {analysisStats.latestAnalysisDate && (
            <Typography variant="caption" color="text.secondary" mt={1} display="block">
              최근 분석: {new Date(analysisStats.latestAnalysisDate).toLocaleString()}
            </Typography>
          )}
        </Box>
      )}
    </Paper>
  );
};

export default RealtimeStats;