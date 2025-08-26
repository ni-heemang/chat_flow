import { useState, useRef, useEffect } from 'react';
import {
  Box,
  TextField,
  IconButton,
  Paper,
  Typography,
} from '@mui/material';
import {
  Send as SendIcon,
} from '@mui/icons-material';

const MessageInput = ({ 
  onSendMessage, 
  onTyping, 
  disabled = false,
  placeholder = '메시지를 입력하세요...'
}) => {
  const [message, setMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const typingTimeoutRef = useRef(null);
  const inputRef = useRef(null);

  const handleSend = () => {
    const trimmedMessage = message.trim();
    console.log('handleSend called:', { trimmedMessage, hasCallback: !!onSendMessage });
    
    if (trimmedMessage && onSendMessage) {
      console.log('Sending message:', trimmedMessage);
      onSendMessage(trimmedMessage);
      setMessage('');
      handleStopTyping();
      console.log('Message sent successfully');
    } else {
      console.warn('Cannot send message:', { 
        hasMessage: !!trimmedMessage, 
        hasCallback: !!onSendMessage 
      });
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInputChange = (e) => {
    const value = e.target.value;
    setMessage(value);

    if (onTyping) {
      if (value.trim() && !isTyping) {
        setIsTyping(true);
        onTyping(true);
      }

      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }

      typingTimeoutRef.current = setTimeout(() => {
        handleStopTyping();
      }, 1000);
    }
  };

  const handleStopTyping = () => {
    if (isTyping) {
      setIsTyping(false);
      if (onTyping) {
        onTyping(false);
      }
    }
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = null;
    }
  };

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (inputRef.current && !disabled) {
      inputRef.current.focus();
    }
  }, [disabled]);

  return (
    <Paper 
      elevation={3} 
      sx={{ 
        position: 'sticky', 
        bottom: 0, 
        p: 2, 
        backgroundColor: 'background.paper',
        borderTop: 1,
        borderColor: 'divider',
      }}
    >
      <Box display="flex" alignItems="flex-end" gap={1}>
        <TextField
          ref={inputRef}
          fullWidth
          multiline
          maxRows={4}
          value={message}
          onChange={handleInputChange}
          onKeyPress={handleKeyPress}
          placeholder={placeholder}
          disabled={disabled}
          variant="outlined"
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: 2,
            },
          }}
        />
        <IconButton
          color="primary"
          onClick={handleSend}
          disabled={disabled || !message.trim()}
          sx={{
            backgroundColor: 'primary.main',
            color: 'white',
            '&:hover': {
              backgroundColor: 'primary.dark',
            },
            '&:disabled': {
              backgroundColor: 'grey.300',
              color: 'grey.500',
            },
          }}
        >
          <SendIcon />
        </IconButton>
      </Box>
      
      {isTyping && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
          입력 중...
        </Typography>
      )}
    </Paper>
  );
};

export default MessageInput;