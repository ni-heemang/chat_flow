import {
  Box,
  Typography,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Chip,
  Divider,
} from '@mui/material';
import {
  Person as PersonIcon,
  Circle as CircleIcon,
} from '@mui/icons-material';

const UserList = ({ participants = [] }) => {
  if (!participants || participants.length === 0) {
    return (
      <Box sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          참여자
        </Typography>
        <Typography variant="body2" color="text.secondary">
          참여자 정보를 불러오는 중...
        </Typography>
      </Box>
    );
  }

  const onlineUsers = participants.filter(p => p.isOnline);

  return (
    <Box sx={{ height: '100%', overflow: 'auto' }}>
      <Box sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          참여자 ({participants.length}명)
        </Typography>
        <Typography variant="subtitle2" color="text.secondary">
          온라인 — {onlineUsers.length}명
        </Typography>
      </Box>

      <Divider />

      <List dense>
        {participants.map((participant) => (
          <ListItem key={participant.id}>
            {/* 온라인/오프라인 상태 표시 동그라미 */}
            <Box sx={{ mr: 1, display: 'flex', alignItems: 'center' }}>
              <CircleIcon 
                sx={{ 
                  fontSize: 12,
                  color: participant.isOnline ? 'success.main' : 'error.main'
                }} 
              />
            </Box>
            
            <ListItemAvatar>
              <Avatar 
                sx={{ 
                  width: 32, 
                  height: 32,
                  opacity: participant.isOnline ? 1 : 0.6
                }}
              >
                {participant.name ? participant.name[0] : <PersonIcon />}
              </Avatar>
            </ListItemAvatar>
            
            <ListItemText
              primary={
                <Typography 
                  sx={{ 
                    color: participant.isOnline ? 'text.primary' : 'text.secondary',
                    fontWeight: participant.isOnline ? 'normal' : 'normal'
                  }}
                >
                  {participant.name || participant.username}
                </Typography>
              }
              secondary={
                <Typography 
                  variant="caption" 
                  sx={{ 
                    color: participant.isOnline ? 'text.secondary' : 'text.disabled'
                  }}
                >
                  {participant.username}
                </Typography>
              }
            />
            
            {participant.isAdmin && (
              <Chip 
                label="관리자" 
                size="small" 
                color="primary" 
                variant="outlined"
                sx={{ opacity: participant.isOnline ? 1 : 0.6 }}
              />
            )}
          </ListItem>
        ))}
      </List>

      {participants.length === 0 && (
        <Box sx={{ p: 2, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            아직 참여한 사용자가 없습니다.
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default UserList;