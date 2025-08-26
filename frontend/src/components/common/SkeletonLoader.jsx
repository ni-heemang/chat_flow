import { Box, Skeleton, Paper } from '@mui/material';

const MessageSkeleton = ({ count = 5 }) => {
  return (
    <Box sx={{ p: 2 }}>
      {Array.from({ length: count }).map((_, index) => (
        <Box key={index} sx={{ mb: 2, display: 'flex', gap: 1 }}>
          <Skeleton variant="circular" width={40} height={40} />
          <Box sx={{ flexGrow: 1 }}>
            <Skeleton variant="text" width="20%" height={20} />
            <Skeleton variant="text" width="80%" height={24} />
            <Skeleton variant="text" width="60%" height={24} />
          </Box>
        </Box>
      ))}
    </Box>
  );
};

const AnalysisSkeleton = () => {
  return (
    <Box sx={{ p: 2 }}>
      <Skeleton variant="text" width="30%" height={32} sx={{ mb: 2 }} />
      
      {/* 탭 영역 */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <Skeleton variant="rounded" width={80} height={32} />
        <Skeleton variant="rounded" width={100} height={32} />
        <Skeleton variant="rounded" width={90} height={32} />
      </Box>
      
      {/* 차트 영역 */}
      <Paper elevation={2} sx={{ p: 2, mb: 2 }}>
        <Skeleton variant="text" width="40%" height={24} sx={{ mb: 2 }} />
        <Skeleton variant="rounded" width="100%" height={300} />
      </Paper>
      
      {/* 통계 카드들 */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 2 }}>
        {Array.from({ length: 4 }).map((_, index) => (
          <Paper key={index} elevation={1} sx={{ p: 2 }}>
            <Skeleton variant="text" width="70%" height={20} />
            <Skeleton variant="text" width="40%" height={36} />
          </Paper>
        ))}
      </Box>
    </Box>
  );
};

const RoomListSkeleton = ({ count = 8 }) => {
  return (
    <Box sx={{ p: 2 }}>
      {Array.from({ length: count }).map((_, index) => (
        <Paper key={index} elevation={1} sx={{ p: 2, mb: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
            <Skeleton variant="text" width="60%" height={24} />
            <Skeleton variant="rounded" width={60} height={20} />
          </Box>
          <Skeleton variant="text" width="80%" height={20} />
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
            <Skeleton variant="text" width="30%" height={16} />
            <Skeleton variant="text" width="20%" height={16} />
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

const StatsSkeleton = () => {
  return (
    <Box sx={{ p: 2 }}>
      <Skeleton variant="text" width="40%" height={32} sx={{ mb: 3 }} />
      
      {/* 통계 카드 그리드 */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 2, mb: 4 }}>
        {Array.from({ length: 6 }).map((_, index) => (
          <Paper key={index} elevation={2} sx={{ p: 3, textAlign: 'center' }}>
            <Skeleton variant="text" width="70%" height={20} sx={{ mx: 'auto', mb: 1 }} />
            <Skeleton variant="text" width="40%" height={40} sx={{ mx: 'auto' }} />
          </Paper>
        ))}
      </Box>
      
      {/* 차트 영역 */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: 2 }}>
        {Array.from({ length: 2 }).map((_, index) => (
          <Paper key={index} elevation={2} sx={{ p: 3 }}>
            <Skeleton variant="text" width="50%" height={24} sx={{ mb: 2 }} />
            <Skeleton variant="rounded" width="100%" height={300} />
          </Paper>
        ))}
      </Box>
    </Box>
  );
};

export {
  MessageSkeleton,
  AnalysisSkeleton,
  RoomListSkeleton,
  StatsSkeleton,
};