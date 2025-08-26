import { create } from 'zustand';
import { analysisService } from '../services/analysisService';

const useAnalysisStore = create((set, get) => ({
  // 분석 데이터 상태
  keywordData: null,
  participationData: null,
  hourlyActivityData: null,
  analysisStats: null,
  analysisHistory: [],
  
  // UI 상태
  loading: false,
  error: null,
  lastUpdated: null,

  // 실시간 데이터 플래그
  hasRealtimeData: false,

  // 종합 분석 결과 가져오기
  fetchAnalysisSummary: async (roomId) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getRoomAnalysisSummary(roomId);
      
      set({
        keywordData: data.keywords,
        participationData: data.participation,
        hourlyActivityData: data.hourlyActivity,
        loading: false,
        lastUpdated: new Date().toISOString(),
        hasRealtimeData: true,
      });
      
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 키워드 분석 데이터 가져오기
  fetchKeywordAnalysis: async (roomId) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getKeywordAnalysis(roomId);
      set({ 
        keywordData: data,
        loading: false,
        lastUpdated: new Date().toISOString(),
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 참여도 분석 데이터 가져오기
  fetchParticipationAnalysis: async (roomId) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getParticipationAnalysis(roomId);
      set({ 
        participationData: data,
        loading: false,
        lastUpdated: new Date().toISOString(),
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 시간대별 활동 분석 데이터 가져오기
  fetchHourlyAnalysis: async (roomId) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getHourlyAnalysis(roomId);
      set({ 
        hourlyActivityData: data,
        loading: false,
        lastUpdated: new Date().toISOString(),
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 분석 통계 가져오기
  fetchAnalysisStats: async (roomId) => {
    try {
      const data = await analysisService.getAnalysisStats(roomId);
      set({ 
        analysisStats: data,
        hasRealtimeData: data.hasRealtimeData,
      });
      return { success: true, data };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 분석 이력 가져오기
  fetchAnalysisHistory: async (roomId, page = 0, size = 20, analysisType = null) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getAnalysisHistory(roomId, page, size, analysisType);
      
      if (page === 0) {
        set({ analysisHistory: data.content });
      } else {
        // 페이지네이션 - 기존 데이터에 추가
        set(state => ({
          analysisHistory: [...state.analysisHistory, ...data.content]
        }));
      }
      
      set({ loading: false });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 기간별 분석 결과 가져오기
  fetchPeriodAnalysis: async (roomId, startDate, endDate, analysisType = null) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getPeriodAnalysis(roomId, startDate, endDate, analysisType);
      set({ 
        analysisHistory: data,
        loading: false,
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 실시간 키워드 데이터 업데이트
  updateKeywordData: (keywordUpdate) => {
    set({ 
      keywordData: {
        roomId: keywordUpdate.roomId,
        topKeywords: keywordUpdate.keywords.labels.map((label, index) => ({
          keyword: label,
          count: keywordUpdate.keywords.datasets[0].data[index]
        })),
        totalKeywords: keywordUpdate.keywords.datasets[0].data.reduce((sum, count) => sum + count, 0),
        lastUpdated: keywordUpdate.timestamp,
      },
      lastUpdated: keywordUpdate.timestamp,
    });
  },

  // 실시간 참여도 데이터 업데이트
  updateParticipationData: (participationUpdate) => {
    set({ 
      participationData: {
        roomId: participationUpdate.roomId,
        userParticipation: participationUpdate.participation.labels.map((label, index) => ({
          username: label, // 실제로는 nickname이 들어있음
          messageCount: participationUpdate.participation.datasets[0].data[index]
        })),
        totalUsers: participationUpdate.participation.labels.length,
        lastUpdated: participationUpdate.timestamp,
      },
      lastUpdated: participationUpdate.timestamp,
    });
  },

  // 실시간 시간대별 활동 데이터 업데이트
  updateHourlyActivityData: (hourlyUpdate) => {
    set({ 
      hourlyActivityData: {
        roomId: hourlyUpdate.roomId,
        hourlyActivity: hourlyUpdate.hourlyActivity.labels.map((label, index) => ({
          hour: parseInt(label),
          messageCount: hourlyUpdate.hourlyActivity.datasets[0].data[index]
        })),
        lastUpdated: hourlyUpdate.timestamp,
      },
      lastUpdated: hourlyUpdate.timestamp,
    });
  },

  // 에러 클리어
  clearError: () => set({ error: null }),

  // 분석 데이터 초기화
  clearAnalysisData: () => set({
    keywordData: null,
    participationData: null,
    hourlyActivityData: null,
    analysisStats: null,
    analysisHistory: [],
    loading: false,
    error: null,
    lastUpdated: null,
    hasRealtimeData: false,
  }),

  // 분석 데이터 재구축
  rebuildServerAnalysisData: async (roomId) => {
    set({ loading: true, error: null });
    try {
      const result = await analysisService.rebuildAnalysisData(roomId);
      set({ loading: false });
      return { success: true, data: result };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 모든 채팅방 분석 데이터 재구축
  rebuildAllAnalysisData: async () => {
    set({ loading: true, error: null });
    try {
      const result = await analysisService.rebuildAllAnalysisData();
      set({ loading: false });
      return { success: true, data: result };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 개발/테스트용 데이터 클리어
  clearServerAnalysisData: async (roomId) => {
    try {
      const result = await analysisService.clearAnalysisData(roomId);
      get().clearAnalysisData();
      return { success: true, data: result };
    } catch (error) {
      set({ error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 기간별 분석 메소드들

  // 기간별 종합 분석 결과 가져오기
  fetchAnalysisSummaryByPeriod: async (roomId, days) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getRoomAnalysisSummaryByPeriod(roomId, days);
      
      set({
        keywordData: data.keywords,
        participationData: data.participation,
        hourlyActivityData: data.hourlyActivity,
        loading: false,
        lastUpdated: new Date().toISOString(),
        hasRealtimeData: true,
      });
      
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 기간별 키워드 분석 데이터 가져오기
  fetchKeywordAnalysisByPeriod: async (roomId, days) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getKeywordAnalysisByPeriod(roomId, days);
      set({ 
        keywordData: data,
        loading: false,
        lastUpdated: new Date().toISOString(),
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 기간별 참여도 분석 데이터 가져오기
  fetchParticipationAnalysisByPeriod: async (roomId, days) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getParticipationAnalysisByPeriod(roomId, days);
      set({ 
        participationData: data,
        loading: false,
        lastUpdated: new Date().toISOString(),
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },

  // 기간별 시간대별 활동 분석 데이터 가져오기
  fetchHourlyAnalysisByPeriod: async (roomId, days) => {
    set({ loading: true, error: null });
    try {
      const data = await analysisService.getHourlyAnalysisByPeriod(roomId, days);
      set({ 
        hourlyActivityData: data,
        loading: false,
        lastUpdated: new Date().toISOString(),
      });
      return { success: true, data };
    } catch (error) {
      set({ loading: false, error: error.message });
      return { success: false, error: error.message };
    }
  },
}));

export default useAnalysisStore;