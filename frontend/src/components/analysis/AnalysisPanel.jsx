import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Tabs,
  Tab,
  Paper,
  Alert,
  IconButton,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  Card,
  CardContent,
  CardActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Clear as ClearIcon,
  Info as InfoIcon,
  GetApp as DownloadIcon,
  Build as BuildIcon,
} from '@mui/icons-material';
import KeywordChart from './KeywordChart';
import TimeChart from './TimeChart';
import ParticipationChart from './ParticipationChart';
import RealtimeStats from './RealtimeStats';
import useAnalysisStore from '../../store/analysisStore';
import useSocket from '../../hooks/useSocket';
import useChatStore from '../../store/chatStore';
import useNotificationStore from '../../store/notificationStore';
import { exportAnalysisDataToPDF } from '../../utils/pdfExport';

const AnalysisPanel = ({ roomId, compact = false }) => {
  const [tabValue, setTabValue] = useState(0);
  const [clearDialogOpen, setClearDialogOpen] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState(1); // 기본값: 최근 1일
  
  // 기간 옵션 정의
  const periodOptions = [
    { value: null, label: '전체 기간', shortLabel: '전체' },
    { value: 1, label: '최근 1일', shortLabel: '1일' },
    { value: 7, label: '최근 7일', shortLabel: '7일' },
    { value: 30, label: '최근 30일', shortLabel: '30일' },
  ];
  
  const {
    keywordData,
    participationData,
    hourlyActivityData,
    analysisStats,
    loading,
    error,
    lastUpdated,
    fetchAnalysisSummary,
    fetchAnalysisStats,
    fetchAnalysisSummaryByPeriod,
    fetchKeywordAnalysisByPeriod,
    fetchParticipationAnalysisByPeriod,
    fetchHourlyAnalysisByPeriod,
    updateKeywordData,
    updateParticipationData,
    updateHourlyActivityData,
    clearError,
    clearServerAnalysisData,
    rebuildServerAnalysisData,
  } = useAnalysisStore();

  const { subscribeToAnalysis, isConnected } = useSocket();
  const { rooms } = useChatStore();
  const { success: showSuccessNotification, error: showErrorNotification } = useNotificationStore();

  // 실시간 분석 데이터 구독
  useEffect(() => {
    if (!isConnected || !roomId) return;

    console.log('Subscribing to analysis data for room:', roomId);
    
    const analysisSubscription = subscribeToAnalysis(roomId, (analysisUpdate) => {
      console.log('Analysis update received:', analysisUpdate);
      
      switch (analysisUpdate.type) {
        case 'KEYWORD_UPDATE':
          updateKeywordData(analysisUpdate);
          break;
        case 'PARTICIPATION_UPDATE':
          updateParticipationData(analysisUpdate);
          break;
        case 'HOURLY_UPDATE':
          updateHourlyActivityData(analysisUpdate);
          break;
        default:
          console.log('Unknown analysis update type:', analysisUpdate.type);
      }
    });

    return () => {
      if (analysisSubscription) {
        analysisSubscription.unsubscribe();
      }
    };
  }, [isConnected, roomId, subscribeToAnalysis, updateKeywordData, updateParticipationData, updateHourlyActivityData]);

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleRefresh = useCallback(async (period = selectedPeriod) => {
    if (!roomId) return;
    
    console.log('Refreshing analysis data for room:', roomId, 'period:', period);
    
    // 기간별 데이터 요청
    if (period !== null) {
      // 기간이 선택된 경우 기간별 API 사용
      await Promise.all([
        fetchAnalysisSummaryByPeriod(roomId, period),
        fetchAnalysisStats(roomId)
      ]);
    } else {
      // 전체 기간은 기존 방식 사용
      await Promise.all([
        fetchAnalysisSummary(roomId),
        fetchAnalysisStats(roomId)
      ]);
    }
  }, [roomId, selectedPeriod, fetchAnalysisSummary, fetchAnalysisStats, fetchAnalysisSummaryByPeriod]);

  // 기간 변경 핸들러
  const handlePeriodChange = useCallback((event) => {
    const newPeriod = event.target.value === '' ? null : event.target.value;
    setSelectedPeriod(newPeriod);
    console.log('Period changed to:', newPeriod);
    
    // 기간 변경 시 자동으로 새로고침
    handleRefresh(newPeriod);
  }, [handleRefresh]);

  // 초기 데이터 로드
  useEffect(() => {
    if (roomId) {
      handleRefresh();
    }
  }, [roomId, handleRefresh]);

  const handleClearData = async () => {
    if (!roomId) return;
    
    const result = await clearServerAnalysisData(roomId);
    if (result.success) {
      setClearDialogOpen(false);
      // 데이터 클리어 후 새로고침
      setTimeout(() => {
        handleRefresh();
      }, 1000);
    }
  };

  const handleRebuildData = useCallback(async () => {
    if (!roomId) return;
    
    console.log('분석 데이터 재구축 시작:', roomId);
    const result = await rebuildServerAnalysisData(roomId);
    
    if (result.success) {
      showSuccessNotification('데이터 재구축 성공', '분석 데이터가 재구축되었습니다.');
      // 재구축 후 새로고침
      setTimeout(() => {
        handleRefresh();
      }, 1000);
    } else {
      showErrorNotification('데이터 재구축 실패', result.error || '분석 데이터 재구축에 실패했습니다.');
    }
  }, [roomId, rebuildServerAnalysisData, showSuccessNotification, showErrorNotification, handleRefresh]);

  const handleExportPDF = async () => {
    try {
      const roomName = rooms.find(room => room.id === roomId)?.name || `채팅방 ${roomId}`;
      const analysisData = {
        keywordData,
        participationData,
        hourlyActivityData,
        analysisStats,
      };
      
      await exportAnalysisDataToPDF(analysisData, roomName);
      showSuccessNotification('PDF 내보내기 성공', '분석 보고서가 다운로드되었습니다.');
    } catch (error) {
      console.error('PDF export error:', error);
      showErrorNotification('PDF 내보내기 실패', '분석 보고서 다운로드에 실패했습니다.');
    }
  };

  const tabs = [
    { label: '통계 요약', component: null },
    { label: '키워드 분석', component: <KeywordChart keywordData={keywordData} loading={loading} error={error} /> },
    { label: '참여도 분석', component: <ParticipationChart participationData={participationData} loading={loading} error={error} /> },
    { label: '시간 패턴', component: <TimeChart hourlyActivityData={hourlyActivityData} loading={loading} error={error} /> },
  ];

  if (!roomId) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: compact ? 300 : 500 }}>
        <Box display="flex" justifyContent="center" alignItems="center" height="100%">
          <Typography variant="body2" color="text.secondary">
            채팅방을 선택하면 분석 데이터를 확인할 수 있습니다
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
          {error}
        </Alert>
      )}

      {/* 실시간 통계 - 항상 최상단에 표시 */}
      {tabValue === 0 && (
        <RealtimeStats 
          analysisStats={analysisStats}
          keywordData={keywordData}
          participationData={participationData}
          hourlyActivityData={hourlyActivityData}
          lastUpdated={lastUpdated}
          roomId={roomId}
        />
      )}

      <Paper elevation={2}>
        <Box display="flex" justifyContent="space-between" alignItems="center" p={2} pb={0}>
          <Box display="flex" alignItems="center" gap={2}>
            <Tabs value={tabValue} onChange={handleTabChange} variant="scrollable" scrollButtons="auto">
              {tabs.map((tab, index) => (
                <Tab key={index} label={tab.label} />
              ))}
            </Tabs>
          </Box>
          
          <Box display="flex" alignItems="center" gap={2}>
            {/* 기간 선택기 */}
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>분석 기간</InputLabel>
              <Select
                value={selectedPeriod === null ? '' : selectedPeriod}
                label="분석 기간"
                onChange={handlePeriodChange}
              >
                {periodOptions.map((option) => (
                  <MenuItem key={option.value || 'all'} value={option.value === null ? '' : option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {/* 현재 선택된 기간 표시 */}
            {selectedPeriod !== null && (
              <Chip 
                label={periodOptions.find(opt => opt.value === selectedPeriod)?.shortLabel || '전체'}
                size="small"
                color="primary"
                variant="outlined"
              />
            )}
            
            <Box display="flex" gap={1}>
              <Tooltip title="PDF로 내보내기">
                <IconButton onClick={handleExportPDF} disabled={loading || !keywordData}>
                  <DownloadIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title="분석 데이터 재구축">
                <IconButton onClick={handleRebuildData} disabled={loading} color="primary">
                  <BuildIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title="데이터 새로고침">
                <IconButton onClick={handleRefresh} disabled={loading}>
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
              
              <Tooltip title="분석 데이터 초기화 (개발용)">
                <IconButton 
                  onClick={() => setClearDialogOpen(true)} 
                  color="warning"
                  disabled={loading}
                >
                  <ClearIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title={`실시간 연결: ${isConnected ? '연결됨' : '연결 안됨'}`}>
                <IconButton color={isConnected ? 'success' : 'error'}>
                  <InfoIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
        </Box>

        <Box sx={{ p: 2, pt: 0 }}>
          {/* 빈 데이터 상태 표시 */}
          {!loading && !keywordData && !participationData && !hourlyActivityData ? (
            <Card sx={{ textAlign: 'center', py: 4 }}>
              <CardContent>
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  분석 데이터가 없습니다
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  이 채팅방에는 아직 분석할 메시지가 없거나 분석 데이터가 초기화되지 않았습니다.
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  기존 메시지를 기반으로 분석 데이터를 생성하려면 '데이터 재구축' 버튼을 클릭하세요.
                </Typography>
              </CardContent>
              <CardActions sx={{ justifyContent: 'center', pb: 3 }}>
                <Button 
                  variant="contained" 
                  startIcon={<BuildIcon />}
                  onClick={handleRebuildData}
                  disabled={loading}
                  sx={{ mr: 1 }}
                >
                  데이터 재구축
                </Button>
                <Button 
                  variant="outlined"
                  startIcon={<RefreshIcon />}
                  onClick={handleRefresh}
                  disabled={loading}
                >
                  새로고침
                </Button>
              </CardActions>
            </Card>
          ) : (
            <>
              {/* 통계 요약 탭에서는 차트들을 그리드로 표시 */}
              {tabValue === 0 ? (
                <Box sx={{ display: 'grid', gridTemplateColumns: compact ? '1fr' : 'repeat(auto-fit, minmax(400px, 1fr))', gap: 2 }}>
                  <KeywordChart keywordData={keywordData} loading={loading} error={error} />
                  <ParticipationChart participationData={participationData} loading={loading} error={error} />
                  <TimeChart hourlyActivityData={hourlyActivityData} loading={loading} error={error} />
                </Box>
              ) : (
                // 개별 탭에서는 해당 차트만 표시
                tabs[tabValue].component
              )}
            </>
          )}
        </Box>
      </Paper>

      {/* 데이터 초기화 확인 다이얼로그 */}
      <Dialog open={clearDialogOpen} onClose={() => setClearDialogOpen(false)}>
        <DialogTitle color="warning.main">분석 데이터 초기화</DialogTitle>
        <DialogContent>
          <Typography>
            채팅방의 모든 분석 데이터를 초기화하시겠습니까?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            이 작업은 되돌릴 수 없으며, 새로운 메시지부터 다시 분석이 시작됩니다.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClearDialogOpen(false)}>취소</Button>
          <Button onClick={handleClearData} color="warning" variant="contained">
            초기화
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AnalysisPanel;