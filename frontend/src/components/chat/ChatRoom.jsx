import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  AppBar,
  Toolbar,
  IconButton,
  Chip,
  Alert,
  CircularProgress,
  Button,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Group as GroupIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import UserList from './UserList';
import useSocket from '../../hooks/useSocket';
import useChatStore from '../../store/chatStore';
import useUserStore from '../../store/userStore';
import { chatService } from '../../services/chatService';

const ChatRoom = () => {
  const navigate = useNavigate();
  const { roomId } = useParams();
  const { user } = useUserStore();
  const {
    currentRoom,
    messages,
    participants,
    typingUsers,
    setCurrentRoom,
    addMessage,
    addTypingUser,
    removeTypingUser,
    setMessageHistory,
    fetchRoomMessages,
    fetchMessageHistory,
    fetchRoomMembers,
    joinRoom,
    leaveRoom,
  } = useChatStore();

  const {
    isConnected,
    error: socketError,
    subscribe,
    subscribeToMessageHistory,
    connectToRoom: socketConnectToRoom,
    joinRoom: socketJoinRoom,
    leaveRoom: socketLeaveRoom,
    disconnectFromRoom: socketDisconnectFromRoom,
    sendChatMessage,
    sendTypingStatus,
  } = useSocket();

  const [loading, setLoading] = useState(true);
  const [showUserList, setShowUserList] = useState(false);
  const [isJoined, setIsJoined] = useState(false);
  const [joiningRoom, setJoiningRoom] = useState(false);

  const roomIdNum = parseInt(roomId, 10);
  const roomMessages = messages[roomIdNum] || [];
  const roomParticipants = participants[roomIdNum] || [];
  const roomTypingUsers = typingUsers[roomIdNum] || [];

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

    // 멤버 목록 실시간 업데이트 구독
    const membersSubscription = subscribe(`/topic/room/${roomIdNum}/members`, (membersUpdate) => {
      console.log('Received members update:', membersUpdate);
      // 멤버 목록 새로고침
      fetchRoomMembers(roomIdNum);
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
      membersSubscription?.unsubscribe();
      historySubscription?.unsubscribe();
    };
  }, [isConnected, roomIdNum, subscribe, subscribeToMessageHistory, addMessage, addTypingUser, removeTypingUser, setMessageHistory, user?.username, currentRoom, setCurrentRoom]);

  // 채팅방 입장 및 초기 데이터 로드
  useEffect(() => {
    const initializeRoom = async () => {
      if (!roomIdNum) return;

      setLoading(true);
      try {
        // API로 채팅방 상세 정보 로드 (멤버십 생성하지 않음)
        try {
          const roomDetails = await chatService.getRoomDetails(roomIdNum);
          setCurrentRoom(roomDetails);
          console.log('채팅방 정보 로드 완료:', roomDetails);
          
          // 해당 사용자가 이미 멤버인지 확인
          const membersResult = await fetchRoomMembers(roomIdNum);
          if (membersResult.success) {
            const isMember = membersResult.data.some(member => member.id === user?.id);
            setIsJoined(isMember);
            console.log('채팅방 멤버 상태:', isMember ? '참여 중' : '미참여');
          }
          
          // 비멤버도 최근 메시지 로드 (읽기 전용)
          const recentMessagesResult = await fetchMessageHistory(roomIdNum, 0, 20);
          if (recentMessagesResult.success) {
            console.log('최근 메시지 로드 완료:', recentMessagesResult.data?.messages?.length || 0, '개');
            console.log('메시지 데이터:', recentMessagesResult.data?.messages);
            // 스토어 상태 확인
            setTimeout(() => {
              console.log('스토어의 메시지 상태:', messages[roomIdNum]);
            }, 100);
          } else {
            console.error('메시지 로드 실패:', recentMessagesResult.error);
          }
        } catch (error) {
          console.error('채팅방 정보 로드 실패:', error);
        }

        // WebSocket만 사용 - 실시간으로 받는 메시지를 클라이언트에서 관리
        console.log('WebSocket 전용 메시지 관리 모드로 동작합니다.');

        // WebSocket으로 채팅방 연결 (멤버십 생성 안함)
        if (isConnected) {
          socketConnectToRoom(roomIdNum);
        }
        
        // 채팅방 멤버 목록 로드
        const membersResult = await fetchRoomMembers(roomIdNum);
        if (membersResult.success) {
          console.log('채팅방 멤버 목록 로드 성공:', membersResult.data);
        } else {
          console.error('채팅방 멤버 목록 로드 실패:', membersResult.error);
        }
      } catch (error) {
        console.error('Failed to initialize room:', error);
      } finally {
        setLoading(false);
      }
    };

    initializeRoom();

    // 컴포넌트 언마운트 시 WebSocket 연결 해제만
    return () => {
      if (isConnected && roomIdNum) {
        socketDisconnectFromRoom(roomIdNum);
      }
    };
  }, [roomIdNum, isConnected, joinRoom, fetchRoomMessages, socketJoinRoom, socketLeaveRoom, leaveRoom, setCurrentRoom]);

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
  
  // 채팅방 참여 버튼 클릭
  const handleJoinRoom = async () => {
    if (joiningRoom) return;
    
    setJoiningRoom(true);
    try {
      // REST API로 멤버십 생성
      const joinResult = await joinRoom(roomIdNum);
      if (joinResult.success) {
        // WebSocket으로 멤버십 참여 알림
        if (isConnected) {
          socketJoinRoom(roomIdNum);
        }
        
        setIsJoined(true);
        setCurrentRoom(joinResult.data.chatRoom || joinResult.data);
        
        // 멤버 목록 새로고침
        const membersResult = await fetchRoomMembers(roomIdNum);
        if (membersResult.success) {
          console.log('채팅방 참여 완료. 멤버 목록 업데이트:', membersResult.data);
        }
        
        console.log('채팅방 참여 성공!');
      } else {
        console.error('채팅방 참여 실패:', joinResult.error);
      }
    } catch (error) {
      console.error('채팅방 참여 중 오류:', error);
    } finally {
      setJoiningRoom(false);
    }
  };
  
  // 채팅방 퇴장 버튼 클릭
  const handleLeaveRoom = async () => {
    try {
      // REST API로 멤버십 제거
      const leaveResult = await leaveRoom(roomIdNum);
      if (leaveResult.success) {
        // WebSocket으로 멤버십 퇴장 알림
        if (isConnected) {
          socketLeaveRoom(roomIdNum);
        }
        
        setIsJoined(false);
        
        // 멤버 목록 새로고침
        await fetchRoomMembers(roomIdNum);
        
        console.log('채팅방 퇴장 완료');
      } else {
        console.error('채팅방 퇴장 실패:', leaveResult.error);
      }
    } catch (error) {
      console.error('채팅방 퇴장 중 오류:', error);
    }
  };

  const [messageHistoryPage, setMessageHistoryPage] = useState(0);
  const [hasMoreMessages, setHasMoreMessages] = useState(true);

  const handleLoadMoreMessages = async () => {
    if (!roomIdNum || !hasMoreMessages) return;

    try {
      const nextPage = messageHistoryPage + 1;
      const result = await fetchMessageHistory(roomIdNum, nextPage, 20);
      
      if (result.success) {
        const hasMore = result.data?.messages?.length === 20; // 20개 미만이면 더 이상 없음
        setHasMoreMessages(hasMore);
        setMessageHistoryPage(nextPage);
        console.log(`페이지 ${nextPage} 메시지 로드 완료. 더 있음: ${hasMore}`);
      }
    } catch (error) {
      console.error('Failed to load more messages:', error);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* 채팅방 헤더 */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <IconButton edge="start" onClick={handleBack} sx={{ mr: 2 }}>
            <ArrowBackIcon />
          </IconButton>
          
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h6">
              {currentRoom?.name || `채팅방 ${roomId}`}
            </Typography>
            {currentRoom?.description && (
              <Typography variant="caption" sx={{ color: 'white' }}>
                {currentRoom.description}
              </Typography>
            )}
          </Box>

          <Chip
            icon={<GroupIcon />}
            label={`${currentRoom?.currentParticipants || 0}명`}
            size="small"
            variant="outlined"
            sx={{ mr: 1 }}
          />

          {!isJoined ? (
            <Button
              variant="contained"
              color="primary"
              size="small"
              onClick={handleJoinRoom}
              disabled={joiningRoom}
              sx={{ mr: 1 }}
            >
              {joiningRoom ? '참여 중...' : '참여하기'}
            </Button>
          ) : (
            <Button
              variant="outlined"
              color="secondary"
              size="small"
              onClick={handleLeaveRoom}
              sx={{ mr: 1 }}
            >
              나가기
            </Button>
          )}

          <IconButton onClick={() => setShowUserList(!showUserList)}>
            <InfoIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      {/* 연결 상태 및 참여 상태 알림 */}
      {!isJoined && (
        <Alert severity="info" sx={{ mx: 2 }}>
          👀 채팅방을 구경하고 계시네요! 메시지를 보내려면 위의 "참여하기" 버튼을 눌러주세요.
        </Alert>
      )}
      
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

      <Box sx={{ display: 'flex', flexGrow: 1, overflow: 'hidden' }}>
        {/* 메시지 영역 */}
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
            disabled={!isConnected || !isJoined}
            placeholder={
              !isJoined 
                ? '채팅방에 참여해야 메시지를 보낼 수 있습니다. 위 버튼을 눌러 참여하세요!' 
                : isConnected 
                  ? '멤버로서 메시지를 입력하세요...' 
                  : '연결을 기다리는 중...'
            }
          />
        </Box>

        {/* 사용자 목록 (옵션) */}
        {showUserList && (
          <Paper
            elevation={2}
            sx={{
              width: 250,
              borderLeft: 1,
              borderColor: 'divider',
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <UserList participants={roomParticipants} />
          </Paper>
        )}
      </Box>
    </Box>
  );
};

export default ChatRoom;