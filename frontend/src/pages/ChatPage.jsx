import { useParams } from 'react-router-dom';
import { Box, Typography, Paper } from '@mui/material';
import ChatRoom from '../components/chat/ChatRoom';

const ChatPage = () => {
  const { roomId } = useParams();

  if (!roomId) {
    return (
      <Box>
        <Typography variant="h4" gutterBottom>
          채팅
        </Typography>
        <Paper elevation={2} sx={{ p: 3, minHeight: '400px' }}>
          <Typography variant="body1" color="text.secondary">
            채팅방을 선택해주세요.
          </Typography>
        </Paper>
      </Box>
    );
  }

  return <ChatRoom />;
};

export default ChatPage;