import api from './api';

const messageService = {
  // 최근 메시지 조회
  getRecentMessages: async (roomId, limit = 50) => {
    try {
      const response = await api.get(`/messages/room/${roomId}/recent`, {
        params: { limit },
      });

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Recent messages fetch error:', error);
      return {
        success: false,
        error: error.response?.data?.message || '최근 메시지 조회에 실패했습니다.',
      };
    }
  },

  // 페이징된 메시지 히스토리 조회
  getMessageHistory: async (roomId, page = 0, size = 50) => {
    try {
      console.log('메시지 히스토리 API 호출:', `/messages/room/${roomId}`);
      
      const response = await api.get(`/messages/room/${roomId}`, {
        params: { page, size },
      });

      console.log('메시지 히스토리 응답:', response.data);
      
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Message history fetch error:', error);
      console.error('Error response:', error.response?.data);
      return {
        success: false,
        error: error.response?.data?.message || '메시지 히스토리 조회에 실패했습니다.',
      };
    }
  },

  // 메시지 통계 조회
  getMessageStats: async (roomId) => {
    try {
      const response = await api.get(`/messages/room/${roomId}/stats`);

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Message stats fetch error:', error);
      return {
        success: false,
        error: error.response?.data?.message || '메시지 통계 조회에 실패했습니다.',
      };
    }
  },

  // 메시지 검색 (새로운 엔드포인트)
  searchMessages: async (roomId, keyword, page = 0, size = 20) => {
    try {
      const response = await api.get(`/messages/room/${roomId}/search`, {
        params: { keyword, page, size },
      });

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Message search error:', error);
      return {
        success: false,
        error: error.response?.data?.message || '메시지 검색에 실패했습니다.',
      };
    }
  },
};

export default messageService;