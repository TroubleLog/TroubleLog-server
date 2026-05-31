# TroubleLog-server
2026-1 인공지능산업체특강

## 개발 환경

- Java 21
- Spring Boot 3.5.14

## Azure OpenAI 설정

이 서버는 Spring AI Azure OpenAI 클라이언트를 사용합니다. 키와 엔드포인트는 코드에 저장하지 않고 환경변수로 주입합니다.

프롬프팅 담당자를 위한 자세한 파일 설명과 테스트 방법은 [Azure OpenAI 프롬프팅 테스트 가이드](docs/azure-openai-prompting.md)를 참고하세요.

```bash
export AZURE_OPENAI_ENDPOINT="https://<your-resource-name>.openai.azure.com/"
export AZURE_OPENAI_API_KEY="<your-api-key>"
export AZURE_OPENAI_DEPLOYMENT_NAME="<your-gpt-4o-deployment-name>"
export AZURE_OPENAI_MODEL="gpt-4o"
```

CLI 테스트:

```bash
export AZURE_OPENAI_SYSTEM_PROMPT="너는 장애 기록을 구조화하는 백엔드 어시스턴트야."
export AZURE_OPENAI_TEST_PROMPT="로그 내용을 요약하고 원인 후보를 3개 뽑아줘."

./gradlew aiCli
```

한 번만 실행할 질문을 인라인 환경변수로 넣을 수도 있습니다.

```bash
AZURE_OPENAI_TEST_PROMPT="로그 내용을 요약하고 원인 후보를 3개 뽑아줘." ./gradlew aiCli
```
