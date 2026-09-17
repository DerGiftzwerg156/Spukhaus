import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DesignDetail,
  DesignImage,
  DesignSummary,
  DesignVersion,
  MyDesignSummary,
  PageResponse,
} from '../models/design.model';

@Injectable({ providedIn: 'root' })
export class DesignService {
  constructor(private readonly http: HttpClient) {}

  list(query: string, page: number, size = 24): Observable<PageResponse<DesignSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) {
      params = params.set('query', query);
    }
    return this.http.get<PageResponse<DesignSummary>>('/api/designs', { params });
  }

  mine(): Observable<MyDesignSummary[]> {
    return this.http.get<MyDesignSummary[]>('/api/designs/mine');
  }

  reviewQueue(): Observable<DesignVersion[]> {
    return this.http.get<DesignVersion[]>('/api/designs/review-queue');
  }

  detail(id: number): Observable<DesignDetail> {
    return this.http.get<DesignDetail>(`/api/designs/${id}`);
  }

  versionHistory(id: number): Observable<DesignVersion[]> {
    return this.http.get<DesignVersion[]>(`/api/designs/${id}/versions`);
  }

  create(name: string, description: string): Observable<DesignDetail> {
    return this.http.post<DesignDetail>('/api/designs', { name, description });
  }

  fork(id: number): Observable<DesignDetail> {
    return this.http.post<DesignDetail>(`/api/designs/${id}/fork`, {});
  }

  startEditing(id: number): Observable<DesignVersion> {
    return this.http.post<DesignVersion>(`/api/designs/${id}/edit`, {});
  }

  updateDraft(id: number, name: string, description: string): Observable<DesignVersion> {
    return this.http.put<DesignVersion>(`/api/designs/${id}/draft`, { name, description });
  }

  addImage(id: number, file: File): Observable<DesignImage> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DesignImage>(`/api/designs/${id}/images`, formData);
  }

  removeImage(id: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`/api/designs/${id}/images/${imageId}`);
  }

  setPreviewImage(id: number, imageId: number): Observable<void> {
    return this.http.put<void>(`/api/designs/${id}/images/${imageId}/preview`, {});
  }

  submit(id: number): Observable<DesignVersion> {
    return this.http.post<DesignVersion>(`/api/designs/${id}/submit`, {});
  }

  approve(id: number): Observable<DesignVersion> {
    return this.http.post<DesignVersion>(`/api/designs/${id}/approve`, {});
  }

  reject(id: number, comment: string): Observable<DesignVersion> {
    return this.http.post<DesignVersion>(`/api/designs/${id}/reject`, { comment });
  }
}
