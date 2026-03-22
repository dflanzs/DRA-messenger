import api from './client';
import type { AuthResponse } from '../types';

interface LoginDto {
  email: string;
  password: string;
}

export async function login(data: LoginDto): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>('/api/auth/login', data);
  return response.data;
}

export async function logout(): Promise<void> {
  await api.post('/api/auth/logout');
}
