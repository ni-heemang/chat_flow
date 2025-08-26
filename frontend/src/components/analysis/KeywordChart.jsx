import { useRef } from 'react';
import { Box, Typography, Paper, CircularProgress } from '@mui/material';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';

// Chart.js 컴포넌트 등록
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

const KeywordChart = ({ keywordData, loading, error }) => {
  const chartRef = useRef();

  // 차트 데이터 변환
  const chartData = keywordData ? {
    labels: keywordData.topKeywords?.map(item => item.keyword) || [],
    datasets: [
      {
        label: '키워드 빈도',
        data: keywordData.topKeywords?.map(item => item.count) || [],
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
      },
    ],
  } : null;

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        display: false, // 키워드 차트에서는 범례 숨김
      },
      title: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            return `${context.label}: ${context.parsed.y}회`;
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
          text: '빈도수'
        }
      },
      x: {
        title: {
          display: true,
          text: '키워드'
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
          키워드 분석
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
          키워드 분석
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <Typography color="error">{error}</Typography>
        </Box>
      </Paper>
    );
  }

  if (!keywordData || !keywordData.topKeywords || keywordData.topKeywords.length === 0) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: 400 }}>
        <Typography variant="h6" gutterBottom>
          키워드 분석
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" height={300}>
          <Typography variant="body2" color="text.secondary">
            분석할 키워드가 없습니다
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper elevation={2} sx={{ p: 3, height: 400 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">
          키워드 분석
        </Typography>
        {keywordData.lastUpdated && (
          <Typography variant="caption" color="text.secondary">
            {new Date(keywordData.lastUpdated).toLocaleString()}
          </Typography>
        )}
      </Box>
      
      <Box sx={{ height: 300 }}>
        <Bar ref={chartRef} data={chartData} options={options} />
      </Box>

      <Box mt={2}>
        <Typography variant="body2" color="text.secondary">
          총 키워드 수: {keywordData.totalKeywords}개
        </Typography>
      </Box>
    </Paper>
  );
};

export default KeywordChart;