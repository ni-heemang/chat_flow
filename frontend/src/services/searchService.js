import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const searchService = {
  // 메시지 검색
  searchMessages: async (roomId, keyword, page = 0, size = 20) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        return { success: false, error: '로그인이 필요합니다.' };
      }

      const response = await axios.get(
        `${API_BASE_URL}/rooms/${roomId}/search`,
        {
          params: { keyword, page, size },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

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

  // 전체 채팅방에서 메시지 검색
  searchAllMessages: async (keyword, page = 0, size = 20) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        return { success: false, error: '로그인이 필요합니다.' };
      }

      const response = await axios.get(
        `${API_BASE_URL}/search/messages`,
        {
          params: { keyword, page, size },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Global message search error:', error);
      return {
        success: false,
        error: error.response?.data?.message || '전체 메시지 검색에 실패했습니다.',
      };
    }
  },

  // 사용자별 메시지 검색
  searchMessagesByUser: async (roomId, username, page = 0, size = 20) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        return { success: false, error: '로그인이 필요합니다.' };
      }

      const response = await axios.get(
        `${API_BASE_URL}/rooms/${roomId}/search/user`,
        {
          params: { username, page, size },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('User message search error:', error);
      return {
        success: false,
        error: error.response?.data?.message || '사용자별 메시지 검색에 실패했습니다.',
      };
    }
  },

  // 날짜 범위로 메시지 검색
  searchMessagesByDate: async (roomId, startDate, endDate, page = 0, size = 20) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        return { success: false, error: '로그인이 필요합니다.' };
      }

      const response = await axios.get(
        `${API_BASE_URL}/rooms/${roomId}/search/date`,
        {
          params: { startDate, endDate, page, size },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Date range message search error:', error);
      return {
        success: false,
        error: error.response?.data?.message || '날짜별 메시지 검색에 실패했습니다.',
      };
    }
  },
};

export default searchService;