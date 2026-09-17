import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideGhost } from '@lucide/angular';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, LucideGhost],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-spuk-black px-4">
      <div class="w-full max-w-sm rounded-lg border border-spuk-charcoal-light bg-spuk-charcoal p-8 shadow-xl">
        <div class="mb-6 flex flex-col items-center gap-2 text-center">
          <svg lucideGhost class="h-12 w-12 text-spuk-red-bright"></svg>
          <h1 class="font-display text-2xl text-spuk-bone">Spukhaus</h1>
          <p class="text-sm text-spuk-fog">Maskendesign-Verwaltung</p>
        </div>

        <form (ngSubmit)="submit()" class="flex flex-col gap-4">
          <div>
            <label class="mb-1 block text-sm text-spuk-fog" for="username">Benutzername</label>
            <input
              id="username"
              name="username"
              type="text"
              autocomplete="username"
              class="w-full rounded border border-spuk-charcoal-light bg-spuk-black px-3 py-2 text-spuk-bone outline-none focus:border-spuk-red-bright"
              [(ngModel)]="username"
              required
            />
          </div>
          <div>
            <label class="mb-1 block text-sm text-spuk-fog" for="password">Passwort</label>
            <input
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
              class="w-full rounded border border-spuk-charcoal-light bg-spuk-black px-3 py-2 text-spuk-bone outline-none focus:border-spuk-red-bright"
              [(ngModel)]="password"
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
            {{ loading() ? 'Anmelden…' : 'Anmelden' }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  username = '';
  password = '';
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    if (!this.username || !this.password) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.user.mustChangePassword) {
          this.router.navigateByUrl('/change-password');
          return;
        }
        const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo') ?? '/designs';
        this.router.navigateByUrl(redirectTo);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(
          err.status === 401 ? 'Benutzername oder Passwort ist falsch.' : 'Anmeldung fehlgeschlagen.',
        );
      },
    });
  }
}
