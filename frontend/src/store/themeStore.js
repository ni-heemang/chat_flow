import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useThemeStore = create(
  persist(
    (set, get) => ({
      // 테마 설정
      mode: 'light', // 'light' | 'dark' | 'system'
      
      // 테마 변경
      setMode: (mode) => {
        set({ mode });
        // 테마 변경 시 즉시 이벤트 발생
        window.dispatchEvent(new Event('theme-change'));
      },
      
      // 시스템 테마 확인
      getSystemTheme: () => {
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      },
      
      // 실제 적용될 테마 모드 계산
      getEffectiveMode: () => {
        const { mode } = get();
        return mode === 'system' ? get().getSystemTheme() : mode;
      },
      
      // 다크모드 여부
      isDarkMode: () => {
        return get().getEffectiveMode() === 'dark';
      },
      
      // 테마 토글
      toggleMode: () => {
        const { mode } = get();
        const newMode = mode === 'light' ? 'dark' : 'light';
        set({ mode: newMode });
      },
    }),
    {
      name: 'flowchat-theme-settings',
      partialize: (state) => ({
        mode: state.mode,
      }),
    }
  )
);

export default useThemeStore;