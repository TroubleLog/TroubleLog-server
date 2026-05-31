# Azure OpenAI 프롬프팅 테스트 가이드

이 문서는 TroubleLog 서버 안에서 Azure OpenAI 연결과 프롬프트 테스트를 담당하는 팀원을 위한 안내입니다.

현재 단계에서는 API 컨트롤러를 열지 않고, Spring 환경에서 Azure OpenAI 호출이 되는지만 CLI로 확인합니다. 나중에 프롬프트와 출력 형식이 확정되면 같은 서비스 코드를 도메인 기능에 연결하면 됩니다.

## 전체 구조

```text
src/main/java/com/example/trouble_log/
  AiCliApplication.java
  domain/ai/
    cli/AzureOpenAiCliRunner.java
    service/AzureOpenAiPromptService.java
  global/config/AiConfig.java

src/main/resources/
  application.properties
  application-ai-cli.properties
```

## 파일별 역할

### `build.gradle`

Azure OpenAI 호출을 위해 Spring AI Azure OpenAI starter를 사용합니다.

```gradle
implementation 'org.springframework.ai:spring-ai-starter-model-azure-openai'
```

또한 `aiCli`라는 Gradle task가 정의되어 있습니다. 이 task는 일반 서버를 띄우지 않고 AI CLI 전용 앱만 실행합니다.

```bash
./gradlew aiCli
```

### `src/main/java/com/example/trouble_log/AiCliApplication.java`

AI CLI 전용 Spring Boot entry point입니다.

일반 서버 앱인 `TroubleLogApplication`과 분리되어 있습니다. CLI 테스트 때 회원, 프로젝트, DB, JPA 같은 일반 백엔드 Bean이 같이 뜨지 않도록 AI 관련 패키지만 스캔합니다.

### `src/main/java/com/example/trouble_log/global/config/AiConfig.java`

Spring AI의 `ChatClient` Bean을 생성합니다.

다른 서비스에서 Azure OpenAI를 호출하고 싶으면 이 `ChatClient`를 직접 쓰기보다, 우선 `AzureOpenAiPromptService`를 통해 호출하는 것을 권장합니다.

### `src/main/java/com/example/trouble_log/domain/ai/service/AzureOpenAiPromptService.java`

실제 Azure OpenAI 호출을 담당하는 서비스입니다.

입력:

- `systemPrompt`: 모델의 역할과 응답 규칙
- `userPrompt`: 실제 질문 또는 분석 요청

출력:

- 모델 응답 텍스트

나중에 백엔드 기능으로 이식할 때는 컨트롤러에서 바로 AI를 호출하지 말고, `AnalysisService`, `InterviewQuestionService` 같은 도메인 서비스에서 이 서비스를 호출하는 방식이 좋습니다.

### `src/main/java/com/example/trouble_log/domain/ai/cli/AzureOpenAiCliRunner.java`

CLI 테스트용 실행 코드입니다.

프롬프트 입력 우선순위는 다음과 같습니다.

1. 실행 인자로 전달한 값
2. `AZURE_OPENAI_TEST_PROMPT` 환경변수

일반적으로는 `AZURE_OPENAI_TEST_PROMPT` 환경변수에 테스트할 질문을 넣고 `./gradlew aiCli`를 실행하면 됩니다.

### `src/main/resources/application.properties`

일반 서버 실행 시 사용하는 공통 설정입니다.

Azure OpenAI 설정도 여기에 있습니다. 실제 키와 엔드포인트는 파일에 직접 쓰지 말고 환경변수로 넣어야 합니다.

```properties
spring.ai.azure.openai.endpoint=${AZURE_OPENAI_ENDPOINT:https://example.openai.azure.com/}
spring.ai.azure.openai.api-key=${AZURE_OPENAI_API_KEY:local-placeholder}
spring.ai.azure.openai.chat.options.deployment-name=${AZURE_OPENAI_DEPLOYMENT_NAME:gpt-4o}
spring.ai.azure.openai.chat.options.model=${AZURE_OPENAI_MODEL:gpt-4o}
spring.ai.azure.openai.chat.options.temperature=${AZURE_OPENAI_TEMPERATURE:0.2}
```

주의: `AZURE_OPENAI_API_KEY` 값은 절대 커밋하지 마세요.

### `src/main/resources/application-ai-cli.properties`

`ai-cli` 프로필에서만 쓰는 설정입니다.

CLI 실행에서는 웹 서버와 DB/JPA가 필요 없으므로 꺼둡니다.

```properties
spring.main.web-application-type=none
spring.autoconfigure.exclude=...
```

또한 CLI 테스트용 프롬프트 환경변수를 읽습니다.

```properties
app.ai-cli.system-prompt=${AZURE_OPENAI_SYSTEM_PROMPT:You are a concise prompt testing assistant for TroubleLog.}
app.ai-cli.user-prompt=${AZURE_OPENAI_TEST_PROMPT:}
```

## 최초 설정

Java 21을 사용합니다.

```bash
java -version
```

`21` 계열이 보이면 됩니다.

Azure OpenAI 환경변수를 설정합니다.

```bash
export AZURE_OPENAI_ENDPOINT="https://<your-resource-name>.openai.azure.com/"
export AZURE_OPENAI_API_KEY="<your-api-key>"
export AZURE_OPENAI_DEPLOYMENT_NAME="<your-gpt-4o-deployment-name>"
export AZURE_OPENAI_MODEL="gpt-4o"
```

`AZURE_OPENAI_DEPLOYMENT_NAME`은 모델 이름이 아니라 Azure Portal에서 만든 deployment 이름입니다. deployment 이름을 `gpt-4o`로 만들었다면 `gpt-4o`를 넣으면 됩니다.

## 실행 방법

### 환경변수로 질문 실행하기

```bash
export AZURE_OPENAI_TEST_PROMPT="TroubleLog에서 장애 원인 분석 기능을 어떻게 설계하면 좋을까?"
./gradlew aiCli
```

### 한 번만 실행할 질문을 환경변수로 넣기

```bash
AZURE_OPENAI_TEST_PROMPT="로그 내용을 요약하고 원인 후보를 3개 뽑아줘." ./gradlew aiCli
```

### system prompt 바꿔보기

```bash
export AZURE_OPENAI_SYSTEM_PROMPT="너는 장애 기록을 분석하는 백엔드 어시스턴트야. 답변은 한국어로, bullet 3개 이내로 해줘."
./gradlew aiCli
```

## 프롬프팅 테스트 예시

### 장애 로그 요약

System prompt:

```text
너는 장애 기록을 구조화하는 백엔드 어시스턴트야. 답변은 한국어로 해줘.
```

User prompt:

```text
다음 상황을 요약하고 원인 후보 3개와 추가 질문 3개를 제안해줘.

상황:
배포 후 로그인 API에서 500 에러가 발생했다.
로컬에서는 정상 동작했다.
서버 로그에는 DB connection refused가 찍혔다.
```

### 인터뷰 질문 생성

System prompt:

```text
너는 개발자의 트러블슈팅 경험을 인터뷰 질문으로 바꾸는 도우미야.
```

User prompt:

```text
사용자가 겪은 문제:
Spring Boot 배포 후 MySQL 연결 실패.

이 경험을 더 자세히 기록하기 위한 후속 질문 5개를 만들어줘.
```

### JSON 출력 실험

System prompt:

```text
너는 장애 분석 결과를 JSON으로만 반환하는 도우미야. 설명 문장 없이 JSON만 출력해.
```

User prompt:

```text
다음 장애 상황을 분석해줘.

상황: 배포 후 로그인 API 500 에러. 로그에는 DB connection refused.

반환 형식:
{
  "summary": "...",
  "rootCauseCandidates": ["...", "...", "..."],
  "followUpQuestions": ["...", "...", "..."]
}
```

## 테스트 체크리스트

CLI 테스트 전에 확인할 것:

- Java 버전이 21인지 확인
- `AZURE_OPENAI_ENDPOINT`가 Azure OpenAI endpoint인지 확인
- `AZURE_OPENAI_API_KEY`가 환경변수로 설정되어 있는지 확인
- `AZURE_OPENAI_DEPLOYMENT_NAME`이 Azure Portal의 deployment 이름과 같은지 확인
- API key를 `application.properties`에 직접 적지 않았는지 확인

연결이 정상이라면 `./gradlew aiCli` 실행 후 모델 응답이 터미널에 출력됩니다.

## 자주 나는 오류

### `UnsupportedClassVersionError`

컴파일한 Java 버전과 실행 Java 버전이 다를 때 발생합니다.

이 프로젝트는 Java 21로 통일했습니다. 아래 명령으로 확인하세요.

```bash
java -version
./gradlew clean test
```

### `401 Unauthorized`

API key가 잘못되었거나 endpoint와 key가 서로 다른 Azure OpenAI 리소스의 값일 수 있습니다.

### `404 Not Found`

대부분 deployment 이름이 잘못된 경우입니다. `AZURE_OPENAI_DEPLOYMENT_NAME`은 Azure Portal의 deployment 이름이어야 합니다.

### 응답이 영어로 나옴

system prompt에 한국어 응답 규칙을 넣습니다.

```bash
export AZURE_OPENAI_SYSTEM_PROMPT="답변은 항상 한국어로 해줘."
```

## 나중에 백엔드 기능으로 연결할 때

프롬프트와 출력 형식이 확정되면 CLI가 아니라 도메인 서비스에서 `AzureOpenAiPromptService`를 호출하면 됩니다.

예상 흐름:

```text
Controller
  -> AnalysisService
      -> AzureOpenAiPromptService
      -> AnalysisResult 저장
```

이때 권장하는 방식:

- 컨트롤러에서 직접 AI 호출하지 않기
- 프롬프트 문자열은 가능하면 `src/main/resources/prompts/` 아래 파일로 분리하기
- 모델 응답이 DB에 저장될 값이라면 JSON 스키마를 먼저 정하기
- timeout, retry, 예외 처리 정책을 도메인 서비스에서 관리하기
