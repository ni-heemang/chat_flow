import { useEffect, useRef } from 'react';
import {
  Box,
  Paper,
  Typography,
  Avatar,
  Chip,
  Divider,
} from '@mui/material';
import {
  Person as PersonIcon,
  Computer as SystemIcon,
} from '@mui/icons-material';

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInHours = (now - date) / (1000 * 60 * 60);

  if (diffInHours < 24) {
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  } else {
    return date.toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
};

const MessageBubble = ({ message, isOwn, showAvatar, showTime }) => {
  const isSystem = message.messageType === 'SYSTEM';

  if (isSystem) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', my: 1 }}>
        <Chip
          icon={<SystemIcon />}
          label={message.content}
          size="small"
          variant="outlined"
          sx={{ backgroundColor: 'grey.100' }}
        />
      </Box>
    );
  }

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: isOwn ? 'row-reverse' : 'row',
        alignItems: 'flex-start',
        mb: 1,
        gap: 1,
      }}
    >
      {showAvatar && !isOwn && (
        <Avatar sx={{ width: 32, height: 32 }}>
          {message.name ? message.name[0] : <PersonIcon />}
        </Avatar>
      )}
      
      <Box sx={{ maxWidth: '70%', minWidth: 0 }}>
        {showAvatar && !isOwn && (
          <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
            {message.name || message.username}
          </Typography>
        )}
        
        <Paper
          elevation={1}
          sx={{
            p: 1.5,
            backgroundColor: isOwn ? 'primary.main' : 'grey.100',
            color: isOwn ? 'white' : 'text.primary',
            borderRadius: 2,
            ...(isOwn ? {
              borderBottomRightRadius: 4,
            } : {
              borderBottomLeftRadius: 4,
            }),
          }}
        >
          <Typography variant="body1" sx={{ wordBreak: 'break-word' }}>
            {message.content}
          </Typography>
        </Paper>
        
        {showTime && (
          <Typography 
            variant="caption" 
            color="text.secondary" 
            sx={{ 
              display: 'block', 
              mt: 0.5,
              textAlign: isOwn ? 'right' : 'left',
              ml: isOwn ? 0 : 1,
              mr: isOwn ? 1 : 0,
            }}
          >
            {formatTime(message.timestamp)}
          </Typography>
        )}
      </Box>
    </Box>
  );
};

const TypingIndicator = ({ typingUsers }) => {
  if (!typingUsers || typingUsers.length === 0) return null;

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1 }}>
      <Avatar sx={{ width: 24, height: 24 }}>
        <PersonIcon fontSize="small" />
      </Avatar>
      <Typography variant="caption" color="text.secondary">
        {typingUsers.length === 1
          ? `${typingUsers[0]}님이 입력 중...`
          : `${typingUsers.slice(0, 2).join(', ')}${typingUsers.length > 2 ? ` 외 ${typingUsers.length - 2}명` : ''}이 입력 중...`
        }
      </Typography>
    </Box>
  );
};

const MessageList = ({ 
  messages = [], 
  typingUsers = [], 
  currentUserId,
  loading = false,
  onLoadMore,
  hasMore = false,
}) => {
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, typingUsers]);

  const handleScroll = () => {
    if (messagesContainerRef.current && hasMore && !loading && onLoadMore) {
      const { scrollTop } = messagesContainerRef.current;
      if (scrollTop === 0) {
        onLoadMore();
      }
    }
  };

  const groupedMessages = messages.reduce((groups, message, index) => {
    const prevMessage = messages[index - 1];
    const showAvatar = !prevMessage || 
      prevMessage.userId !== message.userId || 
      prevMessage.messageType === 'SYSTEM' ||
      message.messageType === 'SYSTEM';
    
    const showTime = !messages[index + 1] || 
      messages[index + 1].userId !== message.userId ||
      new Date(messages[index + 1].timestamp) - new Date(message.timestamp) > 60000;

    groups.push({
      ...message,
      showAvatar,
      showTime,
    });

    return groups;
  }, []);

  return (
    <Box
      sx={{
        flexGrow: 1,
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Box
        ref={messagesContainerRef}
        onScroll={handleScroll}
        sx={{
          flexGrow: 1,
          overflow: 'auto',
          p: 2,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {loading && hasMore && (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <Typography variant="caption" color="text.secondary">
              이전 메시지를 불러오는 중...
            </Typography>
          </Box>
        )}

        {groupedMessages.length === 0 ? (
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              flexGrow: 1,
              textAlign: 'center',
            }}
          >
            <Typography variant="h6" color="text.secondary" gutterBottom>
              첫 번째 메시지를 보내보세요!
            </Typography>
            <Typography variant="body2" color="text.secondary">
              이 채팅방에서 대화를 시작해보세요.
            </Typography>
          </Box>
        ) : (
          groupedMessages.map((message) => (
            <MessageBubble
              key={message.id}
              message={message}
              isOwn={message.userId === currentUserId}
              showAvatar={message.showAvatar}
              showTime={message.showTime}
            />
          ))
        )}

        <TypingIndicator typingUsers={typingUsers} />
        <div ref={messagesEndRef} />
      </Box>
    </Box>
  );
};

export default MessageList;