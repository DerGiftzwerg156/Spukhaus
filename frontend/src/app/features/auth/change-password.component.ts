import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-spuk-black px-4">
      <div class="w-full max-w-sm rounded-lg border border-spuk-charcoal-light bg-spuk-charcoal p-8 shadow-xl">
        <h1 class="mb-2 font-display text-xl text-spuk-bone">Passwort ändern</h1>
        <p class="mb-6 text-sm text-spuk-fog">
          @if (mustChange) {
            Bitte lege beim ersten Login ein eigenes Passwort fest.
          } @else {
            Ändere hier dein Passwort.
          }
        </p>

        <form (ngSubmit)="submit()" class="flex flex-col gap-4">
          <div>
            <label class="mb-1 block text-sm text-spuk-fog" for="current">Aktuelles Passwort</label>
            <input
              id="current"
              name="current"
              type="password"
              autocomplete="current-password"
              class="w-full rounded border border-spuk-charcoal-light bg-spuk-black px-3 py-2 text-spuk-bone outline-none focus:border-spuk-red-bright"
              [(ngModel)]="currentPassword"
              required
            />
          </div>
          <div>
            <label class="mb-1 block text-sm text-spuk-fog" for="new">Neues Passwort (mind. 8 Zeichen)</label>
            <input
              id="new"
              name="new"
              type="password"
              autocomplete="new-password"
              minlength="8"
              class="w-full rounded border border-spuk-charcoal-light bg-spuk-black px-3 py-2 text-spuk-bone outline-none focus:border-spuk-red-bright"
              [(ngModel)]="newPassword"
              required
            />
          </div>
          <div>
            <label class="mb-1 block text-sm text-spuk-fog" for="confirm">Neues Passwort bestätigen</label>
            <input
              id="confirm"
              name="confirm"
              type="password"
              autocomplete="new-password"
              class="w-full rounded border border-spuk-charcoal-light bg-spuk-black px-3 py-2 text-spuk-bone outline-none focus:border-spuk-red-bright"
              [(ngModel)]="confirmPassword"
              required
            />
          </div>

          @if (error()) {
            <p class="text-sm text-spuk-red-glow">{{ error() }}</p>
          }

          <button
            type="submit"
            [disabled]="loading()"
            class="mt-2 rounded bg-spuk-red px-4 py-2 font-display text-spuk-bone transition hover:bg-spuk-red-bright disabled:opacity-50"
          >
            {{ loading() ? 'Speichern…' : 'Passwort speichern' }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class ChangePasswordComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  get mustChange(): boolean {
    return this.authService.mustChangePassword();
  }

  submit(): void {
    if (this.newPassword !== this.confirmPassword) {
      this.error.set('Die neuen Passwörter stimmen nicht überein.');
      return;
    }
    if (this.newPassword.length < 8) {
      this.error.set('Das neue Passwort muss mindestens 8 Zeichen lang sein.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/designs');
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Passwort konnte nicht geändert werden.');
      },
    });
  }
}
