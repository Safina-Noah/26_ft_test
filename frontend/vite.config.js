import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target = process.env.BACKEND_URL || env.BACKEND_URL || "http://localhost:8080";
  return {
    server: {
      proxy: Object.fromEntries(
        ["/api", "/oauth2", "/login"].map((path) => [
          path,
          { target, changeOrigin: true },
        ]),
      ),
    },
  };
});
