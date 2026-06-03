# Azure OpenAI 프롬프팅 테스트 가이드

이 문서는 TroubleLog에서 프롬프트를 작성하고 테스트하는 담당자를 위한 안내입니다.

현재 백엔드는 Azure OpenAI 호출 코드를 가지고 있고, 면접 질문 생성용 프롬프트는 Java 코드가 아니라 `txt` 파일로 분리되어 있습니다. 프롬프트 담당자는 Java 코드를 직접 수정하지 않고, 아래 프롬프트 파일만 수정하면 됩니다.

## 담당 범위

프롬프트 담당자가 주로 수정하는 파일:

```text
src/main/resources/prompts/interview-question-system.txt
src/main/resources/prompts/interview-question-user.txt
```

백엔드 담당자가 주로 관리하는 파일:

```text
src/main/java/com/example/trouble_log/domain/ai/service/AzureOpenAiPromptService.java
src/main/java/com/example/trouble_log/domain/projectSession/service/ProjectSessionService.java
```

역할을 나누면 다음과 같습니다.

```text
프롬프트 담당자
-> system prompt 문구 수정
-> user prompt 템플릿 수정
-> 응답 형식 규칙 수정
-> 질문 품질 개선

백엔드 담당자
-> DB에서 ProjectSession, PreContext 조회
-> 프롬프트 파일 읽기
-> 플레이스홀더에 실제 값 넣기
-> Azure OpenAI 호출
-> 응답 파싱
-> InterviewQuestion 저장
```

## 프롬프트 파일 설명

### `interview-question-system.txt`

모델의 역할을 정의하는 파일입니다.

예:

```text
당신은 개발자의 트러블슈팅 경험을 검증하는 기술 면접관입니다.
사용자가 제출한 소스코드와 사전 컨텍스트를 바탕으로 구체적인 면접 질문을 생성하세요.
```

이 파일에는 보통 다음 내용을 적습니다.

```text
모델의 역할
응답 태도
답변 언어
전문성 수준
절대 지켜야 하는 상위 규칙
```

### `interview-question-user.txt`

실제 요청 템플릿입니다. 이 파일에는 백엔드가 DB에서 가져온 값이 들어갈 자리가 있습니다.

현재 사용하는 플레이스홀더:

```text
{codeContent}
{codePurpose}
{techRationale}
{exceptionHandling}
{projectScale}
```

이 이름들은 백엔드 코드에서 찾아서 실제 값으로 바꾸는 값입니다. 따라서 프롬프트 문장은 자유롭게 수정해도 되지만, 플레이스홀더 이름은 그대로 유지해야 합니다.

예:

```text
소스코드:
{codeContent}

코드 목적:
{codePurpose}
```

실행 시 백엔드는 위 내용을 다음처럼 바꿔서 Azure OpenAI에 보냅니다.

```text
소스코드:
public class Main { ... }

코드 목적:
로그인 API를 구현하기 위한 코드입니다.
```

## 소스코드는 어떻게 주입되나요?

실제 서비스 흐름에서는 프론트가 보낸 소스코드가 먼저 DB에 저장됩니다.

```text
프론트에서 소스코드 입력
-> POST /api/projects
-> ProjectSession.codeContent에 저장
-> POST /api/projects/{sessionId}/pre-context
-> 백엔드가 ProjectSession.codeContent를 조회
-> interview-question-user.txt의 {codeContent} 자리에 주입
-> Azure OpenAI에 전달
```

백엔드 코드에서는 아래 치환 로직을 통해 소스코드를 넣습니다.

```java
.replace("{codeContent}", codeContent)
```

즉 프롬프트 파일에 있는:

```text
소스코드:
{codeContent}
```

부분은 실행 시점에 실제 소스코드로 바뀝니다.

```text
소스코드:
public class LoginService { ... }
```

CLI 테스트에서는 DB를 사용하지 않기 때문에 `application-ai-cli.properties`에 들어 있는 기본 샘플 코드가 사용됩니다. 현재 기본 샘플은 로그인 검증 코드입니다.

```text
public class LoginService {
  public boolean login(String email, String password) {
    if (email == null || password == null) {
      throw new IllegalArgumentException("required");
    }
    return "test@example.com".equals(email) && "1234".equals(password);
  }
}
```

그래서 CLI 기본 테스트에서 아래와 같은 질문이 나오면 정상입니다.

```text
이메일과 비밀번호를 하드코딩한 이유
IllegalArgumentException을 사용한 이유
Spring Security로 바꾼다면 어떤 구조가 좋을지
```

이 질문들은 실제 프로젝트 전체 코드가 아니라, CLI 테스트용 기본 샘플 코드를 기반으로 생성된 것입니다.

## 현재 AI 호출 흐름

현재 질문 생성 요청은 아래 서비스에서 처리합니다.

```text
AzureOpenAiPromptService.generateInterviewQuestions(projectSession, preContext)
```

내부 흐름:

```text
1. ProjectSession, PreContext 값 검증
2. prompts/interview-question-system.txt 읽기
3. prompts/interview-question-user.txt 읽기
4. user prompt 안의 플레이스홀더를 실제 DB 값으로 치환
5. Azure OpenAI에 system prompt + user prompt 전달
6. 응답을 JSON 배열로 파싱
7. 질문 3개를 List<String>으로 반환
```

AI에게 실제 요청이 나가는 코드는 다음 위치에 있습니다.

```text
src/main/java/com/example/trouble_log/domain/ai/service/AzureOpenAiPromptService.java
```

```java
return requestSpec
        .user(userPrompt)
        .call()
        .content();
```

여기서 `.call()`이 실제 Azure OpenAI 요청 지점입니다.

## 응답 형식 규칙

현재 백엔드는 Azure 응답을 JSON 배열로 파싱합니다. 따라서 프롬프트는 모델이 반드시 아래 형식으로 답하게 유도해야 합니다.

```json
[
  "질문 1",
  "질문 2",
  "질문 3"
]
```

중요 규칙:

```text
질문은 정확히 3개
JSON 배열만 반환
마크다운 코드블록 금지
설명 문장 금지
배열 요소는 문자열
```

좋은 응답:

```json
[
  "Spring Boot와 JPA를 선택한 이유를 현재 코드 구조와 연결해서 설명해 주세요.",
  "로그인 실패 상황에서 어떤 예외 처리를 추가하면 좋을지 설명해 주세요.",
  "현재 서비스 계층에서 트랜잭션을 사용한 이유와 개선할 점을 설명해 주세요."
]
```

나쁜 응답:

```text
아래는 질문 3개입니다.

[
  ...
]
```

위처럼 설명 문장이나 마크다운 코드블록이 섞이면 백엔드 파싱이 실패할 수 있습니다.

## 로컬 테스트 방법

### 1. txt 프롬프트 파일만 따로 테스트하기

DB, Postman, Swagger 없이 프롬프트 파일만 Azure OpenAI에 보내서 확인할 수 있습니다.

이 방식은 아래 파일을 실제로 읽습니다.

```text
src/main/resources/prompts/interview-question-system.txt
src/main/resources/prompts/interview-question-user.txt
```

먼저 Azure 환경변수를 설정합니다.

```bash
export AZURE_OPENAI_ENDPOINT="https://<your-resource-name>.openai.azure.com/"
export AZURE_OPENAI_API_KEY="<your-api-key>"
export AZURE_OPENAI_DEPLOYMENT_NAME="<your-deployment-name>"
export AZURE_OPENAI_MODEL="gpt-4o"
```

실행:

```bash
AZURE_OPENAI_TEST_MODE=interview-question ./gradlew aiCli
```

기본 샘플 값으로 `{codeContent}`, `{codePurpose}` 같은 플레이스홀더가 채워지고, Azure 응답 질문 3개가 터미널에 출력됩니다.

주의:

```text
이 기본 샘플은 실제 사용자가 제출한 코드가 아닙니다.
DB 없이 프롬프트 파일만 빠르게 검증하기 위한 테스트 데이터입니다.
```

샘플 값을 바꾸고 싶으면 환경변수로 덮어쓸 수 있습니다.

```bash
export AZURE_OPENAI_TEST_MODE=interview-question
export AZURE_OPENAI_TEST_CODE_CONTENT='public class Main { public static void main(String[] args) { System.out.println("Hello"); } }'
export AZURE_OPENAI_TEST_CODE_PURPOSE="Java main 메서드 실행 흐름을 확인하는 코드입니다."
export AZURE_OPENAI_TEST_TECH_RATIONALE="가장 단순한 Java 실행 예제로 동작을 확인하기 위해 작성했습니다."
export AZURE_OPENAI_TEST_EXCEPTION_HANDLING="현재 예외 처리는 없고, 입력값이 생기면 null 검증을 추가할 예정입니다."
export AZURE_OPENAI_TEST_PROJECT_SCALE="개인 학습용 작은 예제입니다."

./gradlew aiCli
```

이 테스트는 DB 저장을 하지 않습니다. 순수하게 프롬프트 파일과 Azure 응답 형식만 확인하는 용도입니다.

### 2. Azure 연결 확인용 일반 CLI 테스트

가장 빠른 테스트 방법입니다. Azure OpenAI 연결이 되는지, 프롬프트를 넣었을 때 응답이 오는지 확인할 수 있습니다.

먼저 환경변수를 설정합니다.

```bash
export AZURE_OPENAI_ENDPOINT="https://<your-resource-name>.openai.azure.com/"
export AZURE_OPENAI_API_KEY="<your-api-key>"
export AZURE_OPENAI_DEPLOYMENT_NAME="<your-deployment-name>"
export AZURE_OPENAI_MODEL="gpt-4o"
```

실행:

```bash
./gradlew aiCli --args="소스코드와 사전 컨텍스트를 바탕으로 질문 3개 만들어줘"
```

또는 환경변수로 질문을 넣을 수 있습니다.

```bash
export AZURE_OPENAI_TEST_PROMPT="로그인 API 코드 리뷰 면접 질문 3개를 JSON 배열로 만들어줘."
./gradlew aiCli
```

주의:

```text
일반 CLI 테스트는 txt 프롬프트 파일을 자동으로 읽어서 플레이스홀더를 치환하는 테스트가 아닙니다.
간단한 Azure 연결 확인과 프롬프트 초안 실험용입니다.
```

### 3. 실제 서비스 흐름 테스트

txt 파일에 있는 프롬프트가 실제 DB 값과 함께 잘 동작하는지 보려면 API 흐름으로 테스트해야 합니다.

예상 흐름:

```text
1. 서버 실행
2. POST /api/projects 호출
   -> 소스코드 저장
   -> sessionId 반환

3. POST /api/projects/{sessionId}/pre-context 호출
   -> 사전 컨텍스트 저장
   -> Azure 질문 생성 요청
   -> InterviewQuestion 저장
   -> 질문 목록 반환
```

현재 구현 상태:

```text
프롬프트 txt 파일 분리 완료
Azure 질문 생성 메서드 구현 완료
pre-context 저장 API 구현 완료
InterviewQuestion 저장 및 질문 목록 반환 연결 완료
```

따라서 txt 프롬프트 파일이 실제 DB 값과 함께 적용되는지 확인하려면 Swagger 또는 Postman에서 `POST /api/projects/{sessionId}/pre-context`를 호출하면 됩니다.

## 프롬프트 수정 시 주의사항

플레이스홀더 이름은 바꾸지 마세요.

```text
{codeContent}
{codePurpose}
{techRationale}
{exceptionHandling}
{projectScale}
```

응답 형식은 JSON 배열을 유지해야 합니다.

```json
["질문 1", "질문 2", "질문 3"]
```

질문 개수는 3개를 유지해야 합니다. 현재 백엔드는 질문이 3개가 아니면 잘못된 응답으로 처리합니다.

API key는 절대 코드나 문서에 적지 말고 환경변수로만 관리합니다.

## 자주 나는 오류

### `401 Unauthorized`

API key가 잘못되었거나 endpoint와 key가 서로 다른 Azure OpenAI 리소스의 값일 수 있습니다.

### `404 Not Found`

대부분 deployment 이름이 잘못된 경우입니다. `AZURE_OPENAI_DEPLOYMENT_NAME`은 모델 이름이 아니라 Azure에서 만든 deployment 이름입니다.

### 응답이 JSON으로 안 나옴

`interview-question-user.txt`에서 응답 규칙을 더 강하게 적어야 합니다.

예:

```text
반드시 JSON 배열만 반환해.
설명 문장, 번호 목록, 마크다운 코드블록을 절대 포함하지 마.
```

### 질문이 너무 일반적임

소스코드와 사전 컨텍스트를 근거로 질문하라는 규칙을 강화합니다.

예:

```text
질문은 반드시 제출된 소스코드의 구조, 예외 처리, 기술 선택 이유 중 하나와 연결해서 작성해.
일반적인 CS 질문은 만들지 마.
```
