export type UserRole = 'ADMIN' | 'USER';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  publicKey: string;
  onlineStatus: boolean;
  role: UserRole;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}

export interface User {
  id: number;
  name: string;
  email: string;
  publicKey: string;
  onlineStatus: boolean;
  role: UserRole;
  createdAt: string;
}

export interface GroupChat {
  id: number;
  userIds: number[];
}

export interface PrivateChat {
  id: number;
  senderId: number;
  receiverId: number;
  userIds: number[];
}

export interface Message {
  id: number;
  sender: User;
  oneToOneChat: PrivateChat | null;
  groupChat: GroupChat | null;
  content: string;
  createdAt: string;
  read: boolean;
}

export interface TrustCircle {
  id: number;
  name: string;
  domainKeyId: string;
  consentDomain: boolean;
  members: User[];
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface AdminStats {
  totalUsers: number;
  onlineUsers: number;
  totalMessages: number;
  totalGroupChats: number;
  totalPrivateChats: number;
  totalTrustCircles: number;
}
