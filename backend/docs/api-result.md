## http://localhost:8080/api/llm-analysis/analyze-message

```
curl --location 'http://localhost:8080/api/llm-analysis/analyze-message' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInN1YiI6InRlc3QxIiwiaWF0IjoxNzU2MTU3Nzc0LCJleHAiOjE3NTYyNDQxNzR9.PGnygX4g1o9122ej0AFoKnspzjWT4BnCHfLpB7utNSI' \
--header 'Content-Type: application/json' \
--data '{
    "content": "프로젝트 일정이 지연되어 걱정스럽습니다. 도움이 필요해요."
}'
```

```
{
    "keywords": [
        "프로젝트",
        "일정",
        "지연",
        "걱정",
        "도움"
    ],
    "topic": "업무",
    "emotion": "부정",
    "sentiment_score": 0.3,
    "urgency": "높음",
    "intent": "요청",
    "confidence": 0.7
}
```

## http://localhost:8080/api/llm-analysis/extract-keywords

```
curl --location 'http://localhost:8080/api/llm-analysis/extract-keywords' \
--header 'Content-Type: application/json' \
--header 'Authorization: ••••••' \
--data '{
    "content": "오늘 회의에서 새로운 AI 기능 개발과 데이터베이스 최적화, 사용자 경험 개선에 대해 논의했습니다."
}'
```

```
{
    "keywords": [
        "AI 기능",
        "데이터베이스 최적화",
        "사용자 경험",
        "개발",
        "논의",
        "회의",
        "개선"
    ]
}
```

## http://localhost:8080/api/llm-analysis/classify-topic

```
curl --location 'http://localhost:8080/api/llm-analysis/classify-topic' \
--header 'Content-Type: application/json' \
--header 'Authorization: ••••••' \
--data '{
    "content": "서버에 치명적인 버그가 발생했습니다. 시스템이 다운되어 긴급히 수정이 필요합니다."
}'
```

```
{
    "topic": "문제"
}
```

## http://localhost:8080/api/llm-analysis/analyze-emotion

```
curl --location 'http://localhost:8080/api/llm-analysis/analyze-emotion' \
--header 'Content-Type: application/json' \
--header 'Authorization: ••••••' \
--data '{
    "content": "새로운 프로젝트가 대성공을 거두어서 팀원들 모두가 정말 기뻐하고 있어요! 축하합니다!"
}'
```

```
{
    "emotion": "긍정",
    "intensity": "강함",
    "score": 0.7,
    "emotions": {
        "기쁨": 0.3,
        "분노": 0.1,
        "슬픔": 0,
        "놀람": 0.2,
        "두려움": 0,
        "중립": 0.4
    }
}
```

## http://localhost:8080/api/llm-analysis/analyze-context

```
curl --location 'http://localhost:8080/api/llm-analysis/analyze-context' \
--header 'Content-Type: application/json' \
--header 'Authorization: ••••••' \
--data '{
    "messages": [
          "안녕하세요, 새로운 기능 개발 진행상황이 어떻게 되나요?",
          "현재 80% 정도 완료되었고 테스트 중입니다.",
          "좋네요! 출시 예정일은 언제쯤인가요?",
          "다음 주 화요일에 베타 버전을 출시할 예정입니다.",
          "완벽합니다. 마케팅 준비도 시작하겠습니다."
      ]
}'
```

```
{
    "flow": "활발",
    "tone": "전문적",
    "main_topics": [
        "기능 개발 진행상황",
        "출시 일정"
    ],
    "participation_pattern": "주도적",
    "resolution_status": "진행중",
    "summary": "새로운 기능 개발 진행 상황과 출시 일정에 대한 논의"
}
```