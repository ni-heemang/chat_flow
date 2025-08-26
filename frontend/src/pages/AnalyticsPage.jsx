import { useState, useEffect } from 'react';
import { 
  Box, 
  Typography, 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  Alert,
  CircularProgress
} from '@mui/material';
import AnalysisPanel from '../components/analysis/AnalysisPanel';
import useChatStore from '../store/chatStore';

const AnalyticsPage = () => {
  const [selectedRoomId, setSelectedRoomId] = useState('');
  const { rooms, fetchJoinedRooms, loading, error } = useChatStore();

  useEffect(() => {
    // 페이지 로드 시 사용자가 참여한 채팅방 목록만 가져오기
    fetchJoinedRooms();
  }, [fetchJoinedRooms]);

  useEffect(() => {
    // 채팅방이 로드되면 첫 번째 방을 자동 선택
    if (rooms.length > 0 && !selectedRoomId) {
      setSelectedRoomId(rooms[0].id.toString());
    }
  }, [rooms, selectedRoomId]);

  const handleRoomChange = (event) => {
    setSelectedRoomId(event.target.value);
  };

  const selectedRoom = rooms.find(room => room.id.toString() === selectedRoomId);

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        실시간 채팅 분석
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        채팅방별 키워드 분석, 사용자 참여도, 시간대별 활동 패턴을 실시간으로 확인하세요
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* 채팅방 선택 */}
      <Box sx={{ mb: 3, maxWidth: 400 }}>
        <FormControl fullWidth>
          <InputLabel>분석할 채팅방 선택</InputLabel>
          <Select
            value={selectedRoomId}
            label="분석할 채팅방 선택"
            onChange={handleRoomChange}
            disabled={loading}
          >
            {rooms.map((room) => (
              <MenuItem key={room.id} value={room.id.toString()}>
                {room.name} ({room.currentParticipants || 0}명 참여)
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {loading && rooms.length === 0 ? (
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
          <CircularProgress />
        </Box>
      ) : selectedRoomId ? (
        <>
          {selectedRoom && (
            <Box sx={{ mb: 2, p: 2, backgroundColor: 'grey.50', borderRadius: 1 }}>
              <Typography variant="h6">{selectedRoom.name}</Typography>
              {selectedRoom.description && (
                <Typography variant="body2" color="text.secondary">
                  {selectedRoom.description}
                </Typography>
              )}
              <Typography variant="caption" color="text.secondary">
                현재 참여자: {selectedRoom.currentParticipants || 0}명 | 
                생성일: {new Date(selectedRoom.createdAt).toLocaleDateString()}
              </Typography>
            </Box>
          )}
          
          <AnalysisPanel roomId={parseInt(selectedRoomId)} />
        </>
      ) : (
        <Alert severity="info">
          분석할 채팅방을 선택해주세요
        </Alert>
      )}
    </Box>
  );
};

export default AnalyticsPage;