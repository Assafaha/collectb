import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CurrentUserService } from '../../core/current-user.service';
import { Catalog } from './catalog';

describe('Catalog template required fields', () => {
  it('marks missing fields after submit, clears corrected fields and resets on cancel', async () => {
    TestBed.configureTestingModule({
      imports: [Catalog],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: CurrentUserService, useValue: { me: () => null, hasRecentStepUp: () => false } },
      ],
    });
    const init = vi.spyOn(Catalog.prototype, 'ngOnInit').mockImplementation(() => {});
    const fixture = TestBed.createComponent(Catalog);
    const component = fixture.componentInstance;
    vi.spyOn(component, 'can').mockReturnValue(true);
    vi.spyOn(component, 'canEditBrand').mockReturnValue(true);
    component.selectedCollection.set({
      collectionId: 'collection', brandId: 'brand', collectionCode: 'TEST', name: 'Test',
      description: null, releaseStatus: 'DRAFT', releaseStartAt: null, releaseEndAt: null,
      createdByUserId: 'creator', updatedByUserId: null, reviewedByUserId: null,
      reviewedAt: null, reviewComment: null, createdAt: '', updatedAt: '',
    });
    fixture.detectChanges();
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;
    const form = element.querySelector<HTMLFormElement>('.catalog-template-form')!;
    const code = form.querySelector<HTMLInputElement>('[name="templateCode"]')!;
    expect(code.getAttribute('aria-invalid')).toBe('false');
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(code.getAttribute('aria-invalid')).toBe('true');
    expect(form.classList.contains('ng-submitted')).toBe(true);
    expect(form.querySelectorAll('.catalog-field-error')).toHaveLength(3);
    TestBed.inject(HttpTestingController).expectNone((request) => request.method === 'POST');
    code.value = '   ';
    code.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(code.getAttribute('aria-invalid')).toBe('true');
    code.value = 'VALID-CODE';
    code.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(code.getAttribute('aria-invalid')).toBe('false');
    component.startTemplate();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(form.classList.contains('ng-submitted')).toBe(false);
    expect(form.querySelectorAll('.catalog-field-error')).toHaveLength(0);
    TestBed.inject(HttpTestingController).verify();
    init.mockRestore();
  });
});
