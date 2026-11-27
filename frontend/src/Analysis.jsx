import React, { useId, useRef, useState } from "react";
import {
  Boxes,
  Code2,
  Database,
  FileCode2,
  GitBranch,
  GitCommitHorizontal,
  Github,
  Layers3,
  Monitor,
  NotebookPen,
  Pencil,
  Server,
  Sparkles,
  X,
} from "lucide-react";
import { navigate } from "./useWorkspace";

const base = "project/week11_forked";
const sections = [["", "Overview"]];
export function Concept({ name, analysis, link = false }) {
  const data = analysis.concepts[name];
  const id = useId();
  const tip = useRef(null);
  function position(e) {
    const box = e.currentTarget.getBoundingClientRect();
    const width = Math.min(340, window.innerWidth - 32);
    Object.assign(tip.current.style, {
      width: width + "px",
      left: Math.min(box.left, window.innerWidth - width - 16) + "px",
      top: Math.min(box.bottom + 6, window.innerHeight * 0.42) + "px",
    });
  }
  return (
    <span className="concept">
      <button className="term" popoverTarget={id} onClick={position}>
        {name}
      </button>
      <span
        role="dialog"
        aria-label={name + " 개념 설명"}
        ref={tip}
        id={id}
        popover="auto"
        className="tip"
      >
        <span className="tip-title">
          <strong>{name}</strong>
          <button
            aria-label="개념 설명 닫기"
            popoverTarget={id}
            popoverTargetAction="hide"
          >
            <X size={16} />
          </button>
        </span>
        <span>{data[0]}</span>
        <b>프로젝트 적용</b>
        <span>{data[1]}</span>
        <details className="question">
          <summary>면접 꼬리 질문</summary>
          <strong>{data[2]}</strong>
          <p>{data[3]}</p>
        </details>
        {link && (
          <a href={"#/" + base + "/feature"}>
            관련 기능 · 첨부 이미지 미리보기
          </a>
        )}
      </span>
    </span>
  );
}
function ExplainedText({ text, analysis }) {
  const names = Object.keys(analysis.concepts).sort(
    (a, b) => b.length - a.length,
  );
  const parts = text.split(
    new RegExp(
      "(" +
        names
          .map((name) => name.replace(/[.*+?^$\{\}()|[\]\\]/g, "\\$&"))
          .join("|") +
        ")",
      "g",
    ),
  );
  return parts.map((part, i) =>
    analysis.concepts[part] ? (
      <Concept key={i} name={part} analysis={analysis} />
    ) : (
      part
    ),
  );
}
function Architecture({ analysis }) {
  const [selected, setSelected] = useState(analysis.architecture[0]);
  function node(id) {
    const item = analysis.architecture.find((n) => n.id === id);
    return (
      <button
        className="structure-node"
        aria-pressed={selected.id === id}
        onClick={() => setSelected(item)}
      >
        <strong>{item.title}</strong>
        <small>{item.file}</small>
      </button>
    );
  }
  return (
    <section className="panel architecture-panel">
      <div className="kicker">
        <GitBranch size={15} />
        ARCHITECTURE
      </div>
      <h2>프로그램 구조와 데이터 흐름</h2>
      <p>
        화면을 만드는 요청과 이미지를 가져오는 요청을 따라 각 계층의 역할을
        확인하세요.
      </p>
      <div
        className="structure-tree"
        aria-label="브라우저에서 서버와 저장소로 이어지는 구조도"
      >
        {node("browser")}
        <div className="tree-edge">게시글 요청 · 첨부 이미지 요청</div>
        {node("controller")}
        <div className="tree-edge">처리 위임</div>
        {node("service")}
        <div className="tree-edge">데이터 조회 · 파일 읽기</div>
        <div className="tree-branches">
          <div>{node("db")}</div>
          <div>{node("disk")}</div>
        </div>
      </div>
      <div className="structure-explanation" aria-live="polite">
        <h3>{selected.title}</h3>
        <p>
          <ExplainedText text={selected.description} analysis={analysis} />
        </p>
        <a href={"#/" + base + "/code/" + selected.code}>연결된 코드 보기</a>
      </div>
    </section>
  );
}
function Overview({ analysis }) {
  return (
    <div className="analysis-grid">
      <section className="panel wide">
        <div className="kicker">
          <Boxes size={15} />
          FEATURES
        </div>
        <h2>프로젝트 주요 기능</h2>
        <p>
          이 게시판은 Spring Boot 앱 하나에서 HTML 화면과 파일 응답을
          제공합니다. 게시글·첨부 정보는 DB에, 파일 본문은 서버 디스크에
          저장하며 첨부 ID로 둘을 연결합니다.
        </p>
        <div className="feature-explanations">
          {analysis.mainFeatures.map((f) => (
            <article className="feature-row" key={f.title}>
              <h3>{f.title}</h3>
              <p>
                <ExplainedText text={f.description} analysis={analysis} />
              </p>
              <p className="learning-point">
                <strong>이해할 개념</strong>{" "}
                <ExplainedText text={f.concept} analysis={analysis} />
              </p>
            </article>
          ))}
        </div>
      </section>
      <Architecture analysis={analysis} />
      <section className="panel">
        <div className="kicker">
          <Layers3 size={15} />
          TECH STACK
        </div>
        <h2>프로젝트 기술 스택</h2>
        <small>언어별 비중 · 샘플 비율</small>
        <div className="langbar">
          {analysis.languages.map(([name, value], i) => (
            <i
              className={"lang" + i}
              key={name}
              style={{ width: value + "%" }}
            />
          ))}
        </div>
        <div className="legends">
          {analysis.languages.map(([name, value], i) => (
            <span key={name}>
              <i className={"lang" + i} />
              {name} {value}%
            </span>
          ))}
        </div>
      </section>
      <section className="panel wide contribution-section">
        <div className="kicker">
          <Code2 size={15} />
          MY CONTRIBUTION
        </div>
        <h2>내가 구현한 기능</h2>
        <p>첨부 이미지 미리보기 구현을 화면·서버·검증 역할별로 살펴보세요.</p>
        <div className="contribution-cards">
          {analysis.contributions.map((f, index) => (
            <article className="contribution-card" key={f.id}>
              <small>
                구현 기능 {index + 1}: {f.category}
              </small>
              <h3>{f.title}</h3>
              <p>
                <ExplainedText text={f.summary} analysis={analysis} />
              </p>
              <div className="contribution-commit">
                <GitCommitHorizontal size={15} />
                {f.commit} · {f.author}
              </div>
              <a className="btn soft" href={"#/" + base + "/feature/" + f.id}>
                기능 심화 분석 보기
              </a>
            </article>
          ))}
        </div>
      </section>
      <section className="panel wide">
        <div className="kicker">
          <Sparkles size={15} />
          INTERVIEW TIPS
        </div>
        <h2>면접 어필 포인트</h2>
        <div className="twocol">
          <div>
            <h3>내가 구현한 부분</h3>
            <p>
              <ExplainedText
                analysis={analysis}
                text="Content-Disposition과 Content-Type을 함께 설정해 같은 파일을 미리보기와 다운로드로 제공한 과정을 설명해 보세요. 이미지 형식 검사와 실패 응답까지 코드로 연결하면 구현의 범위를 구체적으로 말할 수 있습니다."
              />
            </p>
            <a href={"#/" + base + "/code/java/2"}>미리보기 응답 확인</a>
          </div>
          <div>
            <h3>전체 설계에서 이해한 부분</h3>
            <p>
              <ExplainedText
                analysis={analysis}
                text="MariaDB에는 파일 메타데이터가, 디스크에는 실제 바이트가 있습니다. JPA의 엔티티 삭제가 디스크 파일까지 지우지는 않는 이유와, 두 저장소 사이에서 실패가 발생하면 무엇을 정리해야 하는지 설명해 보세요."
              />
            </p>
            <a href={"#/" + base + "/code/java/1"}>첨부 조회 코드 확인</a>
          </div>
        </div>
      </section>
    </div>
  );
}
function Editor({
  title,
  value,
  initial = "",
  onSave,
  memo = false,
  analysis,
}) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState("");
  const current = value ?? initial;
  return (
    <section className="editor">
      <div className="section-line">
        <h3>{title}</h3>
        {!editing && (
          <button
            className="text-button"
            onClick={() => {
              setDraft(current);
              setEditing(true);
            }}
          >
            <Pencil size={15} />
            {memo && !current ? "작성" : "수정"}
          </button>
        )}
      </div>
      {editing ? (
        <>
          <textarea
            aria-label={title}
            rows={memo ? 4 : 7}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
          />
          <div className="actions">
            <button
              className="btn primary small"
              onClick={() => {
                onSave(draft);
                setEditing(false);
              }}
            >
              저장
            </button>
            <button
              className="btn soft small"
              onClick={() => setEditing(false)}
            >
              취소
            </button>
          </div>
        </>
      ) : (
        <p className="preserve">
          {current ? (
            analysis ? (
              <ExplainedText text={current} analysis={analysis} />
            ) : (
              current
            )
          ) : memo ? (
            "면접에서 기억하고 싶은 표현과 질문을 기록하세요."
          ) : (
            "저장된 내용이 비어 있어요."
          )}
        </p>
      )}
    </section>
  );
}
export function Detail({
  p,
  analysis,
  tab,
  fileId,
  rangeId,
  state,
  setState,
  notify,
}) {
  const f =
    analysis?.contributions.find((item) => item.id === fileId) ||
    analysis?.feature;
  const key = p.id + "/" + f?.id;
  function save(group, value) {
    setState((prev) => ({
      ...prev,
      [group]: { ...prev[group], [key]: value },
    }));
    notify("저장했어요.");
  }
  return (
    <main className="wrap inner-page">
      <a href="#/gallery" className="crumb">
        프로젝트 갤러리 / {p.type} 레포
      </a>
      <div className="project-heading">
        <div className={"project-icon " + p.color}>
          <Github />
        </div>
        <div>
          <small>{p.owner}</small>
          <h1>{p.title}</h1>
        </div>
      </div>
      <p className="lede">{p.description}</p>
      {!p.full ? (
        <section className="panel">
          <h2>프로젝트 소개</h2>
          <p>
            {p.description}예요. 갤러리와 레포 선택을 체험하기 위한 샘플
            프로젝트입니다.
          </p>
          <div className="pills">
            {p.tags.map((t) => (
              <span key={t}>{t}</span>
            ))}
          </div>
          <a className="btn soft" href="#/gallery">
            갤러리로 돌아가기
          </a>
        </section>
      ) : !analysis ? (
        <p>분석을 불러오는 중…</p>
      ) : (
        <>
          <div className="tabbar">
            {sections.map(([id, t]) => (
              <a
                key={t}
                href={"#/" + base + (id ? "/" + id : "")}
                aria-current={(tab || "") === id ? "page" : undefined}
              >
                {t}
              </a>
            ))}
          </div>
          {tab === "code" ? (
            <Explorer analysis={analysis} fileId={fileId} rangeId={rangeId} />
          ) : tab === "feature" ? (
            <div className="analysis-grid">
              <section className="panel wide">
                <div
                  className="feature-switcher"
                  aria-label="내 구현 기능 선택"
                >
                  {analysis.contributions.map((item) => (
                    <a
                      key={item.id}
                      aria-current={item.id === f.id ? "page" : undefined}
                      href={"#/" + base + "/feature/" + item.id}
                    >
                      {item.title}
                    </a>
                  ))}
                </div>
                <div className="kicker">
                  <Scan />
                  FEATURE ANALYSIS
                </div>
                <h2>{f.title}</h2>
                <Editor
                  key={f.id}
                  analysis={analysis}
                  title="기능과 주요 기술 분석"
                  value={state.edits[key]}
                  initial={f.summary + "\n\n" + f.technical}
                  onSave={(v) => save("edits", v)}
                />
                <a
                  className="btn primary"
                  href={"#/" + base + "/code/" + f.code}
                >
                  <FileCode2 size={16} />
                  관련 코드 탐색
                </a>
              </section>
              <section className="panel">
                <div className="kicker">
                  <GitCommitHorizontal size={16} />
                  COMMIT
                </div>
                <h2>근거 커밋</h2>
                <code className="commit">{f.commit}</code>
                <p>{f.commitSummary}</p>
                <small>작성자 · {f.author}</small>
              </section>
              <section className="panel">
                <div className="kicker">
                  <NotebookPen size={16} />
                  INTERVIEW NOTE
                </div>
                <Editor
                  key={f.id}
                  title="내 면접 메모"
                  value={state.notes[key]}
                  memo
                  onSave={(v) => save("notes", v)}
                />
              </section>
            </div>
          ) : (
            <Overview analysis={analysis} />
          )}
        </>
      )}
    </main>
  );
}
function Scan() {
  return <Code2 size={16} />;
}
function Explorer({ analysis, fileId, rangeId }) {
  const file = analysis.files.find((f) => f.id === fileId) || analysis.files[0];
  const range =
    file.ranges[Number(rangeId)] || file.ranges[file.id === "html" ? 1 : 0];
  const lines = file.code
    .replace(/\r/g, "")
    .split("\n")
    .slice(range.from - 1, range.to);
  return (
    <section className="panel explorer">
      <div className="kicker">
        <FileCode2 size={16} />
        CODE EXPLORER
      </div>
      <h2>코드 탐색</h2>
      <div className="explore-controls">
        <label>
          관련 파일
          <select
            value={file.id}
            onChange={(e) => navigate(base + "/code/" + e.target.value + "/0")}
          >
            {analysis.files.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          코드 구간
          <select
            value={file.ranges.indexOf(range)}
            onChange={(e) =>
              navigate(base + "/code/" + file.id + "/" + e.target.value)
            }
          >
            {file.ranges.map((r, i) => (
              <option key={r.from} value={i}>
                {r.from}–{r.to}줄 · {r.title}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="codebox">
        <div>
          <FileCode2 size={15} />
          {file.path}
        </div>
        <pre>
          {lines.map((s, i) => (
            <span key={i}>
              <em>{range.from + i}</em>
              {s}
              {"\n"}
            </span>
          ))}
        </pre>
      </div>
      <h3>{range.title}</h3>
      <p>
        <ExplainedText text={range.text} analysis={analysis} />
      </p>
      <p className="code-context">
        <ExplainedText
          text={
            analysis.concepts[range.concept][1] +
            " " +
            range.concept +
            " 개념을 눌러 적용 사례와 면접 질문을 확인하세요."
          }
          analysis={analysis}
        />
      </p>
    </section>
  );
}
