let csrf;
export async function request(path, options = {}) {
  const method = options.method || "GET";
  const headers = { ...options.headers };
  if (options.body && !(options.body instanceof FormData))
    headers["Content-Type"] = "application/json";
  if (!["GET", "HEAD"].includes(method)) {
    if (!csrf) csrf = await request("/csrf");
    headers[csrf.headerName] = csrf.token;
  }
  const response = await fetch("/api" + path, {
    ...options,
    headers,
    credentials: "include",
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    if (response.status === 403) csrf = undefined;
    throw new Error(
      error.message ||
        (response.status === 401
          ? "GitHub 로그인이 필요해요."
          : "요청을 처리하지 못했어요."),
    );
  }
  if (response.status === 204) return null;
  return response.json();
}
export async function upload(file, projectId) {
  const body = new FormData();
  body.append("file", file);
  if (projectId) body.append("projectId", projectId);
  return request("/images", { method: "POST", body });
}
export function adaptAnalysis(response, projectId) {
  const data = response.payload;
  const ranges = data.codeExplanations;
  const files = response.sources
    .map((source) => ({
      ...source,
      name: source.path.split("/").pop(),
      ranges: ranges.filter((r) => r.fileId === source.id),
    }))
    .filter((file) => file.ranges.length > 0);
  const linkFor = (ids) => {
    const range = ranges.find((r) => ids.includes(r.fileId));
    if (!range) return null;
    return (
      range.fileId +
      "/" +
      files.find((f) => f.id === range.fileId).ranges.indexOf(range)
    );
  };
  const contributions = data.contributions.map((f) => ({
    ...f,
    code: linkFor(f.evidenceIds),
    commit: f.commitShas.map((s) => s.slice(0, 7)).join(", "),
    author: "",
    commitSummary: "이 분석에 수집된 작성자 커밋을 근거로 정리했습니다.",
  }));
  const entries = Object.entries(response.languages);
  const total = entries.reduce((sum, [, n]) => sum + n, 0);
  return {
    ...response,
    projectId,
    overview: data.overview,
    mainFeatures: data.features,
    contributions,
    feature: contributions[0],
    files,
    interviewTips: data.interviewTips,
    architecture: data.architecture.nodes.map((n) => ({
      ...n,
      code: linkFor(n.evidenceIds),
    })),
    edges: data.architecture.edges,
    concepts: Object.fromEntries(
      data.concepts.map((c) => [
        c.name,
        [c.definition, c.application, c.question, c.answer],
      ]),
    ),
    languages: entries.map(([name, n]) => [
      name,
      total ? Math.round((n / total) * 1000) / 10 : 0,
    ]),
  };
}
