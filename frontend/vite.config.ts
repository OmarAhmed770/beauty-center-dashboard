import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  // Frontend-only Demo mode: existing APIs remain root-relative in both dev and build.
  define: mode === "demo-local" ? {
    "import.meta.env.VITE_API_BASE_URL": JSON.stringify("/"),
    "import.meta.env.VITE_DEMO_READ_ONLY": JSON.stringify("true"),
  } : undefined,
  server: mode === "demo-local" ? {
    host: "127.0.0.1",
    allowedHosts: ["volumes-unveiling-clutter.ngrok-free.dev"],
    port: 5175,
    strictPort: true,
    headers: { "X-Beauty-Demo-Frontend": "same-origin" },
    proxy: {
      "/api": { target: "http://127.0.0.1:8082", changeOrigin: false },
    },
  } : undefined,
  preview: mode === "demo-local" ? {
    host: "127.0.0.1",
    port: 5175,
    strictPort: true,
    proxy: {
      "/api": { target: "http://127.0.0.1:8082", changeOrigin: false },
    },
  } : undefined,
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.ts",
    css: true,
  },
}));
