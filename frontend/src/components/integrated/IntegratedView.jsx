import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Grid,
  Paper,
  Typography,
  AppBar,
  Toolbar,
  IconButton,
  Chip,
  Alert,
  CircularProgress,
  Divider,
  Tooltip,
  FormControl,
  Select,
  MenuItem,
  InputLabel,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Group as GroupIcon,
  Fullscreen as FullscreenIcon,
  FullscreenExit as FullscreenExitIcon,
  SwapHoriz as SwapHorizIcon,
} from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import MessageList from '../chat/MessageList';
import MessageInput from '../chat/MessageInput';
import UserList from '../chat/UserList';
import AnalysisPanel from '../analysis/AnalysisPanel';
import useSocket from '../../hooks/useSocket';
import useChatStore from '../../store/chatStore';
import useUserStore from '../../store/userStore';

const IntegratedView = () => {
  const navigate = useNavigate();
  const { roomId } = useParams();
  const { user } = useUserStore();
  const {
    currentRoom,
    messages,
    participants,
    typingUsers,
    rooms,
    setCurrentRoom,
    addMessage,
    addTypingUser,
    removeTypingUser,
    setMessageHistory,
    fetchMessageHistory,
    joinRoom,
    leaveRoom,
    fetchRooms,
  } = useChatStore();

  const {
    isConnected,
    error: socketError,
    subscribe,
    subscribeToMessageHistory,
    joinRoom: socketJoinRoom,
    leaveRoom: socketLeaveRoom,
    sendChatMessage,
    sendTypingStatus,
  } = useSocket();

  const [loading, setLoading] = useState(true);
  const [showUserList, setShowUserList] = useState(false);
  const [chatFullscreen, setChatFullscreen] = useState(false);
  const [analysisFullscreen, setAnalysisFullscreen] = useState(false);
  const [selectedRoomId, setSelectedRoomId] = useState(roomId || '');
  const [chatWidth, setChatWidth] = useState(6); // Grid 크기 (12 중 6)

  const roomIdNum = parseInt(selectedRoomId, 10);
  const roomMessages = messages[roomIdNum] || [];
  const roomParticipants = participants[roomIdNum] || [];
  const roomTypingUsers = typingUsers[roomIdNum] || [];

  // 채팅방 목록 로드
  useEffect(() => {
    fetchRooms();
  }, [fetchRooms]);

  // 기본 채팅방 선택
  useEffect(() => {
    if (rooms.length > 0 && !selectedRoomId) {
      setSelectedRoomId(rooms[0].id.toString());
    }
  }, [rooms, selectedRoomId]);

  // 메시지 수신 구독
  useEffect(() => {
    if (!isConnected || !roomIdNum) return;

    const messageSubscription = subscribe(`/topic/room/${roomIdNum}`, (message) => {
      console.log('Received message:', message);
      addMessage(roomIdNum, message);
    });

    const typingSubscription = subscribe(`/topic/room/${roomIdNum}/typing`, (typingData) => {
      console.log('Received typing:', typingData);
      if (typingData.username !== user?.username) {
        if (typingData.isTyping) {
          addTypingUser(roomIdNum, typingData.username);
        } else {
          removeTypingUser(roomIdNum, typingData.username);
        }
      }
    });

    const roomInfoSubscription = subscribe(`/topic/room/${roomIdNum}/info`, (roomInfo) => {
      console.log('Received room info:', roomInfo);
      if (roomInfo.currentParticipants !== undefined) {
        setCurrentRoom({
          ...currentRoom,
          currentParticipants: roomInfo.currentParticipants,
        });
      }
    });

    // 메시지 히스토리 구독
    const historySubscription = subscribeToMessageHistory(roomIdNum, (historyData) => {
      console.log('Received message history:', historyData);
      if (historyData.messages && historyData.messages.length > 0) {
        setMessageHistory(roomIdNum, historyData.messages);
      }
    });

    return () => {
      messageSubscription?.unsubscribe();
      typingSubscription?.unsubscribe();
      roomInfoSubscription?.unsubscribe();
      historySubscription?.unsubscribe();
    };
  }, [isConnected, roomIdNum, subscribe, subscribeToMessageHistory, addMessage, addTypingUser, removeTypingUser, setMessageHistory, user?.username, currentRoom, setCurrentRoom]);

  // 채팅방 입장 및 초기 데이터 로드
  useEffect(() => {
    const initializeRoom = async () => {
      if (!roomIdNum) return;

      setLoading(true);
      try {
        // API로 채팅방 입장
        const joinResult = await joinRoom(roomIdNum);
        if (joinResult.success) {
          setCurrentRoom(joinResult.data);
        }

        // 웹소켓으로 채팅방 입장
        if (isConnected) {
          socketJoinRoom(roomIdNum);
        }
      } catch (error) {
        console.error('Failed to initialize room:', error);
      } finally {
        setLoading(false);
      }
    };

    initializeRoom();

    // 컴포넌트 언마운트 시 채팅방 퇴장
    return () => {
      if (isConnected && roomIdNum) {
        socketLeaveRoom(roomIdNum);
      }
      leaveRoom(roomIdNum);
    };
  }, [roomIdNum, isConnected, joinRoom, socketJoinRoom, socketLeaveRoom, leaveRoom, setCurrentRoom]);

  const handleSendMessage = useCallback((content) => {
    if (isConnected && roomIdNum) {
      sendChatMessage(roomIdNum, content);
    }
  }, [isConnected, roomIdNum, sendChatMessage]);

  const handleTyping = useCallback((isTyping) => {
    if (isConnected && roomIdNum) {
      sendTypingStatus(roomIdNum, isTyping);
    }
  }, [isConnected, roomIdNum, sendTypingStatus]);

  const handleBack = () => {
    navigate('/rooms');
  };

  const handleRoomChange = (event) => {
    const newRoomId = event.target.value;
    setSelectedRoomId(newRoomId);
    navigate(`/integrated/${newRoomId}`);
  };

  const [messageHistoryPage, setMessageHistoryPage] = useState(0);
  const [hasMoreMessages, setHasMoreMessages] = useState(true);

  const handleLoadMoreMessages = async () => {
    if (!roomIdNum || !hasMoreMessages) return;

    try {
      const nextPage = messageHistoryPage + 1;
      const result = await fetchMessageHistory(roomIdNum, nextPage, 20);
      
      if (result.success) {
        const hasMore = result.data?.content?.length === 20; // 20개 미만이면 더 이상 없음
        setHasMoreMessages(hasMore);
        setMessageHistoryPage(nextPage);
        console.log(`페이지 ${nextPage} 메시지 로드 완료. 더 있음: ${hasMore}`);
      }
    } catch (error) {
      console.error('Failed to load more messages:', error);
    }
  };

  const handleChatFullscreen = () => {
    setChatFullscreen(!chatFullscreen);
    setAnalysisFullscreen(false);
  };

  const handleAnalysisFullscreen = () => {
    setAnalysisFullscreen(!analysisFullscreen);
    setChatFullscreen(false);
  };

  const handleSwapLayout = () => {
    setChatWidth(chatWidth === 6 ? 8 : chatWidth === 8 ? 4 : 6);
  };

  const selectedRoom = rooms.find(room => room.id.toString() === selectedRoomId);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  // 풀스크린 모드 렌더링
  if (chatFullscreen) {
    return (
      <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
        <AppBar position="static" color="default" elevation={1}>
          <Toolbar>
            <IconButton edge="start" onClick={handleBack} sx={{ mr: 2 }}>
              <ArrowBackIcon />
            </IconButton>
            
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="h6">
                {selectedRoom?.name || `채팅방 ${selectedRoomId}`}
              </Typography>
            </Box>

            <IconButton onClick={handleChatFullscreen}>
              <FullscreenExitIcon />
            </IconButton>
          </Toolbar>
        </AppBar>

        {!isConnected && (
          <Alert severity="warning">
            실시간 연결이 끊어졌습니다. 재연결을 시도하고 있습니다...
          </Alert>
        )}

        <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
          <MessageList
            messages={roomMessages}
            typingUsers={roomTypingUsers}
            currentUserId={user?.id}
            onLoadMore={handleLoadMoreMessages}
            hasMore={hasMoreMessages}
          />
          
          <MessageInput
            onSendMessage={handleSendMessage}
            onTyping={handleTyping}
            disabled={!isConnected}
            placeholder={
              isConnected 
                ? '메시지를 입력하세요...' 
                : '연결을 기다리는 중...'
            }
          />
        </Box>
      </Box>
    );
  }

  if (analysisFullscreen) {
    return (
      <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
        <AppBar position="static" color="default" elevation={1}>
          <Toolbar>
            <IconButton edge="start" onClick={handleBack} sx={{ mr: 2 }}>
              <ArrowBackIcon />
            </IconButton>
            
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="h6">
                분석 대시보드 - {selectedRoom?.name || `채팅방 ${selectedRoomId}`}
              </Typography>
            </Box>

            <IconButton onClick={handleAnalysisFullscreen}>
              <FullscreenExitIcon />
            </IconButton>
          </Toolbar>
        </AppBar>

        <Box sx={{ flexGrow: 1, p: 2, overflow: 'auto' }}>
          <AnalysisPanel roomId={roomIdNum} />
        </Box>
      </Box>
    );
  }

  // 일반 통합 뷰 렌더링
  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* 헤더 */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <IconButton edge="start" onClick={handleBack} sx={{ mr: 2 }}>
            <ArrowBackIcon />
          </IconButton>
          
          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center', gap: 2 }}>
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>채팅방 선택</InputLabel>
              <Select
                value={selectedRoomId}
                label="채팅방 선택"
                onChange={handleRoomChange}
              >
                {rooms.map((room) => (
                  <MenuItem key={room.id} value={room.id.toString()}>
                    {room.name} ({room.currentParticipants || 0}명)
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {selectedRoom && (
              <Chip
                icon={<GroupIcon />}
                label={`${selectedRoom.currentParticipants || 0}명`}
                size="small"
                variant="outlined"
              />
            )}
          </Box>

          <Box sx={{ display: 'flex', gap: 1 }}>
            <Tooltip title="레이아웃 변경">
              <IconButton onClick={handleSwapLayout}>
                <SwapHorizIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="채팅 전체화면">
              <IconButton onClick={handleChatFullscreen}>
                <FullscreenIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="분석 전체화면">
              <IconButton onClick={handleAnalysisFullscreen}>
                <FullscreenIcon />
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </AppBar>

      {/* 연결 상태 알림 */}
      {!isConnected && (
        <Alert severity="warning">
          실시간 연결이 끊어졌습니다. 재연결을 시도하고 있습니다...
        </Alert>
      )}

      {socketError && (
        <Alert severity="error">
          연결 오류: {socketError}
        </Alert>
      )}

      {/* 메인 콘텐츠 */}
      <Box sx={{ flexGrow: 1, display: 'flex', overflow: 'hidden' }}>
        <Grid container sx={{ height: '100%' }}>
          {/* 채팅 영역 */}
          <Grid item xs={chatWidth} sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Paper elevation={1} sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6">
                  채팅
                </Typography>
                <IconButton size="small" onClick={() => setShowUserList(!showUserList)}>
                  <GroupIcon />
                </IconButton>
              </Box>
              
              <Box sx={{ display: 'flex', flexGrow: 1, overflow: 'hidden' }}>
                <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
                  <MessageList
                    messages={roomMessages}
                    typingUsers={roomTypingUsers}
                    currentUserId={user?.id}
                    onLoadMore={handleLoadMoreMessages}
                    hasMore={false}
                  />
                  
                  <MessageInput
                    onSendMessage={handleSendMessage}
                    onTyping={handleTyping}
                    disabled={!isConnected}
                    placeholder={
                      isConnected 
                        ? '메시지를 입력하세요...' 
                        : '연결을 기다리는 중...'
                    }
                  />
                </Box>

                {/* 사용자 목록 (옵션) */}
                {showUserList && (
                  <>
                    <Divider orientation="vertical" flexItem />
                    <Box sx={{ width: 200, overflow: 'auto' }}>
                      <UserList participants={roomParticipants} />
                    </Box>
                  </>
                )}
              </Box>
            </Paper>
          </Grid>

          <Divider orientation="vertical" flexItem />

          {/* 분석 영역 */}
          <Grid item xs={12 - chatWidth} sx={{ height: '100%', overflow: 'auto' }}>
            <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Typography variant="h6">
                  실시간 분석
                </Typography>
              </Box>
              
              <Box sx={{ flexGrow: 1, p: 1, overflow: 'auto' }}>
                <AnalysisPanel roomId={roomIdNum} compact />
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default IntegratedView;