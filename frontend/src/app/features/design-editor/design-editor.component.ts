import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideStar, LucideTrash, LucideUpload } from '@lucide/angular';
import { DesignService } from '../../core/services/design.service';
import { DesignImage, DesignVersion } from '../../core/models/design.model';
import { SecureImageComponent } from '../../shared/secure-image.component';

@Component({
  selector: 'app-design-editor',
  standalone: true,
  imports: [FormsModule, RouterLink, SecureImageComponent, LucideUpload, LucideTrash, LucideStar],
  templateUrl: './design-editor.component.html',
})
export class DesignEditorComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly designService = inject(DesignService);

  readonly designId = signal<number | null>(null);
  readonly name = signal('');
  readonly description = signal('');
  readonly images = signal<DesignImage[]>([]);
  readonly version = signal<DesignVersion | null>(null);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly submitting = signal(false);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly dragOver = signal(false);

  readonly isCreateMode = computed(() => this.designId() === null);
  readonly canSubmit = computed(
    () => this.name().trim().length > 0 && this.description().trim().length > 0 && this.images().length > 0,
  );

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.designId.set(id);
      this.loadDraft(id);
    } else {
      this.loading.set(false);
    }
  }

  private loadDraft(id: number): void {
    this.loading.set(true);
    this.designService.startEditing(id).subscribe({
      next: (version) => {
        this.applyVersion(version);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Entwurf konnte nicht geladen werden.');
      },
    });
  }

  private applyVersion(version: DesignVersion): void {
    this.version.set(version);
    this.name.set(version.name);
    this.description.set(version.description);
    this.images.set([...version.images].sort((a, b) => a.sortOrder - b.sortOrder));
  }

  createDesign(): void {
    if (!this.name().trim() || !this.description().trim()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.designService.create(this.name().trim(), this.description().trim()).subscribe({
      next: (detail) => {
        this.saving.set(false);
        this.router.navigate(['/designs', detail.id, 'edit'], { replaceUrl: true });
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Design konnte nicht erstellt werden.');
      },
    });
  }

  saveDraft(): void {
    const id = this.designId();
    if (!id || !this.name().trim() || !this.description().trim()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.designService.updateDraft(id, this.name().trim(), this.description().trim()).subscribe({
      next: (version) => {
        this.saving.set(false);
        this.version.set(version);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Änderungen konnten nicht gespeichert werden.');
      },
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.uploadFiles(input.files);
      input.value = '';
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (event.dataTransfer?.files) {
      this.uploadFiles(event.dataTransfer.files);
    }
  }

  private uploadFiles(fileList: FileList): void {
    const id = this.designId();
    if (!id) return;
    this.uploading.set(true);
    this.error.set(null);
    const files = Array.from(fileList);
    let remaining = files.length;
    for (const file of files) {
      this.designService.addImage(id, file).subscribe({
        next: (image) => {
          this.images.update((imgs) => [...imgs, image]);
          remaining -= 1;
          if (remaining === 0) this.uploading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          remaining -= 1;
          if (remaining === 0) this.uploading.set(false);
          this.error.set(err.error?.message ?? `Bild "${file.name}" konnte nicht hochgeladen werden.`);
        },
      });
    }
  }

  removeImage(image: DesignImage): void {
    const id = this.designId();
    if (!id) return;
    this.designService.removeImage(id, image.id).subscribe({
      next: () => {
        this.images.update((imgs) => imgs.filter((i) => i.id !== image.id));
      },
      error: () => this.error.set('Bild konnte nicht entfernt werden.'),
    });
  }

  setPreview(image: DesignImage): void {
    const id = this.designId();
    if (!id) return;
    this.designService.setPreviewImage(id, image.id).subscribe({
      next: () => {
        this.images.update((imgs) => imgs.map((i) => ({ ...i, preview: i.id === image.id })));
      },
      error: () => this.error.set('Vorschaubild konnte nicht gesetzt werden.'),
    });
  }

  submitForReview(): void {
    const id = this.designId();
    if (!id || !this.canSubmit()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.designService.submit(id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/designs', id]);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(err.error?.message ?? 'Einreichung fehlgeschlagen.');
      },
    });
  }

  thumbUrl(imageId: number): string {
    return `/api/images/${imageId}/thumbnail`;
  }
}
