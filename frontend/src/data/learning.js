export const learning = {
  features: [
    {
      title: "게시글 작성과 첨부 연결",
      description: "작성 폼이 제목·본문과 파일을 multipart/form-data 요청으로 전송하면 PostController가 PostForm의 입력을 검증합니다. PostService는 FileStorageService에 파일 저장을 맡기고, 반환된 첨부 정보를 게시글에 연결합니다. JPA가 Post와 Attachment를 MariaDB에 저장하며, 저장 후 상세 페이지로 리다이렉트해 새로고침에 의한 작성 요청 재전송을 줄입니다.",
      concept: "Post 하나에 Attachment 여러 개가 연결되는 일대다 관계입니다. JPA의 cascade는 연관된 첨부 엔티티를 함께 저장하지만 디스크 파일 저장은 서비스가 별도로 수행합니다.",
    },
    {
      title: "파일 저장과 다운로드",
      description: "FileStorageService는 원본 이름, 허용 확장자와 10MB 크기 제한을 검사한 뒤 UUID 이름으로 파일을 저장합니다. MariaDB에는 원본 이름·저장 이름·유형·크기만 보관합니다. 다운로드 시 AttachmentController가 첨부 ID로 이 정보를 조회하고, 저장 이름으로 파일을 찾아 응답합니다.",
      concept: "원본 이름과 서버의 저장 이름을 분리하면 같은 이름의 업로드가 충돌할 가능성을 줄일 수 있습니다. Content-Disposition의 attachment 값은 다운로드 처리와 원본 파일명을 전달합니다. 확장자 검사는 파일 내용 자체의 형식 검증과 별도로 이해해야 합니다.",
    },
    {
      title: "이미지 미리보기와 실패 처리",
      description: "Thymeleaf의 조건식은 JPEG·PNG·GIF 첨부에만 이미지 요소를 만듭니다. HTML을 받은 브라우저가 img의 src 주소로 별도 요청을 보내면, 서버는 이미지 유형을 확인하고 실제 파일을 반환합니다. Content-Type을 이미지 유형으로, Content-Disposition을 inline으로 설정하므로 상세 화면 안에서 사진을 볼 수 있습니다.",
      concept: "본문 HTML과 이미지 파일은 서로 다른 HTTP 응답입니다. 없는 첨부나 파일은 404, 지원하지 않는 미리보기 유형은 415로 처리하고, 화면은 이미지 로드 실패 시 안내 문구를 표시합니다.",
    },
  ],
  structure: [
    { id: "browser", title: "브라우저 · Thymeleaf 화면", file: "templates/posts/detail.html", code: "html/1",
      description: "게시글 요청을 받은 서버가 Thymeleaf로 HTML을 만듭니다. 브라우저는 HTML을 표시하고 img 주소로 이미지를 다시 요청합니다. 따라서 글을 보여주는 응답과 이미지 응답을 각각 추적해야 합니다." },
    { id: "controller", title: "요청 처리 · Controller", file: "PostController / AttachmentController", code: "java/1",
      description: "PostController는 게시글 데이터를 Model에 담아 템플릿 이름을 반환합니다. AttachmentController는 첨부 ID를 받아 파일 메타데이터를 조회하고 Content-Disposition과 이미지 유형을 포함한 파일 응답을 만듭니다. 첨부 컨트롤러는 Repository와 FileStorageService에 직접 의존합니다." },
    { id: "service", title: "처리 조율 · Service", file: "PostService / FileStorageService", code: "java/2",
      description: "PostService는 게시글과 첨부 정보 저장을 조율합니다. FileStorageService는 실제 파일 저장·조회·삭제와 경로 검사를 담당합니다. 게시글 작성 요청은 PostService를 거치며, 미리보기는 AttachmentController가 FileStorageService를 직접 호출합니다." },
    { id: "db", title: "Repository · MariaDB", file: "Post / Attachment 메타데이터", code: "java/1",
      description: "JPA Repository가 MariaDB에서 첨부 ID에 해당하는 행을 찾습니다. 행에는 원본 이름, UUID 저장 이름, 유형과 크기가 담깁니다. 이 저장 이름이 디스크 파일과 메타데이터를 연결하는 열쇠입니다." },
    { id: "disk", title: "파일 시스템 · uploads/", file: "실제 이미지·문서 바이트", code: "java/2",
      description: "FileStorageService가 UUID 저장 이름으로 디스크 경로를 찾고 업로드 폴더 안의 파일인지 검사합니다. 컨트롤러가 받은 Resource를 응답 본문으로 설정하면 Spring이 파일 바이트를 브라우저로 전송합니다." },
  ],
};

// Each card describes a distinct responsibility within the verified preview commit.
// IDs keep existing image-preview edits and notes accessible.
export const contributions = [
  {
    id: "image-preview", category: "서버 응답", title: "첨부 이미지 미리보기",
    summary: "이미지 유형을 확인하고 Content-Disposition과 이미지 응답 유형을 지정해 사진을 상세 화면 안에 표시했습니다.",
    technical: "AttachmentController.view()는 첨부 ID로 정보를 찾고 JPEG·PNG·GIF인지 검사합니다. 기존 FileStorageService.load()를 재사용해 경로와 파일 존재를 확인한 뒤 이미지 Content-Type과 inline 헤더를 설정합니다. 다운로드는 attachment를 사용하므로 같은 파일을 용도별로 전달할 수 있습니다. 없는 ID나 파일은 404, 지원하지 않는 유형은 415로 처리합니다.",
    code: "java/2",
    commit: "cb23fe8", author: "Safina-Noah", commitSummary: "미리보기 엔드포인트와 이미지 유형 검사, 파일 응답 추가",
  },
  {
    id: "preview-display", category: "상세 화면", title: "이미지 표시와 로드 실패 안내",
    summary: "Thymeleaf 조건식으로 이미지 첨부를 골라 표시하고, 파일을 읽을 수 없을 때 안내 문구가 나오도록 구성했습니다.",
    technical: "detail.html은 첨부 목록을 순회하고 JPEG·PNG·GIF에만 figure를 만듭니다. img의 src는 첨부 ID가 들어간 미리보기 주소입니다. 브라우저는 HTML 표시 후 별도로 이미지를 요청합니다. lazy 로딩을 지정했고 onerror에서는 이미지 요소를 숨기고 다음 안내 문구를 표시합니다. 원본 파일명은 대체 텍스트와 캡션으로 사용합니다.",
    code: "html/1",
    commit: "cb23fe8", author: "Safina-Noah", commitSummary: "상세 템플릿의 이미지 조건부 표시와 실패 안내 추가",
  },
  {
    id: "preview-validation", category: "동작 검증", title: "미리보기·다운로드 응답 검증",
    summary: "정상 이미지의 바이트와 Content-Disposition 헤더, 없는 파일과 지원하지 않는 유형의 응답을 테스트했습니다.",
    technical: "MockMvc는 실제 서버 포트 없이 컨트롤러에 HTTP 요청을 보내 결과를 확인합니다. 테스트는 임시 폴더의 파일과 모의 Repository를 사용합니다. JPEG·PNG·GIF의 정상 바이트와 Content-Type, inline 헤더를 검사하고, 없는 ID·파일·폴더 밖 경로는 404인지 확인합니다. PDF·TXT는 미리보기 415와 다운로드 성공을 함께 검사합니다. 임시 바이트로 HTTP 동작을 검증하므로 이미지 디코딩 자체는 별도 검증 범위입니다.",
    code: "test/0",
    commit: "cb23fe8", author: "Safina-Noah", commitSummary: "AttachmentControllerTest의 정상·오류·다운로드 회귀 검증 추가",
  },
];
