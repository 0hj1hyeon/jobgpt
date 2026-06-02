# jobgpt

Spring Boot 기반 Discord 채용정보 챗봇입니다.

## Run

### Discord bot

```bash
cd /home/ohj01/jobgpt
docker compose up -d
export DISCORD_BOT_TOKEN="your-discord-bot-token"
export GEMINI_API_KEY="your-gemini-api-key"
./gradlew bootRun
```

Discord 개발자 포털에서 `MESSAGE CONTENT INTENT`를 활성화해야 일반 메시지 본문을 수신할 수 있습니다.

정상 연결되면 콘솔에 다음 로그가 출력됩니다.

```text
Discord bot connected
```

Discord에서 다음처럼 입력하면 Gemini가 검색 조건 JSON으로 변환하고, 봇이 채널에 JSON을 응답합니다.

```text
서울 신입 Java 백엔드 공고 찾아줘
```

이후 사람인/잡코리아 첫 페이지를 크롤링하고, PostgreSQL에 저장한 뒤 상위 5개 공고를 Discord에 출력합니다.

조건 JSON 예시:

```json
{"keyword":"Java 백엔드","location":"서울","experience":"신입"}
```

Discord 출력에는 `source`, `title`, `company`, `location`, `experience`, `deadline`, `url`이 포함됩니다.

### Parser smoke test without Discord

Discord 봇 로그인 없이 Gemini 파싱만 확인할 수 있습니다.

```bash
cd /home/ohj01/jobgpt
docker compose up -d
export GEMINI_API_KEY="your-gemini-api-key"
export DISCORD_BOT_ENABLED=false
export JOBGPT_TEST_MESSAGE_ENABLED=true
./gradlew bootRun
```

기본 테스트 입력은 `서울 신입 Java 백엔드 공고 찾아줘`이고, 콘솔에 `JobSearchCondition` JSON이 출력됩니다.

### Parser + crawler smoke test without Discord

Discord 없이 Gemini 파싱, 사람인/잡코리아 크롤링, DB 저장까지 한 번에 확인할 수 있습니다.

```bash
cd /home/ohj01/jobgpt
docker compose up -d
export GEMINI_API_KEY="your-gemini-api-key"
export DISCORD_BOT_ENABLED=false
export JOBGPT_TEST_MESSAGE_ENABLED=true
export JOBGPT_TEST_SEARCH_ENABLED=true
./gradlew bootRun
```

크롤링은 현재 첫 페이지 검색 결과만 대상으로 합니다. 특정 사이트 크롤링이 실패해도 나머지 사이트 결과는 계속 처리하며, 실패 원인은 로그에 남깁니다.

### Required environment variables

- `GEMINI_API_KEY`: Gemini API 호출에 필요합니다.
- `DISCORD_BOT_TOKEN`: Discord 봇 실행에 필요합니다. 단, `DISCORD_BOT_ENABLED=false`이면 필요하지 않습니다.

선택 환경변수:

- `GEMINI_MODEL`: 기본값은 `gemini-2.5-flash`
- `DISCORD_BOT_ENABLED`: 기본값은 `true`
- `JOBGPT_TEST_MESSAGE_ENABLED`: 기본값은 `false`
- `JOBGPT_TEST_SEARCH_ENABLED`: 기본값은 `false`
- `JOBGPT_TEST_MESSAGE_INPUT`: 테스트 입력 문장

## Current Features

- Gemini API로 Discord 사용자 메시지를 `JobSearchCondition` JSON으로 변환
- 사람인/잡코리아 첫 페이지 채용공고 크롤링
- 수집 필드: `source`, `externalId`, `title`, `company`, `location`, `experience`, `url`, `deadline`
- `source + externalId` unique 제약조건으로 중복 저장 방지
- 여러 크롤러 결과 병합 후 상위 5개 Discord 출력
- 크롤러별 실패 격리 및 로그 출력
