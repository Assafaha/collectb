import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { BrandPortalApiService } from '../../core/api.service';
import { CurrentUserService } from '../../core/current-user.service';
import {
  Brand,
  CatalogCollection,
  CatalogCollectionRequest,
  CatalogMediaAsset,
  CatalogTemplate,
  CatalogTemplateRequest,
} from '../../core/models';

interface CollectionForm {
  collectionCode: string;
  name: string;
  description: string;
  releaseStartAt: string;
  releaseEndAt: string;
}

interface TemplateForm {
  templateCode: string;
  displayName: string;
  rarity: string;
  editionSize: number;
  metadataSchemaVersion: string;
  metadataUri: string;
  metadataHash: string;
  mediaAssetId: string;
  traitsJson: string;
}

interface MediaForm {
  assetType: string;
  storageUri: string;
  cdnUri: string;
  contentType: string;
  checksum: string;
  fileSizeBytes: string;
}

@Component({
  selector: 'app-catalog',
  imports: [FormsModule, RouterLink, ButtonModule, InputTextModule, MessageModule],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog implements OnInit {
  @ViewChild('templateNgForm') private templateNgForm?: NgForm;
  private readonly api = inject(BrandPortalApiService);
  readonly currentUser = inject(CurrentUserService);
  private readonly route = inject(ActivatedRoute);

  readonly brand = signal<Brand | null>(null);
  readonly collections = signal<CatalogCollection[]>([]);
  readonly selectedCollection = signal<CatalogCollection | null>(null);
  readonly templates = signal<CatalogTemplate[]>([]);
  readonly mediaAssets = signal<CatalogMediaAsset[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly collectionForm = this.emptyCollectionForm();
  readonly templateForm = this.emptyTemplateForm();
  readonly mediaForm: MediaForm = {
    assetType: 'ARTWORK',
    storageUri: '',
    cdnUri: '',
    contentType: '',
    checksum: '',
    fileSizeBytes: '',
  };

  brandId = '';
  editingCollectionId: string | null = null;
  editingTemplateId: string | null = null;
  cloneSourceTemplateId: string | null = null;
  reviewComment = '';

  ngOnInit(): void {
    this.brandId = this.route.snapshot.paramMap.get('brandId') ?? '';
    this.currentUser.ensureLoaded().subscribe();
    this.api.getBrand(this.brandId).subscribe({
      next: (brand) => this.brand.set(brand),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
    this.loadCollections();
    this.loadMediaAssets();
  }

  can(permission: string): boolean {
    return this.currentUser.me()?.permissions.includes(permission) ?? false;
  }

  hasStepUp(): boolean {
    return this.currentUser.me()?.catalogMfaRequired === false || this.currentUser.hasRecentStepUp();
  }

  canEditBrand(): boolean {
    return this.brand()?.licenseStatus === 'ACTIVE';
  }

  canReview(permission: string): boolean {
    return this.can(permission) && this.hasStepUp();
  }

  selectCollection(collection: CatalogCollection): void {
    this.selectedCollection.set(collection);
    this.cancelTemplateEdit();
    this.loadTemplates(collection.collectionId);
  }

  startCollection(): void {
    this.editingCollectionId = null;
    Object.assign(this.collectionForm, this.emptyCollectionForm());
    this.clearMessages();
  }

  editCollection(collection: CatalogCollection): void {
    this.editingCollectionId = collection.collectionId;
    Object.assign(this.collectionForm, {
      collectionCode: collection.collectionCode,
      name: collection.name,
      description: collection.description ?? '',
      releaseStartAt: this.toLocalDateTime(collection.releaseStartAt),
      releaseEndAt: this.toLocalDateTime(collection.releaseEndAt),
    });
    this.clearMessages();
  }

  saveCollection(): void {
    this.clearMessages();
    const request: CatalogCollectionRequest = {
      collectionCode: this.collectionForm.collectionCode,
      name: this.collectionForm.name,
      description: this.collectionForm.description || null,
      releaseStartAt: this.toInstant(this.collectionForm.releaseStartAt),
      releaseEndAt: this.toInstant(this.collectionForm.releaseEndAt),
    };
    this.saving.set(true);
    const operation = this.editingCollectionId
      ? this.api.updateCollection(this.brandId, this.editingCollectionId, request)
      : this.api.createCollection(this.brandId, request);
    operation.subscribe({
      next: (saved) => {
        this.successMessage.set(this.editingCollectionId ? 'Collection updated.' : 'Collection created.');
        this.saving.set(false);
        this.editingCollectionId = null;
        this.loadCollections(saved.collectionId);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  deleteCollection(collection: CatalogCollection): void {
    if (!window.confirm(`Delete draft collection "${collection.name}"?`)) {
      return;
    }
    this.runCollectionAction(
      () => this.api.deleteCollection(this.brandId, collection.collectionId),
      'Collection deleted.',
      collection.collectionId,
    );
  }

  collectionAction(
    collection: CatalogCollection,
    action: 'submit-for-review' | 'withdraw-from-review' | 'publish' | 'retire',
  ): void {
    this.runCollectionAction(
      () => this.api.collectionAction(this.brandId, collection.collectionId, action),
      `Collection ${action.replaceAll('-', ' ')} completed.`,
      collection.collectionId,
    );
  }

  hasCollectionAwaitingReview(): boolean {
    return this.collections().some((collection) => collection.releaseStatus === 'READY_FOR_REVIEW');
  }

  hasTemplatesAwaitingReview(): boolean {
    return this.templates().some((template) => template.publishStatus === 'READY_FOR_REVIEW');
  }

  reviewCollection(collection: CatalogCollection, decision: 'APPROVE' | 'REQUEST_CHANGES'): void {
    this.clearMessages();
    this.api.reviewCollection(
      this.brandId,
      collection.collectionId,
      decision,
      this.reviewComment || null,
    ).subscribe({
      next: () => {
        this.successMessage.set(
          decision === 'APPROVE' ? 'Collection approved.' : 'Changes requested for collection.',
        );
        this.reviewComment = '';
        this.loadCollections(collection.collectionId);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  startTemplate(): void {
    this.editingTemplateId = null;
    this.cloneSourceTemplateId = null;
    Object.assign(this.templateForm, this.emptyTemplateForm());
    this.templateNgForm?.resetForm(this.templateForm);
    this.clearMessages();
  }

  editTemplate(template: CatalogTemplate): void {
    this.editingTemplateId = template.templateId;
    this.cloneSourceTemplateId = null;
    this.fillTemplateForm(template);
    this.templateNgForm?.resetForm(this.templateForm);
    this.clearMessages();
  }

  cloneTemplate(template: CatalogTemplate): void {
    this.editingTemplateId = null;
    this.cloneSourceTemplateId = template.templateId;
    this.fillTemplateForm(template);
    this.templateForm.templateCode = `${template.templateCode}-COPY`.slice(0, 80);
    this.templateForm.displayName = `${template.displayName} Copy`.slice(0, 200);
    this.templateNgForm?.resetForm(this.templateForm);
    this.clearMessages();
  }

  saveTemplate(form: NgForm): void {
    const collection = this.selectedCollection();
    if (!collection) {
      return;
    }
    this.clearMessages();
    if (form.invalid) {
      form.form.markAllAsTouched();
      this.errorMessage.set('Please correct the highlighted template fields.');
      return;
    }
    let traits: Record<string, string> | null;
    try {
      const parsed: unknown = JSON.parse(this.templateForm.traitsJson || '{}');
      if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object'
          || Object.values(parsed).some((value) => typeof value !== 'string')) {
        throw new Error('Traits must be a JSON object with string values.');
      }
      traits = Object.keys(parsed).length ? parsed as Record<string, string> : null;
    } catch (error) {
      this.errorMessage.set(error instanceof Error ? error.message : 'Traits must contain valid JSON.');
      return;
    }
    const request: CatalogTemplateRequest = {
      templateCode: this.templateForm.templateCode,
      displayName: this.templateForm.displayName,
      rarity: this.templateForm.rarity || null,
      editionSize: Number(this.templateForm.editionSize),
      metadataSchemaVersion: this.templateForm.metadataSchemaVersion,
      metadataUri: this.templateForm.metadataUri,
      metadataHash: this.templateForm.metadataHash || null,
      mediaAssetId: this.templateForm.mediaAssetId || null,
      traits,
    };
    this.saving.set(true);
    let operation;
    if (this.cloneSourceTemplateId) {
      operation = this.api.cloneTemplate(
        this.brandId,
        collection.collectionId,
        this.cloneSourceTemplateId,
        request.templateCode,
        request.displayName,
      );
    } else if (this.editingTemplateId) {
      operation = this.api.updateTemplate(
        this.brandId,
        collection.collectionId,
        this.editingTemplateId,
        request,
      );
    } else {
      operation = this.api.createTemplate(this.brandId, collection.collectionId, request);
    }
    operation.subscribe({
      next: () => {
        const action = this.cloneSourceTemplateId ? 'cloned' : this.editingTemplateId ? 'updated' : 'created';
        this.successMessage.set(`Template ${action}.`);
        this.saving.set(false);
        this.cancelTemplateEdit();
        this.loadTemplates(collection.collectionId);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  deleteTemplate(template: CatalogTemplate): void {
    const collection = this.selectedCollection();
    if (!collection || !window.confirm(`Delete draft template "${template.displayName}"?`)) {
      return;
    }
    this.clearMessages();
    this.api.deleteTemplate(this.brandId, collection.collectionId, template.templateId).subscribe({
      next: () => {
        this.successMessage.set('Template deleted.');
        this.loadTemplates(collection.collectionId);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  templateAction(
    template: CatalogTemplate,
    action: 'submit-for-review' | 'withdraw-from-review' | 'publish' | 'retire',
  ): void {
    const collection = this.selectedCollection();
    if (!collection) {
      return;
    }
    this.clearMessages();
    this.api.templateAction(this.brandId, collection.collectionId, template.templateId, action).subscribe({
      next: () => {
        this.successMessage.set(`Template ${action.replaceAll('-', ' ')} completed.`);
        this.loadTemplates(collection.collectionId);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  reviewTemplate(template: CatalogTemplate, decision: 'APPROVE' | 'REQUEST_CHANGES'): void {
    const collection = this.selectedCollection();
    if (!collection) {
      return;
    }
    this.clearMessages();
    this.api.reviewTemplate(
      this.brandId,
      collection.collectionId,
      template.templateId,
      decision,
      this.reviewComment || null,
    ).subscribe({
      next: () => {
        this.successMessage.set(
          decision === 'APPROVE' ? 'Template approved.' : 'Changes requested for template.',
        );
        this.reviewComment = '';
        this.loadTemplates(collection.collectionId);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  registerMedia(): void {
    this.clearMessages();
    const fileSize = this.mediaForm.fileSizeBytes ? Number(this.mediaForm.fileSizeBytes) : null;
    this.api.registerMediaAsset(this.brandId, {
      assetType: this.mediaForm.assetType,
      storageUri: this.mediaForm.storageUri,
      cdnUri: this.mediaForm.cdnUri || null,
      contentType: this.mediaForm.contentType || null,
      checksum: this.mediaForm.checksum,
      fileSizeBytes: fileSize,
    }).subscribe({
      next: () => {
        this.successMessage.set('Media reference registered.');
        this.mediaForm.storageUri = '';
        this.mediaForm.cdnUri = '';
        this.mediaForm.contentType = '';
        this.mediaForm.checksum = '';
        this.mediaForm.fileSizeBytes = '';
        this.loadMediaAssets();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private runCollectionAction(
    operation: () => Observable<unknown>,
    success: string,
    collectionId: string,
  ): void {
    this.clearMessages();
    operation().subscribe({
      next: () => {
        this.successMessage.set(success);
        if (this.selectedCollection()?.collectionId === collectionId) {
          this.selectedCollection.set(null);
          this.templates.set([]);
        }
        this.loadCollections();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private loadCollections(selectId?: string): void {
    this.loading.set(true);
    this.api.getCollections(this.brandId).subscribe({
      next: (collections) => {
        this.collections.set(collections);
        this.loading.set(false);
        const id = selectId ?? this.selectedCollection()?.collectionId;
        const selected = id ? collections.find((collection) => collection.collectionId === id) : undefined;
        if (selected) {
          this.selectCollection(selected);
        } else if (id) {
          this.selectedCollection.set(null);
          this.templates.set([]);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.fail(error);
      },
    });
  }

  private loadTemplates(collectionId: string): void {
    this.api.getTemplates(this.brandId, collectionId).subscribe({
      next: (templates) => this.templates.set(templates),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private loadMediaAssets(): void {
    this.api.getMediaAssets(this.brandId).subscribe({
      next: (assets) => this.mediaAssets.set(assets),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private cancelTemplateEdit(): void {
    this.editingTemplateId = null;
    this.cloneSourceTemplateId = null;
    Object.assign(this.templateForm, this.emptyTemplateForm());
    this.templateNgForm?.resetForm(this.templateForm);
  }

  private fillTemplateForm(template: CatalogTemplate): void {
    Object.assign(this.templateForm, {
      templateCode: template.templateCode,
      displayName: template.displayName,
      rarity: template.rarity ?? '',
      editionSize: template.editionSize,
      metadataSchemaVersion: template.metadataSchemaVersion,
      metadataUri: template.metadataUri,
      metadataHash: template.metadataHash ?? '',
      mediaAssetId: template.mediaAssetId ?? '',
      traitsJson: JSON.stringify(template.traits ?? {}, null, 2),
    });
  }

  private toInstant(value: string): string | null {
    return value ? new Date(value).toISOString() : null;
  }

  private toLocalDateTime(value: string | null): string {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 16);
  }

  private fail(error: HttpErrorResponse): void {
    this.saving.set(false);
    this.errorMessage.set(
      typeof error.error === 'string' ? error.error : error.message || 'The catalog request failed.',
    );
  }

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private emptyCollectionForm(): CollectionForm {
    return { collectionCode: '', name: '', description: '', releaseStartAt: '', releaseEndAt: '' };
  }

  private emptyTemplateForm(): TemplateForm {
    return {
      templateCode: '',
      displayName: '',
      rarity: '',
      editionSize: 1,
      metadataSchemaVersion: '1.0',
      metadataUri: '',
      metadataHash: '',
      mediaAssetId: '',
      traitsJson: '{}',
    };
  }
}
