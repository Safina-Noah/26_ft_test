import React, { useState } from "react";
import { LoaderCircle } from "lucide-react";
import { request } from "./api";

const labels = {PENDING:"대기 중",RUNNING:"분석 중",COMPLETED:"완료",FAILED:"실패",BLOCKED:"앞선 분석 완료 후 시작"};
export function AnalysisProgress({ job, onRefresh, sections = job?.sections || [] }) {
  const [busy,setBusy]=useState("");
  const [error,setError]=useState("");
  if(!sections.length) return null;
  const running=["QUEUED","RUNNING"].includes(job?.status);
  return <section className="analysis-progress" aria-label="항목별 분석 진행 상황">
    <p className="progress-intro">완료된 항목부터 확인할 수 있어요. 면접 포인트는 앞선 세 분석을 바탕으로 정리합니다.</p>
    <ul>{sections.map(s=><li key={s.section_key} className={"section-status status-"+s.status.toLowerCase()}>
      <div><strong>{s.title}</strong><span role="status">{s.status==="RUNNING" && <LoaderCircle size={14} className="spin"/>}{labels[s.status] || s.status}</span></div>
      {s.error_message && <p>{s.error_message}</p>}
      {s.status==="FAILED" && <button className="btn soft small" disabled={!!busy || running} onClick={async()=>{
        setBusy(s.section_key);setError("");
        try { await request("/analysis-jobs/"+job.id+"/sections/"+s.section_key+"/retry",{method:"POST"}); await onRefresh?.(); }
        catch(e) { setError(e.message); } finally {setBusy("");}
      }}>{busy===s.section_key ? "요청 중…" : "이 항목 다시 분석"}</button>}
    </li>)}</ul>
    {running && sections.some(s=>s.status==="FAILED") && <small>진행 중인 항목이 끝나면 실패한 항목을 다시 분석할 수 있어요.</small>}
    {error && <p className="warning" role="alert">{error}</p>}
  </section>;
}
