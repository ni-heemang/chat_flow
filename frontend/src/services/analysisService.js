import api from './api';

export const analysisService = {
  // 종합 분석 결과 조회
  async getRoomAnalysisSummary(roomId) {
    try {
      const response = await api.get(`/analysis/rooms/${roomId}/summary`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '종합 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 키워드 분석 결과
  async getKeywordAnalysis(roomId) {
    try {
      const response = await api.get(`/analysis/rooms/${roomId}/keywords`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '키워드 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 참여도 분석 결과
  async getParticipationAnalysis(roomId) {
    try {
      const response = await api.get(`/analysis/rooms/${roomId}/participation`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '참여도 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 시간대별 활동 분석
  async getHourlyAnalysis(roomId) {
    try {
      const response = await api.get(`/analysis/rooms/${roomId}/hourly`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '시간대별 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 분석 통계 요약
  async getAnalysisStats(roomId) {
    try {
      const response = await api.get(`/analysis/rooms/${roomId}/stats`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '분석 통계를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 분석 결과 이력 (페이징)
  async getAnalysisHistory(roomId, page = 0, size = 20, analysisType = null) {
    try {
      const params = { page, size };
      if (analysisType) {
        params.analysisType = analysisType;
      }

      const response = await api.get(`/analysis/rooms/${roomId}/history`, {
        params
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '분석 이력을 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 기간별 분석 결과
  async getPeriodAnalysis(roomId, startDate, endDate, analysisType = null) {
    try {
      const params = { startDate, endDate };
      if (analysisType) {
        params.analysisType = analysisType;
      }

      const response = await api.get(`/analysis/rooms/${roomId}/period`, {
        params
      });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '기간별 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 캐시된 종합 분석 결과
  async getCachedAnalysis(roomId) {
    try {
      const response = await api.get(`/analysis/rooms/${roomId}`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 분석 데이터 재구축
  async rebuildAnalysisData(roomId) {
    try {
      const response = await api.post(`/analysis/rooms/${roomId}/rebuild`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '분석 데이터 재구축 중 오류가 발생했습니다.'
      );
    }
  },

  // 모든 채팅방 분석 데이터 재구축
  async rebuildAllAnalysisData() {
    try {
      const response = await api.post('/analysis/rebuild-all');
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '전체 분석 데이터 재구축 중 오류가 발생했습니다.'
      );
    }
  },

  // 분석 데이터 초기화 (개발/테스트용)
  async clearAnalysisData(roomId) {
    try {
      const response = await api.delete(`/analysis/rooms/${roomId}/clear`);
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '분석 데이터 초기화 중 오류가 발생했습니다.'
      );
    }
  },

  // 기간별 분석 API 메소드들
  
  // 기간별 종합 분석 결과 조회
  async getRoomAnalysisSummaryByPeriod(roomId, days) {
    try {
      const params = days !== null ? { days } : {};
      const response = await api.get(`/analysis/rooms/${roomId}/summary/period`, { params });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '기간별 종합 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 기간별 키워드 분석 결과
  async getKeywordAnalysisByPeriod(roomId, days) {
    try {
      const params = days !== null ? { days } : {};
      const response = await api.get(`/analysis/rooms/${roomId}/keywords/period`, { params });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '기간별 키워드 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 기간별 참여도 분석 결과
  async getParticipationAnalysisByPeriod(roomId, days) {
    try {
      const params = days !== null ? { days } : {};
      const response = await api.get(`/analysis/rooms/${roomId}/participation/period`, { params });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '기간별 참여도 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },

  // 기간별 시간대별 활동 분석
  async getHourlyAnalysisByPeriod(roomId, days) {
    try {
      const params = days !== null ? { days } : {};
      const response = await api.get(`/analysis/rooms/${roomId}/hourly/period`, { params });
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || '기간별 시간대별 분석 결과를 불러오는 중 오류가 발생했습니다.'
      );
    }
  },
};

export default analysisService;