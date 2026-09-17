import { DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DesignService } from '../../core/services/design.service';
import { SecureImageComponent } from '../../shared/secure-image.component';
import { DesignVersion } from '../../core/models/design.model';

@Component({
  selector: 'app-review-queue',
  standalone: true,
  imports: [RouterLink, SecureImageComponent, DatePipe],
  templateUrl: './review-queue.component.html',
})
export class ReviewQueueComponent {
  private readonly designService = inject(DesignService);

  readonly queue = toSignal(this.designService.reviewQueue(), { initialValue: [] as DesignVersion[] });

  previewUrl(version: DesignVersion): string | null {
    const preview = version.images.find((i) => i.preview) ?? version.images[0];
    return preview ? `/api/images/${preview.id}/thumbnail` : null;
  }
}
