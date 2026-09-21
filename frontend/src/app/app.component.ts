import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideGhost, LucideMenu, LucideX } from '@lucide/angular';
import { AuthService } from './core/services/auth.service';
import {NgOptimizedImage} from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideGhost, LucideMenu, LucideX, NgOptimizedImage],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  readonly title = 'Das Spukhaus';
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly menuOpen = signal(false);

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.closeMenu();
    this.authService.logout();
  }

  get showChrome(): boolean {
    return !['/login', '/change-password'].includes(this.router.url.split('?')[0]);
  }
}
