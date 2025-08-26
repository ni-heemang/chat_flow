import { useRef } from 'react';
import { Box, Typography, Paper, CircularProgress } from '@mui/material';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Doughnut } from 'react-chartjs-2';

// Chart.js 컴포넌트 등록
ChartJS.register(ArcElement, Tooltip, Legend);

const ParticipationChart = ({ participationData, loading, error }) => {
  const chartRef = useRef();

  // 차트 데이터 변환
  const chartData = participationData ? {
    labels: participationData.userParticipation?.map(item => item.username) || [],
    datasets: [
      {
        label: '메시지 수',
        data: participationData.userParticipation?.map(item => item.messageCount) || [],
        backgroundColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40',
          '#FF6384',
          '#C9CBCF',
          '#4BC0C0',
          '#FF9F40'
        ],
        borderColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40',
          '#FF6384',
          '#C9CBCF',
          '#4BC0C0',
          '#FF9F40'
        ],
        borderWidth: 1,
        hoverOffset: 4,
      },
    ],
  } : null;

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
        labels: {
          padding: 20,
          usePointStyle: true,
          font: {
            size: 12
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const total = context.dataset.data.reduce((sum, value) => sum + value, 0);
            const percentage = total > 0 ? ((context.parsed / total) * 100).toFixed(1) : 0;
            return `${context.label}: ${context.parsed}개 (${percentage}%)`;
          }
        }
      }
    },
    cutout: '50%', // 도넛 차트의 중앙 구멍 크기
    animation: {
      duration: 1000,
    },
  };

  if (loading) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: 400 }}>
        <Typography variant="h6" gutterBottom>
          사용자별 참여도
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <CircularProgress />
        </Box>
      </Paper>
    );
  }

  if (error) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: 400 }}>
        <Typography variant="h6" gutterBottom color="error">
          사용자별 참여도
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <Typography color="error">{error}</Typography>
        </Box>
      </Paper>
    );
  }

  if (!participationData || !participationData.userParticipation || participationData.userParticipation.length === 0) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: 400 }}>
        <Typography variant="h6" gutterBottom>
          사용자별 참여도
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <Typography variant="body2" color="text.secondary">
            참여도 데이터가 없습니다
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper elevation={2} sx={{ p: 3, height: 400 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">
          사용자별 참여도
        </Typography>
        {participationData.lastUpdated && (
          <Typography variant="caption" color="text.secondary">
            {new Date(participationData.lastUpdated).toLocaleString()}
          </Typography>
        )}
      </Box>
      
      <Box sx={{ height: 300 }}>
        <Doughnut ref={chartRef} data={chartData} options={options} />
      </Box>

      <Box mt={2}>
        <Typography variant="body2" color="text.secondary">
          총 참여자: {participationData.totalUsers}명 | 
          총 메시지: {participationData.userParticipation?.reduce((sum, item) => sum + item.messageCount, 0) || 0}개
        </Typography>
      </Box>
    </Paper>
  );
};

export default ParticipationChart;