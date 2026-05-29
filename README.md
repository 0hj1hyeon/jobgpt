# jobgpt

Spring Boot 기반 Discord 채용정보 챗봇입니다.

## Run

```bash
docker compose up -d
export DISCORD_BOT_TOKEN="your-discord-bot-token"
export OPENAI_API_KEY="your-openai-api-key"
./gradlew bootRun
```

Discord 개발자 포털에서 `MESSAGE CONTENT INTENT`를 활성화해야 일반 메시지 본문을 수신할 수 있습니다.
