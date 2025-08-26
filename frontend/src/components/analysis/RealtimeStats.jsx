import { Box, Typography, Paper, Grid, Card, CardContent, Chip, Divider } from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  People as PeopleIcon,
  Message as MessageIcon,
  Schedule as ScheduleIcon,
  Analytics as AnalyticsIcon,
  Flag as FlagIcon,
  AccessTime as AccessTimeIcon,
} from '@mui/icons-material';
import { useState, useEffect } from 'react';
import analysisService from '../../services/analysisService';

const RealtimeStats = ({ analysisStats, keywordData, participationData, hourlyActivityData, lastUpdated, roomId }) => {
  const [purposeData, setPurposeData] = useState(null);
  const [peakHoursData, setPeakHoursData] = useState(null);
  const [loading, setLoading] = useState(false);

  // 채팅방 목적과 활발한 시간대 데이터 로드
  useEffect(() => {
    if (roomId) {
      fetchPurposeAndPeakHours();
    }
  }, [roomId]);

  const fetchPurposeAndPeakHours = async () => {
    if (loading) return;
    
    setLoading(true);
    try {
      const [purposeResult, peakHoursResult] = await Promise.all([
        analysisService.getRoomPurposeAnalysis(roomId),
        analysisService.getRoomPeakHours(roomId)
      ]);
      
      setPurposeData(purposeResult);
      setPeakHoursData(peakHoursResult);
    } catch (error) {
      console.error('Purpose and peak hours fetch error:', error);
    } finally {
      setLoading(false);
    }
  };


  return (
    <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
      {/* 채팅방 목적 및 활발한 시간대 */}
      {(purposeData || peakHoursData) && (
        <>
          <Box>
            <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
              <Typography variant="h6" display="flex" alignItems="center">
                <FlagIcon sx={{ mr: 1 }} />
                채팅방 인사이트
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ fontStyle: 'italic', fontSize: '13px' }}>
                * 30일 기반
              </Typography>
            </Box>
            
            <Grid container spacing={2}>
              {/* 채팅방 목적 */}
              {purposeData && (
                <Grid item xs={12}>
                  <Card 
                    elevation={1} 
                    sx={{ 
                      borderLeft: 4,
                      borderLeftColor: 'secondary.main',
                      mb: 2
                    }}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="flex-start" gap={2}>
                        <FlagIcon color="secondary" sx={{ mt: 0.5 }} />
                        <Box flex={1}>
                          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                            채팅방의 주요 목적
                          </Typography>
                          <Typography variant="body1" sx={{ fontWeight: 'medium', mb: 1 }}>
                            {purposeData.purpose || '분석 중입니다...'}
                          </Typography>
                          <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
                            {purposeData.confidence && (
                              <Chip 
                                label={`신뢰도 ${Math.round(purposeData.confidence * 100)}%`}
                                size="small"
                                color="secondary"
                                variant="outlined"
                              />
                            )}
                            {purposeData.analyzedMessages && (
                              <Chip 
                                label={`${purposeData.analyzedMessages}개 메시지 분석`}
                                size="small"
                                variant="outlined"
                              />
                            )}
                          </Box>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              )}

              {/* 활발한 시간대 */}
              {peakHoursData && (
                <Grid item xs={12}>
                  <Card 
                    elevation={1} 
                    sx={{ 
                      borderLeft: 4,
                      borderLeftColor: 'info.main',
                    }}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="flex-start" gap={2}>
                        <AccessTimeIcon color="info" sx={{ mt: 0.5 }} />
                        <Box flex={1}>
                          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                            가장 활발한 시간대
                          </Typography>
                          <Typography variant="h6" sx={{ color: 'info.main', mb: 1 }}>
                            {peakHoursData.peakHour || '정보 없음'}
                          </Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                            {peakHoursData.peakHourDescription || '활동 패턴을 분석할 수 없습니다.'}
                          </Typography>
                          
                          {/* 인사이트 표시 */}
                          {peakHoursData.insights && peakHoursData.insights.length > 0 && (
                            <Box sx={{ mb: 2 }}>
                              {peakHoursData.insights.map((insight, index) => (
                                <Typography 
                                  key={index}
                                  variant="body2" 
                                  sx={{ 
                                    color: 'text.secondary',
                                    display: 'flex',
                                    alignItems: 'center',
                                    mb: 0.5,
                                    '&:before': {
                                      content: '"💡"',
                                      marginRight: '8px'
                                    }
                                  }}
                                >
                                  {insight}
                                </Typography>
                              ))}
                            </Box>
                          )}

                          <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
                            {peakHoursData.totalMessages && (
                              <Chip 
                                label={`총 ${peakHoursData.totalMessages}개 메시지`}
                                size="small"
                                variant="outlined"
                              />
                            )}
                            {peakHoursData.analysisPeriod && (
                              <Chip 
                                label={peakHoursData.analysisPeriod}
                                size="small"
                                color="info"
                                variant="outlined"
                              />
                            )}
                          </Box>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              )}
            </Grid>
          </Box>
        </>
      )}
    </Paper>
  );
};

export default RealtimeStats;