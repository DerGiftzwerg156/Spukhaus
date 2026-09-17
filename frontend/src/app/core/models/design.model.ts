export type VersionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'SUPERSEDED';

export interface DesignImage {
  id: number;
  preview: boolean;
  sortOrder: number;
  originalFilename: string;
}

export interface DesignVersion {
  id: number;
  designId: number;
  versionNumber: number;
  name: string;
  description: string;
  status: VersionStatus;
  createdAt: string;
  updatedAt: string;
  submittedAt: string | null;
  reviewedAt: string | null;
  reviewedByDisplayName: string | null;
  rejectionComment: string | null;
  images: DesignImage[];
}

export interface DesignRef {
  id: number;
  name: string;
  previewImageId: number | null;
}

export interface DesignSummary {
  id: number;
  name: string;
  previewImageId: number | null;
  ownerDisplayName: string;
  publishedAt: string | null;
  isFork: boolean;
}

export interface MyDesignSummary {
  id: number;
  name: string;
  status: VersionStatus;
  previewImageId: number | null;
  isFork: boolean;
  hasPublishedVersion: boolean;
}

export interface DesignDetail {
  id: number;
  ownerId: number;
  ownerDisplayName: string;
  createdAt: string;
  canManage: boolean;
  forkedFrom: DesignRef | null;
  publishedVersion: DesignVersion | null;
  inFlightVersion: DesignVersion | null;
  forks: DesignRef[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
