import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideGitFork, LucideSearch } from '@lucide/angular';
import { combineLatest, debounceTime, distinctUntilChanged, Subject, startWith, switchMap } from 'rxjs';
import { DesignService } from '../../core/services/design.service';
import { PageResponse, DesignSummary } from '../../core/models/design.model';
import { SecureImageComponent } from '../../shared/secure-image.component';

const EMPTY_PAGE: PageResponse<DesignSummary> = { content: [], page: 0, size: 24, totalElements: 0, totalPages: 0 };

@Component({
  selector: 'app-gallery',
  standalone: true,
  imports: [FormsModule, RouterLink, LucideSearch, LucideGitFork, SecureImageComponent],
  templateUrl: './gallery.component.html',
})
export class GalleryComponent {
  private readonly designService = inject(DesignService);

  searchTerm = '';
  readonly page = signal(0);
  private readonly searchSubject = new Subject<string>();

  readonly result = toSignal(
    combineLatest([
      this.searchSubject.pipe(startWith(''), debounceTime(300), distinctUntilChanged()),
      toObservable(this.page),
    ]).pipe(switchMap(([query, page]) => this.designService.list(query, page))),
    { initialValue: EMPTY_PAGE },
  );

  onSearchChange(): void {
    this.page.set(0);
    this.searchSubject.next(this.searchTerm);
  }

  goToPage(page: number): void {
    this.page.set(page);
  }

  previewUrl(design: DesignSummary): string | null {
    return design.previewImageId ? `/api/images/${design.previewImageId}/thumbnail` : null;
  }
}
