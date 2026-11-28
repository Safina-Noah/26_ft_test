import React, { Suspense, lazy } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
const App =
  import.meta.env.VITE_DEMO === "true"
    ? lazy(() => import("./DemoApp"))
    : lazy(() => import("./LiveApp"));
createRoot(document.getElementById("root")).render(
  <Suspense fallback={<main className="narrow">불러오는 중…</main>}>
    <App />
  </Suspense>,
);
