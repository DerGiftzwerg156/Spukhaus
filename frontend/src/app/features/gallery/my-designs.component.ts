import { toSignal } from '@angular/core/rxjs-interop';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideGitFork, LucidePlus } from '@lucide/angular';
import { DesignService } from '../../core/services/design.service';
import { SecureImageComponent } from '../../shared/secure-image.component';

@Component({
  selector: 'app-my-designs',
  standalone: true,
  imports: [RouterLink, SecureImageComponent, LucideGitFork, LucidePlus],
  templateUrl: './my-designs.component.html',
})
export class MyDesignsComponent {
  private readonly designService = inject(DesignService);
  private readonly router = inject(Router);

  readonly designs = toSignal(this.designService.mine(), { initialValue: [] });

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
      default:
        return status;
    }
  }

  statusClass(status: string): string {
    switch (status) {
      case 'PUBLISHED':
        return 'bg-green-900 text-green-200';
      case 'PENDING_REVIEW':
        return 'bg-amber-900 text-amber-200';
      case 'REJECTED':
        return 'bg-spuk-red/40 text-spuk-bone';
      default:
        return 'bg-spuk-charcoal-light text-spuk-fog';
    }
  }

  createNew(): void {
    this.router.navigate(['/designs/new']);
  }
}
