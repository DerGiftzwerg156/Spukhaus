import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginResponse, Role, User } from '../models/user.model';

const TOKEN_KEY = 'spukhaus.token';
const USER_KEY = 'spukhaus.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly userSignal = signal<User | null>(readStoredUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly currentUser = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly mustChangePassword = computed(() => this.userSignal()?.mustChangePassword ?? false);

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap((response) => this.setSession(response.token, response.user)),
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<User> {
    return this.http
      .post<User>('/api/auth/change-password', { currentPassword, newPassword })
      .pipe(tap((user) => this.setUser(user)));
  }

  refreshMe(): Observable<User> {
    return this.http.get<User>('/api/auth/me').pipe(tap((user) => this.setUser(user)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    this.router.navigateByUrl('/login');
  }

  hasAnyRole(...roles: Role[]): boolean {
    const user = this.userSignal();
    return !!user && roles.includes(user.role);
  }

  isAtLeastCreator(): boolean {
    return this.hasAnyRole('CREATOR', 'ADMIN', 'TECH_ADMIN');
  }

  isAtLeastAdmin(): boolean {
    return this.hasAnyRole('ADMIN', 'TECH_ADMIN');
  }

  private setSession(token: string, user: User): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenSignal.set(token);
    this.setUser(user);
  }

  private setUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.userSignal.set(user);
  }
}

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}
