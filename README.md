# RepoNote

참여한 프로젝트의 코드와 커밋을 연결해 복습하고 면접 메모를 작성하는 React 데모입니다.

## 로컬 실행

Node.js 22 환경에서 프로젝트 루트의 터미널로 실행합니다.

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

터미널에 표시되는 로컬 주소를 엽니다. 프로덕션 빌드는 `npm.cmd run build`로 확인할 수 있습니다.

## 디렉터리

- `frontend/`에는 React 화면, 샘플 데이터 조회 모듈, 브라우저 저장 로직이 있습니다.
- `backend/`는 후속 Java + Spring Boot 서버를 위한 자리입니다.
- `frontend/src/data/projects.js`의 비동기 API가 목록과 분석을 제공합니다. 향후 Spring Boot API로 연결할 지점입니다.
- 샘플 코드는 `week11_forked`의 `5058ed8`에서 가져왔습니다. 미리보기 기여 근거는 `cb23fe8`입니다.

## 시연

‘GitHub로 시작하기’에서 예시 토큰으로 연결하고 복수 레포를 등록합니다. ‘샘플 분석 보기’로 바로 시작할 수도 있습니다. 오버뷰에서 기술명을 누르거나, 내 기능 분석에서 관련 코드와 준비된 줄 구간을 탐색합니다. 수정한 분석·면접 메모·등록 목록·마지막 기능 위치는 현재 브라우저의 localStorage에 보관합니다. 언어 비율은 샘플 값입니다.
