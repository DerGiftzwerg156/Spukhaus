import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Role, TemporaryPasswordResponse, User } from '../models/user.model';

export interface CreateUserRequest {
  username: string;
  displayName: string;
  role: Role;
}

export interface UpdateUserRequest {
  displayName?: string;
  role?: Role;
  enabled?: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<User[]> {
    return this.http.get<User[]>('/api/users');
  }

  create(request: CreateUserRequest): Observable<TemporaryPasswordResponse> {
    return this.http.post<TemporaryPasswordResponse>('/api/users', request);
  }

  update(id: number, request: UpdateUserRequest): Observable<User> {
    return this.http.patch<User>(`/api/users/${id}`, request);
  }

  resetPassword(id: number): Observable<TemporaryPasswordResponse> {
    return this.http.post<TemporaryPasswordResponse>(`/api/users/${id}/reset-password`, {});
  }
}
