import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import useUserStore from '../store/userStore';

const useSocket = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);
  const stompClient = useRef(null);
  const { accessToken, user } = useUserStore();

  const connect = useCallback(() => {
    if (!accessToken || !user) {
      console.warn('No token or user available for WebSocket connection', { accessToken: !!accessToken, user: !!user });
      return;
    }

    console.log('Attempting WebSocket connection to: http://localhost:8080/ws');
    console.log('User:', user);
    console.log('Token available:', !!accessToken);

    try {
      const socket = new SockJS('http://localhost:8080/ws');
      stompClient.current = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          Authorization: `Bearer ${accessToken}`,
        },
        debug: (str) => {
          console.log('STOMP Debug:', str);
        },
        onConnect: (frame) => {
          console.log('✅ WebSocket Connected successfully:', frame);
          setIsConnected(true);
          setError(null);
        },
        onDisconnect: (frame) => {
          console.log('WebSocket Disconnected:', frame);
          setIsConnected(false);
        },
        onStompError: (frame) => {
          console.error('STOMP Error:', frame);
          setError(frame.body || 'WebSocket connection error');
          setIsConnected(false);
        },
        onWebSocketError: (error) => {
          console.error('WebSocket Error:', error);
          setError('WebSocket connection failed');
          setIsConnected(false);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.current.activate();
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      setError('Failed to initialize WebSocket connection');
    }
  }, [accessToken, user]);

  const disconnect = useCallback(() => {
    if (stompClient.current) {
      stompClient.current.deactivate();
      stompClient.current = null;
      setIsConnected(false);
    }
  }, []);

  const sendMessage = useCallback((destination, body) => {
    if (stompClient.current && isConnected) {
      try {
        stompClient.current.publish({
          destination,
          body: JSON.stringify(body),
        });
      } catch (error) {
        console.error('Failed to send message:', error);
        setError('Failed to send message');
      }
    } else {
      console.warn('WebSocket not connected. Cannot send message.');
    }
  }, [isConnected]);

  const subscribe = useCallback((destination, callback) => {
    if (stompClient.current && isConnected) {
      try {
        const subscription = stompClient.current.subscribe(destination, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            callback(parsedMessage);
          } catch (error) {
            console.error('Failed to parse message:', error);
            callback(message.body);
          }
        });
        return subscription;
      } catch (error) {
        console.error('Failed to subscribe:', error);
        setError('Failed to subscribe to topic');
      }
    } else {
      console.warn('WebSocket not connected. Cannot subscribe.');
    }
    return null;
  }, [isConnected]);

  const connectToRoom = useCallback((roomId) => {
    sendMessage('/app/connect-room', { roomId });
    console.log('Room WebSocket connection sent:', roomId);
  }, [sendMessage]);
  
  const joinRoom = useCallback((roomId) => {
    sendMessage('/app/join-room', { roomId });
    console.log('Room membership join sent:', roomId);
  }, [sendMessage]);

  const leaveRoom = useCallback((roomId) => {
    sendMessage('/app/leave-room', { roomId });
    console.log('Room membership leave sent:', roomId);
  }, [sendMessage]);
  
  const disconnectFromRoom = useCallback((roomId) => {
    sendMessage('/app/disconnect-room', { roomId });
    console.log('Room WebSocket disconnect sent:', roomId);
  }, [sendMessage]);

  const sendChatMessage = useCallback((roomId, content, messageType = 'TEXT') => {
    console.log('Attempting to send message:', { roomId, content, messageType });
    console.log('WebSocket connected:', isConnected);
    
    if (!isConnected) {
      console.error('WebSocket not connected. Cannot send message.');
      setError('WebSocket 연결이 끊어졌습니다. 다시 연결을 시도해주세요.');
      return;
    }
    
    sendMessage('/app/send-message', {
      roomId,
      content,
      messageType,
    });
    
    console.log('Message sent to: /app/send-message with payload:', { roomId, content, messageType });
  }, [sendMessage, isConnected]);

  const sendTypingStatus = useCallback((roomId, isTyping) => {
    sendMessage('/app/typing', {
      roomId,
      isTyping,
    });
    console.log('Typing status sent:', { roomId, isTyping });
  }, [sendMessage]);

  useEffect(() => {
    if (accessToken && user) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [accessToken, user, connect, disconnect]);

  // 분석 데이터 구독
  const subscribeToAnalysis = useCallback((roomId, callback) => {
    if (stompClient.current && isConnected) {
      try {
        const subscription = stompClient.current.subscribe(`/topic/analysis/${roomId}`, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            console.log('Received analysis data:', parsedMessage);
            callback(parsedMessage);
          } catch (error) {
            console.error('Failed to parse analysis message:', error);
          }
        });
        console.log('Subscribed to analysis updates:', `/topic/analysis/${roomId}`);
        return subscription;
      } catch (error) {
        console.error('Failed to subscribe to analysis:', error);
        setError('Failed to subscribe to analysis updates');
      }
    } else {
      console.warn('WebSocket not connected. Cannot subscribe to analysis.');
    }
    return null;
  }, [isConnected]);

  // 메시지 히스토리 구독
  const subscribeToMessageHistory = useCallback((roomId, callback) => {
    if (stompClient.current && isConnected) {
      try {
        const subscription = stompClient.current.subscribe(`/user/queue/room/${roomId}/history`, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            console.log('Received message history:', parsedMessage);
            callback(parsedMessage);
          } catch (error) {
            console.error('Failed to parse message history:', error);
          }
        });
        console.log('Subscribed to message history:', `/user/queue/room/${roomId}/history`);
        return subscription;
      } catch (error) {
        console.error('Failed to subscribe to message history:', error);
        setError('Failed to subscribe to message history');
      }
    } else {
      console.warn('WebSocket not connected. Cannot subscribe to message history.');
    }
    return null;
  }, [isConnected]);

  return {
    isConnected,
    error,
    connect,
    disconnect,
    sendMessage,
    subscribe,
    subscribeToAnalysis,
    subscribeToMessageHistory,
    connectToRoom,
    joinRoom,
    leaveRoom,
    disconnectFromRoom,
    sendChatMessage,
    sendTypingStatus,
  };
};

export default useSocket;