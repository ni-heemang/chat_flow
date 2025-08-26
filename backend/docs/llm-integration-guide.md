# LLM 기반 채팅 분석 시스템 통합 가이드

## 🚀 개요

기존의 정적 키워드 매칭 방식에서 **LLM(Large Language Model) 기반 지능형 분석 시스템**으로 업그레이드되었습니다.

## 📊 변경 사항 비교

### **기존 정적 분석**
```java
// 하드코딩된 키워드 매칭
Set<String> TOPIC_KEYWORDS = Set.of("회의", "프로젝트", "업무");
if (keywords.contains(word)) {
    topicScores.merge(topic, 3, Integer::sum);
}
```

### **새로운 LLM 기반 분석**
```java
// LLM을 활용한 지능형 분석
llmAnalysisService.analyzeMessage(content)
    .thenAccept(result -> {
        String topic = (String) result.get("topic");
        Double confidence = (Double) result.get("confidence");
        // 맥락을 이해한 정확한 분석
    });
```

## 🔧 구현된 기능

### 1. **LlmAnalysisService** - 핵심 LLM 통합 서비스
- **종합 분석**: 키워드, 주제, 감정, 의도, 긴급도 한번에 분석
- **비동기 처리**: CompletableFuture 기반 성능 최적화
- **폴백 시스템**: LLM 실패 시 기존 정적 분석으로 자동 전환
- **다중 LLM 지원**: OpenAI, Claude, Gemini 등 설정으로 변경 가능

### 2. **ChatAnalysisService** - 업그레이드된 분석 엔진
```java
// 기존 정적 방식 (폴백용)
List<String> keywords = extractKeywords(message.getContent());

// 새로운 LLM 방식
llmAnalysisService.analyzeMessage(message.getContent())
    .thenAccept(analysisResult -> {
        // 훨씬 정확한 분석 결과 활용
    });
```

### 3. **스마트 폴백 시스템**
- LLM API 실패 시 기존 정적 분석으로 자동 전환
- 사용자는 분석 중단을 경험하지 않음
- 부분적 실패 시에도 가능한 결과 제공

## ⚙️ 설정 방법

### 1. **환경 변수 설정**
```bash
# API 키 설정 (application.yml에 자동 주입됨)
export LLM_API_KEY="your-openai-api-key"

# 선택사항: 다른 LLM 사용 시
export LLM_API_URL="https://api.openai.com/v1/chat/completions"
export LLM_MODEL="gpt-4"
```

### 2. **application.yml에서 환경변수 자동 주입**
```yaml
llm:
  provider: "openai"  # openai, claude, gemini 등
  api:
    key: "${LLM_API_KEY:your-api-key-here}"  # 환경변수에서 자동 주입
    url: "${LLM_API_URL:https://api.openai.com/v1/chat/completions}"
  model: "${LLM_MODEL:gpt-3.5-turbo}"  # 기본값: gpt-3.5-turbo
  max-tokens: 1000
  temperature: 0.3
  
  fallback:
    enabled: true
    timeout-seconds: 10
    max-retries: 2
```

**장점**:
- 🔒 **보안**: API 키가 코드에 노출되지 않음
- 🔄 **환경별 설정**: dev/prod 환경에서 다른 모델 사용 가능
- ⚙️ **유연성**: 런타임에 환경변수로 설정 변경

### 3. **프로필별 설정**
- **dev**: gpt-3.5-turbo (빠르고 저렴)
- **prod**: gpt-4 (높은 정확도)
- **test**: mock (테스트용)

## 🎯 LLM 분석 결과 형식

### **종합 분석 응답**
```json
{
  "keywords": ["프로젝트", "개발", "일정"],
  "topic": "업무",
  "emotion": "긍정",
  "sentiment_score": 0.7,
  "urgency": "높음",
  "intent": "요청",
  "confidence": 0.85
}
```

### **감정 분석 응답**
```json
{
  "emotion": "긍정",
  "intensity": "강함", 
  "score": 0.8,
  "emotions": {
    "기쁨": 0.4,
    "중립": 0.3,
    "놀람": 0.2,
    "분노": 0.1
  }
}
```

## 🚀 새로운 API 엔드포인트

### **LlmAnalysisController**
- `POST /api/llm-analysis/analyze-message` - 단일 메시지 종합 분석
- `POST /api/llm-analysis/extract-keywords` - 키워드 추출
- `POST /api/llm-analysis/classify-topic` - 주제 분류
- `POST /api/llm-analysis/analyze-emotion` - 감정 분석
- `POST /api/llm-analysis/analyze-context` - 대화 맥락 분석
- `GET /api/llm-analysis/rooms/{roomId}/compare` - 정적 vs LLM 분석 비교
- `GET /api/llm-analysis/status` - 시스템 상태 확인

## 📈 성능 최적화

### **비동기 처리**
```java
@Async
public void analyzeMessage(MessageReceivedEvent event) {
    llmAnalysisService.analyzeMessage(content)
        .thenAccept(result -> {
            // 결과 처리
        })
        .exceptionally(throwable -> {
            // 폴백 처리
            performFallbackAnalysis(roomId, message, username);
            return null;
        });
}
```

### **배치 처리** (선택적)
- 메시지를 배치로 모아서 처리하여 API 호출 횟수 최적화
- 실시간성이 중요한 경우 비활성화 가능

## 🧪 테스트 방법

### 1. **단위 테스트**
```bash
# LLM 서비스 테스트
curl -X POST http://localhost:8080/api/llm-analysis/analyze-message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"content": "프로젝트 진행 상황 공유드립니다"}'
```

### 2. **비교 테스트**
```bash
# 기존 vs LLM 분석 결과 비교
curl http://localhost:8080/api/llm-analysis/rooms/1/compare \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. **상태 확인**
```bash
curl http://localhost:8080/api/llm-analysis/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔄 마이그레이션 가이드

### **단계별 전환**
1. **Phase 1**: LLM 서비스 병렬 운영 (현재 상태)
2. **Phase 2**: 특정 채팅방에서 LLM 우선 사용
3. **Phase 3**: 전체 LLM 전환, 정적 분석은 폴백으로만 사용

### **점진적 적용**
```yaml
# 특정 채팅방만 LLM 사용
llm:
  enabled-rooms: [1, 2, 5]  # 특정 채팅방 ID
  fallback-for-others: true
```

## 💡 활용 가능한 고급 기능

### 1. **맥락 인식 분석**
- 이전 대화 내용을 고려한 분석
- 대화의 흐름과 의도 파악

### 2. **다국어 지원**
- 한국어뿐만 아니라 영어, 일본어 등 지원
- 언어별 문화적 맥락 이해

### 3. **커스텀 프롬프트**
- 업무 도메인별 맞춤 분석
- 회사 특화 키워드 학습

### 4. **실시간 학습**
- 사용자 피드백 기반 분석 개선
- 도메인 특화 지식 축적

## 📚 참고 자료

- **OpenAI API 문서**: https://platform.openai.com/docs
- **Claude API 문서**: https://docs.anthropic.com
- **Spring WebFlux**: 비동기 처리 최적화
- **CompletableFuture**: Java 비동기 프로그래밍

## 🎉 완료된 작업

- ✅ LLM 통합 서비스 구현
- ✅ 기존 분석 서비스 업그레이드  
- ✅ 폴백 시스템 구축
- ✅ 설정 파일 업데이트
- ✅ 테스트 API 컨트롤러 구현
- ✅ 비동기 처리 최적화

## 🔜 다음 단계

1. **환경변수 설정** (application.yml에서 자동 주입):
   ```bash
   export LLM_API_KEY="sk-your-actual-openai-api-key"
   ```

2. **애플리케이션 재시작** 후 테스트:
   ```bash
   curl -X POST http://localhost:8080/api/llm-analysis/analyze-message \
     -H "Content-Type: application/json" \
     -d '{"content": "프로젝트 일정이 지연되고 있어서 걱정됩니다"}'
   ```

3. **성능 벤치마크** 테스트 수행
4. **프론트엔드 연동** (더 풍부한 분석 결과 표시)
5. **사용자 피드백** 수집 및 개선