import axios from 'axios';

const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const api = axios.create({
  baseURL: apiUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const requestUrl = config.url ?? '';
  const isPublicAuthEndpoint = requestUrl.startsWith('/api/auth/login')
    || requestUrl.startsWith('/api/auth/register')
    || requestUrl.startsWith('/api/auth/verify-email');

  if (isPublicAuthEndpoint) {
    return config;
  }

  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token');
      localStorage.removeItem('admin_user');
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

export default api;
