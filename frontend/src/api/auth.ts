import api from './client';
import type { AuthResponse } from '../types';

interface LoginDto {
  email: string;
  password: string;
}

interface RegisterDto {
  name: string;
  email: string;
  password: string;
}

interface VerifyEmailDto {
  token: string;
}

export async function login(data: LoginDto): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>('/api/auth/login', data);
  return response.data;
}

export async function register(data: RegisterDto): Promise<{ message: string }> {
  const response = await api.post<{ message: string }>('/api/auth/register', data);
  return response.data;
}

export async function verifyEmail(data: VerifyEmailDto): Promise<{ message: string }> {
  const response = await api.post<{ message: string }>('/api/auth/verify-email', data);
  return response.data;
}

export async function logout(): Promise<void> {
  await api.post('/api/auth/logout');
}
