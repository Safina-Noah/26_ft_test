import React, { useState } from "react";
import {
  BookOpen,
  CalendarDays,
  ChevronDown,
  Search,
  Code2,
  FolderGit2,
  GitBranch,
  GitCommitHorizontal,
  Github,
  Image,
  Layers3,
  NotebookPen,
  Plus,
  ScanLine,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { navigate } from "./useWorkspace";

export function Brand({ staticBrand = false }) {
  const Tag = staticBrand ? "span" : "a";
  return (
    <Tag
      href={staticBrand ? undefined : "#/home"}
      className="brand"
      aria-label={staticBrand ? "RepoNote" : "RepoNote 홈"}
    >
      <span className="brandmark">
        <Layers3 size={23} strokeWidth={2.5} />
      </span>
      RepoNote<span className="branddot">.</span>
    </Tag>
  );
}
export function Header({ page, profile }) {
  const onboarding = ["setup", "select", "add"].includes(page);
  return (
    <header className="top">
      <div className={"top-inner" + (onboarding ? " onboarding-top" : "")}>
        <Brand staticBrand={onboarding} />
        {!onboarding && (
          <>
            <nav aria-label="주 메뉴">
              {[
                ["home", "워크스페이스"],
                ["notes", "면접 메모"],
              ].map(([key, label]) => (
                <a
                  key={key}
                  className={page === key ? "active" : ""}
                  href={"#/" + key}
                >
                  {label}
                </a>
              ))}
            </nav>
            <a className="account" href="#/setup">
              <Github size={17} />
              <span>{profile?.name || "GitHub 연결"}</span>
            </a>
          </>
        )}
      </div>
    </header>
  );
}
export function Cover({ p }) {
  const Icon = { image: Image, calendar: CalendarDays, journal: NotebookPen }[
    p.art
  ];
  return (
    <div className={"cover " + p.color} aria-hidden="true">
      <div className="cover-grid" />
      <div className="tile tile-shadow" />
      <div className="tile tile-face">
        <Icon size={44} strokeWidth={1.6} />
      </div>
      <span className="cover-tag">
        {p.art === "image"
          ? "IMAGE PREVIEW"
          : p.art === "calendar"
            ? "CALENDAR"
            : "DEV JOURNAL"}
      </span>
    </div>
  );
}
export function Cards({ items }) {
  return (
    <div className="cards">
      {items.map((p) => (
        <a className="project-card" href={"#/project/" + p.id} key={p.id}>
          <Cover p={p} />
          <div className="card-body">
            <small className="owner">
              <Github size={14} />
              {p.owner} <span> / </span> {p.type} 레포
            </small>
            <h3>{p.title}</h3>
            <p>{p.description}</p>
            <div className="pills">
              {p.tags.map((tag) => (
                <span key={tag}>{tag}</span>
              ))}
            </div>
            <div className="card-foot">
              {p.full ? (
                <>
                  <ScanLine size={15} />
                  분석 보기
                </>
              ) : (
                <>
                  <BookOpen size={15} />
                  샘플 · 소개만 보기
                </>
              )}
            </div>
          </div>
        </a>
      ))}
    </div>
  );
}
export function Library({ projects, state, standalone = false }) {
  const [filter, setFilter] = useState("전체");
  const registered = projects.filter((p) => state.registered.includes(p.id));
  const pool = registered.length ? registered : projects;
  const items = pool.filter((p) => filter === "전체" || p.type === filter);
  return (
    <section className="library">
      <div className="section-line">
        <div>
          <div className="kicker">
            <span className="line" />
            PROJECT LIBRARY
          </div>
          <h2>{standalone ? "프로젝트 갤러리" : "프로젝트 라이브러리"}</h2>
          <p>
            {registered.length
              ? "등록한 프로젝트에서 복습할 기능을 선택하세요."
              : "샘플 프로젝트를 열어 코드와 기여 기록을 살펴보세요."}
          </p>
        </div>
        <button
          className="soft btn"
          onClick={() => navigate(state.profile ? "add" : "setup")}
        >
          <Plus size={17} />
          레포 추가
        </button>
      </div>
      <div className="filter-row">
        <div className="seg">
          {["전체", "개인", "팀"].map((x) => (
            <button
              key={x}
              aria-pressed={filter === x}
              onClick={() => setFilter(x)}
            >
              {x}
              {x === "전체" && <small>{pool.length}</small>}
            </button>
          ))}
        </div>
        <span className="count">{items.length}개 프로젝트</span>
      </div>
      {items.length ? (
        <Cards items={items} />
      ) : (
        <p className="empty">이 유형으로 등록한 레포가 없어요.</p>
      )}
    </section>
  );
}
export function Home({ projects, state }) {
  return (
    <main className="wrap">
      <Library projects={projects} state={state} />
    </main>
  );
}
export function Setup({ profile, onSave, onPhotoError }) {
  const [name, setName] = useState(profile?.name || "Safina-Noah");
  const [photo, setPhoto] = useState(profile?.photo || "");
  const [token, setToken] = useState("");
  const [connected, setConnected] = useState(Boolean(profile));
  async function pickPhoto(file) {
    if (!file) return;
    if (
      !["image/png", "image/jpeg", "image/webp"].includes(file.type) ||
      file.size > 512000
    ) {
      onPhotoError("500KB 이하의 PNG, JPEG 또는 WebP 사진을 선택하세요.");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setPhoto(String(reader.result));
    reader.readAsDataURL(file);
  }
  return (
    <main className="narrow">
      <div className="kicker">
        <Github size={16} />
        GITHUB CONNECT
      </div>
      <h1>내 계정 연결</h1>
      <p>샘플 프로필을 설정하고 복습할 레포를 선택하세요.</p>
      <form
        className="panel form"
        onSubmit={(e) => {
          e.preventDefault();
          onSave({ name: name.trim(), photo, login: "Safina-Noah" });
        }}
      >
        <div className="profile-row">
          <div className="avatar">
            {photo ? <img src={photo} alt="프로필" /> : <UserRound />}
          </div>
          <label className="upload">
            프로필 사진 선택
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(e) => pickPhoto(e.target.files[0])}
            />
          </label>
        </div>
        <label>
          닉네임
          <input
            value={name}
            maxLength={32}
            required
            onChange={(e) => setName(e.target.value)}
          />
        </label>
        <label>
          GitHub 토큰
          <input
            type="password"
            readOnly
            value={token}
            placeholder="아래 버튼으로 예시 토큰을 채우세요"
          />
        </label>
        <details>
          <summary>토큰 등록 가이드</summary>
          <p>
            이 데모는 예시 토큰으로 연결 과정을 재현해요. ‘예시 토큰 채우기’를
            누른 뒤 ‘연결하기’를 선택하세요.
          </p>
        </details>
        <div className="actions">
          <button
            type="button"
            className="btn soft"
            onClick={() => {
              setToken("demo-reponote-sample");
              setConnected(false);
            }}
          >
            예시 토큰 채우기
          </button>
          <button
            type="button"
            className="btn outline"
            disabled={!token}
            onClick={() => setConnected(true)}
          >
            연결하기
          </button>
        </div>
        {connected && (
          <div className="success">
            <ShieldCheck size={19} />
            연결된 샘플 계정 · Safina-Noah
          </div>
        )}
        <button disabled={!connected || !name.trim()} className="btn primary">
          레포 선택으로
        </button>
      </form>
    </main>
  );
}
export function SelectRepos({ projects, state, add, onDone }) {
  const [selected, setSelected] = useState([]);
  const [query, setQuery] = useState("");
  const visible = projects.filter((p) =>
    (p.owner + "/" + p.title)
      .toLowerCase()
      .includes(query.trim().toLowerCase()),
  );
  function toggle(id) {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  }
  return (
    <main className="narrow">
      <div className="kicker">
        <FolderGit2 size={16} />
        REPOSITORIES
      </div>
      <h1>{add ? "레포 추가" : "복습할 레포 선택"}</h1>
      <p>개인 레포와 참여한 팀 레포를 함께 등록할 수 있어요.</p>
      <div className="panel form">
        <label id="repo-picker-label">GitHub 레포지토리</label>
        <details className="repo-dropdown">
          <summary aria-labelledby="repo-picker-label repo-picker-value">
            <span id="repo-picker-value">
              {selected.length
                ? selected.length + "개 레포 선택됨"
                : "레포를 선택하세요"}
            </span>
            <ChevronDown size={18} />
          </summary>
          <div className="repo-dropdown-menu">
            <label className="repo-search">
              <Search size={17} />
              <input
                type="search"
                aria-label="레포 검색"
                placeholder="소유자 또는 레포명 검색"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </label>
            <div className="repo-results">
              {visible.length ? (
                visible.map((p) => {
                  const exists = state.registered.includes(p.id);
                  return (
                    <label className="repo-option" key={p.id}>
                      <input
                        type="checkbox"
                        checked={exists || selected.includes(p.id)}
                        disabled={exists}
                        onChange={() => toggle(p.id)}
                      />
                      <div>
                        <b>
                          {p.owner}/{p.title}
                        </b>
                        <small>{p.description}</small>
                      </div>
                      <span>{exists ? "등록됨" : p.type}</span>
                    </label>
                  );
                })
              ) : (
                <p className="repo-no-results">검색 결과가 없어요.</p>
              )}
            </div>
          </div>
        </details>
        {selected.length > 0 && (
          <p className="selected-repos">
            {projects
              .filter((p) => selected.includes(p.id))
              .map((p) => p.owner + "/" + p.title)
              .join(", ")}
          </p>
        )}
        <button
          className="btn primary"
          disabled={!selected.length}
          onClick={() => onDone(selected)}
        >
          {selected.length}개 프로젝트 등록
        </button>
        {state.registered.length > 0 && (
          <a href="#/home">워크스페이스로 돌아가기</a>
        )}
      </div>
    </main>
  );
}
