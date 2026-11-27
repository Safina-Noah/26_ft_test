import testController from "./snapshots/AttachmentControllerTest.java.txt?raw";
import { learning, contributions } from "./learning";
import template from "./snapshots/detail.html.txt?raw";
import controller from "./snapshots/AttachmentController.java.txt?raw";

const projects = [
  {
    id: "week11_forked",
    owner: "Safina-Noah",
    title: "week11_forked",
    type: "개인",
    color: "violet",
    art: "image",
    full: true,
    description: "게시글에 자료를 첨부하고 이미지를 미리 보는 파일 공유 게시판",
    tags: ["Java", "Spring Boot", "Thymeleaf"],
  },
  {
    id: "study-calendar",
    owner: "campus-lab",
    title: "study-calendar",
    type: "팀",
    color: "mint",
    art: "calendar",
    full: false,
    description: "팀원과 스터디 일정을 함께 관리하는 캘린더",
    tags: ["TypeScript", "React"],
  },
  {
    id: "dev-journal",
    owner: "Safina-Noah",
    title: "dev-journal",
    type: "개인",
    color: "peach",
    art: "journal",
    full: false,
    description: "개발 과정에서 배운 내용을 기록하는 개인 저널",
    tags: ["JavaScript", "CSS"],
  },
];

const concepts = {
  "Spring MVC": [
    "HTTP 요청을 컨트롤러에 연결하고 화면이나 데이터 응답을 만드는 웹 계층입니다.",
    "PostController는 템플릿을, AttachmentController는 파일 응답을 반환합니다.",
    "두 컨트롤러의 반환 방식은 어떻게 다른가요?",
    "일반 Controller는 템플릿 이름을 해석하고, RestController는 ResponseEntity의 상태·헤더·본문을 응답합니다.",
  ],
  "multipart/form-data": [
    "텍스트 필드와 파일 바이트를 여러 부분으로 나누어 보내는 HTTP 요청 형식입니다.",
    "제목과 본문은 PostForm에, 첨부파일은 MultipartFile 목록에 전달됩니다.",
    "일반 텍스트 폼으로 파일도 보낼 수 있나요?",
    "이 작성 폼은 파일 데이터를 전송하기 위해 multipart/form-data 인코딩을 지정합니다.",
  ],
  "Content-Type": [
    "응답 본문이 어떤 형식인지 나타내는 HTTP 헤더입니다.",
    "미리보기는 image/jpeg 같은 이미지 유형을 보내고 다운로드는 application/octet-stream을 사용합니다.",
    "확장자로 정한 유형이 실제 내용을 보증하나요?",
    "이 구현은 허용 확장자로 유형을 정합니다. 실제 바이트를 검사하는 검증은 별도로 필요합니다.",
  ],
  MockMvc: [
    "서버 포트를 열지 않고 Spring MVC 요청과 응답을 테스트하는 도구입니다.",
    "임시 파일과 모의 Repository로 미리보기의 상태 코드, 헤더와 바이트를 검증합니다.",
    "이미지 자체가 정상 파일인지도 검사하나요?",
    "테스트의 임시 바이트로 HTTP 응답을 확인합니다. 실제 이미지 디코딩까지 검증하지는 않습니다.",
  ],
  Thymeleaf: [
    "서버에서 데이터를 HTML에 채우는 템플릿 엔진입니다.",
    "detail.html은 첨부 유형을 확인한 뒤 이미지 요청 주소를 만듭니다.",
    "페이지 HTML과 이미지 파일은 같은 요청으로 오나요?",
    "서버가 먼저 HTML을 반환하고, 브라우저가 img의 src로 별도 요청을 보냅니다.",
  ],
  "Spring Boot": [
    "Java 웹 애플리케이션의 설정과 실행을 지원합니다.",
    "Spring MVC 컨트롤러가 첨부 ID를 받아 파일과 HTTP 헤더를 응답합니다.",
    "파일 조회와 응답 구성을 왜 나누었나요?",
    "저장소 접근은 FileStorageService에서 재사용하고, 컨트롤러가 다운로드와 미리보기 각각의 HTTP 응답을 결정합니다.",
  ],
  JPA: [
    "Java 객체를 관계형 DB 행에 매핑합니다.",
    "게시글과 첨부파일을 일대다 관계로 연결하고 ID로 첨부 정보를 조회합니다.",
    "게시글을 삭제하면 디스크 파일도 자동으로 없어지나요?",
    "JPA의 연쇄 삭제는 엔티티에 적용됩니다. 디스크 파일은 서비스에서 별도로 삭제합니다.",
  ],
  MariaDB: [
    "행과 열로 구성된 데이터를 저장하는 관계형 데이터베이스입니다.",
    "첨부 ID, 원본 이름, 저장 이름, 유형과 크기를 보관합니다. 파일 본문은 디스크에 있습니다.",
    "미리보기에서 DB가 필요한 이유는 무엇인가요?",
    "첨부 ID로 메타데이터를 조회해야 서버의 저장 이름과 응답할 이미지 유형을 알 수 있습니다.",
  ],
  "Content-Disposition": [
    "브라우저가 응답 본문을 어떻게 취급할지 알려주는 HTTP 헤더입니다.",
    "미리보기 응답은 inline, 다운로드 응답은 attachment를 지정합니다.",
    "inline만 지정하면 모든 파일이 이미지로 표시될까요?",
    "브라우저가 표시할 수 있는 형식이어야 합니다. 이 코드는 허용 이미지 유형을 검사하고 Content-Type도 함께 설정합니다.",
  ],
};
const files = [
  {
    id: "test",
    name: "AttachmentControllerTest.java",
    path: "src/test/java/edu/handong/web11/controller/AttachmentControllerTest.java",
    code: testController,
    ranges: [
      {
        from: 37,
        to: 50,
        title: "이미지 유형과 바이트 검증",
        concept: "MockMvc",
        text: "JPEG·PNG·GIF 각각에 대해 정상 상태 코드, Content-Type, inline 헤더와 파일 바이트를 검사합니다.",
      },
      {
        from: 52,
        to: 69,
        title: "없는 파일과 경로 검사",
        concept: "MockMvc",
        text: "존재하지 않는 ID와 디스크 파일, 업로드 폴더 바깥을 가리키는 저장 이름이 404로 처리되는지 검증합니다.",
      },
      {
        from: 78,
        to: 92,
        title: "문서 다운로드 회귀 검증",
        concept: "Content-Disposition",
        text: "PDF·TXT는 미리보기 415를 반환하지만 다운로드는 attachment 헤더와 원래 파일 바이트를 반환하는지 검사합니다.",
      },
    ],
  },
  {
    id: "html",
    name: "detail.html",
    path: "src/main/resources/templates/posts/detail.html",
    code: template,
    ranges: [
      {
        from: 21,
        to: 28,
        title: "첨부 정보와 다운로드",
        concept: "Thymeleaf",
        text: "첨부 목록을 순회해 원본 이름과 크기, 유형을 표시합니다. 다운로드 링크는 파일 ID를 사용합니다.",
      },
      {
        from: 30,
        to: 38,
        title: "미리보기 이미지 요청",
        concept: "Thymeleaf",
        text: "지원하는 이미지에만 figure를 생성합니다. img의 src가 미리보기 엔드포인트를 가리키고, 로드 실패 시 안내 문구를 표시합니다.",
      },
    ],
  },
  {
    id: "java",
    name: "AttachmentController.java",
    path: "src/main/java/edu/handong/web11/controller/AttachmentController.java",
    code: controller,
    ranges: [
      {
        from: 29,
        to: 42,
        title: "다운로드 응답",
        concept: "Content-Disposition",
        text: "원본 파일명을 UTF-8로 인코딩하고 attachment 헤더를 보냅니다. 파일은 일반 바이너리 응답으로 전달됩니다.",
      },
      {
        from: 44,
        to: 54,
        title: "첨부 조회와 유형 확인",
        concept: "MariaDB",
        text: "ID로 메타데이터를 찾아 JPEG, PNG, GIF인지 확인합니다. 없는 ID는 404, 지원하지 않는 유형은 415로 응답합니다.",
      },
      {
        from: 55,
        to: 62,
        title: "이미지 응답 구성",
        concept: "Content-Disposition",
        text: "기존 서비스에서 파일 리소스를 가져와 실제 이미지 유형과 inline 헤더를 붙입니다. 브라우저가 받아서 img 요소 안에 표시합니다.",
      },
    ],
  },
];

// Keep fixture access behind the same asynchronous contract as a future HTTP client.
export const projectApi = {
  async listProjects() {
    return projects;
  },
  async getAnalysis(projectId) {
    if (projectId !== projects[0].id) return null;
    return {
      feature: contributions[0],
      contributions,
      concepts,
      files,
      architecture: learning.structure,
      mainFeatures: learning.features,
      languages: [
        ["Java", 72],
        ["HTML", 23],
        ["CSS", 5],
      ],
    };
  },
};
