import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
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

export default api;
