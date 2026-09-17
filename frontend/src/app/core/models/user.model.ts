export type Role = 'TECH_ADMIN' | 'ADMIN' | 'CREATOR' | 'USER';

export interface User {
  id: number;
  username: string;
  displayName: string;
  role: Role;
  enabled: boolean;
  mustChangePassword: boolean;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface TemporaryPasswordResponse {
  user: User;
  temporaryPassword: string;
}
