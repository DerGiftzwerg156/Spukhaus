import { HttpClient } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';

/**
 * Bilder benötigen den Authorization-Header (JWT), den ein normales <img src="..">
 * nicht mitschicken kann. Deshalb wird das Bild per HttpClient geladen und als
 * Object-URL gerendert.
 */
@Component({
  selector: 'app-secure-image',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (objectUrl(); as url) {
      <img [src]="url" [alt]="alt()" [class]="imgClass()" />
    } @else if (loading()) {
      <div [class]="imgClass() + ' animate-pulse bg-spuk-charcoal-light'"></div>
    } @else {
      <div [class]="imgClass() + ' flex items-center justify-center bg-spuk-charcoal-light text-spuk-fog/50 text-xs'">
        kein Bild
      </div>
    }
  `,
})
export class SecureImageComponent {
  readonly src = input<string | null>(null);
  readonly alt = input<string>('');
  readonly imgClass = input<string>('');

  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly objectUrl = signal<string | null>(null);
  readonly loading = signal(false);

  constructor() {
    effect((onCleanup) => {
      const url = this.src();
      this.objectUrl.set(null);
      if (!url) {
        return;
      }
      this.loading.set(true);
      let currentObjectUrl: string | null = null;
      const subscription = this.http.get(url, { responseType: 'blob' }).subscribe({
        next: (blob) => {
          currentObjectUrl = URL.createObjectURL(blob);
          this.objectUrl.set(currentObjectUrl);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
      onCleanup(() => {
        subscription.unsubscribe();
        if (currentObjectUrl) {
          URL.revokeObjectURL(currentObjectUrl);
        }
      });
    });

    this.destroyRef.onDestroy(() => {
      const url = this.objectUrl();
      if (url) {
        URL.revokeObjectURL(url);
      }
    });
  }
}
