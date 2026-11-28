import React, { useEffect, useState, useRef } from "react";
import {
  Github,
  Download,
  RefreshCw,
  Plus,
  LoaderCircle,
  ImagePlus,
} from "lucide-react";
import { Brand, Header, Cover } from "./Workspace";
import { Detail } from "./Analysis";
import { AnalysisProgress } from "./AnalysisProgress";
import { navigate, useRoute } from "./useWorkspace";
import { request, upload, adaptAnalysis } from "./api";

function ErrorMessage({ error }) {
  return error ? (
    <p className="warning" role="alert">
      {error}
    </p>
  ) : null;
}
function Login() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(
    location.hash.includes("error=")
      ? "GitHub 연결을 완료하지 못했어요. 다시 시도해주세요."
      : "",
  );
  useEffect(() => {
    request("/health")
      .then(setHealth)
      .catch((e) => setError(e.message));
  }, []);
  return (
    <main className="narrow">
      <div className="kicker">GITHUB CONNECT</div>
      <h1>내 프로젝트에서 시작하세요.</h1>
      <p>GitHub로 가입하고 직접 만든 코드를 다시 이해해 보세요.</p>
      <section className="panel form">
        <a className="btn primary" href="/oauth2/authorization/github">
          <Github size={18} />
          GitHub로 시작하기
        </a>
        {health && !health.githubConfigured && (
          <p>
            서버의 GitHub OAuth App과 토큰 암호화 키 설정이 필요해요. README의
            실행 설정을 확인하세요.
          </p>
        )}
        <ErrorMessage error={error} />
      </section>
    </main>
  );
}
function Profile({ profile, onDone, onLogout, loggingOut }) {
  const [name, setName] = useState(profile.name);
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  return (
    <main className="narrow">
      <h1>프로필 확인</h1>
      <p>GitHub 계정 {profile.login}에 연결됐어요.</p>
      <button className="btn soft small" disabled={loggingOut} onClick={onLogout}>로그아웃하고 다른 계정으로 연결</button>
      <form
        className="panel form"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError("");
          try {
            if (file) await upload(file);
            await request("/me", {
              method: "PATCH",
              body: JSON.stringify({ name }),
            });
            await onDone();
          } catch (e) {
            setError(e.message);
          } finally {
            setBusy(false);
          }
        }}
      >
        {profile.photo && (
          <img className="profile-photo" src={profile.photo} alt="프로필" />
        )}
        <label>
          닉네임
          <input
            required
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </label>
        <label>
          프로필 사진
          <input
            type="file"
            accept="image/png,image/jpeg,image/gif"
            onChange={(e) => setFile(e.target.files[0])}
          />
        </label>
        <ErrorMessage error={error} />
        <button className="btn primary" disabled={busy || !name.trim()}>
          {busy ? "저장 중…" : "계속하기"}
        </button>
      </form>
    </main>
  );
}
function RepositoryPicker({ projects, onDone, onLogout, loggingOut, login }) {
  const [items, setItems] = useState([]);
  const [selected, setSelected] = useState([]);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [more, setMore] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function load(next = 1) {
    setBusy(true);
    setError("");
    try {
      const r = await request(
        "/github/repositories?page=" + next + "&q=" + encodeURIComponent(query),
      );
      setItems((old) => (next === 1 ? r.items : [...old, ...r.items]));
      setPage(next);
      setMore(r.hasMore);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  useEffect(() => {
    load();
  }, []);
  return (
    <main className="narrow">
      <div className="kicker">REPOSITORIES</div>
      <h1>복습할 레포 선택</h1>
      <p>{login} 계정으로 접근 가능한 개인·팀 레포를 선택하세요.</p>
      <button className="btn soft small" disabled={loggingOut} onClick={onLogout}>로그아웃하고 다른 계정으로 연결</button>
      <section className="panel form">
        <details className="repo-dropdown">
          <summary>레포 선택 · {selected.length}개 선택됨</summary>
          <div className="repo-dropdown-menu">
            <form
              className="repo-search"
              onSubmit={(e) => {
                e.preventDefault();
                load();
              }}
            >
              <input
                aria-label="레포 검색"
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="소유자 또는 레포명"
              />
              <button className="btn soft small" disabled={busy}>
                검색
              </button>
            </form>
            <div className="repo-results">
              {items.map((r) => {
                const exists = projects.some(
                  (p) => p.owner === r.owner.login && p.title === r.name,
                );
                return (
                  <label className="repo-option" key={r.id}>
                    <input
                      type="checkbox"
                      disabled={exists}
                      checked={exists || selected.some((x) => x.id === r.id)}
                      onChange={() =>
                        setSelected((old) =>
                          old.some((x) => x.id === r.id)
                            ? old.filter((x) => x.id !== r.id)
                            : [...old, r],
                        )
                      }
                    />
                    <div>
                      <b>{r.full_name}</b>
                      <small>{r.description}</small>
                    </div>
                    <span>
                      {exists ? "등록됨" : r.private ? "비공개" : "공개"}
                    </span>
                  </label>
                );
              })}
            </div>
            {more && (
              <button
                className="btn soft small"
                disabled={busy}
                onClick={() => load(page + 1)}
              >
                더 보기
              </button>
            )}
            {!busy && !items.length && (
              <p>
                접근 가능한 레포가 없어요. GitHub OAuth 권한과 조직의 앱 접근
                승인 상태를 확인한 뒤 새로 불러오세요.
              </p>
            )}
          </div>
        </details>
        <p className="selected-repos">
          {selected.map((r) => r.full_name).join(", ")}
        </p>
        <button className="btn outline" disabled={busy} onClick={() => load()}>
          목록 새로 불러오기
        </button>
        <ErrorMessage error={error} />
        <button
          className="btn primary"
          disabled={busy || !selected.length || selected.length > 10}
          onClick={async () => {
            setBusy(true);
            setError("");
            try {
              await request("/projects", {
                method: "POST",
                body: JSON.stringify({
                  repositories: selected.map((r) => ({
                    owner: r.owner.login,
                    name: r.name,
                  })),
                }),
              });
              await onDone();
            } catch (e) {
              setError(e.message);
            } finally {
              setBusy(false);
            }
          }}
        >
          {busy ? "불러오는 중…" : selected.length + "개 등록하고 분석하기"}
        </button>
        {selected.length > 10 && <p>한 번에 최대 10개를 등록할 수 있어요.</p>}
        {projects.length > 0 && <a href="#/home">워크스페이스로 돌아가기</a>}
      </section>
    </main>
  );
}
function Jobs({ projects, refresh }) {
  return (
    <main className="narrow">
      <div className="kicker">PROJECT ANALYSIS</div>
      <h1>프로젝트를 분석하고 있어요.</h1>
      <p>다른 화면으로 이동해도 분석은 계속됩니다.</p>
      <div className="panel form">
        {projects.map((p) => (
          <div key={p.id} className="job-row">
            <strong>{p.title}</strong>
            <span>{p.job.stage || "대기"}</span>
            {p.job.status === "RUNNING" && (
              <LoaderCircle className="spin" size={18} />
            )}
            {p.full && <a href={"#/project/" + p.id}>{p.complete ? "완료된 분석 보기" : "완료된 항목 보기"}</a>}
            <AnalysisProgress job={p.job} onRefresh={refresh}/>
            {p.job.status === "FAILED" && (
              <p role="alert">{p.job.error_message}</p>
            )}
          </div>
        ))}
        <a className="btn primary" href="#/home">
          워크스페이스 보기
        </a>
      </div>
    </main>
  );
}
function ProjectCards({ projects, refresh, onError }) {
  const [pending, setPending] = useState("");
  const [filter, setFilter] = useState("전체");
  async function analyse(p) {
    setPending(p.id);
    try {
      await request("/projects/" + p.id + "/analyses", { method: "POST" });
      await refresh();
      navigate("jobs");
    } catch (e) {
      onError(e.message);
    } finally {
      setPending("");
    }
  }
  return (
    <main className="wrap">
      <section className="library">
        <div className="section-line">
          <div>
            <div className="kicker">PROJECT LIBRARY</div>
            <h1>프로젝트 라이브러리</h1>
            <p>내 코드와 커밋에서 면접을 준비하세요.</p>
          </div>
          <a className="btn soft" href="#/add">
            <Plus size={17} />
            레포 추가
          </a>
        </div>
        <div className="filter-row">
          <div className="seg">
            {["전체", "개인", "팀"].map((f) => (
              <button
                key={f}
                aria-pressed={filter === f}
                onClick={() => setFilter(f)}
              >
                {f}
              </button>
            ))}
          </div>
          <span>{projects.length}개 프로젝트</span>
        </div>
        <div className="cards">
          {projects
            .filter((p) => filter === "전체" || p.type === filter)
            .map((p) => (
              <article className="project-card" key={p.id}>
                {p.imageUrl ? (
                  <img
                    className="project-cover"
                    src={p.imageUrl}
                    alt={p.title + " 대표 이미지"}
                  />
                ) : (
                  <Cover p={p} />
                )}
                <div className="card-body">
                  <small>
                    {p.owner} / {p.type}
                  </small>
                  <h3>{p.title}</h3>
                  <p>{p.description}</p>
                  <p className="job-status">
                    {p.job.stage || "대기"}
                    {["RUNNING", "QUEUED"].includes(p.job.status) && (
                      <LoaderCircle size={15} className="spin" />
                    )}
                  </p>
                  {p.job.status === "FAILED" && (
                    <p className="job-error">{p.job.error_message}</p>
                  )}
                  {p.job.sections?.length > 0 && <p className="section-count">{p.job.sections.filter(s=>s.status==="COMPLETED").length} / {p.job.sections.length}개 항목 완료</p>}
                  <div className="card-actions">
                    {p.full && (
                      <a
                        className="btn primary small"
                        href={"#/project/" + p.id}
                      >
                        {p.complete ? "분석 보기" : "항목별 분석 보기"}
                      </a>
                    )}
                    {p.complete && (
                      <a
                        className="btn soft small"
                        href={"/api/projects/" + p.id + "/overview.md"}
                      >
                        <Download size={15} />
                        정리본 다운받기
                      </a>
                    )}
                    <button
                      className="btn soft small"
                      disabled={
                        pending === p.id ||
                        ["RUNNING", "QUEUED"].includes(p.job.status)
                      }
                      onClick={() => analyse(p)}
                    >
                      <RefreshCw size={15} />
                      {p.full ? "최신 코드로 재분석" : "다시 분석"}
                    </button>
                    <label className="image-upload">
                      <ImagePlus size={15} />
                      대표 이미지
                      <input
                        type="file"
                        accept="image/png,image/jpeg,image/gif"
                        onChange={async (e) => {
                          const file = e.target.files[0];
                          if (!file) return;
                          try {
                            await upload(file, p.id);
                            await refresh();
                          } catch (e) {
                            onError(e.message);
                          }
                          e.target.value = "";
                        }}
                      />
                    </label>
                  </div>
                </div>
              </article>
            ))}
        </div>
      </section>
    </main>
  );
}
function LiveDetail({ project, tab, fileId, rangeId, initialVersion = "", refresh }) {
  const [analysis, setAnalysis] = useState(null);
  const [version, setVersion] = useState(initialVersion);
  const [writings, setWritings] = useState({
    edits: {},
    notes: {},
    revisions: {},
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const saveState = useRef({ epoch: 0, busy: false });
  useEffect(() => {
    let active = true;
    setAnalysis(null);
    setError("");
    let done = false, inFlight = false;
    async function load() {
    if(inFlight) return;
    inFlight = true;
    const epoch = saveState.current.epoch;
    return request(
      "/projects/" +
        project.id +
        "/overview" +
        (version ? "?version=" + version : ""),
    )
      .then((response) => {
        if (!active) return;
        const next = adaptAnalysis(response, project.id);
        const values = { edits: {}, notes: {}, revisions: {} };
        response.writings.forEach((w) => {
          const key = project.id + "/" + w.feature_id;
          if (w.body !== null) values.edits[key] = w.body;
          if (w.note !== null) values.notes[key] = w.note;
          values.revisions[key] = w.revision;
        });
        if(epoch === saveState.current.epoch && !saveState.current.busy) setWritings(values);
        done = response.complete !== false;
        setError("");
        setAnalysis(next);
      })
      .catch((e) => {
        if (active) setError(e.message);
      }).finally(() => { inFlight = false; });
    }
    load();
    const timer = setInterval(() => { if (!done) load(); }, 5000);
    return () => { active=false; clearInterval(timer); };
  }, [project.id, version]);
  async function save(group, key, value) {
    if (saveState.current.busy) throw new Error("저장 중입니다.");
    saveState.current = { epoch: saveState.current.epoch + 1, busy: true };
    setSaving(true);
    try {
      const featureId = key.slice(project.id.length + 1);
      const result = await request(
        "/projects/" +
          project.id +
          "/features/" +
          encodeURIComponent(featureId) +
          (group === "notes" ? "/note" : ""),
        {
          method: group === "notes" ? "PUT" : "PATCH",
          body: JSON.stringify({
            analysisId: analysis.id,
            value,
            revision: writings.revisions[key] || 0,
          }),
        },
      );
      setWritings((old) => ({
        ...old,
        [group]: { ...old[group], [key]: value },
        revisions: { ...old.revisions, [key]: result.revision },
      }));
    } finally {
      saveState.current = { epoch: saveState.current.epoch + 1, busy: false };
      setSaving(false);
    }
  }
  return (
    <>
      <div className="wrap version-bar">
        <label>
          분석 버전
          <select
            disabled={!analysis || saving}
            value={version || analysis?.id || ""}
            onChange={(e) => setVersion(e.target.value)}
          >
            {analysis?.versions.map((v) => (
              <option key={v.id} value={v.id}>
                {String(v.created_at)} · {v.commit_sha.slice(0, 7)}{v.complete ? "" : " · 항목별 분석 중"}
              </option>
            ))}
          </select>
        </label>
        {analysis?.complete && (
          <a
            className="btn soft small"
            href={
              "/api/projects/" +
              project.id +
              "/overview.md?version=" +
              analysis.id
            }
          >
            이 버전 정리본 다운로드
          </a>
        )}
      </div>
      <div className="wrap">
        <ErrorMessage error={error} />
        {analysis && <AnalysisProgress job={analysis.job || {id:analysis.jobId,status:analysis.complete?"COMPLETED":"FAILED"}} sections={analysis.sections} onRefresh={refresh}/>}
      </div>
      {analysis ? (
        <Detail
          key={analysis.id}
          p={project}
          analysis={analysis}
          tab={tab}
          fileId={fileId}
          rangeId={rangeId}
          state={writings}
          onSaveWriting={save}
          notify={() => {}}
        />
      ) : (
        !error && (
          <main className="narrow">
            <LoaderCircle className="spin" />
            분석 내용을 불러오는 중…
          </main>
        )
      )}
    </>
  );
}

function Notes() {
  const [notes, setNotes] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    request("/notes")
      .then(setNotes)
      .catch((e) => setError(e.message));
  }, []);
  return (
    <main className="narrow">
      <h1>면접 메모</h1>
      <p>프로젝트별 분석 버전에 남긴 내 기록을 다시 읽어보세요.</p>
      <ErrorMessage error={error} />
      {notes.map((note) => (
        <section
          className="panel"
          key={note.analysis_id + "/" + note.feature_id}
        >
          <h2>{note.repo_name}</h2>
          <small>기준 커밋 {note.commit_sha.slice(0, 7)}</small>
          <p className="preserve">{note.note}</p>
          <a
            href={
              "#/project/" +
              note.project_id +
              "/feature/" +
              note.feature_id +
              "?version=" +
              note.analysis_id
            }
          >
            작성한 분석 버전 열기
          </a>
        </section>
      ))}
      {!notes.length && !error && <p>아직 작성한 메모가 없어요.</p>}
    </main>
  );
}

export default function LiveApp() {
  const route = useRoute();
  const [routePath, query] = route.split("?");
  const [page, id, tab, fileId, rangeId] = routePath.split("/");
  const requestedVersion =
    new URLSearchParams(query || "").get("version") || "";
  const [profile, setProfile] = useState(null);
  const [loggingOut, setLoggingOut] = useState(false);
  async function logout() {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await request("/logout", {method: "POST"});
      // A new page clears in-memory CSRF, profile, repository and analysis state together.
      window.location.replace("/#/setup");
      window.location.reload();
    } catch (e) {
      setError(e.message);
      setLoggingOut(false);
    }
  }
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  async function refresh() {
    const me = await request("/me");
    setProfile(me.authenticated ? me.profile : null);
    if (me.authenticated) setProjects(await request("/projects"));
    else setProjects([]);
  }
  useEffect(() => {
    refresh()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => {
    if (!profile?.profileReady) return;
    let active = true;
    const t = setInterval(
      () =>
        request("/projects")
          .then((p) => {
            if (active) setProjects(p);
          })
          .catch((e) => {
            if (active) setError(e.message);
          }),
      5000,
    );
    return () => {
      active = false;
      clearInterval(t);
    };
  }, [profile?.id, profile?.profileReady]);
  const effective = !profile
    ? "setup"
    : !profile.profileReady
      ? "profile"
      : !projects.length
        ? "select"
        : page;
  const project = projects.find((p) => p.id === id);
  let content;
  if (loading)
    content = (
      <main className="narrow">
        <LoaderCircle className="spin" />
        연결 정보를 확인하고 있어요.
      </main>
    );
  else if (!profile) content = <Login />;
  else if (!profile.profileReady || page === "setup")
    content = (
      <Profile
        profile={profile}
        onLogout={logout}
        loggingOut={loggingOut}
        onDone={async () => {
          await refresh();
          navigate(projects.length ? "home" : "select");
        }}
      />
    );
  else if (["select", "add"].includes(effective))
    content = (
      <RepositoryPicker
        key={effective}
        login={profile.login}
        onLogout={logout}
        loggingOut={loggingOut}
        projects={projects}
        onDone={async () => {
          await refresh();
          navigate("jobs");
        }}
      />
    );
  else if (page === "jobs") content = <Jobs projects={projects} refresh={refresh} />;
  else if (page === "project" && project?.full)
    content = (
      <LiveDetail
        key={id + requestedVersion}
        project={project}
        tab={tab}
        fileId={fileId}
        rangeId={rangeId}
        initialVersion={requestedVersion}
        refresh={refresh}
      />
    );
  else if (page === "project")
    content = <Jobs projects={project ? [project] : projects} refresh={refresh} />;
  else if (page === "notes") content = <Notes />;
  else
    content = (
      <ProjectCards projects={projects} refresh={refresh} onError={setError} />
    );
  return (
    <>
      <Header
        page={effective === "profile" ? "setup" : effective}
        profile={profile}
        onLogout={profile ? logout : undefined}
        loggingOut={loggingOut}
      />
      {error && (
        <div className="wrap">
          <ErrorMessage error={error} />
          <button
            className="btn soft small"
            onClick={() => {
              setError("");
              refresh().catch((e) => setError(e.message));
            }}
          >
            다시 연결
          </button>
        </div>
      )}
      {content}
      <footer>
        <div className="wrap">
          <Brand
            staticBrand={["setup", "profile", "select", "add"].includes(
              effective,
            )}
          />
          <span>프로젝트 복습 워크스페이스</span>
        </div>
      </footer>
    </>
  );
}
