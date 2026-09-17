import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideKeyRound, LucideUserPlus } from '@lucide/angular';
import { AuthService } from '../../core/services/auth.service';
import { CreateUserRequest, UserService } from '../../core/services/user.service';
import { Role, User } from '../../core/models/user.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [FormsModule, LucideUserPlus, LucideKeyRound],
  templateUrl: './user-management.component.html',
})
export class UserManagementComponent {
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);

  readonly users = signal<User[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);
  readonly lastTemporaryPassword = signal<{ username: string; password: string } | null>(null);

  private readonly allRoles: Role[] = ['USER', 'CREATOR', 'ADMIN', 'TECH_ADMIN'];

  /** Nur ein TechAdmin darf die Rolle TechAdmin vergeben (Rollen-Hierarchie: Admin < TechAdmin). */
  get roles(): Role[] {
    return this.authService.currentUser()?.role === 'TECH_ADMIN'
      ? this.allRoles
      : this.allRoles.filter((role) => role !== 'TECH_ADMIN');
  }

  newUsername = '';
  newDisplayName = '';
  newRole: Role = 'CREATOR';

  constructor() {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.userService.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  createUser(): void {
    if (!this.newUsername.trim() || !this.newDisplayName.trim()) {
      return;
    }
    const request: CreateUserRequest = {
      username: this.newUsername.trim(),
      displayName: this.newDisplayName.trim(),
      role: this.newRole,
    };
    this.creating.set(true);
    this.error.set(null);
    this.userService.create(request).subscribe({
      next: (response) => {
        this.creating.set(false);
        this.lastTemporaryPassword.set({ username: response.user.username, password: response.temporaryPassword });
        this.newUsername = '';
        this.newDisplayName = '';
        this.newRole = 'CREATOR';
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.creating.set(false);
        this.error.set(err.error?.message ?? 'Benutzer konnte nicht angelegt werden.');
      },
    });
  }

  changeRole(user: User, role: Role): void {
    this.userService.update(user.id, { role }).subscribe({
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => this.error.set(err.error?.message ?? 'Rolle konnte nicht geändert werden.'),
    });
  }

  toggleEnabled(user: User): void {
    this.userService.update(user.id, { enabled: !user.enabled }).subscribe({
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => this.error.set(err.error?.message ?? 'Status konnte nicht geändert werden.'),
    });
  }

  resetPassword(user: User): void {
    this.userService.resetPassword(user.id).subscribe({
      next: (response) => {
        this.lastTemporaryPassword.set({ username: response.user.username, password: response.temporaryPassword });
        this.replaceUser(response.user);
      },
      error: () => this.error.set('Passwort konnte nicht zurückgesetzt werden.'),
    });
  }

  private replaceUser(updated: User): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
  }

  isSelf(user: User): boolean {
    return this.authService.currentUser()?.id === user.id;
  }

  /** Ein einfacher Admin darf keinen TechAdmin-Account verändern (Rollen-Hierarchie: Admin < TechAdmin). */
  canManageTarget(user: User): boolean {
    return user.role !== 'TECH_ADMIN' || this.authService.currentUser()?.role === 'TECH_ADMIN';
  }
}
