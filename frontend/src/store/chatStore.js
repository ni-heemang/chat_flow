import { create } from 'zustand';
import { chatService } from '../services/chatService';
import messageService from '../services/messageService';

const useChatStore = create((set, get) => ({
  rooms: [],
  currentRoom: null,
  messages: {},
  participants: {},
  typingUsers: {},
  loading: false,
  error: null,

  setRooms: (rooms) => set({ rooms }),
  
  setCurrentRoom: (room) => set({ currentRoom: room }),

  addMessage: (roomId, message) => set((state) => ({
    messages: {
      ...state.messages,
      [roomId]: [...(state.messages[roomId] || []), message]
    }
  })),

  setMessages: (roomId, messages) => set((state) => ({
    messages: {
      ...state.messages,
      [roomId]: messages
    }
  })),

  // 메시지 히스토리 설정 (히스토리는 시간 순서대로 정렬)
  setMessageHistory: (roomId, historyMessages) => set((state) => {
    // 중복 메시지 제거를 위해 ID 기반으로 필터링
    const existingMessages = state.messages[roomId] || [];
    const existingIds = new Set(existingMessages.map(msg => msg.id));
    
    // 히스토리에서 중복되지 않은 메시지만 필터링
    const newHistoryMessages = historyMessages.filter(msg => !existingIds.has(msg.id));
    
    return {
      messages: {
        ...state.messages,
        [roomId]: [...newHistoryMessages, ...existingMessages]
      }
    };
  }),

  // 이전 메시지들을 앞에 추가 (더 오래된 메시지들)
  prependMessages: (roomId, olderMessages) => set((state) => {
    const existingMessages = state.messages[roomId] || [];
    const existingIds = new Set(existingMessages.map(msg => msg.id));
    
    // 중복되지 않은 메시지만 필터링
    const newOlderMessages = olderMessages.filter(msg => !existingIds.has(msg.id));
    
    return {
      messages: {
        ...state.messages,
        [roomId]: [...newOlderMessages, ...existingMessages]
      }
    };
  }),

  setParticipants: (roomId, participants) => set((state) => ({
    participants: {
      ...state.participants,
      [roomId]: participants
    }
  })),

  fetchRoomMembers: async (roomId) => {
    try {
      const members = await chatService.getRoomMembers(roomId);
      // 멤버 정보를 participants 형태로 변환
      const participants = members.map(member => ({
        id: member.userId,
        username: member.username,
        name: member.name,
        isOnline: member.isOnline,
        joinedAt: member.joinedAt,
        lastSeen: member.lastSeen
      }));
      
      get().setParticipants(roomId, participants);
      return { success: true, data: participants };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  setTypingUsers: (roomId, typingUsers) => set((state) => ({
    typingUsers: {
      ...state.typingUsers,
      [roomId]: typingUsers
    }
  })),

  addTypingUser: (roomId, username) => set((state) => {
    const currentTyping = state.typingUsers[roomId] || [];
    if (!currentTyping.includes(username)) {
      return {
        typingUsers: {
          ...state.typingUsers,
          [roomId]: [...currentTyping, username]
        }
      };
    }
    return state;
  }),

  removeTypingUser: (roomId, username) => set((state) => {
    const currentTyping = state.typingUsers[roomId] || [];
    return {
      typingUsers: {
        ...state.typingUsers,
        [roomId]: currentTyping.filter(user => user !== username)
      }
    };
  }),

  fetchRooms: async (search = '', filter = 'all') => {
    set({ loading: true, error: null });
    try {
      const rooms = await chatService.getRooms(search, filter);
      set({ rooms, loading: false });
      return { success: true, data: rooms };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  fetchMyRooms: async () => {
    set({ loading: true, error: null });
    try {
      const rooms = await chatService.getMyRooms();
      set({ rooms, loading: false });
      return { success: true, data: rooms };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 사용자가 참여 중인 채팅방 목록 조회 (분석창용)
  fetchJoinedRooms: async () => {
    set({ loading: true, error: null });
    try {
      const rooms = await chatService.getJoinedRooms();
      set({ rooms, loading: false });
      return { success: true, data: rooms };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  createRoom: async (roomData) => {
    set({ loading: true, error: null });
    try {
      const newRoom = await chatService.createRoom(roomData);
      set((state) => ({
        rooms: [...state.rooms, newRoom],
        loading: false
      }));
      return { success: true, data: newRoom };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  fetchRoomMessages: async (roomId, page = 0, size = 20) => {
    try {
      const messagesData = await chatService.getRoomMessages(roomId, page, size);
      if (page === 0) {
        get().setMessages(roomId, messagesData.content || messagesData);
      } else {
        const existingMessages = get().messages[roomId] || [];
        const newMessages = messagesData.content || messagesData;
        get().setMessages(roomId, [...newMessages, ...existingMessages]);
      }
      return { success: true, data: messagesData };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  joinRoom: async (roomId) => {
    try {
      const result = await chatService.joinRoom(roomId);
      return { success: true, data: result };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  leaveRoom: async (roomId) => {
    try {
      const result = await chatService.leaveRoom(roomId);
      return { success: true, data: result };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  clearError: () => set({ error: null }),

  updateRoom: async (roomId, roomData) => {
    set({ loading: true, error: null });
    try {
      const updatedRoom = await chatService.updateRoom(roomId, roomData);
      set((state) => ({
        rooms: state.rooms.map(room => 
          room.id === roomId ? updatedRoom : room
        ),
        currentRoom: state.currentRoom?.id === roomId ? updatedRoom : state.currentRoom,
        loading: false
      }));
      return { success: true, data: updatedRoom };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  deleteRoom: async (roomId) => {
    set({ loading: true, error: null });
    try {
      await chatService.deleteRoom(roomId);
      set((state) => ({
        rooms: state.rooms.filter(room => room.id !== roomId),
        currentRoom: state.currentRoom?.id === roomId ? null : state.currentRoom,
        loading: false
      }));
      return { success: true };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  getStats: async () => {
    try {
      const stats = await chatService.getStats();
      return { success: true, data: stats };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 메시지 히스토리 관련 메서드들
  fetchRecentMessages: async (roomId, limit = 50) => {
    try {
      const result = await messageService.getRecentMessages(roomId, limit);
      if (result.success && result.data?.messages) {
        get().setMessageHistory(roomId, result.data.messages);
      }
      return result;
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  fetchMessageHistory: async (roomId, page = 0, size = 50) => {
    try {
      const result = await messageService.getMessageHistory(roomId, page, size);
      if (result.success && result.data?.messages) {
        if (page === 0) {
          get().setMessageHistory(roomId, result.data.messages);
        } else {
          get().prependMessages(roomId, result.data.messages);
        }
      }
      return result;
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  fetchMessageStats: async (roomId) => {
    try {
      const result = await messageService.getMessageStats(roomId);
      return result;
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  searchRoomMessages: async (roomId, keyword, page = 0, size = 20) => {
    try {
      const result = await messageService.searchMessages(roomId, keyword, page, size);
      return result;
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  reset: () => set({
    rooms: [],
    currentRoom: null,
    messages: {},
    participants: {},
    typingUsers: {},
    loading: false,
    error: null,
  }),
}));

export default useChatStore;