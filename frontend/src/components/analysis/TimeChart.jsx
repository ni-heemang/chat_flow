import { useRef } from 'react';
import { Box, Typography, Paper, CircularProgress } from '@mui/material';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

// Chart.js 컴포넌트 등록
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

const TimeChart = ({ hourlyActivityData, loading, error }) => {
  const chartRef = useRef();

  // 시간대 레이블 생성 (00시 ~ 23시)
  const timeLabels = Array.from({ length: 24 }, (_, i) => 
    i.toString().padStart(2, '0') + '시'
  );

  // 차트 데이터 변환
  const chartData = hourlyActivityData ? {
    labels: timeLabels,
    datasets: [
      {
        label: '시간대별 메시지 수',
        data: timeLabels.map((_, hour) => {
          const activity = hourlyActivityData.hourlyActivity?.find(item => item.hour === hour);
          return activity ? activity.messageCount : 0;
        }),
        borderColor: '#36A2EB',
        backgroundColor: 'rgba(54, 162, 235, 0.1)',
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#36A2EB',
        pointBorderColor: '#36A2EB',
        pointHoverBackgroundColor: '#36A2EB',
        pointHoverBorderColor: '#36A2EB',
        pointRadius: 4,
        pointHoverRadius: 6,
      },
    ],
  } : null;

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        display: true,
      },
      title: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            return `${context.label}: ${context.parsed.y}개 메시지`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 1,
          callback: function(value) {
            return Number.isInteger(value) ? value : '';
          }
        },
        title: {
          display: true,
          text: '메시지 수'
        }
      },
      x: {
        title: {
          display: true,
          text: '시간대'
        },
        ticks: {
          maxTicksLimit: 12, // 화면 크기에 따라 조절
        }
      }
    },
    animation: {
      duration: 1000,
    },
  };

  if (loading) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: 400 }}>
        <Typography variant="h6" gutterBottom>
          시간대별 활동 패턴
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
          시간대별 활동 패턴
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <Typography color="error">{error}</Typography>
        </Box>
      </Paper>
    );
  }

  if (!hourlyActivityData || !hourlyActivityData.hourlyActivity) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: 400 }}>
        <Typography variant="h6" gutterBottom>
          시간대별 활동 패턴
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <Typography variant="body2" color="text.secondary">
            활동 패턴 데이터가 없습니다
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper elevation={2} sx={{ p: 3, height: 400 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">
          시간대별 활동 패턴
        </Typography>
        {hourlyActivityData.lastUpdated && (
          <Typography variant="caption" color="text.secondary">
            {new Date(hourlyActivityData.lastUpdated).toLocaleString()}
          </Typography>
        )}
      </Box>
      
      <Box sx={{ height: 300 }}>
        <Line ref={chartRef} data={chartData} options={options} />
      </Box>

      <Box mt={2}>
        <Typography variant="body2" color="text.secondary">
          총 메시지: {hourlyActivityData.hourlyActivity?.reduce((sum, item) => sum + item.messageCount, 0) || 0}개
        </Typography>
      </Box>
    </Paper>
  );
};

export default TimeChart;