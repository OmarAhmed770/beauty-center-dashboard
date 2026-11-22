import axios from "axios";
import { clearAccessToken, readAccessToken } from "@/lib/auth-token";
import { ApiClientError } from "@/lib/api/errors";
import { demoReadOnly, demoReadOnlyMessage, isDemoWrite } from "@/lib/demo-mode";

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

export const apiBaseUrl = (configuredBaseUrl || (import.meta.env.DEV ? "http://localhost:8080" : window.location.origin)).replace(/\/$/, "");

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15_000,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  if (demoReadOnly && isDemoWrite(config.method, config.url, config.data)) {
    throw new ApiClientError(demoReadOnlyMessage, { status: 403, code: "DEMO_READ_ONLY" });
  }
  const token = readAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && readAccessToken()) {
      clearAccessToken();
      window.dispatchEvent(new CustomEvent("beauty-center:unauthorized"));
    }
    return Promise.reject(error);
  },
);
