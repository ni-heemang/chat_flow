# 채팅 서비스 + 분석기 프론트엔드 기술 스택 (간소화)

## 1. 핵심 프론트엔드 스택

### 1.1 프레임워크 및 빌드 도구

- **React 18+**: 메인 프론트엔드 프레임워크
- **Vite**: 빌드 도구 (빠른 개발 환경)
- **TypeScript**: 타입 안전성 (선택사항, 필요시 추가)

### 1.2 상태 관리

- **Zustand**: 간단한 상태 관리 라이브러리 (Redux보다 학습 곡선이 낮음)

### 1.3 UI 라이브러리

- **Material-UI (MUI)**: React 컴포넌트 라이브러리
- **@mui/icons-material**: 아이콘 세트

### 1.4 실시간 통신 (채팅 기능)

- **Socket.IO Client**: 실시간 양방향 통신
- **WebSocket API**: 브라우저 기본 WebSocket (백업 옵션)

### 1.5 차트 및 시각화 (분석 기능)

- **Chart.js** + **react-chartjs-2**: 간단하고 직관적인 차트 라이브러리

### 1.6 HTTP 통신

- **Axios**: REST API 통신

### 1.7 라우팅

- **React Router DOM**: 페이지 라우팅

## 2. 핵심 기능별 구현 방안

### 2.1 실시간 채팅 기능 구현

#### 채팅방 인터페이스

- **MUI Paper**: 채팅 컨테이너
- **MUI List + ListItem**: 메시지 리스트
- **MUI TextField + IconButton**: 메시지 입력창
- **MUI Avatar**: 사용자 프로필 이미지

#### Socket.IO 연동

```jsx
// ChatRoom.jsx
import { useEffect, useState } from 'react';
import io from 'socket.io-client';
import { Paper, List, ListItem, TextField, IconButton } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';

const ChatRoom = ({ roomId }) => {
  const [socket, setSocket] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');

  useEffect(() => {
    const newSocket = io('http://localhost:8080');
    setSocket(newSocket);

    newSocket.emit('join-room', roomId);

    newSocket.on('new-message', (message) => {
      setMessages((prev) => [...prev, message]);
    });

    return () => newSocket.close();
  }, [roomId]);

  const sendMessage = () => {
    if (newMessage.trim()) {
      socket.emit('send-message', {
        roomId,
        content: newMessage,
        timestamp: new Date(),
      });
      setNewMessage('');
    }
  };

  return (
    <Paper elevation={3}>
      <List style={{ height: '400px', overflow: 'auto' }}>
        {messages.map((msg, index) => (
          <ListItem key={index}>
            <strong>{msg.user}:</strong> {msg.content}
          </ListItem>
        ))}
      </List>
      <div style={{ display: 'flex', padding: '10px' }}>
        <TextField
          fullWidth
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="메시지를 입력하세요..."
        />
        <IconButton onClick={sendMessage}>
          <SendIcon />
        </IconButton>
      </div>
    </Paper>
  );
};
```

### 2.2 분석 결과 표시 기능

#### 실시간 분석 데이터 수신

```jsx
// AnalysisPanel.jsx
import { useEffect, useState } from 'react';
import { Card, CardContent, Typography, Tabs, Tab } from '@mui/material';
import { Bar, Line } from 'react-chartjs-2';

const AnalysisPanel = ({ roomId }) => {
  const [analysisData, setAnalysisData] = useState(null);
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    // 실시간 분석 데이터 구독
    const socket = io('http://localhost:8080');

    socket.on('analysis-update', (data) => {
      setAnalysisData(data);
    });

    // 주기적으로 분석 데이터 요청
    const interval = setInterval(() => {
      socket.emit('request-analysis', { roomId });
    }, 30000); // 30초마다

    return () => {
      socket.close();
      clearInterval(interval);
    };
  }, [roomId]);

  return (
    <Card>
      <CardContent>
        <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)}>
          <Tab label="키워드 분석" />
          <Tab label="시간 분석" />
          <Tab label="참여도 분석" />
        </Tabs>

        {tabValue === 0 && analysisData?.keywords && (
          <Bar
            data={{
              labels: analysisData.keywords.map((k) => k.word),
              datasets: [
                {
                  label: '빈도',
                  data: analysisData.keywords.map((k) => k.count),
                  backgroundColor: '#2563EB',
                },
              ],
            }}
          />
        )}

        {tabValue === 1 && analysisData?.timePattern && (
          <Line data={analysisData.timePattern} />
        )}
      </CardContent>
    </Card>
  );
};
```

### 2.3 통합 대시보드 구현

#### 채팅 + 분석 동시 표시

- **MUI Grid**: 화면을 채팅 영역(좌측)과 분석 영역(우측)으로 분할
- **MUI Drawer**: 채팅방 목록 사이드바
- **실시간 업데이트**: 채팅 메시지가 추가될 때마다 분석 데이터 자동 갱신

## 3. 최소 의존성 목록

### 3.1 package.json 필수 의존성

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.8.0",
    "@mui/material": "^5.11.0",
    "@mui/icons-material": "^5.11.0",
    "@emotion/react": "^11.10.0",
    "@emotion/styled": "^11.10.0",
    "chart.js": "^4.2.0",
    "react-chartjs-2": "^5.2.0",
    "axios": "^1.3.0",
    "zustand": "^4.3.0",
    "socket.io-client": "^4.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^3.1.0",
    "vite": "^4.1.0",
    "eslint": "^8.34.0",
    "eslint-plugin-react": "^7.32.0"
  }
}
```

## 4. 실시간 채팅 + 분석 프로젝트 구조

```
frontend/
├── public/
│   └── index.html              # HTML 템플릿
├── src/
│   ├── components/             # 재사용 컴포넌트
│   │   ├── common/
│   │   │   ├── Header.jsx      # 상단 네비게이션
│   │   │   ├── Sidebar.jsx     # 채팅방 목록 사이드바
│   │   │   └── Layout.jsx      # 전체 레이아웃
│   │   ├── chat/               # 채팅 관련 컴포넌트
│   │   │   ├── ChatRoom.jsx    # 채팅방 메인
│   │   │   ├── MessageList.jsx # 메시지 리스트
│   │   │   ├── MessageInput.jsx # 메시지 입력창
│   │   │   ├── UserList.jsx    # 접속자 목록
│   │   │   └── RoomList.jsx    # 채팅방 목록
│   │   ├── analysis/           # 분석 관련 컴포넌트
│   │   │   ├── AnalysisPanel.jsx   # 분석 패널
│   │   │   ├── KeywordChart.jsx    # 키워드 차트
│   │   │   ├── TimeChart.jsx       # 시간 분석 차트
│   │   │   ├── ParticipationChart.jsx # 참여도 차트
│   │   │   └── RealtimeStats.jsx   # 실시간 통계
│   │   └── dashboard/
│   │       ├── IntegratedView.jsx  # 채팅+분석 통합 뷰
│   │       └── MetricCard.jsx      # 지표 카드
│   ├── pages/                  # 페이지 컴포넌트
│   │   ├── ChatPage.jsx        # 채팅 메인 페이지
│   │   ├── RoomListPage.jsx    # 채팅방 목록 페이지
│   │   └── AnalyticsPage.jsx   # 분석 전용 페이지
│   ├── services/               # API 및 소켓 서비스
│   │   ├── chatService.js      # 채팅 관련 API
│   │   ├── socketService.js    # Socket.IO 관리
│   │   └── analysisService.js  # 분석 데이터 API
│   ├── store/                  # 상태 관리
│   │   ├── chatStore.js        # 채팅 상태 (메시지, 방 정보)
│   │   ├── userStore.js        # 사용자 상태
│   │   └── analysisStore.js    # 분석 데이터 상태
│   ├── hooks/                  # 커스텀 훅
│   │   ├── useSocket.js        # Socket.IO 훅
│   │   ├── useChat.js          # 채팅 로직 훅
│   │   └── useAnalysis.js      # 분석 데이터 훅
│   ├── utils/                  # 유틸리티 함수
│   │   ├── socketConfig.js     # Socket.IO 설정
│   │   └── chartConfig.js      # 차트 설정
│   ├── App.jsx                 # 메인 앱 컴포넌트
│   └── main.jsx                # 앱 진입점
├── package.json                # 의존성 관리
└── vite.config.js              # Vite 설정
```

## 5. 디자인 시스템 (간소화)

### 5.1 색상 팔레트

```javascript
// theme.js
import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#2563EB', // 주 색상 (블루)
    },
    secondary: {
      main: '#10B981', // 보조 색상 (그린)
    },
    warning: {
      main: '#F59E0B', // 주의 (오렌지)
    },
    error: {
      main: '#EF4444', // 경고 (레드)
    },
  },
  typography: {
    fontFamily: 'Pretendard, Arial, sans-serif',
  },
});
```

### 5.2 반응형 브레이크포인트

- **xs**: 0px (모바일)
- **sm**: 600px (태블릿)
- **md**: 900px (데스크톱)
- **lg**: 1200px (큰 데스크톱)

## 6. 주요 기능별 구현 가이드

### 6.1 파일 업로드

```jsx
// FileUpload.jsx
import { useState } from 'react';
import { Button, LinearProgress, Alert } from '@mui/material';
import { uploadChatFile } from '../services/api';

const FileUpload = () => {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');

  const handleUpload = async () => {
    if (!file) return;

    setUploading(true);
    try {
      await uploadChatFile(file);
      setMessage('업로드 성공!');
    } catch (error) {
      setMessage('업로드 실패!');
    }
    setUploading(false);
  };

  return (
    <div>
      <input type="file" onChange={(e) => setFile(e.target.files[0])} />
      <Button onClick={handleUpload} disabled={uploading}>
        업로드
      </Button>
      {uploading && <LinearProgress />}
      {message && <Alert severity="info">{message}</Alert>}
    </div>
  );
};
```

### 6.2 차트 컴포넌트

```jsx
// KeywordChart.jsx
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

const KeywordChart = ({ keywords }) => {
  const data = {
    labels: keywords.map((k) => k.word),
    datasets: [
      {
        label: '빈도',
        data: keywords.map((k) => k.count),
        backgroundColor: '#2563EB',
      },
    ],
  };

  return <Bar data={data} />;
};
```

### 6.3 상태 관리 (Zustand)

```javascript
// store/chatStore.js
import { create } from 'zustand';

const useChatStore = create((set) => ({
  analysisResult: null,
  loading: false,
  setAnalysisResult: (result) => set({ analysisResult: result }),
  setLoading: (loading) => set({ loading }),
}));

export default useChatStore;
```

## 7. 개발 환경 설정

### 7.1 프로젝트 초기화

```bash
# Vite로 React 프로젝트 생성
npm create vite@latest chat-analyzer -- --template react

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

### 7.2 추천 VS Code 확장

- ES7+ React/Redux/React-Native snippets
- Prettier - Code formatter
- ESLint

## 8. 구현 우선순위

### Phase 1 (실시간 채팅 MVP)

1. 기본 레이아웃 및 라우팅 설정
2. Socket.IO 연결 및 기본 채팅 기능
3. 채팅방 생성/입장/퇴장
4. 실시간 메시지 송수신
5. 사용자 인증 (간단한 닉네임 기반)

### Phase 2 (분석 기능 추가)

1. 실시간 키워드 분석 표시
2. 메시지 빈도 차트
3. 시간대별 활동 패턴 분석
4. 채팅방별 통계 데이터

### Phase 3 (통합 및 개선)

1. 채팅 + 분석 통합 대시보드
2. 반응형 디자인 개선
3. 사용자 경험 향상 (로딩, 에러 처리)
4. 분석 데이터 내보내기

### 데이터 흐름

```
사용자 메시지 입력
  ↓
Socket.IO로 서버 전송
  ↓
서버에서 실시간 분석 처리
  ↓
분석 결과를 모든 클라이언트에 브로드캐스트
  ↓
프론트엔드에서 차트 자동 업데이트
```

이 통합된 채팅 + 분석 시스템으로 실시간 소통과 데이터 분석을 동시에 제공할 수 있습니다.
