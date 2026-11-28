import React, { useEffect, useState } from "react";

import { BookOpen, Check, NotebookPen } from "lucide-react";
import { contributions } from "./data/learning";
import { projectApi } from "./data/projects";
import { Detail } from "./Analysis";
import { Brand, Header, Home, Library, SelectRepos, Setup } from "./Workspace";
import { navigate, useRoute, useWorkspace } from "./useWorkspace";
import "./style.css";

export default function DemoApp() {
  const [state, setState, storageError] = useWorkspace();
  const route = useRoute();
  const requestedPage = route.split("/")[0];
  const effectiveRoute = !state.profile
    ? "setup"
    : !state.registered.length && requestedPage !== "setup"
      ? "select"
      : route;
  const [page, id, tab, file, range] = effectiveRoute.split("/");
  useEffect(() => {
    if (effectiveRoute !== route) navigate(effectiveRoute);
  }, [effectiveRoute, route]);
  const [projects, setProjects] = useState([]);
  const [analysis, setAnalysis] = useState(null);
  const [message, setMessage] = useState("");
  useEffect(() => {
    projectApi.listProjects().then(setProjects);
  }, []);
  useEffect(() => {
    let active = true;
    if (page === "project")
      projectApi.getAnalysis(id).then((a) => {
        if (active) setAnalysis(a);
      });
    return () => {
      active = false;
    };
  }, [page, id]);
  useEffect(() => {
    if (
      page === "project" &&
      id === "week11_forked" &&
      ["feature", "code"].includes(tab)
    )
      setState((prev) =>
        prev.last === route ? prev : { ...prev, last: route },
      );
  }, [route, page, id, tab, setState]);
  useEffect(() => {
    if (message) {
      const t = setTimeout(() => setMessage(""), 2700);
      return () => clearTimeout(t);
    }
  }, [message]);
  const p = projects.find((item) => item.id === id);
  const notes = Object.entries(state.notes).filter(([, v]) => v.trim());
  const home = <Home projects={projects} state={state} />;
  let content = home;
  if (page === "gallery")
    content = (
      <main className="wrap">
        <Library projects={projects} state={state} standalone />
      </main>
    );
  if (page === "setup" || (["select", "add"].includes(page) && !state.profile))
    content = (
      <Setup
        profile={state.profile}
        onPhotoError={setMessage}
        onSave={(profile) => {
          setState((prev) => ({ ...prev, profile }));
          navigate(state.registered.length ? "add" : "select");
        }}
      />
    );
  if (["select", "add"].includes(page) && state.profile)
    content = (
      <SelectRepos
        key={page}
        projects={projects}
        state={state}
        add={page === "add"}
        onDone={(ids) => {
          setState((prev) => ({
            ...prev,
            registered: [...new Set([...prev.registered, ...ids])],
          }));
          navigate(page === "add" ? "gallery" : "home");
          setMessage("프로젝트를 등록했어요.");
        }}
      />
    );
  if (page === "project")
    content = p ? (
      <Detail
        p={p}
        analysis={analysis}
        tab={tab}
        fileId={file}
        rangeId={range}
        state={state}
        setState={setState}
        notify={setMessage}
      />
    ) : (
      <main className="wrap">
        <p>{projects.length ? "프로젝트를 찾을 수 없어요." : "불러오는 중…"}</p>
        <a href="#/gallery">갤러리로</a>
      </main>
    );
  if (page === "notes")
    content = (
      <main className="narrow">
        <div className="kicker">
          <NotebookPen size={16} />
          INTERVIEW NOTES
        </div>
        <h1>면접 메모</h1>
        <p>기능을 복습하며 남긴 나만의 답변을 다시 읽어보세요.</p>
        {notes.length ? (
          notes.map(([key, v]) => (
            <section className="panel" key={key}>
              <small>week11_forked</small>
              <h2>
                {contributions.find((f) => key.endsWith("/" + f.id))?.title ||
                  "첨부 이미지 미리보기"}
              </h2>
              <p className="preserve">{v}</p>
              <a href={"#/project/week11_forked/feature/" + key.split("/")[1]}>
                기능과 메모 열기
              </a>
            </section>
          ))
        ) : (
          <div className="panel">
            <h2>아직 작성한 메모가 없어요.</h2>
            <p>기능 심화 분석에서 첫 면접 메모를 작성해 보세요.</p>
            <a className="btn primary" href="#/project/week11_forked/feature">
              <BookOpen size={16} />
              샘플 기능 열기
            </a>
          </div>
        )}
      </main>
    );
  return (
    <>
      <Header
        page={page === "project" ? "gallery" : page}
        profile={state.profile}
      />
      {storageError && (
        <div role="alert" className="warning">
          브라우저 저장 공간을 사용할 수 없어 이번 변경은 현재 화면에서만
          유지돼요.
        </div>
      )}
      {content}
      <footer>
        <div className="wrap">
          <Brand staticBrand={["setup", "select", "add"].includes(page)} />
          <span>프로젝트 복습 워크스페이스</span>
        </div>
      </footer>
      {message && (
        <div role="status" className="toast">
          <Check size={17} />
          {message}
        </div>
      )}
    </>
  );
}
