# RepoNote

GitHub에 연결한 프로젝트를 수집하고, 내 커밋과 코드를 근거로 면접 준비용 분석을 만드는 서비스입니다.

## 구성

- frontend에는 React + Vite 화면이 있습니다.
- backend에는 Java 21 + Spring Boot 3.5 + Spring Security + Spring JDBC 서버가 있습니다.
- MariaDB에 사용자, 암호화된 GitHub 연결 정보, 세션, 프로젝트, 분석 작업·버전, 수정 내용, 메모, 이미지 바이트·메타데이터를 저장합니다.
- AI 기본 주소는 https://open.hasa.re.kr/v1 이며 Chat Completions API를 사용합니다. 기본 모델명은 서비스의 공식 예제에 있는 qwen3-coder입니다.
- backend/src/main/resources/analysis.schema.json은 저장된 통합 분석의 계약입니다. 각 AI 요청에는 해당 항목의 속성만 추린 작은 스키마를 전달합니다.

## 백엔드 패키지 구조

backend/src/main/java/team/ft/reponote 아래에서 기능별 패키지를 사용합니다. 각 기능 안의 controller는 HTTP 요청·응답, service는 처리 순서와 트랜잭션, repository는 해당 데이터의 SQL, dto는 전달 형식을 담당합니다.

- auth에는 세션 사용자 확인, OAuth 로그인 처리, 보안 설정과 토큰 암호화가 있습니다.
- user에는 회원 프로필 조회·수정이 있습니다.
- github에는 REST API 클라이언트, 레포 검색과 코드·커밋 수집이 있습니다.
- project에는 프로젝트 등록과 카드 조회가 있습니다.
- analysis에는 결과 조회·발행, 작업 큐 관리, 워커, AI 클라이언트와 검증이 있습니다.
- writing에는 분석 수정과 면접 메모가 있습니다.
- image에는 업로드 검증, 이미지 메타데이터와 이미지 본문 저장소가 있습니다.
- export에는 Markdown 조회·렌더링·다운로드가 있습니다.
- common에는 JSON·HTTP 유틸리티와 공통 예외 처리가 있습니다.
- system에는 서버 상태 확인이 있습니다.

AnalysisRepository와 AnalysisJobRepository가 결과·작업 큐를, ImageRepository와 ImageContentRepository가 메타데이터·본문을 각각 담당합니다. 여러 테이블을 함께 변경해야 하는 작업은 service의 트랜잭션으로 묶습니다.
## 개발 환경

Java 21, Node.js 22 이상, MariaDB를 준비합니다. MariaDB에서 reponote 데이터베이스와 해당 DB에 접근할 사용자를 만들고 utf8mb4를 사용합니다. 테이블은 Flyway가 생성합니다.

backend/.env.example을 backend/.env로 복사해 빈 설정을 채웁니다. .env는 Git에서 제외합니다.

TOKEN_ENCRYPTION_KEY는 최초 한 번 생성한 32바이트 Base64 값을 사용하며 서버를 재시작할 때 동일한 값을 유지합니다. 아래 PowerShell 명령은 로컬에서 키를 생성해 출력합니다.

```powershell
$randomBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($randomBytes)
$rng.Dispose()
[Convert]::ToBase64String($randomBytes)
```

서버 환경변수에만 AI 개발키와 GitHub 비밀키를 설정합니다.

### GitHub OAuth App

GitHub의 Settings → Developer settings → OAuth Apps에서 앱을 생성합니다.

- Client ID와 Client Secret을 GITHUB_CLIENT_ID와 GITHUB_CLIENT_SECRET에 설정합니다.
- Authorization callback URL은 http://localhost:5173/login/oauth2/code/github 로 설정합니다.
- 인증은 authorization code + PKCE를 사용하고, 사용자 토큰을 암호화해 저장합니다.
- 로그인 시 read:user와 repo 범위를 요청합니다. 비공개 레포 접근에 쓰는 repo 범위에는 GitHub 정책상 쓰기 권한도 포함됩니다. 서비스의 레포 수집 클라이언트는 GET 요청을 사용합니다.
- GitHub REST API의 GET /user/repos로 소유 레포·협업 레포·조직 레포를 조회하고 페이지를 순회합니다.
- 조직이 OAuth 앱 접근을 제한하면 조직 관리자의 승인 또는 SSO 인증이 필요할 수 있습니다.
- 토큰이 취소되면 재로그인으로 연결을 갱신합니다. GitHub OAuth App 토큰의 수명을 임의로 설정하지 않습니다.

### 실행

```powershell
cd backend
.\run.ps1
```

다른 터미널에서 실행합니다.

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

http://localhost:5173 에서 GitHub 로그인 → 프로필 확인 → 레포 선택을 진행합니다. 로그인과 API 요청은 Vite 개발 프록시를 통해 같은 사이트 경로로 전달됩니다.

기존 프론트 데모를 보려면 frontend/.env.local에 VITE_DEMO=true를 설정한 뒤 개발 서버를 다시 시작합니다. 기본값은 실제 서버를 사용하는 모드입니다. 데모 계정과 실제 회원 데이터는 별도로 관리됩니다.

## 분석 작업

레포 등록 시 DB에 대기 작업이 생성됩니다. 기본 워커는 한 번에 하나씩 처리하고, 화면을 닫아도 서버에서 계속합니다. 프론트는 5초마다 카드 상태를 조회합니다.

작업은 커밋 SHA를 고정한 뒤 해당 시점의 파일 트리, 본인 작성 커밋과 변경 코드, 핵심 파일을 수집합니다. 기본 범위는 소스 파일 24개, 파일당 60KB, 소스·패치 문자 예산 90,000자, 본인 커밋 최대 20개입니다. 패키지·빌드 설정과 기여 파일을 우선하며 제외 범위를 결과에 기록합니다. GitHub 언어 비율은 수집 시점 기본 브랜치의 통계입니다.

자료에 들어 있는 지시문은 분석 자료로 취급하도록 AI에 전달합니다. 파일 내용은 실행하지 않습니다. AI 출력은 항목별 JSON Schema, 파일·줄 범위, 개념, 노드·연결선 및 근거 커밋을 검증한 뒤 부분 결과로 저장합니다. 네 항목이 모두 완료되면 해당 분석 버전이 완료 상태가 됩니다. 파일·패치의 일반적인 비밀번호·토큰 할당 라인은 마스킹합니다.

AI_STRUCTURED_OUTPUT=false가 기본값입니다. 서비스 문서에서 json_schema 응답 옵션 지원 여부를 확인할 수 없어, 기본 요청은 스키마를 프롬프트에 포함하고 서버에서 검증합니다. 모델이 지원함을 확인한 경우 true로 설정하면 response_format에 strict JSON Schema를 전달합니다. 모델 출력 중단·JSON 오류·참조 오류는 실패로 표시합니다.

429, 5xx 및 네트워크 오류는 최대 2회 추가 재시도합니다. 401·403 등 권한 오류는 사용자에게 재연결을 안내합니다. 재시도 시에도 같은 기준 SHA를 사용합니다. 프로세스 중단으로 10분 이상 임대가 갱신되지 않은 작업은 실패로 복구해 수동 재시도를 제공합니다. 네트워크 응답이 유실된 AI 요청은 제공자에서 이미 처리됐을 수 있어 재시도 시 제공자 사용량이 추가될 수 있습니다.

### 항목별 AI 요청과 진행 상태

1. 프로젝트 소개·주요 기능: 목적, 처리 과정, 주요 기술과 개념·코드 설명
2. 아키텍처: 구조도 노드·연결 관계와 동작 설명
3. 내 구현 기능: 본인 커밋에 근거한 기능·기술 심화 분석
4. 면접 어필 포인트: 앞선 세 분석이 완료된 후 검증된 결과를 바탕으로 생성

한 번 수집한 저장소 스냅샷과 SHA를 analysis_jobs.snapshot에 저장해 모든 항목과 재시도에서 재사용합니다. 각 항목은 별도 Chat Completions 요청이며, 요청마다 해당 항목만 포함한 스키마를 전달합니다. 소개·아키텍처 요청에는 커밋 패치를 생략하고, 면접 요청은 원본 파일 대신 완료된 분석과 근거 파일 ID를 전달합니다.

analysis_sections에는 항목별 상태·시도 횟수·응답·오류가 저장됩니다. 워커는 한 번에 한 항목을 처리합니다. 화면은 5초 간격으로 진행 상태를 확인하고 완료된 항목부터 보여줍니다. 면접 항목은 나머지 세 항목의 완료를 기다립니다. 실패한 항목이 있어도 다른 독립 항목은 계속 분석합니다.

실패한 항목은 다른 진행 작업이 종료된 후 `POST /api/analysis-jobs/{id}/sections/{key}/retry`로 다시 요청합니다. key는 overview, architecture, contributions, interview 중 하나입니다. 소유자 인증과 CSRF 검증을 적용하며, 완료된 항목과 작성한 메모는 보존합니다. 일시적인 외부 오류의 자동 재시도 횟수는 항목별로 계산합니다. 새 SHA로 전체를 분석하려면 기존 프로젝트 재분석 기능을 사용합니다.

오버뷰 응답은 complete, jobId, sections를 포함하며, 프로젝트 카드의 full은 부분 분석을 포함한 조회 가능 여부, complete는 최신 분석의 전체 완료 여부입니다. 통합 payload 구조는 기존과 동일합니다. 미완료 항목은 빈 기본값으로 저장하고 화면에서 대기·실패 상태를 구분합니다. Markdown 다운로드는 모든 항목이 완료된 뒤 허용합니다. V2 마이그레이션은 기존 분석을 완료 상태로 보존합니다.

## 분석 버전과 개인 기록

재분석은 새 버전을 생성합니다. 수정 본문·면접 메모는 각 분석 버전의 기능에 연결합니다. 이전 기록은 버전 선택에서 열 수 있습니다. 동일 기능임을 자동으로 추정해 다른 버전에 덮어쓰는 처리는 수행하지 않습니다. 동시 수정은 revision으로 감지하고 충돌을 안내합니다.

프로젝트 카드의 정리본 다운로드는 마지막 완료 버전을 사용합니다. 상세의 다운로드는 현재 선택 버전을 사용하며 해당 버전에서 수정한 본문과 메모를 포함합니다. Mermaid 도표·텍스트 관계 목록·주요 기능·코드·CS 해설·면접 팁을 UTF-8 Markdown으로 생성합니다.

프로필 사진과 대표 이미지는 PNG·JPEG·GIF, 최대 5MB 및 2천만 픽셀로 제한합니다. 이미지 본문과 메타데이터를 분리해 DB에 저장하며 소유권을 확인한 후 응답합니다.

## 배포

프론트는 Vercel의 Vite 프리셋과 frontend 루트, npm run build, dist 출력을 사용합니다. 실제 서비스에서 프론트 도메인의 /api/*, /oauth2/*, /login/* 경로를 Spring Boot HTTPS 주소로 프록시하도록 호스팅의 rewrite 설정을 구성합니다.

서버는 Java 21 실행 환경에 bootJar를 배포합니다. FRONTEND_URL과 GITHUB_CALLBACK_URL을 배포된 프론트 도메인으로 변경하고 GitHub OAuth App에도 동일 콜백 주소를 등록합니다. 운영에서는 COOKIE_SECURE=true를 사용합니다. 프록시를 사용할 경우 신뢰할 수 있는 프록시가 전달하는 Forwarded 헤더만 서버에 도달하도록 구성합니다.

배포할 때 VITE_DEMO=true가 설정돼 있으면 샘플 화면으로 빌드되므로 실제 연동 모드에서는 false로 설정합니다. AI 개발키는 프론트의 VITE_ 환경변수에 넣지 않습니다.

## 검증

```powershell
cd backend
.\gradlew.bat test bootJar
cd ../frontend
npm.cmd run build
```

테스트는 H2 MySQL 호환 모드에서 실제 Spring Security·JDBC 세션·Flyway를 사용해 권한 분리, CSRF, 동시 분석 요청, 이전 버전 보존, 수정 충돌, 작업 소유권과 토큰 암호화를 확인합니다. AI는 로컬 HTTP 서버에서 Chat Completions 요청 계약과 잘못된 근거 거부를 확인합니다. 외부 계정과 실제 MariaDB의 최종 연결은 실행 설정을 채운 뒤 확인해야 합니다.

## 기존 데이터베이스와 로컬 포트

기존 테이블이 있는 DB에 Flyway 이력이 없으면, 먼저 V1이 만드는 테이블 이름과 충돌하는지 확인합니다. 충돌이 없는 DB의 최초 도입은 DB_FLYWAY_BASELINE_ON_MIGRATE=true로 시작할 수 있습니다. 기준 버전은 0으로 설정돼 있어 V1 테이블 생성이 적용됩니다. 초기 도입 후에는 false를 사용합니다. 이미 생성된 이력은 이후 시작 때 자동으로 인식합니다.

이 PC에서는 Windows 예약 포트 범위에 8080이 포함돼 로컬 backend/.env의 PORT를 18080으로, frontend/.env.local의 BACKEND_URL을 http://localhost:18080으로 설정했습니다. 프론트 접속 주소와 GitHub 콜백 주소는 localhost:5173을 사용합니다.
