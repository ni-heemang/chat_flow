import { create } from 'zustand';

const useNotificationStore = create((set, get) => ({
  notifications: [],
  soundEnabled: true,
  desktopEnabled: true,

  // 알림 추가
  addNotification: (notification) => {
    const id = Date.now() + Math.random();
    const newNotification = {
      id,
      timestamp: new Date(),
      read: false,
      ...notification,
    };

    set(state => ({
      notifications: [newNotification, ...state.notifications.slice(0, 49)] // 최대 50개까지만 보관
    }));

    // 사운드 알림
    if (get().soundEnabled && notification.type !== 'info') {
      try {
        // 브라우저 내장 알림음
        const audio = new Audio();
        audio.volume = 0.3;
        if (notification.type === 'error') {
          // 에러음 (높은 음)
          audio.src = 'data:audio/wav;base64,UklGRvIAAABXQVZFZm10IBAAAAABAAEARESAAA=';
        } else {
          // 일반 알림음 (낮은 음)
          audio.src = 'data:audio/wav;base64,UklGRvIAAABXQVZFZm10IBAAAAABAAEARESAAB=';
        }
        audio.play().catch(() => {
          // 음성 재생 실패 시 무시
        });
      } catch {
        // 오디오 재생 실패 무시
      }
    }

    // 데스크톱 알림
    if (get().desktopEnabled && 'Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(notification.title || '새 알림', {
          body: notification.message,
          icon: '/vite.svg',
          tag: notification.type,
        });
      } catch {
        // 데스크톱 알림 실패 무시
      }
    }

    return id;
  },

  // 알림 제거
  removeNotification: (id) => {
    set(state => ({
      notifications: state.notifications.filter(n => n.id !== id)
    }));
  },

  // 알림 읽음 처리
  markAsRead: (id) => {
    set(state => ({
      notifications: state.notifications.map(n => 
        n.id === id ? { ...n, read: true } : n
      )
    }));
  },

  // 모든 알림 읽음 처리
  markAllAsRead: () => {
    set(state => ({
      notifications: state.notifications.map(n => ({ ...n, read: true }))
    }));
  },

  // 모든 알림 삭제
  clearAllNotifications: () => {
    set({ notifications: [] });
  },

  // 설정 업데이트
  updateSettings: (settings) => {
    set(state => ({ ...state, ...settings }));
  },

  // 데스크톱 알림 권한 요청
  requestDesktopPermission: async () => {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }
    return false;
  },

  // 편의 메서드들
  success: (title, message) => {
    return get().addNotification({
      type: 'success',
      title,
      message,
      duration: 4000,
    });
  },

  error: (title, message) => {
    return get().addNotification({
      type: 'error',
      title,
      message,
      duration: 6000,
    });
  },

  warning: (title, message) => {
    return get().addNotification({
      type: 'warning',
      title,
      message,
      duration: 5000,
    });
  },

  info: (title, message) => {
    return get().addNotification({
      type: 'info',
      title,
      message,
      duration: 3000,
    });
  },

  // 새 메시지 알림
  newMessage: (roomName, senderName, messageContent) => {
    return get().addNotification({
      type: 'message',
      title: `새 메시지 - ${roomName}`,
      message: `${senderName}: ${messageContent.length > 50 ? messageContent.substring(0, 50) + '...' : messageContent}`,
      duration: 4000,
    });
  },

  // 분석 완료 알림
  analysisComplete: (roomName) => {
    return get().addNotification({
      type: 'analysis',
      title: '분석 완료',
      message: `${roomName}의 채팅 분석이 완료되었습니다.`,
      duration: 4000,
    });
  },
}));

export default useNotificationStore;