import { useEffect, useState } from "react";

const key = "reponote.workspace.v1";
const defaults = {
  profile: null,
  registered: [],
  last: null,
  edits: {},
  notes: {},
};
export function useWorkspace() {
  const [state, setState] = useState(() => {
    try {
      return { ...defaults, ...JSON.parse(localStorage.getItem(key)) };
    } catch {
      return defaults;
    }
  });
  const [storageError, setStorageError] = useState(false);
  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify(state));
      setStorageError(false);
    } catch {
      setStorageError(true);
    }
  }, [state]);
  return [state, setState, storageError];
}
export function navigate(path) {
  location.hash = "/" + path;
}
export function useRoute() {
  const read = () => location.hash.replace(/^#\/?/, "") || "home";
  const [route, setRoute] = useState(read);
  useEffect(() => {
    const update = () => {
      setRoute(read());
      window.scrollTo(0, 0);
    };
    window.addEventListener("hashchange", update);
    return () => window.removeEventListener("hashchange", update);
  }, []);
  return route;
}
