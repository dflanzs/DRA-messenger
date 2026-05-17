import api from './client';
import type {
  AdminStats,
  GroupChat,
  Message,
  PrivateChat,
  TrustCircle,
  User,
  UserRole,
} from '../types';

export async function getStats(): Promise<AdminStats> {
  const [users, messages, groupChats, privateChats, trustCircles] = await Promise.all([
    getUsers(),
    getMessages(),
    getGroupChats(),
    getPrivateChats(),
    getTrustCircles(),
  ]);

  return {
    totalUsers: users.length,
    onlineUsers: users.filter((user) => user.onlineStatus).length,
    totalMessages: messages.length,
    totalGroupChats: groupChats.length,
    totalPrivateChats: privateChats.length,
    totalTrustCircles: trustCircles.length,
  };
}

export async function getUsers(): Promise<User[]> {
  const { data } = await api.get<User[]>('/api/users');
  return data;
}

export async function updateUserRole(userId: number, role: UserRole): Promise<User> {
  const { data } = await api.put<User>(`/api/users/${userId}/role`, { role });
  return data;
}

export async function deleteUser(userId: number): Promise<void> {
  await api.delete(`/api/users/${userId}`);
}

export async function getGroupChats(): Promise<GroupChat[]> {
  const { data } = await api.get<GroupChat[]>('/api/group-chats');
  return data;
}

export async function deleteGroupChat(groupChatId: number): Promise<void> {
  await api.delete(`/api/group-chats/${groupChatId}`);
}

export async function getPrivateChats(): Promise<PrivateChat[]> {
  const { data } = await api.get<PrivateChat[]>('/api/private-chats');
  return data;
}

export async function deletePrivateChat(chatId: number): Promise<void> {
  await api.delete(`/api/private-chats/${chatId}`);
}

export async function getMessages(): Promise<Message[]> {
  const { data } = await api.get<Message[]>('/api/messages');
  return data;
}

export async function deleteMessage(messageId: number): Promise<void> {
  await api.delete(`/api/messages/${messageId}`);
}

export async function getTrustCircles(): Promise<TrustCircle[]> {
  const { data } = await api.get<TrustCircle[]>('/api/trust-circles');
  return data;
}

export async function createTrustCircle(name: string, userIds: number[]): Promise<TrustCircle> {
  const { data } = await api.post<TrustCircle>('/api/trust-circles', {
    name,
    userIds,
  });
  return data;
}

export async function addUserToCircle(circleId: number, userId: number): Promise<TrustCircle> {
  const { data } = await api.post<TrustCircle>(
    `/api/trust-circles/${circleId}/users/${userId}`,
    {}
  );
  return data;
}

export async function removeUserFromCircle(circleId: number, userId: number): Promise<TrustCircle> {
  const { data } = await api.delete<TrustCircle>(
    `/api/trust-circles/${circleId}/users/${userId}`
  );
  return data;
}

export async function deleteTrustCircle(circleId: number): Promise<void> {
  await api.delete(`/api/trust-circles/${circleId}`);
}
