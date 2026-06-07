# jobgpt

Spring Boot 기반 Discord 채용정보 챗봇입니다.

## Run

### Discord bot

```bash
cd /home/ohj01/jobgpt
docker compose up -d
set -a
source .env
set +a
./gradlew bootRun
```

`.env`에는 최소한 다음 값이 필요합니다.

```dotenv
DISCORD_BOT_TOKEN=your-discord-bot-token
GEMINI_API_KEY=your-gemini-api-key
```

Discord 개발자 포털에서 `MESSAGE CONTENT INTENT`를 활성화해야 일반 메시지 본문을 수신할 수 있습니다.

정상 연결되면 콘솔에 다음 로그가 출력됩니다.

```text
Discord bot connected
```

Discord 메시지를 수신하면 콘솔에 다음 흐름의 로그가 출력됩니다.

```text
Discord message received
Calling IntentParserService for Discord message.
Parsing job search intent with Gemini.
Gemini response parsed into ChatIntent
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

### PostgreSQL

로컬 DB는 Docker Compose로 실행합니다.

```bash
cd /home/ohj01/jobgpt
docker compose up -d
```

기본 연결 정보:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/jobgpt
DB_USERNAME=jobgpt
DB_PASSWORD=jobgpt
```

### Parser smoke test without Discord

Discord 봇 로그인 없이 Gemini 파싱만 확인할 수 있습니다.

```bash
cd /home/ohj01/jobgpt
docker compose up -d
set -a
source .env
set +a
export DISCORD_BOT_ENABLED=false
export JOBGPT_TEST_MESSAGE_ENABLED=true
./gradlew bootRun
```

기본 테스트 입력은 `서울 신입 Java 백엔드 공고 찾아줘`이고, 콘솔에 `JobSearchCondition` JSON이 출력됩니다.

### Step 2: Job crawling

현재 2단계 기능은 Gemini가 추출한 `JobSearchCondition`을 기반으로 사람인과 잡코리아에서 채용공고 첫 페이지를 1회 검색합니다.

동작 흐름:

```text
Discord message
-> IntentParserService
-> JobSearchCondition
-> JobSearchService
-> SaraminCrawler + JobKoreaCrawler
-> merge + deduplicate
-> top 5 Discord response
```

수집 필드:

- `source`
- `externalId`
- `title`
- `company`
- `location`
- `experience`
- `url`
- `deadline`

크롤링은 요청 1회 기준으로만 실행합니다. 한 사이트에서 실패가 발생해도 다른 사이트 결과는 계속 반환하고, 실패 원인은 서버 로그에 남깁니다.

### Parser + crawler smoke test without Discord

Discord 없이 Gemini 파싱, 사람인/잡코리아 크롤링, DB 저장까지 한 번에 확인할 수 있습니다.

```bash
cd /home/ohj01/jobgpt
docker compose up -d
set -a
source .env
set +a
export DISCORD_BOT_ENABLED=false
export JOBGPT_TEST_MESSAGE_ENABLED=true
export JOBGPT_TEST_SEARCH_ENABLED=true
./gradlew bootRun
```

크롤링은 현재 첫 페이지 검색 결과만 대상으로 합니다. 특정 사이트 크롤링이 실패해도 나머지 사이트 결과는 계속 처리하며, 실패 원인은 로그에 남깁니다.

### Step 3: DB persistence

크롤링한 채용공고는 `job_post` 테이블에 저장됩니다.

저장 필드:

- `id`
- `source`
- `externalId`
- `title`
- `company`
- `location`
- `experience`
- `url`
- `deadline`
- `createdAt`

중복 저장 방지 구조:

- 엔티티에 `source + externalId` unique 제약조건을 둡니다.
- 저장 전 `existsBySourceAndExternalId(source, externalId)`로 이미 저장된 공고인지 확인합니다.
- 이미 존재하는 공고는 새로 저장하지 않고 기존 공고를 반환합니다.
- 동시에 같은 공고가 저장되는 경우에도 DB unique 제약조건으로 한 번 더 막습니다.

중복 저장 여부는 같은 검색을 두 번 실행한 뒤 아래 쿼리로 확인할 수 있습니다.

```bash
docker exec -it jobgpt-postgres psql -U jobgpt -d jobgpt
```

```sql
select source, external_id, count(*)
from job_post
group by source, external_id
having count(*) > 1;
```

결과가 없으면 중복 저장이 발생하지 않은 상태입니다.

### Step 4: Subscription notifications

Discord에서 자연어로 알림을 등록, 조회, 삭제할 수 있습니다. Gemini가 메시지를 아래 의도로 분류합니다.

- `SEARCH_JOB`: 채용공고 검색
- `CREATE_SUBSCRIPTION`: 알림 등록
- `LIST_SUBSCRIPTION`: 내 알림 목록 조회
- `DELETE_SUBSCRIPTION`: 내 활성 알림 삭제

사용 예시:

```text
서울 신입 백엔드 공고 찾아줘
서울 신입 백엔드 공고 매일 알려줘
내 알림 목록 보여줘
알림 삭제해줘
```

구독 정보는 `subscription` 테이블에 저장됩니다.

저장 필드:

- `id`
- `discordUserId`
- `discordChannelId`
- `keyword`
- `location`
- `experience`
- `active`
- `createdAt`

스케줄러는 `active=true` 구독을 주기적으로 확인합니다. 구독 조건으로 채용공고를 다시 검색하고, 아직 해당 사용자에게 보내지 않은 공고만 Discord 채널로 전송합니다.

발송 이력은 `sent_notifications` 테이블에 저장됩니다.

저장 필드:

- `id`
- `discordUserId`
- `jobPostId`
- `sentAt`

중복 알림 방지 구조:

- `sent_notifications`에 `discordUserId + jobPostId` unique 제약조건을 둡니다.
- 스케줄러는 발송 전 `existsByDiscordUserIdAndJobPostId`로 이미 보낸 공고인지 확인합니다.
- 이미 보낸 공고는 다시 전송하지 않습니다.

스케줄러 설정:

```dotenv
JOBGPT_NOTIFICATION_ENABLED=true
JOBGPT_NOTIFICATION_FIXED_DELAY_MS=3600000
```

기본 주기는 1시간입니다. 개발 중 빠르게 확인하려면 `JOBGPT_NOTIFICATION_FIXED_DELAY_MS=60000`처럼 줄일 수 있습니다.

### Required environment variables

- `GEMINI_API_KEY`: Gemini API 호출에 필요합니다.
- `DISCORD_BOT_TOKEN`: Discord 봇 실행에 필요합니다. 단, `DISCORD_BOT_ENABLED=false`이면 필요하지 않습니다.

선택 환경변수:

- `GEMINI_MODEL`: 기본값은 `gemini-2.5-flash`
- `DISCORD_BOT_ENABLED`: 기본값은 `true`
- `JOBGPT_TEST_MESSAGE_ENABLED`: 기본값은 `false`
- `JOBGPT_TEST_SEARCH_ENABLED`: 기본값은 `false`
- `JOBGPT_TEST_MESSAGE_INPUT`: 테스트 입력 문장
- `JOBGPT_NOTIFICATION_ENABLED`: 기본값은 `true`
- `JOBGPT_NOTIFICATION_FIXED_DELAY_MS`: 기본값은 `3600000`

## Current Features

- Gemini API로 Discord 사용자 메시지를 `JobSearchCondition` JSON으로 변환
- 사람인/잡코리아 첫 페이지 채용공고 크롤링
- 수집 필드: `source`, `externalId`, `title`, `company`, `location`, `experience`, `url`, `deadline`, `createdAt`
- `source + externalId` unique 제약조건으로 중복 저장 방지
- PostgreSQL 저장 전 `existsBySourceAndExternalId`로 기존 공고 확인
- 여러 크롤러 결과 병합 후 상위 5개 Discord 출력
- Discord 자연어 알림 등록/조회/삭제
- 스케줄러 기반 신규 공고 자동 알림
- `sent_notifications` 이력 기반 중복 알림 방지
- 크롤러별 실패 격리 및 로그 출력
