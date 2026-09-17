import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideGitFork,
  LucidePencil,
  LucideCheck,
  LucideX,
} from '@lucide/angular';
import { DesignService } from '../../core/services/design.service';
import { AuthService } from '../../core/services/auth.service';
import { SecureImageComponent } from '../../shared/secure-image.component';
import { DesignDetail, DesignVersion } from '../../core/models/design.model';

@Component({
  selector: 'app-design-detail',
  standalone: true,
  imports: [
    RouterLink,
    SecureImageComponent,
    LucideChevronLeft,
    LucideChevronRight,
    LucideGitFork,
    LucidePencil,
    LucideCheck,
    LucideX,
  ],
  templateUrl: './design-detail.component.html',
})
export class DesignDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly designService = inject(DesignService);
  private readonly destroyRef = inject(DestroyRef);
  readonly authService = inject(AuthService);

  readonly detail = signal<DesignDetail | null>(null);

  readonly imageIndex = signal(0);
  readonly forking = signal(false);
  readonly reviewing = signal(false);
  readonly rejectMode = signal(false);
  readonly rejectComment = signal('');
  readonly actionError = signal<string | null>(null);

  constructor() {
    const subscription = this.route.paramMap.subscribe((params) => {
      this.loadDetail(Number(params.get('id')));
    });
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  private loadDetail(id: number): void {
    this.imageIndex.set(0);
    this.designService.detail(id).subscribe((detail) => this.detail.set(detail));
  }

  private reload(): void {
    const current = this.detail();
    if (current) {
      this.loadDetail(current.id);
    }
  }

  readonly displayVersion = computed<DesignVersion | null>(() => {
    const detail = this.detail();
    return detail?.publishedVersion ?? null;
  });

  readonly currentImage = computed(() => {
    const version = this.displayVersion();
    if (!version || version.images.length === 0) {
      return null;
    }
    const index = Math.min(this.imageIndex(), version.images.length - 1);
    return version.images[index];
  });

  imageUrl(imageId: number): string {
    return `/api/images/${imageId}`;
  }

  thumbUrl(imageId: number): string {
    return `/api/images/${imageId}/thumbnail`;
  }

  nextImage(): void {
    const version = this.displayVersion();
    if (!version) return;
    this.imageIndex.update((i) => (i + 1) % version.images.length);
  }

  prevImage(): void {
    const version = this.displayVersion();
    if (!version) return;
    this.imageIndex.update((i) => (i - 1 + version.images.length) % version.images.length);
  }

  selectImage(index: number): void {
    this.imageIndex.set(index);
  }

  edit(): void {
    const detail = this.detail();
    if (!detail) return;
    this.router.navigate(['/designs', detail.id, 'edit']);
  }

  fork(): void {
    const detail = this.detail();
    if (!detail) return;
    this.forking.set(true);
    this.designService.fork(detail.id).subscribe({
      next: (forkDetail) => {
        this.forking.set(false);
        this.router.navigate(['/designs', forkDetail.id, 'edit']);
      },
      error: () => this.forking.set(false),
    });
  }

  approve(): void {
    const detail = this.detail();
    if (!detail) return;
    this.reviewing.set(true);
    this.actionError.set(null);
    this.designService.approve(detail.id).subscribe({
      next: () => {
        this.reviewing.set(false);
        this.reload();
      },
      error: () => {
        this.reviewing.set(false);
        this.actionError.set('Freigabe fehlgeschlagen.');
      },
    });
  }

  startReject(): void {
    this.rejectMode.set(true);
    this.rejectComment.set('');
  }

  cancelReject(): void {
    this.rejectMode.set(false);
  }

  confirmReject(): void {
    const detail = this.detail();
    if (!detail || !this.rejectComment().trim()) return;
    this.reviewing.set(true);
    this.actionError.set(null);
    this.designService.reject(detail.id, this.rejectComment().trim()).subscribe({
      next: () => {
        this.reviewing.set(false);
        this.rejectMode.set(false);
        this.reload();
      },
      error: () => {
        this.reviewing.set(false);
        this.actionError.set('Ablehnung fehlgeschlagen.');
      },
    });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'Entwurf';
      case 'PENDING_REVIEW':
        return 'Wartet auf Prüfung';
      case 'PUBLISHED':
        return 'Veröffentlicht';
      case 'REJECTED':
        return 'Abgelehnt';
      case 'SUPERSEDED':
        return 'Ersetzt';
      default:
        return status;
    }
  }
}
