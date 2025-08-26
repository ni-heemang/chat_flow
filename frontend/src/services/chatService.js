import api from './api';

export const chatService = {
  // 채팅방 목록 조회
  async getRooms(search = '', filter = 'all') {
    try {
      const params = {};
      if (search) params.search = search;
      if (filter && filter !== 'all') params.filter = filter;

      const response = await api.get('/chatrooms', { params });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 목록을 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 내 채팅방 목록 조회
  async getMyRooms() {
    try {
      const response = await api.get('/chatrooms/my');
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '내 채팅방 목록을 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 상세 조회
  async getRoomDetails(roomId) {
    try {
      const response = await api.get(`/chatrooms/${roomId}`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 정보를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 생성
  async createRoom(roomData) {
    try {
      const response = await api.post('/chatrooms', {
        name: roomData.name,
        description: roomData.description || null,
        maxParticipants: roomData.maxParticipants || 50,
        isPublic: roomData.isPublic !== false, // 기본값 true
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 생성 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 수정
  async updateRoom(roomId, roomData) {
    try {
      const response = await api.put(`/chatrooms/${roomId}`, {
        name: roomData.name,
        description: roomData.description || null,
        maxParticipants: roomData.maxParticipants || 50,
        isPublic: roomData.isPublic !== false,
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 수정 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 삭제
  async deleteRoom(roomId) {
    try {
      const response = await api.delete(`/chatrooms/${roomId}`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 삭제 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 입장
  async joinRoom(roomId) {
    try {
      const response = await api.post(`/chatrooms/${roomId}/join`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 입장 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 퇴장
  async leaveRoom(roomId) {
    try {
      const response = await api.post(`/chatrooms/${roomId}/leave`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 퇴장 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 멤버 목록 조회
  async getRoomMembers(roomId) {
    try {
      const response = await api.get(`/chatrooms/${roomId}/members`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 멤버 목록을 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 채팅방 통계 조회
  async getStats() {
    try {
      const response = await api.get('/chatrooms/stats');
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '채팅방 통계를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 사용자가 참여 중인 채팅방 목록 조회 (분석창용)
  async getJoinedRooms() {
    try {
      const response = await api.get('/analysis/user/joined-rooms');
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '참여 중인 채팅방 목록을 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 메시지 조회 - BE API 스펙에 맞춤
  async getRoomMessages(roomId, page = 0, size = 20) {
    try {
      const response = await api.get(`/chatrooms/${roomId}/messages`, {
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '메시지를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },
};

export default chatService;